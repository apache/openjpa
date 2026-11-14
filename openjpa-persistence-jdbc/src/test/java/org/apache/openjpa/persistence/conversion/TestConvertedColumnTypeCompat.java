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
package org.apache.openjpa.persistence.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <code>ResolveConverterTypeHierarchy=false</code> restores the OpenJPA 4.1.x
 * mapping for hierarchy converters: a converter that gets its database type
 * from a generic superclass or from an interface extending
 * AttributeConverter does not decide the column type, so the serializable
 * attribute is mapped as a serialized object, and an auto apply converter of
 * that kind or of a parameterized attribute type is not applied.
 */
class TestConvertedColumnTypeCompat extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(ConvertedColumnTypeEntity.class, RankConverter.class, BoxConverter.class,
            DROP_TABLES, "openjpa.Compatibility", "ResolveConverterTypeHierarchy=false");
    }

    private MappingRepository repository() {
        return ((JDBCConfiguration) emf.getConfiguration()).getMappingRepositoryInstance();
    }

    private Column column(String field) {
        ClassMapping cm = repository().getMapping(ConvertedColumnTypeEntity.class, null, true);
        FieldMapping fm = (FieldMapping) cm.getField(field);
        Column[] cols = fm.getColumns();
        assertEquals(1, cols.length);
        return cols[0];
    }

    private void assertColumn(String field, int javaType, int... sqlTypes) {
        Column col = column(field);
        assertEquals(javaType, col.getJavaType(), field + " java type");
        for (int sqlType : sqlTypes) {
            if (col.getType() == sqlType) {
                return;
            }
        }
        fail(field + " has SQL type " + col.getType() + " (" + col.getTypeIdentifier() + ")");
    }

    private void assertSerialized(String field) {
        assertColumn(field, JavaTypes.OBJECT, Types.BLOB, Types.LONGVARBINARY,
            Types.VARBINARY, Types.BINARY);
    }

    @Test
    void directlyDeclaredConvertersKeepTheirColumnType() {
        assertColumn("converted", JavaTypes.STRING, Types.VARCHAR);
        assertColumn("code", JavaTypes.LONG_OBJ, Types.BIGINT, Types.NUMERIC, Types.DECIMAL);
    }

    @Test
    void converterWithGenericSuperclassIsSerialized() {
        assertSerialized("inheritedCode");
    }

    @Test
    void converterWithAttributeConverterSubinterfaceIsSerialized() {
        assertSerialized("interfaceCode");
    }

    @Test
    void autoApplyConverterWithGenericSuperclassIsNotRegistered() {
        column("rating");
        assertNull(repository().getAutoApplyConverter(Rank.class));
        assertSerialized("rating");
    }

    @Test
    void autoApplyConverterOfParameterizedTypeIsNotRegistered() {
        column("box");
        assertNull(repository().getAutoApplyConverter(Box.class));
        assertSerialized("box");
    }
}
