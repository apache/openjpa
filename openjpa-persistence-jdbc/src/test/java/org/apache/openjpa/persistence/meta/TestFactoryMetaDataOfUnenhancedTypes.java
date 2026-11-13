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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.test.PersistenceTestCase;

/**
 * The metadata a factory hands out must be available for types left to
 * runtime enhancement before the factory created its first EntityManager.
 *<p>
 * Preparing such a type registers it in the JVM-wide {@link PCRegistry}
 * together with its runtime subclass, so each test uses an entity of its own
 * that no other test prepares, and asserts that it is not registered yet.
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
        assertFalse(type.getName() + " must be left to runtime enhancement",
            PersistenceCapable.class.isAssignableFrom(type));
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
        Class<?> subclass = runtimeSubclass(UnenhancedFactoryMetamodelEntity.class);

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
        // and did not prepare it a second time
        assertSame(subclass, runtimeSubclass(UnenhancedFactoryMetamodelEntity.class));
    }

    public void testConcurrentFirstCallers() throws Exception {
        createUnenhancedEMF(UnenhancedFactoryConcurrentEntity.class);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // each caller reports the runtime subclass it saw once it was done
            Callable<Class<?>> metamodel = () -> {
                start.await();
                assertNotNull(emf.getMetamodel().entity(UnenhancedFactoryConcurrentEntity.class));
                return runtimeSubclass(UnenhancedFactoryConcurrentEntity.class);
            };
            Callable<Class<?>> entityManager = () -> {
                start.await();
                try (EntityManager em = emf.createEntityManager()) {
                    assertNotNull(em.getMetamodel().entity(UnenhancedFactoryConcurrentEntity.class));
                }
                return runtimeSubclass(UnenhancedFactoryConcurrentEntity.class);
            };
            Future<Class<?>> first = pool.submit(metamodel);
            Future<Class<?>> second = pool.submit(entityManager);
            start.countDown();
            Class<?> subclass = first.get(30, TimeUnit.SECONDS);
            assertSame(subclass, second.get(30, TimeUnit.SECONDS));
            assertSame(subclass, runtimeSubclass(UnenhancedFactoryConcurrentEntity.class));
        } finally {
            pool.shutdownNow();
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

    public void testIdentifierAfterClose() {
        createUnenhancedEMF(UnenhancedFactoryClosedEntity.class);
        PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
        emf.close();

        // a closed factory must not prepare the type any more
        UnenhancedFactoryClosedEntity e = new UnenhancedFactoryClosedEntity();
        e.setId(3L);
        try {
            util.getIdentifier(e);
            fail("getIdentifier() of an unprepared type on a closed factory must fail");
        } catch (IllegalArgumentException expected) {
            // the type is not manageable
        }
        assertFalse(PCRegistry.isRegistered(UnenhancedFactoryClosedEntity.class));
    }

    /**
     * The runtime subclass the PCRegistry holds for the given type. Preparing
     * the type again would generate and register another one.
     */
    private static Class<?> runtimeSubclass(Class<?> type) {
        assertTrue(type.getName() + " must have been prepared", PCRegistry.isRegistered(type));
        Class<?> subclass = PCRegistry.getPCType(type);
        assertNotNull(subclass);
        assertTrue(subclass.getName(), PCEnhancer.isPCSubclassName(subclass.getName()));
        return subclass;
    }
}
