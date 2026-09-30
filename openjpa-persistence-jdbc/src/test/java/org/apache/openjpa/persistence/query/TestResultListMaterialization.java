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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests the default materialized <code>Query.getResultList()</code> and the
 * <code>MaterializeQueryResultList</code> compatibility option that restores
 * the lazy, streaming result list of earlier releases.
 */
public class TestResultListMaterialization extends SingleEMFTestCase {

    private static final String JPQL = "SELECT o FROM AllFieldTypes o";

    @Override
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
        populate(emf);
    }

    /**
     * A query with a small fetch batch size, so that the kernel uses a lazy,
     * forward-only result list when results are not materialized.
     */
    private OpenJPAQuery<?> batchedQuery(EntityManager em) {
        OpenJPAQuery<?> q = OpenJPAPersistence.cast(em.createQuery(JPQL));
        q.getFetchPlan().setFetchBatchSize(3);
        return q;
    }

    private void populate(OpenJPAEntityManagerFactorySPI factory) {
        EntityManager em = factory.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < 10; i++) {
            AllFieldTypes aft = new AllFieldTypes();
            aft.setIntField(i);
            em.persist(aft);
        }
        em.getTransaction().commit();
        em.close();
    }

    public void testResultListIsMaterializedByDefault() {
        EntityManager em = emf.createEntityManager();
        List<?> result = batchedQuery(em).getResultList();
        assertTrue("expected an ArrayList, got " + result.getClass(), result instanceof ArrayList);
        assertFalse(result instanceof ResultList);
        em.close();
        // the snapshot is detached from the EntityManager lifecycle
        assertEquals(10, result.size());
    }

    public void testCompatibilityOptionRestoresLazyResultList() {
        OpenJPAEntityManagerFactorySPI factory = createEMF(AllFieldTypes.class,
            "openjpa.Compatibility", "MaterializeQueryResultList=false");
        try {
            EntityManager em = factory.createEntityManager();
            List<?> result = batchedQuery(em).getResultList();
            assertTrue("expected a lazy ResultList, got " + result.getClass(), result instanceof ResultList);
            // rows are still streamed from the open result object provider
            assertTrue("result list should not be materialized", ((ResultList<?>) result).isProviderOpen());
            assertEquals(10, result.size());
            em.close();
        } finally {
            closeEMF(factory);
        }
    }
}
