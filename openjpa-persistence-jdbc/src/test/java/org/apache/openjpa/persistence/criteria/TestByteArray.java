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

package org.apache.openjpa.persistence.criteria;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestByteArray extends SingleEMFTestCase {
    protected CriteriaBuilder cb;
    String pic = "IamNotAPicture";
    Character[] chars = new Character[] {'a', 'b'};

    @BeforeEach
    public void setUp() {
        super.setUp(BlogUser.class
            // Using the following property will require corresponding
            // changes in BlogUser_.java
//            ,"openjpa.Compatibility", "UseListAttributeForArrays=true"
            );
        cb = emf.getCriteriaBuilder();
        populate();
    }

    public void populate() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();

        BlogUser bu = new BlogUser();
        bu.setUsername("jode1");
        bu.setPic(pic.getBytes());
        bu.setCharacters(chars);
        bu.setChars(pic.toCharArray());

        em.persist(bu);
        tran.commit();

        em.close();
    }

    public void assertBlogUser(BlogUser bu) {
        assertNotNull(bu);
        assertNotNull(bu.getPic());
        assertNotNull(bu.getCharacters());
        assertNotNull(bu.getChars());
    }

    @Test
    void simpleQuery() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        cq.select(cq.from(BlogUser.class));

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertFalse(users.isEmpty());

        assertBlogUser(users.get(0));

        em.close();
    }

    @Test
    void simpleQueryBytesNotNull() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        Root<BlogUser> bloguser = cq.from(BlogUser.class);
        cq.select(bloguser);
        cq.where(bloguser.get(BlogUser_.pic).isNotNull());

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertBlogUser(users.get(0));

        em.close();
    }

    @Test
    void simpleQueryBytesNull() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        Root<BlogUser> bloguser = cq.from(BlogUser.class);
        cq.select(bloguser);
        cq.where(bloguser.get(BlogUser_.pic).isNull());

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertTrue(users.isEmpty());

        em.close();
    }

    @Test
    void simpleQueryCharactersNotNull() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        Root<BlogUser> bloguser = cq.from(BlogUser.class);
        cq.select(bloguser);
        cq.where(bloguser.get(BlogUser_.characters).isNotNull());

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertBlogUser(users.get(0));

        em.close();
    }

    @Test
    void simpleQueryCharactersNull() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        Root<BlogUser> bloguser = cq.from(BlogUser.class);
        cq.select(bloguser);
        cq.where(bloguser.get(BlogUser_.characters).isNull());

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertTrue(users.isEmpty());

        em.close();
    }

    @Test
    void simpleQueryCharsNotNull() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        Root<BlogUser> bloguser = cq.from(BlogUser.class);
        cq.select(bloguser);
        cq.where(bloguser.get(BlogUser_.chars).isNotNull());

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertBlogUser(users.get(0));

        em.close();
    }

    @Test
    void simpleQueryCharsNull() {
        EntityManager em = emf.createEntityManager();
        CriteriaQuery<BlogUser> cq = cb.createQuery(BlogUser.class);
        Root<BlogUser> bloguser = cq.from(BlogUser.class);
        cq.select(bloguser);
        cq.where(bloguser.get(BlogUser_.chars).isNull());

        List<BlogUser> users = em.createQuery(cq).getResultList();

        assertNotNull(users);
        assertTrue(users.isEmpty());

        em.close();
    }

}
