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
/**
 * 
 */
package org.apache.openjpa.meta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.meta.SourceTracker;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.MetaDataException;

/**
 * Captures fetch group metadata.
 */
public class FetchGroup
    implements SourceTracker {

    private final String _name;
    private final ClassMetaData _declaringClass;
    private List _includes;
    private Map _depths;

    public static final int DEFAULT_RECURSION_DEPTH = 1;
    private static String DEFAULT_GROUP_NAME = "default";

    private static final Localizer _loc = Localizer.forPackage
        (FetchGroup.class);

    /**
     * Supply immutable name.
     *
     * @param must not by null or empty.
     */
    FetchGroup(ClassMetaData cm, String name) {
        super();

        if (cm == null)
            throw new MetaDataException(_loc.get("null-class-fg", name));
        if (StringUtils.isEmpty(name))
            throw new MetaDataException(_loc.get("invalid-fg-name", cm,
                name));

        _name = name;
        _declaringClass = cm;
    }

    public String getName() {
        return _name;
    }

    /**
     * Includes given fetch group within this receiver.
     *
     * @param fg must not be null or this receiver itself or must not include
     * this receiver.
     */
    public void addInclude(FetchGroup fg) {
        if (fg == this)
            throw new MetaDataException(_loc.get("self-include-fg", this));
        if (fg == null)
            throw new MetaDataException(_loc.get("null-include-fg", this));
        if (fg.includes(this, true))
            throw new MetaDataException(_loc.get("cyclic-fg", this, fg));

        if (_includes == null)
            _includes = new ArrayList();
        _includes.add(fg);
    }

    /**
     * Affirms if given fetch group is included by this receiver.
     *
     * @param fg
     * @param recurse if true then recursively checks within the included
     * fecth groups. Otherwise just checks within direct includes.
     * @return
     */
    public boolean includes(FetchGroup fg, boolean recurse) {
        if (_includes == null)
            return false;
        if (_includes.contains(fg))
            return true;

        if (recurse)
            for (Iterator i = _includes.iterator(); i.hasNext();)
                if (((FetchGroup) i.next()).includes(fg, true))
                    return true;

        return false;
    }

    /**
     * Sets recursion depth for a field.
     *
     * @param fm
     * @param depth
     */
    public void setDepthFor(FieldMetaData fm, int depth) {
        if (depth < -1)
            throw new MetaDataException(_loc.get("invalid-fetch-depth",
                _name, fm, new Integer(depth)));

        if (_depths == null)
            _depths = new HashMap();

        _depths.put(fm, new Integer(depth));
    }

    /**
     * Gets recusrion depth for the given field.
     *
     * @param fm
     * @return defaults to 1.
     */
    public int getDepthFor(FieldMetaData fm) {
        if (_depths == null || !_depths.containsKey(fm))
            return DEFAULT_RECURSION_DEPTH;

        return ((Integer) _depths.get(fm)).intValue();
    }

    /**
     * Set the name for default group.
     * It is expected to be set only once by a compliant implementation.
     * If multiple attempts are made to set the <em>default</em> group name,
     * then an attempt will succeed only for the first time or if the given
     * name matches with the current name.
     *
     * @param name of the default fetch group
     */
    public static void setDefaultGroupName(String name) {
        //###JDO2 -- better mechanics required to set default group name
        DEFAULT_GROUP_NAME = name;
    }

    /**
     * Get the name in which <em>default</em> fetch group is known.
     *
     * @return name of the default group. Can be null, if not set.
     */
    public static final String getDefaultGroupName() {
        return DEFAULT_GROUP_NAME;
    }

    /**
     * Affirms equality if the other has the same name.
     */
    public boolean equals(Object other) {
        if (other instanceof FetchGroup) {
            FetchGroup that = (FetchGroup) other;
            return _name.equals(that._name)
                && _declaringClass.equals(that._declaringClass);
        }

        return false;
    }

    public int hashCode() {
        return _name.hashCode() + _declaringClass.hashCode();
    }

    public String toString() {
        return _name;
    }

    /////////////////
    // SourceTracker
    /////////////////

    public File getSourceFile() {
        return _declaringClass.getSourceFile();
    }

    public Object getSourceScope() {
        return _declaringClass;
    }

    public int getSourceType() {
        return _declaringClass.getSourceType();
    }

    public String getResourceName() {
        return _declaringClass.getResourceName ();
	}
}
