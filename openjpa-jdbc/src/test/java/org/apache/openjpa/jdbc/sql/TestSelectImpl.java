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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
import static org.apache.openjpa.jdbc.sql.Select.FROM_SELECT_ALIAS;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests about SelectImpl.
 */
public class TestSelectImpl {

    @Test
    public void testSelectResultGetColumnAlias() {
        verifySelectResultGetColumnAlias(true, false, false, -1, "Col", 56, "t56.\"Col\"");
        verifySelectResultGetColumnAlias(false, false, false, -1, "Col", 18, "t18.Col");

        // tests with additional select in from
        verifySelectResultGetColumnAlias(false, true /* fromSelect */, false, -1, "Col", 18, null);
        verifySelectResultGetColumnAlias(false, true, false, 92, "Col", 18, "t92_Col");
        verifySelectResultGetColumnAlias(false, true, true /* requiresAliasForSubselect */, 92, "Col", 18, FROM_SELECT_ALIAS + ".t92_Col");

        verifySelectResultGetColumnAlias(true, true, false, -1, "Col", 18, null);
        verifySelectResultGetColumnAlias(true, true, false, 92, "Col", 18, "t92_Col");
        verifySelectResultGetColumnAlias(true, true, true /* requiresAliasForSubselect */, 92, "Col", 18, FROM_SELECT_ALIAS + ".t92_Col");
    }

    private void verifySelectResultGetColumnAlias(boolean delimitIdentifiers, boolean fromSelect, boolean requiresAliasForSubselect,
            int fromSelectTableIndex, String colName, int tableIndex, String expected) {
        DBDictionary dict = new DBDictionary();
        dict.setDelimitIdentifiers(delimitIdentifiers);
        dict.requiresAliasForSubselect = requiresAliasForSubselect;
        dict.setSupportsDelimitedIdentifiers(delimitIdentifiers);
        dict.configureNamingRules();
        DBIdentifier columnName = DBIdentifier.newColumn(colName, false);

        JDBCConfiguration conf = new JDBCConfigurationImpl();
        dict.setConfiguration(conf);
        conf.setDBDictionary(dict);
        SelectImpl selectImpl = new SelectImpl(conf) {
            @Override
            int getTableIndex(Table table, PathJoins pj, boolean create) {
                return tableIndex;
            }
        };
        Connection conn = null;
        Statement stmnt = null;
        ResultSet rs = null;
        SelectImpl.SelectResult result = new SelectImpl.SelectResult(conn, stmnt, rs, dict);
        result.setSelect(selectImpl);
        PathJoins pj = null;
        Column column = new Column(columnName, null);
        if (fromSelect) {
            SelectImpl fromSelectImpl = new SelectImpl(conf) {
                @Override
                int getTableIndex(Table table, PathJoins pj, boolean create) {
                    return fromSelectTableIndex;
                }
            };
            selectImpl.setFromSelect(fromSelectImpl);
        } else {
            assertEquals(-1, fromSelectTableIndex);
        }
        String res = result.getColumnAlias(column, pj);
        assertEquals(expected, res);
    }
}
