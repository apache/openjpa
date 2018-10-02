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

public class TestPUDefaultCascadePersist extends SingleEMFTestCase {
    public void setUp() throws Exception {
        super.setUp(PUDEntityA01.class, PUDEntityA02.class, PUDEntityAE01.class, PUDEntityB.class,
            AnEmbeddable.class, EmbeddableWithRelationships.class,
            CLEAR_TABLES);
    }

    protected String getPersistenceUnitName() {
        return "TestPUDefaultCascadePersist";
    }


    public void tearDown() throws Exception {
        super.tearDown();
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }


        em = emf.createEntityManager();

        try {
            PUDEntityA01 f_entity = em.find(PUDEntityA01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertEquals(10, f_entity.getColM2M().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA01 f_entity = em.find(PUDEntityA01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertEquals(10, f_entity.getColO2M().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA01 f_entity = em.find(PUDEntityA01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertNotNull(entity.getO2o());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA01 f_entity = em.find(PUDEntityA01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertNotNull(entity.getM2o());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityAE01 f_entity = em.find(PUDEntityAE01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertEquals(10, f_entity.getColM2M().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityAE01 f_entity = em.find(PUDEntityAE01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertEquals(10, f_entity.getColO2M().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityAE01 f_entity = em.find(PUDEntityAE01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertNotNull(entity.getO2o());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
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
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityAE01 f_entity = em.find(PUDEntityAE01.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertNotNull(entity.getM2o());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }


    public void testPUDefaultCascadePersistOverM2MEmbbedRel() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA02 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA02();
            entity.setStrData("PUDEntityA02");

            for (int i = 0; i < 10; i++) {
                PUDEntityB b = new PUDEntityB();
                b.setStrData("B");
                entity.getEmb().getColM2M().add(b);
            }

            em.persist(entity);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA02 f_entity = em.find(PUDEntityA02.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertEquals(10, f_entity.getEmb().getColM2M().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }

    public void testPUDefaultCascadePersistOverO2MEmbbedRel() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA02 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA02();
            entity.setStrData("PUDEntityA02");

            for (int i = 0; i < 10; i++) {
                PUDEntityB b = new PUDEntityB();
                b.setStrData("B");
                entity.getEmb().getColO2M().add(b);
            }

            em.persist(entity);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA02 f_entity = em.find(PUDEntityA02.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertEquals(10, f_entity.getEmb().getColO2M().size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }

    public void testPUDefaultCascadePersistOverO2OEmbbedRel() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA02 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA02();
            entity.setStrData("PUDEntityA02");

            PUDEntityB b = new PUDEntityB();
            b.setStrData("B");
            entity.getEmb().setO2o(b);

            em.persist(entity);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA02 f_entity = em.find(PUDEntityA02.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertNotNull(entity.getEmb().getO2o());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }

    public void testPUDefaultCascadePersistOverM2OEmbbedRel() {
        EntityManager em = emf.createEntityManager();

        PUDEntityA02 entity = null;
        try {
            em.getTransaction().begin();

            entity = new PUDEntityA02();
            entity.setStrData("PUDEntityA02");

            PUDEntityB b = new PUDEntityB();
            b.setStrData("B");
            entity.getEmb().setM2o(b);

            em.persist(entity);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }

        em.close();
        em = emf.createEntityManager();

        try {
            PUDEntityA02 f_entity = em.find(PUDEntityA02.class, entity.getId());
            assertNotNull(f_entity);
            assertNotSame(entity, f_entity);
            assertNotNull(entity.getEmb().getM2o());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }


}
