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
package org.apache.openjpa.jdbc.meta.strats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The AttributeConverter instance of an element collection is cached on the
 * shared {@link ConverterElementHandler}; concurrent conversions must
 * construct and publish exactly one instance. See OPENJPA-2954.
 */
public class TestConverterElementHandlerCaching {

    private static final int THREADS = 8;

    static final AtomicInteger CONSTRUCTED = new AtomicInteger();
    static volatile CountDownLatch SECOND_CONSTRUCTION;

    public static class SlowConverter {
        public SlowConverter() throws InterruptedException {
            CONSTRUCTED.incrementAndGet();
            SECOND_CONSTRUCTION.countDown();
            // hold the construction window open until another thread also
            // constructs an instance (unsafe caching), or give up after a
            // short timeout (correct caching: nobody else ever gets here)
            SECOND_CONSTRUCTION.await(500, TimeUnit.MILLISECONDS);
        }

        public String convertToDatabaseColumn(String attribute) {
            return attribute.toUpperCase();
        }

        public String convertToEntityAttribute(String dbData) {
            return dbData.toLowerCase();
        }
    }

    @Test
    public void testSingleConverterInstanceUnderConcurrency() throws Exception {
        CONSTRUCTED.set(0);
        SECOND_CONSTRUCTION = new CountDownLatch(2);

        final ConverterElementHandler handler =
            new ConverterElementHandler(SlowConverter.class, String.class);

        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Object[]>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return new Object[] {
                        handler.toDataStoreValue(null, "abc", null),
                        handler.toObjectValue(null, "ABC") };
                }));
            }
            start.countDown();
            for (Future<Object[]> f : results) {
                Object[] r = f.get(10, TimeUnit.SECONDS);
                assertEquals("ABC", r[0]);
                assertEquals("abc", r[1]);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, CONSTRUCTED.get());
    }
}
