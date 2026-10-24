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

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Converted attributes declared as LOBs in a mapping file, with the
 * converters on the fields. The mapping file is read before the annotations,
 * so the kind of LOB can only follow the converter once the field is mapped.
 */
public class TestLobConvertedXml extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(XmlLobConvertedEntity.class, DROP_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "lob-converted-xml";
    }

    private int columnType(String field) {
        ClassMapping cm = ((JDBCConfiguration) emf.getConfiguration())
            .getMappingRepositoryInstance()
            .getMapping(XmlLobConvertedEntity.class, null, true);
        FieldMapping fm = (FieldMapping) cm.getField(field);
        assertEquals(1, fm.getColumns().length);
        return fm.getColumns()[0].getType();
    }

    public void testListConvertedToStringIsClob() {
        assertEquals(Types.CLOB, columnType("items"));
    }

    public void testItemConvertedToStringIsClob() {
        assertEquals(Types.CLOB, columnType("item"));
    }

    public void testStringConvertedToBytesIsBlob() {
        assertEquals(Types.BLOB, columnType("bytes"));
    }

    public void testRoundTrip() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            XmlLobConvertedEntity e = new XmlLobConvertedEntity();
            e.setId(1L);
            e.getItems().add(new Item("one"));
            e.getItems().add(new Item("two"));
            e.setItem(new Item("single"));
            e.setBytes("binary");
            em.persist(e);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            XmlLobConvertedEntity found = em.find(XmlLobConvertedEntity.class, 1L);
            assertEquals(2, found.getItems().size());
            assertEquals("one", found.getItems().get(0).getName());
            assertEquals("two", found.getItems().get(1).getName());
            assertEquals("single", found.getItem().getName());
            assertEquals("binary", found.getBytes());
        }
    }
}
