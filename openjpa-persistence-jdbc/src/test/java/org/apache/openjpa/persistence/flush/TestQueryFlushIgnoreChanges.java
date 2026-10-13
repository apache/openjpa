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
package org.apache.openjpa.persistence.flush;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Asserts how <code>openjpa.IgnoreChanges=true</code> interacts with the flush mode.
 * <p>
 * <code>openjpa.IgnoreChanges</code> is a documented OpenJPA option that lets a query
 * run straight against the datastore without considering in-memory changes; the
 * reference guide recommends it together with the default
 * <code>openjpa.FlushBeforeQueries=true</code> as a performance setting. It therefore
 * has to keep working whether the flush mode is the inherited default or
 * <code>FlushModeType.AUTO</code> set on the query object: section 3.10.8 states the
 * same duty for both triggers, and the option is a vendor opt-out from it, not a
 * distinction the specification draws. Enabling it is therefore a deliberate, documented
 * departure from the visibility guarantee for SELECTs, and nothing about the flush mode
 * re-enables it.
 */
public class TestQueryFlushIgnoreChanges extends SQLListenerTestCase {

    private static final String ANY_UPDATE = "(?i).*UPDATE\\s+FLUSH_.*";
    private static final String UPDATE_CUSTOMER = "(?i).*UPDATE\\s+FLUSH_CUSTOMER.*";

    private static final String JPQL =
        "SELECT o FROM UnenhancedFlushOrder o WHERE o.customer.name = 'Michael Bouschen'";

    @Override
    public void setUp() {
        setUp(UnenhancedFlushSpouse.class,
              UnenhancedFlushCustomer.class,
              UnenhancedFlushOrder.class,
              UnenhancedFlushExpressOrder.class,
              "openjpa.RuntimeUnenhancedClasses", "supported",
              "openjpa.IgnoreChanges", "true",
              DROP_TABLES);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedFlushCustomer cust = new UnenhancedFlushCustomer("10", "Kate P. Hudson");
            em.persist(cust);
            em.persist(new UnenhancedFlushOrder("11", cust));
            em.getTransaction().commit();
        }
        resetSQL();
    }

    /**
     * With the flush mode inherited from the persistence context, IgnoreChanges keeps
     * its documented effect: the query runs against the datastore and nothing is flushed.
     */
    public void testIgnoreChangesSuppressesFlushForInheritedFlushMode() {
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(FlushModeType.AUTO, em.getFlushMode());
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            List<UnenhancedFlushOrder> orders =
                em.createQuery(JPQL, UnenhancedFlushOrder.class).getResultList();

            assertNotSQL(ANY_UPDATE);
            assertEquals(0, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A flush mode of AUTO set on the query object is the mode the query already has,
     * and IgnoreChanges opts out of the visibility guarantee for either trigger, so the
     * query still runs against the datastore.
     */
    public void testExplicitAutoOnQueryKeepsIgnoreChanges() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            List<UnenhancedFlushOrder> orders =
                em.createQuery(JPQL, UnenhancedFlushOrder.class)
                  .setFlushMode(FlushModeType.AUTO)
                  .getResultList();

            assertNotSQL(ANY_UPDATE);
            assertEquals(0, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * Setting a flush mode must not leave the query in a different state than it was
     * created in: AUTO followed by COMMIT has to behave exactly like COMMIT alone, and
     * in particular the query must still be executed against the datastore rather than
     * in memory over the whole extent.
     */
    public void testAutoThenCommitBehavesLikeCommitAlone() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            List<UnenhancedFlushOrder> orders =
                em.createQuery(JPQL, UnenhancedFlushOrder.class)
                  .setFlushMode(FlushModeType.AUTO)
                  .setFlushMode(FlushModeType.COMMIT)
                  .getResultList();

            assertNotSQL(ANY_UPDATE);
            assertEquals(0, orders.size());
            // the filter has to be pushed down, not evaluated in memory over the extent
            assertSQL("(?i).*SELECT.*FROM\\s+FLUSH_ORDER.*WHERE.*");
            em.getTransaction().rollback();
        }
    }

    /**
     * A query obtained through {@code createNamedQuery()} for a registration made with
     * {@code addNamedQuery()} must not lose the option either: the registration replays
     * the source query's flush mode, which is never unset.
     */
    public void testIgnoreChangesSurvivesNamedQueryRoundTrip() {
        emf.addNamedQuery("TestQueryFlushIgnoreChanges.byName",
            emf.createEntityManager().createQuery(JPQL, UnenhancedFlushOrder.class));
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            List<UnenhancedFlushOrder> orders = em.createNamedQuery(
                "TestQueryFlushIgnoreChanges.byName", UnenhancedFlushOrder.class)
                .getResultList();

            assertNotSQL(ANY_UPDATE);
            assertEquals(0, orders.size());
            em.getTransaction().rollback();
        }
    }
}
