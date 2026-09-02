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

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.PessimisticLockScope;

import org.apache.openjpa.kernel.DataCacheRetrieveMode;
import org.apache.openjpa.kernel.DataCacheStoreMode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the conversion of user supplied property values to values consumable by the kernel.
 */
public class TestJPAProperties {

    @Test
    public void testStringConvertedToSpecificationEnum() {
        assertEquals(CacheRetrieveMode.USE, JPAProperties.convertToKernelValue(CacheRetrieveMode.class,
            JPAProperties.CACHE_RETRIEVE_MODE, "USE"));
        assertEquals(CacheStoreMode.BYPASS, JPAProperties.convertToKernelValue(CacheStoreMode.class,
            JPAProperties.CACHE_STORE_MODE, "bypass"));
        assertEquals(CacheStoreMode.REFRESH, JPAProperties.convertToKernelValue(CacheStoreMode.class,
            JPAProperties.CACHE_STORE_MODE, " REFRESH "));
    }

    @Test
    public void testStringConvertedToKernelEnum() {
        assertEquals(DataCacheStoreMode.REFRESH, JPAProperties.convertToKernelValue(DataCacheStoreMode.class,
            JPAProperties.CACHE_STORE_MODE, "REFRESH"));
        assertEquals(DataCacheRetrieveMode.BYPASS, JPAProperties.convertToKernelValue(DataCacheRetrieveMode.class,
            JPAProperties.CACHE_RETRIEVE_MODE, "BYPASS"));
    }

    @Test
    public void testEnumConvertedAcrossSpecificationAndKernel() {
        assertEquals(DataCacheStoreMode.REFRESH, JPAProperties.convertToKernelValue(DataCacheStoreMode.class,
            JPAProperties.CACHE_STORE_MODE, CacheStoreMode.REFRESH));
        assertEquals(CacheStoreMode.REFRESH, JPAProperties.convertToKernelValue(CacheStoreMode.class,
            JPAProperties.CACHE_STORE_MODE, DataCacheStoreMode.REFRESH));
        assertEquals(DataCacheRetrieveMode.BYPASS, JPAProperties.convertToKernelValue(DataCacheRetrieveMode.class,
            JPAProperties.CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS));
        assertEquals(CacheRetrieveMode.BYPASS, JPAProperties.convertToKernelValue(CacheRetrieveMode.class,
            JPAProperties.CACHE_RETRIEVE_MODE, DataCacheRetrieveMode.BYPASS));
    }

    @Test
    public void testMatchingEnumIsPassedThrough() {
        assertSame(CacheRetrieveMode.BYPASS, JPAProperties.convertToKernelValue(CacheRetrieveMode.class,
            JPAProperties.CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS));
        assertSame(DataCacheStoreMode.USE, JPAProperties.convertToKernelValue(DataCacheStoreMode.class,
            JPAProperties.CACHE_STORE_MODE, DataCacheStoreMode.USE));
    }

    @Test
    public void testInvalidEnumStringFails() {
        try {
            JPAProperties.convertToKernelValue(CacheRetrieveMode.class, JPAProperties.CACHE_RETRIEVE_MODE, "NOPE");
            fail("Expected an IllegalArgumentException for an invalid cache retrieve mode");
        } catch (IllegalArgumentException iae) {
            String message = iae.getMessage();
            assertTrue(message, message.contains(JPAProperties.CACHE_RETRIEVE_MODE));
            assertTrue(message, message.contains("NOPE"));
            assertTrue(message, message.contains("USE"));
            assertTrue(message, message.contains("BYPASS"));
        }
    }

    @Test
    public void testNullValues() {
        assertNull(JPAProperties.convertToKernelValue(CacheStoreMode.class, JPAProperties.CACHE_STORE_MODE, null));
        assertNull(JPAProperties.convertToKernelValue(CacheStoreMode.class, JPAProperties.CACHE_STORE_MODE, "null"));
    }

    @Test
    public void testNonEnumValuesAreUnchanged() {
        assertEquals(Integer.valueOf(12345), JPAProperties.convertToKernelValue(Integer.class,
            JPAProperties.QUERY_TIMEOUT, "12345"));
        assertEquals(Integer.valueOf(500), JPAProperties.convertToKernelValue(int.class,
            JPAProperties.LOCK_TIMEOUT, "500"));
        assertEquals(Integer.valueOf(200), JPAProperties.convertToKernelValue(Integer.class,
            JPAProperties.QUERY_TIMEOUT, Integer.valueOf(200)));
        assertEquals("USE", JPAProperties.convertToKernelValue(CacheStoreMode.class,
            "openjpa.some.other.property", "USE"));
    }

    @Test
    public void testLockScopeString() {
        assertEquals(PessimisticLockScope.EXTENDED, JPAProperties.convertToKernelValue(PessimisticLockScope.class,
            JPAProperties.LOCK_SCOPE, "EXTENDED"));
    }
}
