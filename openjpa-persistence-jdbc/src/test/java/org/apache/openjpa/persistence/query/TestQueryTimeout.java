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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.PersistenceException;
import org.apache.openjpa.persistence.QueryTimeoutException;
import org.apache.openjpa.persistence.query.common.apps.QTimeout;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests the new query timeout hint support in the JPA 2.0 spec.
 * Query timeout scenarios being tested:
 *   1) By default, there is no timeout
 *   2) Setting timeout to 0 is same as no timeout (JDBC defined)
 *     2.1) using Map properties on createEMF (or PU properties)
 *     2.2) using the QueryHint annotation
 *     2.3) using setHint()
 *   3) Setting timeout to msecs < DELAY value causes new 
 *      javax.persistence.QueryTimeoutException for databases that do not
 *      cause a rollback or a PersistenceException if they do, when set by:
 *     3.1) using persistence.xml PU properties (or createEMF Map properties)
 *     3.2) using the QueryHint annotation
 *     3.3) calling setHint()
 * Query operations to validate through cross coverage of items #1-#3:
 *   a) getResultList()
 *   b) getSingleResult()
 *   c) executeUpdate()
 * Other behaviors to test for:
 *   4) Setting timeout to -1 should be treated as no timeout supplied
 *   5) Setting timeout to < -1 should throw an IllegalArgumentExpection
 *   6) Updates after EM.find()/findAll() are not affected by query timeout
 * Exception generation to test for:
 *   If the DB query timeout does not cause a transaction rollback, then a 
 *   QueryTimeoutException should be thrown.
 *     Applicable to:  unknown
 *   Else if the DB query timeout causes a transaction rollback, then a 
 *   PersistenceException should be thrown instead of a QTE.
 *     Applicable to:  Derby
 * 
 * @version $Rev$ $Date$
 */
public class TestQueryTimeout extends SQLListenerTestCase {

    private boolean skipTests = false;

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
        if (dict.supportsQueryTimeout) {
            if (!(dict instanceof DerbyDictionary)) {
                // FIXME drwoods - OPENJPA-964 - haven't determined what the
                // other DBs support
                // setQueryTimeout is not working with DB2 v9.5.3a on Windows
                getLog().info("FIXME - TestQueryTimeout tests are being " +
                    "skipped, as tests are not being run against a Derby DB.");
                skipTests = true;
            }
        } else {
            getLog().info("TestQueryTimeout tests are being skipped, " +
                "due to DB not supporting Query Timeouts.");
            skipTests = true;
        }
        if (skipTests)
            return;

        // create some initial entities
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

        // create delay function only on Derby, other DBs require manual setup
        if (dict instanceof DerbyDictionary) {
            getLog().trace("setUp() - creating DELAY function only for " +
                "Derby.  Other DBs require manual setup.");
            // remove existing function if it exists and recreate
            try {
                exec(true, 0, "DROP FUNCTION DELAY");
                exec(false, 0, "CREATE FUNCTION DELAY(SECONDS INTEGER, " + 
                    "VALUE INTEGER) RETURNS INTEGER PARAMETER STYLE JAVA " +
                    "NO SQL LANGUAGE JAVA EXTERNAL NAME " +
                    "'org.apache.openjpa.persistence." +
                    "query.TestQueryTimeout.delay'");
            } catch (SQLException sqe) {
                fail(sqe.toString());
            }
        }
        
