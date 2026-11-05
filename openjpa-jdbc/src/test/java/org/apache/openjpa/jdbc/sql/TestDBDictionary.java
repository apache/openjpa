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

import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestDBDictionary {
    @Test
    public void snakeCase() {
        final DBDictionary dictionary = new DBDictionary();
        assertEquals("foo", dictionary.toSnakeCase("foo"));
        assertEquals("foo_bar", dictionary.toSnakeCase("fooBar"));
        assertEquals("fooba_r", dictionary.toSnakeCase("FoobaR"));
        assertEquals("o_f_o_ob", dictionary.toSnakeCase("oFOOb"));
        assertEquals("\"foo_bar\"", dictionary.toSnakeCase("\"fooBar\""));
    }

    /**
     * The name is tried as given, then folded to upper case if the database folds unquoted
     * names to upper case, then in lower case (the former fallback) and finally in upper case.
     */
    @Test
    public void storedProcedureNameCandidates() {
        DBDictionary dict = new DBDictionary();
        dict.schemaCase = DBDictionary.SCHEMA_CASE_UPPER;
        assertEquals(Arrays.asList("GetEmp", "GETEMP", "getemp"), dict.getStoredProcedureNameCandidates("GetEmp"));
        assertEquals(Arrays.asList("GETEMP", "getemp"), dict.getStoredProcedureNameCandidates("GETEMP"));

        dict.schemaCase = DBDictionary.SCHEMA_CASE_LOWER;
        assertEquals(Arrays.asList("GetEmp", "getemp", "GETEMP"), dict.getStoredProcedureNameCandidates("GetEmp"));
        assertEquals(Arrays.asList("getemp", "GETEMP"), dict.getStoredProcedureNameCandidates("getemp"));

        dict.schemaCase = DBDictionary.SCHEMA_CASE_PRESERVE;
        assertEquals(Arrays.asList("GetEmp", "getemp", "GETEMP"), dict.getStoredProcedureNameCandidates("GetEmp"));
    }

    /**
     * Every candidate name is looked up, all metadata result sets are closed and an unknown
     * procedure is not cached, so a later lookup queries the database again.
     */
    @Test
    public void unknownStoredProcedureClosesResultSets() throws SQLException {
        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(new JDBCConfigurationImpl());
        dict.schemaCase = DBDictionary.SCHEMA_CASE_UPPER;
        List<String> lookups = new ArrayList<>();
        AtomicInteger opened = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        DatabaseMetaData meta = (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {DatabaseMetaData.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getProcedureColumns":
                    case "getProcedures":
                        lookups.add(method.getName() + ":" + args[2]);
                        opened.incrementAndGet();
                        return emptyResultSet(closed);
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });

        assertNull(dict.getStoredProcedure(meta, null, null, "GetEmp"));
        assertEquals(Arrays.asList("getProcedureColumns:GetEmp", "getProcedures:GetEmp",
            "getProcedureColumns:GETEMP", "getProcedures:GETEMP",
            "getProcedureColumns:getemp", "getProcedures:getemp"), lookups);
        assertEquals(6, opened.get());
        assertEquals(6, closed.get());

        assertNull(dict.getStoredProcedure(meta, null, null, "GetEmp"));
        assertEquals(12, lookups.size());
        assertEquals(12, closed.get());
    }

    private ResultSet emptyResultSet(AtomicInteger closed) {
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {ResultSet.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "next":
                        return false;
                    case "close":
                        closed.incrementAndGet();
                        return null;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
    }
}
