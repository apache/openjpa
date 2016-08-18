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

import org.apache.openjpa.kernel.DelegatingResultList;
import org.apache.openjpa.kernel.QueryResultCallback;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MultiQueryMetaData;
import org.apache.openjpa.util.RuntimeExceptionTranslator;
import org.apache.openjpa.util.UserException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

/**
 * Implements Store Procedure based query for JPA facade.
 * <br>
 * A {@link StoredProcedureQuery stored procedure query} differs from other query types because it may return
 * more than one result set, apart from an optional update count, whereas the traditional query processing in OpenJPA
 * via the abstractions of {@link ResultObjectProvider} and {@link org.apache.openjpa.jdbc.sql.Result}
 * assumed that a query will return its
 * result in a single list.
 * <br>
 * This query resorts to a callback mechanism, where the execution of the query returns not a result, but a
 * {@link QueryResultCallback callback object} that can be used to callback to OpenJPA kernel to get a series of
 * results via the traditional result processing pathway.
 *
 * @author Pinaki Poddar
 * @author Romain Manni-Bucau
 */
// TODO: add lock
public class StoredProcedureQueryImpl implements StoredProcedureQuery {
    private static final Localizer _loc = Localizer.forPackage(QueryImpl.class);

    private final String _name;
    private final QueryImpl<?> _delegate;
    private final MultiQueryMetaData _meta;
    private QueryResultCallback _callback;
    private boolean _declaredParams; // mainly a flag for now (null or not)

    /**
     * Construct a query for executing a Stored Procedure.
     *  @param procedureName name of the database stored procedure.
     * @param meta
     * @param delegate      the delegate which manages bind parameters on behalf of this
     */
    public StoredProcedureQueryImpl(String procedureName, MultiQueryMetaData meta, QueryImpl<?> delegate) {
        _name = procedureName;
        if (!isValidProcedureName(procedureName)) {
            throw new RuntimeException(procedureName + " is not a valid procedure name");
        }
        _meta = meta;
        _delegate = delegate;
        _delegate.compile();
    }

    /**
     * Gets the facade delegate that manages bind parameters on behalf of this receiver.
     *
     * @return
     */
    public OpenJPAQuery<?> getDelegate() {
        return _delegate;
    }

    /**
     * Gets the kernel delegate that is handles actual execution on behalf of this receiver.
     *
     * @return
     */
    public org.apache.openjpa.kernel.Query getExecutableQuery() {
        return _delegate.getDelegate();
    }

    private void buildParametersIfNeeded() {
        if (!_declaredParams) {
            for (MultiQueryMetaData.Parameter entry : _meta.getParameters()) {
                final Object key;
                final Parameter<?> param;
                if (entry.getName() == null) {
                    key = entry.getPosition();
                    param = new ParameterImpl(entry.getPosition(), entry.getType());
                } else {
                    key = entry.getName();
                    param = new ParameterImpl(entry.getName(), entry.getType());
                }
                _delegate.declareParameter(key, param);
            }
            _declaredParams = true;
        }
    }

    /**
     * Executes this receiver by delegation to the underlying executable query.
     * <br>
     * This method is multi-call safe. The underlying executable query is executed
     * <em>only</em> for the first invocation. Subsequent
     */
    @Override
    public boolean execute() {
        if (_callback == null) {
            _callback = (QueryResultCallback) getExecutableQuery().execute(_delegate.getParameterValues());
        }
        return _callback.getExecutionResult();
    }

    @Override
    public List getResultList() {
        execute();
        try {
            Object list = _callback.callback();
            RuntimeExceptionTranslator trans = PersistenceExceptions
                    .getRollbackTranslator(_delegate.getEntityManager());
            return new DelegatingResultList((ResultList) list, trans);
        } catch (Exception ex) {
            throw new javax.persistence.PersistenceException(ex);
        }
    }

    @Override
    public Object getSingleResult() {
        execute();
        try {
            ResultList result = (ResultList) _callback.callback();
            if (result == null || result.isEmpty())
                throw new NoResultException(_loc.get("no-result", _name)
                        .getMessage());
            if (result.size() > 1)
                throw new NonUniqueResultException(_loc.get("non-unique-result",
                        _name, result.size()).getMessage());
            RuntimeExceptionTranslator trans = PersistenceExceptions
                    .getRollbackTranslator(_delegate.getEntityManager());
            return new DelegatingResultList(result, trans).iterator().next();
        } catch (Exception ex) {
            throw new javax.persistence.PersistenceException(ex);
        }
    }

    @Override
    public boolean hasMoreResults() {
        return _callback != null && _callback.hasMoreResults();
    }

