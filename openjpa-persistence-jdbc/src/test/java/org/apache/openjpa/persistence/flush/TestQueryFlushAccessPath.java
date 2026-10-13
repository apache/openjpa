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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Asserts which dirty instances make a <code>FlushModeType.AUTO</code> query flush.
 * <p>
 * Section 3.10.8 of the specification scopes the provider's duty to updates
 * "which could potentially affect the result of the query", and lets the provider
 * discharge it "by flushing those entities to the database or by some other means".
 * OpenJPA's "other means" is the query access path: only a dirty instance of a type
 * the query actually reads can affect its result, so only such an instance forces a
 * flush. The assertions below are on emitted SQL, not on results, because a
 * full-context flush returns the same results while costing an extra round trip.
 */
public class TestQueryFlushAccessPath extends SQLListenerTestCase {

    private static final String ANY_UPDATE = "(?i).*UPDATE\\s+FLUSH_.*";
    private static final String UPDATE_SPOUSE = "(?i).*UPDATE\\s+FLUSH_SPOUSE.*";
    private static final String UPDATE_CUSTOMER = "(?i).*UPDATE\\s+FLUSH_CUSTOMER.*";
    private static final String UPDATE_ORDER = "(?i).*UPDATE\\s+FLUSH_ORDER.*";
    private static final String INSERT_ORDER = "(?i).*INSERT\\s+INTO\\s+FLUSH_ORDER.*";
    private static final String DELETE_ORDER = "(?i).*DELETE\\s+FROM\\s+FLUSH_ORDER.*";

