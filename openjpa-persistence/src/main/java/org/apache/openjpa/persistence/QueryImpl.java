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

import static org.apache.openjpa.kernel.QueryLanguages.LANG_PREPARED_SQL;

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.DelegatingQuery;
import org.apache.openjpa.kernel.DelegatingResultList;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.QueryStatistics;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.UserException;

/**
 * Implementation of {@link Query} interface.
 * 
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @nojavadoc
 */
public class QueryImpl implements OpenJPAQuerySPI, Serializable {

    private static final List EMPTY_LIST = new ArrayList(0);

	private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

	private DelegatingQuery _query;
	private transient EntityManagerImpl _em;
	private transient FetchPlan _fetch;

	private Map<String, Object> _named;
	private Map<Integer, Object> _positional;
	private String _id;
	
	private final HintHandler _hintHandler;

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
		_hintHandler = new HintHandler(this);
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
		if (!isNative() && _query.getOperation() != QueryOperations.OP_SELECT)
			throw new InvalidStateException(_loc.get("not-select-query", _query
					.getQueryString()), null, null, false);
		
        Map params = _positional != null ? _positional 
            : _named != null ? _named : new HashMap();
        boolean registered = preExecute(params);
        Object result = _query.execute(params);
        if (registered) {
            postExecute(result);
        }
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
		setHint("openjpa.hint.OptimizeResultCount", 1); // for DB2 optimization
		List result = getResultList();
		if (result == null || result.isEmpty())
			throw new NoResultException(_loc.get("no-result", getQueryString())
				.getMessage());
		if (result.size() > 1)
			throw new NonUniqueResultException(_loc.get("non-unique-result",
				getQueryString(), result.size()).getMessage());
		try {
		    return result.get(0);
		} catch (Exception e) {
            throw new NoResultException(_loc.get("no-result", getQueryString())
                .getMessage());
		}
	}

	public int executeUpdate() {
		_em.assertNotCloseInvoked();
		if (_query.getOperation() == QueryOperations.OP_DELETE) {
			// handle which types of parameters we are using, if any
			if (_positional != null)
				return asInt(_query.deleteAll(_positional));
			if (_named != null)
				return asInt(_query.deleteAll(_named));
			return asInt(_query.deleteAll());
		}
		if (_query.getOperation() == QueryOperations.OP_UPDATE) {
			// handle which types of parameters we are using, if any
			if (_positional != null)
				return asInt(_query.updateAll(_positional));
			if (_named != null)
				return asInt(_query.updateAll(_named));
			return asInt(_query.updateAll());
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
		_hintHandler.setHint(key, value);
		return this;
	}

	public OpenJPAQuery setParameter(int position, Calendar value,
			TemporalType t) {
		return setParameter(position, convertTemporalType(value, t));
	}

	public OpenJPAQuery setParameter(int position, Date value, TemporalType type) {
		return setParameter(position, convertTemporalType(value, type));
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
			if (isNative() && position < 1) {
				throw new IllegalArgumentException(_loc.get("bad-pos-params",
						position, _query.getQueryString()).toString());
			}
			// not allowed to mix positional and named parameters (EDR2 3.6.4)
			if (_named != null)
				throw new InvalidStateException(_loc.get(
						"no-pos-named-params-mix", _query.getQueryString()),
						null, null, false);

			if (position < 1)
				throw new InvalidStateException(_loc.get("illegal-index",
						position), null, null, false);

			if (_positional == null)
				_positional = new TreeMap<Integer, Object>();

			_positional.put(position, value);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery setParameter(String name, Calendar value,
			TemporalType type) {
		return setParameter(name, convertTemporalType(value, type));
	}

	public OpenJPAQuery setParameter(String name, Date value, TemporalType type) {
		return setParameter(name, convertTemporalType(value, type));
	}

	public OpenJPAQuery setParameter(String name, Object value) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			if (isNative()) {
				throw new IllegalArgumentException(_loc.get("no-named-params",
						name, _query.getQueryString()).toString());
			}
			// not allowed to mix positional and named parameters (EDR2 3.6.4)
			if (_positional != null)
				throw new InvalidStateException(_loc.get(
						"no-pos-named-params-mix", _query.getQueryString()),
						null, null, false);

			if (_named == null)
				_named = new HashMap();
			_named.put(name, value);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public boolean isNative() {
		return QueryLanguages.LANG_SQL.equals(getLanguage());
	}

	public boolean hasPositionalParameters() {
		return _positional != null;
	}

    /**
     * Gets the list of positional parameter values. A value of
     * <code>GAP_FILLER</code> indicates that user has not set the
     * corresponding positional parameter. A value of null implies that user has
     * set the value as null.
     */
    public List getPositionalParameters() {
        _query.lock();
        try {
            return (_positional == null) ? EMPTY_LIST : 
                    new ArrayList<Object>(_positional.values());
        } finally {
            _query.unlock();
        }
    }

	public OpenJPAQuery setParameters(Object... params) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			_positional = null;
			_named = null;
			if (params != null)
				for (int i = 0; i < params.length; i++)
					setParameter(i + 1, params[i]);
			return this;
		} finally {
			_query.unlock();
		}
	}

	public Map<String, Object> getNamedParameters() {
		_query.lock();
		try {
			return (_named == null) ? Collections.EMPTY_MAP : Collections
					.unmodifiableMap(_named);
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery setParameters(Map params) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
			_positional = null;
			_named = null;
			if (params != null)
				for (Map.Entry e : (Set<Map.Entry>) params.entrySet())
					setParameter((String) e.getKey(), e.getValue());
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

	/**
	 * Get all the active hints and their values.
	 * 
	 */
    //TODO: JPA 2.0 Hints that are not set to FetchConfiguration 
    public Map<String, Object> getHints() {
        return _hintHandler.getHints();
    }

    public LockModeType getLockMode() {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public Set<String> getSupportedHints() {
        return _hintHandler.getSupportedHints();
    }

    public Query setLockMode(LockModeType lockMode) {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public <T> T unwrap(Class<T> cls) {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }
    
    //
    // Prepared Query Cache related methods
    //
    
    /**
     * Invoked before a query is executed.
     * If this receiver is cached as a {@linkplain PreparedQuery prepared query}
     * then re-parameterizes the given user parameters. The given map is cleared
     * and re-parameterized values are filled in. 
     * 
     * @param params user supplied parameter key-values. Always supply a 
     * non-null map even if the user has not specified any parameter, because 
     * the same map will to be populated by re-parameterization.
     * 
     * @return true if this invocation caused the query being registered in the
     * cache. 
     */
    private boolean preExecute(Map params) {
        PreparedQueryCache cache = _em.getPreparedQueryCache();
        if (cache == null) {
            return false;
        }
        FetchConfiguration fetch = _query.getFetchConfiguration();
        Boolean registered = cache.register(_id, _query, fetch);
        boolean alreadyCached = (registered == null);
        String lang = _query.getLanguage();
        QueryStatistics stats = cache.getStatistics();
        if (alreadyCached && LANG_PREPARED_SQL.equals(lang)) {
            PreparedQuery pq = _em.getPreparedQuery(_id);
            if (pq.isInitialized()) {
                try {
                    Map rep = pq.reparametrize(params, _em.getBroker());
                    params.clear();
                    params.putAll(rep);
                } catch (UserException ue) {
                    invalidatePreparedQuery();
                    Log log = _em.getConfiguration().getLog(
                        OpenJPAConfiguration.LOG_RUNTIME);
                    if (log.isWarnEnabled())
                        log.warn(ue.getMessage());
                    return false;
                }
            }
            stats.recordExecution(pq.getOriginalQuery(), alreadyCached);
        } else {
            stats.recordExecution(_query.getQueryString(), alreadyCached);
        }
        return registered == Boolean.TRUE;
    }
    
    /**
     * Initialize the registered Prepared Query from the given opaque object.
     * 
     * @param result an opaque object representing execution result of a query
     * 
     * @return true if the prepared query can be initialized.
     */
    boolean postExecute(Object result) {
        PreparedQueryCache cache = _em.getPreparedQueryCache();
        if (cache == null) {
            return false;
        }
        return cache.initialize(_id, result) != null;
    }
    
    /**
     * Remove this query from PreparedQueryCache. 
     */
    boolean invalidatePreparedQuery() {
        PreparedQueryCache cache = _em.getPreparedQueryCache();
        if (cache == null)
            return false;
        ignorePreparedQuery();
        return cache.invalidate(_id);
    }
    
    /**
     * Ignores this query from PreparedQueryCache by recreating the original
     * query if it has been cached. 
     */
    void ignorePreparedQuery() {
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
        _query = new DelegatingQuery(newQuery, _em.getExceptionTranslator());
    }
    
    QueryImpl setId(String id) {
        _id = id;
        return this;
    }
}
