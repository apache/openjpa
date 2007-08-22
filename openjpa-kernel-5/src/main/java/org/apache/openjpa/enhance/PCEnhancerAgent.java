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
package org.apache.openjpa.enhance;

import java.lang.instrument.Instrumentation;
import java.security.AccessController;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.util.ClassResolver;

/**
 * <p>Java agent that makes persistent classes work with OpenJPA at runtime.
 * This is achieved by either running the enhancer on the classes as they
 * are loaded, or by redefining the classes on the fly.
 * The agent is launched at JVM startup from the command line:</p>
 *
 * <p><code>java -javaagent:openjpa.jar[=&lt;options&gt;]</code>
 *  The options string should be formatted as a OpenJPA plugin, and may
 * contain any properties understood by the OpenJPA enhancer or any
 * configuration properties. For example:</p>
 *
 * <p><code>java -javaagent:openjpa.jar</code></p>
 *
 * <p>By default, if specified, the agent runs the OpenJPA enhancer on
 * all classes listed in the first persistence unit as they are loaded,
 * and redefines all other persistent classes when they are encountered.
 * To disable enhancement at class-load time and rely solely on the
 * redefinition logic, set the ClassLoadEnhancement flag to false. To
 * disable redefinition and rely solely on pre-deployment or class-load
 * enhancement, set the RuntimeRedefinition flag to false.
 * </p>
 *
 * <p><code>java -javaagent:openjpa.jar=ClassLoadEnhancement=false</code></p>
 *
 * @author Abe White
 * @author Patrick Linskey
 */
public class PCEnhancerAgent {

    public static void premain(String args, Instrumentation inst) {
        Options opts = Configurations.parseProperties(args);

        if (opts.getBooleanProperty(
            "ClassLoadEnhancement", "classLoadEnhancement", true))
            registerClassLoadEnhancer(inst, opts);

        // Deprecated property setting
        if (opts.getBooleanProperty(
            "RuntimeEnhancement", "runtimeEnhancement", true))
            registerClassLoadEnhancer(inst, opts);

        if (opts.getBooleanProperty(
            "RuntimeRedefinition", "runtimeRedefinition", true)) {
            InstrumentationFactory.setInstrumentation(inst);
        } else {
            InstrumentationFactory.setDynamicallyInstallAgent(false);
        }
    }

    private static void registerClassLoadEnhancer(Instrumentation inst,
        Options opts) {
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        Configurations.populateConfiguration(conf, opts);
        // don't allow connections
        conf.setConnectionUserName(null);
        conf.setConnectionPassword(null);
        conf.setConnectionURL(null);
        conf.setConnectionDriverName(null);
        conf.setConnectionFactoryName(null);
        // set single class resolver
        final ClassLoader tmpLoader = (ClassLoader) AccessController
            .doPrivileged(J2DoPrivHelper
                .newTemporaryClassLoaderAction((ClassLoader) AccessController
                    .doPrivileged(J2DoPrivHelper.getContextClassLoaderAction())
                    ));
        conf.setClassResolver(new ClassResolver() {
            public ClassLoader getClassLoader(Class context, ClassLoader env) {
                return tmpLoader;
            }
        });
        conf.setReadOnly(true);
        conf.instantiateAll(); // avoid threading issues

        PCClassFileTransformer transformer = new PCClassFileTransformer
            (conf.newMetaDataRepositoryInstance(), opts, tmpLoader);
        inst.addTransformer(transformer);
    }
}
