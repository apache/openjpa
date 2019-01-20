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
import java.time.LocalDate;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Value handler for JDK8 java.time.LocalDate field types.
 *
 */
public class LocalDateValueHandler extends AbstractValueHandler {
    private static final long serialVersionUID = 1L;
    private static final Localizer _loc = Localizer.forPackage(LocalDateValueHandler.class);

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public Column[] map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(name, dict != null ? dict.delimitAll() : false);

        Column column = new Column();
        column.setIdentifier(colName);
        column.setJavaType(JavaSQLTypes.SQL_DATE);
        column.setType(Types.DATE);

        return new Column[]{column};
    }

    @Override
    public boolean isVersionable(ValueMapping vm) {
        return true;
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val, JDBCStore store) {
        if (val == null) {
            return null;
        }

        return java.sql.Date.valueOf((LocalDate) val);
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        if (val == null) {
            return null;
        }

        return ((java.sql.Date) val).toLocalDate();
    }
}
