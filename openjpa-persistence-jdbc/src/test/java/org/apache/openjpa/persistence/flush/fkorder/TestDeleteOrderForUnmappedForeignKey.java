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
package org.apache.openjpa.persistence.flush.fkorder;

import java.sql.Connection;
import java.sql.Statement;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * A schema not created by OpenJPA - the TCK creates its tables from DDL
 * scripts - can have foreign key constraints the mapping knows nothing of:
 * a relation is then mapped to a logical foreign key. Removing the referenced
 * entity before the referencing one in the same flush must still delete the
 * referencing row first (TCK core.entitytest.persist.manyXone persistMX1Test2
 * and cascadeall.manyXone cascadeAllMX1Test2 on Derby and SQL Server,
 * OPENJPA-3034). Inserts are ordered by logical foreign keys since
 * OPENJPA-2940; this is the delete side.
 */
public class TestDeleteOrderForUnmappedForeignKey extends SQLListenerTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp(DROP_TABLES, FkoParent.class, FkoChild.class, FkoNode.class, FkoLink.class);
        // add a constraint the mapping does not know of, as a DDL script would
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Connection conn = em.unwrap(Connection.class);
            try (Statement stmnt = conn.createStatement()) {
                stmnt.execute("ALTER TABLE OJ3034_CHILD ADD CONSTRAINT OJ3034_FK "
                    + "FOREIGN KEY (PARENT_ID) REFERENCES OJ3034_PARENT (ID)");
                stmnt.execute("ALTER TABLE OJ3034_LINK ADD CONSTRAINT OJ3034_LINK_FK "
                    + "FOREIGN KEY (NEXT_ID) REFERENCES OJ3034_LINK (ID)");
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testRelationIsMappedToALogicalForeignKey() {
        ClassMapping mapping = ((JDBCConfiguration) emf.getConfiguration())
            .getMappingRepositoryInstance().getMapping(FkoChild.class, null, true);
        ForeignKey fk = mapping.getFieldMapping("parent").getForeignKey();
        assertNotNull(fk);
        assertTrue("the scenario needs a foreign key the mapping does not enforce", fk.isLogical());
    }

    /**
     * The referenced entity is removed first, as in the TCK test; the
     * referencing row has to be deleted before it all the same.
     */
    public void testRemoveReferencedEntityFirst() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            FkoChild child = new FkoChild("c1", new FkoParent("p1"));
            em.persist(child);
            em.flush();

            em.remove(child.getParent());
            em.remove(child);
            resetSQL();
            em.flush();
            assertNoUpdate();
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        em = emf.createEntityManager();
        try {
            assertNull(em.find(FkoChild.class, "c1"));
            assertNull(em.find(FkoParent.class, "p1"));
        } finally {
            em.close();
        }
    }

    /**
     * Two rows referencing each other through a non nullable logical foreign
     * key form a cycle that no nullable key can break. As nothing tells that
     * the database enforces the key, the ordering it asks for is dropped
     * instead of failing the flush - for the inserts and for the deletes.
     */
    public void testCycleOfNonNullableLogicalForeignKeys() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            FkoNode a = new FkoNode("a");
            FkoNode b = new FkoNode("b");
            a.setNext(b);
            b.setNext(a);
            em.persist(a);
            em.persist(b);
            em.getTransaction().commit();

            em.getTransaction().begin();
            em.remove(a);
            em.remove(b);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        em = emf.createEntityManager();
        try {
            assertNull(em.find(FkoNode.class, "a"));
            assertNull(em.find(FkoNode.class, "b"));
        } finally {
            em.close();
        }
    }

    /**
     * Two rows referencing each other through a nullable logical foreign key,
     * which the database enforces, removed in one flush: the cycle is broken
     * as for any other ordered key, by one UPDATE setting a key to null
     * before the two DELETEs. Without the ordering, the first DELETE would
     * violate the constraint.
     */
    public void testCycleOfNullableLogicalForeignKeys() {
        // the statements are counted, so they are not batched, as on Oracle and DB2 by default
        DBDictionary dict = getDbDictionary(emf);
        int batchLimit = dict.getBatchLimit();
        dict.setBatchLimit(0);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            FkoLink a = new FkoLink("a");
            FkoLink b = new FkoLink("b");
            a.setNext(b);
            b.setNext(a);
            em.persist(a);
            em.persist(b);
            em.getTransaction().commit();

            em.getTransaction().begin();
            em.remove(a);
            em.remove(b);
            resetSQL();
            em.flush();
            int updates = 0;
            int deletes = 0;
            for (String s : sql) {
                String stmnt = s.toUpperCase();
                if (stmnt.startsWith("UPDATE OJ3034_LINK")) {
                    assertEquals("the key is nulled before the rows are deleted: " + sql, 0, deletes);
                    assertTrue(s, stmnt.contains("NEXT_ID = ?"));
                    updates++;
                } else if (stmnt.startsWith("DELETE FROM OJ3034_LINK")) {
                    deletes++;
                }
            }
            assertEquals(sql.toString(), 1, updates);
            assertEquals(sql.toString(), 2, deletes);
            em.getTransaction().commit();
        } finally {
            dict.setBatchLimit(batchLimit);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        em = emf.createEntityManager();
        try {
            assertNull(em.find(FkoLink.class, "a"));
            assertNull(em.find(FkoLink.class, "b"));
        } finally {
            em.close();
        }
    }

    private void assertNoUpdate() {
        for (String s : sql) {
            assertFalse(s, s.toUpperCase().startsWith("UPDATE"));
        }
    }
}
