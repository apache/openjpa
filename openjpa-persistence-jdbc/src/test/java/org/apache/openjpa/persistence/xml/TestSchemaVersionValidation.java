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
package org.apache.openjpa.persistence.xml;

import java.util.MissingResourceException;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import org.apache.openjpa.persistence.ViewStream;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * These test cases come from the branch of OpenJPA that implements JPA 2.0.
 * The purpose here is simply to ensure that no 2.0 persistence.xml will fail
 * to be detected.
 */
public class TestSchemaVersionValidation extends TestCase {
    
    private ViewStream viewStream;
    
    @Override
    protected void setUp() throws Exception {
        System.setErr(viewStream = new ViewStream(System.err));
    }

    @Override
    protected void tearDown() throws Exception {
        if (viewStream != null)
            System.setErr(viewStream.getOriginal());
    }

    /**
     * Verify that a null will be returned and an informative error message will be
     * issued when attempting to read a Version 2.0 persistence.xml document
     */
    public void test2_0PersistenceXml() {
        OpenJPAEntityManagerFactory emf = null;
        try {
            emf = OpenJPAPersistence.createEntityManagerFactory("XSDTest", 
                    "org/apache/openjpa/persistence/xml/persistence-2_0.xml");
            fail("Did not throw exception");
        } catch (MissingResourceException e) {
            // expected
        } finally {
            if (emf != null)
                emf.close();
        }
        
        assertTrue("Did not find expected warning in System.err print stream", 
                viewStream.contains("This version of OpenJPA cannot read a " +
                "persistence.xml document with a version different from \"1.0\""));
    }

    /**
     * Verify that we will get back a null EMF if we attempt to load a
     * 2.0 ORM within a 1.0 persistence.xml.
     */
    public void test1_0Persistence2_0OrmXml() {
        OpenJPAEntityManagerFactory emf = null;
        
        try {
            emf = OpenJPAPersistence.createEntityManagerFactory("XSDTest", 
                "org/apache/openjpa/persistence/xml/persistence-2_0-orm-1_0.xml");
            assertNull(emf);
        } finally {
            if (emf != null)
                emf.close();
        }
    }

    /**
     * Verify that a persistence.xml document without a version tag will be flagged
     * as non-conforming.
     */
    public void testPersistenceWithoutVersion() {
        OpenJPAEntityManagerFactory emf = null;
        try {
            emf = OpenJPAPersistence.createEntityManagerFactory("XSDTest", 
                    "org/apache/openjpa/persistence/xml/persistence-no-version.xml");
        } catch (MissingResourceException e) {
            // expected
        } finally {
            if (emf != null)
                emf.close();
        }
        
        assertTrue("Did not find expected warning in error stream", 
                viewStream.contains("This version of OpenJPA cannot read a " +
                "persistence.xml document with a version different from \"1.0\""));
    }
}
