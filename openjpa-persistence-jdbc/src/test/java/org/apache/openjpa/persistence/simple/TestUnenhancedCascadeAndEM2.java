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
 * Runtime-enhanced (unenhanced) versions of tests for TCK failures:
 * - cascadeAllMX1Test2: re-persist removed entity with cascade ManyToOne
 * - persistMX1Test2: re-persist removed entity with cascade PERSIST ManyToOne
 * - getReferenceForNonExistingEntityTest: getReference(entity) for non-existing entity
 * - runWithConnectionTest / callWithConnectionTest: JPA 3.2 connection methods
 */
public class TestUnenhancedCascadeAndEM2 extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(UnenhancedCascadeAEntity.class, UnenhancedCascadeBAllEntity.class,
              UnenhancedCascadeBPersistEntity.class, UnenhancedSimpleOrderEntity.class,
              CLEAR_TABLES,
              "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * TCK: cascadeAllMX1Test2
     */
    public void testCascadeAllMX1RePersistRemoved() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            UnenhancedCascadeAEntity aRef = new UnenhancedCascadeAEntity("2", "bean2", 2);
            UnenhancedCascadeBAllEntity b1 = new UnenhancedCascadeBAllEntity("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            UnenhancedCascadeAEntity newA1 = b1.getA1();
            assertNotNull("A should be persisted via cascade", newA1);

            em.remove(newA1);
            em.remove(b1);
            em.flush();

            UnenhancedCascadeBAllEntity found = em.find(UnenhancedCascadeBAllEntity.class, "2");
            assertNull("B should be removed", found);

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
     */
    public void testPersistMX1RePersistRemoved() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            UnenhancedCascadeAEntity aRef = new UnenhancedCascadeAEntity("2", "bean2", 2);
            UnenhancedCascadeBPersistEntity b1 = new UnenhancedCascadeBPersistEntity("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            UnenhancedCascadeAEntity newA1 = b1.getA1();
            assertNotNull("A should be persisted via cascade", newA1);

            em.remove(newA1);
            em.remove(em.find(UnenhancedCascadeBPersistEntity.class, "2"));
            em.flush();

            UnenhancedCascadeBPersistEntity found = em.find(UnenhancedCascadeBPersistEntity.class, "2");
            assertNull("B should be removed", found);

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
     */
    public void testGetReferenceForNonExistingEntity() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            UnenhancedSimpleOrderEntity order = new UnenhancedSimpleOrderEntity(99999, 0, "nonexistent");
            UnenhancedSimpleOrderEntity ref = em.getReference(order);
            assertNotNull("getReference should return a non-null reference", ref);
            try {
                String desc = ref.getDescription();
                fail("Expected EntityNotFoundException when accessing non-existing entity reference"
                    + " but got description='" + desc + "'"
                    + ", ref class=" + ref.getClass().getName()
                    + ", ref id=" + ref.getId());
            } catch (EntityNotFoundException e) {
                // expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * TCK: runWithConnectionTest
     */
    public void testRunWithConnection() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            em.getTransaction().begin();

            em.<Connection>runWithConnection(connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO UNENH_SIMPLE_ORDER(ID, TOTAL, DESCRIPTION) VALUES(?, ?, ?)")) {
                    stmt.setInt(1, 50);
                    stmt.setInt(2, 5555);
                    stmt.setString(3, "desc55");
                    stmt.executeUpdate();
                }
            });

            em.getTransaction().commit();

            UnenhancedSimpleOrderEntity found = em.find(UnenhancedSimpleOrderEntity.class, 50);
            assertNotNull("Order should be found after runWithConnection insert", found);
            assertEquals(50, found.getId());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: callWithConnectionTest
     */
    public void testCallWithConnection() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            em.getTransaction().begin();

            UnenhancedSimpleOrderEntity selectedOrder = em.<Connection, UnenhancedSimpleOrderEntity>callWithConnection(
                connection -> {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO UNENH_SIMPLE_ORDER(ID, TOTAL, DESCRIPTION) VALUES(?, ?, ?)")) {
                        stmt.setInt(1, 60);
                        stmt.setInt(2, 6666);
                        stmt.setString(3, "desc66");
                        stmt.executeUpdate();
                    }
                    UnenhancedSimpleOrderEntity order = new UnenhancedSimpleOrderEntity();
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT ID, TOTAL, DESCRIPTION FROM UNENH_SIMPLE_ORDER WHERE ID = ?")) {
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
                }
            );

            em.getTransaction().commit();

            assertNotNull("Selected order should not be null", selectedOrder);
            assertEquals(60, selectedOrder.getId());

            UnenhancedSimpleOrderEntity found = em.find(UnenhancedSimpleOrderEntity.class, 60);
            assertNotNull("Order should be found after callWithConnection", found);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
