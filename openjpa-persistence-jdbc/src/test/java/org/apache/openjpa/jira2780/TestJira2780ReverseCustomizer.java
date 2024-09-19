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
package org.apache.openjpa.jira2780;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ReverseMappingTool;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests the added useSchemaElement functionality of the
 * ReverseMappingTool and CodeGenerator classes.
 *
 * @author Austin Dorenkamp (ajdorenk)
 */
public class TestJira2780ReverseCustomizer extends SingleEMFTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        File f = new File("target/orm.xml");

        // Make sure to clean up orm.xml from a prior run
        if (f.exists()) {
            assertTrue(f.delete());
        }
        setSupportedDatabases(org.apache.openjpa.jdbc.sql.DerbyDictionary.class);
    }

    @Override
    public String getPersistenceUnitName(){
        return "rev-mapping-jira2780-pu";
    }

    public void testGettersAndSetters() throws Exception {

        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();

        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        Query q = em.createNativeQuery("CREATE TABLE JIRA2780.ABC (ID INTEGER PRIMARY KEY, TEST_ENUM VARCHAR(1))");
        try {
            q.executeUpdate();
            em.getTransaction().commit();
        } catch (Throwable t) {
            em.getTransaction().rollback();
            System.out.println(t.toString());
        }

        final String clsName = "Abc";
        ReverseMappingTool.Flags flags = new ReverseMappingTool.Flags();
        flags.metaDataLevel = "none";
        flags.generateAnnotations = true;
        flags.packageName = getClass().getPackage().getName();
        flags.directory = Files.getFile("./target", null);
        flags.customizer = new Jira2780ReverseCustomizer();
        Properties customProps = new Properties();
        customProps.put(flags.packageName + "." + clsName + ".testEnum.type"
            , Jira2780Enum.class.getName());
        flags.customizer.setConfiguration(customProps);
        ReverseMappingTool.run(conf, new String[0], flags, null);

        /* Now that the tool has been run, we will test it by reading the generated files */
        File abc = new File(Files.getPackageFile(flags.directory, flags.packageName, false)
            , clsName + ".java");
        String currLine = null, prevLine;
        try (Scanner inFile = new Scanner(abc)) {
            while (inFile.hasNextLine()) {
                prevLine = currLine;
                currLine = inFile.nextLine();
                if (currLine.isEmpty() || !currLine.contains("Jira2780Enum testEnum")) {
                    continue;
                }
                if (prevLine.contains("@Enumerated(EnumType.STRING)")) {
                    break;
                } else {
                    fail("@Enumerated annotation has not been injected");
                }
            }
        } catch (FileNotFoundException e) {
            fail(clsName + ".java not generated under ./target by ReverseMappingTool");
        }

        // Delete file to clean up workspace
        assertTrue(abc.delete());
    }
}
