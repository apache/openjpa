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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.conf.Configuration;

/**
 * An implementation of the cache of {@link PreparedQuery prepared queries}. 
 * 
 * @author Pinaki Poddar
 *
 * @since 1.3.0
 * @nojavadoc
 */
public class PreparedQueryCacheImpl implements PreparedQueryCache {
	private static final String PATTERN_SEPARATOR = "\\;";
	private static final String EXLUDED_BY_USER = "Excluded by user";
	
	// Key: Query identifier 
	private final Map<String, PreparedQuery> _delegate;
	// Key: Query identifier Value: Reason why excluded
	private final Map<String, String> _uncachables;
	private List<String> _exclusionPatterns;
	private final QueryStatistics _stats;
	private ReentrantLock _lock = new ReentrantLock();

	public PreparedQueryCacheImpl() {
		_delegate = new HashMap<String, PreparedQuery>();
		_uncachables = new HashMap<String, String>();
		_stats = new QueryStatistics.Default();
	}
	
	public Map<String,String> getMapView() {
		lock();
		try {
			Map<String, String> view = new TreeMap<String, String>();
			for (Map.Entry<String, PreparedQuery> entry : _delegate.entrySet())
				view.put(entry.getKey(), entry.getValue().getDatastoreAction());
			return view;
		} finally {
			unlock();
		}
	}
	
	/**
	 * Cache the given query keyed by its identifier. Does not cache if the 
	 * identifier matches any exclusion pattern or has been marked as 
	 * non-cachable. Also register the identifier as not cachable against 
	 * the matched exclusion pattern.
	 */
	public boolean cache(PreparedQuery q) {
		lock();
		try {
			String id = q.getIdentifier();
			if (isCachable(id) == Boolean.FALSE)
				return false;
			String pattern = getMatchedExclusionPattern(id);
			if (pattern != null) {
				markUncachable(q.getIdentifier(), pattern);
				return false;
			}
			_delegate.put(q.getIdentifier(), q);
			return true;
		} finally {
			unlock();
		}
	}
	
	public boolean invalidate(String id) {
		lock();
		try {
			return _delegate.remove(id) != null;
		} finally {
			unlock();
		}
	}
	
	public PreparedQuery get(String id) {
		lock();
		try {
			return _delegate.get(id);
		} finally {
			unlock();
		}
	}
	
	public Boolean isCachable(String id) {
		lock();
		try {
			if (_uncachables.containsKey(id))
				return Boolean.FALSE;
			if (_delegate.containsKey(id))
				return Boolean.TRUE;
			return null;
		} finally {
			unlock();
		}
	}
	
	public PreparedQuery markUncachable(String id) {
		return markUncachable(id, EXLUDED_BY_USER);
	}
	
	private PreparedQuery markUncachable(String id, String pattern) {
		lock();
		try {
			if (_uncachables.get(id) != EXLUDED_BY_USER)
				_uncachables.put(id, pattern);
			return _delegate.remove(id);
		} finally {
			unlock();
		}
	}
	
	public boolean isExcluded(String id) {
		return getMatchedExclusionPattern(id) != null;
	}
	
	public void setExcludes(String excludes) {
		lock();
		try {
			if (StringUtils.isEmpty(excludes))
				return;
			if (_exclusionPatterns == null)
				_exclusionPatterns = new ArrayList<String>();
			String[] patterns = excludes.split(PATTERN_SEPARATOR);
			for (String pattern : patterns)
				addExclusionPattern(pattern);
		} finally {
			unlock();
		}
	}

	public List<String> getExcludes() {
		return _exclusionPatterns == null ? Collections.EMPTY_LIST : 
			Collections.unmodifiableList(_exclusionPatterns);
	}
	
	/**
	 * Adds a pattern for exclusion. Any query cached currently whose identifier
	 * matches the given pattern will be marked invalidated as a side-effect.
	 */
	public void addExclusionPattern(String pattern) {
		lock();
		try {
			if (_exclusionPatterns == null)
				_exclusionPatterns = new ArrayList<String>();
			_exclusionPatterns.add(pattern);
			Collection<String> invalidKeys = getMatchedKeys(pattern, 
					_delegate.keySet());
			for (String invalidKey : invalidKeys)
				markUncachable(invalidKey, pattern);
		} finally {
			unlock();
		}
	}
	
	/**
	 * Removes a pattern for exclusion. Any query identifier marked as not 
	 * cachable due to the given pattern will now be removed from the list of
	 * uncachables as a side-effect.
	 */
	public void removeExclusionPattern(String pattern) {
		lock();
		try {
			if (_exclusionPatterns == null)
				return;
			_exclusionPatterns.remove(pattern);
			Collection<String> rebornKeys = getMatchedKeys(pattern, _uncachables);
			for (String rebornKey : rebornKeys)
				_uncachables.remove(rebornKey);
		} finally {
			unlock();
		}
	}
	
	public QueryStatistics getStatistics() {
		return _stats;
	}
	
	/**
	 * Gets the pattern that matches the given identifier.
	 */
	private String getMatchedExclusionPattern(String id) {
		lock();
		try {
			if (_exclusionPatterns == null || _exclusionPatterns.isEmpty())
				return null;
			for (String pattern : _exclusionPatterns)
				if (matches(pattern, id))
					return pattern;
			return null;
		} finally {
			unlock();
		}
	}
	
	/**
	 * Gets the keys of the given map whose values match the given pattern. 
	 */
	private Collection<String> getMatchedKeys(String pattern, 
			Map<String,String> map) {
		List<String> result = new ArrayList<String>();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (matches(pattern, entry.getValue())) {
				result.add(entry.getKey());
			}
		}
		return result;
	}
	
	/**
	 * Gets the elements of the given list which match the given pattern. 
	 */
	private Collection<String> getMatchedKeys(String pattern, 
			Collection<String> coll) {
		List<String> result = new ArrayList<String>();
		for (String key : coll) {
			if (matches(pattern, key)) {
				result.add(key);
			}
		}
		return result;
	}

    void lock() {
        if (_lock != null)
            _lock.lock();
    }

    void unlock() {
        if (_lock != null && _lock.isLocked())
            _lock.unlock();
    }
    
    boolean matches(String pattern, String target) {
    	return target != null && (target.equals(pattern) 
    	  || target.matches(pattern));
    }
    
	//-------------------------------------------------------
	// Configurable contract
	//-------------------------------------------------------
    public void setConfiguration(Configuration conf) {
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }
}
