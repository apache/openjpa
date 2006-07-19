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

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * A variable in a filter. Typically, the {@link #initialize} and
 * {@link #getJoins} methods of this value are not called. They are
 * only called if the variable is bound but otherwise unused in the filter,
 * in which case we must at least make the joins to the variable because the
 * act of binding a variable should at least guarantee that an instance
 * represting the variable could exist (i.e. the binding collection is not
 * empty).
 *
 * @author Abe White
 */
class Variable
    implements Val {

    private final String _name;
    private final Class _type;
    private ClassMetaData _meta;
    private PCPath _path = null;
    private Class _cast = null;

    /**
     * Constructor. Supply variable name and type.
     */
    public Variable(String name, Class type) {
        _name = name;
        _type = type;
    }

    /**
     * Return the variable name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Return true if the variable is bound.
     */
    public boolean isBound() {
        return _path != null;
    }

    /**
     * Return the path this variable is aliased to.
     */
    public PCPath getPCPath() {
        return _path;
    }

    /**
     * Set the path this variable is aliased to.
     */
    public void setPCPath(PCPath path) {
        _path = path;
    }

    public ClassMetaData getMetaData() {
        return _meta;
    }

    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    public boolean isVariable() {
        return true;
    }

    public Class getType() {
        if (_cast != null)
            return _cast;
        return _type;
    }

    public void setImplicitType(Class type) {
        _cast = type;
        if (_path != null)
            _path.setImplicitType(type);
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        if (_path != null) {
            _path.addVariableAction(this);
            _path.initialize(sel, store, nullTest);
            _path.joinRelation();
        }
    }

    public Joins getJoins() {
        return (_path == null) ? null : _path.getJoins();
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchState fetchState) {
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchState fetchState) {
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchState fetchState) {
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchState fetchState)
        throws SQLException {
        return null;
    }

    public boolean hasVariable(Variable var) {
        return this == var;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchState fetchState) {
        if (_path != null)
            _path.calculateValue(sel, store, params, other, fetchState);
    }

    public void clearParameters() {
        if (_path != null)
            _path.clearParameters();
    }

    public int length() {
        return 0;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
    }

    public void appendSize(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
    }

    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
    }
}
