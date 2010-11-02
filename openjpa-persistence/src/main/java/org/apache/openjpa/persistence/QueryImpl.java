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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.DelegatingQuery;
import org.apache.openjpa.kernel.DelegatingResultList;
import org.apache.openjpa.kernel.DistinctResultList;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.Filters;
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
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.persistence.criteria.CriteriaBuilderImpl;
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

    private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

	private DelegatingQuery _query;
	private transient EntityManagerImpl _em;
	private transient FetchPlan _fetch;

    private Map<Parameter<?>, Object> _boundParams;
    private Map<Object, Parameter<?>> _declaredParams;
	private String _id;
    private transient ReentrantLock _lock = null;
	private HintHandler _hintHandler;

	/**
	 * Constructor; supply factory exception translator and delegate.
	 * 
	 * @param em  The EntityManager which created this query
	 * @param ret Exception translator for this query
	 * @param query The underlying "kernel" query.
	 */
	public QueryImpl(EntityManagerImpl em, RuntimeExceptionTranslator ret, org.apache.openjpa.kernel.Query query) {
		_em = em;
		_query = new DelegatingQuery(query, ret);
		_lock = new ReentrantLock();
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
		String result = _query.getQueryString();
		return result != null ? result : _id;
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

	public OpenJPAQuery<X> setResultClass(Class cls) {
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
	 * Gets a map of values of each parameter indexed by their <em>original</em> key.
	 * 
	 * @return an empty map if no parameter is declared for this query.
     * The unbound parameters has a value of null which is indistinguishable
     * from the value being bound to null.
	 */
	Map<Object,Object> getParameterValues() {
	    Map<Object,Object> result = new HashMap<Object,Object>();
	    if (_boundParams == null)
	        return result;
	    for (Map.Entry<Object,Parameter<?>> entry : getDeclaredParameters().entrySet()) {
	        Object paramKey = entry.getKey();
	        Parameter<?> param = entry.getValue();
	        result.put(paramKey, _boundParams.get(param));
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
		} catch (LockTimeoutException e) {
		    throw new QueryTimeoutException(e.getMessage(), new Throwable[]{e}, this);
		} finally {
		    unlock();
		}
	}
	
	public List getResultList() {
		_em.assertNotCloseInvoked();
		Object ob = execute();
		if (ob instanceof List) {
			List ret = (List) ob;
			if (ret instanceof ResultList) {
			    RuntimeExceptionTranslator trans = PersistenceExceptions.getRollbackTranslator(_em);
			    if (_query.isDistinct()) {
			        return new DistinctResultList((ResultList) ret, trans);
			    } else {
			        return new DelegatingResultList((ResultList) ret, trans);
			    }
			} else {
				return ret;
			}
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

	public boolean isNative() {
		return QueryLanguages.LANG_SQL.equals(getLanguage());
	}

	/**
	 * Asserts that this query is a JPQL or Criteria Query.
	 */
	void assertJPQLOrCriteriaQuery() {
        String language = getLanguage();
        if (JPQLParser.LANG_JPQL.equals(language) 
         || QueryLanguages.LANG_PREPARED_SQL.equals(language)
         || CriteriaBuilderImpl.LANG_CRITERIA.equals(language)) {
            return;
        } else {
            throw new IllegalStateException(_loc.get("not-jpql-or-criteria-query").getMessage());
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
        assertJPQLOrCriteriaQuery();
        return getFetchPlan().getReadLockMode();
    }

    /**
     * Sets lock mode on the given query.
     * If the target query has been prepared and cached, then ignores the cached version.
     * @see #ignorePreparedQuery()
     */
    public TypedQuery<X> setLockMode(LockModeType lockMode) {
        String language = getLanguage();
        if (QueryLanguages.LANG_PREPARED_SQL.equals(language)) {
            ignorePreparedQuery();
        }
        assertJPQLOrCriteriaQuery();
       getFetchPlan().setReadLockMode(lockMode);
       return this;
    }

	public int hashCode() {
        return (_query == null) ? 0 : _query.hashCode();
	}

	public boolean equals(Object other) {
		if (other == this)
			return true;
        if ((other == null) || (other.getClass() != this.getClass()))
            return false;
        if (_query == null)
            return false;
		return _query.equals(((QueryImpl) other)._query);
	}

	/**
	 * Get all the active hints and their values.
	 * 
	 */
    //TODO: JPA 2.0 Hints that are not set to FetchConfiguration 
    public Map<String, Object> getHints() {
        if (_hintHandler == null)
            return Collections.emptyMap();
        return _hintHandler.getHints();
    }

    public OpenJPAQuery<X> setHint(String key, Object value) {
        _em.assertNotCloseInvoked();
        if (_hintHandler == null) {
            _hintHandler = new HintHandler(this);
        }
        _hintHandler.setHint(key, value);
        return this;
    }

    public Set<String> getSupportedHints() {
        if (_hintHandler == null) {
            _hintHandler = new HintHandler(this);
        }
        return _hintHandler.getSupportedHints();
    }

    /**
     * Unwraps this receiver to an instance of the given class, if possible.
     * 
     * @exception if the given class is null, generic <code>Object.class</code> or a class
     * that is not wrapped by this receiver.  
     * 
     * @since 2.0.0
     */
    public <T> T unwrap(Class<T> cls) {
        Object[] delegates = new Object[]{_query.getInnermostDelegate(), _query.getDelegate(), _query, this};
        for (Object o : delegates) {
            if (cls != null && cls != Object.class && cls.isInstance(o))
                return (T)o;
        }
        // Set this transaction to rollback only (as per spec) here because the raised exception 
        // does not go through normal exception translation pathways
        RuntimeException ex = new PersistenceException(_loc.get("unwrap-query-invalid", cls).toString(), null, 
                this, false);
        if (_em.isActive())
            _em.setRollbackOnly(ex);
        throw ex;
    }

    
    // =======================================================================
    // Prepared Query Cache related methods
    // =======================================================================
    
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
        if (fetch.getReadLockLevel() != 0) {
            if (cache.get(_id) != null) {
                ignorePreparedQuery();
            }
            return false;
        }
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
            stats.recordExecution(getQueryString());
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
    // ================ End of Prepared Query related methods =====================
    
    void lock() {
        if (_lock != null) 
            _lock.lock();
    }

    void unlock() {
        if (_lock != null)
            _lock.unlock();
    }

    
    // =================================================================================
    //   Parameter processing routines
    // =================================================================================

    /**
     * Binds the parameter identified by the given position to the given value.
     * The parameter are bound to a value in the context of this query. 
     * The same parameter may be bound to a different value in the context of 
     * another query.
     * <br>
     * For non-native queries, the given position must be a valid position in
     * the declared parameters.
     * <br>
     * As native queries may not be parsed and hence their declared parameters
     * may not be known, setting an positional parameter has the side-effect
     * of a positional parameter being declared.
     *   
     * @param position positive, integer position of the parameter
     * @param value an assignment compatible value
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to a positional 
     * parameter of the query or if the argument is of incorrect type
     */    
    public OpenJPAQuery<X> setParameter(int pos, Object value) {
        _query.assertOpen();
        _em.assertNotCloseInvoked();
        _query.lock();
        try {
            if (pos < 1) {
                throw new IllegalArgumentException(_loc.get("illegal-index", pos).getMessage());
            }
            Parameter<?> param = null;
            if (isNative()) {
                param = new ParameterImpl<Object>(pos, Object.class);
                declareParameter(pos, param);
            } else {
                param = getParameter(pos);
            }
            bindValue(param, value);

            return this;
        } finally {
            _query.unlock();
        }
    }
    
    /**
     * Sets the value of the given positional parameter after conversion of the
     * given value to the given Temporal Type.
     */
    public OpenJPAQuery<X> setParameter(int position, Calendar value, TemporalType t) {
        return setParameter(position, convertTemporalType(value, t));
    }

    /**
     * Sets the value of the given named parameter after conversion of the
     * given value to the given Temporal Type.
     */
    public OpenJPAQuery<X> setParameter(int position, Date value, TemporalType type) {
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
     * Affirms if declared parameters use position identifier.
     */
    public boolean hasPositionalParameters() {
        return !getDeclaredParameterKeys(Integer.class).isEmpty();
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
            Set<Integer> positionalKeys = getDeclaredParameterKeys(Integer.class);
            Object[] result = new Object[calculateMaxKey(positionalKeys)];
            for (Integer pos : positionalKeys) {
                Parameter<?> param = getParameter(pos);
                result[pos.intValue()-1] = isBound(param) ? getParameterValue(pos) : null;
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

    /**
     * Binds the given values as positional parameters. 
     * The n-th array element value is set to a Parameter with (n+1)-th positional identifier.  
     */
    public OpenJPAQuery<X> setParameters(Object... params) {
        _query.assertOpen();
        _em.assertNotCloseInvoked();
        _query.lock();
        try {
            clearBinding();
            for (int i = 0; params != null && i < params.length; i++) {
                setParameter(i + 1, params[i]);
            }
            return this;
        } finally {
            _query.unlock();
        }
    }
    
    void clearBinding() {
        if (_boundParams != null)
            _boundParams.clear();
    }

    /**
     * Gets the value of all the named parameters.  
     * 
     * If a parameter has been declared but not bound to a value then
     * the value is null and hence is indistinguishable from the value
     * being actually null.
     */
    public Map<String, Object> getNamedParameters() {
        _query.lock();
        try {
            Map<String, Object> result = new HashMap<String, Object>();
            Set<String> namedKeys = getDeclaredParameterKeys(String.class);
            for (String name : namedKeys) {
                Parameter<?> param = getParameter(name);
                result.put(name, isBound(param) ? getParameterValue(name) : null);
            }
            return result;
        } finally {
            _query.unlock();
        }
    }

    /**
     * Sets the values of the parameters from the given Map.
     * The keys of the given map designate the name of the declared parameter.
      
     */
    public OpenJPAQuery<X> setParameters(Map params) {
        _query.assertOpen();
        _em.assertNotCloseInvoked();
        _query.lock();
        try {
            clearBinding();
            if (params != null)
                for (Map.Entry e : (Set<Map.Entry>) params.entrySet())
                    setParameter((String) e.getKey(), e.getValue());
            return this;
        } finally {
            _query.unlock();
        }
    }

    /**
     * Get the parameter of the given name and type.
     * 
     * @throws IllegalArgumentException if the parameter of the
     *         specified name does not exist or is not assignable
     *         to the type
     * @throws IllegalStateException if invoked on a native query 
     */
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        Parameter<?> param = getParameter(name);
        if (param.getParameterType().isAssignableFrom(type))
            throw new IllegalArgumentException(param + " does not match the requested type " + type);
        return (Parameter<T>)param;
    }

    /**
     * Get the positional parameter with the given position and type.
     * @throws IllegalArgumentException if the parameter with the
     *         specified position does not exist or is not assignable
     *         to the type
     * @throws IllegalStateException if invoked on a native query unless
     * the same parameter position is bound already.
     */
    public <T> Parameter<T> getParameter(int pos, Class<T> type) {
        Parameter<?> param = getParameter(pos);
        if (param.getParameterType().isAssignableFrom(type))
            throw new IllegalArgumentException(param + " does not match the requested type " + type);
        return (Parameter<T>)param;
    }

    /**
     * Return the value bound to the parameter.
     * @param param parameter object
     * @return parameter value
     * @throws IllegalStateException if the parameter has not been been bound
     * @throws IllegalArgumentException if the parameter does not belong to this query
     */
    public <T> T getParameterValue(Parameter<T> p) {
        if (!isBound(p)) {
           throw new IllegalArgumentException(_loc.get("param-missing", p, getQueryString(), 
               getBoundParameterKeys()).getMessage());
        }
        return (T)_boundParams.get(p);
    }

    /**
     * Gets the parameters declared in this query.
     */
    public Set<Parameter<?>> getParameters() {
        Set<Parameter<?>> result = new HashSet<Parameter<?>>();
        result.addAll(getDeclaredParameters().values());
        return result;
    }

    public <T> OpenJPAQuery<X> setParameter(Parameter<T> p, T arg1) {
        bindValue((Parameter<T>)p, arg1);
        return this;
    }

    public OpenJPAQuery<X> setParameter(Parameter<Date> p, Date date, TemporalType type) {
        return setParameter(p, (Date)convertTemporalType(date, type));
    }

    public TypedQuery<X> setParameter(Parameter<Calendar> p, Calendar cal, TemporalType type) {
        return setParameter(p, (Calendar)convertTemporalType(cal, type));
    }

    /**
     * Get the parameter object corresponding to the declared parameter of the given name.
     * This method is not required to be supported for native queries.
     * 
     * @throws IllegalArgumentException if the parameter of the specified name does not exist
     * @throws IllegalStateException if invoked on a native query
     */
    public Parameter<?> getParameter(String name) {
        if (isNative()) {
            throw new IllegalStateException(_loc.get("param-named-non-native", name).getMessage());
        }
        Parameter<?> param = getDeclaredParameters().get(name);
        if (param == null) {
            Set<ParameterExpression> exps = getDeclaredParameterKeys(ParameterExpression.class);
            for (ParameterExpression<?> e : exps) {
                if (name.equals(e.getName()))
                    return e;
            }
            throw new IllegalArgumentException(_loc.get("param-missing-name", 
                name, getQueryString(), getDeclaredParameterKeys()).getMessage());
        }
        return param;
    }

    /**
     * Get the positional parameter with the given position.
     * The parameter may just have been declared and not bound to a value.
     * 
     * @param position specified in the user query.
     * @return parameter object
     * @throws IllegalArgumentException if the parameter with the given position does not exist
     */
    public Parameter<?> getParameter(int pos) {
        Parameter<?> param = getDeclaredParameters().get(pos);
        if (param == null)
            throw new IllegalArgumentException(_loc.get("param-missing-pos", 
                pos, getQueryString(), getDeclaredParameterKeys()).getMessage());
        return param;
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
        return _boundParams.get(getParameter(name));
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
        Parameter<?> param = getParameter(pos);
        assertBound(param);
        return _boundParams.get(param);
    }
    
    /**
     * Gets the parameter keys bound with this query.
     * Parameter key can be Integer, String or a ParameterExpression itself
     * but all parameters keys of a particular query are of the same type.
     */
    public Set<?> getBoundParameterKeys() {
        if (_boundParams == null)
            return Collections.EMPTY_SET;
        getDeclaredParameters();
        Set<Object> result = new HashSet<Object>();
        for (Map.Entry<Object, Parameter<?>> entry : _declaredParams.entrySet()) {
            if (isBound(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Gets the declared parameter keys in the given query.
     * This information is only available after the query has been parsed.
     * As native language queries are not parsed, this information is not available for them.
     *   
     * @return set of parameter identifiers in a parsed query
     */
    public Set<?> getDeclaredParameterKeys() {
        return getDeclaredParameters().keySet();
    }
    
    public <T> Set<T> getDeclaredParameterKeys(Class<T> keyType) {
        Set<T> result = new HashSet<T>();
        for (Object key : getDeclaredParameterKeys()) {
            if (keyType.isInstance(key))
                result.add((T)key);
        }
        return result;
    }
    
    /**
     * Gets the parameter instances declared in this query.
     * All parameter keys are of the same type. It is not allowed to mix keys of different type
     * such as named and positional keys. 
     * 
     * For string-based queries, the parser supplies the information about the declared parameters
     * as a LinkedMap of expected parameter value type indexed by parameter identifier.
     * For non string-based queries that a facade itself may construct (e.g. CriteriaQuery),
     * the parameters must be declared by the caller.
     * This receiver constructs concrete Parameter instances from the given parameter identifiers.
     * 
     * @return a Map where the key represents the original identifier of the parameter (can be a String,
     * Integer or a ParameterExpression itself) and the value is the concrete Parameter instance
     * either constructed as a result of this call or supplied by declaring the parameter explicitly
     * via {@linkplain #declareParameter(Parameter)}.
     */
    public Map<Object, Parameter<?>> getDeclaredParameters() {
        if (_declaredParams == null) {
            _declaredParams = new HashMap<Object, Parameter<?>>();
            OrderedMap<Object,Class<?>> paramTypes = _query.getOrderedParameterTypes();
            for(Entry<Object,Class<?>> entry : paramTypes.entrySet()){
                Object key = entry.getKey();    
                Class<?> expectedValueType = entry.getValue();
                Parameter<?> param;

                if (key instanceof Integer) {
                    param = new ParameterImpl((Integer)key, expectedValueType);
                } else if (key instanceof String) {
                    param = new ParameterImpl((String)key, expectedValueType);
                } else if (key instanceof Parameter) {
                    param = (Parameter<?>)key;
                } else {
                    throw new IllegalArgumentException("parameter identifier " + key + " unrecognized");
                }
                declareParameter(key, param);
            }
        }
        return _declaredParams;
    }
    
    /**
     * Declares the given parameter for this query. 
     * Used by non-string based queries that are constructed by the facade itself rather than
     * OpenJPA parsing the query to detect the declared parameters.
     * 
     * @param key this is the key to identify the parameter later in the context of this query.
     * Valid key types are Integer, String or ParameterExpression itself.
     * @param the parameter instance to be declared
     */
    public void declareParameter(Object key, Parameter<?> param) {
        if (_declaredParams == null) {
            _declaredParams = new HashMap<Object, Parameter<?>>();
        }
        _declaredParams.put(key, param);
    }

    /**
     * Affirms if the given parameter is bound to a value for this query.
     */
    public boolean isBound(Parameter<?> param) {
        return _boundParams != null && _boundParams.containsKey(param);
    }
    
    void assertBound(Parameter<?> param) {
        if (!isBound(param)) {
            throw new IllegalStateException(_loc.get("param-not-bound", param, getQueryString(), 
                    getBoundParameterKeys()).getMessage());
        }
    }
    /**
     * Binds the given value to the given parameter.
     * Validates if the parameter can accept the value by its type.
     */
    void bindValue(Parameter<?> param, Object value) {
        assertValueAssignable(param, value);
        if (_boundParams == null)
            _boundParams = new HashMap<Parameter<?>, Object>();
        _boundParams.put(param, value);
    }

    public OpenJPAQuery<X> setParameter(String name, Calendar value, TemporalType type) {
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
            // native queries can not have named parameters
            if (isNative()) {
                throw new IllegalArgumentException(_loc.get("no-named-params",
                        name, getQueryString()).toString());
            } else {
                bindValue(getParameter(name), value);
            }
            
            return this;
        } finally {
            _query.unlock();
        }
    }

    void assertValueAssignable(Parameter<?> param, Object v) {
        if (v == null) {
            if (param.getParameterType().isPrimitive())
                throw new IllegalArgumentException(_loc.get("param-null-primitive", param).getMessage());
            return;
        }
        if (!Filters.canConvert(v.getClass(), param.getParameterType(), true)) {
            throw new IllegalArgumentException(_loc.get("param-type-mismatch", new Object[]{
                param, getQueryString(), v, v.getClass().getName(), param.getParameterType().getName()}).getMessage());
        }
    }
    
    // ================== End of Parameter Processing routines ================================
    
    public String toString() {
        String result = _query.getQueryString(); 
        return result != null ? result : _id;
    }
}
