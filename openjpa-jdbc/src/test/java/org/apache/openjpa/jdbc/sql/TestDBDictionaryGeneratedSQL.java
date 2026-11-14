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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.DBIdentifierUtilImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.util.UserException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestDBDictionaryGeneratedSQL {

    @RegisterExtension
    public Mockery context = new JUnit5Mockery();

    @Test
    void createTableLongNameException() {
        final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
        final DBIdentifierUtilImpl idImpl = new DBIdentifierUtilImpl();

        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getIdentifierUtilInstance();
                will(returnValue(idImpl));

                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(mockConfiguration);
        dict.maxTableNameLength = 10;

        Table table = new Table();
        table.setIdentifier(DBIdentifier.newTable("NameIsTooLong"));

        try {
            dict.getCreateTableSQL(table);
            fail("Expected a UserException");
        } catch (UserException ue) {
            // expected - check message in case a different UserException is thrown.
            assertTrue(ue.getMessage().contains("Table name \"NameIsTooLong\""));
        }
    }


    @Test
    void throwsExceptionWithSchemaSet() {
        final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
        final DBIdentifierUtilImpl idImpl = new DBIdentifierUtilImpl();

        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getIdentifierUtilInstance();
                will(returnValue(idImpl));

                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(mockConfiguration);
        dict.maxTableNameLength = 10;

        Table table = new Table();
        table.setIdentifier(DBIdentifier.newTable("NameIsTooLong"));
        table.setSchemaIdentifier(DBIdentifier.newSchema("IAmASchema"));

        try {
            dict.getCreateTableSQL(table);
            fail("Expected a UserException");
        } catch (UserException ue) {
            // expected - check message in case a different UserException is thrown.
            assertTrue(ue.getMessage().contains("Table name \"IAmASchema.NameIsTooLong\""));
        }
    }

    @Test
    void schemaNameIsNotConsidered() {
        final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
        final DBIdentifierUtilImpl idImpl = new DBIdentifierUtilImpl();

        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getIdentifierUtilInstance();
                will(returnValue(idImpl));

                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(mockConfiguration);
        dict.maxTableNameLength = 12;

        Table table = new Table();
        table.setIdentifier(DBIdentifier.newTable("NameIsRight"));
        table.setSchemaIdentifier(DBIdentifier.newSchema("IAmASchema"));

        String[] sqls = dict.getCreateTableSQL(table);
        assertEquals(1, sqls.length);
        assertTrue(sqls[0].contains("NameIsRight"));
        assertTrue(sqls[0].contains("IAmASchema"));
    }

    @Test
    void overrideProperty() {
        final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
        final DBIdentifierUtilImpl idImpl = new DBIdentifierUtilImpl();

        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getIdentifierUtilInstance();
                will(returnValue(idImpl));

                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(mockConfiguration);
        dict.tableLengthIncludesSchema=true;
        dict.maxTableNameLength = 12;

        Table table = new Table();
        table.setIdentifier(DBIdentifier.newTable("NameIsTooLong"));
        table.setSchemaIdentifier(DBIdentifier.newSchema("IAmASchema"));

        try {
            dict.getCreateTableSQL(table);
            fail("Expected a UserException");
        } catch (UserException ue) {
            // expected - check message in case a different UserException is thrown.
            assertTrue(ue.getMessage().contains("Table name \"IAmASchema.NameIsTooLong\""));
        }
    }

    @Test
    void overridePropertyShortName() {
        final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
        final DBIdentifierUtilImpl idImpl = new DBIdentifierUtilImpl();

        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getIdentifierUtilInstance();
                will(returnValue(idImpl));

                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(mockConfiguration);
        dict.tableLengthIncludesSchema=true;
        dict.maxTableNameLength = 18;

        Table table = new Table();
        table.setIdentifier(DBIdentifier.newTable("NameIsRight"));
        table.setSchemaIdentifier(DBIdentifier.newSchema("schema"));

        String[] sqls = dict.getCreateTableSQL(table);
        assertEquals(1, sqls.length);
        assertTrue(sqls[0].contains("NameIsRight"));
        assertTrue(sqls[0].contains("schema"));
    }

    @Test
    void dropColumnWithDelimiters() {
        final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
        final DBIdentifierUtilImpl idImpl = new DBIdentifierUtilImpl();

        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getIdentifierUtilInstance();
                will(returnValue(idImpl));

                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new DBDictionary();
        dict.setDelimitIdentifiers(true);
        dict.setSupportsDelimitedIdentifiers(true);
        dict.setLeadingDelimiter("`");
        dict.setTrailingDelimiter("`");
        dict.setConfiguration(mockConfiguration);
        Table table = new Table();
        table.setIdentifier(dict.fromDBName("NameIsRight", DBIdentifier.DBIdentifierType.TABLE));

        Column column = new Column(dict.fromDBName("MyColumn", DBIdentifier.DBIdentifierType.COLUMN), table);

        String[] sqls = dict.getDropColumnSQL(column);
        assertEquals(1, sqls.length);
        assertEquals("ALTER TABLE `NameIsRight` DROP COLUMN `MyColumn`", sqls[0]);
    }
}
