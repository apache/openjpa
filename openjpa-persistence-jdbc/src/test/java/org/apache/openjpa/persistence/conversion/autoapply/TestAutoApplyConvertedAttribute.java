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
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.apache.openjpa.meta.FieldMetaData;

/**
 * An attribute of a type that is not serializable and that is converted by an
 * auto-apply converter only, without any mapping annotation, is a basic
 * attribute (Jakarta Persistence 3.2, section 3.9). Before OPENJPA-3073 it was
 * silently left out of the mapping and its value was never stored.
 */
public class TestAutoApplyConvertedAttribute extends AbstractAutoApplyTestCase {

    @Override
    public void setUp() {
        setUpUnit(List.of(UnenhancedAutoApplyFieldEntity.class,
            UnenhancedAutoApplyPropertyEntity.class, UnenhancedPackaging.class));
    }

    public void testUnannotatedAttributesAreMappedWithTheirConverter() {
        FieldMetaData sku = persistentField(UnenhancedAutoApplyFieldEntity.class, "sku");
        assertNotNull("the auto-apply converted attribute must be mapped", sku);
        assertEquals(SkuConverter.class, sku.getConverter());
        FieldMetaData weight = persistentField(UnenhancedAutoApplyFieldEntity.class, "weight");
        assertNotNull("the auto-apply converted record must be mapped", weight);
        assertEquals(WeightConverter.class, weight.getConverter());
    }

    public void testColumnsHaveTheDatabaseTypeOfTheConverter() throws SQLException {
        persistSample();
        assertColumnType(STRING_TYPES, "AA_FIELD", "SKU");
        assertColumnType(NUMBER_TYPES, "AA_FIELD", "WEIGHT");
        assertColumnType(STRING_TYPES, "AA_FIELD", "BOXSKU");
        assertColumnType(NUMBER_TYPES, "AA_FIELD", "BOXWEIGHT");
        assertColumnType(STRING_TYPES, "AA_PROPERTY", "SKU");
        assertColumnType(NUMBER_TYPES, "AA_PROPERTY", "WEIGHT");
    }

