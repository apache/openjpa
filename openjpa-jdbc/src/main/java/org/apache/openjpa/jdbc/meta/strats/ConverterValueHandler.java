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

import java.sql.Types;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.JavaTypes;

/**
 * ValueHandler for fields using a JPA {@code AttributeConverter}.
 *
 * <p>This handler is responsible for mapping the correct database column type
 * based on the converter's database type (the Y in
 * {@code AttributeConverter<X,Y>}). The actual value conversion between
 * entity attribute type and database column type is handled by the
 * externalization path in {@code FieldMetaData.getExternalValue()} and
 * {@code FieldMetaData.getFieldValue()}.
 *
 * @since 4.2.0
 */
public class ConverterValueHandler extends AbstractValueHandler {

    private static final long serialVersionUID = 1L;

    private final Class<?> _dbType;

    public ConverterValueHandler(Class<?> dbType) {
        _dbType = dbType;
    }

    @Override
    public Column[] map(ValueMapping vm, String name, ColumnIO io,
            boolean adapt) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(name,
            dict != null ? dict.delimitAll() : false);

        adjustLobColumns(vm, _dbType);
        Column col = new Column();
        col.setIdentifier(colName);
        col.setJavaType(getColumnJavaType(_dbType));
        return new Column[]{ col };
    }

    /**
     * The column java type for the converter's database type. A byte array
     * is read and written as bytes, not as an object.
     */
    static int getColumnJavaType(Class<?> dbType) {
        if (dbType == byte[].class)
            return JavaSQLTypes.BYTES;
        return JavaTypes.getTypeCode(dbType);
    }

    /**
     * A {@code @Lob} or {@code <lob/>} column gets its CLOB or BLOB type from
     * the declared type of the attribute, because the mapping parsers run
     * before the converter is known. With a converter the stored value has
     * the converter's database type, so choose the kind of LOB from that.
     * Columns that are not LOBs, and LOBs whose database type is neither
     * String nor byte[], are left alone.
     */
    static void adjustLobColumns(ValueMapping vm, Class<?> dbType) {
        int lobType;
        if (dbType == String.class)
            lobType = Types.CLOB;
        else if (dbType == byte[].class)
            lobType = Types.BLOB;
        else
            return;

        for (Column col : vm.getValueInfo().getColumns()) {
            if (col.getType() == Types.BLOB || col.getType() == Types.CLOB)
                col.setType(lobType);
        }
    }

    @Override
    public boolean isVersionable(ValueMapping vm) {
        return true;
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        // No conversion here - handled by FieldMetaData.getFieldValue()
        // through the externalization path (sm.store -> getFieldValue)
        return val;
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val,
            org.apache.openjpa.jdbc.kernel.JDBCStore store) {
        // No conversion here - handled by FieldMetaData.getExternalValue()
        // through the externalization path (sm.fetch -> getExternalValue)
        return val;
    }
}
