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

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.util.ClassUtil;

/**
 * No-op mapping defaults.
 *
 * @author Abe White
 */
public class NoneMappingDefaults
    implements MappingDefaults {

    private static final NoneMappingDefaults _instance =
        new NoneMappingDefaults();

    public static NoneMappingDefaults getInstance() {
        return _instance;
    }

    @Override
    public boolean defaultMissingInfo() {
        return false;
    }

    @Override
    public boolean useClassCriteria() {
        return false;
    }

    @Override
    public Object getStrategy(ClassMapping cls, boolean adapt) {
        return null;
    }

    @Override
    public Object getStrategy(Version vers, boolean adapt) {
        return null;
    }

    @Override
    public Object getStrategy(Discriminator disc, boolean adapt) {
        return null;
    }

    @Override
    public Object getStrategy(ValueMapping vm, Class<?> type, boolean adapt) {
        return null;
    }

    @Override
    public Object getDiscriminatorValue(Discriminator disc, boolean adapt) {
        return null;
    }

    @Override
    public String getTableName(ClassMapping cls, Schema schema) {
        return ClassUtil.getClassName(cls.getDescribedType()).replace('$', '_');
    }

    @Override
    public String getTableName(FieldMapping fm, Schema schema) {
        return fm.getName();
    }

    @Override
    public void populateDataStoreIdColumns(ClassMapping cls, Table table,
        Column[] cols) {
    }

    @Override
    public void populateColumns(Version vers, Table table, Column[] cols) {
    }

    @Override
    public void populateColumns(Discriminator disc, Table table,
        Column[] cols) {
    }

    @Override
    public void populateJoinColumn(ClassMapping cm, Table local, Table foreign,
        Column col, Object target, int pos, int cols) {
    }

    @Override
    public void populateJoinColumn(FieldMapping fm, Table local, Table foreign,
        Column col, Object target, int pos, int cols) {
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void populateForeignKeyColumn(ValueMapping vm, String name,
        Table local, Table foreign, Column col, Object target, boolean inverse,
        int pos, int cols) {
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void populateColumns(ValueMapping vm, String name, Table table,
        Column[] cols) {
    }

    @Override
    public boolean populateOrderColumns(FieldMapping fm, Table table,
        Column[] cols) {
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public boolean populateNullIndicatorColumns(ValueMapping vm, String name,
        Table table, Column[] cols) {
        return false;
    }

    @Override
    public ForeignKey getJoinForeignKey(ClassMapping cls, Table local,
        Table foreign) {
        return null;
    }

    @Override
    public ForeignKey getJoinForeignKey(FieldMapping fm, Table local,
        Table foreign) {
        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public ForeignKey getForeignKey(ValueMapping vm, String name, Table local,
        Table foreign, boolean inverse) {
        return null;
    }

    @Override
    public Index getJoinIndex(FieldMapping fm, Table table, Column[] cols) {
        return null;
    }

    @Override
    public Index getIndex(ValueMapping vm, String name, Table table,
        Column[] cols) {
        return null;
    }

    @Override
    public Index getIndex(Version vers, Table table, Column[] cols) {
        return null;
    }

    @Override
    public Index getIndex(Discriminator disc, Table table, Column[] cols) {
        return null;
    }

    @Override
    public Unique getJoinUnique(FieldMapping fm, Table table, Column[] cols) {
        return null;
    }

    @Override
    public Unique getUnique(ValueMapping vm, String name, Table table,
        Column[] cols) {
        return null;
    }

    @Override
    public String getPrimaryKeyName(ClassMapping cm, Table table) {
        return null;
    }

    @Override
    public void installPrimaryKey(FieldMapping fm, Table table) {
    }

    @Override
    public ForeignKey getForeignKey(ValueMapping vm, DBIdentifier name, Table local,
        Table foreign, boolean inverse) {
        return null;
    }

    @Override
    public Index getIndex(ValueMapping vm, DBIdentifier name, Table table,
        Column[] cols) {
        return null;
    }

    @Override
    public Unique getUnique(ValueMapping vm, DBIdentifier name, Table table,
        Column[] cols) {
        return null;
    }

    @Override
    public void populateColumns(ValueMapping vm, DBIdentifier name, Table table,
        Column[] cols) {
    }

    @Override
    public void populateForeignKeyColumn(ValueMapping vm, DBIdentifier name,
        Table local, Table foreign, Column col, Object target, boolean inverse,
        int pos, int cols) {
    }

    @Override
    public boolean populateNullIndicatorColumns(ValueMapping vm, DBIdentifier name,
        Table table, Column[] cols) {
        return false;
    }

    @Override
    public DBIdentifier getTableIdentifier(ClassMapping cls, Schema defaultSchema) {
        return DBIdentifier.newTable(getTableName(cls, defaultSchema));
    }

    @Override
    public DBIdentifier getTableIdentifier(FieldMapping fm, Schema defaultSchema) {
        return DBIdentifier.newTable(getTableName(fm, defaultSchema));
    }

    @Override
    public DBIdentifier getPrimaryKeyIdentifier(ClassMapping cm, Table table) {
        return DBIdentifier.NULL;
    }
}
