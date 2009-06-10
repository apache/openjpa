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
package org.apache.openjpa.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.openjpa.lib.util.J2DoPrivHelper;

import junit.framework.TestCase;

public class TestPersistenceProductDerivation extends TestCase {
    private File sourceFile;
    private File targetFile;
    
    ClassLoader originalLoader = null;
    ClassLoader tempLoader = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        String currentDir = System.getProperty("user.dir");
        
        // openjpa-persistence/src/test/resources/second-persistence/
        //   META-INF/persistence.xml
        sourceFile = new File(currentDir + File.separator + "src" 
            + File.separator  + "test" + File.separator + "resources"
            + File.separator  + "second-persistence" + File.separator 
            + "META-INF" + File.separator + "persistence.xml");
        
        // openjpa-persistence/target/test-classes/
        //   TestPersistenceProductDerivation_generated_(time_stamp).jar        
        targetFile = new File(currentDir + File.separator + "target" + 
            File.separator + "test-classes" + File.separator + 
            "TestPersistenceProductDerivation_generated_" +
            System.currentTimeMillis() + ".jar");
        
        AccessController.doPrivileged(J2DoPrivHelper
            .deleteOnExitAction(targetFile));
        buildJar(sourceFile,targetFile);
        
        // Hold a reference to the current classloader so we can cleanup
        // when we're done.
        originalLoader = Thread.currentThread().getContextClassLoader();
        tempLoader = new TempUrlLoader(new URL[]{targetFile.toURI().toURL()}
            ,originalLoader);        
        AccessController.doPrivileged(J2DoPrivHelper
            .setContextClassLoaderAction(tempLoader));
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        // Restore the original classloader.
        Thread.currentThread().setContextClassLoader(originalLoader);
        
        // For whatever reason, this file won't ever delete. I searched around
        // and found numerous documented problems with deleting files. Perhaps
        // sometime in the future this problem will be fixed. For now it doesn't
        // really matter since we generate a new file every time.
        boolean deleted = AccessController.doPrivileged(J2DoPrivHelper
            .deleteAction(targetFile));
        if(deleted==false){
            System.out.println("The file " + targetFile + " wasn't deleted.");
        }
    }
    /**
     * Added for OPENJPA-932. Verifies a ppd properly loads pu's from multiple 
     * archives.
     * 
     * @throws Exception
     */
    public void testGetAnchorsInResource()throws Exception {
        
        List<String> expectedPUs = Arrays.asList(
            new String[]{"pu_1","pu_2","pu_3"});
        
        PersistenceProductDerivation ppd = new PersistenceProductDerivation();
        List actual = ppd.getAnchorsInResource("META-INF/persistence.xml");
        
        assertEquals(expectedPUs, actual);        
    }
    
    private void buildJar(File sourceFile, File targetFile) throws Exception {
        
        JarOutputStream out = new JarOutputStream(
            new BufferedOutputStream(new FileOutputStream(targetFile)));
        
        BufferedInputStream in = 
            new BufferedInputStream(new FileInputStream(sourceFile));

        out.putNextEntry(new JarEntry("META-INF/"));
        out.putNextEntry(new JarEntry("META-INF/persistence.xml"));
        //write the xml to the jar
        byte[] buf = new byte[1024];
        int i;
        while ((i = in.read(buf)) != -1) {
          out.write(buf, 0, i);
        }
        
        out.close();
        in.close();        
    }

    class TempUrlLoader extends URLClassLoader {
        public TempUrlLoader(URL[] urls, ClassLoader parent) {
            super(urls,parent);
        }
    }
}
