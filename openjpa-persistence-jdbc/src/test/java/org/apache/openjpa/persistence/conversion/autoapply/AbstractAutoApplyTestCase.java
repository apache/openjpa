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
package org.apache.openjpa.persistence.conversion.autoapply;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Common set up and helpers of the tests of attributes that are converted by
 * an auto-apply converter only.
 */
public abstract class AbstractAutoApplyTestCase extends SingleEMFTestCase {

    /**
     * JDBC types of a column that holds the strings of {@link SkuConverter}.
     */
    protected static final Set<Integer> STRING_TYPES = Set.of(Types.VARCHAR,
        Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CHAR,
        Types.NCHAR, Types.CLOB);

    /**
     * JDBC types of a column that holds the numbers of {@link WeightConverter}.
     */
    protected static final Set<Integer> NUMBER_TYPES = Set.of(Types.BIGINT,
        Types.NUMERIC, Types.DECIMAL, Types.INTEGER);

    /**
     * Set up the persistence unit with the given entities and embeddables,
     * the auto-apply converters and the given extra properties.
     *<p>
     * The entities are left to runtime enhancement: the build-time enhancer
     * of this module sees no persistence unit and so no auto-apply converter,
     * and an enhanced class keeps the persistent fields it was enhanced with.
     *<p>
     * The factory subclasses the unenhanced entities when it creates its first
     * entity manager. Until that has happened once in the JVM, the metamodel
     * of a new factory reports the entities as not enhanced, so an entity
     * manager is opened here to make every test independent of the tests
     * that ran before it.
     */
    protected void setUpUnit(List<Class<?>> types, Object... props) {
        List<Object> args = new ArrayList<>();
        args.add(DROP_TABLES);
        args.addAll(types);
        args.add(SkuConverter.class);
        args.add(WeightConverter.class);
        args.add("openjpa.RuntimeUnenhancedClasses");
        args.add("supported");
        args.addAll(List.of(props));
        setUp(args.toArray());
        emf.createEntityManager().close();
    }

    /**
     * The persistent field of the given name, or null if the attribute is
     * not part of the mapping.
     */
    protected FieldMetaData persistentField(Class<?> type, String name) {
        ClassMetaData meta = getMapping(type);
        FieldMetaData fmd = meta.getField(name);
        if (fmd == null || fmd.getManagement() != FieldMetaData.MANAGE_PERSISTENT) {
            return null;
        }
        return fmd;
    }

    /**
     * The JDBC type of the given column as the database reports it, or null
     * if the table has no such column.
     */
    protected Integer columnType(String table, String column) throws SQLException {
        try (EntityManager em = emf.createEntityManager();
             Connection conn = (Connection) OpenJPAPersistence.cast(em).getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            for (String t : new String[] { table.toUpperCase(Locale.ROOT),
                table.toLowerCase(Locale.ROOT) }) {
                try (ResultSet rs = md.getColumns(null, null, t, null)) {
                    while (rs.next()) {
                        if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                            return rs.getInt("DATA_TYPE");
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Assert that the given column exists and has one of the given JDBC types.
     */
    protected void assertColumnType(Set<Integer> expected, String table, String column)
        throws SQLException {
        Integer type = columnType(table, column);
        assertNotNull(type, table + "." + column + " must exist");
        assertTrue(expected.contains(type), table + "." + column + " has JDBC type " + type);
    }
}
