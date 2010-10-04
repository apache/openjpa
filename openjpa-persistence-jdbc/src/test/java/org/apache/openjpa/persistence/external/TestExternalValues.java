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
package org.apache.openjpa.persistence.external;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.persistence.RollbackException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestExternalValues extends SingleEMFTestCase {

    public void setUp() {
        super.setUp(CLEAR_TABLES, EntityA.class);
    }

    public void testExternalValues() {
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        EntityA entity = new EntityA();

        entity.setS1("SMALL");
        entity.setS2("MEDIUM");
        entity.setUseStreaming(true);

        em.persist(entity);
        
        em.getTransaction().commit();
        
        // Validate
        
        Query q = em.createQuery("SELECT a from EntityA a");
        EntityA aPrime = (EntityA) q.getSingleResult();
        Assert.assertEquals("SMALL", aPrime.getS1());
        Assert.assertEquals("MEDIUM", aPrime.getS2());
        Assert.assertEquals(true, aPrime.getUseStreaming());

        em.close();
    }

    public void testUnrecognizedExternalValue() {
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        EntityA entity = new EntityA();

        entity.setS1("ABDEF");
        entity.setS2("NOT_VALID");

        em.persist(entity);

        try {
            em.getTransaction().commit();
            fail("Expected an exception at commit time");
        } catch (RollbackException e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            assertTrue(t.getMessage().contains(
                    "was not found in the list of ExternalValues"));
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
        em.close();
    }
}
