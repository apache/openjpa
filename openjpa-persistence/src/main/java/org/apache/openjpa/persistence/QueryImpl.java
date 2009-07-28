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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.Result;
import javax.persistence.ResultItem;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

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
@SuppressWarnings("serial")
public class QueryImpl<X> implements OpenJPAQuerySPI<X>, Serializable {

    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

	private DelegatingQuery _query;
	private transient EntityManagerImpl _em;
	private transient FetchPlan _fetch;

	private Map<String, QueryParameter<?>> _named;
	private Map<Integer, QueryParameter<?>> _positional;
	private String _id;
    private transient ReentrantLock _lock = null;
	private final HintHandler _hintHandler;

	private Map<Parameter<?>, Object> parmatersToValues = new HashMap<Parameter<?>, Object>();

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
		_lock = new ReentrantLock();
	}

	/**
	 * Constructor; supply factory and delegate.
	 * 
	 * @deprecated
	 */
	public QueryImpl(EntityManagerImpl em,
	        org.apache.openjpa.kernel.Query query) {
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

	public OpenJPAQuery<X> setIgnoreChanges(boolean ignore) {
		_em.assertNotCloseInvoked();
		_query.setIgnoreChanges(ignore);
		return this;
	}

	public OpenJPAQuery<X> addFilterListener(FilterListener listener) {
		_em.assertNotCloseInvoked();
		_query.addFilterListener(listener);
		return this;
	}

	public OpenJPAQuery<X> removeFilterListener(FilterListener listener) {
		_em.assertNotCloseInvoked();
		_query.removeFilterListener(listener);
		return this;
	}

	public OpenJPAQuery<X> addAggregateListener(AggregateListener listener) {
		_em.assertNotCloseInvoked();
		_query.addAggregateListener(listener);
		return this;
	}

    public OpenJPAQuery<X> removeAggregateListener(AggregateListener listener) {
		_em.assertNotCloseInvoked();
		_query.removeAggregateListener(listener);
		return this;
	}

	public Collection<?> getCandidateCollection() {
		return _query.getCandidateCollection();
	}

	public OpenJPAQuery<X> setCandidateCollection(Collection coll) {
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

	public OpenJPAQuery<X> setSubclasses(boolean subs) {
		_em.assertNotCloseInvoked();
		Class<?> cls = _query.getCandidateType();
        _query.setCandidateExtent(_query.getBroker().newExtent(cls, subs));
		return this;
	}

	public int getFirstResult() {
		return asInt(_query.getStartRange());
	}

	public OpenJPAQuery<X> setFirstResult(int startPosition) {
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

	public OpenJPAQuery<X> setMaxResults(int max) {
		_em.assertNotCloseInvoked();
		long start = _query.getStartRange();
		if (max == Integer.MAX_VALUE)
			_query.setRange(start, Long.MAX_VALUE);
		else
			_query.setRange(start, start + max);
		return this;
	}

	public OpenJPAQuery<X> compile() {
		_em.assertNotCloseInvoked();
		_query.compile();
		return this;
	}
	
	/**
	 * Gets a map of values of each parameter indexed by their original key.
	 * 
	 * @return an empty map if no parameter is registered for this query.
	 */
	Map<?,?> getParameterValues() {
	    Map<?,QueryParameter<?>> params = _positional != null ? _positional 
                : _named != null ? _named : new HashMap();
	    Map result = new LinkedHashMap();
	    for (Map.Entry<?,QueryParameter<?>> entry : params.entrySet()) {
	        result.put(entry.getKey(), entry.getValue().getValue());
	    }
	    return result;
	}
	
	private Object execute() {
        if (!isNative() && _query.getOperation() != QueryOperations.OP_SELECT)
            throw new InvalidStateException(_loc.get("not-select-query", getQueryString()), null, null, false);
		try {
		    lock();
            Map params = getParameterValues();
            boolean registered = preExecute(params);
            Object result = _query.execute(params);
            if (registered) {
                postExecute(result);
            }
            return result;
		} finally {
		    unlock();
		}
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
	public X getSingleResult() {
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
		    return (X)result.get(0);
		} catch (Exception e) {
            throw new NoResultException(_loc.get("no-result", getQueryString())
                .getMessage());
		}
	}

	public int executeUpdate() {
		_em.assertNotCloseInvoked();
        Map<?,?> paramValues = getParameterValues();
		if (_query.getOperation() == QueryOperations.OP_DELETE) {
		   return asInt(paramValues.isEmpty() ? _query.deleteAll() : _query.deleteAll(paramValues));
		}
		if (_query.getOperation() == QueryOperations.OP_UPDATE) {
	       return asInt(paramValues.isEmpty() ? _query.updateAll() : _query.updateAll(paramValues));
		}
        throw new InvalidStateException(_loc.get("not-update-delete-query", getQueryString()), null, null, false);
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

	public OpenJPAQuery<X> setFlushMode(FlushModeType flushMode) {
		_em.assertNotCloseInvoked();
		_query.getFetchConfiguration().setFlushBeforeQueries(
                EntityManagerImpl.toFlushBeforeQueries(flushMode));
		return this;
	}

	public OpenJPAQuery<X> setParameter(int position, Calendar value,
			TemporalType t) {
		return setParameter(position, convertTemporalType(value, t));
	}

    public OpenJPAQuery<X> setParameter(int position, Date value,
            TemporalType type) {
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

    /**
     * Bind an argument to a positional parameter.
     * @param position
     * @param value
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to a positional 
     * parameter of the query or if the argument is of incorrect type
     */    
	public OpenJPAQuery<X> setParameter(int position, Object value) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
		    // native queries are not parsed and hence their parameter types are not known
            if (!isNative() && !getDeclaredParameterKeys().contains(position)) {
                throw new IllegalArgumentException(_loc.get("param-missing-pos", 
                    position, getQueryString(), getDeclaredParameterKeys()).getMessage());
            }
			if (isNative() && position < 1) {
                throw new IllegalArgumentException(_loc.get("bad-pos-params",
                        position, getQueryString()).toString());
			}

			if (position < 1)
                throw new InvalidStateException(_loc.get("illegal-index",
                        position), null, null, false);

			Class<?> valueType = (Class<?>)_query.getParameterTypes().get((Object)position);
			ParameterImpl<Object> param = new ParameterImpl<Object>(position, valueType);
			registerParameter(param);
			param.bindValue(value);

			return this;
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery<X> setParameter(String name, Calendar value,
			TemporalType type) {
		return setParameter(name, convertTemporalType(value, type));
	}

    public OpenJPAQuery<X> setParameter(String name, Date value, TemporalType type) {
		return setParameter(name, convertTemporalType(value, type));
	}

    /**
     * Sets the parameter of the given name to the given value.
     */
	public OpenJPAQuery<X> setParameter(String name, Object value) {
		_query.assertOpen();
		_em.assertNotCloseInvoked();
		_query.lock();
		try {
            // native queries are not parsed and hence their parameter types are not known
            if (!isNative() && !getDeclaredParameterKeys().contains(name)) {
                throw new IllegalArgumentException(_loc.get("param-missing-name", 
                    name, getQueryString(), getDeclaredParameterKeys()).getMessage());
            }
			if (isNative()) {
                throw new IllegalArgumentException(_loc.get("no-named-params",
                        name, getQueryString()).toString());
			}
			
			Class<?> valueType = (Class<?>)_query.getParameterTypes().get(name);
			ParameterImpl<Object> param = new ParameterImpl<Object>(name, valueType);
			registerParameter(param);
			param.bindValue(value);
			
			return this;
		} finally {
			_query.unlock();
		}
	}

	/**
	 * Registers a parameter by creating an entry in the named or positional parameter map.
	 * The named or positional parameter map may be created if this is the first parameter
	 * to be registered.
	 * It is not permitted to mix named and positional parameter. So one of them is always
	 * null.
	 * 
	 * @exception IllegalStateException if a positional parameter is given when named parameters 
	 * had been registered or vice versa.
	 */
	QueryImpl<X> registerParameter(QueryParameter<?> param) {
	    if (param.isNamed()) {
	        if (_positional != null)
                throw new IllegalStateException(_loc.get("param-pos-named-mix", param, _query.getQueryString(), 
                        _positional.keySet()).getMessage());
	        if (_named == null) {
	            _named = new HashMap<String, QueryParameter<?>>();
	        }
	        _named.put(param.getName(), param);
	    } else if (param.isPositional()) {
            if (_named != null)
                throw new IllegalStateException(_loc.get("param-pos-named-mix", param, _query.getQueryString(), 
                        _named.keySet()).getMessage());
	        if (_positional == null) {
	            _positional = new TreeMap<Integer, QueryParameter<?>>();
	        }
	        _positional.put(param.getPosition(), param);
	    } else {
	        throw new IllegalStateException(_loc.get("param-no-key", param).getMessage());
	    }

	    return this;   
	}
	
	public boolean isNative() {
		return QueryLanguages.LANG_SQL.equals(getLanguage());
	}

	public boolean hasPositionalParameters() {
		return _positional != null;
	}

    /**
     * Gets the array of positional parameter values.
     * The n-th array element represents (n+1)-th positional parameter.  
     * If a parameter has been declared but not bound to a value then
     * the value is null and hence is indistinguishable from the value
     * being actually null.
     * If the parameter indexing is not contiguous then the unspecified
     * parameters are considered as null.
     */
    public Object[] getPositionalParameters() {
        _query.lock();
        try {
            if (_positional == null) 
                return EMPTY_ARRAY;
            Object[] result = new Object[calculateMaxKey(_positional.keySet())];
            for (Map.Entry<Integer, QueryParameter<?>> e : _positional.entrySet()) {
                result[e.getKey().intValue()-1] = e.getValue().getValue();
            }
            return result;
        } finally {
            _query.unlock();
        }
    }
    
    /**
     * Calculate the maximum value of the given set.
     */
    int calculateMaxKey(Set<Integer> p) {
        if (p == null)
            return 0;
        int max = Integer.MIN_VALUE;
        for (Integer i : p)
            max = Math.max(max, i);
        return max;
    }

	public OpenJPAQuery<X> setParameters(Object... params) {
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

	/**
	 * Gets the value of all the named parameters.
	 * If a parameter has been declared but not bound to a value then
	 * the value is null and hence is indistinguishable from the value
	 * being actually null.
	 */
	public Map<String, Object> getNamedParameters() {
		_query.lock();
		try {
            if (_named == null) 
                return Collections.EMPTY_MAP;
            Map<String, Object> result = new HashMap<String, Object>();
            for (Map.Entry<String, QueryParameter<?>> e : _named.entrySet()) {
                result.put(e.getKey(), e.getValue().getValue());
            }
            return result;
		} finally {
			_query.unlock();
		}
	}

	public OpenJPAQuery<X> setParameters(Map params) {
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

	public OpenJPAQuery<X> closeAll() {
		_query.closeAll();
		return this;
	}

	public String[] getDataStoreActions(Map params) {
		return _query.getDataStoreActions(params);
	}

    public LockModeType getLockMode() {
        return _fetch.getReadLockMode();
    }

    public TypedQuery<X> setLockMode(LockModeType lockMode) {
       _fetch.setReadLockMode(lockMode);
       return this;
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

    public OpenJPAQuery<X> setHint(String key, Object value) {
        _em.assertNotCloseInvoked();
        _hintHandler.setHint(key, value);
        return this;
    }

    public Set<String> getSupportedHints() {
        return _hintHandler.getSupportedHints();
    }

    /**
     * Returns the innermost implementation that is an instance of the given 
     * class. 
     * 
     * @throws PersistenceException if none in the delegate chain is an 
     * instance of the given class.
     * 
     * @since 2.0.0
     */
    public <T> T unwrap(Class<T> cls) {
        Object[] delegates = new Object[]{_query.getInnermostDelegate(), 
            _query.getDelegate(), _query, this};
        for (Object o : delegates) {
            if (cls.isInstance(o))
                return (T)o;
        }
        throw new PersistenceException(_loc.get("unwrap-query-invalid", cls)
            .toString(), null, this, false);
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
        QueryStatistics<String> stats = cache.getStatistics();
        if (alreadyCached && LANG_PREPARED_SQL.equals(lang)) {
            PreparedQuery pq = _em.getPreparedQuery(_id);
            if (pq.isInitialized()) {
                try {
                    Map rep = pq.reparametrize(params, _em.getBroker());
                    params.clear();
                    params.putAll(rep);
                } catch (UserException ue) {
                    invalidatePreparedQuery();
                    Log log = _em.getConfiguration().getLog(OpenJPAConfiguration.LOG_RUNTIME);
                    if (log.isWarnEnabled())
                        log.warn(ue.getMessage());
                    return false;
                }
            }
            stats.recordExecution(pq.getOriginalQuery());
        } else {
            stats.recordExecution(_query.getQueryString());
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
    private boolean postExecute(Object result) {
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
    
    // package protected
    QueryImpl setId(String id) {
        _id = id;
        return this;
    }
    
    void lock() {
        if (_lock != null) 
            _lock.lock();
    }

    void unlock() {
        if (_lock != null)
            _lock.unlock();
    }

    public <T> Parameter<T> getParameter(String name, Class<T> arg1) {
        return (Parameter<T>)_named.get(name);
    }

    public <T> Parameter<T> getParameter(int pos, Class<T> arg1) {
        return (Parameter<T>)_positional.get(pos);
    }

    /**
     * Return the value bound to the parameter.
     * @param param parameter object
     * @return parameter value
     * @throws IllegalStateException if the parameter has not been been bound
     * @throws IllegalArgumentException if the parameter does not belong to this query
     */
    public <T> T getParameterValue(Parameter<T> p) {
        if ((_named == null || !_named.containsValue(p)) 
         && (_positional == null || !_positional.containsValue(p))) {
           throw new IllegalArgumentException(_loc.get("param-missing", p, getQueryString(), 
               getBoundParameterKeys()).getMessage());
        }
        QueryParameter<T> param = (QueryParameter<T>)p;
        return (T)param.getValue(true);
    }

    /**
     * Gets the parameters registered for this query.
     */
    public Set<Parameter<?>> getParameters() {
        Set<Parameter<?>> result = new HashSet<Parameter<?>>();
        if (_named != null) {
            result.addAll(_named.values());
        } else if (_positional != null) {
            result.addAll(_positional.values());
        }
        return result;
    }

    public <T> ResultItem<T> getResultItem(String arg0, Class<T> arg1) {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public <T> ResultItem<T> getResultItem(int arg0, Class<T> arg1) {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public List<ResultItem<?>> getResultItems() {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public List<Result> getTypedResultList() {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public Result getTypedSingleResult() {
        throw new UnsupportedOperationException(
            "JPA 2.0 - Method not yet implemented");
    }

    public <T> OpenJPAQuery<X> setParameter(Parameter<T> p, T arg1) {
        QueryParameter<T> param = (QueryParameter<T>)p;
        param.bindValue(arg1);
        return this;
    }

    public OpenJPAQuery<X> setParameter(Parameter<Date> p, Date date, TemporalType type) {
        return setParameter(p, (Date)convertTemporalType(date, type));
    }

    public TypedQuery<X> setParameter(Parameter<Calendar> p, Calendar cal, TemporalType type) {
        return setParameter(p, (Calendar)convertTemporalType(cal, type));
    }

    public QueryParameter<?> getParameter(String name) {
        if (_named == null || !_named.containsKey(name))
            throw new IllegalArgumentException(_loc.get("param-missing-name", 
                name, getQueryString(), getBoundParameterKeys()).getMessage());
        return _named.get(name);
    }

    /**
     * Get the positional parameter with the given position.
     * 
     * @param position specified in the user query.
     * @return parameter object
     * @throws IllegalArgumentException if the parameter with the given position does not exist
     */
    public QueryParameter<?> getParameter(int pos) {
        if (_positional == null || !_positional.containsKey(pos))
            throw new IllegalArgumentException(_loc.get("param-missing-pos", 
                pos, getQueryString(), getBoundParameterKeys()).getMessage());
        return _positional.get(pos);
    }

    /**
     * Return the value bound to the parameter.
     * 
     * @param name name of the parameter
     * @return parameter value
     * 
     * @throws IllegalStateException if this parameter has not been bound
     */
    public Object getParameterValue(String name) {
        return getParameter(name).getValue(true);
    }

    /**
     * Return the value bound to the parameter.
     * 
     * @param pos position of the parameter
     * @return parameter value
     * 
     * @throws IllegalStateException if this parameter has not been bound
     */
    public Object getParameterValue(int pos) {
        return getParameter(pos).getValue(true);
    }
    
    /**
     * Gets the parameter keys bound with this query.
     */
    public Set<?> getBoundParameterKeys() {
        if (_named != null)
            return _named.keySet();
        if (_positional != null)
            return _positional.keySet();
        return Collections.EMPTY_SET;
    }
    
    /**
     * Gets the declared parameter keys in the given query.
     * This information is only available after the query has been parsed.
     * As native language queries are not parsed, this information is not available for them.
     *   
     * @return set of parameter identifiers in a parsed query
     */
    public Set<?> getDeclaredParameterKeys() {
        return _query.getParameterTypes().keySet();
    }

    public boolean isBound(Parameter<?> param) {
        return parmatersToValues.keySet().contains(param);
    }

}
