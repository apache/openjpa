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

import java.io.File;
import java.util.Map;

/**
 * Implementations of this interface can populate {@link Configuration}s in
 * some environment-specific way. Implementations must implement the
 * <code>equals</code> and <code>hashCode</code> methods so that equivalent
 * configurations compare equal.
 *
 * @nojavadoc
 * @since 0.4.0.0
 */
public interface ConfigurationProvider {

    /**
     * Return properties loaded thus far, or empty map if none.
     */
    public Map getProperties();

    /**
     * Add the given properties to those in this provider, overwriting
     * any exisitng properties under the same keys.
     */
    public void addProperties(Map props);

    /**
     * Add a single property, overwriting any existing property under the
     * same key.
     */
    public Object addProperty(String key, Object value);

    /**
     * Set loaded information into the given configuration.
     */
    public void setInto(Configuration conf);
    
    /**
     * Loads the global resource. The meaning of <em>global</em> is specific
     * to concrte implementaion. 
     * @param loader used to locate the resource. If null uses the current
     * thread's loader.
     * @return true if located and loaded global configuration 
     */
    public boolean loadGlobals(ClassLoader loader) throws Exception;

    /**
     * Loads the default resource. The meaning of <em>default</em> is specific
     * to concrte implementaion. 
     * @param loader used to locate the resource. If null uses the current
     * thread's loader.
     * @return true if located and loaded default configuration 
     */
    public boolean loadDefaults(ClassLoader loader) throws Exception;
    
    /**
     * Loads the given resource. 
     * @param resource name of the resource
     * @param anchor optional named anchor within a resource containing multiple
     * configuration
     * @param loader used to locate the resource. If null uses the current
     * thread's loader.
     * @return true if located and loaded configuration 
     */
    public boolean load(String resource, String anchor, ClassLoader loader) 
        throws Exception;
    
    public boolean load(String resource, String anchor, Map map) 
        throws Exception;

    /**
     * Loads the given resource. 
     * @param file name of the file to load from
     * @param anchor optional named anchor within a file containing multiple
     * configuration
     * @return true if located and loaded configuration 
     */
    public boolean load(File file, String anchor) throws Exception;


}
