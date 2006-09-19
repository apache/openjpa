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

import java.util.Map;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.conf.OpenJPAProductDerivation;
import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.PluginValue;

/**
 * Sets JPA specification defaults.
 *
 * @author Abe White
 * @nojavadoc
 */
public class PersistenceProductDerivation 
    extends AbstractProductDerivation
    implements OpenJPAProductDerivation {

    public static final String SPEC_JPA = "jpa";
    public static final String ALIAS_EJB = "ejb";

    public int getType() {
        return TYPE_SPEC;
    }

    @Override
    public ConfigurationProvider newConfigurationProvider() {
        return new ConfigurationProviderImpl();
    }

    @Override
    public ConfigurationProvider load(String rsrc, String anchor,
            ClassLoader loader)  throws Exception {
        if (rsrc != null && !rsrc.endsWith(".xml"))
            return null;
        return super.load(rsrc, anchor, loader);
    }
    
    public ConfigurationProvider load(String rsrc, String anchor,
            Map map) throws Exception {
        if (rsrc != null && !rsrc.endsWith(".xml"))
            return null;
        return super.load(rsrc, anchor, map);
    }
    
    public boolean beforeConfigurationConstruct(ConfigurationProvider cp) {
        if (EntityManagerFactoryValue.get(cp) == null) {
            cp.addProperty(EntityManagerFactoryValue.getKey(cp),
                EntityManagerFactoryImpl.class.getName());
            return true;
        }
        return false;
    }
    
    public boolean beforeConfigurationLoad(Configuration c) {
        if (!(c instanceof OpenJPAConfigurationImpl))
            return false;
        
        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setAlias(ALIAS_EJB,
            PersistenceMetaDataFactory.class.getName());
        conf.metaFactoryPlugin.setAlias(SPEC_JPA,
            PersistenceMetaDataFactory.class.getName());
        
        PluginValue emfPlugin = new EntityManagerFactoryValue();
        conf.addValue(emfPlugin);
        return true;
    }

    public boolean afterSpecificationSet(Configuration c) {
      if (!(c instanceof OpenJPAConfigurationImpl)
         || !SPEC_JPA.equals(((OpenJPAConfiguration)c).getSpecification()))
          return false;
 
        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setDefault(SPEC_JPA);
        conf.metaFactoryPlugin.setString(SPEC_JPA);
        conf.lockManagerPlugin.setDefault("version");
        conf.lockManagerPlugin.setString("version");
        conf.nontransactionalWrite.setDefault("true");
        conf.nontransactionalWrite.set(true);
        return true;
    }
}