    public void testFieldAccessRoundTrip() {
        persistSample();
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyFieldEntity found = em.find(UnenhancedAutoApplyFieldEntity.class, 1L);
            assertEquals(new Sku("A-1"), found.getSku());
            assertEquals(new Weight(1500L), found.getWeight());
        }
    }

    public void testEmbeddableRoundTrip() {
        persistSample();
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedPackaging packaging = em.find(UnenhancedAutoApplyFieldEntity.class, 1L).getPackaging();
            assertNotNull(packaging);
            assertEquals(new Sku("BOX"), packaging.getBoxSku());
            assertEquals(new Weight(20L), packaging.getBoxWeight());
        }
    }

    public void testPropertyAccessRoundTrip() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyPropertyEntity e = new UnenhancedAutoApplyPropertyEntity();
            e.setId(1L);
            e.setSku(new Sku("P-1"));
            e.setWeight(new Weight(7L));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyPropertyEntity found = em.find(UnenhancedAutoApplyPropertyEntity.class, 1L);
            assertEquals(new Sku("P-1"), found.getSku());
            assertEquals(new Weight(7L), found.getWeight());
        }
    }

    public void testUpdateIsWritten() {
        persistSample();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyFieldEntity found = em.find(UnenhancedAutoApplyFieldEntity.class, 1L);
            found.setSku(new Sku("A-2"));
            found.setWeight(new Weight(1600L));
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyFieldEntity found = em.find(UnenhancedAutoApplyFieldEntity.class, 1L);
            assertEquals(new Sku("A-2"), found.getSku());
            assertEquals(new Weight(1600L), found.getWeight());
        }
    }

    public void testQueryWithParameters() {
        persistSample();
        try (EntityManager em = emf.createEntityManager()) {
            List<UnenhancedAutoApplyFieldEntity> bySku = em.createQuery(
                "SELECT e FROM UnenhancedAutoApplyFieldEntity e WHERE e.sku = :sku",
                UnenhancedAutoApplyFieldEntity.class)
                .setParameter("sku", new Sku("A-1")).getResultList();
            assertEquals(1, bySku.size());
            List<UnenhancedAutoApplyFieldEntity> byWeight = em.createQuery(
                "SELECT e FROM UnenhancedAutoApplyFieldEntity e WHERE e.weight = :weight",
                UnenhancedAutoApplyFieldEntity.class)
                .setParameter("weight", new Weight(1500L)).getResultList();
            assertEquals(1, byWeight.size());
            assertTrue(em.createQuery(
                "SELECT e FROM UnenhancedAutoApplyFieldEntity e WHERE e.sku = :sku",
                UnenhancedAutoApplyFieldEntity.class)
                .setParameter("sku", new Sku("none")).getResultList().isEmpty());
            List<Sku> skus = em.createQuery(
                "SELECT e.packaging.boxSku FROM UnenhancedAutoApplyFieldEntity e "
                + "WHERE e.packaging.boxWeight = :w", Sku.class)
                .setParameter("w", new Weight(20L)).getResultList();
            assertEquals(List.of(new Sku("BOX")), skus);
        }
    }

    /**
     * <code>@Convert(disableConversion = true)</code> keeps the auto-apply
     * converter off the attribute, so a type that is not serializable has no
     * persistence strategy, as before.
     */
    public void testDisabledConversionIsNotMapped() throws SQLException {
        assertNull(persistentField(UnenhancedAutoApplyFieldEntity.class, "unconvertedSku"));
        persistSample();
        assertNull(columnType("AA_FIELD", "UNCONVERTEDSKU"));
        try (EntityManager em = emf.createEntityManager()) {
            assertNull(em.find(UnenhancedAutoApplyFieldEntity.class, 1L).getUnconvertedSku());
        }
    }

    /**
     * An attribute of a type that is neither serializable nor converted stays
     * outside of the mapping.
     */
    public void testUnconvertedNonSerializableAttributeIsNotMapped() throws SQLException {
        assertNull(persistentField(UnenhancedAutoApplyFieldEntity.class, "opaque"));
        persistSample();
        assertNull(columnType("AA_FIELD", "OPAQUE"));
        try (EntityManager em = emf.createEntityManager()) {
            assertNull(em.find(UnenhancedAutoApplyFieldEntity.class, 1L).getOpaque());
        }
    }

    public void testMetamodelShowsBasicAttributes() {
        EntityType<UnenhancedAutoApplyFieldEntity> entity =
            emf.getMetamodel().entity(UnenhancedAutoApplyFieldEntity.class);
        assertBasic(entity.getSingularAttribute("sku"), Sku.class);
        assertBasic(entity.getSingularAttribute("weight"), Weight.class);
        EmbeddableType<UnenhancedPackaging> embeddable =
            emf.getMetamodel().embeddable(UnenhancedPackaging.class);
        assertBasic(embeddable.getSingularAttribute("boxSku"), Sku.class);
        assertBasic(embeddable.getSingularAttribute("boxWeight"), Weight.class);
        EntityType<UnenhancedAutoApplyPropertyEntity> property =
            emf.getMetamodel().entity(UnenhancedAutoApplyPropertyEntity.class);
        assertBasic(property.getSingularAttribute("sku"), Sku.class);
        assertBasic(property.getSingularAttribute("weight"), Weight.class);
    }

    private static void assertBasic(SingularAttribute<?, ?> attr, Class<?> type) {
        assertEquals(PersistentAttributeType.BASIC, attr.getPersistentAttributeType());
        assertEquals(type, attr.getJavaType());
    }

    private void persistSample() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyFieldEntity e = new UnenhancedAutoApplyFieldEntity();
            e.setId(1L);
            e.setSku(new Sku("A-1"));
            e.setWeight(new Weight(1500L));
            e.setUnconvertedSku(new Sku("C-3"));
            e.setOpaque(new Opaque("hidden"));
            e.setPackaging(new UnenhancedPackaging(new Sku("BOX"), new Weight(20L)));
            em.persist(e);
            em.getTransaction().commit();
        }
    }
}
