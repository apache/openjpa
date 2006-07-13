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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.openjpa.kernel.QueryFlushModes;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;

/**
 * Interface implemented by OpenJPA queries.
 *
 * @since 4.0
 * @author Abe White
 * @published
 */
public interface OpenJPAQuery
    extends Query, QueryOperations, QueryFlushModes {

    /**
     * The owning entity manage.
     */
    public OpenJPAEntityManager getEntityManager();

    /**
     * Query language.
     */
    public String getLanguage();

    /**
     * Query operation type.
     */
    public int getOperation();

    /**
     * Fetch plan for controlling the loading of results.
     */
    public FetchPlan getFetchPlan();

    /**
     * Query string.
     */
    public String getQueryString();

    /**
     * Whether to ignore changes in the current transaction.
     */
    public boolean getIgnoreChanges();

    /**
     * Whether to ignore changes in the current transaction.
     */
    public OpenJPAQuery setIgnoreChanges(boolean ignore);

    /**
     * Register a filter listener for the query.
     */
    public OpenJPAQuery addFilterListener(FilterListener listener);

    /**
     * Remove a filter listener from the query.
     */
    public OpenJPAQuery removeFilterListener(FilterListener listener);

    /**
     * Register an aggregate listener for the query.
     */
    public OpenJPAQuery addAggregateListener(AggregateListener listener);

    /**
     * Remove an aggregate listener from the query.
     */
    public OpenJPAQuery removeAggregateListener(AggregateListener listener);

    /**
     * Return the candidate collection, or <code>null</code> if an
     * extent was specified instead of a collection.
     */
    public Collection getCandidateCollection();

    /**
     * Set a collection of candidates.
     */
    public OpenJPAQuery setCandidateCollection(Collection coll);

    /**
     * Query result element type.
     */
    public Class getResultClass();

    /**
     * Query result element type.
     */
    public OpenJPAQuery setResultClass(Class type);

    /**
     * Whether subclasses are included in the query results.
     */
    public boolean hasSubclasses();

    /**
     * Whether subclasses are included in the query results.
     */
    public OpenJPAQuery setSubclasses(boolean subs);

    /**
     * Return the 0-based start index for the returned results.
     */
    public int getFirstResult();

    /**
     * Return the maximum number of results to retrieve.
     * or {@link Integer#MAX_VALUE} for no limit.
     */
    public int getMaxResults();

    /**
     * Compile the query.
     */
    public OpenJPAQuery compile();

    /**
     * Whether this query has positional parameters.
     */
    public boolean hasPositionalParameters();

    /**
     * The positional parameters for the query; empty array if none or
     * if query uses named parameters.
     */
    public Object[] getPositionalParameters();

    /**
     * The named parameters for the query; empty map if none or
     * if query uses named parameters.
     */
    public Map getNamedParameters();

    /**
     * Set parameters.
     */
    public OpenJPAQuery setParameters(Map params);

    /**
     * Set parameters.
     */
    public OpenJPAQuery setParameters(Object... params);

    /**
     * Close all open query results.
     */
    public OpenJPAQuery closeAll();

    /**
     * Returns a description of the commands that will be sent to
     * the datastore in order to execute this query. This will
     * typically be in the native query language of the database (e.g., SQL).
     *
     * @param params the named parameter map for the query invocation
     */
    public String[] getDataStoreActions(Map params);

    public OpenJPAQuery setMaxResults(int maxResult);

    public OpenJPAQuery setFirstResult(int startPosition);

    public OpenJPAQuery setHint(String hintName, Object value);

    public OpenJPAQuery setParameter(String name, Object value);

    public OpenJPAQuery setParameter(String name, Date value,
        TemporalType temporalType);

    public OpenJPAQuery setParameter(String name, Calendar value,
        TemporalType temporalType);

    public OpenJPAQuery setParameter(int position, Object value);

    public OpenJPAQuery setParameter(int position, Date value,
        TemporalType temporalType);

    public OpenJPAQuery setParameter(int position, Calendar value,
        TemporalType temporalType);

    public OpenJPAQuery setFlushMode(FlushModeType flushMode);

    /**
     * Return the current flush mode.
	 */
	public FlushModeType getFlushMode ();
}
