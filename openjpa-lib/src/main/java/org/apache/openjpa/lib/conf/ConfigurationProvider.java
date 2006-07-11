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
 * @since 4.0.0
 */
public interface ConfigurationProvider {

    /**
     * Load defaults, or return false if no defaults for this provider found.
     */
    public boolean loadDefaults(ClassLoader loader) throws Exception;

    /**
     * Load the given given resource, or return false if it is not a resource
     * this provider understands. The given class loader may be null.
     */
    public boolean load(String resource, ClassLoader loader) throws Exception;

    /**
     * Load given file, or return false if it is not a file this provider
     * understands.
     */
    public boolean load(File file) throws Exception;

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
}
