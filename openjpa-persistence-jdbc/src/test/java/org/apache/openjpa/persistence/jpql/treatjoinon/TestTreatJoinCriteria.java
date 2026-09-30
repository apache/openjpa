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
package org.apache.openjpa.persistence.jpql.treatjoinon;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * CriteriaBuilder.treat() over a join of a three level single table hierarchy must narrow the
 * join to instances of the treated class and of its subclasses, as the equivalent JPQL TREAT
 * join does (see {@link TestTreatSubclassDiscriminator}).
 */
public class TestTreatJoinCriteria extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(TProduct.class, TSoftwareProduct.class, TGameProduct.class,
            TLineItem.class, TOrder.class, DROP_TABLES);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            TProduct hw = new TProduct();
            hw.setName("Hardware");
            em.persist(hw);

            TSoftwareProduct sw = new TSoftwareProduct();
            sw.setName("Software");
            sw.setRevisionNumber(2.0);
            em.persist(sw);

            TGameProduct game = new TGameProduct();
            game.setName("Game");
            game.setRevisionNumber(2.0);
            game.setGenre("FPS");
            em.persist(game);

            TOrder order = new TOrder();
            order.getProducts().addAll(List.of(hw, sw, game));
            em.persist(order);

            for (TProduct p : new TProduct[] { hw, sw, game }) {
                TLineItem li = new TLineItem();
                li.setQuantity(1);
                li.setProduct(p);
                li.setOrder(order);
                em.persist(li);
            }

            em.getTransaction().commit();
        }
    }

    public void testUntreatedJoinReturnsAllProducts() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TLineItem> li = cq.from(TLineItem.class);
            Join<TLineItem, TProduct> product = li.join("product");
            cq.select(product.<String>get("name"));

            List<String> results = em.createQuery(cq).getResultList();
            Collections.sort(results);
            assertEquals(List.of("Game", "Hardware", "Software"), results);
        }
    }

    public void testTreatedJoinIncludesSubclasses() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TLineItem> li = cq.from(TLineItem.class);
            Join<TLineItem, TProduct> product = li.join("product");
            Join<TLineItem, TSoftwareProduct> software = cb.treat(product, TSoftwareProduct.class);
            cq.select(software.<String>get("name"));

            List<String> results = em.createQuery(cq).getResultList();
            Collections.sort(results);
            assertEquals(List.of("Game", "Software"), results);
        }
    }

    public void testTreatedJoinToLeafClass() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TLineItem> li = cq.from(TLineItem.class);
            Join<TLineItem, TProduct> product = li.join("product");
            Join<TLineItem, TGameProduct> game = cb.treat(product, TGameProduct.class);
            cq.select(game.<String>get("name"));

            assertEquals(List.of("Game"), em.createQuery(cq).getResultList());
        }
    }

    /**
     * The narrowed join resolves attributes that are declared by the treated class only.
     */
    public void testTreatedJoinResolvesSubclassAttribute() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TLineItem> li = cq.from(TLineItem.class);
            Join<TLineItem, TProduct> product = li.join("product");
            Join<TLineItem, TGameProduct> game = cb.treat(product, TGameProduct.class);
            cq.select(game.<String>get("name")).where(cb.equal(game.get("genre"), "FPS"));

            assertEquals(List.of("Game"), em.createQuery(cq).getResultList());
        }
    }

    public void testTreatedRootIncludesSubclasses() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TProduct> product = cq.from(TProduct.class);
            Root<TSoftwareProduct> software = cb.treat(product, TSoftwareProduct.class);
            cq.select(software.<String>get("name"));

            List<String> results = em.createQuery(cq).getResultList();
            Collections.sort(results);
            assertEquals(List.of("Game", "Software"), results);
        }
    }

    public void testTreatedRootToLeafClass() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TProduct> product = cq.from(TProduct.class);
            Root<TGameProduct> game = cb.treat(product, TGameProduct.class);
            cq.select(game.<String>get("name"));

            assertEquals(List.of("Game"), em.createQuery(cq).getResultList());
        }
    }

    /**
     * A type that is not a subtype of the join can not be a TREAT target.
     */
    public void testTreatToUnrelatedTypeIsRejected() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TLineItem> li = cq.from(TLineItem.class);
            Join<TLineItem, TProduct> product = li.join("product");
            try {
                cb.treat(product, (Class) TOrder.class);
                fail("Expected an IllegalArgumentException for an unrelated TREAT target");
            } catch (IllegalArgumentException expected) {
                // no-op
            }
        }
    }

    public void testTreatedCollectionJoinIncludesSubclasses() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TOrder> order = cq.from(TOrder.class);
            CollectionJoin<TOrder, TProduct> products = order.joinCollection("products");
            CollectionJoin<TOrder, TSoftwareProduct> software = cb.treat(products, TSoftwareProduct.class);
            cq.select(software.<String>get("name"));

            List<String> results = em.createQuery(cq).getResultList();
            Collections.sort(results);
            assertEquals(List.of("Game", "Software"), results);
        }
    }

    /**
     * The narrowed collection join resolves attributes that are declared by the treated class only.
     */
    public void testTreatedCollectionJoinResolvesSubclassAttribute() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TOrder> order = cq.from(TOrder.class);
            CollectionJoin<TOrder, TProduct> products = order.joinCollection("products");
            CollectionJoin<TOrder, TGameProduct> game = cb.treat(products, TGameProduct.class);
            cq.select(game.<String>get("name")).where(cb.equal(game.get("genre"), "FPS"));

            assertEquals(List.of("Game"), em.createQuery(cq).getResultList());
        }
    }

    /**
     * A join that is correlated to an outer query carries no kernel variable of its own, so it
     * can not be narrowed. The attempt must be diagnosed instead of being silently ignored.
     */
    public void testTreatOfCorrelatedJoinIsRejected() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<TOrder> order = cq.from(TOrder.class);
            CollectionJoin<TOrder, TProduct> products = order.joinCollection("products");
            Subquery<Long> sq = cq.subquery(Long.class);
            CollectionJoin<TOrder, TProduct> correlated = sq.correlate(products);
            try {
                cb.treat(correlated, TGameProduct.class);
                fail("Expected an UnsupportedOperationException for TREAT of a correlated join");
            } catch (UnsupportedOperationException expected) {
                // no-op
            }
        }
    }

    /**
     * Narrowing an arbitrary path expression is not implemented and must be diagnosed instead of
     * returning the path unnarrowed.
     */
    public void testTreatOfPlainPathIsRejected() {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<TLineItem> li = cq.from(TLineItem.class);
            Path<TProduct> product = li.get("product");
            try {
                cb.treat(product, TGameProduct.class);
                fail("Expected an UnsupportedOperationException for TREAT of a plain path");
            } catch (UnsupportedOperationException expected) {
                // no-op
            }
        }
    }
}
