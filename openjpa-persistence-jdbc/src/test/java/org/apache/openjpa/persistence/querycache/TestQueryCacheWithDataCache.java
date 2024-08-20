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
package org.apache.openjpa.persistence.querycache;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.FetchPlan;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestQueryCacheWithDataCache extends SingleEMFTestCase {

    @Override
    public void setUp() {
        super.setUp(DROP_TABLES, QCEntityM2O.class, QCEntity.class, "openjpa.DataCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm", "openjpa.QueryCache", "true");
    }

    /*
     * Test for OPENJPA-2586
     */
    public void testWithFetchPlan() {
        populate();

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        doQueryWithFetchPlan(em);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        em.getTransaction().begin();
        doQueryWithFetchPlan(em);
        em.getTransaction().commit();
        em.close();
    }

    public void doQueryWithFetchPlan(EntityManager em) {
        String jpql = "Select e1 from QCEntityM2O e1";

        Query q = em.createQuery(jpql);
        FetchPlan fetchPlan = q.unwrap(OpenJPAQuery.class).getFetchPlan();
        fetchPlan.addField(QCEntityM2O.class, "qc");
        List<QCEntityM2O> results = (List<QCEntityM2O>) q.getResultList();

        em.clear();

        assertTrue("No results returned!", !results.isEmpty());
        for (QCEntityM2O e1 : results) {
            assertNotNull("A 'QCEntity' should have been returned!", e1.getQc());
        }
    }

    public void populate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        QCEntityM2O e1 = new QCEntityM2O("aQCEntityM2O");
        QCEntity e2 = new QCEntity("aQCEntityM2O", "test", 2L);
        e1.setQc(e2);

        em.persist(e1);
        em.persist(e2);

        em.getTransaction().commit();
        em.close();
    }
}
