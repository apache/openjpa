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

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Obtaining the object id of a constant.
 *
 * @author Abe White
 */
class ConstGetObjectId
    extends Const {

    private final Const _constant;
    private Object _val = null;
    private Object _sqlVal = null;
    private int _otherLen = 0;

    /**
     * Constructor. Supply constant to traverse.
     */
    public ConstGetObjectId(Const constant) {
        _constant = constant;
    }

    public Class getType() {
        return Object.class;
    }

    public void setImplicitType(Class type) {
    }

    public Object getValue() {
        return _val;
    }

    public Object getSQLValue() {
        return _sqlVal;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchState fetchState) {
        super.calculateValue(sel, store, params, other, fetchState);
        _constant.calculateValue(sel, store, params, null, fetchState);
        _val = store.getContext().getObjectId(_constant.getValue());
        if (other != null) {
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
        _constant.clearParameters();
        _val = null;
        _sqlVal = null;
    }
}