    @Override
    public int getUpdateCount() {
        assertExecuted();
        return _callback.getUpdateCount();
    }

    @Override
    public int executeUpdate() {
        execute();
        return _callback.getUpdateCount();
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        // TODO JPA 2.1 Method
        return _delegate.getParameter(name, type);
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        // TODO JPA 2.1 Method
        return _delegate.getParameter(position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        // TODO JPA 2.1 Method
        return _delegate.isBound(param);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        // TODO JPA 2.1 Method
        return _delegate.getParameterValue(param);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        // TODO JPA 2.1 Method
        return _delegate.unwrap(cls);
    }

    @Override
    public <T> StoredProcedureQuery setParameter(Parameter<T> param, T value) {
        buildParametersIfNeeded();
        _delegate.setParameter(param, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Calendar> param, Calendar cal, TemporalType temporalType) {
        buildParametersIfNeeded();
        _delegate.setParameter(param, cal, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        buildParametersIfNeeded();
        _delegate.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
        buildParametersIfNeeded();
        ParameterImpl param = new ParameterImpl(position, type);
        _delegate.declareParameter(position, param);
        return this;
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(String name, Class type, ParameterMode mode) {
        buildParametersIfNeeded();
        ParameterImpl param = new ParameterImpl(name, type);
        _delegate.declareParameter(name, param);
        return this;
    }

    @Override
    public Object getOutputParameterValue(int position) {
        return _callback == null ? null : _callback.getOut(position);
    }

    @Override
    public Object getOutputParameterValue(String parameterName) {
        return _callback == null ? null : _callback.getOut(parameterName);
    }

    @Override
    public javax.persistence.Query setMaxResults(int maxResult) {
        // TODO JPA 2.1 Method
        return _delegate.setMaxResults(maxResult);
    }

    @Override
    public int getMaxResults() {
        // TODO JPA 2.1 Method
        return _delegate.getMaxResults();
    }

    @Override
    public javax.persistence.Query setFirstResult(int startPosition) {
        // TODO JPA 2.1 Method
        return _delegate.setFirstResult(startPosition);
    }

    @Override
    public int getFirstResult() {
        // TODO JPA 2.1 Method
        return _delegate.getFirstResult();
    }

    @Override
    public Map<String, Object> getHints() {
        // TODO JPA 2.1 Method
        return _delegate.getHints();
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        buildParametersIfNeeded();
        return _delegate.getParameters();
    }

    @Override
    public Parameter<?> getParameter(String name) {
        buildParametersIfNeeded();
        return _delegate.getParameter(name);
    }

    @Override
    public Parameter<?> getParameter(int position) {
        buildParametersIfNeeded();
        return _delegate.getParameter(position);
    }

    @Override
    public Object getParameterValue(String name) {
        buildParametersIfNeeded();
        return _delegate.getParameterValue(name);
    }

    @Override
    public Object getParameterValue(int position) {
        buildParametersIfNeeded();
        return _delegate.getParameter(position);
    }

    @Override
    public FlushModeType getFlushMode() {
        // TODO JPA 2.1 Method
        return _delegate.getFlushMode();
    }

    @Override
    public javax.persistence.Query setLockMode(LockModeType lockMode) {
        // TODO JPA 2.1 Method
        return _delegate.setLockMode(lockMode);
    }

    @Override
    public LockModeType getLockMode() {
        // TODO JPA 2.1 Method
        return _delegate.getLockMode();
    }

    @Override
    public StoredProcedureQuery setHint(String hintName, Object value) {
        _delegate.setHint(hintName, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Object value) {
        buildParametersIfNeeded();
        _delegate.setParameter(name, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Calendar cal, TemporalType temporalType) {
        buildParametersIfNeeded();
        _delegate.setParameter(name, cal, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Date date, TemporalType temporalType) {
        buildParametersIfNeeded();
        _delegate.setParameter(name, date, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Object value) {
        buildParametersIfNeeded();
        _delegate.setParameter(position, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        buildParametersIfNeeded();
        _delegate.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Date value, TemporalType temporalType) {
        buildParametersIfNeeded();
        _delegate.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setFlushMode(FlushModeType flushMode) {
        // TODO JPA 2.1 Method
        _delegate.setFlushMode(flushMode);
        return this;
    }

    /**
     * Asserts that user has executed this query.
     */
    void assertExecuted() {
        if (_callback == null) {
            throw new UserException(this + " has not been executed");
        }
    }

    boolean isValidProcedureName(String s) {
        if (s == null || s.trim().length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '#' || ch == '$' || Character.isJavaIdentifierPart(ch))
                continue;
            return false;
        }
        return true;
    }

    public String toString() {
        return _name;
    }
}
