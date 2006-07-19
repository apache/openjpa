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

import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.Parameter;
import org.apache.openjpa.util.ImplHelper;

/**
 * A parameter in a filter.
 *
 * @author Abe White
 */
class Param
    extends Const
    implements Parameter {

    private final String _name;
    private Class _type = null;
    private int _idx = -1;
    private boolean _container = false;
    private Object _val = null;
    private Object _sqlVal = null;
    private int _otherLen = 0;

    /**
     * Constructor. Supply parameter name and type.
     */
    public Param(String name, Class type) {
        _name = name;
        setImplicitType(type);
    }

    public String getName() {
        return _name;
    }

    public String getParameterName() {
        return getName();
    }

    public Class getType() {
        return _type;
    }

    public void setImplicitType(Class type) {
        _type = type;
        _container = (getMetaData() == null ||
            !ImplHelper.isManagedType(type))
            && (Collection.class.isAssignableFrom(type)
            || Map.class.isAssignableFrom(type));
    }

    public int getIndex() {
        return _idx;
    }

    public void setIndex(int idx) {
        _idx = idx;
    }

    public Object getValue() {
        return _val;
    }

    public Object getSQLValue() {
        return _sqlVal;
    }

    public Object getValue(Object[] params) {
        return params[_idx];
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchState fetchState) {
        super.calculateValue(sel, store, params, other, fetchState);
        _val = Filters.convert(params[_idx], getType());
        if (other != null && !_container) {
            _sqlVal = other.toDataStoreValue(_val, store);
            _otherLen = other.length();
        } else
            _sqlVal = _val;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchState fetchState) {
        if (_otherLen > 1)
            sql.appendValue(((Object[]) _sqlVal)[index], getColumn(index));
        else
            sql.appendValue(_sqlVal, getColumn(index));
    }

    public void clearParameters() {
        _val = null;
        _sqlVal = null;
    }
}
