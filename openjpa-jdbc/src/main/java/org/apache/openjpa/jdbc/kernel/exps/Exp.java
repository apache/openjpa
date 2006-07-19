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

import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.Expression;

/**
 * An Expression represents a query ready for execution. Generally, it is
 * a set of conditions that must be met for the query to be true.
 *
 * @author Abe White
 */
interface Exp
    extends Expression {

    /**
     * Initialize the expression. This method should recursively
     * initialize any sub-expressions or values. It should also cache
     * the {@link Joins} instance containing the joins for this expression.
     *
     * @param params the parameter values; the initialization process
     * should not rely on exact values, but may need
     * to see if parameter values are null
     * @param contains map of relation paths to the number of times
     * the paths appear in a contains() expression;
     * used to ensure paths used for contains() within
     * the same AND expression used different aliases
     */
    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains);

    /**
     * Append the SQL for this expression to the given buffer. The SQL
     * should optionally include any joins this expression needs.
     */
    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState);

    /**
     * Select just the columns for this value.
     */
    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState);

    /**
     * Return the joins for this expression. These joins should be created
     * and cached during the {@link #initialize} method. The parent
     * expression might modify these joins during its own initialization so
     * that common joins are moved up the expression tree.
     */
    public Joins getJoins();

    /**
     * Return true if this expression is or is made up of a contains expression.
     */
    public boolean hasContainsExpression();

    /**
     * Return true if the expression or any subexpression uses the given
     * variable.
     */
    public boolean hasVariable(Variable var);
}
