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


public class TestEmbeddableXml extends SingleEMFTestCase {
   
    public int numEmbeddables = 1;
    public int numBasicTypes = 1;
    public int ID = 1;
    
    public void setUp() {
        setUp(CLEAR_TABLES);
    }
    
    @Override
    protected String getPersistenceUnitName() {
        return "embed-pu";
    }
    
    public void testEntityA_Coll_StringXml() {
        createEntityA_Coll_StringXml();
        queryEntityA_Coll_StringXml();
        findEntityA_Coll_StringXml();
    }

    public void testEntityA_Coll_Embed_Embed() {
        createEntityA_Coll_Embed_EmbedXml();
        queryEntityA_Coll_Embed_EmbedXml();
        findEntityA_Coll_Embed_EmbedXml();
    }

    /*
     * Create EntityA_Coll_StringXml
     */
    public void createEntityA_Coll_StringXml() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_StringXml(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_StringXml(EntityManager em, int id) {
        EntityA_Coll_StringXml a = new EntityA_Coll_StringXml();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numBasicTypes; i++)
            a.addNickName("nickName_" + id + i);
        em.persist(a);
    }

    /*
     * Create EntityA_Coll_Embed_EmbedXml
     */
    public void createEntityA_Coll_Embed_EmbedXml() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_Embed_EmbedXml(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_Embed_EmbedXml(EntityManager em, int id) {
        EntityA_Coll_Embed_EmbedXml a = new EntityA_Coll_Embed_EmbedXml();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed_EmbedXml embed = createEmbed_EmbedXml(em, id, i);
            a.addEmbed(embed);
        }
        em.persist(a);
    }

    public Embed_EmbedXml createEmbed_EmbedXml(EntityManager em, int id, int idx) {
        Embed_EmbedXml embed = new Embed_EmbedXml();
        embed.setIntVal1(id * 100 + idx * 10 + 1);
        embed.setIntVal2(id * 100 + idx * 10 + 2);
        embed.setIntVal3(id * 100 + idx * 10 + 3);
        EmbedXml embed1 = createEmbedXml(id, idx);
        embed.setEmbed(embed1);
        return embed;
    }

    public EmbedXml createEmbedXml(int id, int idx) {
        EmbedXml embed = new EmbedXml();
        embed.setIntVal1(id * 100 + idx * 10 + 4);
        embed.setIntVal2(id * 100 + idx * 10 + 5);
        embed.setIntVal3(id * 100 + idx * 10 + 6);
        return embed;
    }

    /*
     * Find EntityA_Coll_StringXml
     */
    public void findEntityA_Coll_StringXml() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_StringXml a = em.find(EntityA_Coll_StringXml.class, ID);
        checkEntityA_Coll_StringXml(a);
        em.close();
    }

    /*
     * Find EntityA_Coll_Embed_EmbedXml
     */
    public void findEntityA_Coll_Embed_EmbedXml() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_Embed_EmbedXml a = em.find(EntityA_Coll_Embed_EmbedXml.class, ID);
        checkEntityA_Coll_Embed_EmbedXml(a);
        em.close();
    }

    /*
     * check EntityA_Coll_String
     */
    public void checkEntityA_Coll_StringXml(EntityA_Coll_StringXml a) {
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
     * check EntityA_Coll_Embed_EmbedXml
     */
    public void checkEntityA_Coll_Embed_EmbedXml(EntityA_Coll_Embed_EmbedXml a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        List<Embed_EmbedXml> embeds = a.getEmbeds();
        for (Embed_EmbedXml embed : embeds)
            checkEmbed_EmbedXml(embed);
    }

    public void checkEmbed_EmbedXml(Embed_EmbedXml embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(101, intVal1);
        assertEquals(102, intVal2);
        assertEquals(103, intVal3);
        EmbedXml embed1 = embed.getEmbed();
        checkEmbedXml(embed1);
    }

    public void checkEmbedXml(EmbedXml embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(104, intVal1);
        assertEquals(105, intVal2);
        assertEquals(106, intVal3);
    }

    /*
     * Query EntityA_Coll_StringXml
     */
    public void queryEntityA_Coll_StringXml() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_StringXml a");
        List<EntityA_Coll_StringXml> as = q.getResultList();
        for (EntityA_Coll_StringXml a : as) {
            checkEntityA_Coll_StringXml(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Coll_Embed_Embed
     */
    public void queryEntityA_Coll_Embed_EmbedXml() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_Embed_EmbedXml a");
        List<EntityA_Coll_Embed_EmbedXml> as = q.getResultList();
        for (EntityA_Coll_Embed_EmbedXml a : as) {
            checkEntityA_Coll_Embed_EmbedXml(a);
        }
        tran.commit();
        em.close();
    }
}
