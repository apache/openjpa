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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA 3.2 StaticMetamodel constants: EntityType class_
 * and QUERY_ string constants for named queries.
 */
public class TestMetamodelConstants extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, MetamodelConstantsEntity.class);
    }

    public void testQueryConstantsExist() {
        assertEquals("MetamodelConstantsEntity.findAll",
            MetamodelConstantsEntity_.QUERY_METAMODELCONSTANTSENTITY_FINDALL);
        assertEquals("MetamodelConstantsEntity.findByName",
            MetamodelConstantsEntity_.QUERY_METAMODELCONSTANTSENTITY_FINDBYNAME);
        assertEquals("MetamodelConstantsEntity.findNative",
            MetamodelConstantsEntity_.QUERY_METAMODELCONSTANTSENTITY_FINDNATIVE);
    }

    public void testResultSetMappingConstantExists() {
        assertEquals("MetamodelConstantsEntity.resultMapping",
            MetamodelConstantsEntity_.MAPPING_METAMODELCONSTANTSENTITY_RESULTMAPPING);
    }

    public void testQueryConstantsArePublicStaticFinal() throws Exception {
        Field f = MetamodelConstantsEntity_.class
            .getDeclaredField("QUERY_METAMODELCONSTANTSENTITY_FINDALL");
        int mod = f.getModifiers();
        assertTrue("Should be public", Modifier.isPublic(mod));
        assertTrue("Should be static", Modifier.isStatic(mod));
        assertTrue("Should be final", Modifier.isFinal(mod));
        assertEquals(String.class, f.getType());
    }

    public void testEntityTypeFieldExists() throws Exception {
        Field f = MetamodelConstantsEntity_.class.getDeclaredField("class_");
        int mod = f.getModifiers();
        assertTrue("Should be public", Modifier.isPublic(mod));
        assertTrue("Should be static", Modifier.isStatic(mod));
        assertTrue("Should be volatile", Modifier.isVolatile(mod));
        assertEquals(EntityType.class, f.getType());
    }

    public void testQueryConstantsUsableWithCreateNamedQuery() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        MetamodelConstantsEntity e = new MetamodelConstantsEntity();
        e.setId(1);
        e.setName("test");
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        TypedQuery<MetamodelConstantsEntity> q = em.createNamedQuery(
            MetamodelConstantsEntity_.QUERY_METAMODELCONSTANTSENTITY_FINDALL,
            MetamodelConstantsEntity.class);
        assertEquals(1, q.getResultList().size());

        q = em.createNamedQuery(
            MetamodelConstantsEntity_.QUERY_METAMODELCONSTANTSENTITY_FINDBYNAME,
            MetamodelConstantsEntity.class);
        q.setParameter("name", "test");
        assertEquals(1, q.getResultList().size());
        em.close();
    }
}
