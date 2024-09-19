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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.kernel.QueryOperations;

/**
 * Interface implemented by OpenJPA queries.
 *
 * @since 0.4.0
 * @author Abe White
 * @published
 */
public interface OpenJPAQuery<X> extends TypedQuery<X> {

    /**
     * Hint key for specifying the number of rows to optimize for.
     */
    String HINT_RESULT_COUNT = QueryHints.HINT_RESULT_COUNT;

    /**
     * The owning entity manager.
     */
    OpenJPAEntityManager getEntityManager();

    /**
     * Query language.
     */
    String getLanguage();

    /**
     * Query operation type.
     */
    QueryOperationType getOperation();

    /**
     * Fetch plan for controlling the loading of results.
     */
    FetchPlan getFetchPlan();

    /**
     * Query string.
     */
    String getQueryString();

    /**
     * Whether to ignore changes in the current transaction.
     */
    boolean getIgnoreChanges();

    /**
     * Whether to ignore changes in the current transaction.
     */
    OpenJPAQuery<X>setIgnoreChanges(boolean ignore);

    /**
     * Return the candidate collection, or <code>null</code> if an
     * extent was specified instead of a collection.
     */
    Collection getCandidateCollection();

    /**
     * Set a collection of candidates.
     */
    OpenJPAQuery<X> setCandidateCollection(Collection coll);

    /**
     * Query result element type.
     */
    Class getResultClass();

    /**
     * Query result element type.
     */
    OpenJPAQuery<X> setResultClass(Class type);

    /**
     * Whether subclasses are included in the query results.
     */
    boolean hasSubclasses();

    /**
     * Whether subclasses are included in the query results.
     */
    OpenJPAQuery<X> setSubclasses(boolean subs);

    /**
     * Return the 0-based start index for the returned results.
     */
    @Override int getFirstResult();

    /**
     * Return the maximum number of results to retrieve.
     * or {@link Integer#MAX_VALUE} for no limit.
     */
    @Override int getMaxResults();

    /**
     * Compile the query.
     */
    OpenJPAQuery<X> compile();

    /**
     * Whether this query has positional parameters.
     */
    boolean hasPositionalParameters();

    /**
     * The positional parameters for the query; empty array if none or
     * if query uses named parameters.
     */
    Object[] getPositionalParameters();

    /**
     * The named parameters for the query; empty map if none or
     * if query uses positional parameters.
     */
    Map<String, Object> getNamedParameters();

    /**
     * Set parameters.
     */
    OpenJPAQuery<X> setParameters(Map params);

    /**
     * Set parameters.
     */
    OpenJPAQuery<X> setParameters(Object... params);

    /**
     * Close all open query results.
     */
    OpenJPAQuery<X>closeAll();

    /**
     * Returns a description of the commands that will be sent to
     * the datastore in order to execute this query. This will
     * typically be in the native query language of the database (e.g., SQL).
     *
     * @param params the named parameter map for the query invocation
     */
    String[] getDataStoreActions(Map params);

    @Override OpenJPAQuery<X> setMaxResults(int maxResult);

    @Override OpenJPAQuery<X> setFirstResult(int startPosition);

    @Override OpenJPAQuery<X> setHint(String hintName, Object value);

    @Override OpenJPAQuery<X> setParameter(String name, Object value);

    @Override OpenJPAQuery<X> setParameter(String name, Date value, TemporalType temporalType);

    @Override OpenJPAQuery<X> setParameter(String name, Calendar value, TemporalType temporalType);

    @Override OpenJPAQuery<X> setParameter(int position, Object value);

    @Override OpenJPAQuery<X> setParameter(int position, Date value, TemporalType temporalType);

    @Override OpenJPAQuery<X> setParameter(int position, Calendar value, TemporalType temporalType);

    /**
     * Sets whether the type of user-supplied bind parameter value and the type of target persistent
     * property they bind to are checked with strong or weak constraint.
     * <br>
     * The same can be set via {@link Query#setHint(String, Object) hint} without puncturing standard
     * JPA API.
     *
     * @see Filters#canConvert(Class, Class, boolean)
     * @see Filters#convert(Object, Class, boolean)
     *
     * @param hint a String or Boolean value.
     */
    void setRelaxBindParameterTypeChecking(Object hint);

    /**
     * Gets whether the type of user-supplied bind parameter value and the type of target persistent
     * property they bind to are checked with strong or weak constraint.
     *
     * @return the booelan state. False by default, i.e. the type of a bind parameter value is checked
     * strongly against the target property type.
     */
    boolean getRelaxBindParameterTypeChecking();


    @Override OpenJPAQuery<X> setFlushMode(FlushModeType flushMode);

    /**
     * Return the current flush mode.
	 */
	@Override FlushModeType getFlushMode ();

    /**
     * @deprecated use the {@link QueryOperationType} instead.
     */
    @Deprecated int OP_SELECT = QueryOperations.OP_SELECT;

    /**
     * @deprecated use the {@link QueryOperationType} instead.
     */
    @Deprecated int OP_DELETE = QueryOperations.OP_DELETE;

    /**
     * @deprecated use the {@link QueryOperationType} instead.
     */
    @Deprecated int OP_UPDATE = QueryOperations.OP_DELETE;

    /**
     * @deprecated use the {@link FlushModeType} enum instead.
     */
    @Deprecated int FLUSH_TRUE = QueryFlushModes.FLUSH_TRUE;

    /**
     * @deprecated use the {@link FlushModeType} enum instead.
     */
    @Deprecated int FLUSH_FALSE = QueryFlushModes.FLUSH_FALSE;

    /**
     * @deprecated use the {@link FlushModeType} enum instead.
     */
    @Deprecated int FLUSH_WITH_CONNECTION =
        QueryFlushModes.FLUSH_WITH_CONNECTION;

    /**
     * @deprecated cast to {@link QueryImpl} instead. This
     * method pierces the published-API boundary, as does the SPI cast.
     */
    @Deprecated OpenJPAQuery<X> addFilterListener(org.apache.openjpa.kernel.exps.FilterListener listener);

    /**
     * @deprecated cast to {@link QueryImpl} instead. This
     * method pierces the published-API boundary, as does the SPI cast.
     */
    @Deprecated OpenJPAQuery<X> removeFilterListener(org.apache.openjpa.kernel.exps.FilterListener listener);

    /**
     * @deprecated cast to {@link QueryImpl} instead. This
     * method pierces the published-API boundary, as does the SPI cast.
     */
    @Deprecated OpenJPAQuery<X> addAggregateListener(org.apache.openjpa.kernel.exps.AggregateListener listener);

    /**
     * @deprecated cast to {@link QueryImpl} instead. This
     * method pierces the published-API boundary, as does the SPI cast.
     */
    @Deprecated OpenJPAQuery<X> removeAggregateListener(org.apache.openjpa.kernel.exps.AggregateListener listener);

    /**
     * Gets hints supported by this query.
     *
     * @since 2.0.0
     */
    Set<String> getSupportedHints();
}
