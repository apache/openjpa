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
package org.apache.openjpa.lib.conf.test;

import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.ProductDerivation;

/**
 * A Product Derivation to test loading of global and default configuration with
 * System settings. This provider uses 
 * {@link ConfigurationTestConfigurationProvider} which reads its global from
 * a file specified by <code>"openjpatest.properties"</code> system property.
 *
 * @author Pinaki Poddar
 */
public class ConfigurationTestProductDerivation 
    extends AbstractProductDerivation {
    
    public int getType() {
        return ProductDerivation.TYPE_PRODUCT;
    }
    
    public ConfigurationProvider newConfigurationProvider() {
        return new ConfigurationTestConfigurationProvider();
    }
    
}

