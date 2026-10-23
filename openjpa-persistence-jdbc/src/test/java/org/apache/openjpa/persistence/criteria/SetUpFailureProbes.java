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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

/**
 * Criteria tests that fail on purpose, driven by
 * {@link TestSetUpFailureReleasesConnection}.
 * <p>
 * They live outside a {@code Test*} compilation unit on purpose: surefire would otherwise
 * pick the nested classes up as tests of their own and report their deliberate failures as
 * failures of the build.
 */
public final class SetUpFailureProbes {

    private SetUpFailureProbes() {
    }

    /**
     * A test whose {@code setUp()} starts a transaction and writes in it - which makes the
     * store manager hold on to a JDBC connection for the rest of the transaction - and then
     * fails, exactly as TestMultiselect does when its own {@code setUp()} clears the tables
     * and hits a schema or data problem after {@code super.setUp()}.
     */
    public static class FailingSetUp extends CriteriaTest {

        /** The EntityManager the last probe opened, so the caller can affirm it was closed. */
        static EntityManager opened;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            opened = getEntityManager();
            opened.getTransaction().begin();
            opened.createQuery("DELETE FROM CompUser c WHERE c.userid = -1").executeUpdate();
            throw new IllegalStateException("deliberate setUp failure after taking a connection");
        }

        /**
         * Releases only this probe's EntityManager and leaves the shared factory - and so its
         * connection pool - open, so that a connection a probe fails to release stays missing
         * from the pool for the probes that follow it.
         */
        @Override
        public void tearDown() throws Exception {
            try {
                closeEntityManager();
            } finally {
                setEntityManager(null);
                setCriteriaBuilder(null);
            }
        }

        public void testNeverRuns() {
            fail("setUp() must have failed before this ran");
        }
    }

    /**
     * A test whose {@code setUp()} succeeds but whose EntityManager throws on {@code close()},
     * to pin the ordering {@link AbstractCriteriaTestCase#releaseResources()} guarantees: the
     * factory, and with it the pool, has to be closed even when releasing the EntityManager
     * fails.
     */
    public static class FailingEntityManagerClose extends CriteriaTest {

        /** The factory in use while the probe ran, so the caller can affirm it was closed. */
        static OpenJPAEntityManagerFactorySPI factory;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            factory = getEntityManagerFactory();
            final EntityManager delegate = getEntityManager();
            setEntityManager((EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        delegate.close();
                        throw new IllegalStateException("deliberate failure closing the EntityManager");
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }));
        }

        public void testPasses() {
            assertNotNull(getEntityManagerFactory());
        }
    }

    /**
     * The same failure in the second criteria base class: {@link EmbeddableDomainTestCase} has
     * its own static factory and its own {@code tearDown()}, so the guarantee
     * {@link AbstractCriteriaTestCase#runBare()} relies on has to hold there too.
     */
    public static class FailingEmbeddableSetUp extends EmbeddableDomainTestCase {

        /** The EntityManager the probe opened, so the caller can affirm it was closed. */
        static EntityManager opened;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            opened = getEntityManager();
            opened.getTransaction().begin();
            opened.createQuery("DELETE FROM Division d WHERE d.id = -1").executeUpdate();
            throw new IllegalStateException("deliberate setUp failure after taking a connection");
        }

        public void testNeverRuns() {
            fail("setUp() must have failed before this ran");
        }
    }
}
