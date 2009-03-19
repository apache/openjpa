/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.openjpa.persistence.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.PersistenceException;
import org.apache.openjpa.persistence.QueryTimeoutException;
import org.apache.openjpa.persistence.query.common.apps.QTimeout;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests the new query timeout hint support in the JPA 2.0 spec.
 * 
 * @version $Rev$ $Date$
 */
public class TestQueryTimeout extends SQLListenerTestCase {

    private boolean skipTests = false;
    // does the DB platform allow retry instead of forcing transaction rollback
    private boolean supportsQueryTimeoutException = false; 

    @Override
    public void setUp() {
        super.setUp(DROP_TABLES, QTimeout.class);
        getLog().trace("setUp()");
        String[] _strings = new String[] { "a", "b", "c" };
        QTimeout qt = null;
        EntityManager em = null;

        // determine if we should run our tests on this DB platform and what 
        // exception type to catch
        DBDictionary dict = ((JDBCConfiguration) emf.getConfiguration())
            .getDBDictionaryInstance();
        if ((dict.supportsQueryTimeout) && (dict instanceof DerbyDictionary)) {
            // set whether we expect to see QueryTimeoutException or 
            // PersistenceException
            supportsQueryTimeoutException = false;
        } else {
            // FIXME drwoods - OPENJPA-964 - haven't determined what the other 
            // DBs support
            // setQueryTimeout is not working with DB2 v9.5.3a on Windows...
            getLog().info("TestQueryTimeout tests are being skipped, due to " +
                "DB not supporting Query Timeouts.");
            skipTests = true;
            return;
        }

        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            getLog().trace("setUp() - creating Qtimeout entities");
            em.getTransaction().begin();
            for (int i = 0; i < _strings.length; i++) {
                qt = new QTimeout(i, _strings[i]);
                em.persist(qt);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            fail("Unexpected setup exception occurred - " + e);
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }

        // execute some native SQL with no timeouts
        if (dict instanceof DerbyDictionary) {
            getLog().trace("setUp() - creating DELAY function only for Derby." +
                "  Other DBs require manual setup.");
            // remove existing function if it exists and ignore any errors
            exec(true, 0, "DROP FUNCTION DELAY");
            exec(false, 0, "CREATE FUNCTION DELAY(SECONDS INTEGER, " + 
                "VALUE INTEGER) RETURNS INTEGER PARAMETER STYLE JAVA NO SQL " +
                "LANGUAGE JAVA EXTERNAL NAME 'org.apache.openjpa.persistence." +
                "query.TestQueryTimeout.delay'");
        }
        getLog().trace("setUp() - creating BEFORE UPDATE/INSERT TRIGGERs for " +
            "all DBs");
        exec(false, 0, "CREATE TRIGGER t1 NO CASCADE BEFORE UPDATE ON " +
            "qtimeout FOR EACH ROW MODE DB2SQL values DELAY(2,-1)");
        exec(false, 0, "CREATE TRIGGER t2 NO CASCADE BEFORE INSERT ON " +
            "qtimeout FOR EACH ROW MODE DB2SQL values DELAY(2,-2)");
        // Don't include a DELETE trigger, as it slows down the DROP_TABLES 
        // cleanup between tests
        // exec(0, "CREATE TRIGGER t3 NO CASCADE BEFORE DELETE ON qtimeout " +
        //     "FOR EACH ROW MODE DB2SQL values DELAY(2,-3)");
    }

    /*
     * Query timeout scenarios to test for:
     *   1) By default, there is no timeout
     *   2) Setting timeout to 0 is same as no timeout (JDBC defined)
     *     2.1) using the QueryHint annotation
     *     2.2) calling setHint()
     *   3) Setting timeout to msecs < DELAY value causes new 
     *      javax.persistence.QueryTimeoutException when set by:
     *     3.1) using the QueryHint annotation
     *     3.2) calling setHint()
     * Operations to validate through cross coverage of items #1-#3:
     *   a) getResultList()
     *   b) getSingleResult()
     *   c) executeUpdate()
     * Other behaviors to test for:
     *   4) Setting timeout to < 0 should be treated as no timeout supplied
     * Exception generation to test for:
     *   If the DB query timeout does not cause a transaction rollback, then a 
     *   QueryTimeoutException should be thrown.
     *     Applicable to:  unknown
     *   Else if the DB query timeout causes a transaction rollback, then a 
     *   PersistenceException should be thrown instead of a QTE.
     *     Applicable to:  Derby
     */

