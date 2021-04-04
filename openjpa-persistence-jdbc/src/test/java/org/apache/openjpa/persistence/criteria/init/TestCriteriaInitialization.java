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
package org.apache.openjpa.persistence.criteria.init;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestCriteriaInitialization extends SingleEMFTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, AddressEntity.class, AddressPk.class, MyUserEntity.class);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            em.persist(new MyUserEntity("wayne", 1L));
            em.persist(new MyUserEntity("garth", 2L));

            em.persist(new AddressEntity(new AddressPk("street_1", 1L)));
            em.persist(new AddressEntity(new AddressPk("street_2", 2L)));

            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    public void test() {
        emf.close();

        EntityManagerFactory oldEmf = emf;
        emf = createEMF(AddressEntity.class, AddressPk.class, MyUserEntity.class);
        // ensure that we get a fresh emf
        assertNotEquals(oldEmf, emf);
        emf.getCriteriaBuilder();
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaQuery<MyUserEntity> cq = em.getCriteriaBuilder().createQuery(MyUserEntity.class);
            Root<MyUserEntity> from = cq.from(MyUserEntity.class);
            CriteriaQuery<MyUserEntity> selectAll = cq.select(from);
            TypedQuery<MyUserEntity> query = em.createQuery(selectAll);
            List<MyUserEntity> res = query.getResultList();
            // Make sure we get two results
            assertEquals(2, res.size());
        } finally {
            em.close();
        }
    }
}
