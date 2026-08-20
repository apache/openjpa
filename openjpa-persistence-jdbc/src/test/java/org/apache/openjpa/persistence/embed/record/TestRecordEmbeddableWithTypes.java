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
package org.apache.openjpa.persistence.embed.record;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests record embeddables with non-String component types (long, double)
 * and custom @Column names.
 */
public class TestRecordEmbeddableWithTypes extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(LocationEntity.class, CoordinateRecord.class,
                CLEAR_TABLES);
    }

    public void testPersistAndLoadWithLongAndDouble() {
        final CoordinateRecord coord =
                new CoordinateRecord(39.7817, -89.6501, 200L);
        final LocationEntity loc = new LocationEntity("Springfield", coord);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(loc);
        em.getTransaction().commit();
        final long id = loc.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final LocationEntity loaded =
                em2.find(LocationEntity.class, id);
        assertNotNull(loaded);
        assertEquals("Springfield", loaded.getName());
        assertNotNull(loaded.getCoordinate());
        assertEquals(39.7817, loaded.getCoordinate().latitude(), 0.0001);
        assertEquals(-89.6501, loaded.getCoordinate().longitude(), 0.0001);
        assertEquals(200L, loaded.getCoordinate().altitude());
        em2.close();
    }

    public void testUpdateCoordinate() {
        final LocationEntity loc = new LocationEntity("MountainBase",
                new CoordinateRecord(46.8523, 9.5308, 1500L));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(loc);
        em.getTransaction().commit();
        final long id = loc.getId();
        em.close();

        // Update to a new coordinate
        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        final LocationEntity managed =
                em2.find(LocationEntity.class, id);
        managed.setCoordinate(
                new CoordinateRecord(46.8524, 9.5310, 2800L));
        em2.getTransaction().commit();
        em2.close();

        // Verify
        final EntityManager em3 = emf.createEntityManager();
        final LocationEntity updated =
                em3.find(LocationEntity.class, id);
        assertNotNull(updated.getCoordinate());
        assertEquals(46.8524, updated.getCoordinate().latitude(), 0.0001);
        assertEquals(9.5310, updated.getCoordinate().longitude(), 0.0001);
        assertEquals(2800L, updated.getCoordinate().altitude());
        em3.close();
    }

    public void testDeleteLocation() {
        final LocationEntity loc = new LocationEntity("ToDelete",
                new CoordinateRecord(0.0, 0.0, 0L));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(loc);
        em.getTransaction().commit();
        final long id = loc.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        em2.createQuery(
                "DELETE FROM LocationEntity l WHERE l.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        em2.getTransaction().commit();
        em2.close();

        final EntityManager em3 = emf.createEntityManager();
        assertNull(em3.find(LocationEntity.class, id));
        em3.close();
    }

    public void testNullCoordinate() {
        final LocationEntity loc = new LocationEntity("NoCoord", null);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(loc);
        em.getTransaction().commit();
        final long id = loc.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final LocationEntity loaded =
                em2.find(LocationEntity.class, id);
        assertNotNull(loaded);
        assertEquals("NoCoord", loaded.getName());
        // All-zero/null columns: record with default values or null
        em2.close();
    }

    public void testRecordEqualityAfterLoad() {
        final CoordinateRecord original =
                new CoordinateRecord(51.5074, -0.1278, 11L);
        final LocationEntity loc = new LocationEntity("London", original);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(loc);
        em.getTransaction().commit();
        final long id = loc.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final LocationEntity loaded =
                em2.find(LocationEntity.class, id);
        final CoordinateRecord loadedCoord = loaded.getCoordinate();

        // Record equals/hashCode should work across persist/load
        assertEquals(original, loadedCoord);
        assertEquals(original.hashCode(), loadedCoord.hashCode());
        em2.close();
    }
}
