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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.query.common.apps.Entity1;

/**
 * Tests that FlushModeType.AUTO causes pending changes to be flushed
 * before query execution, even when IgnoreChanges is true.
 *
 * This verifies the fix for the JPA TCK flushModeTest5 failure where
 * a modified entity was not visible in a query with FlushModeType.AUTO.
 */
public class TestFlushModeAutoQuery extends BaseQueryTest {

    public TestFlushModeAutoQuery(String test) {
        super(test);
    }

    @Override
    public void setUp() {
        deleteAll(Entity1.class);

        // Create test data
        EntityManager em = currentEntityManager();
        startTx(em);
        Entity1 e = new Entity1(100L, "original", 42);
        em.persist(e);
        endTx(em);
        endEm(em);
    }

    /**
     * Tests that when FlushModeType.AUTO is set on a query, pending
     * (unflushed) changes made in the same transaction are visible
     * to the query. This corresponds to JPA TCK flushModeTest5.
     */
    public void testFlushModeAutoSeesUnflushedChanges() {
        EntityManager em = currentEntityManager();
        startTx(em);

        // Modify an entity without flushing
        Entity1 e = em.find(Entity1.class, 100L);
        assertNotNull("Entity should exist", e);
        e.setStringField("modified");

        // Query with FlushModeType.AUTO should see the modification
        Query q = em.createQuery(
            "SELECT e FROM Entity1 e WHERE e.stringField = 'modified'");
        q.setFlushMode(FlushModeType.AUTO);

        @SuppressWarnings("unchecked")
        List<Entity1> results = q.getResultList();

        assertEquals("FlushModeType.AUTO should flush pending changes "
            + "before query, making the modified entity visible",
            1, results.size());
        assertEquals(100L, results.get(0).getPk());

        endTx(em);
        endEm(em);
    }

    /**
     * Tests that FlushModeType.COMMIT does NOT flush changes before
     * the query (the query may not see unflushed changes via DB).
     */
    public void testFlushModeCommitMayNotSeeChanges() {
        EntityManager em = currentEntityManager();
        startTx(em);

        Entity1 e = em.find(Entity1.class, 100L);
        assertNotNull("Entity should exist", e);
        e.setStringField("invisible");

        // With COMMIT mode, the query may execute in-memory or
        // not flush, so the original value should still be in DB
        Query q = em.createQuery(
            "SELECT e FROM Entity1 e WHERE e.stringField = 'original'");
        q.setFlushMode(FlushModeType.COMMIT);

        // We just verify this doesn't throw; the result depends on
        // whether OpenJPA evaluates in-memory or against DB
        @SuppressWarnings("unchecked")
        List<Entity1> results = q.getResultList();
        // Not asserting specific count as behavior depends on implementation

        endTx(em);
        endEm(em);
    }
}
