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
package org.apache.openjpa.jdbc.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.Normalizer;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.identifier.IdentifierRule;
import org.junit.After;
import org.junit.Test;

/**
 * The default name of an index over several columns is built from the column
 * names without their delimiters (OPENJPA-3079).
 */
public class TestDefaultIndexName {

    /**
     * Puts back the delimiting of reserved words that a configured MySQL
     * dictionary sets for the whole JVM.
     */
    @After
    public void tearDown() {
        IdentifierRule rule = Normalizer.getNamingConfiguration().getDefaultIdentifierRule();
        rule.setDelimitReservedWords(false);
        rule.setReservedWords(Set.of());
    }

    @Test
    public void testDerby() {
        DBDictionary dict = configure(new DerbyDictionary());
        assertIndex(dict, "I_SCORES_RANK_POINTS", "CREATE INDEX I_SCORES_RANK_POINTS ON SCORES (",
            "\"rank\"", "POINTS");
        assertIndex(dict, "I_SCORES_RANK_WINDOW", "CREATE INDEX I_SCORES_RANK_WINDOW ON SCORES (",
            "\"rank\"", "\"window\"");
        assertIndex(dict, "I_SCORES_RANK_POINTS", "CREATE INDEX I_SCORES_RANK_POINTS ON SCORES (",
            "RANK", "POINTS");
    }

    @Test
    public void testMySQL() {
        DBDictionary dict = configure(new MySQLDictionary());
        assertIndex(dict, "I_SCORES_RANK_POINTS", "CREATE INDEX I_SCORES_RANK_POINTS ON SCORES (",
            "\"rank\"", "POINTS");
        assertIndex(dict, "I_SCORES_RANK_WINDOW", "CREATE INDEX I_SCORES_RANK_WINDOW ON SCORES (",
            "\"rank\"", "\"window\"");
        assertIndex(dict, "I_SCORES_RANK_POINTS", "CREATE INDEX I_SCORES_RANK_POINTS ON SCORES (",
            "RANK", "POINTS");
    }

    @Test
    public void testPostgres() {
        DBDictionary dict = configure(new PostgresDictionary());
        assertIndex(dict, "I_SCORES_RANK_POINTS", "CREATE INDEX I_SCORES_RANK_POINTS ON SCORES (",
            "\"rank\"", "POINTS");
        assertIndex(dict, "I_SCORES_RANK_WINDOW", "CREATE INDEX I_SCORES_RANK_WINDOW ON SCORES (",
            "\"rank\"", "\"window\"");
        assertIndex(dict, "I_SCORES_RANK_POINTS", "CREATE INDEX I_SCORES_RANK_POINTS ON SCORES (",
            "RANK", "POINTS");
    }

    /**
     * A name that needs delimiters as a whole is delimited once.
     */
    @Test
    public void testNameDelimitedAsWhole() {
        DBDictionary dict = configure(new DerbyDictionary());
        assertIndex(dict, "\"I_SCORES_my rank_POINTS\"",
            "CREATE INDEX \"I_SCORES_my rank_POINTS\" ON SCORES (", "\"my rank\"", "POINTS");
    }

    private static DBDictionary configure(DBDictionary dict) {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        Configurations.configureInstance(dict, conf, (String) null, "DBDictionary");
        return dict;
    }

    private static void assertIndex(DBDictionary dict, String name, String sqlPrefix, String... colNames) {
        Schema schema = new Schema(DBIdentifier.NULL, new SchemaGroup());
        Table table = new Table(DBIdentifier.newTable("SCORES"), schema);
        Column[] cols = new Column[colNames.length];
        for (int i = 0; i < colNames.length; i++) {
            cols[i] = table.addColumn(DBIdentifier.newColumn(colNames[i]));
        }
        DBIdentifier idxName = dict.getValidIndexName(MappingInfo.getDefaultIndexName(cols), table);
        assertEquals(name, idxName.getName());

        Index idx = table.addIndex(idxName);
        for (Column col : cols) {
            idx.addColumn(col);
        }
        String sql = dict.getCreateIndexSQL(idx)[0];
        assertTrue(sql, sql.startsWith(sqlPrefix));
    }
}
