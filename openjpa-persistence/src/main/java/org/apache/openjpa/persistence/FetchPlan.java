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

import java.util.Collection;
import java.util.Map;

import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;

import org.apache.openjpa.kernel.DataCacheRetrieveMode;
import org.apache.openjpa.kernel.DataCacheStoreMode;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.lib.util.Reflectable;
import org.apache.openjpa.meta.FetchGroup;

/**
 * The fetch plan allows you to dynamically alter eager fetching
 * configuration and other aspects of data loading.
 *
 * @author Abe White
 * @author Pinaki Poddar
 * @since 0.4.1
 * @published
 */
public interface FetchPlan {

    /**
     * Fetch group representing all fields.
     */
    String GROUP_ALL = FetchGroup.NAME_ALL;

    /**
     * The default fetch group.
     */
    String GROUP_DEFAULT = FetchGroup.NAME_DEFAULT;

    /**
     * Infinite fetch depth.
     */
    int DEPTH_INFINITE = FetchGroup.DEPTH_INFINITE;

    /**
     * Constant to revert any setting to its default value.
     */
    int DEFAULT = FetchConfiguration.DEFAULT;

    /**
     * The maximum fetch depth when loading an object.
     */
    int getMaxFetchDepth();

    /**
     * The maximum fetch depth when loading an object.
     */
    FetchPlan setMaxFetchDepth(int depth);

    /**
     * Return the fetch batch size for large result set support.
     * Defaults to the	<code>openjpa.FetchBatchSize</code> setting. Note
     * that this property will be ignored under some data stores.
     */
    int getFetchBatchSize();

    /**
     * Set the fetch batch size for large result set support.
     * Defaults to the	<code>openjpa.FetchBatchSize</code> setting. Note
     * that this property will be ignored under some data stores.
     */
    FetchPlan setFetchBatchSize(int fetchBatchSize);

    /**
     * Return whether or not query caching is enabled. If this returns
     * <code>true</code> but the datacache plugin is not installed, caching
     * will not be enabled. If this
     * returns <code>false</code>, query caching will not be used
     * even if the datacache plugin is installed.
     *
     * @since 1.0.0
     */
    boolean getQueryResultCacheEnabled();

    /**
     * Control whether or not query caching is enabled. This has no effect
     * if the datacache plugin is not installed, or if the query cache size
     * is set to zero.
     *
     * @since 1.0.0
     */
    FetchPlan setQueryResultCacheEnabled(boolean cache);

    /**
     * @deprecated use {@link #getQueryResultCacheEnabled()} instead.
     */
    @Deprecated boolean getQueryResultCache();

    /**
     * @deprecated use {@link #setQueryResultCacheEnabled} instead.
     */
    @Deprecated FetchPlan setQueryResultCache(boolean cache);


    /**
     * Returns the names of the fetch groups that this component will use
     * when loading objects. Defaults to the
     * <code>openjpa.FetchGroups</code> setting.
     */
    Collection<String> getFetchGroups();

    /**
     * Adds <code>group</code> to the set of fetch group to
     * use when loading objects.
     */
    FetchPlan addFetchGroup(String group);

    /**
     * Adds <code>groups</code> to the set of fetch group names to
     * use when loading objects.
     */
    FetchPlan addFetchGroups(String... groups);

    /**
     * Adds <code>groups</code> to the set of fetch group names to
     * use when loading objects.
     */
    FetchPlan addFetchGroups(Collection groups);

    /**
     * Remove the given fetch group.
     */
    FetchPlan removeFetchGroup(String group);

    /**
     * Removes <code>groups</code> from the set of fetch group names
     * to use when loading objects.
     */
    FetchPlan removeFetchGroups(String... groups);

    /**
     * Removes <code>groups</code> from the set of fetch group names
     * to use when loading objects.
     */
    FetchPlan removeFetchGroups(Collection groups);

    /**
     * Clears the set of fetch group names to use wen loading
     * data. After this operation is invoked, only those fields in
     * the default fetch group (and any requested field) will be
     * loaded when loading an object.
     */
    FetchPlan clearFetchGroups();

    /**
     * Resets the set of fetch groups to the list in the global configuration.
     */
    FetchPlan resetFetchGroups();

    /**
     * Returns the fully qualified names of the fields that this component
     * will use when loading objects. Defaults to the empty set.
     */
    Collection<String> getFields();

    /**
     * Return true if the given field has been added.
     */
    boolean hasField(String field);

    /**
     * Return true if the given field has been added.
     */
    boolean hasField(Class cls, String field);

    /**
     * Adds <code>field</code> to the set of fully-qualified field names to
     * use when loading objects.
     */
    FetchPlan addField(String field);

