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
package org.apache.openjpa.persistence.jdbc.sqlcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.PreparedQueryCacheImpl;
import org.apache.openjpa.jdbc.kernel.PreparedQueryImpl;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test exclusion patterns of PreparedQueryCache in isolation.
 *
 * @author Pinaki Poddar
 *
 */
public class TestPreparedQueryCacheExclusion {
	private PreparedQueryCache cache;
	private String[] keys   = {"jpql1", "jpql2", "jpql3"};
	private String[] values = {"sql1", "sql2", "sql3"};

    @BeforeEach
    public void setUp() throws Exception {
		cache = new PreparedQueryCacheImpl();
		for (int i = 0; i < keys.length; i++) {
            PreparedQuery p = new PreparedQueryImpl(keys[i], values[i], null);
			cache.cache(p);
		}
	}

    @AfterEach
    public void tearDown() throws Exception {
	}

    @Test
    public void testExclusionPatternsAreSet() {
		String excludes = "a;b;c";
		cache.setExcludes(excludes);
		assertEquals(3, cache.getExcludes().size());
		assertNotNull(cache.isExcluded("a"));
		assertNotNull(cache.isExcluded("b"));
		assertNotNull(cache.isExcluded("c"));
		assertNull(cache.isExcluded("d"));

//		List<PreparedQueryCache.Exclusion> exclusions = cache.getExcludes();
//		for (PreparedQueryCache.Exclusion e : exclusions)
//		    System.err.println(e);
	}

    @Test
    public void testCachePopulationSetUp() {
		assertContent(keys, values);
	}

    @Test
    public void testAddExclusionPatternDisallowsCacheing() {
		int EXCLUDED = 1;
		cache.addExclusionPattern(keys[EXCLUDED]);

        PreparedQuery p = new PreparedQueryImpl(keys[EXCLUDED],
                values[EXCLUDED], null);
        assertFalse(cache.cache(p),
                "Must not cache excluded key " + keys[EXCLUDED]);
	}

    @Test
    public void testAddExclusionPatternMakesExistingEntryInvalid() {
		int EXCLUDED = 1;
		cache.addExclusionPattern(keys[EXCLUDED]);
		Map<String, String> view = cache.getMapView();
		for (int i = 0; i < keys.length; i++) {
			if (i == EXCLUDED) {
				assertFalse(view.containsKey(keys[i]));
				assertFalse(view.containsValue(values[i]));
			} else {
				assertTrue(view.containsKey(keys[i]));
				assertTrue(view.containsValue(values[i]));
			}
		}
	}

    @Test
    public void testRemoveExclusionPatternAllowsCacheing() {
		int EXCLUDED = 1;
		cache.addExclusionPattern(keys[EXCLUDED]);

        PreparedQuery p = new PreparedQueryImpl(keys[EXCLUDED],
                values[EXCLUDED], null);
        assertFalse(cache.cache(p),
                "Must not cache excluded key " + keys[EXCLUDED]);

		cache.removeExclusionPattern(keys[EXCLUDED]);
        assertTrue(cache.cache(p),
                "Must cache remove excluded key " + keys[EXCLUDED]);
	}

    @Test
    public void testRemoveExclusionPatternDoesNotRemoveUserProhbitedKeys() {
		String USER_MARKED_UNCACHABLE = "[user prohibited]";
		cache.markUncachable(USER_MARKED_UNCACHABLE,
		        new PreparedQueryCacheImpl.StrongExclusion(USER_MARKED_UNCACHABLE,"for testing"));

        PreparedQuery p = new PreparedQueryImpl(USER_MARKED_UNCACHABLE, "xyz",
                null);
		assertFalse(cache.cache(p), "Must not cache user-prohibited key " +
				USER_MARKED_UNCACHABLE);

		cache.removeExclusionPattern(USER_MARKED_UNCACHABLE);
        assertFalse(cache.cache(p), "Must not cache user-prohibited key even when removed " +
				USER_MARKED_UNCACHABLE);
	}

	void assertContent(String[] keys, String[] values) {
		Map<String, String> view = cache.getMapView();
		for (int i = 0; i < keys.length; i++) {
			assertTrue(view.containsKey(keys[i]),
					"key " + keys[i] + " not in " + view);
			assertTrue(view.containsValue(values[i]),
					"value " + values[i] + " not in " + view);
		}
	}
}
