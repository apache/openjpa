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
package org.apache.openjpa.conf;

import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.ConfigurationProvider;

/**
 * Hooks for deriving products with additional functionality.
 * All implementations of this interface will have a chance to mutate
 * a {@link Configuration} both before and after the user-specified
 * configuration data is loaded. The order in which the
 * derivations are evaluated is determined by the specificity of the
 * derivation type.
 *
 * @since 0.4.1
 */
public interface ProductDerivation {

    public static final int TYPE_SPEC = 0;
    public static final int TYPE_PRODUCT = 1;
    public static final int TYPE_STORE = 2;
    public static final int TYPE_SPEC_STORE = 3;
    public static final int TYPE_PRODUCT_STORE = 4;
    public static final int TYPE_FEATURE = 5;

    /**
     * Return the type of derivation.
     */
    public int getType();

    /**
     * Provides the instance with a callback to mutate the initial properties
     * of the {@link ConfigurationProvider}. This is primarily to alter or
     * add properties that determine what type of configuration is constructed,
     * and therefore is typically used at runtime only.
     */
    public void beforeConfigurationConstruct(ConfigurationProvider cp);

    /**
     * Provides the instance with the opportunity to mutate
     * <code>conf</code> before the user configuration is applied.
     */
    public void beforeConfigurationLoad(OpenJPAConfiguration conf);

    /**
     * Called after the specification has been set.
     */
    public void afterSpecificationSet(OpenJPAConfiguration conf);
}
