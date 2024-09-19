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

import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.BytecodeWriter;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.test.AbstractCachedEMFTestCase;
import org.apache.openjpa.util.asm.ClassNodeTracker;
import org.apache.openjpa.util.asm.EnhancementClassLoader;
import org.apache.openjpa.util.asm.EnhancementProject;
import org.apache.xbean.asm9.Type;


public class TestEnhancementWithMultiplePUs
    extends AbstractCachedEMFTestCase {

    public void testExplicitEnhancementWithClassNotInFirstPU() throws ClassNotFoundException {
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        Configurations.populateConfiguration(conf, new Options());
        MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
        ClassLoader loader = AccessController
            .doPrivileged(J2DoPrivHelper.newTemporaryClassLoaderAction(
                getClass().getClassLoader()));
        EnhancementProject project = new EnhancementProject();

        String className = "org.apache.openjpa.enhance.UnenhancedBootstrapInstance";
        ClassNodeTracker bc = assertNotPC(loader, project, className);

        PCEnhancer enhancer = new PCEnhancer(conf, bc, repos, loader);
        enhancer.setCreateSubclass(true);

        assertEquals(PCEnhancer.ENHANCE_PC, enhancer.run());

        assertTrue(enhancer.getPCBytecode().getClassNode().interfaces.contains(Type.getInternalName(PersistenceCapable.class)));

        // load the Class<?> for real.
        EnhancementProject finalProject = new EnhancementProject();
        EnhancementClassLoader finalLoader = new EnhancementClassLoader(finalProject, this.getClass().getClassLoader());
        final byte[] classBytes2 = AsmHelper.toByteArray(enhancer.getPCBytecode());

        // this is just to make the ClassLoader aware of the bytecode for the enhanced class
        finalProject.loadClass(classBytes2, finalLoader);

        String pcClassName = enhancer.getPCBytecode().getClassNode().name.replace("/", ".");
        final Class<?> implClass = Class.forName(pcClassName, true, finalLoader);
        assertNotNull(implClass);
    }

    private ClassNodeTracker assertNotPC(ClassLoader loader, EnhancementProject project, String className) {
        ClassNodeTracker bc = project.loadClass(className, loader);
        assertTrue(className + " must not be enhanced already; it was.",
            bc.getClassNode().interfaces == null || !bc.getClassNode().interfaces.contains(Type.getInternalName(PersistenceCapable.class)));
        return bc;
    }

    public void testEnhancementOfSecondPUWithClassNotInFirstPU()
        throws IOException {
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        Options opts = new Options();
        opts.setProperty("p",
            "META-INF/persistence.xml#second-persistence-unit");
        Configurations.populateConfiguration(conf, opts);
        MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
        ClassLoader loader = AccessController
            .doPrivileged(J2DoPrivHelper.newTemporaryClassLoaderAction(
                getClass().getClassLoader()));
        EnhancementProject project = new EnhancementProject();

        // make sure that the class is not already enhanced for some reason
        String className = "org/apache/openjpa/enhance/UnenhancedBootstrapInstance";
        assertNotPC(loader, project, className);

        // build up a writer that just stores to a list so that we don't
        // mutate the disk.
        final List<String> written = new ArrayList<>();
        BytecodeWriter writer = new BytecodeWriter() {

            @Override
            public void write(ClassNodeTracker cnt) throws IOException {
                assertTrue(cnt.getClassNode().interfaces.contains(Type.getInternalName(PersistenceCapable.class)));
                written.add(cnt.getClassNode().name);
            }
        };

        PCEnhancer.run(conf, null, new PCEnhancer.Flags(), repos, writer,
            loader);

        // ensure that we don't attempt to process classes listed in other PUs
        assertEquals(1, written.size());

        // ensure that we do process the classes listed in the PU
        assertTrue(written.contains(className));
    }

    public void testEnhancementOfAllPUsWithinAResource()
        throws IOException {
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        Options opts = new Options();
        opts.setProperty("p", "META-INF/persistence.xml");
        Configurations.populateConfiguration(conf, opts);
        MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
        ClassLoader loader = AccessController
            .doPrivileged(J2DoPrivHelper.newTemporaryClassLoaderAction(
                getClass().getClassLoader()));
        EnhancementProject project = new EnhancementProject();

        // make sure that the classes is not already enhanced for some reason
        assertNotPC(loader, project,
            "org.apache.openjpa.enhance.UnenhancedBootstrapInstance");
        assertNotPC(loader, project,
            "org.apache.openjpa.enhance.UnenhancedBootstrapInstance2");

        // build up a writer that just stores to a list so that we don't
        // mutate the disk.
        final List<String> written = new ArrayList<>();
        BytecodeWriter writer = new BytecodeWriter() {

            @Override
            public void write(ClassNodeTracker cnt) throws IOException {
                assertTrue(cnt.getClassNode().interfaces.contains(Type.getInternalName(PersistenceCapable.class)));
                written.add(cnt.getClassNode().name);
            }
        };

        opts = new Options();
        // Use a restricted mdr.  This mdr will not hand out metadata for excluded
        // types.  These are types that have known issues and should not be enhanced.
        // This test tries to enhance all persistent types in the classpath and that
        // can be problematic for tests which include entities that this test should
        // not attempt to enhance.
        opts.setProperty("MetaDataRepository",
            "org.apache.openjpa.enhance.RestrictedMetaDataRepository(excludedTypes=" +
            "\"org.apache.openjpa.persistence.jdbc.annotations.UnenhancedMixedAccess," +
            "org.apache.openjpa.idtool.RecordsPerYear\")");
        opts.put(PCEnhancer.class.getName() + "#bytecodeWriter", writer);
        PCEnhancer.run(null, opts);

        // ensure that we do process the classes listed in the PUs
        assertTrue(written.contains(
            "org/apache/openjpa/enhance/UnenhancedBootstrapInstance"));
        assertTrue(written.contains(
            "org/apache/openjpa/enhance/UnenhancedBootstrapInstance2"));
    }
}
