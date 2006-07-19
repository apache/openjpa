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
package org.apache.openjpa.enhance;

import java.lang.instrument.Instrumentation;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.lib.util.TemporaryClassLoader;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ClassResolver;

/**
 * Java agent that makes persistent classes implement the
 * {@link PersistenceCapable} interface at runtime. The agent is launched
 * at JVM startup from the command line:
 * 
 * <code>java -javaagent:openjpa.jar[=&lt;options&gt;]</code>
 *  The options string should be formatted as a OpenJPA plugin, and may
 * contain any properties understood by the OpenJPA enhancer or any
 * configuration properties. For example:
 * 
 * <code>java -javaagent:openjpa.jar</code>
 *
 * @author Abe White
 */
public class PCEnhancerAgent {

    public static void premain(String args, Instrumentation inst) {
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        Options opts = Configurations.parseProperties(args);
        Configurations.populateConfiguration(conf, opts);
        // don't allow connections
        conf.setConnectionUserName(null);
        conf.setConnectionPassword(null);
        conf.setConnectionURL(null);
        conf.setConnectionDriverName(null);
        conf.setConnectionFactoryName(null);
        // set single class resolver
        final ClassLoader tmpLoader = new TemporaryClassLoader(Thread.
            currentThread().getContextClassLoader());
        conf.setClassResolver(new ClassResolver() {
            public ClassLoader getClassLoader(Class context, ClassLoader env) {
                return tmpLoader;
            }
        });
        conf.setReadOnly(true);
        conf.instantiateAll(); // avoid threading issues

        PCClassFileTransformer transformer = new PCClassFileTransformer
            (new MetaDataRepository(conf), opts, tmpLoader);
        inst.addTransformer(transformer);
    }
}
