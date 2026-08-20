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
package org.apache.openjpa.persistence.simple;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests that EntityManager.close() does not throw when a resource-local
 * transaction is active.
 *
 * Per JPA 3.2 spec Section 7.7: close() with an active resource-local
 * transaction should not throw. The active transaction is rolled back
 * and the entity manager is closed.
 */
public class TestEntityManagerCloseWithActiveTransaction {

    @Test
    public void testCloseWithActiveTransactionDoesNotThrow() {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + AllFieldTypes.class.getName() + ")");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
            "test", props);
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            // Per JPA spec, close() with active transaction should NOT throw.
            // Close is deferred until the transaction completes.
            em.close();

            // Transaction is still active (close is deferred)
            assertTrue("Transaction should still be active after deferred close",
                em.getTransaction().isActive());

            // Roll back to complete the transaction and allow deferred close
            em.getTransaction().rollback();
        } finally {
            emf.close();
        }
    }

    @Test
    public void testMethodsThrowAfterCloseWithActiveTransaction() {
        Map<String, Object> props = new HashMap<>();
        props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + AllFieldTypes.class.getName() + ")");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
            "test", props);
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            em.close();

            // After close invoked, most EM methods should throw IllegalStateException
            try {
                em.persist(new AllFieldTypes());
                fail("persist() should throw IllegalStateException after close");
            } catch (IllegalStateException expected) {
                // correct
            }

            try {
                em.find(AllFieldTypes.class, 1);
                fail("find() should throw IllegalStateException after close");
            } catch (IllegalStateException expected) {
                // correct
            }

            // getTransaction() should still work after close (per spec)
            assertNotNull("getTransaction() should work after close",
                em.getTransaction());

            // Roll back to complete the transaction and allow deferred close
            em.getTransaction().rollback();
        } finally {
            emf.close();
        }
    }
}
