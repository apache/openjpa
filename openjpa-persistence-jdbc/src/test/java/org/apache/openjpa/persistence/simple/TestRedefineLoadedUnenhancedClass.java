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
package org.apache.openjpa.persistence.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.TransformerException;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.InstrumentationFactory;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.lib.util.TemporaryClassLoader;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;
import org.junit.jupiter.api.Test;

/**
 * Tests runtime enhancement of an entity class that is already loaded, and
 * not enhanced, when the EntityManagerFactory starts. The class transformer of
 * the persistence unit is registered with the instrumentation the way a
 * container does it. Redefining the loaded class must not hand the class to
 * that transformer, which would add the PersistenceCapable interface and make
 * the JVM reject the redefinition.
 *
 * Redefinition needs an instrumentation, which is obtained through self-attach
 * with -Djdk.attach.allowAttachSelf=true, so the test runs in a child JVM and
 * the test JVM is not changed.
 */
class TestRedefineLoadedUnenhancedClass {

    @Test
    void redefineClassLoadedBeforeEntityManagerFactory() throws IOException, InterruptedException {
        assumeTrue(ModuleLayer.boot().findModule("jdk.attach").isPresent());

        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-Djdk.attach.allowAttachSelf=true");
        cmd.add("-XX:+EnableDynamicAgentLoading");
        // the database of the build and the location of derby.log are passed to the test JVM as system properties
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith("openjpa.Connection") || name.equals("openjpa.jdbc.DBDictionary")
                || name.startsWith("derby.")) {
                cmd.add("-D" + name + "=" + System.getProperty(name));
            }
        }
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(Main.class.getName());

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(120, TimeUnit.SECONDS), "child JVM did not finish");
        assertEquals(0, p.exitValue(), out);
        assertTrue(out.contains(Main.SUCCESS), out);
        // ClassRedefiner logs a failed redefinition and continues with the subclass only
        assertFalse(out.contains("falling back to subclass enhancement"), out);
    }

    /**
     * Entry point of the child JVM started by
     * {@link #redefineClassLoadedBeforeEntityManagerFactory()}.
     */
    public static class Main {
        static final String SUCCESS = "loaded entity class redefined";

        public static void main(String[] args) throws Exception {
            // load the entity class before the persistence unit is created
            Class<?> cls = Class.forName(UnenhancedRedefinedMovie.class.getName());
            if (PersistenceCapable.class.isAssignableFrom(cls)) {
                System.out.println("entity class was enhanced at build time");
                System.exit(1);
            }

            List<ClassTransformer> transformers = new ArrayList<>();
            PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl() {
                @Override
                public void addTransformer(ClassTransformer transformer) {
                    transformers.add(transformer);
                }

                @Override
                public ClassLoader getNewTempClassLoader() {
                    // like a container, load only the application classes in the temporary loader
                    return new TemporaryClassLoader(Main.class.getClassLoader()) {
                        @Override
                        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                            if (!name.startsWith(Main.class.getPackage().getName() + ".")) {
                                return getParent().loadClass(name);
                            }
                            return super.loadClass(name, resolve);
                        }
                    };
                }
            };
            pui.setPersistenceUnitName("redefine-loaded-unenhanced");
            pui.addManagedClassName(cls.getName());
            pui.setExcludeUnlistedClasses(true);

            Map<String, Object> props = new HashMap<>();
            // use the database of the build, or an in-memory Derby database when run without it
            for (String name : System.getProperties().stringPropertyNames()) {
                if (name.startsWith("openjpa.Connection") || name.equals("openjpa.jdbc.DBDictionary")) {
                    String value = System.getProperty(name);
                    if (!value.isEmpty() && !value.startsWith("${")) {
                        props.put(name, value);
                    }
                }
            }
            if (!props.containsKey("openjpa.ConnectionURL")) {
                props.put("openjpa.ConnectionDriverName", "org.apache.derby.jdbc.EmbeddedDriver");
                props.put("openjpa.ConnectionURL", "jdbc:derby:memory:openjpa-redefine-loaded;create=true");
            }
            // the database of the build is shared with other tests and kept between runs
            props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(SchemaAction='add,deleteTableContents')");
            props.put("openjpa.RuntimeUnenhancedClasses", "supported");
            props.put("openjpa.DynamicEnhancementAgent", "false");
            props.put("openjpa.Log", "DefaultLevel=WARN, Enhance=INFO");

            EntityManagerFactory emf = new PersistenceProviderImpl().createContainerEntityManagerFactory(pui, props);

            // register the class transformers of the persistence unit like a container does
            OpenJPAConfiguration conf = OpenJPAPersistence.cast(emf).getConfiguration();
            Instrumentation inst = InstrumentationFactory.getInstrumentation(conf.getLog(OpenJPAConfiguration.LOG_ENHANCE));
            if (inst == null) {
                System.out.println("no instrumentation available");
                System.exit(1);
            }
            if (transformers.isEmpty()) {
                System.out.println("no class transformer registered");
                System.exit(1);
            }
            for (ClassTransformer transformer : transformers) {
                inst.addTransformer(new ClassFileTransformer() {
                    @Override
                    public byte[] transform(ClassLoader loader, String className, Class<?> redef,
                        ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
                        try {
                            return transformer.transform(loader, className, redef, domain, bytes);
                        } catch (TransformerException e) {
                            throw new IllegalClassFormatException(e.getMessage());
                        }
                    }
                });
            }

            try {
                // the persistent types are loaded and redefined here
                EntityManager em = emf.createEntityManager();
                em.getTransaction().begin();
                UnenhancedRedefinedMovie movie = new UnenhancedRedefinedMovie(1, "Reservoir Dogs", 1992);
                em.persist(movie);
                em.getTransaction().commit();

                em.getTransaction().begin();
                movie.setTitle("Pulp Fiction");
                movie.setYear(1994);
                em.getTransaction().commit();
                em.close();

                em = emf.createEntityManager();
                UnenhancedRedefinedMovie found = em.find(UnenhancedRedefinedMovie.class, 1L);
                if (found == null || !"Pulp Fiction".equals(found.getTitle()) || found.getYear() != 1994) {
                    System.out.println("change of the managed instance was not stored: "
                        + (found == null ? null : found.getTitle() + " " + found.getYear()));
                    System.exit(1);
                }
                em.close();
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                System.exit(1);
            } finally {
                emf.close();
            }
            if (PersistenceCapable.class.isAssignableFrom(cls)) {
                System.out.println("PersistenceCapable was added to the loaded entity class");
                System.exit(1);
            }
            System.out.println(SUCCESS);
        }
    }
}