    /**
     * Adds <code>field</code> to the set of field names to
     * use when loading objects.
     */
    FetchPlan addField(Class cls, String field);

    /**
     * Adds <code>fields</code> to the set of fully-qualified field names to
     * use when loading objects.
     */
    FetchPlan addFields(String... fields);

    /**
     * Adds <code>fields</code> to the set of field names to
     * use when loading objects.
     */
    FetchPlan addFields(Class cls, String... fields);

    /**
     * Adds <code>fields</code> to the set of fully-qualified field names to
     * use when loading objects.
     */
    FetchPlan addFields(Collection fields);

    /**
     * Adds <code>fields</code> to the set of field names to
     * use when loading objects.
     */
    FetchPlan addFields(Class cls, Collection fields);

    /**
     * Remove the given fully-qualified field.
     */
    FetchPlan removeField(String field);

    /**
     * Remove the given field.
     */
    FetchPlan removeField(Class cls, String field);

    /**
     * Removes <code>fields</code> from the set of fully-qualified field names
     * to use when loading objects.
     */
    FetchPlan removeFields(String... fields);

    /**
     * Removes <code>fields</code> from the set of field names
     * to use when loading objects.
     */
    FetchPlan removeFields(Class cls, String... fields);

    /**
     * Removes <code>fields</code> from the set of fully-qualified field names
     * to use when loading objects.
     */
    FetchPlan removeFields(Collection fields);

    /**
     * Removes <code>fields</code> from the set of field names
     * to use when loading objects.
     */
    FetchPlan removeFields(Class cls, Collection fields);

    /**
     * Clears the set of field names to use wen loading
     * data. After this operation is invoked, only those fields in
     * the configured fetch groups will be loaded when loading an object.
     */
    FetchPlan clearFields();

    /**
     * The number of milliseconds to wait for an object lock, or -1 for no
     * limit.
     */
    int getLockTimeout();

    /**
     * The number of milliseconds to wait for an object lock, or -1 for no
     * limit.
     */
    FetchPlan setLockTimeout(int timeout);

    /**
     * The lock scope to use for locking loaded objects.
     */
    PessimisticLockScope getLockScope();

    /**
     * The lock scope to use for locking loaded objects.
     */
    FetchPlan setLockScope(PessimisticLockScope scope);

    /**
     * The number of milliseconds to wait for a query, or -1 for no
     * limit.
     */
    int getQueryTimeout();

    /**
     * The number of milliseconds to wait for a query, or -1 for no
     * limit.
     */
    FetchPlan setQueryTimeout(int timeout);

    /**
     * The lock level to use for locking loaded objects.
     */
    LockModeType getReadLockMode();

    /**
     * The lock level to use for locking loaded objects.
     */
    FetchPlan setReadLockMode(LockModeType mode);

    /**
     * The lock level to use for locking dirtied objects.
     */
    LockModeType getWriteLockMode();

    /**
     * The lock level to use for locking dirtied objects.
     */
    FetchPlan setWriteLockMode(LockModeType mode);

    /**
     * @deprecated cast to {@link FetchPlanImpl} instead. This
     * method pierces the published-API boundary, as does the SPI cast.
     */
    @Deprecated
    @Reflectable(false) org.apache.openjpa.kernel.FetchConfiguration getDelegate();

    /**
     * Affirms if extended path lookup feature is active.
     *
     * @since 2.0.0
     */
    boolean getExtendedPathLookup();

    /**
     * Sets extended path lookup feature.
     *
     * @since 2.0.0
     */
    FetchPlan setExtendedPathLookup(boolean flag);

    /**
     * Gets the current storage mode for data cache.
     *
     * @since 2.0.0
     */
    DataCacheStoreMode getCacheStoreMode();

    /**
     * Sets the current storage mode for data cache.
     *
     * @since 2.0.0
     */
    FetchPlan setCacheStoreMode(DataCacheStoreMode mode);

    /**
     * Gets the current retrieve mode for data cache.
     *
     * @since 2.0.0
     */
    DataCacheRetrieveMode getCacheRetrieveMode();

    /**
     * Sets the current retrieve mode for data cache.
     *
     * @since 2.0.0
     */
    FetchPlan setCacheRetrieveMode(DataCacheRetrieveMode mode);

    /**
     * Set the hint for the given key to the given value.
     *
     * @param value the value of the hint.
     * @param name the name of the hint.
     *
     * @since 2.0.0
     */
    void setHint(String key, Object value);

    /**
     * Get the hints and their values currently set on this receiver.
     *
     * @return empty map if no hint has been set.
     */
    Map<String, Object> getHints();

    /**
     * Get the hint value for the given key.
     *
     * @return null if the key has not been set.
     */
    Object getHint(String key);
}
