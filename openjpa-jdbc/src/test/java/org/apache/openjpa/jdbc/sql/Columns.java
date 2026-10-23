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
package org.apache.openjpa.jdbc.sql;

import org.apache.openjpa.jdbc.schema.Column;

/**
 * Builds the columns the dictionary tests pass to
 * {@link DBDictionary#getTypeName(Column)}.
 */
final class Columns {

    private Columns() {
    }

    /**
     * A column of the given JDBC type and size, declared with the given
     * column definition, or with none when {@code typeName} is null.
     */
    static Column column(int type, String typeName, int size) {
        Column col = new Column();
        col.setType(type);
        if (typeName != null) {
            col.setTypeName(typeName);
        }
        col.setSize(size);
        return col;
    }
}
