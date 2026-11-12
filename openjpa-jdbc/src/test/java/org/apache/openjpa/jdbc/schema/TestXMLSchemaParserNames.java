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
package org.apache.openjpa.jdbc.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.StringReader;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier.DBIdentifierType;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.junit.jupiter.api.Test;

/**
 * The names of a schema file are named for the dictionary of the
 * configuration: on MySQL and MariaDB a name that is a reserved word is
 * delimited, so that it equals the name that is read back from the
 * database, on other databases it is left alone (OPENJPA-3076).
 */
class TestXMLSchemaParserNames {

    private static final String SCHEMA = """
        <schemas>
          <schema name="Current">
            <sequence name="Order"/>
            <table name="Window">
              <column name="Rank" type="integer"/>
              <column name="Rows" type="integer"/>
              <pk name="Primary" column="Rank"/>
              <index name="Over" column="Rows"/>
              <unique name="Key" column="Rows"/>
            </table>
            <table name="Item">
              <column name="Id" type="integer"/>
              <column name="Lag" type="integer"/>
              <pk column="Id"/>
              <fk name="Lead" to-table="Current.Window" column="Lag"/>
            </table>
          </schema>
        </schemas>
        """;

    @Test
    void reservedWordsAreDelimitedOnMySQL() throws IOException {
        for (String dictionary : new String[] {"mysql", "mariadb"}) {
            JDBCConfiguration conf = configuration(dictionary);
            DBDictionary dict = conf.getDBDictionaryInstance();
            SchemaGroup group = parse(conf);

            Schema schema = group.getSchema(dict.fromDBName("Current", DBIdentifierType.SCHEMA));
            assertNotNull(schema, dictionary);
            assertEquals("\"Current\"", schema.getIdentifier().getName(), dictionary);

            Sequence seq = schema.getSequence(dict.fromDBName("Order", DBIdentifierType.SEQUENCE));
            assertNotNull(seq, dictionary);
            assertEquals("\"Order\"", seq.getIdentifier().getName(), dictionary);

            Table window = schema.getTable(dict.fromDBName("Window", DBIdentifierType.TABLE));
            assertNotNull(window, dictionary);
            assertEquals("\"Rank\"", window.getColumn("Rank").getIdentifier().getName(), dictionary);
            assertEquals("\"Primary\"", window.getPrimaryKey().getIdentifier().getName(), dictionary);
            Index over = window.getIndex(dict.fromDBName("Over", DBIdentifierType.INDEX));
            assertNotNull(over, dictionary);
            assertEquals("\"Over\"", over.getIdentifier().getName(), dictionary);
            assertEquals("\"Key\"", window.getUniques()[0].getIdentifier().getName(), dictionary);

            Table item = schema.getTable(dict.fromDBName("Item", DBIdentifierType.TABLE));
            ForeignKey lead = item.getForeignKeys()[0];
            assertEquals("\"Lead\"", lead.getIdentifier().getName(), dictionary);
            assertSame(window, lead.getPrimaryKeyTable(), dictionary);
        }
    }

    @Test
    void reservedWordsAreLeftAloneOnOtherDatabases() throws IOException {
        JDBCConfiguration conf = configuration("derby");
        SchemaGroup group = parse(conf);
        Schema schema = group.getSchema("Current");
        assertEquals("Current", schema.getIdentifier().getName());
        assertEquals("Order", schema.getSequences()[0].getIdentifier().getName());
        Table window = schema.getTable("Window");
        assertEquals("Window", window.getIdentifier().getName());
        assertEquals("Rank", window.getColumn("Rank").getIdentifier().getName());
        assertEquals("Primary", window.getPrimaryKey().getIdentifier().getName());
        assertEquals("Over", window.getIndexes()[0].getIdentifier().getName());
    }

    private static JDBCConfiguration configuration(String dictionary) {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setDBDictionary(dictionary);
        return conf;
    }

    private static SchemaGroup parse(JDBCConfiguration conf) throws IOException {
        XMLSchemaParser parser = new XMLSchemaParser(conf);
        parser.parse(new StringReader(SCHEMA), "names.schema");
        return parser.getSchemaGroup();
    }
}
