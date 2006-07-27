/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.util.*;

import org.apache.openjpa.meta.FetchGroup;
import org.apache.openjpa.meta.FieldMetaData;

/**
 * Holds dynamic status of fetch operation. Decides whether a field
 * requires to be selected/loaded under present condition.
 *
 * @author <A HREF="mailto:pinaki.poddar@gmail.com>Pinaki Poddar</A>
 * @nojavadoc
 */
public class FetchStateImpl implements FetchState {

    private final FetchConfiguration _config;
    private FetchState _parent;
    private FieldMetaData _relation;
    private int _availableDepth;
    
    /**
     * Supply configuration.
     *
     * @param fc must not be null.
     */
    public FetchStateImpl(FetchConfiguration fc) {
        _config = fc;
        _parent = null;
        _relation = null;
        _availableDepth  = _config.getMaxFetchDepth();
    }

    public FetchConfiguration getFetchConfiguration() {
        return _config;
    }

    public FetchState getParent() {
        return _parent;
    }
    
    public boolean isRoot() {
        return _parent == null;
    }
    
    public FetchState getRoot() {
        return (isRoot()) ? this : getParent().getRoot();
    }

    public int getAvailableFetchDepth() {
        return _availableDepth;
    }

    public List getPath() {
        if (isRoot())
            return Collections.EMPTY_LIST;
        List result = new ArrayList();
        result.add(this);
        return ((FetchStateImpl)_parent).trackPath(result);
    }
    
    private List trackPath(List path) {
        if (isRoot())
            return path;
        path.add(this);
        return ((FetchStateImpl)_parent).trackPath(path);
    }
    
    public List getRelationPath() {
        if (isRoot())
            return Collections.EMPTY_LIST;
        List result = new ArrayList();
        result.add(_relation);
        return ((FetchStateImpl)_parent).trackRelationPath(result);
    }
    
    private List trackRelationPath(List path) {
        if (isRoot())
            return path;
        path.add(_relation);
        return ((FetchStateImpl)_parent).trackRelationPath(path);
    }
    
    
    public int getCurrentRecursionDepth(FieldMetaData fm) {
        if (isRoot())
            return 0;
        int rd = (_relation == fm) ? 1 : 0;
        return rd + _parent.getCurrentRecursionDepth(fm);
    }
    
    public boolean isDefault(FieldMetaData fm) {
        return (fm.isInDefaultFetchGroup() 
            && _config.hasFetchGroup(FetchConfiguration.FETCH_GROUP_DEFAULT)) 
            || _config.hasFetchGroup(FetchConfiguration.FETCH_GROUP_ALL);
    }

    public boolean requiresFetch(FieldMetaData fm) {
        boolean selectable = isDefault(fm)
            || _config.hasAnyFetchGroup(fm.getFetchGroups())
            || _config.hasField(fm.getFullName());
        if (selectable && isRelation(fm)) {
            int rd  = getRecursionDepth(fm);
            int crd = getCurrentRecursionDepth(fm);
            selectable = (_availableDepth==INFINITE_DEPTH || _availableDepth>0)
                && (rd == INFINITE_DEPTH || crd < rd);
        }
        return selectable;
    }

    public boolean requiresLoad(OpenJPAStateManager sm, FieldMetaData fm) {
        if (sm!=null && sm.getLoaded().get(fm.getIndex()))
            return false;
        boolean loadable = isDefault(fm)
            || _config.hasAnyFetchGroup(fm.getFetchGroups())
            || _config.hasField(fm.getFullName());
        if (loadable && isRelation(fm)) {
            int rd  = getRecursionDepth(fm);
            int crd = getCurrentRecursionDepth(fm);
            loadable = (_availableDepth==INFINITE_DEPTH || _availableDepth>0)
                && (rd == INFINITE_DEPTH || crd<rd);
        }
        return loadable;
    }
    

    /**
     * Sets the recursion depth for the given field as the maximum recusion
     * depth among the groups common to this field and configured fetch groups.
     *
     * @param fm
     * @return maximum recursion depth across common fetch groups. -1 is treated
     *         as positive infinity.
     */
    public int getRecursionDepth(FieldMetaData fm) {
        Set commonFGNs = new HashSet();
        commonFGNs.addAll(_config.getFetchGroups());
        commonFGNs.retainAll(fm.getFetchGroups());
        int dMax =
            (commonFGNs.isEmpty()) ? FetchGroup.DEFAULT_RECURSION_DEPTH : 0;
        Iterator i = commonFGNs.iterator();
        while (i.hasNext()) {
            FetchGroup fg = fm.getDeclaringMetaData()
                .getFetchGroup(i.next().toString());
            int d = fg.getDepthFor(fm);
            if (d == INFINITE_DEPTH) {
                dMax = INFINITE_DEPTH;
                break;
            }
            dMax = Math.max(d, dMax);
        }
        int maxDepth = _config.getMaxFetchDepth();
        if (maxDepth != INFINITE_DEPTH)
            if (dMax != INFINITE_DEPTH)
                dMax = Math.min (maxDepth, dMax);
            else
                dMax = maxDepth;

        return dMax;
    }


    public FetchState traverse(FieldMetaData fm) {
        if (isRelation(fm)) {
            try {
                FetchStateImpl clone = (FetchStateImpl)clone();
                clone._parent = this;
                clone._relation = fm;
                clone._availableDepth  = reduce(_availableDepth);
                return clone;
            } catch (CloneNotSupportedException e) {
                // ignore
            }
        }
        return this;
    }


    int reduce(int d) {
        if (d==0)
            return 0;//throw new InternalException(this.toString());
        if (d==INFINITE_DEPTH)
            return INFINITE_DEPTH;
        return d-1;
    }
    
    protected boolean isRelation(FieldMetaData fm) {
        return fm != null && (fm.isDeclaredTypePC() 
            || fm.getElement().isDeclaredTypePC()
            || fm.getKey().isDeclaredTypePC());
    }
    
    public String toString() {
        return System.identityHashCode(this) + "("+_availableDepth+"): " 
            + printPath();
    }
    
    private String printPath()
    {
        List path = getRelationPath();
        if (path.isEmpty())
            return "";
        StringBuffer tmp = new StringBuffer();
        Iterator i = path.iterator();
        tmp.append(((FieldMetaData)i.next()).getName());
        for (;i.hasNext();)
            tmp.append(".").append(((FieldMetaData)i.next()).getName());
        return tmp.toString();
    }
}
