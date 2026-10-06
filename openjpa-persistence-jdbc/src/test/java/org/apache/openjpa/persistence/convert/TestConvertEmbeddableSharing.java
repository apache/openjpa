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
package org.apache.openjpa.persistence.convert;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * A <code>@Convert(attributeName=...)</code> override is declared by a
 * single embedding of an embeddable, resp. by a single subclass of a
 * MappedSuperclass. The override must stay local to that embedding resp.
 * subclass and must not leak into the metadata shared with the other
 * embeddings and siblings (OPENJPA-2953).
 */
public class TestConvertEmbeddableSharing extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES,
            ConvertEmbedEntity.class,
            ConvertPlainEmbedEntity.class,
            ConvertDoubleEmbedEntity.class,
            ConvertEmbedCollectionEntity.class,
            ConvertInheritedBase.class,
            ConvertInheritedEntity.class,
            ConvertPlainInheritedEntity.class,
            ConvertAddress.class,
            ConvertSelfAddress.class,
            DotConverter.class,
            NumberToStateConverter.class);
    }

    /**
     * The converting entity still converts: DotConverter turns "." into "#"
     * on the way to the database and "#" into "_" on the way back.
     */
    public void testOverrideAppliesToDeclaringEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertEmbedEntity("c1", "name1", 1,
            new ConvertAddress("500.Oracle.Parkway", "Redwood Shores", 1)));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        List<?> raw = em.createNativeQuery(
            "SELECT street FROM CONV_B_EMBED WHERE id = 'c1'")
            .getResultList();
        assertEquals("500#Oracle#Parkway", raw.get(0));

        ConvertEmbedEntity found =
            em.find(ConvertEmbedEntity.class, "c1");
        assertEquals("500_Oracle_Parkway", found.getAddress().getStreet());
        em.close();
    }

    /**
     * The entity that declares no override must store the embeddable
     * attributes verbatim, no matter which entity's metadata was resolved
     * first.
     */
    public void testOverrideDoesNotLeakToOtherEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        // resolve the converting entity first, so that a leaking override
        // would already be in place for the plain one
        em.persist(new ConvertEmbedEntity("c2", "name2", 2,
            new ConvertAddress("500.Oracle.Parkway", "Redwood Shores", 1)));
        em.persist(new ConvertPlainEmbedEntity("p1", "name3",
            new ConvertAddress("200.Main.Street", "Springfield", 7)));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        List<?> raw = em.createNativeQuery(
            "SELECT street, state FROM CONV_PLAIN_EMBED WHERE id = 'p1'")
            .getResultList();
        Object[] row = (Object[]) raw.get(0);
        assertEquals("200.Main.Street", row[0]);
        assertEquals(7, ((Number) row[1]).intValue());

        ConvertPlainEmbedEntity found =
            em.find(ConvertPlainEmbedEntity.class, "p1");
        assertEquals("200.Main.Street", found.getAddress().getStreet());
        assertEquals(7, found.getAddress().getState());
        em.close();
    }

    /**
     * A converter declared on the embeddable's own attribute is not an
     * override of a single embedding, so it must still apply to every
     * entity embedding that embeddable.
     */
    public void testEmbeddableOwnConverterAppliesEverywhere() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ConvertEmbedEntity b = new ConvertEmbedEntity("c3", "name4", 3,
            new ConvertAddress("500.Oracle.Parkway", "Redwood Shores", 1));
        b.setSelfAddress(new ConvertSelfAddress("1.2.3"));
        em.persist(b);
        ConvertPlainEmbedEntity p = new ConvertPlainEmbedEntity("p2",
            "name5", new ConvertAddress("200.Main.Street", "Springfield", 7));
        p.setSelfAddress(new ConvertSelfAddress("4.5.6"));
        em.persist(p);
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        assertEquals("1#2#3", em.createNativeQuery(
            "SELECT SELF_STREET FROM CONV_B_EMBED WHERE id = 'c3'")
            .getSingleResult());
        assertEquals("4#5#6", em.createNativeQuery(
            "SELECT SELF_STREET FROM CONV_PLAIN_EMBED WHERE id = 'p2'")
            .getSingleResult());
        em.close();
    }

    /**
     * One entity embedding the same embeddable twice, with the override
     * declared on one of the two embeddings only. The second embedding
     * must store its attributes verbatim.
     */
    public void testOverrideDoesNotLeakToSecondEmbedding() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new ConvertDoubleEmbedEntity("d1",
            new ConvertAddress("1.2.3", "Springfield", 1),
            new ConvertAddress("4.5.6", "Shelbyville", 2)));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        Object[] row = (Object[]) em.createNativeQuery(
            "SELECT street, PLAIN_STREET FROM CONV_DOUBLE_EMBED "
            + "WHERE id = 'd1'").getSingleResult();
        assertEquals("1#2#3", row[0]);
        assertEquals("4.5.6", row[1]);
        em.close();
    }

    /**
     * For an element collection of embeddables the override addresses an
     * attribute of the collection's elements.
     */
    public void testOverrideAppliesToCollectionElements() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ConvertEmbedCollectionEntity e =
            new ConvertEmbedCollectionEntity("e1");
        e.getAddresses().add(
            new ConvertAddress("1.2.3", "Springfield", 1));
        em.persist(e);
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        assertEquals("1#2#3", em.createNativeQuery(
            "SELECT street FROM CONV_EMBED_COLL_ADDR").getSingleResult());
        em.close();
    }

    /**
     * A class-level override of an attribute inherited from a
     * MappedSuperclass must not reach the sibling entities, which share
     * the superclass' field metadata.
     */
    public void testInheritedOverrideDoesNotLeakToSibling() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        // resolve the overriding entity first, so that a leaking override
        // would already be in place for the plain one
        em.persist(new ConvertInheritedEntity("i1", "1.2.3"));
        em.persist(new ConvertPlainInheritedEntity("i2", "4.5.6"));
        em.getTransaction().commit();
        em.clear();
        emf.getCache().evictAll();

        assertEquals("1#2#3", em.createNativeQuery(
            "SELECT street FROM CONV_INHERIT_CONV WHERE id = 'i1'")
            .getSingleResult());
        assertEquals("4.5.6", em.createNativeQuery(
            "SELECT street FROM CONV_INHERIT_PLAIN WHERE id = 'i2'")
            .getSingleResult());

        assertEquals("1_2_3",
            em.find(ConvertInheritedEntity.class, "i1").getStreet());
        assertEquals("4.5.6",
            em.find(ConvertPlainInheritedEntity.class, "i2").getStreet());
        em.close();
    }
}
