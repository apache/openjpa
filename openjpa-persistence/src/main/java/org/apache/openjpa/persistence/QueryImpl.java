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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.swing.text.Position;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.kernel.DelegatingQuery;
import org.apache.openjpa.kernel.DelegatingResultList;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.RuntimeExceptionTranslator;

/**
 * Implementation of {@link Query} interface.
 * 
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @nojavadoc
 */
public class QueryImpl implements OpenJPAQuerySPI, Serializable {

	private static final Object[] EMPTY_ARRAY = new Object[0];

	private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

	private final DelegatingQuery _query;
	private transient EntityManagerImpl _em;
	private transient FetchPlan _fetch;

	private Map<String, Object> _named;
	private Map<Integer, Object> _positional;

	private static Object GAP_FILLER = new Object();

	/**
	 * Constructor; supply factory exception translator and delegate.
	 * 
	 * @param em
	 *            The EntityManager which created this query
	 * @param ret
	 *            Exception translater for this query
	 * @param query
	 *            The underlying "kernel" query.
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

		validateParameters();

		// handle which types of parameters we are using, if any
		if (_positional != null)
			return _query.execute(_positional);
		if (_named != null)
			return _query.execute(_named);
		return _query.execute();
	}
	
	/**
	 * Validate that the types of the parameters are correct.
	 * The idea is to catch as many validation error as possible at the facade
	 * layer itself.
	 * 
	 * The expected parameters are parsed from the query and in a LinkedMap 
	 *	key   : name of the parameter as declared in query
	 *  value : expected Class of allowed value
	 *  
	 * The bound parameters depends on positional or named parameter style
	 * 
	 * TreeMap<Integer, Object> for positional parameters:
	 *   key   : 1-based Integer index
	 *   value : bound value. GAP_FILLER if the position is not set. This
	 *   simplifies validation at the kernel layer
	 *   
	 * Map<String, Object> for named parameters:
	 *   key   : parameter name
	 *   value : the bound value
	 *   
	 *  Validation accounts for 
	 *    a) gaps in positional parameters
	 *       SELECT p FROM PObject p WHERE p.a1=?1 AND p.a3=?3
	 *    
	 *    b) repeated parameters
	 *       SELECT p FROM PObject p WHERE p.a1=?1 AND p.a2=?1 AND p.a3=?2
	 *       
	 *    c) parameter is bound but not declared
	 *    
	 *    d) parameter is declared but not bound
	 *    
	 *    e) parameter does not match the value type
	 *    
	 *    f) parameter is primitive type but bound to null value
	 */
	private void validateParameters() {
		String query = getQueryString();
		if (_positional != null) {
			LinkedMap expected = _query.getParameterTypes();
			Map<Integer, Object> actual = _positional;
			for (Object o : expected.keySet()) {
				String position = (String) o;
				Class expectedParamType = (Class) expected.get(position);
				try {
					Integer.parseInt(position);
				} catch (NumberFormatException ex) {
					newValidationException("param-style-mismatch", query,
							expected.asList(),
							Arrays.toString(actual.keySet().toArray()));
				}
				Object actualValue = actual.get(Integer.parseInt(position));
				boolean valueUnspecified = (actualValue == GAP_FILLER)
						|| (actualValue == null && (actual.size() < expected
								.size()));
				if (valueUnspecified) 
					newValidationException("param-missing", position, query,
							Arrays.toString(actual.keySet().toArray()));
				
				if (expectedParamType.isPrimitive() && actualValue == null)
					newValidationException("param-type-null", 
							position, query, expectedParamType.getName());
				if (actualValue == null)
					continue;
				if (!Filters.wrap(expectedParamType).isInstance(actualValue)) 
					newValidationException("param-type-mismatch",
							position, query, actualValue,
							actualValue.getClass().getName(),
							expectedParamType.getName());
				
			}
			for (Integer position : actual.keySet()) {
				Object actualValue = actual.get(position);
				Class expectedParamType = (Class) expected.get("" + position);
				boolean paramExpected = expected.containsKey("" + position);
				if (actualValue == GAP_FILLER) {
					if (paramExpected) {
						newValidationException("param-missing", position, query,
								Arrays.toString(actual.keySet().toArray()));
					}
				} else {
					if (!paramExpected)
						newValidationException("param-extra", position, query,
								expected.asList());
					if (expectedParamType.isPrimitive() && actualValue == null)
						newValidationException("param-type-null", 
								position, query, expectedParamType.getName());
					if (!Filters.wrap(expectedParamType)
							.isInstance(actualValue)) 
						newValidationException("param-type-mismatch",
								position, query, actualValue,
								actualValue.getClass().getName(),
								expectedParamType.getName());
					
				}
			}

		} else if (_named != null) {
			LinkedMap expected = _query.getParameterTypes();
			// key : name of the parameter used while binding
			// value : user supplied parameter value. null may mean either
			// user has supplied a value or not specified at all
			Map<String, Object> actual = _named;
			for (Object o : expected.keySet()) {
				String expectedName = (String) o;
				Class expectedParamType = (Class) expected.get(expectedName);
				Object actualValue = actual.get(expectedName);
				boolean valueUnspecified = !actual.containsKey(expectedName);
				if (valueUnspecified) {
					newValidationException("param-missing", expectedName, query,
							Arrays.toString(actual.keySet().toArray()));
				}
				if (expectedParamType.isPrimitive() && actualValue == null)
					newValidationException("param-type-null", 
							expectedName, query, expectedParamType.getName());
				if (actualValue == null)
					continue;
				if (!Filters.wrap(expectedParamType).isInstance(actualValue)) {
					newValidationException("param-type-mismatch",
							expectedName, query, actualValue,
							actualValue.getClass().getName(),
							expectedParamType.getName());
				}
			}
			for (String actualName : actual.keySet()) {
				Object actualValue = actual.get(actualName);
				Class expectedParamType = (Class) expected.get(actualName);
				boolean paramExpected = expected.containsKey(actualName);
				if (!paramExpected) {
					newValidationException("param-extra", actualName, query,
							expected.asList());
				}
				if (expectedParamType.isPrimitive() && actualValue == null)
					newValidationException("param-type-null", 
							actualName, query, expectedParamType.getName());
				if (!Filters.wrap(expectedParamType).isInstance(actualValue)) {
					newValidationException("param-type-mismatch",
							actualName, query, actualValue,
							actualValue.getClass().getName(),
							expectedParamType.getName());
				}
			}
		}
	}

	void newValidationException(String msgKey, Object...args) {
		throw new ArgumentException(_loc.get(msgKey, args), null, null, false);
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
				}
				_query.getFetchConfiguration().setHint(key, value);
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
			for (int i = 1; i < position; i++)
				if (!_positional.containsKey(i))
					_positional.put(i, GAP_FILLER);

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
	 * Gets the array of positional parameter values. A value of
	 * <code>GAP_FILLER</code> indicates that user has not set the
	 * corresponding positional parameter. A value of null implies that user has
	 * set the value as null.
	 */
	public Object[] getPositionalParameters() {
		_query.lock();
		try {
			return (_positional == null) ? EMPTY_ARRAY : _positional.values()
					.toArray();
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

	public Map getNamedParameters() {
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
}
