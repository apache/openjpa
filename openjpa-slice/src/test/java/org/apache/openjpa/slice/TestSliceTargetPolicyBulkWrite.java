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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.slice.policy.FirstSliceQueryTargetPolicy;
import org.apache.openjpa.slice.policy.UniformDistributionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A configured {@link QueryTargetPolicy} is consulted for bulk statements as
 * it is for queries. The policy here confines every query to the first slice.
 */
public class TestSliceTargetPolicyBulkWrite extends SliceTestCase {

    private List<String> sliceNames;

    @Override
    protected String getPersistenceUnitName() {
        return "slice";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(PObject.class, CLEAR_TABLES,
            "openjpa.slice.DistributionPolicy", UniformDistributionPolicy.class.getName(),
            "openjpa.slice.QueryTargetPolicy", FirstSliceQueryTargetPolicy.class.getName());
    }

    private void seed() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
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

    /** Counts with an explicit target hint, which takes precedence over the policy. */
    private long countIn(String slice, int value) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select count(p) from PObject p where p.value = :v", Long.class)
                .setParameter("v", value)
                .setHint(SlicePersistence.HINT_TARGET, slice)
                .getSingleResult();
        }
    }

    private int run(Query query) {
        return query.executeUpdate();
    }

    @Test
    public void testPolicyAppliesToJpqlBulkDelete() {
        seed();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            run(em.createQuery("delete from PObject p"));
            em.getTransaction().commit();
        }
        assertEquals(0, countIn(sliceNames.get(0), 0));
        assertEquals(4, countIn(sliceNames.get(1), 0));
    }

    @Test
    public void testPolicyAppliesToJpqlBulkUpdate() {
        seed();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(4, run(em.createQuery("update PObject p set p.value = 42")));
            em.getTransaction().commit();
        }
        assertEquals(4, countIn(sliceNames.get(0), 42));
        assertEquals(0, countIn(sliceNames.get(1), 42));
    }

    @Test
    public void testPolicyAppliesToJpqlBulkUpdateWithParameters() {
        seed();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(4, run(em.createQuery("update PObject p set p.value = :v where p.value = :old")
                .setParameter("v", 42).setParameter("old", 0)));
            assertEquals(4, run(em.createQuery("update PObject p set p.value = ?1 where p.value = ?2")
                .setParameter(1, 43).setParameter(2, 42)));
            em.getTransaction().commit();
        }
        assertEquals(4, countIn(sliceNames.get(0), 43));
        assertEquals(4, countIn(sliceNames.get(1), 0));
    }

    @Test
    public void testPolicyAppliesToNativeBulkStatements() {
        seed();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(4, run(em.createNativeQuery("UPDATE PObject SET value = 42")));
            assertEquals(4, run(em.createNativeQuery("DELETE FROM PObject WHERE value = 42")));
            em.getTransaction().commit();
        }
        assertEquals(0, countIn(sliceNames.get(0), 42));
        assertEquals(4, countIn(sliceNames.get(1), 0));
    }

    /** An explicit target stays in place when other hints are set after it. */
    @Test
    public void testExplicitTargetWinsOverPolicyAfterAnotherHint() {
        seed();
        String second = sliceNames.get(1);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int updated = run(em.createQuery("update PObject p set p.value = 42")
                .setHint(SlicePersistence.HINT_TARGET, second)
                .setHint("openjpa.FetchPlan.FetchBatchSize", 10));
            em.getTransaction().commit();
            assertEquals(4, updated);
        }
        assertEquals(4, countIn(second, 42));
        assertEquals(0, countIn(sliceNames.get(0), 42));
    }
}
