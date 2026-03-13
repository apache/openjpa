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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for CriteriaUpdate and CriteriaDelete (JPA 2.1).
 */
public class TestCriteriaUpdateDelete extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
    }

    private void insertTestData(EntityManager em) {
        em.getTransaction().begin();
        AllFieldTypes e1 = new AllFieldTypes();
        e1.setStringField("Alice");
        e1.setIntField(10);
        em.persist(e1);

        AllFieldTypes e2 = new AllFieldTypes();
        e2.setStringField("Bob");
        e2.setIntField(20);
        em.persist(e2);

        AllFieldTypes e3 = new AllFieldTypes();
        e3.setStringField("Charlie");
        e3.setIntField(30);
        em.persist(e3);
        em.getTransaction().commit();
    }

    /**
     * CriteriaDelete should delete all rows when no WHERE clause.
     */
    public void testCriteriaDeleteAll() {
        EntityManager em = emf.createEntityManager();
        insertTestData(em);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<AllFieldTypes> cd = cb.createCriteriaDelete(AllFieldTypes.class);
        cd.from(AllFieldTypes.class);

        em.getTransaction().begin();
        int deleted = em.createQuery(cd).executeUpdate();
        em.getTransaction().commit();

        assertEquals(3, deleted);

        // Verify all deleted
        long count = em.createQuery("SELECT COUNT(e) FROM AllFieldTypes e", Long.class)
            .getSingleResult();
        assertEquals(0L, count);
        em.close();
    }

    /**
     * CriteriaDelete with WHERE clause.
     */
    public void testCriteriaDeleteWithWhere() {
        EntityManager em = emf.createEntityManager();
        insertTestData(em);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<AllFieldTypes> cd = cb.createCriteriaDelete(AllFieldTypes.class);
        Root<AllFieldTypes> root = cd.from(AllFieldTypes.class);
        cd.where(cb.equal(root.get("stringField"), "Alice"));

        em.getTransaction().begin();
        int deleted = em.createQuery(cd).executeUpdate();
        em.getTransaction().commit();

        assertEquals(1, deleted);

        long count = em.createQuery("SELECT COUNT(e) FROM AllFieldTypes e", Long.class)
            .getSingleResult();
        assertEquals(2L, count);
        em.close();
    }

    /**
     * CriteriaUpdate should update matching rows.
     */
    public void testCriteriaUpdateWithWhere() {
        EntityManager em = emf.createEntityManager();
        insertTestData(em);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<AllFieldTypes> cu = cb.createCriteriaUpdate(AllFieldTypes.class);
        Root<AllFieldTypes> root = cu.from(AllFieldTypes.class);
        cu.set(root.get("stringField"), "Updated");
        cu.where(cb.equal(root.get("stringField"), "Alice"));

        em.getTransaction().begin();
        int updated = em.createQuery(cu).executeUpdate();
        em.getTransaction().commit();

        assertEquals(1, updated);

        em.clear();
        long count = em.createQuery(
            "SELECT COUNT(e) FROM AllFieldTypes e WHERE e.stringField = 'Updated'", Long.class)
            .getSingleResult();
        assertEquals(1L, count);
        em.close();
    }

    /**
     * CriteriaUpdate should update all rows when no WHERE.
     */
    public void testCriteriaUpdateAll() {
        EntityManager em = emf.createEntityManager();
        insertTestData(em);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<AllFieldTypes> cu = cb.createCriteriaUpdate(AllFieldTypes.class);
        Root<AllFieldTypes> root = cu.from(AllFieldTypes.class);
        cu.set(root.get("intField"), 99);

        em.getTransaction().begin();
        int updated = em.createQuery(cu).executeUpdate();
        em.getTransaction().commit();

        assertEquals(3, updated);

        em.clear();
        long count = em.createQuery(
            "SELECT COUNT(e) FROM AllFieldTypes e WHERE e.intField = 99", Long.class)
            .getSingleResult();
        assertEquals(3L, count);
        em.close();
    }

    /**
     * CriteriaUpdate with set(String attributeName, Object value).
     */
    public void testCriteriaUpdateSetByAttributeName() {
        EntityManager em = emf.createEntityManager();
        insertTestData(em);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<AllFieldTypes> cu = cb.createCriteriaUpdate(AllFieldTypes.class);
        cu.from(AllFieldTypes.class);
        cu.set("stringField", "NameUpdated");

        em.getTransaction().begin();
        int updated = em.createQuery(cu).executeUpdate();
        em.getTransaction().commit();

        assertEquals(3, updated);

        em.clear();
        long count = em.createQuery(
            "SELECT COUNT(e) FROM AllFieldTypes e WHERE e.stringField = 'NameUpdated'", Long.class)
            .getSingleResult();
        assertEquals(3L, count);
        em.close();
    }
}
