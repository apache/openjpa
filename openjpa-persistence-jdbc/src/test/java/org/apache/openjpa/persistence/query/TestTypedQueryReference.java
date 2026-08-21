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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests {@code EntityManager.createQuery(TypedQueryReference)} added in JPA 3.2.
 */
public class TestTypedQueryReference extends SingleEMFTestCase {

    private static final String TIMEOUT_HINT = "jakarta.persistence.query.timeout";

    @Override
    public void setUp() {
        setUp(SimpleEntity.class, CLEAR_TABLES);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new SimpleEntity("Name One", "Value One"));
        em.persist(new SimpleEntity("Name Two", "Value Two"));
        em.getTransaction().commit();
        em.close();
    }

    private static <R> TypedQueryReference<R> ref(final String name, final Class<R> type,
            final Map<String, Object> hints) {
        return new TypedQueryReference<R>() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<? extends R> getResultType() {
                return type;
            }

            @Override
            public Map<String, Object> getHints() {
                return hints;
            }
        };
    }

    public void testCreateQueryFromReference() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<SimpleEntity> q =
                em.createQuery(ref("FindAll", SimpleEntity.class, Collections.<String, Object>emptyMap()));
            assertNotNull(q);
            List<SimpleEntity> list = q.getResultList();
            assertEquals(2, list.size());
            for (SimpleEntity s : list) {
                assertNotNull(s.getName());
            }
        } finally {
            em.close();
        }
    }

    public void testResultTypeIsApplied() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<SimpleEntity> q =
                em.createQuery(ref("FindAll", SimpleEntity.class, Collections.<String, Object>emptyMap()));
            assertEquals(SimpleEntity.class, ((OpenJPAQuery<?>) q).getResultClass());
        } finally {
            em.close();
        }
    }

    public void testReferenceHintsAreApplied() {
        EntityManager em = emf.createEntityManager();
        try {
            Map<String, Object> hints =
                Collections.<String, Object>singletonMap(TIMEOUT_HINT, Integer.valueOf(12345));
            TypedQuery<SimpleEntity> q = em.createQuery(ref("FindAll", SimpleEntity.class, hints));
            assertEquals(Integer.valueOf(12345), q.getHints().get(TIMEOUT_HINT));
        } finally {
            em.close();
        }
    }

    public void testParametersStillWork() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<SimpleEntity> q =
                em.createQuery(ref("FindOne", SimpleEntity.class, Collections.<String, Object>emptyMap()));
            q.setParameter(1, "Name One");
            assertEquals("Value One", q.getSingleResult().getValue());
        } finally {
            em.close();
        }
    }

    public void testNullReference() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createQuery((TypedQueryReference<SimpleEntity>) null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        } finally {
            em.close();
        }
    }

    public void testUnknownQueryName() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createQuery(ref("NoSuchNamedQuery", SimpleEntity.class, Collections.<String, Object>emptyMap()));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        } finally {
            em.close();
        }
    }

    public void testClosedEntityManager() {
        EntityManager em = emf.createEntityManager();
        em.close();
        try {
            em.createQuery(ref("FindAll", SimpleEntity.class, Collections.<String, Object>emptyMap()));
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
