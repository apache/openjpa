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

import javax.persistence.Cache;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

public class TestCacheModeNone extends AbstractJPACacheTestCase {

    private static OpenJPAEntityManagerFactorySPI emf = null;
    private static Cache cache = null;

    @Override
    public OpenJPAEntityManagerFactorySPI getEntityManagerFactory() {
        if (emf == null) {
            emf = createEntityManagerFactory("cache-mode-none");
            assertNotNull(emf);
            cache = emf.getCache();
            assertNotNull(cache);
        }
        return emf;
    }

    public void testCacheables() {
        assertCacheables(cache, false);
    }

    public void testUncacheables() {
        assertUncacheables(cache, false);
    }

    public void testUnspecified() {
        assertUnspecified(cache, false);
    }
}
