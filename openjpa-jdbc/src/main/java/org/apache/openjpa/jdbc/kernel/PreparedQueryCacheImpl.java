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
package org.apache.openjpa.jdbc.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.QueryStatistics;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;

/**
 * An implementation of the cache of {@link PreparedQuery prepared queries}. 
 * 
 * @author Pinaki Poddar
 *
 * @since 2.0.0
 * 
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
	private final QueryStatistics<String> _stats;
	private ReentrantLock _lock = new ReentrantLock();
	private Log _log;
	private Localizer _loc = Localizer.forPackage(PreparedQueryCacheImpl.class);

	public PreparedQueryCacheImpl() {
		_delegate = new HashMap<String, PreparedQuery>();
		_uncachables = new HashMap<String, String>();
		_stats = new QueryStatistics.Default<String>();
	}
	
	public Boolean register(String id, Query query, FetchConfiguration hints) {
        if (id == null 
            || query == null 
            || QueryLanguages.LANG_SQL.equals(query.getLanguage()) 
            || QueryLanguages.LANG_METHODQL.equals(query.getLanguage())
            || isHinted(hints, QueryHints.HINT_IGNORE_PREPARED_QUERY)
            || isHinted(hints, QueryHints.HINT_INVALIDATE_PREPARED_QUERY))
            return Boolean.FALSE;
        if (isCachable(id) == Boolean.FALSE)
            return Boolean.FALSE;
        PreparedQuery cached = get(id);
        if (cached != null)
            return null; // implies that it is already cached
        
        PreparedQuery newEntry = new PreparedQueryImpl(id, query); 
        return cache(newEntry);
	}
	
	public Map<String,String> getMapView() {
		lock();
		try {
			Map<String, String> view = new TreeMap<String, String>();
			for (Map.Entry<String, PreparedQuery> entry : _delegate.entrySet())
				view.put(entry.getKey(), entry.getValue().getTargetQuery());
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
			if (isCachable(id) == Boolean.FALSE) {
				if (_log != null && _log.isWarnEnabled())
					_log.warn(_loc.get("prepared-query-not-cachable", id));
				return false;
			}
			String pattern = getMatchedExclusionPattern(id);
			if (pattern != null) {
				markUncachable(id, pattern);
				return false;
			}
			_delegate.put(id, q);
            if (_log != null && _log.isTraceEnabled())
                _log.trace(_loc.get("prepared-query-cached", id, 
                    q.getTargetQuery()));
			return true;
		} finally {
			unlock();
		}
	}
	
    public PreparedQuery initialize(String key, Object result) {
        PreparedQuery pq = get(key);
        if (pq == null)
            return null;
        
        boolean cacheable = pq.initialize(result);
        if (!cacheable) {
            markUncachable(key);
            return null;
        } 
        return pq;
    }
	
	public boolean invalidate(String id) {
		lock();
		try {
			if (_log.isTraceEnabled())
				_log.trace(_loc.get("prepared-query-invalidate", id));
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
	
	private PreparedQuery markUncachable(String id, String reason) {
		lock();
		try {
			boolean excludedByUser = _uncachables.get(id) == EXLUDED_BY_USER;
			if (!excludedByUser)
				_uncachables.put(id, reason);
			if (_log != null && _log.isInfoEnabled()) {
				if (excludedByUser) 
					_log.info(_loc.get("prepared-query-uncache-strong", id));
				else 
					_log.info(_loc.get("prepared-query-uncache-weak", id, 
						reason));
			}
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
			if (!invalidKeys.isEmpty() && _log != null && _log.isInfoEnabled())
				_log.info(_loc.get("prepared-query-add-pattern", pattern, 
					invalidKeys.size(), invalidKeys));
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
			Collection<String> reborns = getMatchedKeys(pattern, _uncachables);
			if (!reborns.isEmpty() && _log != null && _log.isInfoEnabled())
				_log.info(_loc.get("prepared-query-remove-pattern", pattern, 
					reborns.size(), reborns));
			for (String rebornKey : reborns)
				_uncachables.remove(rebornKey);
		} finally {
			unlock();
		}
	}
	
	public QueryStatistics<String> getStatistics() {
		return _stats;
	}
	
	/**
	 * Gets the pattern that matches the given identifier.
	 */
	private String getMatchedExclusionPattern(String id) {
		if (_exclusionPatterns == null || _exclusionPatterns.isEmpty())
			return null;
		for (String pattern : _exclusionPatterns)
			if (matches(pattern, id))
				return pattern;
		return null;
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
    
    boolean isHinted(FetchConfiguration fetch, String hint) {
        if (fetch == null)
            return false;
        Object result = fetch.getHint(hint);
        return result != null && "true".equalsIgnoreCase(result.toString());
    }
        
	//-------------------------------------------------------
	// Configurable contract
	//-------------------------------------------------------
    public void setConfiguration(Configuration conf) {
    	_log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }
}