    /**
     * Scenario being tested: 1a) By default, there is no timeout for queries.
     * Expected Results: The DELAY function is being called and the query takes
     * 6000+ msecs to complete.
     */
    public void testQueryTimeout1a() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout1a() - No Query timeout");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintList");
            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));
            try {
                long startTime = System.currentTimeMillis();
                @SuppressWarnings("unchecked")
                List results = q.getResultList();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout1a() - NoHintList runTime" + 
                    " msecs=" + runTime);
                // Hack - Windows sometimes returns 5999 instead of 6000+
                assertTrue("Should have taken 6+ secs, but was msecs=" +
                    runTime, runTime > 5900);
                assertEquals("Verify we found 2 results.", 2, results.size());
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout1a() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 1c) By default, there is no timeout for updates.
     * Expected Results: The DELAY function is being called and the query takes
     * 2000+ msecs to complete.
     */
    public void testQueryTimeout1c() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout1c() - No Update timeout");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            try {
                long startTime = System.currentTimeMillis();
                QTimeout qt = em.find(QTimeout.class, new Integer(1));
                em.getTransaction().begin();
                qt.setStringField("updated");
                em.flush();
                em.getTransaction().commit();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout1c() - EM find/update runTime" +
                    " msecs=" + runTime);
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
                em.clear();
                qt = em.find(QTimeout.class, new Integer(1));
                assertEquals("Verify the entity was updated.",
                    qt.getStringField(), "updated");
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout1c() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 2.1.a) Explicit annotated QueryHint of timeout=0
     * is treated the same as the default no timeout for queries.
     * Expected Results: The DELAY function is being called and the query
     * takes 6000+ msecs to complete.
     */
    public void testQueryTimeout2a() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout2a() - QueryHint=0");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("Hint0msec");
            // verify javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = new Integer(
                (String) hints.get("javax.persistence.query.timeout"));
            getLog().trace("testQueryTimeout2a() - Retrieved hint " + 
                "javax.persistence.query.timeout=" + timeout);
            assertEquals(timeout, new Integer(0));

            try {
                long startTime = System.currentTimeMillis();
                @SuppressWarnings("unchecked")
                List results = q.getResultList();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout2a() - Hint0msec runTime msecs="
                    + runTime);
                // Hack - Windows sometimes returns 5999 instead of 6000+
                assertTrue("Should have taken 6+ secs, but was msecs=" +
                    runTime, runTime > 5900);
                assertEquals("Verify we found 2 results.", 2, results.size());
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout2a() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 2.1.b) Explicit setHint of timeout=0 is treated
     * the same as the default no timeout for queries.
     * Expected Results: The DELAY function is being called and the query
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout2b() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(0);
        getLog().trace("testQueryTimeout2b() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value to 0 and verify it was set
            getLog().trace("testQueryTimeout2b() - Setting hint " + 
                "javax.persistence.query.timeout=" + setTime);
            q.setHint("javax.persistence.query.timeout", setTime);
            hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = (Integer) hints.get(
                "javax.persistence.query.timeout");
            getLog().trace("testQueryTimeout2b() - Retrieved hint " +
                "javax.persistence.query.timeout=" + timeout);
            assertEquals(timeout, setTime);

            try {
                long startTime = System.currentTimeMillis();
                Object result = q.getSingleResult();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout2b() - NoHintSingle runTime " + 
                    "msecs=" + runTime);
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
                assertNotNull("Verify we received a result.", result);
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout2b() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 3.1.a) Explicit annotated QueryHint of
     * timeout=1000 msecs will cause the query to timeout.
     * Expected Results: QueryTimeoutException or PersistenceException
     */
    public void testQueryTimeout3a() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(1000);
        getLog().trace("testQueryTimeout3a() - QueryHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("Hint1000msec");

            // verify javax.persistence.query.timeout hint via annotation set
            Map<String, Object> hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = new Integer((String) hints.get(
                "javax.persistence.query.timeout"));
            getLog().trace(
                "testQueryTimeout3a() - Found javax.persistence.query.timeout="
                + timeout);
            assertTrue("Expected to find a javax.persistence.query.timeout="
                + setTime, (timeout.intValue() == setTime.intValue()));

            try {
                long startTime = System.currentTimeMillis();
                @SuppressWarnings( { "unchecked", "unused" })
                List results = q.getResultList();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace(
                    "testQueryTimeout3a() - Hint1000msec runTime msecs="
                    + runTime);
                //assertEquals("Should never get valid results due to the " + 
                // "timeout.", 2, results.size());
                fail("QueryTimeout annotation failed to cause an Exception " +
                    "in testQueryTimeout3a(" + setTime +
                    " msecs), runTime msecs=" + runTime);
            } catch (Exception e) {
                // expected
                checkException("testQueryTimeout3a()", e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 3.2.b) Explicit setHint of timeout to 1000 msecs
     * will cause the query to timeout.
     * Expected Results: QueryTimeoutException or PersistenceException
     */
    public void testQueryTimeout3b() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(1000);
        getLog().trace("testQueryTimeout3b() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value and verify it was set
            getLog().trace("testQueryTimeout3b() - Setting hint " +
                "javax.persistence.query.timeout=" + setTime);
            q.setHint("javax.persistence.query.timeout", setTime);
            hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = (Integer) hints.get(
                "javax.persistence.query.timeout");
            assertEquals(timeout, setTime);

            try {
                long startTime = System.currentTimeMillis();
                @SuppressWarnings("unused")
                Object result = q.getSingleResult();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace(
                    "testQueryTimeout3b() - NoHintSingle runTime msecs="
                    + runTime);
                //assertNull("Should never get valid result due to the timeout.", result);
                fail("QueryTimeout annotation failed to cause an Exception " + 
                    "in testQueryTimeout3b(" + setTime +
                    " mscs), runTime msecs=" + runTime);
            } catch (Exception e) {
                // expected
                checkException("testQueryTimeout3b()", e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 4) Timeouts < 0 are ignored and treated as the
     * default no timeout scenario.
     * Expected Results: The DELAY function is being called and the query
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout4() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(-2000);
        getLog().trace("testQueryTimeout4() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value to -2000 and verify it was set
            getLog().trace("testQueryTimeout4() - Setting hint " +
                "javax.persistence.query.timeout="
                + setTime);
            q.setHint("javax.persistence.query.timeout", setTime);
            hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = (Integer) hints.get(
                "javax.persistence.query.timeout");
            getLog().trace("testQueryTimeout4() - Retrieved hint " +
                "javax.persistence.query.timeout="
                + timeout);
            assertEquals(timeout, setTime);

            try {
                long startTime = System.currentTimeMillis();
                Object result = q.getSingleResult();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace(
                    "testQueryTimeout4() - NoHintSingle runTime msecs="
                    + runTime);
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
                assertNotNull("Verify we received a result.", result);
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout4() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Internal convenience method to execute SQL statements
     * 
     * @param em
     * @param sql
     * @param timeoutSecs
     * @param fail
     */
    private void exec(boolean ignoreExceptions, int timeoutSecs, String sql) {
        EntityManager em = null;
        Statement s = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Broker broker = JPAFacadeHelper.toBroker(em);
            Connection conn = (Connection) broker.getConnection();
            s = conn.createStatement();
            if (timeoutSecs > 0) {
                s.setQueryTimeout(timeoutSecs);
            }
            getLog().trace("execute(" + sql + ")");
            s.execute(sql);
        } catch (SQLException sqe) {
            if (!ignoreExceptions) {
                fail(sqe.toString());
            }
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Internal convenience method for getting the OpenJPA logger
     * 
     * @return
     */
    private Log getLog() {
        return emf.getConfiguration().getLog("Tests");
    }

    /**
     * Internal convenience method for checking that the given Exception matches
     * the expected type for a given DB platform.
     * 
     * @param test
     * @param e
     */
    private void checkException(String test, Exception e) {
        if (supportsQueryTimeoutException) {
            assertTrue("Expected QueryTimeoutException instead of " + e,
                matchesExpectedException(QueryTimeoutException.class, e));
        } else {
            assertTrue("Expected PersistenceException instead of " + e,
                matchesExpectedException(PersistenceException.class, e));
        }
        getLog().trace(test + " - Caught expected Exception = " + e);
    }

    /**
     * Internal convenience method for checking that the given Exception matches
     * the expected type.
     * 
     * @param expected
     * @param tested
     * @return
     */
    private boolean matchesExpectedException(Class<?> expected, 
        Exception tested) {
        assertNotNull(expected);
        boolean exMatched = false;
        if (tested != null) {
            Class<?> testExClass = tested.getClass();
            exMatched = expected.isAssignableFrom(testExClass);
        }
        return exMatched;
    }

    /**
     * This is the user-defined DB FUNCTION which is called from our queries to
     * sleep and cause timeouts, based on seconds.
     * 
     * @param secs
     * @param value
     * @return value
     * @throws SQLException
     */
    public static int delay(int secs, int value) throws SQLException {
        try {
            /*
            if (value >= 0) {
                System.out.println("  Native SQL called delay(secs=" + secs + 
                ",value=" + value + ")");
            } else {
                System.out.println("   Trigger called delay(secs=" + secs + 
                ",value=" + value + ")");
            }
            */
            Thread.sleep(secs * 1000);
        } catch (InterruptedException e) {
            // Ignore
        }
        return value;
    }

    public static void main(String[] args) {
    }
}
