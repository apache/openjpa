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
package org.apache.openjpa.persistence.embed.compositepk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.simple.SimpleOrderEntity;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JPA 3.2 getReference(entity) for single, IdClass and EmbeddedId identities,
 * using detached instances as well as new instances with assigned ids.
 */
class TestGetReferenceByEntity extends SingleEMFTestCase {

    private SimpleOrderEntity order;
    private Subject subject;
    private SubjectWithIdClass subjectWithIdClass;

    @BeforeEach
    public void setUp() {
        super.setUp(DROP_TABLES, Subject.class, SubjectKey.class, SubjectWithIdClass.class, Topic.class,
            SimpleOrderEntity.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        order = new SimpleOrderEntity(1, 10, "order");
        em.persist(order);
        subject = new Subject();
        subject.setKey(new SubjectKey(1, "Type"));
        em.persist(subject);
        subjectWithIdClass = newSubjectWithIdClass(2, "Type");
        em.persist(subjectWithIdClass);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void singleIdDetached() {
        EntityManager em = emf.createEntityManager();
        try {
            SimpleOrderEntity ref = em.getReference(order);
            assertTrue(em.contains(ref));
            assertEquals(1, ref.getId());
            assertEquals("order", ref.getDescription());
        } finally {
            em.close();
        }
    }

    @Test
    void singleIdNewInstance() {
        EntityManager em = emf.createEntityManager();
        try {
            SimpleOrderEntity ref = em.getReference(new SimpleOrderEntity(1, 0, null));
            assertTrue(em.contains(ref));
            assertEquals("order", ref.getDescription());
        } finally {
            em.close();
        }
    }

    @Test
    void singleIdManaged() {
        EntityManager em = emf.createEntityManager();
        try {
            SimpleOrderEntity managed = em.find(SimpleOrderEntity.class, 1);
            assertSame(managed, em.getReference(managed));
        } finally {
            em.close();
        }
    }

    @Test
    void idClassDetached() {
        EntityManager em = emf.createEntityManager();
        try {
            SubjectWithIdClass ref = em.getReference(subjectWithIdClass);
            assertTrue(em.contains(ref));
            assertEquals(Integer.valueOf(2), ref.getSubjectNummer());
            assertEquals("Type", ref.getSubjectTypeCode());
        } finally {
            em.close();
        }
    }

    @Test
    void idClassNewInstance() {
        EntityManager em = emf.createEntityManager();
        try {
            SubjectWithIdClass ref = em.getReference(newSubjectWithIdClass(2, "Type"));
            assertTrue(em.contains(ref));
            assertEquals(Integer.valueOf(2), ref.getSubjectNummer());
            assertEquals("Type", ref.getSubjectTypeCode());
        } finally {
            em.close();
        }
    }

    @Test
    void idClassManaged() {
        EntityManager em = emf.createEntityManager();
        try {
            SubjectWithIdClass managed = em.createQuery("select s from SubjectWithIdClass s",
                SubjectWithIdClass.class).getSingleResult();
            assertSame(managed, em.getReference(managed));
        } finally {
            em.close();
        }
    }

    @Test
    void idClassUnassignedId() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getReference(newSubjectWithIdClass(null, null));
            fail("Expected IllegalArgumentException for unassigned id");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    @Test
    void embeddedIdDetached() {
        EntityManager em = emf.createEntityManager();
        try {
            Subject ref = em.getReference(subject);
            assertTrue(em.contains(ref));
            assertEquals(Integer.valueOf(1), ref.getKey().getSubjectNummer());
            assertEquals("Type", ref.getKey().getSubjectTypeCode());
        } finally {
            em.close();
        }
    }

    @Test
    void embeddedIdNewInstance() {
        EntityManager em = emf.createEntityManager();
        try {
            Subject s = new Subject();
            s.setKey(new SubjectKey(1, "Type"));
            Subject ref = em.getReference(s);
            assertTrue(em.contains(ref));
            assertSame(em.find(Subject.class, new SubjectKey(1, "Type")), ref);
        } finally {
            em.close();
        }
    }

    @Test
    void embeddedIdUnassignedId() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getReference(new Subject());
            fail("Expected IllegalArgumentException for unassigned id");
        } catch (IllegalArgumentException e) {
            // expected
        } finally {
            em.close();
        }
    }

    private static SubjectWithIdClass newSubjectWithIdClass(Integer nummer, String typeCode) {
        SubjectWithIdClass s = new SubjectWithIdClass();
        s.setSubjectNummer(nummer);
        s.setSubjectTypeCode(typeCode);
        return s;
    }
}
