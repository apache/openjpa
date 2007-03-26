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
package org.apache.openjpa.jdbc.sql;

import java.sql.SQLException;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;

/**
 * Interface for configuring and executing a SQL select.
 *
 * @author Abe White
 */
public interface SelectExecutor {

    /**
     * Return the select configuration.
     */
    public JDBCConfiguration getConfiguration();

    /**
     * Return this select as a SQL statement formatted for the current
     * dictionary.
     */
    public SQLBuffer toSelect(boolean forUpdate, JDBCFetchConfiguration fetch);

    /**
     * Return this select as a COUNT SQL statement formatted for the current
     * dictionary.
     */
    public SQLBuffer toSelectCount();

    /**
     * Whether to automatically make results distinct when relational joins
     * would otherwise introduce duplicates.
     */
    public boolean getAutoDistinct();

    /**
     * Whether to automatically make results distinct when relational joins
     * would otherwise introduce duplicates.
     */
    public void setAutoDistinct(boolean distinct);

    /**
     * Whether this is a SELECT DISTINCT / UNION ALL.
     */
    public boolean isDistinct();

    /**
     * Whether this is a SELECT DISTINCT / UNION ALL.
     */
    public void setDistinct(boolean distinct);

    /**
     * Whether the result of this select should be treated as a large
     * result set.
     */
    public boolean isLRS();

    /**
     * Whether the result of this select should be treated as a large
     * result set.
     */
    public void setLRS(boolean lrs);

    /**
     * The join syntax for this select, as one of the syntax constants from
     * {@link JoinSyntaxes}.
     */
    public int getJoinSyntax();

    /**
     * The join syntax for this select, as one of the syntax constants from
     * {@link JoinSyntaxes}.
     */
    public void setJoinSyntax(int joinSyntax);

    /**
     * Return whether this select can support a random access result set type.
     */
    public boolean supportsRandomAccess(boolean forUpdate);

    /**
     * Whether this select can be executed for update.
     */
    public boolean supportsLocking();

    /**
     * Return the number of instances matching this select.
     */
    public int getCount(JDBCStore store)
        throws SQLException;

    /**
     * Execute this select in the context of the given store manager.
     */
    public Result execute(JDBCStore store, JDBCFetchConfiguration fetch)
        throws SQLException;

    /**
     * Execute this select in the context of the given store manager.
     */
    public Result execute(JDBCStore store, JDBCFetchConfiguration fetch,
        int lockLevel)
        throws SQLException;
    
    /**
     * Return the expected result count for the query
     */
    public int getExpectedResultCount() ;

    /**
     * Set the expected result count for the query
     * force indicates whether the count is internally generated
     * or given by the user as optimize hint
     */
    
    public void setExpectedResultCount(int expectedResultCount,boolean force) ;
    
    /**
     * Indicates whether the expectedResultCount is internally generated
     */
     
     public boolean isExpRsltCntForced();
}
