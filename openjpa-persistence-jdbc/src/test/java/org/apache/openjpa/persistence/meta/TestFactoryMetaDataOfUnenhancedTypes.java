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
package org.apache.openjpa.persistence.meta;

import java.util.Map;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.persistence.test.PersistenceTestCase;

/**
 * The metadata a factory hands out must be available for types left to
 * runtime enhancement before the factory created its first EntityManager.
 *<p>
 * The runtime subclass of such a type is registered in the JVM-wide
 * {@link PCRegistry}, so each test uses an entity of its own that no other
 * test persists, and asserts that it is not registered yet.
 */
public class TestFactoryMetaDataOfUnenhancedTypes extends PersistenceTestCase {

    private EntityManagerFactory emf;

    @Override
    public void tearDown() throws Exception {
        closeEMF(emf);
        emf = null;
        super.tearDown();
    }

    private EntityManagerFactory createUnenhancedEMF(Class<?> type) {
        assertFalse(type.getName() + " must not have been prepared by an earlier EntityManager",
            PCRegistry.isRegistered(type));
        emf = createEMF(type, DROP_TABLES,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false");
        return emf;
    }

    public void testMetamodelBeforeFirstEntityManager() {
        createUnenhancedEMF(UnenhancedFactoryMetamodelEntity.class);

        EntityType<UnenhancedFactoryMetamodelEntity> entity =
            emf.getMetamodel().entity(UnenhancedFactoryMetamodelEntity.class);
        assertEquals(String.class, entity.getSingularAttribute("name").getJavaType());
        assertEquals(long.class, entity.getId(long.class).getJavaType());

        // the EntityManagers created afterwards work on the prepared type
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedFactoryMetamodelEntity e = new UnenhancedFactoryMetamodelEntity();
            e.setId(1L);
            e.setName("one");
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("one", em.find(UnenhancedFactoryMetamodelEntity.class, 1L).getName());
        }
    }

    public void testCriteriaBuilderBeforeFirstEntityManager() {
        createUnenhancedEMF(UnenhancedFactoryCriteriaEntity.class);

        CriteriaBuilder cb = emf.getCriteriaBuilder();
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Root<UnenhancedFactoryCriteriaEntity> root = q.from(UnenhancedFactoryCriteriaEntity.class);
        q.select(root.get("name"));
        assertEquals(UnenhancedFactoryCriteriaEntity.class, root.getJavaType());
    }

    public void testNamedEntityGraphsBeforeFirstEntityManager() {
        createUnenhancedEMF(UnenhancedFactoryEntityGraphEntity.class);

        Map<String, EntityGraph<? extends UnenhancedFactoryEntityGraphEntity>> graphs =
            emf.getNamedEntityGraphs(UnenhancedFactoryEntityGraphEntity.class);
        assertTrue(graphs.keySet().toString(),
            graphs.containsKey("UnenhancedFactoryEntityGraphEntity.name"));
    }

    public void testIdentifierBeforeFirstEntityManager() {
        createUnenhancedEMF(UnenhancedFactoryIdentifierEntity.class);

        UnenhancedFactoryIdentifierEntity e = new UnenhancedFactoryIdentifierEntity();
        e.setId(7L);
        assertEquals(7L, emf.getPersistenceUnitUtil().getIdentifier(e));
    }

    public void testNamedQueriesBeforeFirstEntityManager() {
        createUnenhancedEMF(UnenhancedFactoryNamedQueryEntity.class);

        Map<String, TypedQueryReference<UnenhancedFactoryNamedQueryEntity>> queries =
            emf.getNamedQueries(UnenhancedFactoryNamedQueryEntity.class);
        assertTrue(queries.keySet().toString(),
            queries.containsKey("UnenhancedFactoryNamedQueryEntity.all"));
    }
}
