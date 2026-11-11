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
package org.apache.openjpa.slice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.slice.policy.UniformDistributionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The target hint confines bulk statements to the slices it names, the way
 * it confines a query.
 */
public class TestSliceTargetBulkWrite extends SliceTestCase {

    private List<String> sliceNames;

    @Override
    protected String getPersistenceUnitName() {
        return "slice";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(PObject.class, CLEAR_TABLES,
            "openjpa.slice.DistributionPolicy", UniformDistributionPolicy.class.getName());
    }

    /** Spreads 4 instances per slice, each with value 0. */
    private void seed() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // the active slices are only resolved once a broker has connected
            sliceNames = ((DistributedConfiguration) emf.getConfiguration()).getActiveSliceNames();
            assertTrue(sliceNames.size() > 1, "need at least two slices, got " + sliceNames);
            for (int i = 0; i < 4 * sliceNames.size(); i++) {
                PObject pc = new PObject();
                pc.setValue(0);
                em.persist(pc);
            }
            em.getTransaction().commit();
        }
    }

    private long countIn(String slice, int value) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select count(p) from PObject p where p.value = :v", Long.class)
                .setParameter("v", value)
                .setHint(SlicePersistence.HINT_TARGET, slice)
                .getSingleResult();
        }
    }

    private int run(String statement, boolean nativeSql, Object target) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int count = (nativeSql ? em.createNativeQuery(statement) : em.createQuery(statement))
                .setHint(SlicePersistence.HINT_TARGET, target)
                .executeUpdate();
            em.getTransaction().commit();
            return count;
        }
    }

    @Test
    public void testJpqlBulkUpdateRunsOnTheTargetSlice() {
        seed();
        String target = sliceNames.get(0);
        String other = sliceNames.get(1);

        int updated = run("update PObject p set p.value = 42", false, target);

        assertEquals(4, updated);
        assertEquals(4, countIn(target, 42));
        assertEquals(0, countIn(other, 42));
    }

    @Test
    public void testJpqlBulkUpdateRunsOnAllHintedSlices() {
        seed();

        int updated = run("update PObject p set p.value = 42", false,
            sliceNames.toArray(new String[0]));

        assertEquals(4 * sliceNames.size(), updated);
        for (String slice : sliceNames) {
            assertEquals(4, countIn(slice, 42));
        }
    }

    @Test
    public void testJpqlBulkDeleteRunsOnTheTargetSlice() {
        seed();
        String target = sliceNames.get(0);
        String other = sliceNames.get(1);

        run("delete from PObject p", false, target);

        assertEquals(0, countIn(target, 0));
        assertEquals(4, countIn(other, 0));
    }

    @Test
    public void testNativeBulkUpdateRunsOnTheTargetSlice() {
        seed();
        String target = sliceNames.get(0);
        String other = sliceNames.get(1);

        int updated = run("UPDATE PObject SET value = 42", true, target);

        assertEquals(4, updated);
        assertEquals(4, countIn(target, 42));
        assertEquals(0, countIn(other, 42));
    }

    @Test
    public void testNativeBulkDeleteRunsOnTheTargetSlice() {
        seed();
        String target = sliceNames.get(0);
        String other = sliceNames.get(1);

        int deleted = run("DELETE FROM PObject", true, target);

        assertEquals(4, deleted);
        assertEquals(0, countIn(target, 0));
        assertEquals(4, countIn(other, 0));
    }

    @Test
    public void testBulkUpdateWithoutTargetRunsOnAllSlices() {
        seed();

        int updated;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            updated = em.createQuery("update PObject p set p.value = 42").executeUpdate();
            em.getTransaction().commit();
        }

        assertEquals(4 * sliceNames.size(), updated);
        for (String slice : sliceNames) {
            assertEquals(4, countIn(slice, 42));
        }
    }

    @Test
    public void testUnknownTargetFails() {
        seed();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.createQuery("update PObject p set p.value = 42")
                    .setHint(SlicePersistence.HINT_TARGET, "NoSuchSlice")
                    .executeUpdate();
                fail("a target that names no active slice must fail");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("NoSuchSlice"), e.getMessage());
            } finally {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            }
        }
        for (String slice : sliceNames) {
            assertEquals(0, countIn(slice, 42));
        }
    }

    @Test
    public void testUnknownTargetFailsForAQuery() {
        seed();
        try (EntityManager em = emf.createEntityManager()) {
            em.createQuery("select p from PObject p")
                .setHint(SlicePersistence.HINT_TARGET, "NoSuchSlice")
                .getResultList();
            fail("a target that names no active slice must fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("NoSuchSlice"), e.getMessage());
        }
    }
}
