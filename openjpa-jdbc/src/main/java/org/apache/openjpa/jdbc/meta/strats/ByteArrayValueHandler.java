/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.jdbc.meta.strats;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;

/**
 * Handler for byte array values.
 *
 */
public class ByteArrayValueHandler
    extends AbstractValueHandler {


    private static final long serialVersionUID = 1L;
    private static final ByteArrayValueHandler _instance =
        new ByteArrayValueHandler();

    /**
     * Singleton instance.
     */
    public static ByteArrayValueHandler getInstance() {
        return _instance;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public Column[] map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt) {
        DBIdentifier colName = DBIdentifier.newColumn(name, false);
        return map(vm, colName, io, adapt);
    }

    public Column[] map(ValueMapping vm, DBIdentifier name, ColumnIO io,
        boolean adapt) {
        Column col = new Column();
        col.setIdentifier(name);
        col.setJavaType(JavaSQLTypes.BYTES);
        col.setSize(-1);
        return new Column[]{ col };
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val,
        JDBCStore store) {
        return PrimitiveWrapperArrays.toByteArray(val);
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        return PrimitiveWrapperArrays.toObjectValue(vm, (byte[]) val);
    }
}
