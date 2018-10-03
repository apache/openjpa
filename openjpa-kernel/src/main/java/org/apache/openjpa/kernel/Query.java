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
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;

/**
 * OpenJPA query interface.
 *
 * @since 0.3.0
 * @author Patrick Linskey
 * @author Abe White
 */
public interface Query
    extends Serializable, QueryContext, QueryOperations, QueryFlushModes {

    /**
     * The broker that generated this query.
     */
    Broker getBroker();

    /**
     * The query string or template.
     */
    boolean setQuery(Object query);

    /**
     * Whether to ignore changes in the current transaction.
     */
    void setIgnoreChanges(boolean ignore);

    /**
     * Register a filter listener for the query.
     */
    void addFilterListener(FilterListener listener);

    /**
     * Remove a filter listener from the query.
     */
    void removeFilterListener(FilterListener listener);

    /**
     * Register an aggregate listener for the query.
     */
    void addAggregateListener(AggregateListener listener);

    /**
     * Remove an aggregate listener from the query.
     */
    void removeAggregateListener(AggregateListener listener);

    /**
     * Return the candidate extent, or <code>null</code> if a
     * collection was specified instead of an extent.
     */
    Extent getCandidateExtent();

    /**
     * Set the candidate extent.
     */
    void setCandidateExtent(Extent extent);

    /**
     * Set a collection of candidates.
     */
    void setCandidateCollection(Collection<?> coll);

    /**
     * Compile the query.
     */
    void compile();

    /**
     * Execute.
     */
    Object execute();

    /**
     * Execute with parameter map.
     */
    Object execute(Map<?,?> params);

    /**
     * Execute with parameter array.
     */
    Object execute(Object[] params);

    /**
     * Deletes all of the instances that are satisfied by the query.
     *
     * @return the number of instances that were deleted
     */
    long deleteAll();

    /**
     * Deletes all of the instances that are satisfied by the query.
     *
     * @param parameters the positional parameters for the query
     * @return the number of instances that were deleted
     */
    long deleteAll(Object[] parameters);

    /**
     * Deletes all of the instances that are satisfied by the query.
     *
     * @param parameterMap the named parameter map
     * @return the number of instances that were deleted
     */
    long deleteAll(Map<?,?> parameterMap);

    /**
     * Performs an update of the instances that are satisfied by the query.
     *
     * @return the number of instances that were update
     */
    long updateAll();

    /**
     * Performs an update of the instances that are satisfied by the query.
     *
     * @param parameters the positional parameter array
     * @return the number of instances that were update
     */
    long updateAll(Object[] parameters);

    /**
     * Performs an update of the instances that are satisfied by the query.
     *
     * @param parameterMap the named parameter map
     * @return the number of instances that were update
     */
    long updateAll(Map<?,?> parameterMap);

    /**
     * Close all open query results.
     */
    void closeAll();

    /**
     * Close query results that are consuming resources. Allow results that
     * are not consuming resources to remain open so that they continue to
     * function normally.
     */
    void closeResources();

    /**
     * Returns a description of the commands that will be sent to
     * the datastore in order to execute this query. This will
     * typically be in the native query language of the database (e.g., SQL).
     *
     * @param params the named parameter map for the query invocation
     * @since 0.3.2
     */
    String[] getDataStoreActions(Map<?,?> params);

    /**
     * Assert that the query's broker is still open.
     */
    void assertOpen();

    /**
     * Assert that the query is not read-only.
     */
    void assertNotReadOnly();

    /**
     * Check that the query has not been serialized, which causes it to lose
     * its association with its Broker.
	 */
	void assertNotSerialized ();
}

