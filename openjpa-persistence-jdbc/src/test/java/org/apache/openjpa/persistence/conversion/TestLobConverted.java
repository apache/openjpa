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

import java.sql.Types;
import java.util.Arrays;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A {@code @Lob} attribute with a converter is a CLOB or a BLOB depending on
 * the converter's database type. The kind of LOB used to follow the attribute
 * type, so a list or a bean converted to a string got a BLOB (BYTEA on
 * PostgreSQL) and a string converted to bytes got a CLOB.
 */
public class TestLobConverted extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(LobConvertedEntity.class, DROP_TABLES);
    }

    private Column column(String field, boolean element) {
        ClassMapping cm = ((JDBCConfiguration) emf.getConfiguration())
            .getMappingRepositoryInstance()
            .getMapping(LobConvertedEntity.class, null, true);
        FieldMapping fm = (FieldMapping) cm.getField(field);
        Column[] cols = element ? fm.getElementMapping().getColumns() : fm.getColumns();
        assertEquals(1, cols.length);
        return cols[0];
    }

    public void testListConvertedToStringIsClob() {
        assertEquals(Types.CLOB, column("items", false).getType());
    }

    public void testItemConvertedToStringIsClob() {
        assertEquals(Types.CLOB, column("item", false).getType());
    }

    public void testElementConvertedToBytesIsBlob() {
        assertEquals(Types.BLOB, column("elements", true).getType());
    }

    public void testStringConvertedToBytesIsBlob() {
        assertEquals(Types.BLOB, column("bytes", false).getType());
    }

    /**
     * A converter to a byte array is read as bytes, with or without
     * {@code @Lob}. The column used to be typed as an object, so reading
     * the value back passed the driver's Blob to the converter.
     */
    public void testStringConvertedToBytesWithoutLobIsReadAsBytes() {
        assertEquals(JavaSQLTypes.BYTES, column("plainBytes", false).getJavaType());
    }

    public void testLobsWithoutConverterAreUnchanged() {
        assertEquals(Types.CLOB, column("text", false).getType());
        assertEquals(Types.BLOB, column("data", false).getType());
        assertEquals(Types.BLOB, column("serial", false).getType());
    }

    public void testRoundTrip() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            LobConvertedEntity e = new LobConvertedEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            e.setItem(new Item("single"));
            e.getElements().add("first");
            e.getElements().add("second");
            e.setBytes("binary");
            e.setPlainBytes("plain");
            e.setText("text");
            e.setData(new byte[] { 1, 2, 3 });
            e.setSerial(new Item("serialized"));
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            LobConvertedEntity found = em.find(LobConvertedEntity.class, 1L);
            assertEquals(2, found.getItems().size());
            assertEquals("one", found.getItems().get(0).getName());
            assertEquals("two", found.getItems().get(1).getName());
            assertEquals("single", found.getItem().getName());
            assertEquals(2, found.getElements().size());
            assertTrue(found.getElements().contains("first"));
            assertTrue(found.getElements().contains("second"));
            assertEquals("binary", found.getBytes());
            assertEquals("plain", found.getPlainBytes());
            assertEquals("text", found.getText());
            assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, found.getData()));
            assertEquals("serialized", found.getSerial().getName());
        }
    }
}
