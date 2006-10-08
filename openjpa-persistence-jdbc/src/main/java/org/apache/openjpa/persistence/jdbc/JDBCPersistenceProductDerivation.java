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
package org.apache.openjpa.persistence.jdbc;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAProductDerivation;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.kernel.JDBCStoreManager;
import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.persistence.FetchPlan;
import org.apache.openjpa.persistence.PersistenceProductDerivation;

import java.util.Map;

/**
 * Sets JDBC-specific JPA specification defaults.
 *
 * @author Abe White
 * @nojavadoc
 */
public class JDBCPersistenceProductDerivation 
    extends AbstractProductDerivation 
    implements OpenJPAProductDerivation {
    
    public void initializeBrokerFactoryValueAliases(Map m) {
    }

    public int getType() {
        return TYPE_SPEC_STORE;
    }

    @Override
    public boolean beforeConfigurationLoad(Configuration c) {
        if (c instanceof OpenJPAConfiguration) {
            ((OpenJPAConfiguration) c).getStoreFacadeTypeRegistry().
                registerImplementation(FetchPlan.class, JDBCStoreManager.class, 
                JDBCFetchPlanImpl.class);
        }
        if (!(c instanceof JDBCConfigurationImpl))
            return false;

        JDBCConfigurationImpl conf = (JDBCConfigurationImpl) c;
        String jpa = PersistenceProductDerivation.SPEC_JPA;
        String ejb = PersistenceProductDerivation.ALIAS_EJB;

        conf.metaFactoryPlugin.setAlias(ejb,
            PersistenceMappingFactory.class.getName());
        conf.metaFactoryPlugin.setAlias(jpa,
            PersistenceMappingFactory.class.getName());

        conf.mappingFactoryPlugin.setAlias(ejb,
            PersistenceMappingFactory.class.getName());
        conf.mappingFactoryPlugin.setAlias(jpa,
            PersistenceMappingFactory.class.getName());

        conf.mappingDefaultsPlugin.setAlias(ejb,
            PersistenceMappingDefaults.class.getName());
        conf.mappingDefaultsPlugin.setAlias(jpa,
            PersistenceMappingDefaults.class.getName());
        return true;
    }

    @Override
    public boolean afterSpecificationSet(Configuration c) {
        if (!(c instanceof JDBCConfigurationImpl))
            return false;
        JDBCConfigurationImpl conf = (JDBCConfigurationImpl) c;
        String jpa = PersistenceProductDerivation.SPEC_JPA;
        if (!jpa.equals(conf.getSpecification()))
            return false;
        
        conf.mappingDefaultsPlugin.setDefault(jpa);
        conf.mappingDefaultsPlugin.setString(jpa);
        return true;
    }
}
