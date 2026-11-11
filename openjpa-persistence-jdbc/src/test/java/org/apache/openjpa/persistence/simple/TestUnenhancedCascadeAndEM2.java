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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import org.apache.openjpa.persistence.EntityManagerImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runtime-enhanced (unenhanced) versions of tests for TCK failures:
 * - cascadeAllMX1Test2: re-persist removed entity with cascade ManyToOne
 * - persistMX1Test2: re-persist removed entity with cascade PERSIST ManyToOne
 * - getReferenceForNonExistingEntityTest: getReference(entity) for non-existing entity
 * - runWithConnectionTest / callWithConnectionTest: JPA 3.2 connection methods
 */
public class TestUnenhancedCascadeAndEM2 extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(UnenhancedCascadeAEntity.class, UnenhancedCascadeBAllEntity.class,
              UnenhancedCascadeBPersistEntity.class, UnenhancedSimpleOrderEntity.class,
              CLEAR_TABLES,
              "openjpa.RuntimeUnenhancedClasses", "supported");
    }

    /**
     * TCK: cascadeAllMX1Test2
     */
    @Test
    public void testCascadeAllMX1RePersistRemoved() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            UnenhancedCascadeAEntity aRef = new UnenhancedCascadeAEntity("2", "bean2", 2);
            UnenhancedCascadeBAllEntity b1 = new UnenhancedCascadeBAllEntity("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            UnenhancedCascadeAEntity newA1 = b1.getA1();
            assertNotNull(newA1, "A should be persisted via cascade");

            em.remove(newA1);
            em.remove(b1);
            em.flush();

            UnenhancedCascadeBAllEntity found = em.find(UnenhancedCascadeBAllEntity.class, "2");
            assertNull(found, "B should be removed");

            em.persist(b1);
            em.flush();

            assertTrue(em.contains(b1), "B should be managed");
            assertNotNull(b1.getA1(), "B's ManyToOne A should not be null after re-persist");

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
    @Test
    public void testPersistMX1RePersistRemoved() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            UnenhancedCascadeAEntity aRef = new UnenhancedCascadeAEntity("2", "bean2", 2);
            UnenhancedCascadeBPersistEntity b1 = new UnenhancedCascadeBPersistEntity("2", "b2", 2, aRef);
            em.persist(b1);
            em.flush();

            UnenhancedCascadeAEntity newA1 = b1.getA1();
            assertNotNull(newA1, "A should be persisted via cascade");

            em.remove(newA1);
            em.remove(em.find(UnenhancedCascadeBPersistEntity.class, "2"));
            em.flush();

            UnenhancedCascadeBPersistEntity found = em.find(UnenhancedCascadeBPersistEntity.class, "2");
            assertNull(found, "B should be removed");

            em.persist(b1);
            em.flush();

            assertTrue(em.contains(b1), "B should be managed");
            assertNotNull(b1.getA1(), "B's ManyToOne A should not be null after re-persist");

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
    @Test
    public void testGetReferenceForNonExistingEntity() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            UnenhancedSimpleOrderEntity order = new UnenhancedSimpleOrderEntity(99999, 0, "nonexistent");
            UnenhancedSimpleOrderEntity ref = em.getReference(order);
            assertNotNull(ref, "getReference should return a non-null reference");
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
     * TCK: runWithConnectionTest - mirrors the exact TCK test logic:
     * insert via JDBC, commit, find via JPA, check equals
     */
    @Test
    public void testRunWithConnection() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            UnenhancedSimpleOrderEntity order =
                new UnenhancedSimpleOrderEntity(50, 5555, "desc55");

            em.getTransaction().begin();

            em.<Connection>runWithConnection(connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO UNENH_SIMPLE_ORDER(ID, TOTAL, DESCRIPTION) VALUES(?, ?, ?)")) {
                    stmt.setInt(1, order.getId());
                    stmt.setInt(2, order.getTotal());
                    stmt.setString(3, order.getDescription());
                    stmt.executeUpdate();
                }
            });

            em.getTransaction().commit();

            UnenhancedSimpleOrderEntity found =
                em.find(UnenhancedSimpleOrderEntity.class, order.getId());
            assertNotNull(found,
                "Order should be found after runWithConnection insert");
            assertTrue(found.equals(order),
                "Found entity should equal original order"
                + " (found.id=" + found.getId()
                + ", found.total=" + found.getTotal()
                + ", found.desc=" + found.getDescription()
                + ", order.id=" + order.getId()
                + ", order.total=" + order.getTotal()
                + ", order.desc=" + order.getDescription()
                + ", found.class=" + found.getClass().getName()
                + ", order.class=" + order.getClass().getName()
                + ")");
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * TCK: callWithConnectionTest - mirrors the exact TCK test logic:
     * insert + read via JDBC, commit, find via JPA, check equals
     */
    @Test
    public void testCallWithConnection() {
        EntityManagerImpl em = (EntityManagerImpl) emf.createEntityManager();
        try {
            UnenhancedSimpleOrderEntity order =
                new UnenhancedSimpleOrderEntity(60, 6666, "desc66");

            em.getTransaction().begin();

            UnenhancedSimpleOrderEntity selectedOrder =
                em.<Connection, UnenhancedSimpleOrderEntity>callWithConnection(
                connection -> {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO UNENH_SIMPLE_ORDER(ID, TOTAL, DESCRIPTION)"
                            + " VALUES(?, ?, ?)")) {
                        stmt.setInt(1, order.getId());
                        stmt.setInt(2, order.getTotal());
                        stmt.setString(3, order.getDescription());
                        stmt.executeUpdate();
                    }
                    UnenhancedSimpleOrderEntity o =
                        new UnenhancedSimpleOrderEntity();
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT ID, TOTAL, DESCRIPTION FROM UNENH_SIMPLE_ORDER WHERE ID = ?")) {
                        stmt.setInt(1, order.getId());
                        stmt.execute();
                        ResultSet rSet = stmt.getResultSet();
                        rSet.next();
                        o.setId(rSet.getInt(1));
                        o.setTotal(rSet.getInt(2));
                        o.setDescription(rSet.getString(3));
                        rSet.close();
                    }
                    return o;
                }
            );

            em.getTransaction().commit();

            UnenhancedSimpleOrderEntity found =
                em.find(UnenhancedSimpleOrderEntity.class, order.getId());
            assertNotNull(found,
                "Order should be found after callWithConnection");
            assertTrue(found.equals(selectedOrder),
                "Found entity should equal selected order"
                + " (found.id=" + found.getId()
                + ", found.total=" + found.getTotal()
                + ", found.desc=" + found.getDescription()
                + ", selected.id=" + selectedOrder.getId()
                + ", selected.total=" + selectedOrder.getTotal()
                + ", selected.desc=" + selectedOrder.getDescription()
                + ", found.class=" + found.getClass().getName()
                + ", selected.class=" + selectedOrder.getClass().getName()
                + ")");
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
