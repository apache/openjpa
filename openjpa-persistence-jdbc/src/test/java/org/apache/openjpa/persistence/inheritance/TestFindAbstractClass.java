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
package org.apache.openjpa.persistence.inheritance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test that you can find a concrete subclass record when passing in its
 * abstract base class to EntityManager.find().
 *
 * @author Abe White
 */
class TestFindAbstractClass
    extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(AbstractBase.class, ConcreteSubclass.class, CLEAR_TABLES);

        ConcreteSubclass e = new ConcreteSubclass();
        e.setId("id");
        e.setSubclassData(1);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void entityTypeInheritanceTypeJoined() {
        EntityManager em = emf.createEntityManager();
        String query = "select c from AbstractBase c where TYPE(c) = ConcreteSubclass";
        List rs = em.createQuery(query).getResultList();
        assertInstanceOf(ConcreteSubclass.class, rs.get(0));
        query = "select c from AbstractBase c";
        rs = em.createQuery(query).getResultList();
        assertInstanceOf(ConcreteSubclass.class, rs.get(0));
        em.close();
    }

    @Test
    void find() {
        EntityManager em = emf.createEntityManager();
        AbstractBase e = em.find(AbstractBase.class, "id");
        assertNotNull(e);
        assertInstanceOf(ConcreteSubclass.class, e);
        assertEquals(1, ((ConcreteSubclass) e).getSubclassData());
        em.close();
    }
}

