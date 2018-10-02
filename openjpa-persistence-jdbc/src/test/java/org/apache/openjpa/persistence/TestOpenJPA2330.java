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

import javax.persistence.EntityManager;
import javax.persistence.spi.LoadState;

import junit.framework.Assert;
import org.apache.openjpa.persistence.entity.EntityA;
import org.apache.openjpa.persistence.entity.EntityB;
import org.apache.openjpa.persistence.entity.EntityC;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 *
 */
public class TestOpenJPA2330 extends SingleEMFTestCase {

    public void setUp() {
        setUp(EntityA.class, EntityB.class, EntityC.class);
    }

    public void testOpenJPA2330() {
        final EntityManager em = emf.createEntityManager();

        EntityA a = new EntityA();
        EntityB b = new EntityB(a);
        // set back pointer
        a.getBs().add(b);

        EntityC c = new EntityC(b);
        // set back pointer
        b.getCs().add(c);

        em.persist(a);
        em.persist(b);
        em.persist(c);

        assertEquals(LoadState.LOADED, OpenJPAPersistenceUtil.isLoaded(b, "center"));

        em.close();
    }

    public void testOpenJPA2335() {
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        EntityA a = new EntityA();

        EntityB b1 = new EntityB(a);
        b1.setName("b1");

        EntityB b2 = new EntityB(a);
        b2.setName("b2");

        EntityB b3 = new EntityB(a);
        b3.setName("b3");

        EntityB b4 = new EntityB(a);
        b4.setName("b4");

        a.getBs().add(b1);
        a.getBs().add(b2);
        a.getBs().add(b3);
        a.getBs().add(b4);

        em.persist(a);

        em.getTransaction().commit();
        em.close();

        // now read all back in
        em = emf.createEntityManager();
        em.getTransaction().begin();
        EntityA a2 = em.find(EntityA.class, a.getId());
        Assert.assertNotNull(a2);
        Assert.assertNotNull(a2.getBs());
        Assert.assertEquals(4, a2.getBs().size());

        em.getTransaction().commit();
        em.close();
    }
}
