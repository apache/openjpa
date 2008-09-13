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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.DelegatingQuery;
import org.apache.openjpa.kernel.DelegatingResultList;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.ParameterMap;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.UserException;

/**
 * Implementation of {@link Query} interface.
 * 
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @author Pinaki Poddar
 * @nojavadoc
 */
public class QueryImpl implements OpenJPAQuerySPI, Serializable {

	private static final Object[] EMPTY_ARRAY = new Object[0];

	private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

	private DelegatingQuery _query;
	private transient EntityManagerImpl _em;
	private transient FetchPlan _fetch;
	private transient ParameterMap _params;
	private String _id;


	/**
	 * Constructor; supply factory exception translator and delegate.
	 * 
	 * @param em  The EntityManager which created this query
	 * @param ret Exception translator for this query
	 * @param query The underlying "kernel" query.
	 */
	public QueryImpl(EntityManagerImpl em, RuntimeExceptionTranslator ret,
			org.apache.openjpa.kernel.Query query) {
		_em = em;
		_query = new DelegatingQuery(query, ret);
	}

	/**
	 * Constructor; supply factory and delegate.
	 * 
	 * @deprecated
	 */
	public QueryImpl(EntityManagerImpl em, org.apache.openjpa.kernel.Query query) {
		this(em, null, query);
	}

	/**
	 * Delegate.
	 */
	public org.apache.openjpa.kernel.Query getDelegate() {
		return _query.getDelegate();
	}

	public OpenJPAEntityManager getEntityManager() {
		return _em;
	}

	public String getLanguage() {
		return _query.getLanguage();
	}

	public QueryOperationType getOperation() {
		return QueryOperationType.fromKernelConstant(_query.getOperation());
	}

	public FetchPlan getFetchPlan() {
		_em.assertNotCloseInvoked();
		_query.assertNotSerialized();
		_query.lock();
		try {
			if (_fetch == null)
				_fetch = ((EntityManagerFactoryImpl) _em
						.getEntityManagerFactory()).toFetchPlan(_query
						.getBroker(), _query.getFetchConfiguration());
			return _fetch;
		} finally {
			_query.unlock();
		}
	}

	public String getQueryString() {
		return _query.getQueryString();
	}

	public boolean getIgnoreChanges() {
		return _query.getIgnoreChanges();
	}

	public OpenJPAQuery setIgnoreChanges(boolean ignore) {
		_em.assertNotCloseInvoked();
		_query.setIgnoreChanges(ignore);
		return this;
	}

	public OpenJPAQuery addFilterListener(FilterListener listener) {
		_em.assertNotCloseInvoked();
		_query.addFilterListener(listener);
		return this;
	}

	public OpenJPAQuery removeFilterListener(FilterListener listener) {
		_em.assertNotCloseInvoked();
		_query.removeFilterListener(listener);
		return this;
	}

	public OpenJPAQuery addAggregateListener(AggregateListener listener) {
		_em.assertNotCloseInvoked();
		_query.addAggregateListener(listener);
		return this;
	}

	public OpenJPAQuery removeAggregateListener(AggregateListener listener) {
		_em.assertNotCloseInvoked();
		_query.removeAggregateListener(listener);
		return this;
	}

	public Collection getCandidateCollection() {
		return _query.getCandidateCollection();
	}

	public OpenJPAQuery setCandidateCollection(Collection coll) {
		_em.assertNotCloseInvoked();
		_query.setCandidateCollection(coll);
		return this;
	}

	public Class getResultClass() {
		Class res = _query.getResultType();
		if (res != null)
			return res;
		return _query.getCandidateType();
	}

	public OpenJPAQuery setResultClass(Class cls) {
		_em.assertNotCloseInvoked();
		if (ImplHelper.isManagedType(_em.getConfiguration(), cls))
			_query.setCandidateType(cls, true);
		else
			_query.setResultType(cls);
		return this;
	}

	public boolean hasSubclasses() {
		return _query.hasSubclasses();
	}

	public OpenJPAQuery setSubclasses(boolean subs) {
		_em.assertNotCloseInvoked();
		Class cls = _query.getCandidateType();
		_query.setCandidateExtent(_query.getBroker().newExtent(cls, subs));
		return this;
	}

	public int getFirstResult() {
		return asInt(_query.getStartRange());
	}

