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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.PluginValue;

/**
 * Value type used to represent the {@link BrokerFactory}. This type is
 * defined separately so that it can be used both in the global configuration
 * and in {@link org.apache.openjpa.kernel.Bootstrap} with the same
 * encapsulated configuration.
 *
 * @author Abe White
 * @nojavadoc
 */
public class BrokerFactoryValue
    extends PluginValue {

    private static final String KEY = "BrokerFactory";

    private static final String[] ALIASES = new String[]{
        "abstractstore",
        "org.apache.openjpa.abstractstore.AbstractStoreBrokerFactory",
    };
    
    private static final Collection _prefixes = new HashSet();
    
    static {
        _prefixes.add("openjpa");
    }

    public BrokerFactoryValue() {
        this(KEY);
    }

    public BrokerFactoryValue(String prop) {
        super(prop, false);
        setAliases(ALIASES);
    }

    /**
     * Extract the concrete {@link BrokerFactory} class name that the specified
     * configuration will use.
     */
    public static Object getBrokerFactoryClassName(ConfigurationProvider cp) {
        Map props = cp.getProperties();
        for (Iterator iter = _prefixes.iterator(); iter.hasNext(); ) {
            Object bf = props.get(iter.next() + "." + KEY);
            if (bf != null)
                return  bf;
        }
        return null;
    }

    /**
     * Return the property to use for setting the broker factory for 
     * <code>cp</code>.
     */
    public static String getBrokerFactoryProperty(ConfigurationProvider cp) {
        return _prefixes.iterator().next() + "." 
            + BrokerFactoryValue.KEY; 
    }
    
    /**
     * Add <code>prefix</code> to the list of prefixes under which configuration
     * properties may be scoped.
     */
    public static void addPropertyPrefix(String prefix) {
        _prefixes.add(prefix);
    }
}
