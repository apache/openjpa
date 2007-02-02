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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Test case to ensure that the proper JPA clear semantics are processed.
 *
 * @author Kevin Sutter
 */
public class TestEntityManagerClear
    extends TestCase {

    private EntityManagerFactory emf;
    private EntityManager em;

    public void setUp() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + AllFieldTypes.class.getName() + ")");
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from AllFieldTypes").executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }
    public void testClear() {
        try {
            // Create EntityManager and Start a transaction (1)
            em = emf.createEntityManager();
            em.getTransaction().begin();

            // Insert a new object and flush
            AllFieldTypes testObject1 = new AllFieldTypes();
            testObject1.setStringField("my test object1");
            em.persist(testObject1);
            em.flush();

            // Clear the PC for new object 2
            AllFieldTypes testObject2 = new AllFieldTypes();
            testObject1.setStringField("my test object2");
            em.persist(testObject2);
            em.clear();

            // Commit the transaction (only object 1 should be in database)
            em.getTransaction().commit();

            // Start a new transaction
            em.getTransaction().begin();

            // Attempt retrieve of Object1 from previous PC (should exist)
            assertEquals(1, em.createQuery
                    ("select x from AllFieldTypes x where x.stringField = 'my test object1'").
                    getResultList().size());

            // Attempt retrieve of Object2 from previous PC (should not exist)
            assertEquals(0, em.createQuery
                    ("select x from AllFieldTypes x where x.stringField = 'my test object2'").
                    getResultList().size());

            // Rollback the transaction and close everything
            em.getTransaction().rollback();
            em.close();
        } catch (Exception ex) {
            fail("Unexpected Exception ex = " + ex);
        }
    }

    public static void main(String[] args) {
        TestRunner.run(TestEntityManagerClear.class);
    }
}

