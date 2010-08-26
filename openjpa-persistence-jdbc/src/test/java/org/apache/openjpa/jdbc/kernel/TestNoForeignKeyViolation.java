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
package org.apache.openjpa.jdbc.kernel;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test that sql statements get flushed in an order which does not violate
 * non-nullable foreign key constraints on inserts and deletes.
 *
 * @author Reece Garrett
 */
public class TestNoForeignKeyViolation
    extends SingleEMFTestCase {

    private EntityA entityA;
    private EntityB entityB;
    private EntityC entityC;
    private EntityD entityD;

    public void setUp() {
        setUp(EntityA.class, EntityB.class, EntityC.class, EntityD.class, EntityE.class);

        createTestData();
    }

    private void createTestData() {
        entityA = new EntityA();
        entityB = new EntityB();
        entityC = new EntityC();
        entityD = new EntityD();
        entityA.setName("entityA");
        entityB.setName("entityB");
        entityC.setName("entityC");
        entityD.setName("entityD");
        entityA.setEntityB(entityB);
        entityB.setEntityC(entityC);
        entityC.setEntityD(entityD);
    }

    public void testSqlOrder() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entityA);
            em.getTransaction().commit();

            EntityD newEntityD = new EntityD();
            newEntityD.setName("newEntityD");
            entityC.setEntityD(newEntityD);

            em.getTransaction().begin();
            em.merge(entityC);
            em.getTransaction().commit();

            EntityC newEntityC = new EntityC();
            newEntityC.setName("newEntityC");
            newEntityD = new EntityD();
            newEntityD.setName("newNewEntityD");
            newEntityC.setEntityD(newEntityD);
            entityB.setEntityC(newEntityC);

            em.getTransaction().begin();
            em.merge(entityB);
            em.getTransaction().commit();
        }
        finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
    
    public void testSimpleCycle() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            entityD.setEntityA(entityA);
            em.persist(entityA);
            em.getTransaction().commit();
        }
        finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
    
    public void testComplexCycle() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityE entityE = new EntityE();
            entityE.setName("entityE");
            entityE.setEntityB(entityB);

            em.getTransaction().begin();
            em.persist(entityE);
            entityD.setEntityA(entityA);
            em.persist(entityA);
            em.getTransaction().commit();
            
            em.getTransaction().begin();
            em.remove(entityE);
            em.remove(entityA);
            em.getTransaction().commit();
        }
        finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    public void testComplexTwoCycles() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityE entityE = new EntityE();
            entityE.setName("entityE");
            entityE.setEntityB(entityB);

            em.getTransaction().begin();
            em.persist(entityE);
            entityD.setEntityA(entityA);
            entityD.setEntityB(entityB);
            em.persist(entityA);
            em.getTransaction().commit();
            
            em.getTransaction().begin();
            em.remove(entityE);
            em.remove(entityA);
            em.getTransaction().commit();
        }
        finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
}
