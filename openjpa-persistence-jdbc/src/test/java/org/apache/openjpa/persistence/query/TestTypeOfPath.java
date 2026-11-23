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
package org.apache.openjpa.persistence.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TYPE of a single-valued path joins the related entity.
 */
class TestTypeOfPath extends SQLListenerTestCase {

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, BulkRangeOrder.class, BulkRangeRushOrder.class,
            BulkRangeLine.class, TypePathVehicle.class, TypePathTruck.class,
            TypePathTool.class, TypePathDrill.class, TypePathItem.class);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (int i = 1; i <= 4; i++) {
                BulkRangeOrder order = (i <= 2) ? new BulkRangeOrder(i, "NEW")
                    : new BulkRangeRushOrder(i, "NEW");
                em.persist(order);
                em.persist(new BulkRangeLine(2 * i - 1, order, i));
                em.persist(new BulkRangeLine(2 * i, order, i));
            }
            for (int i = 1; i <= 3; i++) {
                TypePathVehicle vehicle = (i == 1) ? new TypePathVehicle(i)
                    : new TypePathTruck(i);
                TypePathTool tool = (i == 1) ? new TypePathTool(i)
                    : new TypePathDrill(i);
                em.persist(vehicle);
                em.persist(tool);
                em.persist(new TypePathItem(i, vehicle, tool));
            }
            em.persist(new TypePathItem(4, null, null));
            em.getTransaction().commit();
        }
        resetSQL();
    }

    @Test
    void deleteByTypeOfPath() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("DELETE FROM BulkRangeLine l"
                + " WHERE TYPE(l.order) = BulkRangeRushOrder").executeUpdate();
            em.getTransaction().commit();
            assertEquals(4, count, "deleted lines");
        }
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), ids("BulkRangeLine"));
    }

    @Test
    void updateByTypeOfPath() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("UPDATE BulkRangeLine l SET l.quantity = 0"
                + " WHERE TYPE(l.order) <> BulkRangeRushOrder").executeUpdate();
            em.getTransaction().commit();
            assertEquals(4, count, "updated lines");
        }
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L),
            ids("BulkRangeLine", "l.quantity = 0"));
    }

    @Test
    void countByTypeOfPath() {
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(4L, em.createQuery("SELECT COUNT(l) FROM BulkRangeLine l"
                + " WHERE TYPE(l.order) = BulkRangeRushOrder").getSingleResult());
        }
    }

    @Test
    void selectByTypeOfPath() {
        assertEquals(Arrays.asList(5L, 6L, 7L, 8L),
            ids("BulkRangeLine", "TYPE(l.order) = BulkRangeRushOrder"));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L),
            ids("BulkRangeLine", "TYPE(l.order) <> BulkRangeRushOrder"));
        assertEquals(Arrays.asList(5L, 6L, 7L, 8L),
            ids("BulkRangeLine", "TYPE(l.order) IN (BulkRangeRushOrder)"));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L),
            ids("BulkRangeLine", "TYPE(l.order) NOT IN (BulkRangeRushOrder)"));
    }

    @Test
    void selectByTypeOfPathParameter() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Long> ids = em.createQuery("SELECT l.id FROM BulkRangeLine l"
                + " WHERE TYPE(l.order) = :type ORDER BY l.id", Long.class)
                .setParameter("type", BulkRangeRushOrder.class)
                .getResultList();
            assertEquals(Arrays.asList(5L, 6L, 7L, 8L), ids);
        }
    }

    @Test
    void selectTypeOfPath() {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> types = em.createQuery("SELECT TYPE(l.order) FROM BulkRangeLine l"
                + " WHERE l.id IN (1, 8) ORDER BY l.id").getResultList();
            assertEquals(Arrays.asList(BulkRangeOrder.class, BulkRangeRushOrder.class),
                types);
        }
    }

    @Test
    void selectByTypeOfPathInSubquery() {
        assertEquals(Arrays.asList(3L, 4L), ids("BulkRangeOrder",
            "l.id IN (SELECT x.order.id FROM BulkRangeLine x"
            + " WHERE TYPE(x.order) = BulkRangeRushOrder)"));
        assertEquals(Arrays.asList(1L, 2L), ids("BulkRangeOrder",
            "EXISTS (SELECT x FROM BulkRangeLine x WHERE x.order = l"
            + " AND TYPE(x.order) <> BulkRangeRushOrder)"));
    }

    @Test
    void selectCaseTypeOfPath() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> kinds = em.createQuery("SELECT CASE WHEN TYPE(l.order)"
                + " = BulkRangeRushOrder THEN 'R' ELSE 'N' END FROM BulkRangeLine l"
                + " WHERE l.id IN (1, 8) ORDER BY l.id", String.class).getResultList();
            assertEquals(Arrays.asList("N", "R"), kinds);
        }
    }

    /**
     * TypePathTool has no discriminator column, so only the projection of
     * its type is tested.
     */
    @Test
    void selectTypeOfJoinedPath() {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> types = em.createQuery("SELECT TYPE(i.vehicle) FROM TypePathItem i"
                + " WHERE i.id < 4 ORDER BY i.id").getResultList();
            assertEquals(Arrays.asList(TypePathVehicle.class, TypePathTruck.class,
                TypePathTruck.class), types);
            types = em.createQuery("SELECT TYPE(i.tool) FROM TypePathItem i"
                + " WHERE i.id < 4 ORDER BY i.id").getResultList();
            assertEquals(Arrays.asList(TypePathTool.class, TypePathDrill.class,
                TypePathDrill.class), types);
        }
    }

    @Test
    void selectByTypeOfRoot() {
        assertEquals(Arrays.asList(3L, 4L),
            ids("BulkRangeOrder", "TYPE(l) = BulkRangeRushOrder"));
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(2L, em.createQuery("SELECT COUNT(o) FROM BulkRangeOrder o"
                + " WHERE TYPE(o) <> BulkRangeRushOrder").getSingleResult());
        }
    }

    /**
     * An item without a related entity has no type and matches neither
     * comparison.
     */
    @Test
    void selectByTypeOfOptionalPath() {
        assertEquals(Arrays.asList(2L, 3L),
            ids("TypePathItem", "TYPE(l.vehicle) = TypePathTruck"));
        assertEquals(Arrays.asList(1L),
            ids("TypePathItem", "TYPE(l.vehicle) <> TypePathTruck"));
    }

    @Test
    void deleteByTypeOfOptionalPath() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = em.createQuery("DELETE FROM TypePathItem i"
                + " WHERE TYPE(i.vehicle) = TypePathTruck").executeUpdate();
            em.getTransaction().commit();
            assertEquals(2, count, "deleted items");
        }
        assertEquals(Arrays.asList(1L, 4L), ids("TypePathItem"));
    }

    private List<Long> ids(String entity) {
        return ids(entity, "1 = 1");
    }

    private List<Long> ids(String entity, String where) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT l.id FROM " + entity + " l WHERE "
                + where + " ORDER BY l.id", Long.class).getResultList();
        }
    }
}
