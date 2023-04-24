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
package org.apache.openjpa.persistence;

import jakarta.persistence.Query;

import org.apache.openjpa.datacache.QueryCache;

/**
 * Query result cache.
 *
 * @author Abe White
 * @since 0.4.1
 * @published
 */
public interface QueryResultCache {

    /**
     * Pin the given query's result to the cache.
     */
    void pin(Query q);

    /**
     * Unpin a previously-pinned query result.
     */
    void unpin(Query q);

    /**
     * Evict a query result from the cache.
     */
    void evict(Query q);

    /**
     * Clear the cache.
     */
    void evictAll();

    /**
     * Evict all result for queries involving the given class.
     */
    void evictAll(Class cls);

    /**
     * @deprecated cast to {@link QueryResultCacheImpl} instead. This
     * method pierces the published-API boundary, as does the SPI cast.
     */
    @Deprecated QueryCache getDelegate();
}
