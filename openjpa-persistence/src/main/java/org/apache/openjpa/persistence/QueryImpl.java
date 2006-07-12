/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence;

import java.io.Serializable;
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
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.DelegatingQuery;
import org.apache.openjpa.kernel.DelegatingResultList;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.util.Localizer;

/**
 * <p>Implementation of {@link Query} interface.</p>
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 * @nojavadoc
 */
public class QueryImpl
    implements OpenJPAQuery, Serializable {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static final Localizer _loc = Localizer.forPackage
        (QueryImpl.class);

    private final DelegatingQuery _query;
    private transient Broker _broker;    // for profiling
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
        _broker = em.getBroker();
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

    public int getOperation() {
        return _query.getOperation();
    }

    public FetchPlan getFetchPlan() {
        _query.assertNotSerialized();
        _query.lock();
        try {
            if (_fetch == null)
                _fetch = ((EntityManagerFactoryImpl) _em.
                    getEntityManagerFactory()).toFetchPlan(_query.
                    getFetchConfiguration());
            return _fetch;
        }
        finally {
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
        _query.setIgnoreChanges(ignore);
        return this;
    }

    public OpenJPAQuery addFilterListener(FilterListener listener) {
        _query.addFilterListener(listener);
        return this;
    }

    public OpenJPAQuery removeFilterListener(FilterListener listener) {
        _query.removeFilterListener(listener);
        return this;
    }

    public OpenJPAQuery addAggregateListener(AggregateListener listener) {
        _query.addAggregateListener(listener);
        return this;
    }

    public OpenJPAQuery removeAggregateListener(AggregateListener listener) {
        _query.removeAggregateListener(listener);
        return this;
    }

    public Collection getCandidateCollection() {
        return _query.getCandidateCollection();
    }

    public OpenJPAQuery setCandidateCollection(Collection coll) {
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
        if (OpenJPAPersistence.isManagedType(_em, cls))
            _query.setCandidateType(cls, true);
        else
            _query.setResultType(cls);
        return this;
    }

    public boolean hasSubclasses() {
        return _query.hasSubclasses();
    }

    public OpenJPAQuery setSubclasses(boolean subs) {
        Class cls = _query.getCandidateType();
        _query.setCandidateExtent(_query.getBroker().newExtent(cls, subs));
        return this;
    }

    public int getFirstResult() {
        return asInt(_query.getStartRange());
    }

    public OpenJPAQuery setFirstResult(int startPosition) {
        _query.setRange(startPosition, _query.getEndRange());
        return this;
    }

    public int getMaxResults() {
        return asInt(_query.getEndRange() - _query.getStartRange());
    }

    public OpenJPAQuery setMaxResults(int max) {
        long start = _query.getStartRange();
        if (max == Integer.MAX_VALUE)
            _query.setRange(start, Long.MAX_VALUE);
        else
            _query.setRange(start, start + max);
        return this;
    }

    public OpenJPAQuery compile() {
        _query.compile();
        return this;
    }

    private Object execute() {
        if (_query.getOperation() != OP_SELECT)
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
        Object ob = execute();
        if (!(ob instanceof List))
            return ob;

        List res = (List) ob;
        try {
            // don't use size() b/c can be inefficient under some LRS settings
            Iterator itr = res.iterator();
            if (!itr.hasNext())
                throw new NoResultException(_loc.get("no-results",
                    _query.getQueryString()), null, null, false);
            Object ret = itr.next();
            if (itr.hasNext())
                throw new NonUniqueResultException(_loc.get("mult-results",
                    _query.getQueryString()), null, null, false);
            return ret;
        }
        finally {
            OpenJPAPersistence.close(res);
        }
    }

    public int executeUpdate() {
        if (_query.getOperation() == OP_DELETE) {
            // handle which types of parameters we are using, if any
            if (_positional != null)
                return asInt(_query.deleteAll(_positional.toArray()));
            if (_named != null)
                return asInt(_query.deleteAll(_named));
            return asInt(_query.deleteAll());
        }
        if (_query.getOperation() == OP_UPDATE) {
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
     * Cast the specified long down to an int, first checking
     * for overflow.
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
        _query.getFetchConfiguration().setFlushBeforeQueries
            (EntityManagerImpl.toFlushBeforeQueries(flushMode));
        return this;
    }

    public OpenJPAQuery setHint(String key, Object value) {
        if (key == null || !key.startsWith("org.apache.openjpa."))
            return this;
        String k = key.substring("org.apache.openjpa.".length());

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
                Filters.hintToSetter(getFetchPlan(), k, value);
            } else if (k.startsWith("hint."))
                _query.getFetchConfiguration().setHint(key, value);
            else
                throw new ArgumentException(_loc.get("bad-query-hint", key),
                    null, null, false);
            return this;
        }
        catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    public OpenJPAQuery setParameter(int position, Calendar value,
        TemporalType t) {
        return setParameter(position, (Object) value);
    }

    public OpenJPAQuery setParameter(int position, Date value,
        TemporalType type) {
        return setParameter(position, (Object) value);
    }

    public OpenJPAQuery setParameter(int position, Object value) {
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
        }
        finally {
            _query.unlock();
        }
    }

    public OpenJPAQuery setParameter(String name, Calendar value,
        TemporalType t) {
        return setParameter(name, (Object) value);
    }

    public OpenJPAQuery setParameter(String name, Date value,
        TemporalType type) {
        return setParameter(name, (Object) value);
    }

    public OpenJPAQuery setParameter(String name, Object value) {
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
        }
        finally {
            _query.unlock();
        }
    }

    public boolean hasPositionalParameters() {
        return _positional != null;
    }

    public Object[] getPositionalParameters() {
        _query.lock();
        try {
            return (_positional == null) ? EMPTY_ARRAY : _positional.toArray();
        }
        finally {
            _query.unlock();
        }
    }

    public OpenJPAQuery setParameters(Object... params) {
        _query.lock();
        try {
            _positional = null;
            _named = null;
            if (params != null)
                for (int i = 0; i < params.length; i++)
                    setParameter(i + 1, params[i]);
            return this;
        }
        finally {
            _query.unlock();
        }
    }

    public Map getNamedParameters() {
        _query.lock();
        try {
            return (_named == null) ? Collections.EMPTY_MAP
                : Collections.unmodifiableMap(_named);
        }
        finally {
            _query.unlock();
        }
    }

    public OpenJPAQuery setParameters(Map params) {
        _query.lock();
        try {
            _positional = null;
            _named = null;
            if (params != null)
                for (Map.Entry e : (Set<Map.Entry>) params.entrySet())
                    setParameter((String) e.getKey(), e.getValue());
            return this;
        }
        finally {
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
