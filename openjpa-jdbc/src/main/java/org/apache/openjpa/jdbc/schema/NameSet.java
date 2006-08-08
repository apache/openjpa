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
package org.apache.openjpa.jdbc.schema;

import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Name sets track what names have been taken, ignoring case.
 * {@link SchemaGroup}s implement this interface for tables, indexes, and
 * constraints; {@link Table}s implement it for their columns.
 *
 * @author Abe White
 */
public class NameSet {

    private static final Localizer _loc = Localizer.forPackage(NameSet.class);

    private Set _names = null;

    /**
     * Return true if the given name is in use already.
     */
    public boolean isNameTaken(String name) {
        if (name == null)
            return true;
        return _names != null && _names.contains(name.toUpperCase());
    }

    /**
     * Attempt to add the given name to the set.
     *
     * @param name the name to add
     * @param validate if true, null or empty names will not be accepted
     */
    protected void addName(String name, boolean validate) {
        if (name == null || name.length() == 0) {
            if (validate)
                throw new IllegalArgumentException(_loc.get("bad-name", name)
                    .getMessage());
            return;
        }

        // unfortunately, we can't check for duplicate names, because different
        // DBs use different namespaces for components, and it would be
        // difficult to find a scheme that fits all and is still useful
        if (_names == null)
            _names = new HashSet();
        _names.add(name.toUpperCase());
    }

    /**
     * Remove the given name from the table.
     */
    protected void removeName(String name) {
        if (name != null && _names != null)
            _names.remove(name.toUpperCase());
    }
}
