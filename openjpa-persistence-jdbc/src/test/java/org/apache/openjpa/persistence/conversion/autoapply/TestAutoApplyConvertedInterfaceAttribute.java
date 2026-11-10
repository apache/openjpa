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
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.apache.openjpa.meta.FieldMetaData;

/**
 * An attribute whose declared type is an interface that is not serializable
 * is mapped through the auto-apply converter of that interface, with or
 * without {@code @Basic}.
 */
public class TestAutoApplyConvertedInterfaceAttribute extends AbstractAutoApplyTestCase {

    @Override
    public void setUp() {
        setUpUnit(List.of(UnenhancedAutoApplyInterfaceEntity.class, ProductCodeConverter.class));
    }

    public void testInterfaceAttributesAreMappedWithTheirConverter() throws SQLException {
        for (String name : List.of("code", "basicCode")) {
            FieldMetaData fmd = persistentField(UnenhancedAutoApplyInterfaceEntity.class, name);
            assertNotNull(name + " must be persistent", fmd);
            assertEquals(ProductCodeConverter.class, fmd.getConverter());
            assertFalse("the converter maps the value, not serialization", fmd.isSerialized());
        }
        persist();
        assertColumnType(STRING_TYPES, "AA_IFACE", "CODE");
        assertColumnType(STRING_TYPES, "AA_IFACE", "BASICCODE");
    }

    public void testRoundTripAndQuery() {
        persist();
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedAutoApplyInterfaceEntity found =
                em.find(UnenhancedAutoApplyInterfaceEntity.class, 1L);
            assertEquals("P-1", found.getCode().value());
            assertEquals("P-2", found.getBasicCode().value());

            List<UnenhancedAutoApplyInterfaceEntity> byCode = em.createQuery(
                "SELECT e FROM UnenhancedAutoApplyInterfaceEntity e WHERE e.code = :code",
                UnenhancedAutoApplyInterfaceEntity.class)
                .setParameter("code", ProductCode.of("P-1"))
                .getResultList();
            assertEquals(1, byCode.size());
        }
    }

    public void testMetamodelShowsBasicAttributes() {
        EntityType<UnenhancedAutoApplyInterfaceEntity> entity =
            emf.getMetamodel().entity(UnenhancedAutoApplyInterfaceEntity.class);
        for (String name : List.of("code", "basicCode")) {
            SingularAttribute<? super UnenhancedAutoApplyInterfaceEntity, ?> attr =
                entity.getSingularAttribute(name);
            assertEquals(ProductCode.class, attr.getJavaType());
            assertEquals(PersistentAttributeType.BASIC, attr.getPersistentAttributeType());
            assertFalse(attr.isAssociation());
        }
    }

    private void persist() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedAutoApplyInterfaceEntity e = new UnenhancedAutoApplyInterfaceEntity();
            e.setId(1L);
            e.setCode(ProductCode.of("P-1"));
            e.setBasicCode(ProductCode.of("P-2"));
            em.persist(e);
            em.getTransaction().commit();
        }
    }
}
