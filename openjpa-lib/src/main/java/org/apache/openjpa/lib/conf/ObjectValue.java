/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.lib.conf;

import org.apache.commons.lang.ObjectUtils;
import org.apache.openjpa.lib.util.Localizer;

/**
 * An object {@link Value}.
 *
 * @author Abe White
 * @author Pinaki Poddar
 */
public class ObjectValue<T> extends Value<T> {

    private static final Localizer _loc = Localizer.forPackage(ObjectValue.class);

    // cache the types' classloader
//    private static ConcurrentReferenceHashMap _classloaderCache =
//        new ConcurrentReferenceHashMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    private T _value = null;

    public ObjectValue(Class<T> type, String prop) {
        super(type, prop);
    }
    
    public void setAlias(String key, Class<? extends T> value) {
        setAlias(key, value.getName());
    }
    
    /**
     * Sets the fully-qualified name of the given class as the value for an alias with fixed 
     * name <tt>"default"</tt>.
     * Also set the string value to the key i.e. value of this plug-in is set to its
     * default value.
     *   
     * @param value value for the alias. also the value set to this plug-in.
     * 
     * @see #setDefaultAlias(String, Class)
     */
    public void setDefaultAlias(Class<? extends T> value) {
    	setDefaultAlias("default", value);
    }    
    
    /**
     * Sets the given key as the default alias for the fully-qualified name of the given class.
     * Also set the string value to the key i.e. value of this plug-in is set to its
     * default value.
     *   
     * @param key alias which is also the default
     * @param value value for the alias. also the value set to this plug-in.
     */
    public void setDefaultAlias(String key, Class<? extends T> value) {
        setAlias(key, value == null ? null : value.getName());
        setDefault(key);
        setString(key);
    }

    /**
     * The internal value.
     */
    public T get() {
        return _value;
    }

    /**
     * The internal value.
     */
    public void set(T obj) {
        set(obj, false);
    }

    /**
     * The internal value.
     *
     * @param derived if true, this value was derived from other properties
     */
    public void set(T obj, boolean derived) {
        if (!derived) assertChangeable();
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
    public T instantiate(Configuration conf) {
        return instantiate(conf, true);
    }

    /**
     * Instantiate the object as an instance of the given class.
     */
    public T instantiate(Configuration conf, boolean fatal) {
        throw new UnsupportedOperationException();
    }

    /**
     * Configure the given object.
     */
    public Object configure(T obj, Configuration conf) {
        return configure(obj, conf, true);
    }

    /**
     * Configure the given object.
     */
    public Object configure(T obj, Configuration conf, boolean fatal) {
        throw new UnsupportedOperationException();
    }

    /**
     * Allow subclasses to instantiate additional plugins. This method does
     * not perform configuration.
     */
    public T newInstance(String clsName, Configuration conf, boolean fatal) {
        return Configurations.newInstance(clsName, this, conf, conf.getClassLoader(), fatal);
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
            throw new IllegalArgumentException(_loc.get("cant-set-string", getProperty()).getMessage());
    }

    protected void setInternalObject(Object obj) {
        set((T)obj);
    }
    
    public Object getExternal() {
      return isHidden() ? Value.INVISIBLE : getString();
    }

}