	public OpenJPAQuery setFirstResult(int startPosition) {
		_em.assertNotCloseInvoked();
		long end;
		if (_query.getEndRange() == Long.MAX_VALUE)
			end = Long.MAX_VALUE;
		else
			end = startPosition
					+ (_query.getEndRange() - _query.getStartRange());
		_query.setRange(startPosition, end);
		return this;
	}

	public int getMaxResults() {
		return asInt(_query.getEndRange() - _query.getStartRange());
	}

	public OpenJPAQuery setMaxResults(int max) {
		_em.assertNotCloseInvoked();
		long start = _query.getStartRange();
		if (max == Integer.MAX_VALUE)
			_query.setRange(start, Long.MAX_VALUE);
		else
			_query.setRange(start, start + max);
		return this;
	}

	public OpenJPAQuery compile() {
		_em.assertNotCloseInvoked();
		_query.compile();
		return this;
	}

	private Object execute() {
		if (_query.getOperation() != QueryOperations.OP_SELECT)
			throw new InvalidStateException(_loc.get("not-select-query", _query
					.getQueryString()), null, null, false);
		PreparedQuery cachedQuery = cache();
		boolean usingCachedQuery = (cachedQuery != null);
		validate(_query.getParameterTypes(), !usingCachedQuery);
		recordStatistics(usingCachedQuery ? cachedQuery.getIdentifier() 
			: _query.getQueryString(), usingCachedQuery);
		Object result = _query.execute(getParameterMap(usingCachedQuery));
		return result;
	}
	
	public List getResultList() {
		_em.assertNotCloseInvoked();
		Object ob = execute();
		if (ob instanceof List) {
			List ret = (List) ob;
			if (ret instanceof ResultList)
				return new DelegatingResultList((ResultList) ret,
						PersistenceExceptions.getRollbackTranslator(_em));
			else
				return ret;
		}

		return Collections.singletonList(ob);
	}

	/**
	 * Execute a query that returns a single result.
	 */
	public Object getSingleResult() {
		_em.assertNotCloseInvoked();
		// temporarily set query to unique so that a single result is validated
		// and returned; unset again in case the user executes query again
		// via getResultList
		_query.setUnique(true);
		try {
			return execute();
		} finally {
			_query.setUnique(false);
		}
	}

	public int executeUpdate() {
		_em.assertNotCloseInvoked();
		if (_query.getOperation() == QueryOperations.OP_DELETE) {
			return asInt(_query.deleteAll(_params));
		}
		if (_query.getOperation() == QueryOperations.OP_UPDATE) {
			return asInt(_query.updateAll(_params));
		}
		throw new InvalidStateException(_loc.get("not-update-delete-query",
				_query.getQueryString()), null, null, false);
	}

