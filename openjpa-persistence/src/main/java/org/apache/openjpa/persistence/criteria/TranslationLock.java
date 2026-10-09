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
package org.apache.openjpa.persistence.criteria;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Identity of the mutable translation state of one criteria query tree.
 * <p>
 * Translating a criteria tree to a kernel expression tree mutates the tree: the
 * alias, variable and value maps of the query are (re-)populated and a subquery
 * records the kernel subquery it was translated to. That state is deliberately
 * shared by the original query, by every snapshot taken of it and by the captive
 * query of each of its subqueries, so none of those objects identifies it. The
 * root query creates one lock instead and hands it to every object that shares
 * its state, which gives the translation something to serialize on.
 * <p>
 * A set operation translates two independent trees and therefore has to hold two
 * locks at once; the ordering imposed here lets it take them in a fixed order.
 *
 * @since 4.2.0
 */
final class TranslationLock implements Comparable<TranslationLock> {
    private static final AtomicLong COUNTER = new AtomicLong();

    private final long _order = COUNTER.incrementAndGet();

    @Override
    public int compareTo(TranslationLock other) {
        return Long.compare(_order, other._order);
    }
}
