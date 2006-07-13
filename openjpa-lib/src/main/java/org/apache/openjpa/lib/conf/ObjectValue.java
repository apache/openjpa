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
package org.apache.openjpa.lib.conf;

import org.apache.commons.lang.ObjectUtils;
import org.apache.openjpa.lib.util.Localizer;

/**
 * An object {@link Value}.
 *
 * @author Abe White
 */
public class ObjectValue extends Value {

    private static final Localizer _loc = Localizer.forPackage
        (ObjectValue.class);

    private Object _value = null;

    public ObjectValue(String prop) {
        super(prop);
    }

    /**
     * The internal value.
     */
    public Object get() {
        return _value;
    }

    /**
     * The internal value.
     */
    public void set(Object obj) {
        set(obj, false);
    }

    /**
     * The internal value.
     *
     * @param derived if true, this value was derived from other properties
     */
    public void set(Object obj, boolean derived) {
        Object oldValue = _value;
        _value = obj;
        if (!derived && !ObjectUtils.equals(obj, oldValue)) {
            objectChanged();
            valueChanged();
        }
    }

    /**
     * Instantiate the object as an instance of the given class. Equivalent
     * to <code>instantiate(type, conf, true)</code>.
     */
    public Object instantiate(Class type, Configuration conf) {
        return instantiate(type, conf, true);
    }

    /**
     * Instantiate the object as an instance of the given class.
     */
    public Object instantiate(Class type, Configuration conf, boolean fatal) {
        throw new UnsupportedOperationException();
    }

    /**
     * Allow subclasses to instantiate additional plugins. This method does
     * not perform configuration.
     */
    public Object newInstance(String clsName, Class type,
        Configuration conf, boolean fatal) {
        return Configurations.newInstance(clsName, this, conf,
            type.getClassLoader(), fatal);
    }

    public Class getValueType() {
        return Object.class;
    }

    /**
     * Implement this method to synchronize internal data with the new
     * object value.
     */
    protected void objectChanged() {
    }

    protected String getInternalString() {
        return null;
    }

    protected void setInternalString(String str) {
        if (str == null)
            set(null);
        else
            throw new IllegalArgumentException(_loc.get("cant-set-string",
                getProperty()));
    }

    protected void setInternalObject(Object obj) {
        set(obj);
    }
}
