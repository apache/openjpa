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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A subquery nested in another subquery is correlated with the root of the
 * enclosing subquery when it refers to a field of that root, without adding
 * that root once more to the enclosing subquery.
 */
public class TestNestedCorrelatedSubquery extends SingleEMFTestCase {

    private EntityManager em;

    @BeforeEach
    public void setUp() {
        setUp(NestedSubqueryOwner.class, NestedSubqueryDetailedOwner.class, CLEAR_TABLES);
        em = emf.createEntityManager();
        em.getTransaction().begin();
        NestedSubqueryOwner first = new NestedSubqueryOwner(1, "x", "a", "b");
        NestedSubqueryOwner second = new NestedSubqueryOwner(2, "y", "a");
        NestedSubqueryOwner third = new NestedSubqueryOwner(3, "x");
        first.getAttributes().put("k1", "v1");
        second.getAttributes().put("k2", "v2");
        second.setParent(first);
        third.setParent(first);
        em.persist(first);
        em.persist(second);
        em.persist(third);
        em.persist(new NestedSubqueryDetailedOwner(4, "x", "d", "b"));
        em.persist(new NestedSubqueryDetailedOwner(5, "z", "x"));
        em.getTransaction().commit();
        em.clear();
    }

    @AfterEach
    public void closeEntityManager() {
        em.close();
    }

    @Test
    public void testCriteriaCountWithElementCollectionSubqueryInSubquery() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<NestedSubqueryOwner> owner = query.from(NestedSubqueryOwner.class);
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<NestedSubqueryOwner> subqueryOwner = subquery.from(NestedSubqueryOwner.class);
        Subquery<Long> tagCount = subquery.subquery(Long.class);
        Root<NestedSubqueryOwner> tagOwner = tagCount.from(NestedSubqueryOwner.class);
        Join<NestedSubqueryOwner, String> tag = tagOwner.join("tags");
        tagCount.select(cb.countDistinct(tag))
            .where(cb.equal(tag, "b"), cb.equal(tagOwner.get("id"), subqueryOwner.get("id")));
        subquery.select(subqueryOwner.get("id"))
            .where(cb.equal(tagCount, 1L), cb.equal(subqueryOwner.get("id"), owner.get("id")));
        query.select(cb.count(owner)).where(cb.exists(subquery));
        assertEquals(2L, em.createQuery(query).getSingleResult());
    }

    @Test
    public void testCountWithElementCollectionSubqueryInSubquery() {
        assertEquals(2L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE (SELECT COUNT(DISTINCT t) FROM NestedSubqueryOwner n JOIN n.tags t WHERE t = 'b' AND n.id = s.id) = 1"
            + " AND s.id = e.id)"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithEnclosingSubquery() {
        assertEquals(3L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.id = s.id AND n.name = 'x'))"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithSubqueryTwoLevelsUp() {
        assertEquals(3L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT m.id FROM NestedSubqueryOwner m WHERE m.id = s.id"
            + " AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.id = s.id AND n.name = 'x')))"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithRelationOfEnclosingSubquery() {
        assertEquals(2L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.parent = s.parent AND n.id <> s.id))"));
    }

    @Test
    public void testMemberOfElementCollectionOfEnclosingSubquery() {
        assertEquals(2L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.id = s.id AND 'b' MEMBER OF s.tags))"));
    }

    @Test
    public void testMemberOfRelationOfEnclosingSubquery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n MEMBER OF s.children AND n.name = 'x'))"));
    }

    @Test
    public void testKeyOfMapOfEnclosingSubquery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT KEY(t) FROM s.attributes t WHERE KEY(t) = 'k1'))"));
    }

    @Test
    public void testValueOfMapOfEnclosingSubquery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n, IN(s.attributes) t WHERE n.id = s.id AND VALUE(t) = 'v2'))"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithJoinVariableOfEnclosingSubquery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " JOIN s.parent p WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.name = p.name AND n.id = s.id))"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithSecondRootOfEnclosingSubquery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s,"
            + " NestedSubqueryOwner s2 WHERE s.id = e.id AND s2.id = 2"
            + " AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.id = s.id AND n.name = s2.name))"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithSubclassFieldOfEnclosingSubquery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryDetailedOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.name = s.detail AND n.id = 1))"));
    }

    @Test
    public void testExistsInSubqueryCorrelatedWithQuery() {
        assertEquals(3L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e WHERE EXISTS (SELECT s.id FROM NestedSubqueryOwner s"
            + " WHERE s.id = e.id AND EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.id = e.id AND n.name = 'x'))"));
    }

    @Test
    public void testExistsCorrelatedWithQuery() {
        assertEquals(3L, countOf("SELECT COUNT(e) FROM NestedSubqueryOwner e"
            + " WHERE EXISTS (SELECT n.id FROM NestedSubqueryOwner n WHERE n.id = e.id AND n.name = 'x')"));
    }

    @Test
    public void testExistsCorrelatedWithSuperclassFieldOfQuery() {
        assertEquals(1L, countOf("SELECT COUNT(e) FROM NestedSubqueryDetailedOwner e WHERE EXISTS"
            + " (SELECT n.id FROM NestedSubqueryOwner n WHERE n.name = e.name AND n.id <> e.id)"));
    }

    private long countOf(String jpql) {
        return em.createQuery(jpql, Long.class).getSingleResult();
    }
}
