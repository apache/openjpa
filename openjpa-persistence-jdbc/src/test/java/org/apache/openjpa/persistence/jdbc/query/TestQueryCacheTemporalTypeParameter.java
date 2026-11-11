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
package org.apache.openjpa.persistence.jdbc.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.QueryResultCache;
import org.apache.openjpa.persistence.jdbc.query.domain.TimeKeeper;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A query whose Calendar parameter is bound with setParameter(Parameter, Calendar, TemporalType) is cached under the
 * converted value. QueryResultCache.pin/evict(Query) must build the same key, or they do not find the cached result.
 */
public class TestQueryCacheTemporalTypeParameter extends SingleEMFTestCase {
    private static final String JPQL = "SELECT p FROM TimeKeeper p WHERE p.date = :d";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, "openjpa.DataCache", "true", "openjpa.QueryCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm", TimeKeeper.class);
    }

    @Test
    public void testEvictFindsResultOfCalendarParameterWithTemporalType() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<TimeKeeper> q = em.createQuery(JPQL, TimeKeeper.class);
        Parameter<Calendar> d = (Parameter<Calendar>) q.getParameter("d");
        q.setParameter(d, Calendar.getInstance(), TemporalType.DATE);
        q.getResultList();
        assertEquals(1, cacheSize());

        QueryResultCache cache = OpenJPAPersistence.cast(emf).getQueryResultCache();
        cache.evict(q);
        assertEquals(0, cacheSize());
        em.close();
    }

    private int cacheSize() {
        return ((ConcurrentQueryCache) OpenJPAPersistence.cast(emf).getQueryResultCache().getDelegate())
            .getCacheMap().size();
    }
}
