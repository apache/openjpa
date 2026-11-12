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
package org.apache.openjpa.persistence.relations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.DynamicEnhancementTest;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@DynamicEnhancementTest
class TestRelationFieldAsPrimaryKeyAndForeignKey extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(C.class, CM.class, D.class, E.class, VC.class,
                VCS.class, CLEAR_TABLES);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            List<E> es = (List<E>) em.createQuery(
                    "Select e from E e").getResultList();
            for (E e : es) {
                em.remove(e);
            }

            em.getTransaction().commit();
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            E e = new E();
            e.setEId("E1");
            e.setName("E1");

            VC vc = new VC();
            vc.setVcId("VC1");

            VCS vcset = new VCS();
            vcset.setVcsId("VCS1");
            vcset.setName("VCSET1");
            vcset.addVC(vc);
            vcset.setE(e);

            C c = new C();
            c.setCId("C1");

            CM cm = new CM();
            cm.setCmId("CM1");
            cm.setE(e);
            cm.addC(c);

            D d = new D();
            d.setA("addr");
            d.setVc(vc);
            d.setId("IM1");

            em.persist(e);
            em.persist(vc);
            em.persist(vcset);
            em.persist(c);
            em.persist(cm);
            em.persist(d);

            em.getTransaction().commit();
        }
    }


    @Test
    void unboundEntities() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        VCS vcSet = new VCS();
        vcSet.setVcsId("VCSET2");
        vcSet.setName("VCSET2");
        try {
            em.persist(vcSet);
            em.getTransaction().commit();
            fail("didn't throw expected PersistenceException");
        } catch (Exception e) {
            // test pass
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }

        em.getTransaction().begin();
        VC vc = new VC();
        vc.setVcId("VC2");
        try {
            em.persist(vc);
            em.getTransaction().commit();
            fail("didn't throw expected PersistenceException");
        } catch (Exception e) {
            // test pass
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }

        em.getTransaction().begin();
        CM cm = new CM();
        cm.setCmId("CMID2");
        try {
            em.persist(cm);
            em.getTransaction().commit();
            fail("didn't throw expected PersistenceException");
        } catch (Exception e) {
            // test pass
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }

        em.getTransaction().begin();
        C c = new C();
        c.setCId("CID2");
        try {
            em.persist(c);
            em.getTransaction().commit();
            fail("didn't throw expected PersistenceException");
        } catch (Exception e) {
            // test pass
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }

        em.close();
    }

    @Test
    void query() {
        EntityManager em = emf.createEntityManager();
        List<E> es = (List<E>) em.createQuery(
                "Select e from E e where e.name='E1'").getResultList();
        assertEquals(1, es.size());
        E e = (E) es.get(0);
        assertEquals("E1", e.getName());
        assertEquals(1, e.getVcss().size());
        assertEquals(1, e.getCms().size());
        assertEquals(1, e.getVcss().size());

        // Get virtual container set and check that it has a reference to the
        // ensemble
        List<VCS> vcss = (List<VCS>) em.createQuery(
                "Select vcset from VCS vcset where vcset.vcsId='VCS1'")
                .getResultList();
        assertEquals(1, vcss.size());
        assertEquals(e, ((VCS) vcss.get(0)).getE());
        em.close();
    }

    @Test
    void deletes() {
        // Remove VC set and check that all VCs belonging to that set are
        // deleted but not the ensemble itself
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        VCS vcset = (VCS) em.createQuery(
                "Select vcset from VCS vcset where vcset.vcsId='VCS1'")
                .getSingleResult();
        em.remove(vcset);
        em.getTransaction().commit();

        // Get virtualContainer
        List<VC> vcs = (List<VC>) em.createQuery(
                "Select vc from VC vc where vc.vcId='VC1'")
                .getResultList();
        assertEquals(0, vcs.size());

        // Make sure E and I are still there
        List<E> es = (List<E>) em.createQuery(
                "Select e from E e").getResultList();
        assertEquals(1, es.size());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (!isTestsDisabled()) {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            List<E> es = (List<E>) em.createQuery(
                    "Select e from E e").getResultList();
            for (E e : es) {
                em.remove(e);
            }

            em.getTransaction().commit();
            em.close();
        }
        super.tearDown();
    }
}
