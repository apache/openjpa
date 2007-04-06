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
package org.apache.openjpa.persistence.jdbc;

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
    extends FetchPlan, EagerFetchModes, LRSSizes, JoinSyntaxes {

    /**
     * Eager fetch mode in loading relations.
     * 
     * @see EagerFetchModes
     */
    public int getEagerFetchMode();

    /**
     * Eager fetch mode in loading relations.
     * 
     * @see EagerFetchModes
     */
    public JDBCFetchPlan setEagerFetchMode(int mode);

    /**
     * Eager fetch mode in loading subclasses.
     * 
     * @see EagerFetchModes
     */
    public int getSubclassFetchMode();

    /**
     * Eager fetch mode in loading subclasses.
     * 
     * @see EagerFetchModes
     */
    public JDBCFetchPlan setSubclassFetchMode(int mode);

    /**
     * Type of JDBC result set to use for query results.
     * 
     * @see java.sql.ResultSet
     */
    public int getResultSetType();

    /**
     * Type of JDBC result set to use for query results.
     * 
     * @see java.sql.ResultSet
     */
    public JDBCFetchPlan setResultSetType(int type);

    /**
     * Result set fetch direction.
     * 
     * @see java.sql.ResultSet
     */
    public int getFetchDirection();

    /**
     * Result set fetch direction.
     * 
     * @see java.sql.ResultSet
     */
    public JDBCFetchPlan setFetchDirection(int direction);

    /**
     * How to determine the size of a large result set.
     * 
     * @see LRSSizes
     */
    public int getLRSSize();

    /**
     * How to determine the size of a large result set.
     * 
     * @see LRSSizes
     */
    public JDBCFetchPlan setLRSSize(int lrsSize);

    /**
     * SQL join syntax.
     *
     * @see JoinSyntaxes
     */
    public int getJoinSyntax();

    /**
     * SQL join syntax.
     *
     * @see JoinSyntaxes
     */
    public JDBCFetchPlan setJoinSyntax(int syntax);

    /**
     * The isolation level for queries issued to the database. This overrides
     * the persistence-unit-wide <code>openjpa.jdbc.TransactionIsolation</code>
     * value.
     *
     * @since 0.9.7
     */
    public IsolationLevel getIsolation();

    /**
     * The isolation level for queries issued to the database. This overrides
     * the persistence-unit-wide <code>openjpa.jdbc.TransactionIsolation</code>
     * value.
     *
     * @since 0.9.7
     */
    public JDBCFetchPlan setIsolation(IsolationLevel level);
}
