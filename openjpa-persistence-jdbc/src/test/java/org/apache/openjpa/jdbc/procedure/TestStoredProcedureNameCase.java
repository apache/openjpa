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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;

import org.apache.openjpa.jdbc.procedure.entity.EntityWithStoredProcedure;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Procedures created with an unquoted mixed case name are stored by Derby in upper case.
 * They must be found when the query names them the way they were created (as the
 * Jakarta Persistence TCK does, e.g. {@code GetEmpASCFromRS}).
 */
@DatabasePlatform("org.apache.derby.jdbc.EmbeddedDriver")
public class TestStoredProcedureNameCase extends SingleEMFTestCase {
    @Override
    protected boolean isSupported(DBDictionary dict) {
        return dict instanceof DerbyDictionary;
    }

    @BeforeEach
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "unsupported",
              "openjpa.DynamicEnhancementAgent", "false",
              EntityWithStoredProcedure.class, EntityWithStoredProcedure.Mapping2.class, CLEAR_TABLES);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new EntityWithStoredProcedure(1, "Alan"));
        em.persist(new EntityWithStoredProcedure(2, "Grace"));
        em.persist(new EntityWithStoredProcedure(3, "Ada"));
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void outParameter() {
        EntityManager em = emf.createEntityManager();
        try {
            create(em, "GetNameFromOut", "(IN IN_PARAM INTEGER, OUT OUT_PARAM VARCHAR(255))", "nameFromOut", 0);
            StoredProcedureQuery spq = em.createStoredProcedureQuery("GetNameFromOut");
            spq.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);
            spq.setParameter(1, 2);
            assertFalse(spq.execute());
            assertEquals("Grace", spq.getOutputParameterValue(2));
        } finally {
            em.close();
        }
    }

    @Test
    void inOutParameter() {
        EntityManager em = emf.createEntityManager();
        try {
            create(em, "GetNameFromInOut", "(INOUT INOUT_PARAM VARCHAR(255))", "nameFromInOut", 0);
            StoredProcedureQuery spq = em.createStoredProcedureQuery("GetNameFromInOut");
            spq.registerStoredProcedureParameter(1, String.class, ParameterMode.INOUT);
            spq.setParameter(1, "3");
            assertFalse(spq.execute());
            assertEquals("Ada", spq.getOutputParameterValue(1));
        } finally {
            em.close();
        }
    }

    @Test
    void resultSet() {
        EntityManager em = emf.createEntityManager();
        try {
            create(em, "GetAllAscFromRS", "()", "allAscFromRS", 1);
            StoredProcedureQuery spq = em.createStoredProcedureQuery("GetAllAscFromRS",
                    EntityWithStoredProcedure.class);
            assertTrue(spq.execute());
            List<?> result = spq.getResultList();
            assertEquals(3, result.size());
            assertEquals("Alan", ((EntityWithStoredProcedure) result.get(0)).getName());
            assertEquals("Ada", ((EntityWithStoredProcedure) result.get(2)).getName());
        } finally {
            em.close();
        }
    }

    @Test
    void temporalParameter() {
        EntityManager em = emf.createEntityManager();
        try {
            create(em, "GetDayUsingDateFromOut", "(IN IN_PARAM DATE, OUT OUT_PARAM INTEGER)", "dayFromOut", 0);
            Calendar cal = new GregorianCalendar(2000, Calendar.FEBRUARY, 14);

            StoredProcedureQuery spq = em.createStoredProcedureQuery("GetDayUsingDateFromOut");
            spq.registerStoredProcedureParameter(1, Date.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
            spq.setParameter(1, cal.getTime(), TemporalType.DATE);
            assertFalse(spq.execute());
            assertEquals(14, spq.getOutputParameterValue(2));

            spq = em.createStoredProcedureQuery("GetDayUsingDateFromOut");
            spq.registerStoredProcedureParameter(1, Calendar.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
            spq.setParameter(1, cal, TemporalType.DATE);
            assertFalse(spq.execute());
            assertEquals(14, spq.getOutputParameterValue(2));
        } finally {
            em.close();
        }
    }

    @Test
    void unknownProcedure() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createStoredProcedureQuery("DoesNotExistAnywhere");
            fail("Expected an exception for an unknown procedure");
        } catch (RuntimeException e) {
            assertEquals("Procedure [DoesNotExistAnywhere] not found", e.getMessage());
        } finally {
            em.close();
        }
    }

    private static void create(EntityManager em, String name, String params, String method, int resultSets) {
        exec(em, "DROP PROCEDURE " + name, true);
        exec(em, "CREATE PROCEDURE " + name + params + " PARAMETER STYLE JAVA LANGUAGE JAVA"
                + (resultSets > 0 ? " DYNAMIC RESULT SETS " + resultSets : "")
                + " EXTERNAL NAME '" + TestStoredProcedureNameCase.class.getName() + "." + method + "'", false);
    }

    private static void exec(EntityManager em, String sql, boolean ignoreException) {
        try {
            em.getTransaction().begin();
            em.createNativeQuery(sql).executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (!ignoreException) {
                throw new RuntimeException(e);
            }
        }
    }

    // Derby stored procedures

    public static void nameFromOut(int id, String[] name) throws Exception {
        name[0] = findName(id);
    }

    public static void nameFromInOut(String[] idAndName) throws Exception {
        idAndName[0] = findName(Integer.parseInt(idAndName[0]));
    }

    public static void allAscFromRS(ResultSet[] rs) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:default:connection");
        rs[0] = c.prepareStatement("SELECT * FROM EntityWithStoredProcedure ORDER BY id ASC").executeQuery();
    }

    public static void dayFromOut(java.sql.Date date, int[] day) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        day[0] = cal.get(Calendar.DAY_OF_MONTH);
    }

    private static String findName(int id) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:default:connection");
             PreparedStatement ps = c.prepareStatement("SELECT name FROM EntityWithStoredProcedure WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
