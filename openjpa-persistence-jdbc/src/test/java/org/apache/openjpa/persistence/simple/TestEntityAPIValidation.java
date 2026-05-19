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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA TCK entity API requirements:
 * - find() throws IAE for wrong PK type
 * - getReference() throws IAE for non-entity, wrong PK type, null PK
 * - Constructor expression queries match constructors with primitive params
 */
public class TestEntityAPIValidation extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CoffeeBean.class, DROP_TABLES);
    }

    /**
     * find(Class, Object) should throw IAE when PK type is wrong.
     * Per JPA spec, find() must throw IllegalArgumentException if the
     * second argument is not a valid type for the entity's primary key.
     * Mirrors TCK entityAPITest4: Coffee has Integer PK, Long is not valid.
     */
    public void testFindWithWrongPKTypeThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            // Long is not the declared PK type (Integer)
            em.find(CoffeeBean.class, 55L);
            fail("Expected IllegalArgumentException for wrong PK type "
                + "(Long instead of Integer)");
        } catch (IllegalArgumentException e) {
            // expected per spec
        } finally {
            em.close();
        }
    }

    /**
     * find(Class, null) should throw IAE for the 4-arg find variant
     * (which is used by find with LockModeType or properties).
     */
    public void testFindWithNullPKThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            em.find(CoffeeBean.class, null, LockModeType.NONE);
            fail("Expected IllegalArgumentException for null PK");
        } catch (IllegalArgumentException e) {
            // expected per spec
        } finally {
            em.close();
        }
    }

    /**
     * getReference(Class, wrongType) should throw IAE.
     * Mirrors TCK getReferenceExceptionsTest pass2 with Long vs Integer PK.
     */
    public void testGetReferenceWrongPKTypeLongThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            // Long is not a valid PK type for an Integer PK entity
            em.getReference(CoffeeBean.class, 55L);
            fail("Expected IllegalArgumentException for wrong PK type");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    /**
     * getReference() should throw IAE when first arg is not an entity.
     * Mirrors TCK getReferenceExceptionsTest pass1.
     */
    public void testGetReferenceNonEntityThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getReference(CoffeeBeanDTO.class, 1);
            fail("Expected IllegalArgumentException for non-entity class");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    /**
     * getReference() should throw IAE when PK type is wrong.
     * Mirrors TCK getReferenceExceptionsTest pass2.
     */
    public void testGetReferenceWrongPKTypeThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            // String is not a valid PK type for an Integer PK entity
            em.getReference(CoffeeBean.class, "1");
            fail("Expected IllegalArgumentException for wrong PK type");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    /**
     * getReference() should throw IAE when PK is null.
     * Mirrors TCK getReferenceExceptionsTest pass3.
     */
    public void testGetReferenceNullPKThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getReference(CoffeeBean.class, null);
            fail("Expected IllegalArgumentException for null PK");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    /**
     * Constructor expression query with primitive float parameter should work.
     * Mirrors TCK entityAPITest14/15: SELECT NEW Coffee(c.id, c.brandName, c.price).
     * The constructor takes (Integer, String, float) but the query yields
     * (Integer, String, Float) -- the FillStrategy must handle boxing.
     */
    public void testConstructorExpressionWithPrimitiveParam() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Insert test data
            CoffeeBean c = new CoffeeBean(1, "espresso", 3.50f);
            em.persist(c);
            em.flush();

            // Constructor expression query - the constructor takes (Integer, String, float)
            // but the query produces boxed Float for the price column
            List<CoffeeBean> result = em.createQuery(
                "SELECT NEW org.apache.openjpa.persistence.simple.CoffeeBean"
                    + "(c.id, c.brandName, c.price) FROM CoffeeBean c "
                    + "WHERE c.price <> 0",
                CoffeeBean.class).getResultList();

            assertFalse("Should have results", result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(Integer.valueOf(1), result.get(0).getId());
            assertEquals("espresso", result.get(0).getBrandName());

            em.getTransaction().rollback();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Constructor expression with a non-entity result class should work.
     * Mirrors TCK entityAPITest16: SELECT NEW Bar(c.id, c.brandName, c.price).
     * The non-entity constructor takes (Integer, String, float).
     */
    public void testConstructorExpressionNonEntityWithPrimitiveParam() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            CoffeeBean c = new CoffeeBean(5, "mocha", 4.75f);
            em.persist(c);
            em.flush();

            List<CoffeeBeanDTO> result = em.createQuery(
                "SELECT NEW org.apache.openjpa.persistence.simple.CoffeeBeanDTO"
                    + "(c.id, c.brandName, c.price) FROM CoffeeBean c "
                    + "WHERE c.brandName = 'mocha'",
                CoffeeBeanDTO.class).getResultList();

            assertFalse("Should have results", result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(Integer.valueOf(5), result.get(0).getId());
            assertEquals("mocha", result.get(0).getBrandName());
            assertEquals(Float.valueOf(4.75f), result.get(0).getPrice());

            em.getTransaction().rollback();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
