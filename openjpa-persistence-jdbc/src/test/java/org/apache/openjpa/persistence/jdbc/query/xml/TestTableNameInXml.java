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
package org.apache.openjpa.persistence.jdbc.query.xml;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestTableNameInXml extends SQLListenerTestCase {

    String containsSQL = " FROM TableNameInXml ";
    String notContainsSQL = " FROM TableNameInXmlEntity ";

    @Override
    public void setUp() {
        super.setUp(TableNameInXmlEntity.class);
    }

    /*
     * The SQL generated in this test should contain "FROM TableNameInXml" since the table name is defined in XML.
     */
    public void testQuery() {
        EntityManager em = emf.createEntityManager();

        Query q = em.createQuery("SELECT t FROM TableNameInXmlEntity t");
        q.getResultList();
        assertContainsSQL(containsSQL);
        assertNotSQL(notContainsSQL);

        em.close();
    }

    /*
     * The SQL generated in this test should contain "FROM TableNameInXml" since the table name is defined in XML. Prior
     * to OPENJPA-2533, the table name in XML was not being picked up.
     */
    public void testNamedQuery() {
        EntityManager em = emf.createEntityManager();

        Query q = em.createNamedQuery("TableNameInXmlEntity.findAll");
        q.getResultList();
        assertContainsSQL(containsSQL);
        assertNotSQL(notContainsSQL);

        em.close();
    }

    /*
     * The SQL generated in this test should contain "FROM TableNameInXml" since the table name is defined in XML. This
     * test works because the named query is executed second.
     */
    public void testBoth() {
        EntityManager em = emf.createEntityManager();

        Query q = em.createQuery("SELECT t FROM TableNameInXmlEntity t");
        q.getResultList();

        q = em.createNamedQuery("TableNameInXmlEntity.findAll");
        q.getResultList();
        assertContainsSQL(containsSQL);
        assertNotSQL(notContainsSQL);

        em.close();
    }

    @Override
    protected String getPersistenceUnitName() {
        return "TableNameInXml-PU";
    }
}
