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
package org.apache.openjpa.jdbc.kernel.exps;

import java.sql.SQLException;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.Value;

/**
 * A Value represents any non-operator in a query filter, including
 * constants, variables, and object fields.
 *
 * @author Abe White
 * @nojavadoc
 */
public interface Val
    extends Value {

    /**
     * Initialize the value. This method should recursively initialize any
     * sub-values. It should also cache the {@link Joins} instance
     * containing the joins for this value. No additional joins should be
     * made after this call. The parent expression might modify these joins
     * during its own initialization so that common joins are moved up the
     * expression tree. These joins should not be included in the SQL
     * appended through any of the <code>append</code> methods.
     *
     * @param sel used to create {@link Joins} instances
     * @param store the store manager for the query
     * @param nullTest if true, then this value will be compared
     * to null or tested for emptiness
     */
    public void initialize(Select sel, JDBCStore store, boolean nullTest);

    /**
     * Return the joins for this value. These joins should be created
     * and cached during the {@link #initialize} method. The parent
     * expression might modify these joins during its own initialization so
     * that common joins are moved up the expression tree.
     */
    public Joins getJoins();

    /**
     * Return the datastore value of the given object in the context of this
     * value.
     */
    public Object toDataStoreValue(Object val, JDBCStore store);

    /**
     * Select the data for this value.
     */
    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch);

    /**
     * Select just the columns for this value.
     */
    public void selectColumns(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch);

    /**
     * Group by this value.
     */
    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch);

    /**
     * Order by this value.
     */
    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch);

    /**
     * Load the data for this value.
     */
    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException;

    /**
     * Return true if this value uses the given variable.
     */
    public boolean hasVariable(Variable var);

    /**
     * Calculate and cache the SQL for this value. This method is called
     * before <code>length</code> or any <code>append</code> methods.
     *
     * @param other the value being compared to, or null if not a comparison
     */
    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch);

    /**
     * Clear parameter values held by this value or its subcomponents.
     * This method is called sometime after <code>calculateValue</code>.
     */
    public void clearParameters();

    /**
     * Return the number of SQL elements in this value.
     */
    public int length();

    /**
     * Append the <code>index</code>th SQL element to the given buffer.
     */
    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch);

    /**
     * Append the SQL testing whether this value is empty to the given buffer.
     */
    public void appendIsEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch);

    /**
     * Append the SQL testing whether this value is not empty to
     * the given buffer.
     */
    public void appendIsNotEmpty(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch);

    /**
     * Append the SQL checking the size of this value.
     */
    public void appendSize(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch);

    /**
     * Append the SQL testing whether this value is null to the given buffer.
     */
    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch);

    /**
     * Append the SQL testing whether this value is not null to the given
     * buffer.
     */
    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch);
}
