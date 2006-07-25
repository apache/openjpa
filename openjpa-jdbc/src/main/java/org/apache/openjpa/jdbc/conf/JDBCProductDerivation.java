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
package org.apache.openjpa.jdbc.conf;

import org.apache.openjpa.conf.BrokerFactoryValue;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.ProductDerivation;
import org.apache.openjpa.jdbc.kernel.JDBCBrokerFactory;
import org.apache.openjpa.lib.conf.ConfigurationProvider;

/**
 * Sets JDBC as default store.
 */
public class JDBCProductDerivation
    implements ProductDerivation {

    static {
        BrokerFactoryValue.addDefaultAlias("jdbc",
            JDBCBrokerFactory.class.getName());
    }

    public int getType() {
        return TYPE_STORE;
    }

    public void beforeConfigurationConstruct(ConfigurationProvider cp) {
        // default to JDBC when no broker factory set
        if (BrokerFactoryValue.get(cp) == null) {
            cp.addProperty(BrokerFactoryValue.getKey(cp),
                JDBCBrokerFactory.class.getName());
        }
    }

    public void beforeConfigurationLoad(OpenJPAConfiguration c) {
    }

    public void afterSpecificationSet(OpenJPAConfiguration c) {
    }
}
