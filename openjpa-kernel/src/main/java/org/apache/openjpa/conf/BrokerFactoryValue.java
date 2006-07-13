/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.conf;

import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.lib.conf.PluginValue;

/**
 * <p>Value type used to represent the {@link BrokerFactory}.  This type is
 * defined separately so that it can be used both in the global configuration
 * and in {@link org.apache.openjpa.kernel.Bootstrap} with the same 
 * encapsulated configuration.</p>
 *
 * @author Abe White
 * @nojavadoc
 */
public class BrokerFactoryValue
    extends PluginValue {

    public static final String KEY = "org.apache.openjpa.BrokerFactory";

    private static final String[] ALIASES = new String[]{
        "abstractstore",
        "org.apache.openjpa.abstractstore.AbstractStoreBrokerFactory",
    };

    public BrokerFactoryValue() {
        this(KEY);
    }

    public BrokerFactoryValue(String prop) {
        super(prop, false);
        setAliases(ALIASES);
    }
}
