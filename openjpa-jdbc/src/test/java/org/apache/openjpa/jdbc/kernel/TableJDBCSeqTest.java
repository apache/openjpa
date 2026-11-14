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
package org.apache.openjpa.jdbc.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.junit5.JUnit5Mockery;

/**
 * Unit tests for TableJDBCSeq and subclasses.
 */
@Disabled // column delimitAll needs a complete overhault.
public class TableJDBCSeqTest {

    @RegisterExtension
    public Mockery context = new JUnit5Mockery();

    final Connection mockConnection = context.mock(Connection.class);
    final DatabaseMetaData mockMetaData = context.mock(DatabaseMetaData.class);

    @Test
    void tableJDBCSeq() throws Exception {
        testAddPrimaryKeyColumnWithDelimitAll(false, () -> new TableJDBCSeq());
    }

    @Test
    void tableJDBCSeqDelimitAll() throws Exception {
        testAddPrimaryKeyColumnWithDelimitAll(true, () -> new TableJDBCSeq());
    }

    @Test
    void classTableJDBCSeq() throws Exception {
        testAddPrimaryKeyColumnWithDelimitAll(false, () -> new ClassTableJDBCSeq());
    }

    @Test
    void classTableJDBCSeqDelimitAll() throws Exception {
        testAddPrimaryKeyColumnWithDelimitAll(true, () -> new ClassTableJDBCSeq());
    }

    @Test
    void valueTableJDBCSeq() throws Exception {
        testAddPrimaryKeyColumnWithDelimitAll(false, () -> new ValueTableJDBCSeq());
    }

    @Test
    void valueTableJDBCSeqDelimitAll() throws Exception {
        testAddPrimaryKeyColumnWithDelimitAll(true, () -> new ValueTableJDBCSeq());
    }

    /**
     * Testing that addPrimaryKeyColumn returns a column identifier respecting dist#delimitAll.
     */
    private void testAddPrimaryKeyColumnWithDelimitAll(boolean delimitAll,
                                                       Supplier<? extends TableJDBCSeq> builder) throws Exception {
        JDBCConfiguration configurationImpl = new JDBCConfigurationImpl(false, false);

        context.checking(new Expectations() {
            {
                // No activity on the connection other than getting the metadata.
                allowing(mockConnection).getMetaData();
                will(returnValue(mockMetaData));

                allowing(mockMetaData).getDatabaseProductName();
                will(returnValue("MockDB"));

                allowing(mockMetaData).getDriverName();
                will(returnValue("MockDB"));

                allowing(mockMetaData).getDriverVersion();
                will(returnValue("1.0"));

                allowing(mockMetaData).getDatabaseProductVersion();
                will(returnValue("10"));

                allowing(mockMetaData).getDatabaseMajorVersion();
                will(returnValue(10));

                allowing(mockMetaData).getDatabaseMinorVersion();
                will(returnValue(0));

                allowing(mockMetaData).getJDBCMajorVersion();
                will(returnValue(4));

                allowing(mockMetaData).getJDBCMinorVersion();
                will(returnValue(0));

                allowing(mockMetaData).supportsMixedCaseIdentifiers();
                will(returnValue(true));

                allowing(mockMetaData).supportsMixedCaseQuotedIdentifiers();
                will(returnValue(true));

                allowing(mockMetaData).storesLowerCaseQuotedIdentifiers();
                will(returnValue(false));

                allowing(mockMetaData).storesUpperCaseQuotedIdentifiers();
                will(returnValue(false));

                allowing(mockMetaData).storesMixedCaseQuotedIdentifiers();
                will(returnValue(false));

                allowing(mockMetaData).supportsGetGeneratedKeys();
                will(returnValue(false));

            }
        });

        Table table = new Table(DBIdentifier.newTable("mytable"), new Schema(DBIdentifier.newSchema("myschema"), new SchemaGroup()));
        TableJDBCSeq instance = new ClassTableJDBCSeq();
        DBDictionary dict = new DBDictionary();
        dict.setConfiguration(configurationImpl);
        dict.setDelimitIdentifiers(delimitAll);

        dict.connectedConfiguration(mockConnection);

        configurationImpl.setDBDictionary(dict);
        instance.setConfiguration(configurationImpl);
        Column result = instance.addPrimaryKeyColumn(table);

        if (dict.delimitAll()) {
            final DBIdentifier identifier = result.getIdentifier();
            assertEquals("\"ID\"", identifier.getName());
        } else {
            assertEquals("ID", result.getIdentifier().getName());
        }
        assertEquals(dict.delimitAll(), result.getIdentifier().isDelimited());
    }

}
