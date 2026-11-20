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
package org.apache.openjpa.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * callInTransaction re-finds a returned entity whose type is left to runtime
 * enhancement, so that it returns the same runtime subclass that find()
 * returns. Results that are not entities must be returned as they are
 * (OPENJPA-3090).
 */
class TestCallInTransactionOfUnenhancedTypes extends AbstractPersistenceTestCase {

    private EntityManagerFactory emf;

    @BeforeEach
    void createUnenhancedEMF() {
        assertFalse(PersistenceCapable.class.isAssignableFrom(UnenhancedTransactionEntity.class),
            "UnenhancedTransactionEntity must be left to runtime enhancement");
        assertFalse(PersistenceCapable.class.isAssignableFrom(UnenhancedTransactionLabel.class),
            "UnenhancedTransactionLabel must be left to runtime enhancement");
        emf = createEMF(UnenhancedTransactionEntity.class, UnenhancedTransactionLabel.class, DROP_TABLES,
            "openjpa.RuntimeUnenhancedClasses", "supported",
            "openjpa.DynamicEnhancementAgent", "false");
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeEMF(emf);
        emf = null;
        super.tearDown();
    }

    @Test
    void entityResultHasTheClassFindReturns() {
        UnenhancedTransactionEntity result = emf.callInTransaction(em -> {
            UnenhancedTransactionEntity e = new UnenhancedTransactionEntity();
            e.setId(1L);
            e.setName("one");
            em.persist(e);
            return e;
        });
        assertEquals("one", result.getName());
        try (EntityManager em = emf.createEntityManager()) {
            assertSame(em.find(UnenhancedTransactionEntity.class, 1L).getClass(), result.getClass());
        }
    }

    @Test
    void nonEntityResultsAreReturnedAsTheyAre() {
        UnenhancedTransactionLabel label = new UnenhancedTransactionLabel("label");
        assertSame(label, emf.callInTransaction(em -> label));

        String name = emf.callInTransaction(em -> {
            UnenhancedTransactionEntity e = new UnenhancedTransactionEntity();
            e.setId(2L);
            e.setName("two");
            em.persist(e);
            return e.getName();
        });
        assertEquals("two", name);
    }
}
