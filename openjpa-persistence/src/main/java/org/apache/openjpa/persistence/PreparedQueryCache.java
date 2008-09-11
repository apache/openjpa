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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;

/**
 * A cache to maintain {@link PreparedQuery prepared queries}. 
 * 
 * The target SQL depends on context of query execution such as fetch plan or
 * lock group. No attempt is made to monitor and automatically invalidate a
 * prepared SQL when the same query is executed with different context 
 * parameters.
 * 
 * The user must set a {@link QueryHints#HINT_INVALIDATE_PREPARED_QUERY hint} to 
 * invalidate.
 * 
 * @author Pinaki Poddar
 *
 * @since 1.3.0
 * @nojavadoc
 */
public class PreparedQueryCache implements Configurable {
	private static final String PATTERN_SEPARATOR = "\\;";

	private final Map<String, PreparedQuery> _delegate;
	private final Set<String> _uncachables;
	private List<String> _excludes;
	private ReentrantLock _lock = new ReentrantLock();

	public PreparedQueryCache() {
		_delegate = new HashMap<String, PreparedQuery>();
		_uncachables = new HashSet<String>();
	}
	
	/**
	 * Get a map view of the cached SQL indexed by query identifier.
	 */
	public Map<String,String> getMapView() {
		lock();
		try {
			Map<String, String> view = new TreeMap<String, String>();
			for (Map.Entry<String, PreparedQuery> entry : _delegate.entrySet())
				view.put(entry.getKey(), entry.getValue().getSQL());
			return view;
		} finally {
			unlock();
		}
	}
	
	/**
	 * Cache the given PreparedQuery.
	 * The key is the identifier of the given PreparedQuery itself.
	 */
	public boolean cache(PreparedQuery q) {
		lock();
		try {
			if (isExcluded(q.getIdentifier())) {
				markUncachable(q.getIdentifier());
				return false;
			}
			_delegate.put(q.getIdentifier(), q);
			return true;
		} finally {
			unlock();
		}
	}
	
	/**
	 * Remove the PreparedQuery with the given identifier.
	 */
	public boolean invalidate(String id) {
		lock();
		try {
			return _delegate.remove(id) != null;
		} finally {
			unlock();
		}
	}
	
	/**
	 * Get the PreparedQuery with the given identifier if it exists. null
	 * otherwise.
	 */
	public PreparedQuery get(String id) {
		lock();
		try {
			return _delegate.get(id);
		} finally {
			unlock();
		}
	}
	
	/**
	 * Affirms if a PreparedQuery can be cached against the given key.
	 * 
	 * @return Boolean.FALSE if the given key is explicitly marked before as not
	 * be cached or matches any of the exclusion pattern. 
	 * Boolean.TRUE if the given key has been in the cache. 
	 * Otherwise, return null implying this receiver can not determine whether
	 * this key can be cached on not. 
	 */
	public Boolean isCachable(String id) {
		lock();
		try {
			if (_uncachables.contains(id))
				return Boolean.FALSE;
			if (_delegate.containsKey(id))
				return Boolean.TRUE;
			return null;
		} finally {
			unlock();
		}
	}
	
	/**
	 * Marks the given key as not amenable to caching.
	 * Marking helps to avoid repeated computational cost of determining whether 
	 * a query can be cached or not.
	 * @return The value for the given key if it had been cached before. null
	 * otherwise.
	 */
	public PreparedQuery markUncachable(String id) {
		lock();
		try {
			_uncachables.add(id);
			return _delegate.remove(id);
		} finally {
			unlock();
		}
	}
	
	public boolean isExcluded(String id) {
		lock();
		try {
			if (_excludes == null || _excludes.isEmpty())
				return false;
			for (String exclude : _excludes)
				if (exclude.equalsIgnoreCase(id) || exclude.matches(id))
					return true;
			return false;
		} finally {
			unlock();
		}
	}
	
    void lock() {
        if (_lock != null)
            _lock.lock();
    }

    void unlock() {
        if (_lock != null && _lock.isLocked())
            _lock.unlock();
    }

	//-------------------------------------------------------
	// Configurable implementation
	//-------------------------------------------------------
    /**
     * Invoked prior to setting bean properties.
     */
    public void setConfiguration(Configuration conf) {
    }

    /**
     * Invoked before bean property configuration is begun on this object.
     */
    public void startConfiguration() {
    	
    }

    /**
     * Invoked upon completion of bean property configuration for this object.
     */
    public void endConfiguration() {
    	
    }
    
    /**
     * Sets one or more exclusion patterns separated by semicolon.
     */
	public void setExcludes(String excludes) {
		lock();
		try {
			if (StringUtils.isEmpty(excludes))
				return;
			if (_excludes == null)
				_excludes = new ArrayList<String>();
			String[] patterns = excludes.split(PATTERN_SEPARATOR);
			_excludes.addAll(Arrays.asList(patterns));
		} finally {
			unlock();
		}
	}

	public List<String> getExcludes() {
		return _excludes == null ? Collections.EMPTY_LIST : 
			Collections.unmodifiableList(_excludes);
	}
}
