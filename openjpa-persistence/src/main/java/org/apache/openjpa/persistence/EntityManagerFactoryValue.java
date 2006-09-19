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
package org.apache.openjpa.persistence;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.openjpa.abstractstore.AbstractStoreBrokerFactory;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.PluginValue;

/**
 * Value type used to represent the {@link EntityManagerFactory}. 
 *
 * @nojavadoc
 */
public class EntityManagerFactoryValue
    extends PluginValue {

    public static final String KEY = "EntityManagerFactory";

    private static final List _prefixes = new ArrayList(2);
    static {
        _prefixes.add("openjpa");
    }
    
    /**
     * Add <code>prefix</code> to the list of prefixes under which configuration
     * properties may be scoped.
     */
    public static void addPropertyPrefix(String prefix) {
        if (!_prefixes.contains(prefix))
            _prefixes.add(prefix);
    }
    
    /**
     * Extract the value of this property if set in the given provider.
     */
    public static Object get(ConfigurationProvider cp) {
        Map props = cp.getProperties();
        Object bf;
        for (int i = 0; i < _prefixes.size (); i++) {
            bf = props.get(_prefixes.get(i) + "." + KEY);
            if (bf != null)
                return  bf;
        }
        return null;
    }

    /**
     * Return the key to use for this property.
     */
    public static String getKey(ConfigurationProvider cp) {
        return _prefixes.get(0) + "." + KEY;
    }

    public EntityManagerFactoryValue() {
        super(KEY, false);
    }
}
