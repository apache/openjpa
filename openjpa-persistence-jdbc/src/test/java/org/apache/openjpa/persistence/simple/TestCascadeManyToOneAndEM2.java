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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import org.apache.openjpa.persistence.EntityManagerImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for TCK failures:
 * - cascadeAllMX1Test2: re-persist removed entity with cascade ManyToOne
 * - persistMX1Test2: re-persist removed entity with cascade PERSIST ManyToOne
 * - getReferenceForNonExistingEntityTest: getReference(entity) for non-existing entity
 * - runWithConnectionTest / callWithConnectionTest: JPA 3.2 connection methods
 */
public class TestCascadeManyToOneAndEM2 extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CascadeAEntity.class, CascadeBAllEntity.class, CascadeBPersistEntity.class,
              SimpleOrderEntity.class, CLEAR_TABLES);
    }

    /**
     * TCK: cascadeAllMX1Test2
     * Persist B with cascade=ALL ManyToOne to A, remove both, re-persist B.
     * The cascade should re-persist A, and b.getA() should not be null.
     */
    public void testCascadeAllMX1RePersistRemoved() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            CascadeAEntity aRef = new CascadeAEntity("2", "bean2", 2);
            CascadeBAllEntity b1 = new CascadeBAllEntity("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            CascadeAEntity newA1 = b1.getA1();
            assertNotNull("A should be persisted via cascade", newA1);

            // Remove both entities
            em.remove(newA1);
            em.remove(b1);
            em.flush();

            // Verify B is gone
            CascadeBAllEntity found = em.find(CascadeBAllEntity.class, "2");
            assertNull("B should be removed", found);

            // Re-persist the removed B; cascade should re-persist A
            em.persist(b1);
            em.flush();

            assertTrue("B should be managed", em.contains(b1));
            assertNotNull("B's ManyToOne A should not be null after re-persist", b1.getA1());

            em.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException("testCascadeAllMX1RePersistRemoved failed", e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: persistMX1Test2
     * Same as above but with cascade=PERSIST only.
     */
    public void testPersistMX1RePersistRemoved() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            CascadeAEntity aRef = new CascadeAEntity("2", "bean2", 2);
            CascadeBPersistEntity b1 = new CascadeBPersistEntity("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            CascadeAEntity newA1 = b1.getA1();
            assertNotNull("A should be persisted via cascade", newA1);

            // Remove A and B
            em.remove(newA1);
            em.remove(em.find(CascadeBPersistEntity.class, "2"));
            em.flush();

            // Verify B is removed
            CascadeBPersistEntity found = em.find(CascadeBPersistEntity.class, "2");
            assertNull("B should be removed", found);

            // Re-persist B; cascade=PERSIST should re-persist A
            em.persist(b1);
            em.flush();

            assertTrue("B should be managed", em.contains(b1));
            assertNotNull("B's ManyToOne A should not be null after re-persist", b1.getA1());

            em.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException("testPersistMX1RePersistRemoved failed", e);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: getReferenceForNonExistingEntityTest
     * getReference(entity) for non-existing entity should throw EntityNotFoundException
     * when the proxy is accessed.
     */
    public void testGetReferenceForNonExistingEntity() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            SimpleOrderEntity order = new SimpleOrderEntity(99999, 0, "nonexistent");
            // getReference(entity) should return a proxy
            SimpleOrderEntity ref = em.getReference(order);
            // Accessing the proxy should throw EntityNotFoundException
            try {
                String desc = ref.getDescription();
                fail("Expected EntityNotFoundException when accessing non-existing entity reference");
            } catch (EntityNotFoundException e) {
                // expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * TCK: runWithConnectionTest
     * JPA 3.2 runWithConnection should provide a working JDBC connection.
     */
    public void testRunWithConnection() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Insert a row directly via JDBC connection
            em.<Connection>runWithConnection(connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO SIMPLE_ORDER(ID, TOTAL, DESCRIPTION) VALUES(?, ?, ?)")) {
                    stmt.setInt(1, 50);
                    stmt.setInt(2, 5555);
                    stmt.setString(3, "desc55");
                    stmt.executeUpdate();
                }
            });

            em.getTransaction().commit();

            // Verify the row was persisted
            SimpleOrderEntity found = em.find(SimpleOrderEntity.class, 50);
            assertNotNull("Order should be found after runWithConnection insert", found);
            assertEquals(50, found.getId());
            assertEquals(5555, found.getTotal());
            assertEquals("desc55", found.getDescription());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: callWithConnectionTest
     * JPA 3.2 callWithConnection should provide a working JDBC connection and return a value.
     */
    public void testCallWithConnection() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            em.getTransaction().begin();

            SimpleOrderEntity selectedOrder = em.<Connection, SimpleOrderEntity>callWithConnection(connection -> {
                // Insert a row
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO SIMPLE_ORDER(ID, TOTAL, DESCRIPTION) VALUES(?, ?, ?)")) {
                    stmt.setInt(1, 60);
                    stmt.setInt(2, 6666);
                    stmt.setString(3, "desc66");
                    stmt.executeUpdate();
                }
                // Read it back
                SimpleOrderEntity order = new SimpleOrderEntity();
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT ID, TOTAL, DESCRIPTION FROM SIMPLE_ORDER WHERE ID = ?")) {
                    stmt.setInt(1, 60);
                    stmt.execute();
                    ResultSet rSet = stmt.getResultSet();
                    rSet.next();
                    order.setId(rSet.getInt(1));
                    order.setTotal(rSet.getInt(2));
                    order.setDescription(rSet.getString(3));
                    rSet.close();
                }
                return order;
            });

            em.getTransaction().commit();

            assertNotNull("Selected order should not be null", selectedOrder);
            assertEquals(60, selectedOrder.getId());
            assertEquals(6666, selectedOrder.getTotal());
            assertEquals("desc66", selectedOrder.getDescription());

            // Verify via EM find
            SimpleOrderEntity found = em.find(SimpleOrderEntity.class, 60);
            assertNotNull("Order should be found after callWithConnection", found);
            assertEquals(selectedOrder.getId(), found.getId());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
