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
package org.apache.openjpa.persistence.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.simple.NamedEntity;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test that we can query by an entity's abstract schema name.
 *
 * @author Abe White
 */
public class TestAbstractSchemaName
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(NamedEntity.class);

        NamedEntity e = new NamedEntity();
        e.setName("e"); 

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e);
        em.getTransaction().commit();
        em.close();
    }

    public void testQuery() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("select e from named e");
        NamedEntity e = (NamedEntity) q.getSingleResult();
        assertNotNull(e);
        assertEquals("e", e.getName());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestAbstractSchemaName.class);
    }
}

