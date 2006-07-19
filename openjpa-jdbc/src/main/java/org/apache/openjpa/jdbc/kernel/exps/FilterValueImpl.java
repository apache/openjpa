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
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Implementation of {@link FilterValue} that wraps a {@link Val}.
 *
 * @author Abe White
 */
class FilterValueImpl
    implements FilterValue {

    private final Val _val;
    private final Select _sel;
    private final JDBCStore _store;
    private final Object[] _params;
    private final JDBCFetchState _fetchState;

    public FilterValueImpl(Val val, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState) {
        _val = val;
        _sel = sel;
        _store = store;
        _params = params;
        _fetchState = fetchState;
    }

    public Class getType() {
        return _val.getType();
    }

    public int length() {
        return _val.length();
    }

    public void appendTo(SQLBuffer buf) {
        appendTo(buf, 0);
    }

    public void appendTo(SQLBuffer buf, int index) {
        _val.appendTo(buf, index, _sel, _store, _params, _fetchState);
    }

    public String getColumnAlias(Column col) {
        return _sel.getColumnAlias(col, _val.getJoins());
    }

    public String getColumnAlias(String col, Table table) {
        return _sel.getColumnAlias(col, table, _val.getJoins());
    }

    public Object toDataStoreValue(Object val) {
        return _val.toDataStoreValue(val, _store);
    }

    public boolean isConstant() {
        return _val instanceof Const;
    }

    public Object getValue() {
        return (isConstant()) ? ((Const) _val).getValue() : null;
    }

    public Object getSQLValue() {
        return (isConstant()) ? ((Const) _val).getSQLValue() : null;
    }

    public boolean isPath() {
        return _val instanceof PCPath;
    }

    public ClassMapping getClassMapping() {
        return (isPath()) ? ((PCPath) _val).getClassMapping() : null;
    }

    public FieldMapping getFieldMapping() {
        return (isPath()) ? ((PCPath) _val).getFieldMapping() : null;
    }
}
