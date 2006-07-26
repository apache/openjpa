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

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.conf.ProductDerivation;
import org.apache.openjpa.lib.conf.ConfigurationProvider;

/**
 * Sets JPA specification defaults.
 *
 * @author Abe White
 * @nojavadoc
 */
public class PersistenceProductDerivation
    implements ProductDerivation {

    public static final String SPEC_JPA = "jpa";
    public static final String ALIAS_EJB = "ejb";

    public int getType() {
        return TYPE_SPEC;
    }

    public void beforeConfigurationConstruct(ConfigurationProvider cp) {
    }

    public void beforeConfigurationLoad(OpenJPAConfiguration c) {
        if (!(c instanceof OpenJPAConfigurationImpl))
            return;

        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setAlias(SPEC_JPA,
            PersistenceMetaDataFactory.class.getName());
        conf.metaFactoryPlugin.setAlias(ALIAS_EJB,
            PersistenceMetaDataFactory.class.getName());
        conf.metaFactoryPlugin.setDefault(SPEC_JPA);
        conf.metaFactoryPlugin.setString(SPEC_JPA);
    }

    public void afterSpecificationSet(OpenJPAConfiguration c) {
        if (!(c instanceof OpenJPAConfigurationImpl)
            || !SPEC_JPA.equals(c.getSpecification()))
            return;

        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setDefault(SPEC_JPA);
        conf.metaFactoryPlugin.setString(SPEC_JPA);
        conf.lockManagerPlugin.setDefault("version");
        conf.lockManagerPlugin.setString("version");
        conf.nontransactionalWrite.setDefault("true");
        conf.nontransactionalWrite.set(true);
    }
}
