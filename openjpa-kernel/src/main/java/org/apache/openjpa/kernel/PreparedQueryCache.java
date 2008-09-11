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
package org.apache.openjpa.kernel;

import java.util.List;
import java.util.Map;

import org.apache.openjpa.lib.conf.Configurable;

/**
 * A cache to maintain {@link PreparedQuery prepared queries}. 
 * 
 * The target query associated to a cached prepared query <em>may</em> depend on 
 * query execution context such as fetch plan or lock group. This cache, by 
 * design, does not monitor the context or automatically invalidate an entry
 * when the query is executed with context parameters that affect the target 
 * query. 
 * 
 * The user must notify this receiver to invalidate a cached entry when context
 * changes in a way that will modify the resultant target language query.
 * 
 * One of the built-in mechanism (available in JPA facade) is to set query hints 
 * to either invalidate the query entirely or ignore the cached version for the 
 * current execution. 
 * 
 * @see QueryHints#HINT_IGNORE_PREPARED_QUERY 
 * @see QueryHints#HINT_INVALIDATE_PREPARED_QUERY
 * 
 * This cache allows customization of whether a query can be cached or not
 * via either explicit marking of certain keys as non-cachable (which is 
 * irreversible) or addition/removal of exclusion patterns (which is reversible).
 * 
 * @see #markUncachable(String)
 * @see #addExclusionPattern(String)
 * @see #setExcludes(String)
 * @see #removeExclusionPattern(String)
 * 
 * @author Pinaki Poddar
 *
 * @since 1.3.0
 */
public interface PreparedQueryCache extends Configurable {

	/**
	 * Get a map view of the cached queries indexed by identifier.
	 */
	public Map<String, String> getMapView();

	/**
	 * Cache the given PreparedQuery.
	 * The key is the identifier of the given PreparedQuery itself.
	 * The query must not be cached if either the key matches any exclusion
	 * pattern or the key has been marked non-cachable.
	 * 
	 * @return true if the given query is cached. false if it can not be cached
	 * due to exclusion.
	 * 
	 * @see #markUncachable(String)
	 * @see #setExcludes(String)
	 * @see #addExclusionPattern(String)
	 */
	public boolean cache(PreparedQuery q);

	/**
	 * Remove the PreparedQuery with the given identifier from this cache.
	 */
	public boolean invalidate(String id);

	/**
	 * Get the PreparedQuery with the given identifier if it exists. null
	 * otherwise.
	 */
	public PreparedQuery get(String id);

	/**
	 * Affirms if a PreparedQuery can be cached against the given key.
	 * 
	 * @return Boolean.FALSE if the given key is explicitly marked before as not
	 * be cached or matches any of the exclusion patterns. 
	 * Boolean.TRUE if the given key currently exists in the cache. 
	 * Otherwise, return null implying this receiver can not determine whether
	 * this key can be cached on not. 
	 * 
	 */
	public Boolean isCachable(String id);

	/**
	 * Marks the given key as not amenable to caching.
	 * Explicit marking helps to avoid repeated computational cost of 
	 * determining whether a query can be cached or not.
	 * 
	 * Explicit marking can not be reversed by removal of exclusion patterns.
	 * 
	 * @return The value for the given key if it had been cached before. null
	 * otherwise.
	 */
	public PreparedQuery markUncachable(String id);

	/**
	 * Affirms if the given key matches any of the exclusion patterns.
	 */
	public boolean isExcluded(String id);

	/**
	 * Gets the exclusion patterns.
	 */
	public List<String> getExcludes();
	
	/**
	 * Sets one or more exclusion regular expression patterns separated by 
	 * semicolon. Any existing cache entry whose key matches any of the given
	 * pattern will be marked non-cachable in a reversible manner. 
	 */
	public void setExcludes(String excludes);

	/**
	 * Adds the given pattern to the list of excluded patterns. Any existing 
	 * cache entry whose key matches the given pattern will be marked 
	 * non-cachable in a reversible manner. 
	 */
	public void addExclusionPattern(String pattern);
	
	/**
	 * Removes the given pattern from the list of excluded patterns. 
	 * Any excluded key that matches the given pattern can now be cached
	 * again, unless it has been marked non-cachable explicitly.
	 * 
	 * @see #markUncachable(String)
	 */
	public void removeExclusionPattern(String pattern);
	
	/**
	 * Gets the simple statistics for executed queries.
	 */
	public QueryStatistics getStatistics();
}