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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestEmbeddable extends SingleEMFTestCase {
    public int ID = 1;

    public void setUp() {
        setUp(Embed_Embed_ToMany.class, Embed_ToMany.class, 
            EntityA_Embed_Embed_ToMany.class, EntityB1.class, 
            CLEAR_TABLES);
    }

    public void testEntityA_Embed_Embed_ToMany() {
        createEntityA_Embed_Embed_ToMany();
        queryEntityA_Embed_Embed_ToMany();
        findEntityA_Embed_Embed_ToMany();
    }

    public Embed_ToMany createEmbed_ToMany(EntityManager em, int id) {
        Embed_ToMany embed = new Embed_ToMany();
        embed.setName1("name1");
        embed.setName2("name2");
        embed.setName3("name3");
        for (int i = 0; i < 1; i++) {
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
     * Find EntityA_Embed_Embed_ToMany
     */
    public void findEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Embed_ToMany a = em.find(EntityA_Embed_Embed_ToMany.class, ID);
        checkEntityA_Embed_Embed_ToMany(a);
        em.close();
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


}
