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

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * An AttributeConverter is consulted for null values as well, so a converter
 * may map null to a default in either direction.
 */
public class TestConverterNullValues extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES,
            ConvertNullEntity.class,
            NullDefaultConverter.class);
    }

    private Object rawValue(EntityManager em, int id) {
        return em.createNativeQuery(
            "SELECT CONV_VALUE FROM CONV_NULL_ENTITY WHERE ID = " + id)
            .getSingleResult();
    }

    /**
     * A null attribute must be passed to convertToDatabaseColumn, so the
     * converter's substitute lands in the column.
     */
    public void testNullAttributeIsConverted() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertNullEntity(1, null));
        em.getTransaction().commit();
        em.clear();

        assertEquals(NullDefaultConverter.DB_DEFAULT, rawValue(em, 1));

        // ... and reading it back maps DB_DEFAULT to a null attribute
        ConvertNullEntity found = em.find(ConvertNullEntity.class, 1);
        assertNotNull(found);
        assertNull(found.getValue());
        em.close();
    }

    /**
     * A null column value must be passed to convertToEntityAttribute, so the
     * converter's substitute lands in the attribute. Also asserts that a null
     * returned by convertToDatabaseColumn still stores a null column value.
     */
    public void testNullColumnIsConverted() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertNullEntity(2, NullDefaultConverter.ERASE));
        em.getTransaction().commit();
        em.clear();

        assertNull(rawValue(em, 2));

        ConvertNullEntity found = em.find(ConvertNullEntity.class, 2);
        assertNotNull(found);
        assertEquals(NullDefaultConverter.ENTITY_DEFAULT, found.getValue());
        em.close();
    }

    /**
     * A null in a query means SQL NULL in every construct: the converter is
     * consulted on the store and load paths only. So "IS NULL", a null bound
     * to a parameter and "IS NOT NULL" stay consistent with each other.
     */
    public void testNullInQueryMeansSqlNull() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        // column holds DB_DEFAULT, attribute reads back as null
        em.persist(new ConvertNullEntity(3, null));
        em.persist(new ConvertNullEntity(4, "kept"));
        // column holds null, attribute reads back as ENTITY_DEFAULT
        em.persist(new ConvertNullEntity(5, NullDefaultConverter.ERASE));
        em.getTransaction().commit();
        em.clear();

        assertEquals(List.of(5), ids(em,
            "SELECT e FROM ConvertNullEntity e WHERE e.value IS NULL"));
        assertEquals(List.of(3, 4), ids(em,
            "SELECT e FROM ConvertNullEntity e WHERE e.value IS NOT NULL"
                + " ORDER BY e.id"));

        List<ConvertNullEntity> res = em.createQuery(
            "SELECT e FROM ConvertNullEntity e WHERE e.value = :v",
            ConvertNullEntity.class)
            .setParameter("v", null)
            .getResultList();
        assertEquals("= :nullParam must agree with IS NULL",
            List.of(5), toIds(res));
        em.close();
    }

    /**
     * A bulk update assigning null writes a null column value, whether the
     * null is a literal or a bound parameter.
     */
    public void testNullBulkUpdateMeansSqlNull() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertNullEntity(6, "a"));
        em.persist(new ConvertNullEntity(7, "b"));
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        em.createQuery("UPDATE ConvertNullEntity e SET e.value = NULL"
            + " WHERE e.id = 6").executeUpdate();
        em.createQuery("UPDATE ConvertNullEntity e SET e.value = :v"
            + " WHERE e.id = 7")
            .setParameter("v", null).executeUpdate();
        em.getTransaction().commit();
        em.clear();

        assertNull(rawValue(em, 6));
        assertNull("SET x = :nullParam must agree with SET x = NULL",
            rawValue(em, 7));
        em.close();
    }

    private List<Integer> ids(EntityManager em, String jpql) {
        return toIds(em.createQuery(jpql, ConvertNullEntity.class)
            .getResultList());
    }

    private List<Integer> toIds(List<ConvertNullEntity> res) {
        return res.stream().map(ConvertNullEntity::getId).sorted().toList();
    }
}
