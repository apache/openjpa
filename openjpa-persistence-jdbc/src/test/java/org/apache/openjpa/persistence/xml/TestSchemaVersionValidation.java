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

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.util.GeneralException;
import org.junit.Test;

public class TestSchemaVersionValidation {
    /**
     * Verify a pu can be created with a version 2.0 persistence.xml
     */
    @Test
    public void test2_0PersistenceXml() {
        doCreateEmf("XSDTest", "persistence-2_0.xml", false);
    }

    /**
     * Verify schema validation will fail when using a 2.0
     * persistence.xml that does not contain a persistence unit
     * (the 2.0 spec made it a requirement for the persistence file
     * to contain at least one pu.)
     */
    // JREs fail differently for this test. Detection and
    // assertion of a JRE specific failure has shown to be error prone
    // so only a generic exception is detected.
    @Test(expected = Exception.class)
    public void testBad2_0PersistenceXml() {
        doCreateEmf(null, "persistence-2_0-no-pu.xml", false);
    }

    /**
     * Verify a version 2.0 persistence.xml can reference and the provider
     * can parse a version 1.0 orm.xml
     */
    @Test
    public void test2_0Persistence1_0OrmXml() {
        doCreateEmf("XSDTest", "persistence-2_0-orm-1_0.xml");
    }

    /**
     * Verify a version 2.0 persistence.xml can reference and the provider can
     * parse a version 2.0 orm.xml
     */
    @Test
    public void test2_0Persistence2_0OrmXml() {
        doCreateEmf("XSDTest", "persistence-2_0-orm-2_0.xml");
    }

    /**
     * Verify a 1.0 persistence.xml can include a 2.0 orm.xml
     */
    @Test
    public void test1_0Persistence2_0OrmXml() {
        doCreateEmf("XSDTest", "persistence-2_0-orm-1_0.xml");
    }

    /**
     * Verify a version 66.6 persistence.xml with bad URL cannot be loaded
     */
    @Test(expected = GeneralException.class)
    public void testOffline66_6Persistence2_2OrmXml() {
        doCreateEmf("XSDTest", "offline_persistence-66_6-orm-2_2.xml");
    }

    /**
     * Verify a version 2.1 persistence.xml with bad URL can reference and the provider can
     * parse a version 2.2 orm.xml with bad URL 
     */
    @Test
    public void testOffline2_1Persistence2_2OrmXml() {
        doCreateEmf("XSDTest", "offline_persistence-2_1-orm-2_2.xml");
    }

    private void doCreateEmf(String name, String xml) {
        doCreateEmf(name, xml, true);
    }

    private void doCreateEmf(String name, String xml, boolean createEm) {
        OpenJPAEntityManagerFactory emf = OpenJPAPersistence.
                createEntityManagerFactory(name, "org/apache/openjpa/persistence/xml/" + xml);
        if (createEm) {
            OpenJPAEntityManager em = emf.createEntityManager();
            em.close();
        }
        emf.close();
    }
}
