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
package org.apache.openjpa.persistence.conversion.autoapply;

import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;

/**
 * <code>MapAutoApplyConvertedFields=false</code> restores the 4.1.x mapping:
 * an attribute whose type is not serializable and that has no mapping
 * annotation is left out of the mapping even though an auto-apply converter
 * is registered for its type, and its value is not stored. An attribute
 * declared <code>@Basic</code> still gets its converter, since 4.1.x rejected
 * it.
 */
public class TestAutoApplyConvertedAttributeCompat extends AbstractAutoApplyTestCase {

    @Override
    public void setUp() {
        setUpUnit(List.of(UnenhancedAutoApplyCompatEntity.class,
            UnenhancedAutoApplyBasicEntity.class),
            "openjpa.Compatibility", "MapAutoApplyConvertedFields=false");
    }

    public void testUnannotatedAttributesAreNotMapped() {
        assertNull(persistentField(UnenhancedAutoApplyCompatEntity.class, "sku"));
        assertNull(persistentField(UnenhancedAutoApplyCompatEntity.class, "weight"));
    }

    public void testValuesAreNotStored() throws SQLException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyCompatEntity e = new UnenhancedAutoApplyCompatEntity();
            e.setId(1L);
            e.setSku(new Sku("A-1"));
            e.setWeight(new Weight(1500L));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyCompatEntity found =
                em.find(UnenhancedAutoApplyCompatEntity.class, 1L);
            assertNull(found.getSku());
            assertNull(found.getWeight());
        }
        assertNull(columnType("AA_COMPAT", "SKU"));
        assertNull(columnType("AA_COMPAT", "WEIGHT"));
    }

    public void testMetamodelHasNoSuchAttributes() {
        List<String> names = emf.getMetamodel()
            .entity(UnenhancedAutoApplyCompatEntity.class).getAttributes().stream()
            .map(Attribute::getName).toList();
        assertEquals(List.of("id"), names);
    }

    public void testBasicAttributesAreStillConverted() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyBasicEntity e = new UnenhancedAutoApplyBasicEntity();
            e.setId(1L);
            e.setSku(new Sku("B-2"));
            e.setWeight(new Weight(3L));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyBasicEntity found =
                em.find(UnenhancedAutoApplyBasicEntity.class, 1L);
            assertEquals(new Sku("B-2"), found.getSku());
            assertEquals(new Weight(3L), found.getWeight());
        }
    }
}
