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
package org.apache.openjpa.persistence.cascade.pudefault;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestNoPUDefaultCascadePersist extends SingleEMFTestCase {
    public void setUp() throws Exception {
        super.setUp(PUDEntityA01.class, PUDEntityA02.class, PUDEntityAE01.class, PUDEntityB.class,
            AnEmbeddable.class, EmbeddableWithRelationships.class,
            CLEAR_TABLES);
    }


    public void testPUDefaultCascadePersistOverM2M() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA01();
            entity.setStrData("PUDEntityA01");

            for (int i = 0; i < 10; i++) {
                PUDEntityB b = new PUDEntityB();
                b.setStrData("B");
                entity.getColM2M().add(b);
            }

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverO2M() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA01();
            entity.setStrData("PUDEntityA01");

            for (int i = 0; i < 10; i++) {
                PUDEntityB b = new PUDEntityB();
                b.setStrData("B");
                entity.getColO2M().add(b);
            }

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverO2O() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA01();
            entity.setStrData("PUDEntityA01");

            PUDEntityB b = new PUDEntityB();
            b.setStrData("B");
            entity.setO2o(b);

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverM2O() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA01();
            entity.setStrData("PUDEntityA01");

            PUDEntityB b = new PUDEntityB();
            b.setStrData("B");
            entity.setM2o(b);

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverM2MWithEmbed() {
        EntityManager em = emf.createEntityManager();

        PUDEntityAE01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityAE01();
            entity.setStrData("PUDEntityAE01");

            for (int i = 0; i < 10; i++) {
                PUDEntityB b = new PUDEntityB();
                b.setStrData("B");
                entity.getColM2M().add(b);
            }

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverO2MWithEmbed() {
        EntityManager em = emf.createEntityManager();

        PUDEntityAE01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityAE01();
            entity.setStrData("PUDEntityAE01");

            for (int i = 0; i < 10; i++) {
                PUDEntityB b = new PUDEntityB();
                b.setStrData("B");
                entity.getColO2M().add(b);
            }

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverO2OWithEmbed() {
        EntityManager em = emf.createEntityManager();

        PUDEntityAE01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityAE01();
            entity.setStrData("PUDEntityAE01");

            PUDEntityB b = new PUDEntityB();
            b.setStrData("B");
            entity.setO2o(b);

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void testPUDefaultCascadePersistOverM2OWithEmbed() {
        EntityManager em = emf.createEntityManager();

        PUDEntityAE01 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityAE01();
            entity.setStrData("PUDEntityAE01");

            PUDEntityB b = new PUDEntityB();
            b.setStrData("B");
            entity.setM2o(b);

            em.persist(entity);
            try {
                em.getTransaction().commit();
                fail("No Exception thrown.");
            } catch (Exception e) {
                // Expected
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
