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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;
import java.util.function.Predicate;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.opentest4j.AssertionFailedError;

/**
 * Affirms that a criteria test whose {@code setUp()} fails after it has acquired a JDBC
 * connection does not take the rest of the suite down with it.
 * <p>
 * {@link Assertions#runBare()} calls {@code setUp()} outside its try/finally,
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
    @Test
    public void testSetUpFailureDoesNotExhaustTheConnectionPool() {
        OpenJPAEntityManagerFactorySPI factory = getEntityManagerFactory();
        for (int i = 0; i < FAILURES; i++) {
            SetUpFailureProbes.FailingSetUp.opened = null;
            runProbe(SetUpFailureProbes.FailingSetUp.class, "testNeverRuns");
            assertNotNull(SetUpFailureProbes.FailingSetUp.opened, "probe " + i + " never opened an EntityManager");
            assertFalse(SetUpFailureProbes.FailingSetUp.opened.isOpen(),
                "probe " + i + " left the EntityManager its setUp() opened behind, "
                + "so the connection it retained never went back to the pool");
            assertSame(factory,
                getEntityManagerFactory(), "the probes have to share one factory, and so one pool");
        }

        EntityManager fresh = factory.createEntityManager();
        try {
            assertNotNull(fresh.createQuery("SELECT COUNT(c) FROM CompUser c").getSingleResult(),
                "after " + FAILURES + " failed setUp()s the connection pool must "
                + "still hand out a connection");
        } finally {
            fresh.close();
        }
    }

    /**
     * Affirms that a failure releasing the EntityManager does not skip the factory, which
     * would leak the whole pool.
     */
    @Test
    public void testEntityManagerCloseFailureStillClosesTheFactory() {
        releaseOwnEntityManager();
        SetUpFailureProbes.FailingEntityManagerClose.factory = null;

        runProbe(SetUpFailureProbes.FailingEntityManagerClose.class, "testPasses");

        assertNotNull(SetUpFailureProbes.FailingEntityManagerClose.factory);
        assertFalse(SetUpFailureProbes.FailingEntityManagerClose.factory.isOpen(),
            "tearDown() has to close the EntityManagerFactory even when closing the "
            + "EntityManager threw, or the whole connection pool leaks");
        assertNull(getEntityManagerFactory(),
            "the closed factory must not be handed to the next test");
    }

    /**
     * The same as {@link #testSetUpFailureDoesNotExhaustTheConnectionPool()} for the
     * EmbeddableDomainTestCase hierarchy, which keeps a static factory of its own.
     */
    @Test
    public void testEmbeddableSetUpFailureReleasesTheEntityManager() {
        releaseOwnEntityManager();
        SetUpFailureProbes.FailingEmbeddableSetUp.opened = null;

        runProbe(SetUpFailureProbes.FailingEmbeddableSetUp.class, "testNeverRuns");

        assertNotNull(SetUpFailureProbes.FailingEmbeddableSetUp.opened, "the probe never opened an EntityManager");
        assertFalse(SetUpFailureProbes.FailingEmbeddableSetUp.opened.isOpen(),
            "the probe left the EntityManager its setUp() opened behind, so the "
            + "connection it retained never went back to the pool");
        assertNull(EmbeddableDomainTestCase.emf,
            "the EmbeddableDomainTestCase factory has to be closed and cleared as well");
    }

    /**
     * Runs the probe and affirms that it reported the one deliberate error it was written to
     * report, so that none of the assertions around it can pass on an unrelated failure.
     */
    private void runProbe(Class<? extends AbstractCriteriaTestCase> probe, String method) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(probe))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);

        launcher.execute(request);

        List<TestExecutionSummary.Failure> all = listener.getSummary().getFailures();
        List<TestExecutionSummary.Failure> errors = all.stream().filter(Predicate.not(AssertionFailedError.class::isInstance)).toList();
        int failuresCount = all.size() - errors.size();

        assertEquals(0, failuresCount, "the probe was expected to fail in setUp() or tearDown(), not in the test");
        assertEquals(1, errors.size(), "the probe was expected to report exactly one error");
        Throwable thrown = errors.get(0).getException();
        assertTrue(thrown instanceof IllegalStateException
                && String.valueOf(thrown.getMessage()).startsWith("deliberate "),
            "the probe must have failed on its own deliberate error, not on " + thrown);
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
