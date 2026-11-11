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
package org.apache.openjpa.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.event.AbstractLifecycleListener;
import org.apache.openjpa.event.AbstractTransactionListener;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.event.TransactionEvent;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBrokerFactoryListenerRegistry
    extends SingleEMFTestCase {

    private int persistCount = 0;
    private int beginCount = 0;

    @BeforeEach
    public void setUp() {
        super.setUp(AllFieldTypes.class);
    }

    @Override
    protected OpenJPAEntityManagerFactorySPI createEMF(Object... props) {
        OpenJPAEntityManagerFactorySPI emf = super.createEMF(props);
        emf.addLifecycleListener(new AbstractLifecycleListener() {
            @Override
            public void beforePersist(LifecycleEvent event) {
                persistCount++;
            }
        }, null);
        emf.addTransactionListener(new AbstractTransactionListener() {
            @Override
            public void afterBegin(TransactionEvent event) {
                beginCount++;
            }
        });
        return emf;
    }

    @Test
    public void testLifecycleListenerRegistry() {
        beginCount = 0;
        persistCount = 0;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            em.persist(new AllFieldTypes());
            em.flush();
            assertEquals(1, beginCount);
            assertEquals(1, persistCount);
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }
}