	/**
	 * Cast the specified long down to an int, first checking for overflow.
	 */
	private static int asInt(long l) {
		if (l > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		if (l < Integer.MIN_VALUE) // unlikely, but we might as well check
			return Integer.MIN_VALUE;
		return (int) l;
	}

	public FlushModeType getFlushMode() {
		return EntityManagerImpl.fromFlushBeforeQueries(_query
				.getFetchConfiguration().getFlushBeforeQueries());
	}

	public OpenJPAQuery setFlushMode(FlushModeType flushMode) {
		_em.assertNotCloseInvoked();
		_query.getFetchConfiguration().setFlushBeforeQueries(
				EntityManagerImpl.toFlushBeforeQueries(flushMode));
		return this;
	}

	public OpenJPAQuery setHint(String key, Object value) {
		_em.assertNotCloseInvoked();
		if (key == null || !key.startsWith("openjpa."))
			return this;
		String k = key.substring("openjpa.".length());

		try {
			if ("Subclasses".equals(k)) {
				if (value instanceof String)
					value = Boolean.valueOf((String) value);
				setSubclasses(((Boolean) value).booleanValue());
			} else if ("FilterListener".equals(k))
				addFilterListener(Filters.hintToFilterListener(value, _query
						.getBroker().getClassLoader()));
			else if ("FilterListeners".equals(k)) {
				FilterListener[] arr = Filters.hintToFilterListeners(value,
						_query.getBroker().getClassLoader());
				for (int i = 0; i < arr.length; i++)
					addFilterListener(arr[i]);
			} else if ("AggregateListener".equals(k))
				addAggregateListener(Filters.hintToAggregateListener(value,
						_query.getBroker().getClassLoader()));
			else if ("FilterListeners".equals(k)) {
				AggregateListener[] arr = Filters.hintToAggregateListeners(
						value, _query.getBroker().getClassLoader());
				for (int i = 0; i < arr.length; i++)
					addAggregateListener(arr[i]);
			} else if (k.startsWith("FetchPlan.")) {
				k = k.substring("FetchPlan.".length());
				hintToSetter(getFetchPlan(), k, value);
			} else if (k.startsWith("hint.")) {
				if ("hint.OptimizeResultCount".equals(k)) {
					if (value instanceof String) {
						try {
							value = new Integer((String) value);
						} catch (NumberFormatException nfe) {
						}
					}
					if (!(value instanceof Number)
							|| ((Number) value).intValue() < 0)
						throw new ArgumentException(_loc.get(
								"bad-query-hint-value", key, value), null,
								null, false);
				} else if (QueryHints.HINT_INVALIDATE_PREPARED_QUERY.equals
					(key)) {
					_query.getFetchConfiguration().setHint(key, (Boolean)value);
					invalidatePreparedQuery();
				} else if (QueryHints.HINT_IGNORE_PREPARED_QUERY.equals(key)) {
					_query.getFetchConfiguration().setHint(key, (Boolean)value);
					ignorePreparedQuery();
				} else 
					_query.getFetchConfiguration().setHint(key, (Boolean)value);
			} else
				throw new ArgumentException(_loc.get("bad-query-hint", key),
						null, null, false);
			return this;
		} catch (Exception e) {
			throw PersistenceExceptions.toPersistenceException(e);
		}
	}

	private void hintToSetter(FetchPlan fetchPlan, String k, Object value) {
		if (fetchPlan == null || k == null)
			return;

		Method setter = Reflection.findSetter(fetchPlan.getClass(), k, true);
		Class paramType = setter.getParameterTypes()[0];
		if (Enum.class.isAssignableFrom(paramType) && value instanceof String)
			value = Enum.valueOf(paramType, (String) value);

		Filters.hintToSetter(fetchPlan, k, value);
	}

	public OpenJPAQuery setParameter(int position, Calendar value,
			TemporalType t) {
		return setParameter(position, convertTemporalType(value, t));
	}

	public OpenJPAQuery setParameter(int pos, Date value, TemporalType type) {
		return setParameter(pos, convertTemporalType(value, type));
	}

	/**
	 * Converts the given Date to a value corresponding to given temporal type.
	 */

	Object convertTemporalType(Date value, TemporalType type) {
		switch (type) {
		case DATE:
			return value;
		case TIME:
			return new Time(value.getTime());
		case TIMESTAMP:
			return new Timestamp(value.getTime());
		default:
			return null;
		}
	}

	Object convertTemporalType(Calendar value, TemporalType type) {
		return convertTemporalType(value.getTime(), type);
	}

	public OpenJPAQuery setParameter(int position, Object value) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			if (_params == null)
				_params = new ParameterMap(isNative() ? 
					ParameterMap.Type.POSITIONAL : null);
			_params.put(position, value);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery setParameter(String name, Calendar value,
			TemporalType type) {
		return setParameter(name, convertTemporalType(value, type));
	}

	public OpenJPAQuery setParameter(String name, Date value, TemporalType t) {
		return setParameter(name, convertTemporalType(value, t));
	}

	public OpenJPAQuery setParameter(String name, Object value) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			if (_params == null)
				_params = new ParameterMap(isNative() ? 
					ParameterMap.Type.POSITIONAL : null);
			_params.put(name, value);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public boolean isNative() {
		return QueryLanguages.LANG_SQL.equals(getLanguage());
	}
	
	public boolean hasPositionalParameters() {
		return _params != null && _params.isPositional() && !_params.isEmpty();
	}

	public Object[] getPositionalParameters() {
		_query.lock();
		try {
			return hasPositionalParameters() ? _params.values().toArray()
				: EMPTY_ARRAY;
		} finally {
			_query.unlock();
		}
	}
	
	public OpenJPAQuery setParameters(Object... params) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			if (params == null)
				return this;
			for (int i = 0; i < params.length; i++)
			    setParameter(i + 1, params[i]);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public Map getNamedParameters() {
		_query.lock();
		try {
			return (_params == null || !_params.isNamed())
				? Collections.EMPTY_MAP : _params.getMap();
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery setParameters(Map params) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			if (params == null)
				return this;
			if (_params == null)
				_params = new ParameterMap(isNative() ? 
					ParameterMap.Type.POSITIONAL : null);
			_params.putAll(params);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery closeAll() {
		_query.closeAll();
		return this;
	}

	public String[] getDataStoreActions(Map params) {
		return _query.getDataStoreActions(params);
	}
	
	QueryImpl setId(String id) {
		_id = id;
		return this;
	}
	
	/**
	 * Validates internal parameters against a given expected parameter types.
	 * 
	 * @param expected contains expected types of parameters.
	 * @param strict if true then also enforces that the parameter keys match.
	 * Otherwise only the types are compared.
	 * A more lenient strategy is required to accommodate the use case where
	 * named parameters are set on a JPQL query but the query is executed 
	 * directly as a cached SQL which only supports positional parameters.   
	 */
	void validate(LinkedMap expected, boolean strict) {
		if (_params == null) {
			if (expected != null && !expected.isEmpty())
				throw new UserException(_loc.get("param-unset", expected));
		} else {
			_params.validate(expected, strict);
		}
	}
	
	/**
	 * Gets the values from ParameterMap as a map with deterministic iteration
	 * order, optionally converting a named map to a positional one.
	 */
	Map getParameterMap(boolean convertToPositional) {
		if (_params == null)
			return null;
		if (convertToPositional && !_params.isPositional())
			return _params.toPositional().getMap();
		return _params.getMap();
	}
	
	/**
	 * Cache this query if this query is amenable to caching and has not 
	 * already been cached. If the query can not be cached, then mark it as such
	 * to avoid computing for the same key again.
	 * 
	 * @return non-null if this query has already been cached. null if it can 
	 * not be cached or cached in this call or hint is set to ignore the cached
	 * version.
	 */
	PreparedQuery cache() {
		if (_id == null 
			|| !_em.getConfiguration().getPreparedQueryCache() 
			|| isHinted(QueryHints.HINT_IGNORE_PREPARED_QUERY)
			|| isHinted(QueryHints.HINT_INVALIDATE_PREPARED_QUERY))
			return null;
		PreparedQueryCache cache = _em.getConfiguration()
			.getPreparedQueryCacheInstance();
		if (cache.isCachable(_id) == Boolean.FALSE)
			return null;
		PreparedQuery cached = cache.get(_id);
		if (cached != null)
			return cached;
		
		String[] sqls = _query.getDataStoreActions(getParameterMap(true));
		boolean cacheable = (sqls.length == 1);
		if (!cacheable) {
			cache.markUncachable(_id);
			return null;
		}
		PreparedQuery newEntry = new PreparedQuery(_id, sqls[0], _query); 
		cache.cache(newEntry);
		return null; // because we cached it as a result of this call
	}
	
	boolean isHinted(String hint) {
		Object result = _query.getFetchConfiguration().getHint(hint);
		return result != null && "true".equalsIgnoreCase(result.toString());
	}
	
	/**
	 * Remove this query from PreparedQueryCache. 
	 */
	private boolean invalidatePreparedQuery() {
		if (!_em.getConfiguration().getPreparedQueryCache())
			return false;
		ignorePreparedQuery();
		PreparedQueryCache cache = _em.getConfiguration()
			.getPreparedQueryCacheInstance();
		return cache.invalidate(_id);
	}
	
	/**
	 * Ignores this query from PreparedQueryCache by recreating the original
	 * query if it has been cached. 
	 */
	private void ignorePreparedQuery() {
		PreparedQuery cached = _em.getPreparedQuery(_id);
		if (cached == null)
			return;
		Broker broker = _em.getBroker();
		// Critical assumption: Only JPQL queries are cached and more 
		// importantly, the identifier of the prepared query is the original
		// JPQL String
		String JPQL = JPQLParser.LANG_JPQL;
		String jpql = _id;
		
		org.apache.openjpa.kernel.Query newQuery = broker.newQuery(JPQL, jpql);
		newQuery.getFetchConfiguration().copy(_query.getFetchConfiguration());
		newQuery.compile();
		_query = new DelegatingQuery(newQuery, 
				broker.getInstanceExceptionTranslator());
	}
	
	private void recordStatistics(String query, boolean usingCachedVersion) {
		PreparedQueryCache cache = _em.getConfiguration()
			.getPreparedQueryCacheInstance();
		if (cache == null)
			return;
		cache.getStatistics().recordExecution(query,usingCachedVersion);
	}

	public int hashCode() {
		return _query.hashCode();
	}

	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof QueryImpl))
			return false;
		return _query.equals(((QueryImpl) other)._query);
	}
}
