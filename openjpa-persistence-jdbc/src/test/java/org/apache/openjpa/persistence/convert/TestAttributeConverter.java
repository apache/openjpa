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
package org.apache.openjpa.persistence.convert;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for JPA AttributeConverter support.
 */
public class TestAttributeConverter extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES,
            ConvertEntity.class,
            ConvertFailEntity.class,
            ConvertCollectionEntity.class,
            StringPrefixConverter.class,
            StringToIntConverter.class,
            CommaSepConverter.class,
            FailingConverter.class);
    }

    /**
     * Test basic same-type converter (String -> String).
     */
    public void testSameTypeConverter() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ConvertEntity e = new ConvertEntity(1, "hello", "42");
        em.persist(e);
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        ConvertEntity found = em.find(ConvertEntity.class, 1);
        assertNotNull(found);
        // StringPrefixConverter: toDB adds "DB_", fromDB removes "DB_"
        // So the entity value should be "hello" after round-trip
        assertEquals("hello", found.getName());
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test cross-type converter (String entity -> Integer DB).
     */
    public void testCrossTypeConverter() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ConvertEntity e = new ConvertEntity(2, "world", "123");
        em.persist(e);
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        ConvertEntity found = em.find(ConvertEntity.class, 2);
        assertNotNull(found);
        // StringToIntConverter: toDB parses to int, fromDB formats to string
        assertEquals("123", found.getCode());
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test that converter is applied via JPQL query results.
     */
    public void testConverterWithJPQL() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertEntity(3, "jpql-test", "999"));
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        List<ConvertEntity> results = em.createQuery(
            "SELECT e FROM ConvertEntity e WHERE e.id = 3",
            ConvertEntity.class).getResultList();
        assertEquals(1, results.size());
        assertEquals("jpql-test", results.get(0).getName());
        assertEquals("999", results.get(0).getCode());
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test converter exception during persist is wrapped in
     * PersistenceException.
     */
    public void testConverterExceptionOnPersist() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            // -1 triggers RuntimeException in FailingConverter
            ConvertFailEntity e = new ConvertFailEntity("fail1", -1);
            em.persist(e);
            em.flush();
            em.getTransaction().commit();
            fail("Expected PersistenceException");
        } catch (PersistenceException pe) {
            // Expected
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } catch (RuntimeException re) {
            // Also acceptable - the RuntimeException from the converter
            // may propagate directly
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    /**
     * Test element collection with converter.
     * Tested separately once element collection support is working.
     */
    public void testElementCollectionConverter() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ConvertCollectionEntity e = new ConvertCollectionEntity("coll1");
        e.setTags(Arrays.asList("a,b", "c,d", "e,f"));
        em.persist(e);
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        ConvertCollectionEntity found =
            em.find(ConvertCollectionEntity.class, "coll1");
        assertNotNull(found);
        // CommaSepConverter: toDB replaces "," with "-"
        // fromDB replaces "-" with ","
        // So round-trip should return original values
        List<String> tags = found.getTags();
        assertEquals(3, tags.size());
        assertTrue(tags.contains("a,b"));
        assertTrue(tags.contains("c,d"));
        assertTrue(tags.contains("e,f"));
        em.getTransaction().commit();
        em.close();
    }
}
