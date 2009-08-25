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
package org.apache.openjpa.persistence.cache.jpa;

import java.lang.reflect.Modifier;

import javax.persistence.Cache;
import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.cache.jpa.model.CacheEntity;
import org.apache.openjpa.persistence.cache.jpa.model.CacheableEntity;
import org.apache.openjpa.persistence.cache.jpa.model.NegatedCachableEntity;
import org.apache.openjpa.persistence.cache.jpa.model.NegatedUncacheableEntity;
import org.apache.openjpa.persistence.cache.jpa.model.UncacheableEntity;
import org.apache.openjpa.persistence.cache.jpa.model.UnspecifiedEntity;
import org.apache.openjpa.persistence.cache.jpa.model.XmlCacheableEntity;
import org.apache.openjpa.persistence.cache.jpa.model.XmlUncacheableEntity;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

public abstract class AbstractJPACacheTestCase extends AbstractPersistenceTestCase {
    public abstract OpenJPAEntityManagerFactorySPI getEntityManagerFactory();

    private static Class<?>[] persistentTypes =
        { CacheableEntity.class, UncacheableEntity.class, UnspecifiedEntity.class, 
            NegatedCachableEntity.class, NegatedUncacheableEntity.class, XmlCacheableEntity.class,
            XmlUncacheableEntity.class };

    public void populate() throws IllegalAccessException, InstantiationException {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        for (Class<?> clss : persistentTypes) {
            if (!Modifier.isAbstract(clss.getModifiers())) {
                CacheEntity ce = (CacheEntity) clss.newInstance();
                ce.setId(1);
                em.persist(ce);
            }
        }
        em.getTransaction().commit();
        em.close();
    }

    public OpenJPAEntityManagerFactorySPI createEntityManagerFactory(String puName) {
        OpenJPAEntityManagerFactorySPI emf =
            (OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.createEntityManagerFactory(puName,
                "META-INF/caching-persistence.xml", getPropertiesMap("openjpa.DataCache", "true",
                    "openjpa.RemoteCommitProvider", "sjvm", persistentTypes));
        return emf;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // populate once per test method in case we add more methods
        cleanDatabase();
        populate();
    }

    public void cleanDatabase() throws Exception {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        for (Class<?> clss : persistentTypes) {
            if (!Modifier.isAbstract(clss.getModifiers())) {
                em.createQuery("Delete from " + clss.getSimpleName()).executeUpdate();
            }
        }
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Assert whether the cache contains the expected results.
     * 
     * @param cache
     *            The JPA Cache to verify
     * @param expectCacheables
     *            Whether entities with @Cacheable(true) should be in the cache
     *            (almost always true)
     * @param expectUncacheables
     *            Whether entities with @Cacheable(false) should be in the cache
     *            (almost always false)
     * @param expectUnspecified
     *            Whether entities with no @Cacheable annotation should be in
     *            the cache (varies per config).
     */
    protected void assertCacheContents(Cache cache, boolean expectCacheables, boolean expectUncacheables,
        boolean expectUnspecified) {
        assertCacheables(cache, expectCacheables);
        assertUncacheables(cache, expectUncacheables);
        assertUnspecified(cache, expectUnspecified);
    }

    /**
     * Assert whether the cacheable types are in the cache. This method exits on
     * the first cache 'miss'.
     * 
     * @param cache
     *            JPA Cache to verify
     * @param expected
     *            If true the cacheable types should be in the cache, if false
     *            they should not be.
     */
    protected void assertCacheables(Cache cache, boolean expected) {
        assertCached(cache, CacheableEntity.class, 1, expected);
        assertCached(cache, NegatedUncacheableEntity.class, 1, expected);
        assertCached(cache, XmlCacheableEntity.class, 1, expected);
    }

    /**
     * Assert whether the uncacheable types are in the cache. This method exits
     * on the first cache 'miss'.
     * 
     * @param cache
     *            JPA Cache to verify
     * @param expected
     *            If true the uncacheable types should be in the cache, if false
     *            they should not be.
     */
    protected void assertUncacheables(Cache cache, boolean expected) {
        assertCached(cache, UncacheableEntity.class, 1, expected);
        assertCached(cache, XmlUncacheableEntity.class, 1, expected);
        assertCached(cache, NegatedCachableEntity.class, 1, expected);
    }

    /**
     * Assert whether the unspecified types are in the cache. This method exits
     * on the first cache 'miss'.
     * 
     * @param cache
     *            JPA Cache to verify
     * @param expected
     *            If true the unspecified types should be in the cache, if false
     *            they should not be.
     */
    protected void assertUnspecified(Cache cache, boolean expected) {
        assertCached(cache, UnspecifiedEntity.class, 1, expected);
    }
}
