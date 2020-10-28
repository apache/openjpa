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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.DBIdentifierUtil;
import org.apache.openjpa.jdbc.identifier.DBIdentifierUtilImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.kernel.StoreContext;
import org.hsqldb.types.Types;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestHerdDBDictionary {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    final Statement mockStatement = context.mock(Statement.class);
    final Connection mockConnection = context.mock(Connection.class);
    final ResultSet mockRS = context.mock(ResultSet.class);
    final DataSource mockDS = context.mock(DataSource.class);
    final DatabaseMetaData mockMetaData = context.mock(DatabaseMetaData.class);

    final StoreContext sc = null;

    @Test
    public void testBootDBDictionary() throws Exception {
        // Expected method calls on the mock objects above. If any of these are
        // do not occur, or if any other methods are invoked on the mock objects
        // an exception will be thrown and the test will fail.
        context.checking(new Expectations()
        {
            {

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
                will(returnValue(true));

                allowing(mockMetaData).storesUpperCaseQuotedIdentifiers();
                will(returnValue(false));

                allowing(mockMetaData).storesMixedCaseQuotedIdentifiers();
                will(returnValue(false));

                allowing(mockMetaData).supportsGetGeneratedKeys();
                will(returnValue(false));

            }
        });
        JDBCConfiguration jdbcConfiguration = new JDBCConfigurationImpl(false);

        DBDictionary dict = new HerdDBDictionary();
        dict.setConfiguration(jdbcConfiguration);
        assertNull(dict.getDefaultSchemaName());
        dict.connectedConfiguration(mockConnection);

        assertTrue(dict.supportsForeignKeys);
        assertTrue(dict.supportsUniqueConstraints);
        assertTrue(dict.supportsCascadeDeleteAction);
        assertFalse(dict.supportsCascadeUpdateAction);
        assertFalse(dict.supportsDeferredConstraints);

        SchemaGroup schemaGroup = new SchemaGroup();
        Schema schema = new Schema(DBIdentifier.newSchema("herddb", true), schemaGroup);

        Table parentTable = new Table(DBIdentifier.newTable("parentTable", true), schema);
        Column p1 = parentTable.addColumn(DBIdentifier.newColumn("p1", true));
        p1.setType(Types.VARCHAR);

        Table childTable = new Table(DBIdentifier.newTable("childTable", true), schema);
        childTable.setComment("This is a comment");
        Column k1 = childTable.addColumn(DBIdentifier.newColumn("k1", true));
        k1.setType(Types.VARCHAR);
        Column n1 = childTable.addColumn(DBIdentifier.newColumn("n1", true));
        n1.setType(Types.INTEGER);
        childTable.addPrimaryKey().addColumn(k1);

        childTable.addUnique(DBIdentifier.newConstraint("un1")).addColumn(n1);

        ForeignKey fk1 = childTable.addForeignKey(DBIdentifier.newForeignKey("fk1", true));
        fk1.setDeleteAction(ForeignKey.ACTION_CASCADE);
        fk1.join(n1, p1);

        String[] createTableSQL = dict.getCreateTableSQL(childTable);
        assertEquals("CREATE TABLE `herddb`.`childTable` (`k1` VARCHAR NOT NULL, `n1` INTEGER, PRIMARY KEY (`k1`), CONSTRAINT `un1` UNIQUE (`n1`))", createTableSQL[0]);
        assertEquals(1, createTableSQL.length);

        String[] addForeignKeySQL = dict.getAddForeignKeySQL(fk1);
        assertEquals("ALTER TABLE `herddb`.`childTable` ADD CONSTRAINT `fk1` FOREIGN KEY (`n1`) REFERENCES `herddb`.`parentTable` (`p1`) ON DELETE CASCADE", addForeignKeySQL[0]);
        assertEquals(1, addForeignKeySQL.length);

        String[] dropForeignKeySQL = dict.getDropForeignKeySQL(fk1, mockConnection);
        assertEquals("ALTER TABLE `herddb`.`childTable` DROP CONSTRAINT `fk1`", dropForeignKeySQL[0]);
        assertEquals(1, dropForeignKeySQL.length);


        ForeignKey fk2 = childTable.addForeignKey(DBIdentifier.newForeignKey("fk2", true));
        fk2.setDeleteAction(ForeignKey.ACTION_RESTRICT);
        fk2.setUpdateAction(ForeignKey.ACTION_CASCADE); // not supported
        fk2.join(n1, p1);

        // ON UPDATE CASCADE is not supported, so we are not adding the constraint
        String[] addForeignKeySQL2 = dict.getAddForeignKeySQL(fk2);
        assertEquals(0, addForeignKeySQL2.length);

        // ON UPDATE SET NULL is supported
        // ON DELETE RESTRICT is the default behaviour, so no need to write it in DDL
        fk2.setUpdateAction(ForeignKey.ACTION_NULL);
        String[] addForeignKeySQL3 = dict.getAddForeignKeySQL(fk2);
        assertEquals("ALTER TABLE `herddb`.`childTable` ADD CONSTRAINT `fk2` FOREIGN KEY (`n1`) REFERENCES `herddb`.`parentTable` (`p1`) ON UPDATE SET NULL", addForeignKeySQL3[0]);
        assertEquals(1, addForeignKeySQL3.length);
    }

}
