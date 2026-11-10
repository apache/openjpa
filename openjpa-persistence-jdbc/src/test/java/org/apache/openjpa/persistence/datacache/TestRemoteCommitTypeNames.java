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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.AbstractDataCache;
import org.apache.openjpa.datacache.AbstractQueryCache;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.QueryCache;
import org.apache.openjpa.datacache.TypesChangedEvent;
import org.apache.openjpa.event.RemoteCommitEvent;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * The type names of a remote commit event are looked up among the persistent
 * types of the metadata repository. Names of other types are ignored and do
 * not load or initialize the named class.
 */
public class TestRemoteCommitTypeNames extends SingleEMFTestCase {

    /**
     * Set by the static initializer of {@link NotPersistent}.
     */
    static volatile boolean initialized;

    private static final String NOT_PERSISTENT =
        TestRemoteCommitTypeNames.class.getName() + "$NotPersistent";
    private static final String UNKNOWN = "org.apache.openjpa.example.DoesNotExist";

    @Override
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, CachedPerson.class,
            "openjpa.DataCache", "true",
            "openjpa.QueryCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm");
        initialized = false;
    }

    public void testDataCacheIgnoresUnknownTypeNames() {
        CachedPerson person = persist(1);
        assertTrue(emf.getCache().contains(CachedPerson.class, person.getId()));

        AbstractDataCache cache = getDataCache();
        cache.afterCommit(newEvent(Arrays.asList(NOT_PERSISTENT, UNKNOWN)));
        assertFalse("a non-persistent class must not be initialized", initialized);
        assertTrue(emf.getCache().contains(CachedPerson.class, person.getId()));

        cache.afterCommit(newEvent(Arrays.asList(NOT_PERSISTENT, UNKNOWN, CachedPerson.class.getName())));
        assertFalse("a non-persistent class must not be initialized", initialized);
        assertFalse(emf.getCache().contains(CachedPerson.class, person.getId()));
    }

    public void testQueryCacheIgnoresUnknownTypeNames() {
        AbstractQueryCache cache = getQueryCache();
        List<Collection<?>> changes = new ArrayList<>();
        cache.addTypesChangedListener((TypesChangedEvent event) -> changes.add(event.getTypes()));

        cache.afterCommit(newEvent(Arrays.asList(NOT_PERSISTENT, UNKNOWN)));
        assertFalse("a non-persistent class must not be initialized", initialized);
        assertTrue(changes.isEmpty());

        cache.afterCommit(newEvent(Arrays.asList(NOT_PERSISTENT, UNKNOWN, CachedPerson.class.getName())));
        assertFalse("a non-persistent class must not be initialized", initialized);
        assertEquals(1, changes.size());
        Set<Object> expected = new HashSet<>(Arrays.asList(CachedPerson.class));
        assertEquals(expected, new HashSet<>(changes.get(0)));
    }

    private static RemoteCommitEvent newEvent(Collection<String> updatedTypeNames) {
        return new RemoteCommitEvent(RemoteCommitEvent.PAYLOAD_EXTENTS, null, null,
            updatedTypeNames, null);
    }

    private CachedPerson persist(int id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            CachedPerson person = new CachedPerson();
            person.setId(id);
            em.persist(person);
            em.getTransaction().commit();
            return person;
        } finally {
            em.close();
        }
    }

    private AbstractDataCache getDataCache() {
        OpenJPAConfiguration conf = emf.getConfiguration();
        DataCache cache = conf.getDataCacheManagerInstance().getSystemDataCache();
        if (cache instanceof AbstractDataCache abstractCache) {
            return abstractCache;
        }
        fail("unexpected data cache " + cache);
        return null;
    }

    private AbstractQueryCache getQueryCache() {
        OpenJPAConfiguration conf = emf.getConfiguration();
        QueryCache cache = conf.getDataCacheManagerInstance().getSystemQueryCache();
        if (cache instanceof AbstractQueryCache abstractCache) {
            return abstractCache;
        }
        fail("unexpected query cache " + cache);
        return null;
    }

    /**
     * Not a persistent type; initializing it flips {@link #initialized}.
     */
    public static class NotPersistent {
        static {
            initialized = true;
        }
    }
}
