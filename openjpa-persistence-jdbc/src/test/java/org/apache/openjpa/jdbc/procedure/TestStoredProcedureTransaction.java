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
package org.apache.openjpa.jdbc.procedure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.openjpa.jdbc.procedure.derby.Procedures;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the changes of a stored procedure executed in an optimistic
 * transaction are committed and rolled back with that transaction
 * (OPENJPA-3100). The procedure has an OUT parameter, which made the
 * connection it ran on leave auto-commit mode.
 */
@DatabasePlatform("org.apache.derby.jdbc.EmbeddedDriver")
class TestStoredProcedureTransaction extends SingleEMFTestCase {

    @Override
    protected boolean isSupported(DBDictionary dict) {
        return dict instanceof DerbyDictionary;
    }

    @BeforeEach
    public void setUp() throws Exception {
        setUp("openjpa.Optimistic", "true");
        exec("DROP PROCEDURE SPQ_DELETE_MARKED", true);
        exec("CREATE PROCEDURE SPQ_DELETE_MARKED(OUT DELETED BIGINT) "
            + "PARAMETER STYLE JAVA LANGUAGE JAVA MODIFIES SQL DATA EXTERNAL NAME "
            + "'" + Procedures.class.getName() + ".deleteMarked'", false);
        exec("DROP TABLE SPQ_TX_ROW", true);
        exec("CREATE TABLE SPQ_TX_ROW (ID INTEGER NOT NULL PRIMARY KEY, MARKED SMALLINT NOT NULL)", false);
        exec("INSERT INTO SPQ_TX_ROW VALUES (1, 1), (2, 1), (3, 0)", false);
    }

    @Test
    void changesAreCommittedWithTheTransaction() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        assertEquals(2L, deleteMarked(em));
        em.getTransaction().commit();
        em.close();

        assertEquals(1, countRows());
    }

    @Test
    void changesAreRolledBackWithTheTransaction() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        assertEquals(2L, deleteMarked(em));
        em.getTransaction().rollback();
        em.close();

        assertEquals(3, countRows());
    }

    private long deleteMarked(EntityManager em) {
        StoredProcedureQuery proc = em.createStoredProcedureQuery("SPQ_DELETE_MARKED");
        proc.registerStoredProcedureParameter(1, Long.class, ParameterMode.OUT);
        proc.execute();
        return ((Number) proc.getOutputParameterValue(1)).longValue();
    }

    private int countRows() {
        EntityManager em = emf.createEntityManager();
        try {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM SPQ_TX_ROW").getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    private void exec(String sql, boolean ignoreExceptions) throws Exception {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.createNativeQuery(sql).executeUpdate();
            tx.commit();
        } catch (Exception e) { // DROP of an object that does not exist
            if (tx.isActive()) {
                tx.rollback();
            }
            if (!ignoreExceptions) {
                throw e;
            }
        } finally {
            em.close();
        }
    }
}
