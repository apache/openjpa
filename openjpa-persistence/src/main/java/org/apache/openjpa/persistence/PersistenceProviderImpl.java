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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.enhance.PCClassFileTransformer;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.ProductDerivation;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;


/**
 * Bootstrapping class that allows the creation of a stand-alone
 * {@link EntityManager}.
 *
 * @see Persistence#createEntityManagerFactory(String,Map)
 */
public class PersistenceProviderImpl
    implements PersistenceProvider {

    static final String CLASS_TRANSFORMER_OPTIONS = "ClassTransformerOptions";

    /**
     * Loads the entity manager specified by <code>name</code>, applying
     * the properties in <code>m</code> as overrides to the properties defined
     * in the XML configuration file for <code>name</code>. If <code>name</code>
     * is <code>null</code>, this method loads the XML in the resource
     * identified by <code>resource</code>, and uses the first resource found
     * when doing this lookup, regardless of the name specified in the XML
     * resource or the name of the jar that the resource is contained in.
     * This does no pooling of EntityManagersFactories.
     */
    public EntityManagerFactory createEntityManagerFactory(String name,
        String resource, Map m) {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        try {
            ConfigurationProvider cp = pd.load(resource, name, m);
            if (cp == null)
                return null;

            BrokerFactory factory = Bootstrap.newBrokerFactory(cp, null);
            return OpenJPAPersistence.toEntityManagerFactory(factory);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    public EntityManagerFactory createEntityManagerFactory(String name, Map m) {
        return createEntityManagerFactory(name, null, m);
    }

    public EntityManagerFactory createContainerEntityManagerFactory(
        PersistenceUnitInfo pui, Map m) {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        try {
            ConfigurationProvider cp = pd.load(pui, m);
            if (cp == null)
                return null;

            // add enhancer
            String ctOpts = (String) Configurations.getProperty
                (CLASS_TRANSFORMER_OPTIONS, pui.getProperties());
            pui.addTransformer(new ClassTransformerImpl(cp, ctOpts, 
                pui.getNewTempClassLoader()));

            BrokerFactory factory = Bootstrap.newBrokerFactory(cp, 
                pui.getClassLoader());
            return OpenJPAPersistence.toEntityManagerFactory(factory);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }
    
    /**
     * Java EE 5 class transformer.
     */
    private static class ClassTransformerImpl
        implements ClassTransformer {

        private final ClassFileTransformer _trans;

        private ClassTransformerImpl(ConfigurationProvider cp, String props, 
            final ClassLoader tmpLoader) {
            // create an independent conf for enhancement
            OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
            cp.setInto(conf);
            // don't allow connections
            conf.setConnectionUserName(null);
            conf.setConnectionPassword(null);
            conf.setConnectionURL(null);
            conf.setConnectionDriverName(null);
            conf.setConnectionFactoryName(null);
            // use the tmp loader for everything
            conf.setClassResolver(new ClassResolver() {
                public ClassLoader getClassLoader(Class context, 
                    ClassLoader env) {
                    return tmpLoader;
                }
            });
            conf.setReadOnly(true);
            conf.instantiateAll();

            MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
            repos.setResolve(MetaDataModes.MODE_MAPPING, false);
            _trans = new PCClassFileTransformer(repos,
                Configurations.parseProperties(props), tmpLoader);
        }

        public byte[] transform(ClassLoader cl, String name,
            Class<?> previousVersion, ProtectionDomain pd, byte[] bytes)
            throws IllegalClassFormatException {
            return _trans.transform(cl, name, previousVersion, pd, bytes);
        }
	}
}
