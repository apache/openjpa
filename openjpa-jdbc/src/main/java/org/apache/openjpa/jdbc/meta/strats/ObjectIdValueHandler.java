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
package org.apache.openjpa.jdbc.meta.strats;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.kernel.ObjectIdStateManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.util.InternalException;

/**
 * Handler for embedded object id fields.
 *
 * @author Abe White
 * @nojavadoc
 * @since 0.4.0
 */
public class ObjectIdValueHandler
    extends EmbedValueHandler {

    private Object[] _args = null;

    public Column[] map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt) {
        List cols = new ArrayList();
        List args = new ArrayList();
        super.map(vm, name, io, adapt, cols, args);

        vm.setColumns((Column[]) cols.toArray(new Column[cols.size()]));
        _args = args.toArray();
        return vm.getColumns();
    }

    public Object getResultArgument(ValueMapping vm) {
        return _args;
    }

    public Object toDataStoreValue(ValueMapping vm, Object val,
        JDBCStore store) {
        OpenJPAStateManager sm = (val == null) ? null
            : new ObjectIdStateManager(val, null, vm);
        Column[] cols = vm.getColumns();
        Object rval = null;
        if (cols.length > 1)
            rval = new Object[cols.length];
        return super.toDataStoreValue(sm, vm, store, cols, rval, 0);
    }

    public Object toObjectValue(ValueMapping vm, Object val) {
        if (val == null)
            return null;

        OpenJPAStateManager sm = new ObjectIdStateManager(null, null, vm);
        try {
            super.toObjectValue(sm, vm, val, null, null, vm.getColumns(), 0);
        } catch (SQLException se) {
            // shouldn't be possible
            throw new InternalException(se);
        }
        return sm.getManagedInstance();
    }
}
