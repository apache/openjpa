/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.meta;

import org.apache.commons.collections.map.*;

import org.apache.openjpa.lib.util.*;

import org.xml.sax.*;

import java.util.*;


/**
 *  <p>Helps serialize metadata objects to package and class elements.</p>
 *
 *  @see CFMetaDataParser
 *
 *  @author Abe White
 *  @nojavadoc */
public abstract class CFMetaDataSerializer extends XMLMetaDataSerializer {
    private static final Localizer _loc = Localizer.forPackage(CFMetaDataSerializer.class);
    private String _package = null;

    /**
     *  The default package for objects being serialized.
     */
    protected String getPackage() {
        return _package;
    }

    /**
     *  The default package for objects being serialized.
     */
    protected void setPackage(String pkg) {
        _package = pkg;
    }

    /**
     *  Helper method to group objects by package.
     *
     *  @return mapping of package name to a collection of objects in
     *                          that package
     */
    protected Map groupByPackage(Collection objs) throws SAXException {
        Map packages = new LinkedMap();
        String packageName;
        Collection packageObjs;
        Object obj;

        for (Iterator itr = objs.iterator(); itr.hasNext();) {
            obj = itr.next();
            packageName = getPackage(obj);
            packageObjs = (Collection) packages.get(packageName);

            if (packageObjs == null) {
                packageObjs = new LinkedList();
                packages.put(packageName, packageObjs);
            }

            packageObjs.add(obj);
        }

        return packages;
    }

    /**
     *  Return the package name of the given object, or null if not in a
     *  package.  Used by {@link #groupByPackage}.  Returns null by default.
     */
    protected String getPackage(Object obj) {
        return null;
    }

    /**
     *  Returns the given class name, stripping the package if it is not
     *  needed.
     */
    protected String getClassName(String name) {
        // check if in current package; make sure not in a sub-package
        if ((_package != null) && (name.lastIndexOf('.') == _package.length()) &&
                name.startsWith(_package)) {
            return name.substring(_package.length() + 1);
        }

        // check other known packages
        String[] packages = CFMetaDataParser.PACKAGES;

        for (int i = 0; i < packages.length; i++)
            if (name.startsWith(packages[i]) &&
                    (name.lastIndexOf('.') == (packages[i].length() - 1))) {
                return name.substring(packages[i].length());
            }

        return name;
    }
}
