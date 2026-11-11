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
package org.apache.openjpa.persistence.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.persistence.lifecycle.entity.OrphanXmlChild;
import org.apache.openjpa.persistence.lifecycle.entity.OrphanXmlDetail;
import org.apache.openjpa.persistence.lifecycle.entity.OrphanXmlItem;
import org.apache.openjpa.persistence.lifecycle.entity.OrphanXmlParent;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Orphan removal on relations mapped in orm.xml that also cascade remove,
 * either through cascade-all or cascade-remove.
 */
public class TestOrphanRemovalXml extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "orphan-xml-pu";
    }

    @Test
    public void testOrphanRemovalKeptWithCascadeRemove() {
        ClassMetaData meta = emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData(OrphanXmlParent.class, null, true);
        assertEquals(ValueMetaData.CASCADE_AUTO,
            meta.getField("children").getElement().getCascadeDelete());
        assertEquals(ValueMetaData.CASCADE_AUTO,
            meta.getField("items").getElement().getCascadeDelete());
        assertEquals(ValueMetaData.CASCADE_AUTO,
            meta.getField("detail").getCascadeDelete());
        assertEquals(ValueMetaData.CASCADE_AUTO,
            meta.getField("extra").getCascadeDelete());
    }

    @Test
    public void testOrphanRemovedFromCascadeAllCollection() {
        createParent(1);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = em.find(OrphanXmlParent.class, 1);
            assertEquals(2, parent.getChildren().size());
            parent.getChildren().remove(0);
            em.getTransaction().commit();
        }

        assertEquals(1, count(OrphanXmlChild.class));
    }

    @Test
    public void testOrphanRemovedFromCascadeRemoveCollection() {
        createParent(1);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = em.find(OrphanXmlParent.class, 1);
            assertEquals(2, parent.getItems().size());
            parent.getItems().remove(0);
            em.getTransaction().commit();
        }

        assertEquals(1, count(OrphanXmlItem.class));
    }

    @Test
    public void testOrphanRemovedFromCascadeAllOneToOne() {
        createParent(1);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = em.find(OrphanXmlParent.class, 1);
            assertNotNull(parent.getDetail());
            parent.setDetail(null);
            em.getTransaction().commit();
        }

        assertEquals(1, count(OrphanXmlDetail.class));
    }

    @Test
    public void testOrphanReplacedInCascadeAllOneToOne() {
        createParent(1);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = em.find(OrphanXmlParent.class, 1);
            parent.setDetail(new OrphanXmlDetail(2, "replacement"));
            em.getTransaction().commit();
        }

        assertEquals(2, count(OrphanXmlDetail.class));
        try (EntityManager em = emf.createEntityManager()) {
            assertNull(em.find(OrphanXmlDetail.class, 1));
            assertEquals(2, em.find(OrphanXmlParent.class, 1).getDetail().getId());
        }
    }

    @Test
    public void testOrphanRemovedFromCascadeRemoveOneToOne() {
        createParent(1);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = em.find(OrphanXmlParent.class, 1);
            assertNotNull(parent.getExtra());
            parent.setExtra(null);
            em.getTransaction().commit();
        }

        assertEquals(1, count(OrphanXmlDetail.class));
        try (EntityManager em = emf.createEntityManager()) {
            assertNull(em.find(OrphanXmlDetail.class, 101));
        }
    }

    @Test
    public void testRemoveOwnerRemovesDependents() {
        createParent(1);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = em.find(OrphanXmlParent.class, 1);
            OrphanXmlChild child = parent.getChildren().get(0);
            OrphanXmlItem item = parent.getItems().get(0);
            OrphanXmlDetail detail = parent.getDetail();
            OrphanXmlDetail extra = parent.getExtra();
            em.remove(parent);
            assertFalse(em.contains(child));
            assertFalse(em.contains(item));
            assertFalse(em.contains(detail));
            assertFalse(em.contains(extra));
            em.getTransaction().commit();
        }

        assertEquals(0, count(OrphanXmlParent.class));
        assertEquals(0, count(OrphanXmlChild.class));
        assertEquals(0, count(OrphanXmlItem.class));
        assertEquals(0, count(OrphanXmlDetail.class));
    }

    private void createParent(int id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            OrphanXmlParent parent = new OrphanXmlParent(id);
            for (int i = 1; i <= 2; i++) {
                parent.getChildren().add(new OrphanXmlChild(id * 10 + i, "child" + i, parent));
                parent.getItems().add(new OrphanXmlItem(id * 10 + i, "item" + i));
            }
            parent.setDetail(new OrphanXmlDetail(id, "detail"));
            parent.setExtra(new OrphanXmlDetail(id + 100, "extra"));
            em.persist(parent);
            em.getTransaction().commit();
        }
    }
}
