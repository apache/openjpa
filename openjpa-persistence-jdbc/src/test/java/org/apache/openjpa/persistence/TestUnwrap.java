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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.lib.jdbc.DelegatingConnection;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestUnwrap extends SingleEMFTestCase {

    /**
     * Tests a query can be unwrapped as an instance of a series of class or
     * interface.
     */
    public void testValidQueryUnwrap() {
        OpenJPAEntityManager em = emf.createEntityManager();
        Query query = em.createQuery(QueryLanguages.LANG_SQL,"");

        Class[] validCasts = new Class[] {
            org.apache.openjpa.persistence.OpenJPAQuery.class,
            org.apache.openjpa.persistence.OpenJPAQuerySPI.class,
            org.apache.openjpa.kernel.DelegatingQuery.class,
            org.apache.openjpa.kernel.Query.class,
            org.apache.openjpa.kernel.QueryImpl.class
        };
        for (Class<?> c : validCasts) {
            Object unwrapped = query.unwrap(c);
            assertTrue(c.isInstance(unwrapped));
        }
        em.close();
    }

    /**
     * Tests a EntityManager can be unwrapped as an instance of a series of
     * class or interface.
     */
    public void testValidEntityManagerUnwrap() {
        EntityManager em = emf.createEntityManager();

        Class<?>[] validCasts = new Class[] {
            org.apache.openjpa.persistence.OpenJPAEntityManager.class,
            org.apache.openjpa.persistence.OpenJPAEntityManagerSPI.class,
            org.apache.openjpa.kernel.DelegatingBroker.class,
            org.apache.openjpa.kernel.Broker.class
        };
        for (Class<?> c : validCasts) {
            Object unwrapped = em.unwrap(c);
            assertTrue(c.isInstance(unwrapped));
        }
        em.close();
    }

    /**
     * Tests a EntityManager can be unwrapped as an instance of a series of
     * class or interface.
     */
    public void testValidOtherUnwrap() {
        EntityManager em = emf.createEntityManager();

        Class<?>[] validCasts = new Class[] {
            java.sql.Connection.class
        };
        for (Class<?> c : validCasts) {
            Object unwrapped = em.unwrap(c);
            assertTrue(c.isInstance(unwrapped));
        }

        em.close();
    }

    public void testConnectionUnwrap() throws Exception {
        String dbDict = ((JDBCConfiguration) emf.getConfiguration()).getDBDictionaryInstance().getClass().getName();

        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = em.unwrap(OpenJPAEntityManager.class);
        try {
            Connection c = (Connection) oem.getConnection();
            assertNotNull(c);
            assertTrue(DelegatingConnection.class.isAssignableFrom(c.getClass()));

            List<Class> acceptedConnectionClassTypes = new ArrayList<>();
            if (DerbyDictionary.class.getName().equals(dbDict)) {
                // Connection type can be network or embedded
                String[] connectionTypes = {
                    "org.apache.derby.impl.jdbc.EmbedConnection40",
                    "org.apache.derby.impl.jdbc.EmbedConnection30",
                    "org.apache.derby.iapi.jdbc.BrokeredConnection40",
                    "org.apache.derby.iapi.jdbc.BrokeredConnection30" };
                for (String ct : connectionTypes) {
                    try {
                        Class cls = Class.forName(ct);
                        acceptedConnectionClassTypes.add(cls);
                    } catch (ClassNotFoundException cnfe) {
                        // Swallow
                    }
                }
            }

            if (!acceptedConnectionClassTypes.isEmpty()) {
                boolean pass = false;
                for (Class cls : acceptedConnectionClassTypes) {
                    try {
                        Connection castC = (Connection) c.unwrap(cls);
                        assertNotNull(castC);
                        assertEquals(cls, castC.getClass());
                        pass = true;
                        break;
                    } catch (Throwable t) {
                        // Swallow
                    }

                   assertTrue(pass);
                }
            }
        } finally {
            em.close();
        }
    }

    public void testNegativeConnectionUnwrap() {
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = em.unwrap(OpenJPAEntityManager.class);

        try {
            Connection c = (Connection) oem.getConnection();
            assertNotNull(c);
            assertTrue(DelegatingConnection.class.isAssignableFrom(c.getClass()));

            // Make a completely bogus unwrap() attempt
            try {
                c.unwrap(TestUnwrap.class);
                fail("Bogus unwrap should have thrown a SQLException.");
            } catch (java.sql.SQLException se) {
                // Expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * Tests a EntityManager can not be unwrapped as Object class, null or an interface.
     * And each such failure raises a Persistence Exception and causes an active transaction
     * to rollback.
     */
    public void testInvalidEntityManagerUnwrap() {
        EntityManager em = emf.createEntityManager();

        Class<?>[] invalidCasts = new Class[] {
            Object.class,
            Properties.class,
            Map.class,
            null,
        };
        for (Class<?> c : invalidCasts) {
            try {
                em.getTransaction().begin();
                em.unwrap(c);
                fail("Expected to fail to unwarp with invalid " + c);
            } catch (PersistenceException e) {
                EntityTransaction txn = em.getTransaction();
                assertTrue(txn.getRollbackOnly());
                txn.rollback();
            }
        }
        em.close();
    }

    /**
     * Tests a Query can not be unwrapped as Object class, null or an interface.
     * And each such failure raises a Persistence Exception and causes an active transaction
     * to rollback.
     */
    public void testInvalidQueryUnwrap() {
        OpenJPAEntityManager em = emf.createEntityManager();

        Class<?>[] invalidCasts = new Class[] {
            Object.class,
            Properties.class,
            Map.class,
            null,
        };
        for (Class<?> c : invalidCasts) {
            try {
                em.getTransaction().begin();
                Query query = em.createQuery(QueryLanguages.LANG_SQL,"");
                query.unwrap(c);
                fail("Expected to fail to unwarp with invalid " + c);
            } catch (PersistenceException e) {
                EntityTransaction txn = em.getTransaction();
                assertTrue(txn.getRollbackOnly());
                txn.rollback();
            }
        }
        em.close();
    }

}
