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
package org.apache.openjpa.persistence.criteria;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

import junit.framework.TestResult;

/**
 * Affirms that a criteria test whose {@code setUp()} fails after it has acquired a JDBC
 * connection does not take the rest of the suite down with it.
 * <p>
 * {@link junit.framework.TestCase#runBare()} calls {@code setUp()} outside its try/finally,
 * so before OPENJPA-3016 a failure there skipped {@code tearDown()}: the
 * {@link EntityManager} opened by {@link CriteriaTest#setUp()} stayed open, kept the
 * connection its transaction had retained, and - because the EntityManagerFactory is static
 * and shared by every criteria test class - the leaked connections accumulated in one pool.
 * The pool holds ten (MaxTotal=10, MaxWait=10s in the test profiles), so a dozen such
 * failures emptied it and every later test in the same JVM failed with "Cannot get a
 * connection, pool error Timeout waiting for idle object".
 * <p>
 * The probes in {@link SetUpFailureProbes} therefore let one factory - and so one pool -
 * survive all of their failures, which is what the real criteria tests do to one another.
 */
public class TestSetUpFailureReleasesConnection extends CriteriaTest {

    /**
     * More failures than the pool has connections, so that one leaked connection per failure
     * is enough to empty it.
     */
    private static final int FAILURES = 14;

    /**
     * Runs more failing probes than the pool has connections against one surviving factory,
     * then affirms that the pool still hands out a connection. Without the fix the first probe
     * already leaves its EntityManager open, and the pool runs dry part way through the loop.
     */
    public void testSetUpFailureDoesNotExhaustTheConnectionPool() {
        OpenJPAEntityManagerFactorySPI factory = getEntityManagerFactory();
        for (int i = 0; i < FAILURES; i++) {
            SetUpFailureProbes.FailingSetUp.opened = null;
            runProbe(new SetUpFailureProbes.FailingSetUp(), "testNeverRuns");
            assertNotNull("probe " + i + " never opened an EntityManager", SetUpFailureProbes.FailingSetUp.opened);
            assertFalse("probe " + i + " left the EntityManager its setUp() opened behind, "
                + "so the connection it retained never went back to the pool",
                SetUpFailureProbes.FailingSetUp.opened.isOpen());
            assertSame("the probes have to share one factory, and so one pool",
                factory, getEntityManagerFactory());
        }

        EntityManager fresh = factory.createEntityManager();
        try {
            assertNotNull("after " + FAILURES + " failed setUp()s the connection pool must "
                + "still hand out a connection",
                fresh.createQuery("SELECT COUNT(c) FROM CompUser c").getSingleResult());
        } finally {
            fresh.close();
        }
    }

    /**
     * Affirms that a failure releasing the EntityManager does not skip the factory, which
     * would leak the whole pool.
     */
    public void testEntityManagerCloseFailureStillClosesTheFactory() {
        releaseOwnEntityManager();
        SetUpFailureProbes.FailingEntityManagerClose.factory = null;

        runProbe(new SetUpFailureProbes.FailingEntityManagerClose(), "testPasses");

        assertNotNull(SetUpFailureProbes.FailingEntityManagerClose.factory);
        assertFalse("tearDown() has to close the EntityManagerFactory even when closing the "
            + "EntityManager threw, or the whole connection pool leaks",
            SetUpFailureProbes.FailingEntityManagerClose.factory.isOpen());
        assertNull("the closed factory must not be handed to the next test",
            getEntityManagerFactory());
    }

    /**
     * The same as {@link #testSetUpFailureDoesNotExhaustTheConnectionPool()} for the
     * EmbeddableDomainTestCase hierarchy, which keeps a static factory of its own.
     */
    public void testEmbeddableSetUpFailureReleasesTheEntityManager() {
        releaseOwnEntityManager();
        SetUpFailureProbes.FailingEmbeddableSetUp.opened = null;

        runProbe(new SetUpFailureProbes.FailingEmbeddableSetUp(), "testNeverRuns");

        assertNotNull("the probe never opened an EntityManager", SetUpFailureProbes.FailingEmbeddableSetUp.opened);
        assertFalse("the probe left the EntityManager its setUp() opened behind, so the "
            + "connection it retained never went back to the pool",
            SetUpFailureProbes.FailingEmbeddableSetUp.opened.isOpen());
        assertNull("the EmbeddableDomainTestCase factory has to be closed and cleared as well",
            EmbeddableDomainTestCase.emf);
    }

    /**
     * Runs the probe and affirms that it reported the one deliberate error it was written to
     * report, so that none of the assertions around it can pass on an unrelated failure.
     */
    private void runProbe(AbstractCriteriaTestCase probe, String method) {
        probe.setName(method);
        TestResult result = new TestResult();
        probe.run(result);
        assertEquals("the probe was expected to fail in setUp() or tearDown(), not in the test",
            0, result.failureCount());
        assertEquals("the probe was expected to report exactly one error", 1, result.errorCount());
        Throwable thrown = result.errors().nextElement().thrownException();
        assertTrue("the probe must have failed on its own deliberate error, not on " + thrown,
            thrown instanceof IllegalStateException
                && String.valueOf(thrown.getMessage()).startsWith("deliberate "));
    }

    /**
     * Releases the EntityManager this test's own {@code setUp()} opened, before a probe closes
     * the factory underneath it.
     */
    private void releaseOwnEntityManager() {
        try {
            closeEntityManager();
        } finally {
            setEntityManager(null);
        }
    }
}
