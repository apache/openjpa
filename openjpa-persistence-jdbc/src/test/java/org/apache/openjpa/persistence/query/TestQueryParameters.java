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

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.ParameterImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests query parameters.
 *
 * @author Pinaki Poddar
 *
 */
class TestQueryParameters extends SingleEMFTestCase {
    private static OpenJPAEntityManagerFactorySPI oemf;
    private static int TEST_COUNT = 0;
    private EntityManager em;

    @BeforeEach
    public void setUp() {
        if (oemf == null) {
            super.setUp(SimpleEntity.class, "openjpa.DynamicEnhancementAgent", "false");
            oemf = (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.cast(emf);
        }
        em = oemf.createEntityManager();
        TEST_COUNT++;
    }

    @AfterEach
    public void tearDown() throws Exception {
        // do not close the factory until done
        if (TEST_COUNT >= 20) {
            closeEMF(oemf);
            oemf = null;
            super.tearDown();
        }
    }

    @Test
    void namedParameterUsingReservedWord() {
        String jpql = "select e from simple e WHERE e.id=:key and e.name=:value";
        Query q = em.createQuery(jpql)
                    .setParameter("key", 100)
                    .setParameter("value", "XYZ");

        assertEquals(2, q.getParameters().size());
        Parameter<?> param1 = q.getParameter("key");
        Parameter<?> param2 = q.getParameter("value");

        assertEquals(100, q.getParameterValue("key"));
        assertEquals(100, q.getParameterValue(param1));
        assertEquals("XYZ", q.getParameterValue("value"));
        assertEquals("XYZ", q.getParameterValue(param2));

        q.getResultList();
    }

    @Test
    void positionalParameterInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=?1 and e.name=?2";
        Query q = em.createQuery(jpql)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        assertEquals(2, q.getParameters().size());
        Parameter<?> param1 = q.getParameter(1);
        Parameter<?> param2 = q.getParameter(2);

        assertEquals(100, q.getParameterValue(1));
        assertEquals(100, q.getParameterValue(param1));
        assertEquals("XYZ", q.getParameterValue(2));
        assertEquals("XYZ", q.getParameterValue(param2));

        q.getResultList();
    }

    @Test
    void namedParameterInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=:id and e.name=:name";
        Query q = em.createQuery(jpql)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        assertEquals(2, q.getParameters().size());
        Parameter<?> param1 = q.getParameter("id");
        Parameter<?> param2 = q.getParameter("name");

        assertEquals(100, q.getParameterValue("id"));
        assertEquals(100, q.getParameterValue(param1));
        assertEquals("XYZ", q.getParameterValue("name"));
        assertEquals("XYZ", q.getParameterValue(param2));

