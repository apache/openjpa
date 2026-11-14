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
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The column of a converted attribute takes the converter's database type,
 * the Y in {@code AttributeConverter<X, Y>}, also when the converter gets it
 * from a generic superclass or from an interface that extends
 * AttributeConverter, and when it is applied automatically. A converter whose
 * database type could not be found mapped the attribute as a serialized
 * object, a BLOB (BYTEA on PostgreSQL), while the same value externalized by
 * OpenJPA's Externalizer API got a column of the externalized type.
 */
class TestConvertedColumnType extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(ConvertedColumnTypeEntity.class, RankConverter.class, BoxConverter.class,
            DROP_TABLES);
    }

    private Column column(String field) {
        ClassMapping cm = ((JDBCConfiguration) emf.getConfiguration())
            .getMappingRepositoryInstance()
            .getMapping(ConvertedColumnTypeEntity.class, null, true);
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

    @Test
    void uuidColumnsAgree() {
        assertColumn("externalized", JavaTypes.STRING, Types.VARCHAR);
        assertColumn("converted", JavaTypes.STRING, Types.VARCHAR);
        if (getDBDictionary().supportsUuidType) {
            assertColumn("nativeUuid", JavaTypes.UUID_OBJ, Types.OTHER);
        } else {
        	assertColumn("nativeUuid", JavaTypes.STRING, Types.VARCHAR);
        }
    }

    @Test
    void converterToLong() {
        assertColumn("code", JavaTypes.LONG_OBJ, Types.BIGINT, Types.NUMERIC, Types.DECIMAL);
    }

    @Test
    void converterWithGenericSuperclass() {
        assertColumn("inheritedCode", JavaTypes.LONG_OBJ, Types.BIGINT, Types.NUMERIC, Types.DECIMAL);
    }

    @Test
    void converterWithAttributeConverterSubinterface() {
        assertColumn("interfaceCode", JavaTypes.LONG_OBJ, Types.BIGINT, Types.NUMERIC, Types.DECIMAL);
    }

    @Test
    void autoAppliedConverterWithGenericSuperclass() {
        assertColumn("rating", JavaTypes.INT_OBJ, Types.INTEGER, Types.NUMERIC, Types.DECIMAL);
    }

    @Test
    void autoAppliedConverterOfParameterizedType() {
        assertColumn("box", JavaTypes.STRING, Types.VARCHAR);
    }

    @Test
    void schemaColumnTypes() throws SQLException {
        // the table must exist in the database to read its columns back
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new ConvertedColumnTypeEntity());
            em.getTransaction().commit();
        }
        String table = column("code").getTable().getIdentifier().getName();
        try (EntityManager em = emf.createEntityManager();
             Connection conn = (Connection) OpenJPAPersistence.cast(em).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            for (String field : List.of("externalized", "converted", "code",
                "inheritedCode", "interfaceCode", "rating", "box")) {
                String name = column(field).getIdentifier().getName();
                int type = dbColumnType(meta, table, name);
                switch (type) {
                    case Types.BLOB, Types.LONGVARBINARY, Types.VARBINARY, Types.BINARY ->
                        fail(field + " is stored as binary, type " + type);
                    default -> {
                        // not binary
                    }
                }
            }
        }
    }

    private static int dbColumnType(DatabaseMetaData meta, String table, String column)
        throws SQLException {
        for (String t : List.of(table, table.toUpperCase(), table.toLowerCase())) {
            for (String c : List.of(column, column.toUpperCase(), column.toLowerCase())) {
                try (ResultSet rs = meta.getColumns(null, null, t, c)) {
                    if (rs.next()) {
                        return rs.getInt("DATA_TYPE");
                    }
                }
            }
        }
        fail("column " + table + "." + column + " not found");
        return Types.OTHER;
    }

    @Test
    void roundTrip() {
        UUID uuid = UUID.randomUUID();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedColumnTypeEntity e = new ConvertedColumnTypeEntity();
            e.setId(1L);
            e.setExternalized(uuid);
            e.setConverted(uuid);
            e.setCode(new Code(Long.MAX_VALUE));
            e.setInheritedCode(new Code(42L));
            e.setInterfaceCode(new Code(-7L));
            e.setRating(new Rank(3));
            e.setBox(new Box<>("boxed"));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedColumnTypeEntity found = em.find(ConvertedColumnTypeEntity.class, 1L);
            assertEquals(uuid, found.getExternalized());
            assertEquals(uuid, found.getConverted());
            assertEquals(Long.MAX_VALUE, found.getCode().getValue());
            assertEquals(42L, found.getInheritedCode().getValue());
            assertEquals(-7L, found.getInterfaceCode().getValue());
            assertEquals(3, found.getRating().getValue());
            assertEquals("boxed", found.getBox().getContent());
        }
    }
}
