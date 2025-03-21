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
package org.apache.openjpa.persistence.recursive;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import jakarta.persistence.EntityManager;

/**
 * Test accessing a non-recursive relation of an instance that has been loaded via a recursive relation.
 */
public class TestRecursiveThenNonRecursiveRelationship extends SingleEMFTestCase {
    @Override
    public void setUp() {
        setUp(DROP_TABLES, Cat.class, Human.class,
                "openjpa.ConnectionFactoryProperties", "PrintParameters=True",
                "openjpa.Log", "SQL=trace");
    }

    public void testRelationAccessedViaRecursiveRelationship() {
        final String ALICE = "Alice";
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Human alice = new Human();
        alice.setName(ALICE);
        em.persist(alice);
        Human bob = new Human();
        bob.setName("Bob");
        em.persist(bob);

        Cat parent = new Cat();
        parent.setHuman(alice);
        em.persist(parent);

        Cat kitten1 = new Cat();
        kitten1.setParent(parent);
        kitten1.setHuman(bob);
        em.persist(kitten1);

        // Add a sibling that belongs to the same human so that later, when we are finding kitten1, parent will not be
        // loaded directly, but instead via the path `kitten1.human.cats.parent`. Needed to reproduce
        Cat kitten2 = new Cat();
        kitten2.setParent(parent);
        kitten2.setHuman(bob);
        em.persist(kitten2);

        em.getTransaction().commit();

        int catId = parent.getId();
        int kitten1Id = kitten1.getId();

        em.close();
        em = emf.createEntityManager();

        Cat jpqlKitten_1 = em.createQuery("SELECT c FROM Cat c WHERE c.id = :id", Cat.class)
                .setParameter("id", kitten1Id)
                .getSingleResult();
        Cat kitten_1 = em.find(Cat.class, kitten1Id);
        assertEquals(ALICE, kitten_1.getParent().getHuman().getName());
        assertEquals(ALICE, jpqlKitten_1.getParent().getHuman().getName());

        em.close();
    }
}
