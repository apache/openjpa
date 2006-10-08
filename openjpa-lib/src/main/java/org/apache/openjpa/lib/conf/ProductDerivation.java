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
 * Hooks for deriving products with additional functionality.
 * Parses configuration information from global, default or explictly-specified
 * resources. All implementations of this interface will have a chance to mutate
 * a {@link Configuration} both before and after the user-specified 
 * configuration data is loaded. The order in which the product derivations are
 * evaluated is determined by the specificity of the derivation type.
 *
 * @author Abe White
 * @author Pinaki Poddar
 * @since 0.4.1
 */
public interface ProductDerivation {

    public static final int TYPE_PRODUCT = 100;
    public static final int TYPE_FEATURE = 1000;

    /**
     * Return the type of derivation.
     */
    public int getType();

    /**
     * Return the configuration prefix for properties of this product.
     */
    public String getConfigurationPrefix();

    /**
     * Load globals into the returned ConfigurationProvider, or return null if 
     * no globals are found.
     */
    public ConfigurationProvider loadGlobals(ClassLoader loader) 
        throws Exception;

    /**
     * Load defaults into the returned ConfigurationProvider, or return null if 
     * no defaults are found.
     */
    public ConfigurationProvider loadDefaults(ClassLoader loader) 
        throws Exception;

    /**
     * Load the given given resource into the returned ConfigurationProvider, 
     * or return null if it is not a resource this receiver understands. 
     * The given class loader may be null.
     *
     * @param anchor optional named anchor within a multiple-configuration
     * resource
     */
    public ConfigurationProvider load(String resource, String anchor, 
        ClassLoader loader) 
        throws Exception;

    /**
     * Load given file, or return null if it is not a file this receiver
     * understands.
     *
     * @param anchor optional named anchor within a multiple-configuration file
     */
    public ConfigurationProvider load(File file, String anchor) 
        throws Exception;
    
    /**
     * Provides the instance with a callback to mutate the initial properties
     * of the {@link ConfigurationProvider}. This is primarily to alter or
     * add properties that determine what type of configuration is constructed,
     * and therefore is typically used at runtime only.
     * 
     * @return true if given ConfigurationProvider has been mutated.
     */
    public boolean beforeConfigurationConstruct(ConfigurationProvider cp);

    /**
     * Provides the instance with the opportunity to mutate
     * <code>conf</code> before the user configuration is applied.
     *
     * @return true if given Configuration has been mutated.
     */
    public boolean beforeConfigurationLoad(Configuration conf);

    /**
     * Called after the specification has been set.
     *
     * @return true if given Configuration has been mutated.
     */
    public boolean afterSpecificationSet(Configuration conf);
}
