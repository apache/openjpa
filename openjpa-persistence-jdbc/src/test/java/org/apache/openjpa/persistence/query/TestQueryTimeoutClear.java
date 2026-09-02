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

import jakarta.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.query.common.apps.QTimeout;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that Query.setTimeout(null) clears a timeout set through that method.
 */
public class TestQueryTimeoutClear extends SingleEMFTestCase {

    private static final String JPQL = "SELECT o FROM QTimeout o";

    @Override
    public void setUp() throws Exception {
        super.setUp(QTimeout.class, CLEAR_TABLES);
    }

    public void testUnsetTimeoutIsNull() {
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            em.getFetchPlan().setQueryTimeout(9000);
            // the query inherits 9000, but nothing was set on the query itself
            assertNull(em.createQuery(JPQL).getTimeout());
        } finally {
            em.close();
        }
    }

    public void testExplicitZeroIsReported() {
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery(JPQL);
            q.setTimeout(0);
            assertEquals(Integer.valueOf(0), q.getTimeout());
        } finally {
            em.close();
        }
    }

    public void testSetTimeoutIsReported() {
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery(JPQL);
            q.setTimeout(5000);
            assertEquals(Integer.valueOf(5000), q.getTimeout());
            assertEquals(5000, ((OpenJPAQuery<?>) q).getFetchPlan().getQueryTimeout());
        } finally {
            em.close();
        }
    }

    public void testNullTimeoutClearsPreviousValue() {
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery(JPQL);
            q.setTimeout(5000);
            q.setTimeout(null);
            assertNull("A cleared timeout must not report the previous value",
                q.getTimeout());
            assertEquals("A cleared query must use what it inherits", 0,
                ((OpenJPAQuery<?>) q).getFetchPlan().getQueryTimeout());
            assertEquals("The query must fall back to what it inherits",
                em.getFetchPlan().getQueryTimeout(),
                ((OpenJPAQuery<?>) q).getFetchPlan().getQueryTimeout());
        } finally {
            em.close();
        }
    }

    public void testNullTimeoutRestoresInheritedValue() {
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            em.getFetchPlan().setQueryTimeout(9000);
            Query q = em.createQuery(JPQL);
            q.setTimeout(5000);
            assertEquals(Integer.valueOf(5000), q.getTimeout());

            q.setTimeout(null);
            assertNull("Nothing is set on the query after a clear",
                q.getTimeout());
            assertEquals("Clearing must restore the entity manager's timeout, "
                + "not the configuration default", 9000,
                ((OpenJPAQuery<?>) q).getFetchPlan().getQueryTimeout());
        } finally {
            em.close();
        }
    }

    public void testNullTimeoutOnAFreshQueryIsHarmless() {
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery(JPQL);
            q.setTimeout(null);
            assertNull(q.getTimeout());
            assertEquals(0, q.getResultList().size());
        } finally {
            em.close();
        }
    }
}
