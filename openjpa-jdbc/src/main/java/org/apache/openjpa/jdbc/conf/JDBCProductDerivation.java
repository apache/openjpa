/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.jdbc.conf;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.conf.BrokerFactoryValue;
import org.apache.openjpa.conf.OpenJPAProductDerivation;
import org.apache.openjpa.jdbc.kernel.JDBCBrokerFactory;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.ConfigurationProvider;

/**
 * Sets JDBC as default store.
 */
public class JDBCProductDerivation extends AbstractProductDerivation
    implements OpenJPAProductDerivation {

    private static Set<String> supportedQueryHints = new HashSet<String>(2);

    static {
        supportedQueryHints.add(MySQLDictionary.SELECT_HINT);
        supportedQueryHints.add(OracleDictionary.SELECT_HINT);
        supportedQueryHints = Collections.unmodifiableSet(supportedQueryHints);
    }

    public void putBrokerFactoryAliases(Map m) {
        m.put("jdbc", JDBCBrokerFactory.class.getName());
    }

    public int getType() {
        return TYPE_STORE;
    }

    public boolean beforeConfigurationConstruct(ConfigurationProvider cp) {
        // default to JDBC when no broker factory set
        if (BrokerFactoryValue.get(cp) == null) {
            BrokerFactoryValue.set(cp, "jdbc");
            return true;
        }
        return false;
    }
    
    @Override
    public Set<String> getSupportedQueryHints() {
        return supportedQueryHints;
    }
}
