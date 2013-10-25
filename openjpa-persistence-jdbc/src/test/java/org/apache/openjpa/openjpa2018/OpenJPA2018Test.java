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
package org.apache.openjpa.openjpa2018;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class OpenJPA2018Test extends TestCase {
    private EntityManager em;
    private EntityTransaction transaction;
    private EntityManagerFactory factory;

    @Override
    public void setUp() {
        factory = OpenJPAPersistence.createEntityManagerFactory("openjpa2018", "META-INF/openjpa2018.xml");
        em = factory.createEntityManager();
        transaction = em.getTransaction();
        transaction.begin();
    }

    @Override
    public void tearDown() {
        transaction.rollback();
        em.close();
        factory.close();
    }

    public void testInCriteriaWithArray() {

        User2018 user = new User2018();
        em.persist(user);
        em.flush();

        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<User2018> criteria = builder.createQuery(User2018.class);
        Root<User2018> root = criteria.from(User2018.class);
        criteria.where(root.get("id").in(builder.parameter(Long[].class)));

        TypedQuery<User2018> query = em.createQuery(criteria);
        for (ParameterExpression parameter : criteria.getParameters()) {
            query.setParameter(parameter, new Long[] { user.id });
        }

        List<User2018> result = query.getResultList();
        assertTrue(!result.isEmpty());
    }

    public void testInCriteriaWithCollection() {

        User2018 user = new User2018();
        em.persist(user);
        em.flush();

        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<User2018> criteria = builder.createQuery(User2018.class);
        Root<User2018> root = criteria.from(User2018.class);
        criteria.where(root.get("id").in(builder.parameter(Collection.class)));

        TypedQuery<User2018> query = em.createQuery(criteria);
        for (ParameterExpression parameter : criteria.getParameters()) {
            query.setParameter(parameter, Arrays.asList(user.id));
        }

        List<User2018> result = query.getResultList();
        assertTrue(!result.isEmpty());
    }

    public void testId() {

        User2018 user = new User2018();
        em.persist(user);
        em.flush();

        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<User2018> criteria = builder.createQuery(User2018.class);
        Root<User2018> root = criteria.from(User2018.class);
        criteria.where(builder.equal(root.get("id"), user.id));

        TypedQuery<User2018> query = em.createQuery(criteria);
        for (ParameterExpression parameter : criteria.getParameters()) {
            query.setParameter(parameter, user.id);
        }

        List<User2018> result = query.getResultList();
        assertTrue(!result.isEmpty());
    }

}
