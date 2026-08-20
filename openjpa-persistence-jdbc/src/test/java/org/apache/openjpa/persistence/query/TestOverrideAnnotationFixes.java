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
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPA TCK override/annotation fixes:
 * 1. Short arithmetic operand result type (should be Integer, not Long)
 * 2. Eager collection loading with runtime-enhanced entities
 * 3. Read-only JoinColumn with unmanaged entity reference
 */
public class TestOverrideAnnotationFixes extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(ShortFieldEntity.class,
              EagerParentEntity.class, EagerChildEntity.class,
              ReadOnlyJoinDept.class, ReadOnlyJoinEmployee.class,
              DROP_TABLES);
    }

    /**
     * Per JPA 3.2 spec section 4.8.5, arithmetic on Short operands
     * should produce Integer results, not Long.
     */
    public void testShortAdditionReturnsInteger() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ShortFieldEntity e = new ShortFieldEntity(1, (short) 10, Short.valueOf((short) 20));
            em.persist(e);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Object result = em.createQuery(
                "SELECT (s.basicShort + 1) FROM ShortFieldEntity s WHERE s.id = 1")
                .getSingleResult();
            assertTrue("Expected Integer but got " + result.getClass().getName(),
                result instanceof Integer);
            assertEquals(11, ((Integer) result).intValue());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    /**
     * Per JPA 3.2 spec section 4.8.5, arithmetic on Short (boxed) operands
     * should also produce Integer results.
     */
    public void testBoxedShortSubtractionReturnsInteger() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ShortFieldEntity e = new ShortFieldEntity(2, (short) 5, Short.valueOf((short) 30));
            em.persist(e);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Object result = em.createQuery(
                "SELECT (s.basicBigShort - 1) FROM ShortFieldEntity s WHERE s.id = 2")
                .getSingleResult();
            assertTrue("Expected Integer but got " + result.getClass().getName(),
                result instanceof Integer);
            assertEquals(29, ((Integer) result).intValue());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    /**
     * Per JPA 3.2 spec, Short * Integer literal should return Integer.
     */
    public void testShortMultiplicationReturnsInteger() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ShortFieldEntity e = new ShortFieldEntity(3, (short) 7, Short.valueOf((short) 15));
            em.persist(e);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Object result = em.createQuery(
                "SELECT (s.basicShort * 2) FROM ShortFieldEntity s WHERE s.id = 3")
                .getSingleResult();
            assertTrue("Expected Integer but got " + result.getClass().getName(),
                result instanceof Integer);
            assertEquals(14, ((Integer) result).intValue());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    /**
     * Verify that Long + integer literal still returns Long.
     * This ensures the integer literal type change doesn't break Long arithmetic.
     */
    public void testLongAdditionStillReturnsLong() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            EagerParentEntity p = new EagerParentEntity(999L, "test");
            em.persist(p);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Object result = em.createQuery(
                "SELECT (p.id + 1) FROM EagerParentEntity p WHERE p.id = 999")
                .getSingleResult();
            assertTrue("Expected Long but got " + result.getClass().getName(),
                result instanceof Long);
            assertEquals(1000L, ((Long) result).longValue());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    /**
     * Test that eagerly loaded collections work correctly with
     * runtime-enhanced (unenhanced) entities. Previously, JDBCStoreManager
     * used getCachedMetaData(pc.getClass()) which returned null for
     * runtime-generated subclasses, causing NPE.
     */
    public void testEagerCollectionLoadWithQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            EagerParentEntity parent = new EagerParentEntity(1L, "Parent1");
            EagerChildEntity child1 = new EagerChildEntity(10L, "Child1");
            EagerChildEntity child2 = new EagerChildEntity(11L, "Child2");
            parent.addChild(child1);
            parent.addChild(child2);
            em.persist(parent);
            em.getTransaction().commit();

            // Clear cache to force loading from DB
            em.clear();
            emf.getCache().evictAll();

            em.getTransaction().begin();
            Query q = em.createQuery(
                "SELECT p FROM EagerParentEntity p WHERE p.id = :id");
            q.setParameter("id", 1L);
            EagerParentEntity loaded = (EagerParentEntity) q.getSingleResult();

            assertNotNull("Parent should not be null", loaded);
            assertEquals("Parent1", loaded.getName());
            assertNotNull("Children should not be null", loaded.getChildren());
            assertEquals("Should have 2 children", 2, loaded.getChildren().size());
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    /**
     * Test that persisting an entity with a read-only JoinColumn
     * (insertable=false, updatable=false) does not throw when the
     * referenced entity is unmanaged/detached. The JPA spec says
     * the FK column should not be written, so the reference should
     * be allowed but ignored during flush.
     */
    public void testReadOnlyJoinColumnWithUnmanagedReference() {
        // First, persist the department
        EntityManager em = emf.createEntityManager();
        ReadOnlyJoinDept dept;
        try {
            em.getTransaction().begin();
            dept = new ReadOnlyJoinDept(1, "Engineering");
            em.persist(dept);
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        // In a new EntityManager (so dept is detached/unmanaged),
        // create an employee referencing the department
        em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ReadOnlyJoinEmployee emp = new ReadOnlyJoinEmployee(100, "Smith");
            // dept is from a closed EM - it's effectively unmanaged
            emp.setDepartment(dept);
            em.persist(emp);
            // This flush should NOT throw, because the JoinColumn is
            // insertable=false, updatable=false
            em.flush();
            em.getTransaction().commit();

            // Verify the employee was persisted
            em.clear();
            ReadOnlyJoinEmployee loaded = em.find(ReadOnlyJoinEmployee.class, 100);
            assertNotNull("Employee should be persisted", loaded);
            assertEquals("Smith", loaded.getLastName());
            // Department FK was not insertable, so it should be null
            assertNull("Department should be null (FK not insertable)",
                loaded.getDepartment());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
}
