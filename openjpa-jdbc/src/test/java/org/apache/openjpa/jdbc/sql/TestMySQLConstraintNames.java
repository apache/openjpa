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

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;
import org.junit.Test;

/**
 * Named foreign key, unique and primary key constraints on MySQL and MariaDB
 * are declared in the spec form <code>CONSTRAINT &lt;name&gt; ...</code>
 * (OPENJPA-3038). On MySQL 8.0.16+ the name in
 * <code>FOREIGN KEY &lt;name&gt; (...)</code> only names the backing index and
 * the constraint gets a generated <code>&lt;table&gt;_ibfk_&lt;n&gt;</code>
 * name, so a later <code>DROP FOREIGN KEY &lt;name&gt;</code> fails. The old
 * form stays available with <code>ConstraintNameMode=mid</code>.
 */
public class TestMySQLConstraintNames {

    @Test
    public void testMySQLNamedConstraints() {
        assertNamedConstraints(new MySQLDictionary());
    }

    @Test
    public void testMariaDBNamedConstraints() {
        assertNamedConstraints(new MariaDBDictionary());
    }

    @Test
    public void testMySQLConstraintNameModeMid() {
        assertConstraintNameModeMid(new MySQLDictionary());
    }

    @Test
    public void testMariaDBConstraintNameModeMid() {
        assertConstraintNameModeMid(new MariaDBDictionary());
    }

    private static void assertNamedConstraints(DBDictionary dict) {
        assertEquals(DBDictionary.CONS_NAME_BEFORE, dict.constraintNameMode);
        dict.setConfiguration(new JDBCConfigurationImpl(false));
        ForeignKey fk = createSchema();
        Table child = fk.getTable();

        String[] add = dict.getAddForeignKeySQL(fk);
        assertEquals(1, add.length);
        assertEquals("ALTER TABLE CHILD ADD CONSTRAINT FK_PARENT FOREIGN KEY (PARENT_ID) REFERENCES PARENT (ID)",
            add[0]);

        String[] drop = dict.getDropForeignKeySQL(fk, null);
        assertEquals(1, drop.length);
        assertEquals("ALTER TABLE CHILD DROP FOREIGN KEY FK_PARENT", drop[0]);

        String create = dict.getCreateTableSQL(child)[0];
        assertEquals("CREATE TABLE CHILD (ID INTEGER NOT NULL, PARENT_ID INTEGER, CONSTRAINT PK_CHILD PRIMARY KEY (ID), "
            + "CONSTRAINT UQ_PARENT UNIQUE (PARENT_ID)) ENGINE = innodb", create);
    }

    /**
     * ConstraintNameMode=mid restores the previous FOREIGN KEY &lt;name&gt; form.
     */
    private static void assertConstraintNameModeMid(DBDictionary dict) {
        dict.constraintNameMode = DBDictionary.CONS_NAME_MID;
        dict.setConfiguration(new JDBCConfigurationImpl(false));
        ForeignKey fk = createSchema();

        assertEquals("ALTER TABLE CHILD ADD FOREIGN KEY FK_PARENT (PARENT_ID) REFERENCES PARENT (ID)",
            dict.getAddForeignKeySQL(fk)[0]);
        assertEquals("CREATE TABLE CHILD (ID INTEGER NOT NULL, PARENT_ID INTEGER, PRIMARY KEY PK_CHILD (ID), "
            + "UNIQUE UQ_PARENT (PARENT_ID)) ENGINE = innodb", dict.getCreateTableSQL(fk.getTable())[0]);
    }

    private static ForeignKey createSchema() {
        Schema schema = new Schema(DBIdentifier.NULL, new SchemaGroup());
        Table parent = new Table(DBIdentifier.newTable("PARENT"), schema);
        Column pid = parent.addColumn(DBIdentifier.newColumn("ID"));
        pid.setType(Types.INTEGER);
        parent.addPrimaryKey().addColumn(pid);

        Table child = new Table(DBIdentifier.newTable("CHILD"), schema);
        Column cid = child.addColumn(DBIdentifier.newColumn("ID"));
        cid.setType(Types.INTEGER);
        Column fkCol = child.addColumn(DBIdentifier.newColumn("PARENT_ID"));
        fkCol.setType(Types.INTEGER);
        child.addPrimaryKey(DBIdentifier.newConstraint("PK_CHILD")).addColumn(cid);
        child.addUnique(DBIdentifier.newConstraint("UQ_PARENT")).addColumn(fkCol);

        ForeignKey fk = child.addForeignKey(DBIdentifier.newForeignKey("FK_PARENT", false));
        fk.setDeleteAction(ForeignKey.ACTION_RESTRICT);
        fk.join(fkCol, pid);
        return fk;
    }
}
