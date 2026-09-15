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

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * TREAT(x AS Middle) over a three level single table hierarchy must match
 * instances of Middle and of its subclasses, but not of the root class.
 */
public class TestTreatSubclassDiscriminator extends SQLListenerTestCase {

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

    public void testTreatJoinIncludesSubclasses() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> results = em.createQuery(
                "SELECT s.name FROM TLineItem l JOIN TREAT(l.product AS TSoftwareProduct) s",
                String.class).getResultList();
            Collections.sort(results);
            assertEquals(List.of("Game", "Software"), results);
        }
    }

    public void testTreatJoinLeafClass() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> results = em.createQuery(
                "SELECT s.name FROM TLineItem l JOIN TREAT(l.product AS TGameProduct) s",
                String.class).getResultList();
            assertEquals(List.of("Game"), results);
        }
    }

    public void testTreatInWhereExclusiveProperty() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> results = em.createQuery(
                "SELECT p.name FROM TProduct p WHERE TREAT(p AS TGameProduct).genre = 'FPS'",
                String.class).getResultList();
            assertEquals(List.of("Game"), results);
        }
    }

    public void testTreatInWhereCommonProperty() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> results = em.createQuery(
                "SELECT p.name FROM TProduct p WHERE TREAT(p AS TGameProduct).revisionNumber = 2.0",
                String.class).getResultList();
            assertEquals(List.of("Game"), results);
        }
    }

    public void testTreatInWhereRestrictsOnlyItsPredicate() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> results = em.createQuery(
                "SELECT p.name FROM TProduct p "
                    + "WHERE TREAT(p AS TGameProduct).revisionNumber = 2.0 OR p.name = 'Hardware'",
                String.class).getResultList();
            Collections.sort(results);
            assertEquals(List.of("Game", "Hardware"), results);
        }
    }

    public void testTreatInWhereOnJoinVariable() {
        try (EntityManager em = emf.createEntityManager()) {
            List<String> results = em.createQuery(
                "SELECT p.name FROM TLineItem l JOIN l.product p "
                    + "WHERE TREAT(p AS TGameProduct).revisionNumber = 2.0",
                String.class).getResultList();
            assertEquals(List.of("Game"), results);
        }
    }
}
