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
package org.apache.openjpa.persistence.embed;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestEmbeddable extends SingleEMFTestCase {
   
    public int numEmbeddables = 1;
    public int numBasicTypes = 1;
    public int ID = 1;

    public void setUp() {
        setUp(Embed.class, Embed_Coll_Embed.class, Embed_Coll_Integer.class, 
            Embed_Embed.class, Embed_Embed_ToMany.class, Embed_ToMany.class, 
            Embed_ToOne.class, EntityA_Coll_Embed_ToOne.class, 
            EntityA_Coll_String.class, EntityA_Embed_Coll_Embed.class, 
            EntityA_Embed_Coll_Integer.class, EntityA_Embed_Embed.class, 
            EntityA_Embed_Embed_ToMany.class, EntityA_Embed_ToMany.class, 
            EntityA_Embed_ToOne.class, EntityB1.class, 
            EntityA_Coll_Embed_Embed.class,
            CLEAR_TABLES);
    }
    
    public void testEntityA_Coll_String() {
        createEntityA_Coll_String();
        queryEntityA_Coll_String();
        findEntityA_Coll_String();
    }

    public void testEntityA_Embed_ToOne() {
        createEntityA_Embed_ToOne();
        queryEntityA_Embed_ToOne();
        findEntityA_Embed_ToOne();
    }

    public void testEntityA_Coll_Embed_ToOne() {
        createEntityA_Coll_Embed_ToOne();
        queryEntityA_Coll_Embed_ToOne();
        findEntityA_Coll_Embed_ToOne();
    }

    public void testEntityA_Embed_ToMany() {
        createEntityA_Embed_ToMany();
        queryEntityA_Embed_ToMany();
        findEntityA_Embed_ToMany();
    }

    public void testEntityA_Embed_Embed_ToMany() {
        createEntityA_Embed_Embed_ToMany();
        queryEntityA_Embed_Embed_ToMany();
        findEntityA_Embed_Embed_ToMany();
    }

    public void testEntityA_Embed_Coll_Integer() {
        createEntityA_Embed_Coll_Integer();
        queryEntityA_Embed_Coll_Integer();
        findEntityA_Embed_Coll_Integer();
    }

    public void testEntityA_Embed_Embed() {
        createEntityA_Embed_Embed();
        queryEntityA_Embed_Embed();
        findEntityA_Embed_Embed();
    }

    public void atestEntityA_Coll_Embed_Embed() {
        createEntityA_Coll_Embed_Embed();
        queryEntityA_Coll_Embed_Embed();
        findEntityA_Coll_Embed_Embed();
    }

    public void testEntityA_Embed_Coll_Embed() {
        createEntityA_Embed_Coll_Embed();
        queryEntityA_Embed_Coll_Embed();
        findEntityA_Embed_Coll_Embed();
    }

    /*
     * Create EntityA_Coll_String
     */
    public void createEntityA_Coll_String() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_String(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_String(EntityManager em, int id) {
        EntityA_Coll_String a = new EntityA_Coll_String();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numBasicTypes; i++)
            a.addNickName("nickName_" + id + i);
        em.persist(a);
    }

    /*
     * Create EntityA_Embed_ToOne
     */
    public void createEntityA_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_ToOne(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_ToOne(EntityManager em, int id) {
        EntityA_Embed_ToOne a = new EntityA_Embed_ToOne();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_ToOne embed = createEmbed_ToOne(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_ToOne createEmbed_ToOne(EntityManager em, int id) {
        Embed_ToOne embed = new Embed_ToOne();
        embed.setName1("name1");
        embed.setName2("name2");
        embed.setName3("name3");
        EntityB1 b = new EntityB1();
        b.setId(id);
        b.setName("b" + id);
        embed.setEntityB(b);
        em.persist(b);
        return embed;
    }

    /*
     * Create EntityA_Coll_Embed_ToOne
     */
    public void createEntityA_Coll_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_Embed_ToOne(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_Embed_ToOne(EntityManager em, int id) {
        EntityA_Coll_Embed_ToOne a = new EntityA_Coll_Embed_ToOne();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed_ToOne embed = createEmbed_ToOne(em, i+id);
            EntityB1 b = new EntityB1();
            b.setId(id + i);
            b.setName("b" + id + i);
            a.addEmbed1ToOne(embed);
        }
        em.persist(a);
    }

    /*
     * Create EntityA_Embed_ToMany
     */
    public void createEntityA_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_ToMany(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_ToMany(EntityManager em, int id) {
        EntityA_Embed_ToMany a = new EntityA_Embed_ToMany();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_ToMany embed = createEmbed_ToMany(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_ToMany createEmbed_ToMany(EntityManager em, int id) {
        Embed_ToMany embed = new Embed_ToMany();
        embed.setName1("name1");
        embed.setName2("name2");
        embed.setName3("name3");
        for (int i = 0; i < numEmbeddables; i++) {
            EntityB1 b = new EntityB1();
            b.setId(id + i);
            b.setName("b" + id + i);
            embed.addEntityB(b);
            em.persist(b);
        }
        return embed;
    }

   /*
     * Create EntityA_Embed_Embed_ToMany
     */
    public void createEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Embed_ToMany(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Embed_ToMany(EntityManager em, int id) {
        EntityA_Embed_Embed_ToMany a = new EntityA_Embed_Embed_ToMany();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Embed_ToMany embed = createEmbed_Embed_ToMany(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Embed_ToMany createEmbed_Embed_ToMany(EntityManager em, int id) {
        Embed_Embed_ToMany embed = new Embed_Embed_ToMany();
        embed.setIntVal1(1);
        embed.setIntVal2(2);
        embed.setIntVal3(3);
        Embed_ToMany embed_ToMany = createEmbed_ToMany(em, id);
        embed.setEmbed(embed_ToMany);
        return embed;
    }
    
    /*
     * Create EntityA_Embed_Coll_Integer
     */
    public void createEntityA_Embed_Coll_Integer() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Coll_Integer(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Coll_Integer(EntityManager em, int id) {
        EntityA_Embed_Coll_Integer a = new EntityA_Embed_Coll_Integer();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Coll_Integer embed = createEmbed_Coll_Integer(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Coll_Integer createEmbed_Coll_Integer(EntityManager em, int id) {
        Embed_Coll_Integer embed = new Embed_Coll_Integer();
        embed.setIntVal1(id*10 + 1);
        embed.setIntVal2(id*10 + 2);
        embed.setIntVal3(id*10 + 3);
        for (int i = 0; i < numBasicTypes; i++) {
            embed.addOtherIntVal(id * 100 + i);
        }
        return embed;
    }

    /*
     * Create EntityA_Embed_Embed
     */
    public void createEntityA_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Embed(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Embed(EntityManager em, int id) {
        EntityA_Embed_Embed a = new EntityA_Embed_Embed();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Embed embed = createEmbed_Embed(em, id, 0);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Embed createEmbed_Embed(EntityManager em, int id, int idx) {
        Embed_Embed embed = new Embed_Embed();
        embed.setIntVal1(id * 100 + idx * 10 + 1);
        embed.setIntVal2(id * 100 + idx * 10 + 2);
        embed.setIntVal3(id * 100 + idx * 10 + 3);
        Embed embed1 = createEmbed(id, idx);
        embed.setEmbed(embed1);
        return embed;
    }

    public Embed createEmbed(int id, int idx) {
        Embed embed = new Embed();
        embed.setIntVal1(id * 100 + idx * 10 + 4);
        embed.setIntVal2(id * 100 + idx * 10 + 5);
        embed.setIntVal3(id * 100 + idx * 10 + 6);
        return embed;
    }

    /*
     * Create EntityA_Coll_Embed_Embed
     */
    public void createEntityA_Coll_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_Embed_Embed(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_Embed_Embed(EntityManager em, int id) {
        EntityA_Coll_Embed_Embed a = new EntityA_Coll_Embed_Embed();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed_Embed embed = createEmbed_Embed(em, id, i);
            a.addEmbed(embed);
        }
        em.persist(a);
    }

    /*
     * Create EntityA_Embed_Coll_Embed
     */
    public void createEntityA_Embed_Coll_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Coll_Embed(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Coll_Embed(EntityManager em, int id) {
        EntityA_Embed_Coll_Embed a = new EntityA_Embed_Coll_Embed();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Coll_Embed embed = createEmbed_Coll_Embed(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Coll_Embed createEmbed_Coll_Embed(EntityManager em, int id) {
        Embed_Coll_Embed embed = new Embed_Coll_Embed();
        embed.setIntVal1(id * 10 + 1);
        embed.setIntVal2(id * 10 + 2);
        embed.setIntVal3(id * 10 + 3);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed embed1 = createEmbed(id, i);
            embed.addEmbed(embed1);
        }
        return embed;
    }

    /*
     * Find EntityA_Coll_String
     */
    public void findEntityA_Coll_String() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_String a = em.find(EntityA_Coll_String.class, ID);
        checkEntityA_Coll_String(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_ToOne
     */
    public void findEntityA_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_ToOne a = em.find(EntityA_Embed_ToOne.class, ID);
        checkEntityA_Embed_ToOne(a);
        em.close();
    }

    /*
     * Find EntityA_Coll_Embed_ToOne
     */
    public void findEntityA_Coll_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_Embed_ToOne a = em.find(EntityA_Coll_Embed_ToOne.class, ID);
        checkEntityA_Coll_Embed_ToOne(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_ToMany
     */
    public void findEntityA_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_ToMany a = em.find(EntityA_Embed_ToMany.class, ID);
        checkEntityA_Embed_ToMany(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Embed_ToMany
     */
    public void findEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Embed_ToMany a = em.find(EntityA_Embed_Embed_ToMany.class, ID);
        checkEntityA_Embed_Embed_ToMany(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Coll_Integer
     */
    public void findEntityA_Embed_Coll_Integer() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Coll_Integer a = em.find(EntityA_Embed_Coll_Integer.class, ID);
        checkEntityA_Embed_Coll_Integer(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Embed
     */
    public void findEntityA_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Embed a = em.find(EntityA_Embed_Embed.class, ID);
        checkEntityA_Embed_Embed(a);
        em.close();
    }

    /*
     * Find EntityA_Coll_Embed_Embed
     */
    public void findEntityA_Coll_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_Embed_Embed a = em.find(EntityA_Coll_Embed_Embed.class, ID);
        checkEntityA_Coll_Embed_Embed(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Coll_Embed
     */
    public void findEntityA_Embed_Coll_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Coll_Embed a = em.find(EntityA_Embed_Coll_Embed.class, ID);
        checkEntityA_Embed_Coll_Embed(a);
        em.close();
    }

    /*
     * check EntityA_Coll_String
     */
    public void checkEntityA_Coll_String(EntityA_Coll_String a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Set<String> nickNames = a.getNickNames();
        for (String nickName : nickNames)
            assertEquals("nickName_" + id + "0", nickName);
    }

    /*
     * check EntityA_Embed_ToOne
     */
    public void checkEntityA_Embed_ToOne(EntityA_Embed_ToOne a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_ToOne embed = a.getEmbed();
        checkEmbed_ToOne(embed);
    }

    /*
     * check EntityA_Coll_Embed_ToOne
     */
    public void checkEntityA_Coll_Embed_ToOne(EntityA_Coll_Embed_ToOne a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Set<Embed_ToOne> embeds = a.getEmbed1ToOnes();
        for (Embed_ToOne embed : embeds)
            checkEmbed_ToOne(embed);
    }

    public void checkEmbed_ToOne(Embed_ToOne embed) {
        String name1 = embed.getName1();
        String name2 = embed.getName2();
        String name3 = embed.getName3();
        assertEquals("name1", name1);
        assertEquals("name2", name2);
        assertEquals("name3", name3);
        EntityB1 b = embed.getEntityB();
        assertEquals(1, b.getId());
        assertEquals("b" + b.getId(), b.getName());
    }

    /*
     * check EntityA_Embed_ToMany
     */
    public void checkEntityA_Embed_ToMany(EntityA_Embed_ToMany a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_ToMany embed = a.getEmbed();
        checkEmbed_ToMany(embed);
    }

    public void checkEmbed_ToMany(Embed_ToMany embed) {
        String name1 = embed.getName1();
        String name2 = embed.getName2();
        String name3 = embed.getName3();
        assertEquals("name1", name1);
        assertEquals("name2", name2);
        assertEquals("name3", name3);
        List<EntityB1> bs = embed.getEntityBs();
        for (EntityB1 b : bs) {
            assertEquals(1, b.getId());
            assertEquals("b" + b.getId() + "0", b.getName());
        }
    }

    /*
     * check EntityA_Embed_Embed_ToMany
     */
    public void checkEntityA_Embed_Embed_ToMany(EntityA_Embed_Embed_ToMany a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Embed_ToMany embed = a.getEmbed();
        checkEmbed_Embed_ToMany(embed);
    }
    
    public void checkEmbed_Embed_ToMany(Embed_Embed_ToMany embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(1, intVal1);
        assertEquals(2, intVal2);
        assertEquals(3, intVal3);
        Embed_ToMany embed1 = embed.getEmbed();
        checkEmbed_ToMany(embed1);
    }
    
    /*
     * check EntityA_Embed_Coll_Integer
     */
    public void checkEntityA_Embed_Coll_Integer(EntityA_Embed_Coll_Integer a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Coll_Integer embed = a.getEmbed();
        checkEmbed_Integers(embed);
    }

    public void checkEmbed_Integers(Embed_Coll_Integer embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(11, intVal1);
        assertEquals(12, intVal2);
        assertEquals(13, intVal3);
        Set<Integer> intVals = embed.getOtherIntVals();
        for (Integer intVal : intVals) {
            assertEquals(100, intVal.intValue());
        }
    }

    /*
     * check EntityA_Embed_Embed
     */
    public void checkEntityA_Embed_Embed(EntityA_Embed_Embed a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Embed embed = a.getEmbed();
        checkEmbed_Embed(embed);
    }

    public void checkEmbed_Embed(Embed_Embed embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(101, intVal1);
        assertEquals(102, intVal2);
        assertEquals(103, intVal3);
        Embed embed1 = embed.getEmbed();
        checkEmbed(embed1);
    }

    public void checkEmbed(Embed embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(104, intVal1);
        assertEquals(105, intVal2);
        assertEquals(106, intVal3);
    }

    /*
     * check EntityA_Coll_Embed_Embed
     */
    public void checkEntityA_Coll_Embed_Embed(EntityA_Coll_Embed_Embed a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        List<Embed_Embed> embeds = a.getEmbeds();
        for (Embed_Embed embed : embeds)
            checkEmbed_Embed(embed);
    }

    /*
     * check EntityA_Embed_Coll_Embed
     */
    public void checkEntityA_Embed_Coll_Embed(EntityA_Embed_Coll_Embed a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Coll_Embed embed = a.getEmbed();
        checkEmbed_Coll_Embed(embed);
    }

    public void checkEmbed_Coll_Embed(Embed_Coll_Embed embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(11, intVal1);
        assertEquals(12, intVal2);
        assertEquals(13, intVal3);
        List<Embed> embeds = embed.getEmbeds();
        for (Embed embed1 : embeds)
            checkEmbed(embed1);
    }

    /*
     * Query EntityA_Coll_String
     */
    public void queryEntityA_Coll_String() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_String a");
        List<EntityA_Coll_String> as = q.getResultList();
        for (EntityA_Coll_String a : as) {
            checkEntityA_Coll_String(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_ToOne
     */
    public void queryEntityA_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_ToOne a");
        List<EntityA_Embed_ToOne> as = q.getResultList();
        for (EntityA_Embed_ToOne a : as) {
            checkEntityA_Embed_ToOne(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Coll_Embed_ToOne
     */
    public void queryEntityA_Coll_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_Embed_ToOne a");
        List<EntityA_Coll_Embed_ToOne> as = q.getResultList();
        for (EntityA_Coll_Embed_ToOne a : as) {
            checkEntityA_Coll_Embed_ToOne(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_ToMany
     */
    public void queryEntityA_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_ToMany a");
        List<EntityA_Embed_ToMany> as = q.getResultList();
        for (EntityA_Embed_ToMany a : as) {
            checkEntityA_Embed_ToMany(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Embed_ToMany
     */
    public void queryEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Embed_ToMany a");
        List<EntityA_Embed_Embed_ToMany> as = q.getResultList();
        for (EntityA_Embed_Embed_ToMany a : as) {
            checkEntityA_Embed_Embed_ToMany(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Coll_Integer
     */
    public void queryEntityA_Embed_Coll_Integer() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Coll_Integer a");
        List<EntityA_Embed_Coll_Integer> as = q.getResultList();
        for (EntityA_Embed_Coll_Integer a : as) {
            checkEntityA_Embed_Coll_Integer(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Embed
     */
    public void queryEntityA_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Embed a");
        List<EntityA_Embed_Embed> as = q.getResultList();
        for (EntityA_Embed_Embed a : as) {
            checkEntityA_Embed_Embed(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Coll_Embed_Embed
     */
    public void queryEntityA_Coll_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_Embed_Embed a");
        List<EntityA_Coll_Embed_Embed> as = q.getResultList();
        for (EntityA_Coll_Embed_Embed a : as) {
            checkEntityA_Coll_Embed_Embed(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Coll_Embed
     */
    public void queryEntityA_Embed_Coll_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Coll_Embed a");
        List<EntityA_Embed_Coll_Embed> as = q.getResultList();
        for (EntityA_Embed_Coll_Embed a : as) {
            checkEntityA_Embed_Coll_Embed(a);
        }
        tran.commit();
        em.close();
    }
}
