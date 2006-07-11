/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FetchGroup;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Holds dynamic status of fetch operation. Decides whether a field
 * requires to be selected/loaded under present condition.
 *
 * @author <A HREF="mailto:pinaki.poddar@gmail.com>Pinaki Poddar</A>
 * @nojavadoc
 */
public class FetchStateImpl implements FetchState, Serializable {

    private final FetchConfiguration _config;
    private Map _selectTraversals;
    private Map _loadTraversals;
    private Map _recursionDepths;
    private Map _depths;
    private int _depth;
    private final Set _knownExcludes;
    private static final int INFINITE_DEPTH = -1;

    /**
     * Supply configuration.
     *
     * @param fc must not be null.
     */
    public FetchStateImpl(FetchConfiguration fc) {
        super();
        _config = fc;
        _knownExcludes = new HashSet();
        _selectTraversals = new HashMap();
        _loadTraversals = new HashMap();
        _depths = new HashMap();
        _recursionDepths = new HashMap();
    }

    public FetchConfiguration getFetchConfiguration() {
        return _config;
    }

    public boolean isDefault(FieldMetaData fm) {
        return _config.hasFetchGroup(FetchGroup.getDefaultGroupName())
            && fm.isInDefaultFetchGroup();
    }

    public boolean requiresSelect(FieldMetaData fm, boolean changeState) {
        if (_knownExcludes.contains(fm))
            return false;
        boolean selectable = isDefault(fm)
            || _config.hasFetchGroup(fm.getFetchGroups())
            || _config.hasField(fm.getFullName());
        if (!selectable)
            _knownExcludes.add(fm);
        if (selectable && JavaTypes.maybePC(fm)) { // relation field
            if (canTraverse(fm)) {
                if (changeState)
                    traverse(fm);
            } else selectable = false;
        }
        return selectable;
    }

    public boolean requiresLoad(OpenJPAStateManager sm, FieldMetaData fm) {
        boolean loadable = isDefault(fm)
            || _config.hasFetchGroup(fm.getFetchGroups())
            || _config.hasField(fm.getFullName());
        if (!loadable)
            _knownExcludes.add(fm);
        // relation field
        if (loadable && JavaTypes.maybePC(fm)) {
            int d = getLoadCount(fm);
            loadable = (d < (getTraversalCount(fm) - 1));
            if (loadable)
                _loadTraversals.put(fm, new Integer(d + 1));
        }
        return loadable;
    }

    /**
     * Get the recusion depth for the given field.
     *
     * @param fm     is the field to look for
     * @param depths is the map of field to integer depth.
     * @return 0 if the field does not appear in the given map.
     */
    protected int getRecursionDepth(FieldMetaData fm) {
        if (_recursionDepths.containsKey(fm)) {
            return ((Integer) _recursionDepths.get(fm)).intValue();
        }
        return initalizeRecusrionDepth(fm);
    }

    /**
     * Sets the recursion depth for the given field as the maximum recusion
     * depth among the groups common to this field and configured fetch groups.
     *
     * @param fm
     * @return maximum recursion depth across common fetch groups. -1 is treated
     *         as positive infinity.
     */
    protected int initalizeRecusrionDepth(FieldMetaData fm) {
        Set commonFGNs = new HashSet();
        commonFGNs.addAll(_config.getFetchGroups());
        commonFGNs.retainAll(fm.getFetchGroups());
        int dMax =
            (commonFGNs.isEmpty()) ? FetchGroup.DEFAULT_RECURSION_DEPTH : 0;
        Iterator i = commonFGNs.iterator();
        while (i.hasNext()) {
            FetchGroup fg = fm.getDeclaringMetaData()
                .getFetchGroup(i.next().toString(), false);
            int d = fg.getDepthFor(fm);
            if (d == INFINITE_DEPTH) {
                dMax = INFINITE_DEPTH;
                break;
            }
            dMax = Math.max(d, dMax);
        }
        _recursionDepths.put(fm, new Integer(dMax));
        return dMax;
    }

    boolean canTraverse(FieldMetaData fm) {
        int maxDepth = _config.getMaxFetchDepth();
        if (maxDepth != INFINITE_DEPTH && _depth > maxDepth)
            return false;
        int sourceDepth = getDepth(fm.getDeclaringMetaData());
        int traversalCount = getTraversalCount(fm);
        int recursionDepth = getRecursionDepth(fm);
        int newtargetDepth = sourceDepth + traversalCount + 1;
        boolean isRecursive = fm.getDeclaringMetaData() ==
            fm.getDeclaredTypeMetaData();
        boolean traversable = (maxDepth == INFINITE_DEPTH)
            || (recursionDepth == INFINITE_DEPTH);
        if (isRecursive)
            traversable = traversable || (traversalCount < recursionDepth);
        else traversable = traversable || (newtargetDepth <= maxDepth);
        return traversable;
    }

    void traverse(FieldMetaData fm) {
        int sourceDepth = getDepth(fm.getDeclaringMetaData());
        int targetDepth = getDepth(fm.getDeclaredTypeMetaData());
        int traversalCount = getTraversalCount(fm);
        boolean isRecursive = fm.getDeclaringMetaData() ==
            fm.getDeclaredTypeMetaData();
        if (!isRecursive) {
            int newDepth = sourceDepth + traversalCount;
            _depths.put(fm.getDeclaredTypeMetaData(), new Integer(newDepth));
            _depth = Math.max(_depth, newDepth);
        }
        _selectTraversals.put(fm, new Integer(traversalCount + 1));
    }

    int getTraversalCount(FieldMetaData fm) {
        Integer n = (Integer) _selectTraversals.get(fm);
        return (n == null) ? 0 : n.intValue();
    }

    int getLoadCount(FieldMetaData fm) {
        Integer n = (Integer) _loadTraversals.get(fm);
        return (n == null) ? 0 : n.intValue();
    }

    int getDepth(ClassMetaData cm) {
        if (_depths.containsKey(cm))
            return ((Integer) _depths.get(cm)).intValue();
        return 0;
    }

    /**
     * Combination of an instance and its field used as key.
     */
    private static class InstanceFieldKey {

        final OpenJPAStateManager _sm;
        final FieldMetaData _fm;

        /**
         * Supply configuration.
         *
         * @param sm can be null
         * @param fm must not be null
         */
        public InstanceFieldKey(OpenJPAStateManager sm, FieldMetaData fm) {
            _sm = sm;
            _fm = fm;
        }

        public boolean equals(Object other) {
            if (other instanceof InstanceFieldKey) {
                InstanceFieldKey that = (InstanceFieldKey) other;
                return (_sm == that._sm) && (_fm == that._fm);
            }
            return false;
        }

        public int hashCode() {
            int smHash = (_sm != null) ? _sm.hashCode() : 0;
            int fmHash = (_fm != null) ? _fm.hashCode() : 0;
            return smHash + fmHash;
        }

        public String toString() {
            return _sm + "." + _fm;
        }
    }
}
