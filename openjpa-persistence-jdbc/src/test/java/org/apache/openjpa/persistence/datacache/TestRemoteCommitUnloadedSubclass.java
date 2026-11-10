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
package org.apache.openjpa.persistence.datacache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.datacache.AbstractQueryCache;
import org.apache.openjpa.datacache.QueryCache;
import org.apache.openjpa.datacache.TypesChangedEvent;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.event.RemoteCommitEvent;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Without a list of persistent types, a remote commit event can name a
 * persistent subclass that has not been loaded yet. The subclass still
 * invalidates the cached query results of its superclass.
 */
public class TestRemoteCommitUnloadedSubclass extends SingleEMFTestCase {

    private static final String DOG = "org.apache.openjpa.persistence.datacache.RemoteCommitDog";

    @Override
    public void setUp() throws Exception {
        // creates the table; the subclass is not part of the listed types
        super.setUp(DROP_TABLES, RemoteCommitAnimal.class);
    }

    public void testQueryCacheInvalidatesSuperclassOfUnloadedType() {
        for (Class<?> cls : PCRegistry.getRegisteredTypes()) {
            assertFalse("the subclass must not be loaded yet", DOG.equals(cls.getName()));
        }

        // no persistent types are listed, so types are found when used
        OpenJPAEntityManagerFactorySPI unlisted = createEMF(FRESH_EMF,
            "openjpa.DataCache", "true",
            "openjpa.QueryCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm");
        try {
            assertNull(unlisted.getConfiguration().getMetaDataRepositoryInstance()
                .getPersistentTypeNames(false, null));
            AbstractQueryCache cache = getQueryCache(unlisted);
            EntityManager em = unlisted.createEntityManager();
            try {
                assertNull(em.find(RemoteCommitAnimal.class, 1));
                em.createQuery("select a from RemoteCommitAnimal a", RemoteCommitAnimal.class)
                    .getResultList();
            } finally {
                em.close();
            }
            assertEquals(1, cache.count());

            List<Collection<?>> changes = new ArrayList<>();
            cache.addTypesChangedListener((TypesChangedEvent event) -> changes.add(event.getTypes()));
            cache.afterCommit(new RemoteCommitEvent(RemoteCommitEvent.PAYLOAD_EXTENTS, null, null,
                Arrays.asList(DOG), null));

            assertEquals(1, changes.size());
            assertEquals(1, changes.get(0).size());
            Class<?> dog = (Class<?>) changes.get(0).iterator().next();
            assertEquals(DOG, dog.getName());
            assertEquals(RemoteCommitAnimal.class, PCRegistry.getPersistentSuperclass(dog));
            assertEquals(0, cache.count());
        } finally {
            closeEMF(unlisted);
        }
    }

    private static AbstractQueryCache getQueryCache(OpenJPAEntityManagerFactorySPI factory) {
        QueryCache cache = factory.getConfiguration().getDataCacheManagerInstance().getSystemQueryCache();
        if (cache instanceof AbstractQueryCache abstractCache) {
            return abstractCache;
        }
        fail("unexpected query cache " + cache);
        return null;
    }
}
