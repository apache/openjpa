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

import org.apache.openjpa.meta.FieldMetaData;

/**
 * An attribute declared <code>@Basic</code> whose type is not serializable is
 * accepted when an auto-apply converter is registered for its type. Before
 * OPENJPA-3073 the persistence unit failed with "isn't supported by declared
 * persistence strategy Basic".
 */
public class TestAutoApplyConvertedBasicAttribute extends AbstractAutoApplyTestCase {

    @Override
    public void setUp() {
        setUpUnit(List.of(UnenhancedAutoApplyBasicEntity.class));
    }

    public void testBasicAttributesAreMappedWithTheirConverter() throws SQLException {
        FieldMetaData sku = persistentField(UnenhancedAutoApplyBasicEntity.class, "sku");
        assertNotNull(sku);
        assertEquals(SkuConverter.class, sku.getConverter());
        assertFalse("the converter maps the value, not serialization", sku.isSerialized());
        FieldMetaData weight = persistentField(UnenhancedAutoApplyBasicEntity.class, "weight");
        assertNotNull(weight);
        assertEquals(WeightConverter.class, weight.getConverter());
        persist();
        assertColumnType(STRING_TYPES, "AA_BASIC", "SKU");
        assertColumnType(NUMBER_TYPES, "AA_BASIC", "WEIGHT");
    }

    public void testRoundTrip() {
        persist();
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyBasicEntity found =
                em.find(UnenhancedAutoApplyBasicEntity.class, 1L);
            assertEquals(new Sku("B-2"), found.getSku());
            assertEquals(new Weight(3L), found.getWeight());
        }
    }

    private void persist() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyBasicEntity e = new UnenhancedAutoApplyBasicEntity();
            e.setId(1L);
            e.setSku(new Sku("B-2"));
            e.setWeight(new Weight(3L));
            em.persist(e);
            em.getTransaction().commit();
        }
    }
}
