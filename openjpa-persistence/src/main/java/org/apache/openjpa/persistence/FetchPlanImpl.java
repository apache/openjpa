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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.LockModeType;

import org.apache.openjpa.kernel.DataCacheRetrieveMode;
import org.apache.openjpa.kernel.DataCacheStoreMode;
import org.apache.openjpa.kernel.DelegatingFetchConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;

/**
 * Implements FetchPlan via delegation to FetchConfiguration.
 *
 * @author Abe White
 * @author Pinaki Poddar
 * @since 0.4.1
 * @nojavadoc
 */
public class FetchPlanImpl
	implements FetchPlan {

    private final DelegatingFetchConfiguration _fetch;
    private FetchPlanHintHandler _hintHandler;
    
    /**
     * Constructor; supply delegate.
     */
    public FetchPlanImpl(FetchConfiguration fetch) {
        _fetch = newDelegatingFetchConfiguration(fetch);
        _hintHandler = new FetchPlanHintHandler(this);
    }

    /**
     * Create a new exception-translating delegating fetch configuration.
     */
    protected DelegatingFetchConfiguration newDelegatingFetchConfiguration
        (FetchConfiguration fetch) {
        return new DelegatingFetchConfiguration(fetch,
            PersistenceExceptions.TRANSLATOR);
    }

    /**
     * Delegate.
     */
    public FetchConfiguration getDelegate() {
        return _fetch.getDelegate();
    }

    public int getMaxFetchDepth() {
        return _fetch.getMaxFetchDepth();
    }

    public FetchPlan setMaxFetchDepth(int depth) {
        _fetch.setMaxFetchDepth(depth);
        return this;
    }

    public int getFetchBatchSize() {
        return _fetch.getFetchBatchSize();
    }

    public FetchPlan setFetchBatchSize(int fetchBatchSize) {
        _fetch.setFetchBatchSize(fetchBatchSize);
        return this;
    }

    public boolean getQueryResultCacheEnabled() {
        return _fetch.getQueryCacheEnabled();
    }

    public FetchPlan setQueryResultCacheEnabled(boolean cache) {
        _fetch.setQueryCacheEnabled(cache);
        return this;
    }

    public boolean getQueryResultCache() {
        return getQueryResultCacheEnabled();
    }

    public FetchPlan setQueryResultCache(boolean cache) {
        return setQueryResultCacheEnabled(cache);
    }

    public Collection<String> getFetchGroups() {
        return _fetch.getFetchGroups();
    }

    public FetchPlan addFetchGroup(String group) {
        _fetch.addFetchGroup(group);
        return this;
    }

    public FetchPlan addFetchGroups(String... groups) {
        return addFetchGroups(Arrays.asList(groups));
    }

    public FetchPlan addFetchGroups(Collection groups) {
        _fetch.addFetchGroups(groups);
        return this;
    }

    public FetchPlan removeFetchGroup(String group) {
        _fetch.removeFetchGroup(group);
        return this;
    }

    public FetchPlan removeFetchGroups(String... groups) {
        return removeFetchGroups(Arrays.asList(groups));
    }

    public FetchPlan removeFetchGroups(Collection groups) {
        _fetch.removeFetchGroups(groups);
        return this;
    }

    public FetchPlan clearFetchGroups() {
        _fetch.clearFetchGroups();
        return this;
    }

    public FetchPlan resetFetchGroups() {
        _fetch.resetFetchGroups();
        return this;
    }

    public Collection<String> getFields() {
        return (Collection<String>) _fetch.getFields();
    }

    public boolean hasField(String field) {
        return _fetch.hasField(field);
    }

    public boolean hasField(Class cls, String field) {
        return hasField(toFieldName(cls, field));
    }

    public FetchPlan addField(String field) {
        _fetch.addField(field);
        return this;
    }

    public FetchPlan addField(Class cls, String field) {
        return addField(toFieldName(cls, field));
    }

    public FetchPlan addFields(String... fields) {
        return addFields(Arrays.asList(fields));
    }

    public FetchPlan addFields(Class cls, String... fields) {
        return addFields(cls, Arrays.asList(fields));
    }

    public FetchPlan addFields(Collection fields) {
        _fetch.addFields(fields);
        return this;
    }

    public FetchPlan addFields(Class cls, Collection fields) {
        return addFields(toFieldNames(cls, fields));
    }

    public FetchPlan removeField(String field) {
        _fetch.removeField(field);
        return this;
    }

    public FetchPlan removeField(Class cls, String field) {
        return removeField(toFieldName(cls, field));
    }

    public FetchPlan removeFields(String... fields) {
        return removeFields(Arrays.asList(fields));
    }

    public FetchPlan removeFields(Class cls, String... fields) {
        return removeFields(cls, Arrays.asList(fields));
    }

    public FetchPlan removeFields(Collection fields) {
        _fetch.removeFields(fields);
        return this;
    }

    public FetchPlan removeFields(Class cls, Collection fields) {
        return removeFields(toFieldNames(cls, fields));
    }

    public FetchPlan clearFields() {
        _fetch.clearFields();
        return this;
    }

    private static String toFieldName(Class cls, String field) {
        return cls.getName() + "." + field;
    }

    private static Collection toFieldNames(Class cls, Collection fields) {
        if (fields.isEmpty())
            return fields;
        Collection names = new ArrayList(fields);
        for (Iterator itr = fields.iterator(); itr.hasNext();)
            names.add(toFieldName(cls, (String) itr.next()));
        return names;
    }

    public int getLockTimeout() {
        return _fetch.getLockTimeout();
    }

    public FetchPlan setLockTimeout(int timeout) {
        _fetch.setLockTimeout(timeout);
        return this;
    }

    public int getQueryTimeout() {
        return _fetch.getQueryTimeout();
    }

    public FetchPlan setQueryTimeout(int timeout) {
        _fetch.setQueryTimeout(timeout);
        return this;
    }

    public LockModeType getReadLockMode() {
        return MixedLockLevelsHelper.fromLockLevel(_fetch.getReadLockLevel());
    }

    public FetchPlan setReadLockMode(LockModeType mode) {
        _fetch.setReadLockLevel(MixedLockLevelsHelper.toLockLevel(mode));
        return this;
    }

    public LockModeType getWriteLockMode() {
        return MixedLockLevelsHelper.fromLockLevel(_fetch.getWriteLockLevel());
    }

    public FetchPlan setWriteLockMode(LockModeType mode) {
        _fetch.setWriteLockLevel(MixedLockLevelsHelper.toLockLevel(mode));
        return this;
    }
    
    public boolean getExtendedPathLookup() {
        return _fetch.getExtendedPathLookup();
    }
    
    public FetchPlan setExtendedPathLookup(boolean flag) {
        _fetch.setExtendedPathLookup(flag);
        return this;
    }

    public Object getHint(String key) {
        return _fetch.getHint(key);
    }
    
    public void addHint(String key, Object value) {
        _fetch.addHint(key, value);
    }

    public void setHint(String key, Object value) {
        setHint(key, value, true);
    }

    public void setHint(String key, Object value, boolean validThrowException) {
        if( _hintHandler.setHint(key, value, validThrowException) )
            _fetch.addHint(key, value);
    }

    public void addHints(Map<String, Object> hints) {
        if (hints != null && hints.size() > 0) {
            for (String name : hints.keySet())
                setHint(name, hints.get(name), false);
        }
    }
    
    public Map<String, Object> getHints() {
        return _fetch.getHints();
    }
    
    public int hashCode() {
        return _fetch.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof FetchPlanImpl))
            return false;
        return _fetch.equals(((FetchPlanImpl) other)._fetch);
    }

    public DataCacheRetrieveMode getCacheRetrieveMode() {
        return _fetch.getCacheRetrieveMode();
    }

    public DataCacheStoreMode getCacheStoreMode() {
        return _fetch.getCacheStoreMode();
    }

    public FetchPlan setCacheStoreMode(DataCacheStoreMode mode) {
        _fetch.setCacheStoreMode(mode);
        return this;
    }

    public FetchPlan setCacheRetrieveMode(DataCacheRetrieveMode mode) {
        _fetch.setCacheRetrieveMode(mode);
        return this;
    }
}