    @Override
    public void setUp() {
        setUp(UnenhancedFlushSpouse.class,
              UnenhancedFlushCustomer.class,
              UnenhancedFlushOrder.class,
              UnenhancedFlushExpressOrder.class,
              "openjpa.RuntimeUnenhancedClasses", "supported",
              DROP_TABLES);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedFlushCustomer cust = new UnenhancedFlushCustomer("10", "Kate P. Hudson");
            em.persist(cust);
            UnenhancedFlushSpouse spouse = new UnenhancedFlushSpouse("4", "Thomas", "Mullen");
            spouse.setCustomer(cust);
            em.persist(spouse);
            cust.setSpouse(spouse);
            em.persist(new UnenhancedFlushOrder("11", cust));
            UnenhancedFlushExpressOrder express = new UnenhancedFlushExpressOrder("12", cust);
            express.setCarrier("DHL");
            em.persist(express);
            em.getTransaction().commit();
        }
        resetSQL();
    }

    /**
     * A dirty instance of a type outside the query's access path cannot affect the
     * query result, so it must not force a flush.
     */
    public void testDirtyTypeOutsideAccessPathDoesNotFlush() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushSpouse.class, "4").setLastName("Miller");
            resetSQL();

            List<UnenhancedFlushOrder> orders = em.createQuery(
                "SELECT o FROM UnenhancedFlushOrder o WHERE o.totalPrice = 0",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertEquals(2, orders.size());
            assertNotSQL(ANY_UPDATE);
            em.getTransaction().rollback();
        }
    }

    /**
     * A dirty instance of the queried type itself must be visible to the query.
     */
    public void testDirtyCandidateTypeFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushOrder.class, "11").setTotalPrice(17d);
            resetSQL();

            List<UnenhancedFlushOrder> orders = em.createQuery(
                "SELECT o FROM UnenhancedFlushOrder o WHERE o.totalPrice = 17",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_ORDER);
            assertEquals(1, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A dirty instance of a subclass of a type in the access path must be visible:
     * the query reads the subclass rows too.
     */
    public void testDirtySubclassOfCandidateTypeFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushExpressOrder.class, "12").setTotalPrice(23d);
            resetSQL();

            List<UnenhancedFlushOrder> orders = em.createQuery(
                "SELECT o FROM UnenhancedFlushOrder o WHERE o.totalPrice = 23",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_ORDER);
            assertEquals(1, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A dirty instance of a type reached only by JPQL navigation is in the access
     * path, so it must be visible to the query.
     */
    public void testDirtyNavigatedTypeFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushSpouse.class, "4").setLastName("Miller");
            resetSQL();

            List<UnenhancedFlushOrder> orders = em.createQuery(
                "SELECT o FROM UnenhancedFlushOrder o "
                + "WHERE o.customer.spouse.lastName = 'Miller'",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_SPOUSE);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * The same navigation expressed with the Criteria API: an implicit join through
     * {@code get()} must contribute to the access path exactly as the JPQL path does.
     */
    public void testDirtyNavigatedTypeFlushesForCriteriaQuery() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushSpouse.class, "4").setLastName("Miller");
            resetSQL();

            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<UnenhancedFlushOrder> cq = cb.createQuery(UnenhancedFlushOrder.class);
            Root<UnenhancedFlushOrder> order = cq.from(UnenhancedFlushOrder.class);
            cq.where(cb.equal(order.get("customer").get("spouse").get("lastName"), "Miller"));
            List<UnenhancedFlushOrder> orders = em.createQuery(cq)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_SPOUSE);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A dirty instance of a type that appears only in a subquery is still read by the
     * query, so it must be visible.
     */
    public void testDirtyTypeInSubqueryFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            List<UnenhancedFlushOrder> orders = em.createQuery(
                "SELECT o FROM UnenhancedFlushOrder o WHERE o.customer.id IN "
                + "(SELECT c.id FROM UnenhancedFlushCustomer c "
                + "WHERE c.name = 'Michael Bouschen')",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_CUSTOMER);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }


    /**
     * A collection-valued path used without a join variable - <code>SIZE()</code> -
     * reads the element type, so a newly persisted element must be visible. The
     * declared type of the field is Collection, which has no metadata of its own, so
     * only the element type registered for the field puts Order into the access path.
     */
    public void testDirtyElementOfCollectionPathInSizeFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            UnenhancedFlushCustomer cust = em.find(UnenhancedFlushCustomer.class, "10");
            em.persist(new UnenhancedFlushOrder("13", cust));
            resetSQL();

            List<UnenhancedFlushCustomer> customers = em.createQuery(
                "SELECT c FROM UnenhancedFlushCustomer c WHERE SIZE(c.orders) = 3",
                UnenhancedFlushCustomer.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(INSERT_ORDER);
            assertEquals(1, customers.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * The same for <code>IS EMPTY</code>: a removed element changes the result, so the
     * delete has to be flushed first.
     */
    public void testDirtyElementOfCollectionPathInIsEmptyFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.remove(em.find(UnenhancedFlushOrder.class, "11"));
            em.remove(em.find(UnenhancedFlushExpressOrder.class, "12"));
            resetSQL();

            List<UnenhancedFlushCustomer> customers = em.createQuery(
                "SELECT c FROM UnenhancedFlushCustomer c WHERE c.orders IS EMPTY",
                UnenhancedFlushCustomer.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(DELETE_ORDER);
            assertEquals(1, customers.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A join of a join reads its type just as a join of the root does, so the join walk
     * has to follow the whole join tree.
     */
    public void testDirtyTypeBehindJoinOfJoinFlushesForCriteriaQuery() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushSpouse.class, "4").setLastName("Miller");
            resetSQL();

            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<UnenhancedFlushOrder> cq = cb.createQuery(UnenhancedFlushOrder.class);
            Root<UnenhancedFlushOrder> order = cq.from(UnenhancedFlushOrder.class);
            Join<Object,Object> spouse = order.join("customer").join("spouse");
            cq.where(cb.equal(spouse.get("lastName"), "Miller"));
            List<UnenhancedFlushOrder> orders = em.createQuery(cq)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_SPOUSE);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A fetch join reads the fetched type, so it belongs in the access path too.
     */
    public void testDirtyTypeBehindFetchJoinFlushesForCriteriaQuery() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<UnenhancedFlushOrder> cq = cb.createQuery(UnenhancedFlushOrder.class);
            Root<UnenhancedFlushOrder> order = cq.from(UnenhancedFlushOrder.class);
            order.fetch("customer");
            cq.where(cb.equal(order.get("totalPrice"), 0d));
            List<UnenhancedFlushOrder> orders = em.createQuery(cq)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_CUSTOMER);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A type read only inside a Criteria subquery must be visible: without it the
     * IN-subquery misses the unflushed name and the query returns nothing.
     */
    public void testDirtyTypeInCriteriaSubqueryFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<UnenhancedFlushOrder> cq = cb.createQuery(UnenhancedFlushOrder.class);
            Root<UnenhancedFlushOrder> order = cq.from(UnenhancedFlushOrder.class);
            Subquery<String> sq = cq.subquery(String.class);
            Root<UnenhancedFlushCustomer> cust = sq.from(UnenhancedFlushCustomer.class);
            sq.select(cust.<String>get("id"))
              .where(cb.equal(cust.get("name"), "Michael Bouschen"));
            cq.where(cb.in(order.get("customer").get("id")).value(sq));
            List<UnenhancedFlushOrder> orders = em.createQuery(cq)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_CUSTOMER);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * A join made on a <em>correlated</em> join is reachable neither from the outer
     * query's roots nor from the subquery's own roots, so the join walk has to start at
     * the correlated joins of the enclosing subquery as well.
     */
    public void testDirtyTypeBehindCorrelatedJoinFlushes() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushSpouse.class, "4").setLastName("Miller");
            resetSQL();

            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<UnenhancedFlushOrder> cq = cb.createQuery(UnenhancedFlushOrder.class);
            Root<UnenhancedFlushOrder> order = cq.from(UnenhancedFlushOrder.class);
            Join<Object,Object> cust = order.join("customer");
            Subquery<String> sq = cq.subquery(String.class);
            Join<Object,Object> spouse = sq.correlate(cust).join("spouse");
            sq.select(spouse.<String>get("id"))
              .where(cb.equal(spouse.get("lastName"), "Miller"));
            cq.where(cb.exists(sq));
            List<UnenhancedFlushOrder> orders = em.createQuery(cq)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertSQL(UPDATE_SPOUSE);
            assertEquals(2, orders.size());
            em.getTransaction().rollback();
        }
    }

    /**
     * With {@code FlushModeType.COMMIT} the effect of pending updates on queries is
     * unspecified, and the provider must not flush on the query's behalf.
     */
    public void testCommitFlushModeDoesNotFlush() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(UnenhancedFlushCustomer.class, "10").setName("Michael Bouschen");
            resetSQL();

            em.createQuery("SELECT o FROM UnenhancedFlushOrder o "
                + "WHERE o.customer.name = 'Michael Bouschen'",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();

            assertNotSQL(ANY_UPDATE);
            em.getTransaction().rollback();
        }
    }

    /**
     * "If there is no transaction active, the persistence provider must not flush to
     * the database" (section 3.10.8).
     */
    public void testNoFlushWithoutActiveTransaction() {
        try (EntityManager em = emf.createEntityManager()) {
            UnenhancedFlushCustomer cust = em.find(UnenhancedFlushCustomer.class, "10");
            cust.setName("Michael Bouschen");
            resetSQL();

            em.createQuery("SELECT o FROM UnenhancedFlushOrder o "
                + "WHERE o.customer.name = 'Michael Bouschen'",
                UnenhancedFlushOrder.class)
                .setFlushMode(FlushModeType.AUTO)
                .getResultList();

            assertNotSQL(ANY_UPDATE);
        }
    }
}
