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
package serp.bytecode;

import java.util.Collection;
import java.util.Collections;

/**
 * State implementing the behavior of a primitive class.
 *
 * @author Abe White
 */
class PrimitiveState extends State {

    private final Class _type;
    private final NameCache _names;

    public PrimitiveState(Class type, NameCache names) {
        _type = type;
        _names = names;
    }

    public int getMagic() {
        return Constants.VALID_MAGIC;
    }

    public int getMajorVersion() {
        return Constants.MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return Constants.MINOR_VERSION;
    }

    public int getAccessFlags() {
        return Constants.ACCESS_PUBLIC | Constants.ACCESS_FINAL;
    }

    public int getIndex() {
        return 0;
    }

    public int getSuperclassIndex() {
        return 0;
    }

    public Collection getInterfacesHolder() {
        return Collections.EMPTY_LIST;
    }

    public Collection getFieldsHolder() {
        return Collections.EMPTY_LIST;
    }

    public Collection getMethodsHolder() {
        return Collections.EMPTY_LIST;
    }

    public Collection getAttributesHolder() {
        return Collections.EMPTY_LIST;
    }

    public String getName() {
        return _names.getExternalForm(_type.getName(), false);
    }

    public String getSuperclassName() {
        return null;
    }

    public String getComponentName() {
        return null;
    }

    public boolean isPrimitive() {
        return true;
    }

    public boolean isArray() {
        return false;
    }
}
