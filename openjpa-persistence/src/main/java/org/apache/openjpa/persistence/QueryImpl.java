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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;

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

/**
 * Implementation of {@link Query} interface.
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @nojavadoc
 */
public class QueryImpl
    implements OpenJPAQuerySPI, Serializable {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static final Localizer _loc = Localizer.forPackage
        (QueryImpl.class);

    private final DelegatingQuery _query;
    private transient EntityManagerImpl _em;
    private transient FetchPlan _fetch;

    private Map _named;
    private List _positional;

    /**
     * Constructor; supply factory and delegate.
     */
    public QueryImpl(EntityManagerImpl em,
        org.apache.openjpa.kernel.Query query) {
        _em = em;
        _query = new DelegatingQuery(query,
            PersistenceExceptions.getRollbackTranslator(em));
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
                _fetch = ((EntityManagerFactoryImpl) _em.
                    getEntityManagerFactory()).toFetchPlan(_query.getBroker(),
                    _query.getFetchConfiguration());
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
            end = startPosition +
                (_query.getEndRange() - _query.getStartRange());
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
        if (! isNative() && _query.getOperation() != QueryOperations.OP_SELECT)
            throw new InvalidStateException(_loc.get("not-select-query",
                _query.getQueryString()), null, null, false);

        validateParameters();

        // handle which types of parameters we are using, if any
        if (_positional != null)
            return _query.execute(_positional.toArray());
        if (_named != null)
            return _query.execute(_named);
        return _query.execute();
    }

    /**
     * Validate that the types of the parameters are correct.
     */
    private void validateParameters() {
        if (_positional != null) {
            LinkedMap types = _query.getParameterTypes();
            for (int i = 0,
                size = Math.min(_positional.size(), types.size());
                i < size; i++)
                validateParameter(String.valueOf(i),
                    (Class) types.getValue(i), _positional.get(i));
        } else if (_named != null) {
            Map types = _query.getParameterTypes();
            for (Iterator i = _named.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String name = (String) entry.getKey();
                validateParameter(name, (Class) types.get(name),
                    entry.getValue());
            }
        }
    }

    private void validateParameter(String paramDesc, Class type, Object param) {
        // null parameters are allowed, so are not validated
        if (param == null || type == null)
            return;

        // check the parameter against the wrapped type
        if (!Filters.wrap(type).isInstance(param))
            throw new ArgumentException(_loc.get("bad-param-type",
                paramDesc, param.getClass().getName(), type.getName()),
                null, null, false);
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
                return asInt(_query.deleteAll(_positional.toArray()));
            if (_named != null)
                return asInt(_query.deleteAll(_named));
            return asInt(_query.deleteAll());
        }
        if (_query.getOperation() == QueryOperations.OP_UPDATE) {
            // handle which types of parameters we are using, if any
            if (_positional != null)
                return asInt(_query.updateAll(_positional.toArray()));
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
        return EntityManagerImpl.fromFlushBeforeQueries(_query.
            getFetchConfiguration().getFlushBeforeQueries());
    }

    public OpenJPAQuery setFlushMode(FlushModeType flushMode) {
        _em.assertNotCloseInvoked();
        _query.getFetchConfiguration().setFlushBeforeQueries
            (EntityManagerImpl.toFlushBeforeQueries(flushMode));
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
                addFilterListener(Filters.hintToFilterListener(value,
                    _query.getBroker().getClassLoader()));
            else if ("FilterListeners".equals(k)) {
                FilterListener[] arr = Filters.hintToFilterListeners(value,
                    _query.getBroker().getClassLoader());
                for (int i = 0; i < arr.length; i++)
                    addFilterListener(arr[i]);
            } else if ("AggregateListener".equals(k))
                addAggregateListener(Filters.hintToAggregateListener(value,
                    _query.getBroker().getClassLoader()));
            else if ("FilterListeners".equals(k)) {
                AggregateListener[] arr = Filters.hintToAggregateListeners
                    (value, _query.getBroker().getClassLoader());
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
                        throw new ArgumentException(_loc.get
                            ("bad-query-hint-value", key, value), null, null, 
                            false);
                }
                _query.getFetchConfiguration().setHint(key, value);
            }
            else
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
        return setParameter(position, value);
    }

    public OpenJPAQuery setParameter(int position, Date value,
        TemporalType type) {
        return setParameter(position, value);
    }

    public OpenJPAQuery setParameter(int position, Object value) {
        _query.assertOpen();
        _em.assertNotCloseInvoked();
        _query.lock();
        try {
            // not allowed to mix positional and named parameters (EDR2 3.6.4)
            if (_named != null)
                throw new InvalidStateException(_loc.get
                    ("no-pos-named-params-mix", _query.getQueryString()),
                    null, null, false);

            if (position < 1)
                throw new InvalidStateException(_loc.get
                    ("illegal-index", position), null, null, false);

            if (_positional == null)
                _positional = new ArrayList();

            // make sure it is at least the requested size
            while (_positional.size() < position)
                _positional.add(null);

            // note that we add it to position - 1, since setPosition
            // starts at 1, while List starts at 0
            _positional.set(position - 1, value);
            return this;
        } finally {
            _query.unlock();
        }
    }

    public OpenJPAQuery setParameter(String name, Calendar value,
        TemporalType t) {
        return setParameter(name, value);
    }

    public OpenJPAQuery setParameter(String name, Date value,
        TemporalType type) {
        return setParameter(name, value);
    }

    public OpenJPAQuery setParameter(String name, Object value) {
        _query.assertOpen();
        _em.assertNotCloseInvoked();
        _query.lock();
        try {
            // not allowed to mix positional and named parameters (EDR2 3.6.4)
            if (_positional != null)
                throw new InvalidStateException(_loc.get
                    ("no-pos-named-params-mix", _query.getQueryString()),
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

    public Object[] getPositionalParameters() {
        _query.lock();
        try {
            return (_positional == null) ? EMPTY_ARRAY : _positional.toArray();
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
            return (_named == null) ? Collections.EMPTY_MAP
                : Collections.unmodifiableMap(_named);
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
