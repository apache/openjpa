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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Stored procedures on Oracle, written like the Jakarta Persistence TCK procedures.
 */
public class TestOracleStoredProcedure extends SingleEMFTestCase {

    private static final String PROC = "OPENJPA_REFCURSOR_PROC";

    @Override
    public void setUp() throws Exception {
        setSupportedDatabases(OracleDictionary.class);
        if (isTestsDisabled()) {
            return;
        }
        super.setUp();
    }

    /**
     * A procedure returning its result in a SYS_REFCURSOR OUT parameter.
     */
    public void testRefCursorOutParameter() {
        EntityManager em = emf.createEntityManager();
        boolean created = false;
        try {
            exec(em, "CREATE OR REPLACE PROCEDURE " + PROC + "(IN_PARAM IN INTEGER, P_RS OUT SYS_REFCURSOR) AS "
                + "BEGIN OPEN P_RS FOR SELECT IN_PARAM AS X, IN_PARAM + 1 AS Y FROM DUAL; END;");
            created = true;

            StoredProcedureQuery spq = em.createStoredProcedureQuery(PROC);
            spq.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter(2, void.class, ParameterMode.REF_CURSOR);
            spq.setParameter(1, 41);
            List<?> rows = spq.getResultList();
            assertEquals(1, rows.size());
            if (!(rows.get(0) instanceof Object[] row)) {
                throw new AssertionError("Expected an Object[] row but got " + rows.get(0));
            }
            assertEquals(41, intValue(row[0]));
            assertEquals(42, intValue(row[1]));
        } finally {
            close(em, created ? PROC : null);
        }
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new AssertionError("Expected a number but got " + value);
    }

    /**
     * Drops the given procedure, if any, and closes the entity manager. A failure of the drop does not hide
     * the failure of the test.
     */
    private static void close(EntityManager em, String procedure) {
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (procedure != null) {
                exec(em, "DROP PROCEDURE " + procedure);
            }
        } catch (RuntimeException e) {
            // keep the outcome of the test
        } finally {
            em.close();
        }
    }

    private static void exec(EntityManager em, String sql) {
        em.getTransaction().begin();
        em.createNativeQuery(sql).executeUpdate();
        em.getTransaction().commit();
    }
}
