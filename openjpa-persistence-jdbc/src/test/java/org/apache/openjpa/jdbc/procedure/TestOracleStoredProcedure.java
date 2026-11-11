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

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.Test;

/**
 * Stored procedures on Oracle, written like the Jakarta Persistence TCK procedures.
 */
public class TestOracleStoredProcedure extends SingleEMFTestCase {

    private static final String PROC = "OPENJPA_REFCURSOR_PROC";
    private static final String NUMBER_PROC = "OPENJPA_NUMBER_OUT_PROC";

    @Override
    protected boolean isSupported(DBDictionary dict) {
        return dict instanceof OracleDictionary;
    }

    /**
     * A procedure returning its result in a SYS_REFCURSOR OUT parameter.
     */
    @Test
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

    /**
     * Oracle reports an INTEGER OUT parameter, which is a NUMBER, as a BigDecimal. It is returned in the
     * registered type if the conversion is exact, and unchanged otherwise.
     */
    @Test
    public void testNumberOutParameterHasRegisteredType() {
        EntityManager em = emf.createEntityManager();
        boolean created = false;
        try {
            exec(em, "CREATE OR REPLACE PROCEDURE " + NUMBER_PROC + "(IN_PARAM IN NUMBER, OUT_PARAM OUT INTEGER) AS "
                + "BEGIN OUT_PARAM := IN_PARAM; END;");
            created = true;

            assertEquals(Integer.valueOf(7), callNumberProc(em, Integer.class, new BigDecimal("7")));
            assertEquals(Long.valueOf(3000000000L), callNumberProc(em, Long.class, new BigDecimal("3000000000")));
            assertEquals(new BigDecimal("3000000000"), callNumberProc(em, Integer.class, new BigDecimal("3000000000")));
        } finally {
            close(em, created ? NUMBER_PROC : null);
        }
    }

    private static Object callNumberProc(EntityManager em, Class<?> outType, BigDecimal in) {
        StoredProcedureQuery spq = em.createStoredProcedureQuery(NUMBER_PROC);
        spq.registerStoredProcedureParameter(1, BigDecimal.class, ParameterMode.IN);
        spq.registerStoredProcedureParameter(2, outType, ParameterMode.OUT);
        spq.setParameter(1, in);
        spq.execute();
        return spq.getOutputParameterValue(2);
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