        q.getResultList();
    }

    @Test
    void positionalParameterMissingInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=?1 and e.name=?2";
        Query q = em.createQuery(jpql)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        assertSetParameterFails(q, 3, 100); // wrong position
    }

    @Test
    void namedParameterMissingInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=:id and e.name=:name";
        Query q = em.createQuery(jpql)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        assertSetParameterFails(q, "xyz", 100); // wrong name
    }

    @Test
    void positionalParameterWrongValueInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=?1 and e.name=?2";
        Query q = em.createQuery(jpql)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        assertSetParameterFails(q, 1, "XYZ"); // wrong value
        assertSetParameterFails(q, 2, 100); // wrong value
    }

    @Test
    void namedParameterWrongValueInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=:id and e.name=:name";
        Query q = em.createQuery(jpql)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        assertSetParameterFails(q, "id", "XYZ"); // wrong value
        assertSetParameterFails(q, "name", 100); // wrong value
    }

    @Test
    void positionalParameterValueTypeInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=?1 and e.name=?2";
        Query q = em.createQuery(jpql)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        Parameter<?> param1 = q.getParameter(1);
        assertInstanceOf(ParameterImpl.class, param1);
        assertEquals(long.class, param1.getParameterType());

        Parameter<?> param2 = q.getParameter(2);
        assertInstanceOf(ParameterImpl.class, param2);
        assertEquals(String.class, param2.getParameterType());
    }

    @Test
    void namedParameterValueTypeInJPQLQuery() {
        String jpql = "select e from simple e WHERE e.id=:id and e.name=:name";
        Query q = em.createQuery(jpql)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        Parameter<?> param1 = q.getParameter("id");
        assertInstanceOf(ParameterImpl.class, param1);
        assertEquals(long.class, param1.getParameterType());

        Parameter<?> param2 = q.getParameter("name");
        assertInstanceOf(ParameterImpl.class, param2);
        assertEquals(String.class, param2.getParameterType());
    }

    @Test
    void namedParameterInPreparedQuery() {
        String jpql = "select x from simple x WHERE x.id=:id and x.name=:name";
        Query q = em.createQuery(jpql)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");
        q.getResultList();

        assertEquals(JPQLParser.LANG_JPQL, OpenJPAPersistence.cast(q).getLanguage());

        Query q2 = em.createQuery(jpql)
                     .setParameter("id", 200)
                     .setParameter("name", "ZXY");

        assertEquals(QueryLanguages.LANG_PREPARED_SQL, OpenJPAPersistence.cast(q2).getLanguage());
        q2.getResultList();
    }

    //--------------------------------------------------------------------------------------------
    // Similar tests with NamedQuery
    //--------------------------------------------------------------------------------------------
    @Test
    void positionalParameterInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_POSITIONAL_PARAMS)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        assertEquals(2, q.getParameters().size());
        Parameter<?> param1 = q.getParameter(1);
        Parameter<?> param2 = q.getParameter(2);

        assertEquals(100, q.getParameterValue(1));
        assertEquals(100, q.getParameterValue(param1));
        assertEquals("XYZ", q.getParameterValue(2));
        assertEquals("XYZ", q.getParameterValue(param2));

        q.getResultList();
    }

    @Test
    void namedParameterInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_NAMED_PARAMS)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        assertEquals(2, q.getParameters().size());
        Parameter<?> param1 = q.getParameter("id");
        Parameter<?> param2 = q.getParameter("name");

        assertEquals(100, q.getParameterValue("id"));
        assertEquals(100, q.getParameterValue(param1));
        assertEquals("XYZ", q.getParameterValue("name"));
        assertEquals("XYZ", q.getParameterValue(param2));

        q.getResultList();
    }

    @Test
    void positionalParameterMissingInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_POSITIONAL_PARAMS)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        assertSetParameterFails(q, 3, 100); // wrong position
    }

    @Test
    void namedParameterMissingInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_NAMED_PARAMS)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        assertSetParameterFails(q, "xyz", 100); // wrong name
    }

    @Test
    void positionalParameterWrongValueInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_POSITIONAL_PARAMS)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        assertSetParameterFails(q, 1, "XYZ"); // wrong value
        assertSetParameterFails(q, 2, 100); // wrong value
    }

    @Test
    void namedParameterWrongValueInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_NAMED_PARAMS)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        assertSetParameterFails(q, "id", "XYZ"); // wrong value
        assertSetParameterFails(q, "name", 100); // wrong value
    }

    @Test
    void positionalParameterValueTypeInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_POSITIONAL_PARAMS)
                    .setParameter(1, 100)
                    .setParameter(2, "XYZ");

        Parameter<?> param1 = q.getParameter(1);
        assertInstanceOf(ParameterImpl.class, param1);
        assertEquals(long.class, param1.getParameterType());

        Parameter<?> param2 = q.getParameter(2);
        assertInstanceOf(ParameterImpl.class, param2);
        assertEquals(String.class, param2.getParameterType());
    }

    @Test
    void namedParameterValueTypeInNamedQuery() {
        Query q = em.createNamedQuery(SimpleEntity.NAMED_QUERY_WITH_NAMED_PARAMS)
                    .setParameter("id", 100)
                    .setParameter("name", "XYZ");

        Parameter<?> param1 = q.getParameter("id");
        assertInstanceOf(ParameterImpl.class, param1);
        assertEquals(long.class, param1.getParameterType());

        Parameter<?> param2 = q.getParameter("name");
        assertInstanceOf(ParameterImpl.class, param2);
        assertEquals(String.class, param2.getParameterType());
    }

    @Test
    void criteriaQueryWithNamedParameter() {
        Metamodel model = oemf.getMetamodel();
        EntityType<SimpleEntity> entity = model.entity(SimpleEntity.class);
        SingularAttribute<SimpleEntity, Long> id =
            (SingularAttribute<SimpleEntity, Long>)entity.getSingularAttribute("id");
        SingularAttribute<SimpleEntity, String> name =
            (SingularAttribute<SimpleEntity, String>)entity.getSingularAttribute("name");

        CriteriaBuilder cb = oemf.getCriteriaBuilder();
        CriteriaQuery<SimpleEntity> c = cb.createQuery(SimpleEntity.class);
        Root<SimpleEntity> root = c.from(SimpleEntity.class);
        ParameterExpression<Long> param1 = cb.parameter(long.class, "id");
        ParameterExpression<String> param2 = cb.parameter(String.class, "name");
        Predicate p1 = cb.equal(root.get(id), param1);
        Predicate p2 = cb.equal(root.get(name), param2);
        c.where(cb.and(p1,p2));

        Query q = em.createQuery(c);
        assertEquals(2, q.getParameters().size());
        assertTrue(q.getParameters().contains(param1));
        assertTrue(q.getParameters().contains(param2));
        assertNotNull(q.getParameter("id"));
        assertNotNull(q.getParameter("name"));
    }

    @Test
    void criteriaQueryWithUnnamedParameter() {
        Metamodel model = oemf.getMetamodel();
        EntityType<SimpleEntity> entity = model.entity(SimpleEntity.class);
        SingularAttribute<SimpleEntity, Long> id =
            (SingularAttribute<SimpleEntity, Long>)entity.getSingularAttribute("id");
        SingularAttribute<SimpleEntity, String> name =
            (SingularAttribute<SimpleEntity, String>)entity.getSingularAttribute("name");

        CriteriaBuilder cb = oemf.getCriteriaBuilder();
        CriteriaQuery<SimpleEntity> c = cb.createQuery(SimpleEntity.class);
        Root<SimpleEntity> root = c.from(SimpleEntity.class);
        ParameterExpression<Long> param1 = cb.parameter(long.class);
        ParameterExpression<String> param2 = cb.parameter(String.class);
        Predicate p1 = cb.equal(root.get(id), param1);
        Predicate p2 = cb.equal(root.get(name), param2);
        c.where(cb.and(p1,p2));

        Query q = em.createQuery(c);
        assertEquals(2, q.getParameters().size());
        assertTrue(q.getParameters().contains(param1));
        assertTrue(q.getParameters().contains(param2));
    }



    void assertSetParameterFails(Query q, String name, Object v) {
        try {
            q.setParameter(name, v);
            fail("Expected " + IllegalArgumentException.class.getName());
        } catch (IllegalArgumentException e) {
            // good
            // Following is expected exception, uncomment to verify error message
            // System.err.println(e);
        }
    }

    void assertSetParameterFails(Query q, int pos, Object v) {
        try {
            q.setParameter(pos, v);
            fail("Expected " + IllegalArgumentException.class.getName());
        } catch (IllegalArgumentException e) {
            // good
            // Following is expected exception, uncomment to verify error message
            // System.err.println(e);
        }
    }
}
