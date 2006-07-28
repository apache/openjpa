/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.persistence.LockModeType;

import org.apache.openjpa.kernel.DelegatingFetchConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.meta.FetchGroup;

/**
 * The fetch plan allows you to dynamically alter eager fetching
 * configuration and other aspects of data loading.
 *
 * @author Abe White
 * @since 4.0
 * @published
 */
public class FetchPlan {

    /**
     * Fetch group representing all fields.
     */
    public static final String GROUP_ALL = FetchGroup.NAME_ALL;

    /**
     * The default fetch group.
     */
    public static final String GROUP_DEFAULT = FetchGroup.NAME_DEFAULT;

    /**
     * Infinite fetch depth.
     */
    public static final int DEPTH_INFINITE = FetchGroup.DEPTH_INFINITE;

    /**
     * Constant to revert any setting to its default value.
     */
    public static final int DEFAULT = FetchConfiguration.DEFAULT;

    private final DelegatingFetchConfiguration _fetch;

    /**
     * Constructor; supply delegate.
     */
    public FetchPlan(FetchConfiguration fetch) {
        _fetch = newDelegatingFetchConfiguration(fetch);
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

    /**
     * The maximum fetch depth when loading an object.
     */
    public int getMaxFetchDepth(int depth) {
        return _fetch.getMaxFetchDepth();
    }

    /**
     * The maximum fetch depth when loading an object.
     */
    public FetchPlan setMaxFetchDepth(int depth) {
        _fetch.setMaxFetchDepth(depth);
        return this;
    }

    /**
     * Return the fetch batch size for large result set support.
     * Defaults to the	<code>openjpa.FetchBatchSize</code> setting. Note
     * that this property will be ignored under some data stores.
     */
    public int getFetchBatchSize() {
        return _fetch.getFetchBatchSize();
    }

    /**
     * Set the fetch batch size for large result set support.
     * Defaults to the	<code>openjpa.FetchBatchSize</code> setting. Note
     * that this property will be ignored under some data stores.
     */
    public FetchPlan setFetchBatchSize(int fetchBatchSize) {
        _fetch.setFetchBatchSize(fetchBatchSize);
        return this;
    }

    /**
     * Return whether or not query caching is enabled. If this returns
     * <code>true</code> but the datacache plugin is not installed, caching
     * will not be enabled. If this
     * returns <code>false</code>, query caching will not be used
     * even if the datacache plugin is installed.
     */
    public boolean getQueryResultCache() {
        return _fetch.getQueryCache();
    }

    /**
     * Control whether or not query caching is enabled. This has no effect
     * if the datacache plugin is not installed, or if the query cache size
     * is set to zero.
     */
    public FetchPlan setQueryResultCache(boolean cache) {
        _fetch.setQueryCache(cache);
        return this;
    }

    /**
     * Returns the names of the fetch groups that this component will use
     * when loading objects. Defaults to the
     * <code>org.apache.openjpa.FetchGroups</code> setting.
     */
    public Collection<String> getFetchGroups() {
        return _fetch.getFetchGroups();
    }

    /**
     * Adds <code>group</code> to the set of fetch group to
     * use when loading objects.
     */
    public FetchPlan addFetchGroup(String group) {
        _fetch.addFetchGroup(group);
        return this;
    }

    /**
     * Adds <code>groups</code> to the set of fetch group names to
     * use when loading objects.
     */
    public FetchPlan addFetchGroups(String... groups) {
        return addFetchGroups(Arrays.asList(groups));
    }

    /**
     * Adds <code>groups</code> to the set of fetch group names to
     * use when loading objects.
     */
    public FetchPlan addFetchGroups(Collection groups) {
        _fetch.addFetchGroups(groups);
        return this;
    }

    /**
     * Remove the given fetch group.
     */
    public FetchPlan removeFetchGroup(String group) {
        _fetch.removeFetchGroup(group);
        return this;
    }

    /**
     * Removes <code>groups</code> from the set of fetch group names
     * to use when loading objects.
     */
    public FetchPlan removeFetchGroups(String... groups) {
        return removeFetchGroups(Arrays.asList(groups));
    }

    /**
     * Removes <code>groups</code> from the set of fetch group names
     * to use when loading objects.
     */
    public FetchPlan removeFetchGroups(Collection groups) {
        _fetch.removeFetchGroups(groups);
        return this;
    }

    /**
     * Clears the set of fetch group names to use wen loading
     * data. After this operation is invoked, only those fields in
     * the default fetch group (and any requested field) will be
     * loaded when loading an object.
     */
    public FetchPlan clearFetchGroups() {
        _fetch.clearFetchGroups();
        return this;
    }

    /**
     * Resets the set of fetch groups to the list in the global configuration.
     */
    public FetchPlan resetFetchGroups() {
        _fetch.resetFetchGroups();
        return this;
    }

    /**
     * Returns the fully qualified names of the fields that this component
     * will use when loading objects. Defaults to the empty set.
     */
    public Collection<String> getFields() {
        return (Collection<String>) _fetch.getFields();
    }

    /**
     * Return true if the given field has been added.
     */
    public boolean hasField(String field) {
        return _fetch.hasField(field);
    }

    /**
     * Return true if the given field has been added.
     */
    public boolean hasField(Class cls, String field) {
        return hasField(toFieldName(cls, field));
    }

    /**
     * Adds <code>field</code> to the set of fully-qualified field names to
     * use when loading objects.
     */
    public FetchPlan addField(String field) {
        _fetch.addField(field);
        return this;
    }

    /**
     * Adds <code>field</code> to the set of field names to
     * use when loading objects.
     */
    public FetchPlan addField(Class cls, String field) {
        return addField(toFieldName(cls, field));
    }

    /**
     * Adds <code>fields</code> to the set of fully-qualified field names to
     * use when loading objects.
     */
    public FetchPlan addFields(String... fields) {
        return addFields(Arrays.asList(fields));
    }

    /**
     * Adds <code>fields</code> to the set of field names to
     * use when loading objects.
     */
    public FetchPlan addFields(Class cls, String... fields) {
        return addFields(cls, Arrays.asList(fields));
    }

    /**
     * Adds <code>fields</code> to the set of fully-qualified field names to
     * use when loading objects.
     */
    public FetchPlan addFields(Collection fields) {
        _fetch.addFields(fields);
        return this;
    }

    /**
     * Adds <code>fields</code> to the set of field names to
     * use when loading objects.
     */
    public FetchPlan addFields(Class cls, Collection fields) {
        return addFields(toFieldNames(cls, fields));
    }

    /**
     * Remove the given fully-qualified field.
     */
    public FetchPlan removeField(String field) {
        _fetch.removeField(field);
        return this;
    }

    /**
     * Remove the given field.
     */
    public FetchPlan removeField(Class cls, String field) {
        return removeField(toFieldName(cls, field));
    }

    /**
     * Removes <code>fields</code> from the set of fully-qualified field names
     * to use when loading objects.
     */
    public FetchPlan removeFields(String... fields) {
        return removeFields(Arrays.asList(fields));
    }

    /**
     * Removes <code>fields</code> from the set of field names
     * to use when loading objects.
     */
    public FetchPlan removeFields(Class cls, String... fields) {
        return removeFields(cls, Arrays.asList(fields));
    }

    /**
     * Removes <code>fields</code> from the set of fully-qualified field names
     * to use when loading objects.
     */
    public FetchPlan removeFields(Collection fields) {
        _fetch.removeFields(fields);
        return this;
    }

    /**
     * Removes <code>fields</code> from the set of field names
     * to use when loading objects.
     */
    public FetchPlan removeFields(Class cls, Collection fields) {
        return removeFields(toFieldNames(cls, fields));
    }

    /**
     * Clears the set of field names to use wen loading
     * data. After this operation is invoked, only those fields in
     * the configured fetch groups will be loaded when loading an object.
     */
    public FetchPlan clearFields() {
        _fetch.clearFields();
        return this;
    }

    /**
     * Combine the class and field to a fully-qualified field name.
     */
    private static String toFieldName(Class cls, String field) {
        return cls.getName() + "." + field;
    }

    /**
     * Combine the class and fields to fully-qualified field names.
     */
    private static Collection toFieldNames(Class cls, Collection fields) {
        if (fields.isEmpty())
            return fields;
        Collection names = new ArrayList(fields);
        for (Iterator itr = fields.iterator(); itr.hasNext();)
            names.add(toFieldName(cls, (String) itr.next()));
        return names;
    }

    /**
     * The number of milliseconds to wait for an object lock, or -1 for no
     * limit.
     */
    public int getLockTimeout() {
        return _fetch.getLockTimeout();
    }

    /**
     * The number of milliseconds to wait for an object lock, or -1 for no
     * limit.
     */
    public FetchPlan setLockTimeout(int timeout) {
        _fetch.setLockTimeout(timeout);
        return this;
    }

    /**
     * The lock level to use for locking loaded objects.
     */
    public LockModeType getReadLockMode() {
        return EntityManagerImpl.fromLockLevel(_fetch.getReadLockLevel());
    }

    /**
     * The lock level to use for locking loaded objects.
     */
    public FetchPlan setReadLockMode(LockModeType mode) {
        _fetch.setReadLockLevel(EntityManagerImpl.toLockLevel(mode));
        return this;
    }

    /**
     * The lock level to use for locking dirtied objects.
     */
    public LockModeType getWriteLockMode() {
        return EntityManagerImpl.fromLockLevel(_fetch.getWriteLockLevel());
    }

    /**
     * The lock level to use for locking dirtied objects.
     */
    public FetchPlan setWriteLockMode(LockModeType mode) {
        _fetch.setWriteLockLevel(EntityManagerImpl.toLockLevel(mode));
        return this;
    }

    public int hashCode() {
        return _fetch.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof FetchPlan))
            return false;
        return _fetch.equals(((FetchPlan) other)._fetch);
    }
}
