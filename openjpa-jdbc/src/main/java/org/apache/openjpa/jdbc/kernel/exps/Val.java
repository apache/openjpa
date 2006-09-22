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
     * Initialization flag indicating that this value will be compared to null.
     */
    public final int NULL_CMP = 2 << 0;

    /**
     * Initialization flag indicating to join into any relation path.
     */
    public final int JOIN_REL = 2 << 1; 

    /**
     * Initialization flag indicating to force an outer join into any relation 
     * path.
     */
    public final int FORCE_OUTER = 2 << 2; 

    /**
     * Initialize the value. This method should recursively initialize any
     * sub-values. 
     */
    public ExpState initialize(Select sel, ExpContext ctx, int flags);

    /**
     * Return the datastore value of the given object in the context of this
     * value.
     */
    public Object toDataStoreValue(Select sel, ExpContext ctx, ExpState state, 
        Object val);

    /**
     * Select the data for this value.
     */
    public void select(Select sel, ExpContext ctx, ExpState state, boolean pks);

    /**
     * Select just the columns for this value.
     */
    public void selectColumns(Select sel, ExpContext ctx, ExpState state, 
        boolean pks);

    /**
     * Group by this value.
     */
    public void groupBy(Select sel, ExpContext ctx, ExpState state);

    /**
     * Order by this value.
     */
    public void orderBy(Select sel, ExpContext ctx, ExpState state, 
        boolean asc);

    /**
     * Load the data for this value.
     */
    public Object load(ExpContext ctx, ExpState state, Result res)
        throws SQLException;

    /**
     * Calculate and cache the SQL for this value. This method is called
     * before <code>length</code> or any <code>append</code> methods.
     *
     * @param other the value being compared to, or null if not a comparison
     */
    public void calculateValue(Select sel, ExpContext ctx, ExpState state, 
        Val other, ExpState otherState);

    /**
     * Return the number of SQL elements in this value.
     */
    public int length(Select sel, ExpContext ctx, ExpState state);

    /**
     * Append the <code>index</code>th SQL element to the given buffer.
     */
    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql, 
        int index);

    /**
     * Append the SQL testing whether this value is empty to the given buffer.
     */
    public void appendIsEmpty(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql);

    /**
     * Append the SQL testing whether this value is not empty to
     * the given buffer.
     */
    public void appendIsNotEmpty(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql);

    /**
     * Append the SQL checking the size of this value.
     */
    public void appendSize(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql);

    /**
     * Append the SQL testing whether this value is null to the given buffer.
     */
    public void appendIsNull(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql);

    /**
     * Append the SQL testing whether this value is not null to the given
     * buffer.
     */
    public void appendIsNotNull(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql);
}
