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
package org.apache.openjpa.persistence.simple;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Simple test case to get an EntityManager and perform some basic operations.
 *
 * @author Marc Prud'hommeaux
 */
public class TestPersistence
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(AllFieldTypes.class);
    }

    public void testCreateEntityManager() {
        EntityManager em = emf.createEntityManager();

        EntityTransaction t = em.getTransaction();
        assertNotNull(t);
        t.begin();
        t.setRollbackOnly();
        t.rollback();

        // openjpa-facade test
        assertTrue(em instanceof OpenJPAEntityManager);
        OpenJPAEntityManager ojem = (OpenJPAEntityManager) em;
        ojem.getFetchPlan().setMaxFetchDepth(1);
        assertEquals(1, ojem.getFetchPlan().getMaxFetchDepth());
        em.close();
    }

    public void testPersist() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new AllFieldTypes());
        em.getTransaction().commit();
        em.close();
    }

    public void testQuery() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes aft = new AllFieldTypes();
        aft.setStringField("foo");
        aft.setIntField(10);
        em.persist(aft);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        em.getTransaction().begin();
        assertEquals(1, em.createQuery
            ("select x from AllFieldTypes x where x.stringField = 'foo'").
            getResultList().size());
        assertEquals(0, em.createQuery
            ("select x from AllFieldTypes x where x.stringField = 'bar'").
            getResultList().size());
        assertEquals(1, em.createQuery
            ("select x from AllFieldTypes x where x.intField >= 10").
            getResultList().size());
        em.getTransaction().rollback();
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestPersistence.class);
    }
}

