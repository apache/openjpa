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
package org.apache.openjpa.persistence.identity;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestStringId extends AbstractPersistenceTestCase {
    private static EntityManagerFactory _emf;

    @BeforeEach
    public void setUp() throws Exception {
        _emf = createEMF(StringIdEntity.class);

        cleanup();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeEMF(_emf);
        _emf = null;
        super.tearDown();
    }

    private void cleanup() {
        EntityManager em = _emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("Delete from StringIdEntity").executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    @Test
    public void testTrailingWhitespace() {
        StringIdEntity sie1 = new StringIdEntity();
        sie1.setId("ABC ");

        EntityManager em = _emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(sie1);
        em.getTransaction().commit();
        assertTrue(em.contains(sie1));

        StringIdEntity sie2 = em.find(StringIdEntity.class, "ABC");
        assertSame(sie1, sie2, "Find should return the same instance with trailing whitespace");

        StringIdEntity sie3 = em.find(StringIdEntity.class, "ABC  ");
        assertSame(sie1, sie3, "Find should return the same instance with trailing whitespace");

        assertNotSame(sie1, em.find(StringIdEntity.class, " ABC"), "Leading WS should not match");

        em.close();
    }

    @Test
    public void testLeadingWhitespace() {
        StringIdEntity sie1 = new StringIdEntity();
        sie1.setId(" ABC");

        EntityManager em = _emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(sie1);
        em.getTransaction().commit();
        assertTrue(em.contains(sie1));

        StringIdEntity sie2 = em.find(StringIdEntity.class, "ABC");
        assertNotSame(sie1, sie2, "Find should not return the same instance with leading whitespace");

        StringIdEntity sie3 = em.find(StringIdEntity.class, "   ABC");
        assertNotSame(sie1, sie3, "Find should not return the same instance with leading whitespace");

        assertSame(sie1, em.find(StringIdEntity.class, " ABC"));

        assertNotSame(sie1, em.find(StringIdEntity.class, "ABC "), "Trailing WS should not match");
        em.close();
    }

    @Test
    public void testInnerWhitespace() {
        StringIdEntity sie1 = new StringIdEntity();
        sie1.setId("A B C");

        EntityManager em = _emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(sie1);
        em.getTransaction().commit();
        assertTrue(em.contains(sie1));

        StringIdEntity sie2 = em.find(StringIdEntity.class, "ABC");
        assertNotSame(sie1, sie2, "Find should not return the same instance with inner whitespace");

        StringIdEntity sie3 = em.find(StringIdEntity.class, "   ABC");
        assertNotSame(sie1, sie3, "Find should not return the same instance with inner whitespace");

        assertSame(sie1, em.find(StringIdEntity.class, "A B C"));

        assertNotSame(sie1, em.find(StringIdEntity.class, "ABC "), "Trailing WS should not match");

        em.close();
    }
}