        // create triggers on all DBs
        try {
            getLog().trace("setUp() - creating BEFORE UPDATE/INSERT " +
                "TRIGGERs for all DBs");
            exec(false, 0, "CREATE TRIGGER t1 NO CASCADE BEFORE UPDATE ON " +
                "qtimeout FOR EACH ROW MODE DB2SQL values DELAY(2,-1)");
            exec(false, 0, "CREATE TRIGGER t2 NO CASCADE BEFORE INSERT ON " +
                "qtimeout FOR EACH ROW MODE DB2SQL values DELAY(2,-2)");
            // Don't include a DELETE trigger, as it slows down the DROP_TABLES
            // cleanup between tests
            // exec(0, "CREATE TRIGGER t3 NO CASCADE BEFORE DELETE ON " +
            //     "qtimeout FOR EACH ROW MODE DB2SQL values DELAY(2,-3)");
        } catch (SQLException sqe) {
            if (dict instanceof DerbyDictionary) {
                // Always fail if we couldn't create triggers in Derby
                fail(sqe.toString());
            } else {
                // just disable tests for other DBs
                getLog().info("TestQueryTimeout tests are being skipped, " +
                    "due to DB delay() function missing and/or problems " +
                    "creating the required triggers.  DBs other than " +
                    "Derby require manual setup steps for these tests.");
                skipTests = true;
                return;
            }
        }
    }

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
        getLog().trace("testQueryTimeout1c() - No executeUpdate timeout");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createQuery("UPDATE QTimeout q SET q.stringField = " +
                ":strVal WHERE q.id = 1");
            q.setParameter("strVal", new String("updated"));
            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));
            try {
                long startTime = System.currentTimeMillis();
                em.getTransaction().begin();
                int count = q.executeUpdate();
                em.getTransaction().commit();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout1c() - executeUpdate runTime " + 
                    "msecs=" + runTime);
                assertTrue("Verify we received one result.", (count == 1));
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
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
     * Scenario being tested: 2.1.b) Explicit Map of properties to createEMF
     * with timeout=0 is treated the same as the default no query timeout.
     * Expected Results: The DELAY function is being called and the query
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout21b() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout21b() - Map(timeout=0)");
        OpenJPAEntityManagerFactory emf = null;
        OpenJPAEntityManager em = null;
        Integer setTime = new Integer(0);
        // create the Map to test overrides
        Map<String,String >props = new HashMap<String,String>();
        props.put("javax.persistence.query.timeout", "0");
        
        try {
            // create our EMF with our timeout property
            emf = OpenJPAPersistence.createEntityManagerFactory(
                "qtimeout-no-properties", "persistence3.xml", props);
            assertNotNull(emf);
            // verify Map properties updated the config
            OpenJPAConfiguration conf = emf.getConfiguration();
            assertNotNull(conf);
            assertEquals("Map provided query timeout", setTime.intValue(),
                conf.getQueryTimeout());
            // verify no default javax.persistence.query.timeout is supplied
            // as the Map properties are not passed through as hints
            em = emf.createEntityManager();
            assertNotNull(em);
            OpenJPAQuery q = em.createNamedQuery("NoHintSingle");
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));
            // verify internal config values were updated
            assertEquals("Map provided query timeout", setTime.intValue(),
                q.getFetchPlan().getQueryTimeout());
            
            try {
                long startTime = System.currentTimeMillis();
                Object result = q.getSingleResult();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout21b() - NoHintSingle runTime " + 
                    "msecs=" + runTime);
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
                assertNotNull("Verify we received a result.", result);
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout21b() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 2.2.a) Explicit annotated QueryHint of timeout=0
     * is treated the same as the default no timeout for queries.
     * Expected Results: The DELAY function is being called and the query
     * takes 6000+ msecs to complete.
     */
    public void testQueryTimeout22a() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout22a() - QueryHint=0");
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
            getLog().trace("testQueryTimeout22a() - Retrieved hint " + 
                "javax.persistence.query.timeout=" + timeout);
            assertEquals(timeout, new Integer(0));

            try {
                long startTime = System.currentTimeMillis();
                @SuppressWarnings("unchecked")
                List results = q.getResultList();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout22a() - Hint0msec runTime " +
                    "msecs=" + runTime);
                // Hack - Windows sometimes returns 5999 instead of 6000+
                assertTrue("Should have taken 6+ secs, but was msecs=" +
                    runTime, runTime > 5900);
                assertEquals("Verify we found 2 results.", 2, results.size());
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout22a() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 2.3.b) Explicit setHint of timeout=0 is treated
     * the same as the default no timeout for queries.
     * Expected Results: The DELAY function is being called and the query
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout23b() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(0);
        getLog().trace("testQueryTimeout23b() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value to 0 and verify it was set
            getLog().trace("testQueryTimeout23b() - Setting hint " + 
                "javax.persistence.query.timeout=" + setTime);
            q.setHint("javax.persistence.query.timeout", setTime);
            hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = (Integer) hints.get(
                "javax.persistence.query.timeout");
            getLog().trace("testQueryTimeout23b() - Retrieved hint " +
                "javax.persistence.query.timeout=" + timeout);
            assertEquals(timeout, setTime);

            try {
                long startTime = System.currentTimeMillis();
                Object result = q.getSingleResult();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout23b() - NoHintSingle runTime " + 
                    "msecs=" + runTime);
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
                assertNotNull("Verify we received a result.", result);
            } catch (Exception e) {
                fail("Unexpected testQueryTimeout23b() exception = " + e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 3.1.c) Explicit persistence.xml provided PU
     * property of timeout=1000 msecs will cause the query to timeout.
     * Expected Results: The DELAY function is being called and the query
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout31c() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout31c() - PU(timeout=1000), " +
            "executeUpdate timeout");
        OpenJPAEntityManagerFactory emf = null;
        OpenJPAEntityManager em = null;
        Integer setTime = new Integer(1000);

        try {
            // create our EMF with our PU set timeout property
            emf = OpenJPAPersistence.createEntityManagerFactory(
                "qtimeout-1000msecs", "persistence3.xml");
            assertNotNull(emf);
            // verify PU properties updated the config
            OpenJPAConfiguration conf = emf.getConfiguration();
            assertNotNull(conf);
            assertEquals("PU provided query timeout", setTime.intValue(),
                conf.getQueryTimeout());
            // create EM and Query
            em = emf.createEntityManager();
            assertNotNull(em);
            OpenJPAQuery q = em.createNativeQuery("UPDATE QTimeout SET " +
                "stringField = ? WHERE mod(DELAY(2,id),2)=0");
            q.setParameter(1, new String("updated"));
            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));
            // verify internal config values were updated
            assertEquals("PU provided query timeout", setTime.intValue(),
                q.getFetchPlan().getQueryTimeout());

            // verify queryTimeout on EM find operations
            try {
                long startTime = System.currentTimeMillis();
                em.getTransaction().begin();
                int count = q.executeUpdate();
                em.getTransaction().commit();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout31c() - executeUpdate " + 
                    "runTime msecs=" + runTime);
                fail("QueryTimeout for executeUpdate failed to cause an " + 
                    "Exception in testQueryTimeout31c(" + setTime +
                    " mscs), runTime msecs=" + runTime);
            } catch (Exception e) {
                // expected - Should cause a QueryTimeoutException for Derby
                checkException("testQueryTimeout31c()", e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 3.2.a) Explicit annotated QueryHint of
     * timeout=1000 msecs will override the PU and Map provided timeouts
     * and cause the query to timeout.
     * Expected Results: QueryTimeoutException or PersistenceException
     */
    public void testQueryTimeout32a() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout32a() - PU(1000), Map(0), " +
            "QueryHint(1000)");
        OpenJPAEntityManagerFactory emf = null;
        OpenJPAEntityManager em = null;
        Integer setTime = new Integer(0);
        // create the Map to test overrides
        Map<String,String> props = new HashMap<String,String>();
        props.put("javax.persistence.query.timeout", "0");
        
        try {
            // create our EMF with our PU set timeout property
            emf = OpenJPAPersistence.createEntityManagerFactory(
                "qtimeout-1000msecs", "persistence3.xml", props);
            assertNotNull(emf);
            // verify Map properties overrode the PU properties in config
            OpenJPAConfiguration conf = emf.getConfiguration();
            assertNotNull(conf);
            assertEquals("Map provided query timeout", setTime.intValue(),
                conf.getQueryTimeout());
            // create EM and named query
            em = emf.createEntityManager();
            assertNotNull(em);
            OpenJPAQuery q = em.createNamedQuery("Hint1000msec");
            setTime = 1000;
            // verify javax.persistence.query.timeout hint via annotation set
            Map<String, Object> hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = new Integer((String) hints.get(
                "javax.persistence.query.timeout"));
            getLog().trace(
                "testQueryTimeout32a() - Found javax.persistence.query.timeout="
                + timeout);
            assertTrue("Expected to find a javax.persistence.query.timeout="
                + setTime, (timeout.intValue() == setTime.intValue()));
            // verify internal config values were updated
            assertEquals("QueryHint provided query timeout", setTime.intValue(),
                q.getFetchPlan().getQueryTimeout());

            try {
                long startTime = System.currentTimeMillis();
                @SuppressWarnings( { "unchecked", "unused" })
                List results = q.getResultList();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace(
                    "testQueryTimeout32a() - Hint1000msec runTime msecs="
                    + runTime);
                //assertEquals("Should never get valid results due to the " + 
                // "timeout.", 2, results.size());
                fail("QueryTimeout annotation failed to cause an Exception " +
                    "in testQueryTimeout32a(" + setTime +
                    " msecs), runTime msecs=" + runTime);
            } catch (Exception e) {
                // expected
                checkException("testQueryTimeout32a()", e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 3.3.b) Explicit setHint of timeout to 1000 msecs
     * will cause the query to timeout.
     * Expected Results: QueryTimeoutException or PersistenceException
     */
    public void testQueryTimeout33b() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(1000);
        getLog().trace("testQueryTimeout33b() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value and verify it was set
            getLog().trace("testQueryTimeout33b() - Setting hint " +
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
                    "testQueryTimeout33b() - NoHintSingle runTime msecs="
                    + runTime);
                //assertNull("Should never get valid result due to the timeout."
                //    , result);
                fail("QueryTimeout annotation failed to cause an Exception " + 
                    "in testQueryTimeout33b(" + setTime +
                    " mscs), runTime msecs=" + runTime);
            } catch (Exception e) {
                // expected
                checkException("testQueryTimeout33b()", e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 3.3.c) Explicit setHint of timeout to 1000 msecs
     * will cause the PU provided timeout=0 value to be overridden and the
     * executeUpdate to timeout.
     * Expected Results: QueryTimeoutException (Derby) or PersistenceException
     */
    public void testQueryTimeout33c() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout33c() - PU(timeout=0), setHint(1000)," +
            " executeUpdate timeout");
        OpenJPAEntityManagerFactory emf = null;
        OpenJPAEntityManager em = null;
        Integer setTime = new Integer(0);        

        try {
            // create our EMF with our PU set timeout property
            emf = OpenJPAPersistence.createEntityManagerFactory(
                "qtimeout-0msecs", "persistence3.xml");
            assertNotNull(emf);
            // verify PU properties updated the config
            OpenJPAConfiguration conf = emf.getConfiguration();
            assertNotNull(conf);
            assertEquals("PU provided no query timeout", setTime.intValue(),
                conf.getQueryTimeout());
            // create EM and Query
            em = emf.createEntityManager();
            assertNotNull(em);
            // Following fails to cause a SQLException, but takes 2+ secs
            // Query q = em.createQuery("UPDATE QTimeout q SET q.stringField = 
            //     + ":strVal WHERE q.id > 0");
            // q.setParameter("strVal", new String("updated"));
            // Following fails to cause a SQLException, but takes 2+ secs
            // Query q = em.createNativeQuery("INSERT INTO QTimeout (id, " +
            //    "stringField) VALUES (?,?)");
            // q.setParameter(1, 99);
            // q.setParameter(2, new String("inserted"));
            OpenJPAQuery q = em.createNativeQuery("UPDATE QTimeout SET " +
                "stringField = ? WHERE mod(DELAY(2,id),2)=0");
            q.setParameter(1, new String("updated"));
            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));            
            // update the query timeout value and verify it was set
            setTime = 1000;
            getLog().trace("testQueryTimeout33c() - Setting hint " +
                "javax.persistence.query.timeout=" + setTime);
            q.setHint("javax.persistence.query.timeout", setTime);
            hints = q.getHints();
            assertTrue(hints.containsKey("javax.persistence.query.timeout"));
            Integer timeout = (Integer) hints.get(
                "javax.persistence.query.timeout");
            assertEquals(timeout, setTime);
            // verify internal config values were updated
            assertEquals("PU provided query timeout", setTime.intValue(),
                q.getFetchPlan().getQueryTimeout());
            
            try {
                long startTime = System.currentTimeMillis();
                em.getTransaction().begin();
                int count = q.executeUpdate();
                em.getTransaction().commit();
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;
                getLog().trace("testQueryTimeout33c() - executeUpdate " + 
                    "runTime msecs=" + runTime);
                fail("QueryTimeout for executeUpdate failed to cause an " + 
                    "Exception in testQueryTimeout33c(" + setTime +
                    " mscs), runTime msecs=" + runTime);
            } catch (Exception e) {
                // expected - Should cause a QueryTimeoutException for Derby
                checkException("testQueryTimeout33c()", e);
            }
        } finally {
            if ((em != null) && em.isOpen()) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 4) Timeout of -1 should be treated the same
     * as the default no timeout scenario.
     * Expected Results: The DELAY function is being called and the query
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout4() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(-1);
        getLog().trace("testQueryTimeout4() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value to -1 and verify it was set
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
     * Scenario being tested: 5) Setting timeout to < -1 should throw an
     * IllegalArgumentExpection
     * Expected Results: IllegalArgumentException
     */
    public void testQueryTimeout5() {
        if (skipTests) {
            return;
        }
        Integer setTime = new Integer(-2000);
        getLog().trace("testQueryTimeout5() - setHint(" + setTime + ")");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertNotNull(em);
            Query q = em.createNamedQuery("NoHintSingle");

            // verify no default javax.persistence.query.timeout is supplied
            Map<String, Object> hints = q.getHints();
            assertFalse(hints.containsKey("javax.persistence.query.timeout"));

            // update the timeout value to -2000 and verify it was set
            getLog().trace("testQueryTimeout5() - Setting hint " +
                "javax.persistence.query.timeout="
                + setTime);
            q.setHint("javax.persistence.query.timeout", setTime);
            fail("Expected testQueryTimeout5() to throw a " + 
                "IllegalArgumentException");
        } catch (Exception e) {
            // expected - setHint(-2000) should cause an IllegalArgumentException
            checkException("testQueryTimeout5()", e, 
                IllegalArgumentException.class, "Invalid value" );
        } finally {
            if ((em != null) && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Scenario being tested: 6) PU Query timeout hints do not affect EM
     * operations like updating Entities returned by EM.find()/findAll()
     * Expected Results: The DELAY function is being called and the update
     * takes 2000+ msecs to complete.
     */
    public void testQueryTimeout6() {
        if (skipTests) {
            return;
        }
        getLog().trace("testQueryTimeout6() - No EM.find() update timeout");
        OpenJPAEntityManagerFactory emf = null;
        OpenJPAEntityManager em = null;
        Integer setTime = new Integer(1000);
        
        try {
            // create our EMF with our PU set timeout property
            emf = OpenJPAPersistence.createEntityManagerFactory(
                "qtimeout-1000msecs", "persistence3.xml");
            assertNotNull(emf);
            // verify PU properties updated the config
            OpenJPAConfiguration conf = emf.getConfiguration();
            assertNotNull(conf);
            assertEquals("PU provided timeout", setTime.intValue(),
                conf.getQueryTimeout());
            // create EM
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
                getLog().trace("testQueryTimeout6() - EM find/update runTime" +
                    " msecs=" + runTime);
                // Hack - Windows sometimes returns 1999 instead of 2000+
                assertTrue("Should have taken 2+ secs, but was msecs=" +
                    runTime, runTime > 1900);
                em.clear();
                qt = em.find(QTimeout.class, new Integer(1));
                assertEquals("Verify the entity was updated.",
                    qt.getStringField(), "updated");
            } catch (Exception e) {
                // setting a timeout property via PU or Map shouldn't cause a
                // timeout exception
                fail("Unexpected testQueryTimeout6() exception = " + e);
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
    private void exec(boolean ignoreExceptions, int timeoutSecs, String sql)
        throws SQLException {
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
                // fail(sqe.toString());
                throw sqe;
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
        String eStr = new String("query statement timeout");
        // no easy way to determine exact Exception type for all DBs
        assertTrue(test + " - UNEXPECTED Exception = " + e,
            matchesExpectedException(QueryTimeoutException.class, e, eStr) ||
            matchesExpectedException(PersistenceException.class, e, eStr));
        getLog().trace(test + " - Caught expected Exception = " + e);
    }

    /**
     * Internal convenience method for checking that the given Exception matches
     * the expected type.
     * 
     * @param test case name
     * @param tested exception type
     * @param expected exception type
     * @param eStr an optional substring to match in the exception text
     */
    private void checkException(String test, Exception tested, Class expected, 
        String eStr) {
        assertTrue(test + " - UNEXPECTED Exception = " + tested,
            matchesExpectedException(expected, tested, eStr));
        getLog().trace(test + " - Caught expected Exception = " + tested);
    }

    /**
     * Internal convenience method for checking that the given Exception matches
     * the expected type.
     * 
     * @param expected
     * @param tested
     * @param eStr an optional substring to match in the exception text
     * @return true if the exception matched, false otherwise
     */
    private boolean matchesExpectedException(Class<?> expected, 
        Exception tested, String eStr) {
        assertNotNull(expected);
        boolean exMatched = false;
        if (tested != null) {
            Class<?> testExClass = tested.getClass();
            exMatched = expected.isAssignableFrom(testExClass);
            if (exMatched && eStr != null) {
                // make sure it is our expected exception text from
                // localizer.properties
                exMatched = (tested.getMessage().indexOf(eStr) != -1);
            }
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
