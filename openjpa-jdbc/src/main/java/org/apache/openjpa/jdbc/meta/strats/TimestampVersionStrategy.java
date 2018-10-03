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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;

/**
 * Uses a timestamp for optimistic versioning.
 *
 * @author Abe White
 */
public class TimestampVersionStrategy
    extends ColumnVersionStrategy {

    
    private static final long serialVersionUID = 1L;
    public static final String ALIAS = "timestamp";

    @Override
    public String getAlias() {
        return ALIAS;
    }

    @Override
    protected int getJavaType() {
        return JavaSQLTypes.TIMESTAMP;
    }

    @Override
    protected Object nextVersion(Object version) {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Map getBulkUpdateValues() {
        Column[] cols = vers.getColumns();
        Map map = new HashMap(cols.length);
        Object d = nextVersion(null);
        for (int i = 0; i < cols.length; i++)
            map.put(cols[i], d);
        return map;
    }
}
