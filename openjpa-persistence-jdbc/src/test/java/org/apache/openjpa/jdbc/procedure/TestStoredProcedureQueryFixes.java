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

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.openjpa.jdbc.procedure.derby.Procedures;
import org.apache.openjpa.jdbc.procedure.entity.EntityWithStoredProcedure;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for StoredProcedureQuery fixes:
 * - getSingleResult() must throw NoResultException/NonUniqueResultException directly
 * - getOutputParameterValue(int) must throw IllegalArgumentException for invalid positions
 * - getParameterValue(Parameter) must throw IllegalArgumentException for parameter from different query
 */
@DatabasePlatform("org.apache.derby.jdbc.EmbeddedDriver")
public class TestStoredProcedureQueryFixes extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "unsupported",
              "openjpa.DynamicEnhancementAgent", "false",
              EntityWithStoredProcedure.class, EntityWithStoredProcedure.Mapping2.class);
        setSupportedDatabases(DerbyDictionary.class);
    }

    /**
     * Test that getOutputParameterValue(int) throws IllegalArgumentException
     * for a position that does not correspond to any registered parameter.
     * Uses positional parameters (like the TCK does).
     */
    public void testGetOutputParameterValueInvalidPosition() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            exec(em, "DROP PROCEDURE XTWO", true);
            exec(em, "CREATE PROCEDURE XTWO(IN SOME_NUMBER INTEGER,OUT x2 INTEGER) " +
                    "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                    "'" + Procedures.class.getName() + ".x2'", false);

            // Use createStoredProcedureQuery with positional parameters
            StoredProcedureQuery spq = em.createStoredProcedureQuery("XTWO");
            spq.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
            spq.setParameter(1, 5);
            spq.execute();

            // Position 99 does not correspond to any registered parameter
            try {
                spq.getOutputParameterValue(99);
                fail("Expected IllegalArgumentException for invalid position 99");
            } catch (IllegalArgumentException e) {
                // expected
            }

            // Position 2 should work (OUT parameter)
            Object result = spq.getOutputParameterValue(2);
            assertEquals(10, result);
        } finally {
            em.close();
        }
    }

    /**
     * Test that getParameterValue(Parameter) throws IllegalArgumentException
     * when the parameter belongs to a different query.
     * Uses positional parameters (like the TCK does).
     */
    public void testGetParameterValueFromDifferentQuery() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            exec(em, "DROP PROCEDURE XINOUT", true);
            exec(em, "CREATE PROCEDURE XINOUT(INOUT P INTEGER) " +
                    "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                    "'" + Procedures.class.getName() + ".inout'", false);
            exec(em, "DROP PROCEDURE XTWO", true);
            exec(em, "CREATE PROCEDURE XTWO(IN SOME_NUMBER INTEGER,OUT x2 INTEGER) " +
                    "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                    "'" + Procedures.class.getName() + ".x2'", false);

            // Create first query with positional params and get its parameter
            StoredProcedureQuery spq1 = em.createStoredProcedureQuery("XINOUT");
            spq1.registerStoredProcedureParameter(1, Integer.class, ParameterMode.INOUT);
            spq1.setParameter(1, 5);
            Parameter<?> paramFromSpq1 = spq1.getParameter(1);
            assertNotNull(paramFromSpq1);

            // Create second query with positional params
            StoredProcedureQuery spq2 = em.createStoredProcedureQuery("XTWO");
            spq2.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
            spq2.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
            spq2.setParameter(1, 7);

            // Getting value with parameter from different query should throw IAE
            try {
                spq2.getParameterValue(paramFromSpq1);
                fail("Expected IllegalArgumentException when using parameter from a different query");
            } catch (IllegalArgumentException e) {
                // expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * Test that getSingleResult() throws NoResultException directly
     * (not wrapped in PersistenceException) when no results are returned.
     */
    public void testGetSingleResultNoResultException() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            exec(em, "DROP PROCEDURE NORESULT", true);
            exec(em, "CREATE PROCEDURE NORESULT() " +
                    "PARAMETER STYLE JAVA LANGUAGE JAVA DYNAMIC RESULT SETS 1 EXTERNAL NAME " +
                    "'" + TestStoredProcedureQueryFixes.class.getName() + ".emptyResultSet'", false);

            StoredProcedureQuery spq = em.createStoredProcedureQuery("NORESULT");
            try {
                spq.getSingleResult();
                fail("Expected NoResultException");
            } catch (NoResultException e) {
                // expected - should be NoResultException, not PersistenceException
            }
        } finally {
            em.close();
        }
    }

    /**
     * Test that getSingleResult() throws NonUniqueResultException directly
     * (not wrapped in PersistenceException) when multiple results are returned.
     */
    public void testGetSingleResultNonUniqueResultException() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            // Insert test data
            em.getTransaction().begin();
            for (int i = 10; i < 13; i++) {
                EntityWithStoredProcedure e = new EntityWithStoredProcedure();
                e.setId(i);
                e.setName("test" + i);
                em.persist(e);
            }
            em.getTransaction().commit();
            em.clear();

            exec(em, "DROP PROCEDURE MULTIRESULT", true);
            exec(em, "CREATE PROCEDURE MULTIRESULT() " +
                    "PARAMETER STYLE JAVA LANGUAGE JAVA DYNAMIC RESULT SETS 1 EXTERNAL NAME " +
                    "'" + TestStoredProcedureQueryFixes.class.getName() + ".multipleResultSet'", false);

            StoredProcedureQuery spq = em.createStoredProcedureQuery("MULTIRESULT",
                EntityWithStoredProcedure.class);
            try {
                spq.getSingleResult();
                fail("Expected NonUniqueResultException");
            } catch (NonUniqueResultException e) {
                // expected - should be NonUniqueResultException, not PersistenceException
            }
        } finally {
            em.close();
        }
    }

    // Derby stored procedure: returns empty result set
    public static void emptyResultSet(java.sql.ResultSet[] rs) throws Exception {
        java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:default:connection");
        rs[0] = c.createStatement().executeQuery(
            "SELECT * FROM EntityWithStoredProcedure WHERE id = -999");
    }

    // Derby stored procedure: returns multiple rows
    public static void multipleResultSet(java.sql.ResultSet[] rs) throws Exception {
        java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:default:connection");
        rs[0] = c.createStatement().executeQuery(
            "SELECT * FROM EntityWithStoredProcedure ORDER BY id");
    }

    private static void exec(EntityManager em, String sql, boolean ignoreException) {
        try {
            em.getTransaction().begin();
            em.createNativeQuery(sql).executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (!ignoreException) {
                throw new RuntimeException(e);
            }
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }
}
