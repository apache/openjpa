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
 * Abstract implementation of ProductDerivation loads configuration data using
 * ConfigurationProvider supplied by concrete implementation via 
 * {@link #newConfigurationProvider()} method.<p>
 * This abstract implementation does <em>not</em> provide a concrete 
 * ConfigurationProvider and hence all its loadXXX() methods would return null
 * by default. When the concrete subclass supplies a ConfigurationProvider,
 * this abstratct implementation will use it to load the configuration data. 
 *
 * @author Pinaki Poddar
 * @since 0.4.1
 */

public abstract class AbstractProductDerivation
    implements ProductDerivation {

    public ConfigurationProvider loadGlobals(ClassLoader loader)
            throws Exception {
        ConfigurationProvider provider = newConfigurationProvider();
        if (provider != null && provider.loadGlobals(loader))
            return provider;
        return null;
    }

    public ConfigurationProvider loadDefaults(ClassLoader loader)
            throws Exception {
        ConfigurationProvider provider = newConfigurationProvider();
        if (provider != null && provider.loadDefaults(loader))
            return provider;
        return null;
    }

    public ConfigurationProvider load(String resource, String anchor,
            ClassLoader loader)  throws Exception {
        ConfigurationProvider provider = newConfigurationProvider();
        if (provider != null && provider.load(resource, anchor, loader))
            return provider;
        return null;
    }
    
    public ConfigurationProvider load(String resource, String anchor,
            Map map) throws Exception {
        ConfigurationProvider provider = newConfigurationProvider();
        if (provider != null && provider.load(resource, anchor, map))
            return provider;
        return null;
    }

    public ConfigurationProvider load(File file, String anchor)
            throws Exception {
        ConfigurationProvider provider = newConfigurationProvider();
        if (provider != null && provider.load(file, anchor))
            return provider;
        return null;
    }

    public boolean beforeConfigurationConstruct(ConfigurationProvider cp) {
        return false;
    }

    public boolean beforeConfigurationLoad(Configuration conf) {
        return false;
    }

    public boolean afterSpecificationSet(Configuration conf) {
        return false;
    }
    
    public abstract ConfigurationProvider newConfigurationProvider();
}
