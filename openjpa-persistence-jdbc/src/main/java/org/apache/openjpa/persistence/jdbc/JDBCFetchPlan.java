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
package org.apache.openjpa.persistence.jdbc;

import java.util.Collection;

import jakarta.persistence.LockModeType;

import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.LRSSizes;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;
import org.apache.openjpa.persistence.FetchPlan;

/**
 * JDBC extensions to the fetch plan.
 *
 * @since 0.4.1
 * @author Abe White
 * @author Pinaki Poddar
 * @published
 */
public interface JDBCFetchPlan
    extends FetchPlan {

    /**
     * Eager fetch mode in loading relations.
     */
    FetchMode getEagerFetchMode();

    /**
     * Eager fetch mode in loading relations.
     */
    JDBCFetchPlan setEagerFetchMode(FetchMode mode);

    /**
     * Eager fetch mode in loading subclasses.
     */
    FetchMode getSubclassFetchMode();

    /**
     * Eager fetch mode in loading subclasses.
     */
    JDBCFetchPlan setSubclassFetchMode(FetchMode mode);

    /**
     * Type of JDBC result set to use for query results.
     */
    ResultSetType getResultSetType();

    /**
     * Type of JDBC result set to use for query results.
     */
    JDBCFetchPlan setResultSetType(ResultSetType type);

    /**
     * Result set fetch direction.
     */
    FetchDirection getFetchDirection();

    /**
     * Result set fetch direction.
     */
    JDBCFetchPlan setFetchDirection(FetchDirection direction);

    /**
     * How to determine the size of a large result set.
     */
    LRSSizeAlgorithm getLRSSizeAlgorithm();

    /**
     * How to determine the size of a large result set.
     */
    JDBCFetchPlan setLRSSizeAlgorithm(LRSSizeAlgorithm lrsSizeAlgorithm);

    /**
     * SQL join syntax.
     */
    JoinSyntax getJoinSyntax();

    /**
     * SQL join syntax.
     */
    JDBCFetchPlan setJoinSyntax(JoinSyntax syntax);

    /**
     * The isolation level for queries issued to the database. This overrides
     * the persistence-unit-wide <code>openjpa.jdbc.TransactionIsolation</code>
     * value.
     *
     * @since 0.9.7
     */
    IsolationLevel getIsolation();

    /**
     * The isolation level for queries issued to the database. This overrides
     * the persistence-unit-wide <code>openjpa.jdbc.TransactionIsolation</code>
     * value.
     *
     * @since 0.9.7
     */
    JDBCFetchPlan setIsolation(IsolationLevel level);


    // covariant type support for return vals

    @Override JDBCFetchPlan addFetchGroup(String group);
    @Override JDBCFetchPlan addFetchGroups(Collection groups);
    @Override JDBCFetchPlan addFetchGroups(String... groups);
    @Override JDBCFetchPlan addField(Class cls, String field);
    @Override JDBCFetchPlan addField(String field);
    @Override JDBCFetchPlan addFields(Class cls, Collection fields);
    @Override JDBCFetchPlan addFields(Class cls, String... fields);
    @Override JDBCFetchPlan addFields(Collection fields);
    @Override JDBCFetchPlan addFields(String... fields);
    @Override JDBCFetchPlan clearFetchGroups();
    @Override JDBCFetchPlan clearFields();
    @Override JDBCFetchPlan removeFetchGroup(String group);
    @Override JDBCFetchPlan removeFetchGroups(Collection groups);
    @Override JDBCFetchPlan removeFetchGroups(String... groups);
    @Override JDBCFetchPlan removeField(Class cls, String field);
    @Override JDBCFetchPlan removeField(String field);
    @Override JDBCFetchPlan removeFields(Class cls, Collection fields);
    @Override JDBCFetchPlan removeFields(Class cls, String... fields);
    @Override JDBCFetchPlan removeFields(String... fields);
    @Override JDBCFetchPlan removeFields(Collection fields);
    @Override JDBCFetchPlan resetFetchGroups();
    @Override JDBCFetchPlan setQueryResultCacheEnabled(boolean cache);
    @Override JDBCFetchPlan setFetchBatchSize(int fetchBatchSize);
    @Override JDBCFetchPlan setLockTimeout(int timeout);
    @Override JDBCFetchPlan setMaxFetchDepth(int depth);
    @Override JDBCFetchPlan setReadLockMode(LockModeType mode);
    @Override JDBCFetchPlan setWriteLockMode(LockModeType mode);
    @Override JDBCFetchPlan setQueryTimeout(int timeout);

    /**
     * @deprecated use the {@link FetchMode} enum instead.
     */
    @Deprecated int EAGER_NONE = EagerFetchModes.EAGER_NONE;

    /**
     * @deprecated use the {@link FetchMode} enum instead.
     */
    @Deprecated int EAGER_JOIN = EagerFetchModes.EAGER_JOIN;

    /**
     * @deprecated use the {@link FetchMode} enum instead.
     */
    @Deprecated int EAGER_PARALLEL = EagerFetchModes.EAGER_PARALLEL;

    /**
     * @deprecated use the {@link LRSSizeAlgorithm} enum instead.
     */
    @Deprecated int SIZE_UNKNOWN = LRSSizes.SIZE_UNKNOWN;

    /**
     * @deprecated use the {@link LRSSizeAlgorithm} enum instead.
     */
    @Deprecated int SIZE_LAST = LRSSizes.SIZE_LAST;

    /**
     * @deprecated use the {@link LRSSizeAlgorithm} enum instead.
     */
    @Deprecated int SIZE_QUERY = LRSSizes.SIZE_QUERY;

    /**
     * @deprecated use the {@link JoinSyntax} enum instead.
     */
    @Deprecated int SYNTAX_SQL92 = JoinSyntaxes.SYNTAX_SQL92;

    /**
     * @deprecated use the {@link JoinSyntax} enum instead.
     */
    @Deprecated int SYNTAX_TRADITIONAL =
        JoinSyntaxes.SYNTAX_TRADITIONAL;

    /**
     * @deprecated use the {@link JoinSyntax} enum instead.
     */
    @Deprecated int SYNTAX_DATABASE = JoinSyntaxes.SYNTAX_DATABASE;

    /**
     * @deprecated use {@link #setEagerFetchMode(FetchMode)} instead.
     */
    @Deprecated JDBCFetchPlan setEagerFetchMode(int mode);

    /**
     * @deprecated use {@link #setSubclassFetchMode(FetchMode)} instead.
     */
    @Deprecated JDBCFetchPlan setSubclassFetchMode(int mode);

    /**
     * @deprecated use {@link #setResultSetType(ResultSetType)} instead.
     */
    @Deprecated JDBCFetchPlan setResultSetType(int mode);

    /**
     * @deprecated use {@link #setFetchDirection(FetchDirection)} instead.
     */
    @Deprecated JDBCFetchPlan setFetchDirection(int direction);

    /**
     * @deprecated use {@link #getLRSSizeAlgorithm()} instead.
     */
    @Deprecated int getLRSSize();

    /**
     * @deprecated use {@link #setLRSSizeAlgorithm(LRSSizeAlgorithm)} instead.
     */
    @Deprecated JDBCFetchPlan setLRSSize(int lrsSizeMode);

    /**
     * @deprecated use {@link #setJoinSyntax(JoinSyntax)} instead.
     */
    @Deprecated JDBCFetchPlan setJoinSyntax(int syntax);

    /**
     * Affirms if foreign key for a relation field will be pre-fetched as part of the owning object irrespective of
     * whether the field is included in the default fetch group of this fetch configuration. <br><br>
     * By default, foreign key for a relation field is pre-fetched as part of the owning object <em>only</em> if the
     * field in included in the default fetch group of this fetch configuration.
     *
     * @since 2.2.0
     */
    boolean getIgnoreDfgForFkSelect();

    /**
     * Affirms if foreign key for a relation field will be pre-fetched as part of the owning object irrespective of
     * whether the field is included in the default fetch group of this fetch configuration. <br><br>
     * By default, foreign key for a relation field is pre-fetched as part of the owning object <em>only</em> if the
     * field in included in the default fetch group of this fetch configuration.
     *
     * @since 2.2.0
     */
    void setIgnoreDfgForFkSelect(boolean b);
}
