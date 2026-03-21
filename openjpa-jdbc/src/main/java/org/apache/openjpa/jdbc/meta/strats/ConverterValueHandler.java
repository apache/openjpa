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

        Column col = new Column();
        col.setIdentifier(colName);
        col.setJavaType(JavaTypes.getTypeCode(_dbType));
        return new Column[]{ col };
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
