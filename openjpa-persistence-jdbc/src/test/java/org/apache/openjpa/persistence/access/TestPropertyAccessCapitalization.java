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
package org.apache.openjpa.persistence.access;

import java.util.Random;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestPropertyAccessCapitalization extends SingleEMFTestCase {
    public void setUp() {
        setUp(DROP_TABLES, PropertyAccessCapitalization.class, PropertyAccessCapitalizationOldBehavior.class);
    }

    public void testCorrectCapitalization() {
        EntityManager em = emf.createEntityManager();

        PropertyAccessCapitalization entity = new PropertyAccessCapitalization();
        Random r = new Random();
        entity.setId(r.nextInt());
        entity.setWord(r.nextInt());
        entity.setaWord(r.nextInt());
        entity.setAaWord(r.nextInt());
        entity.setAaaWord(r.nextInt());
        entity.setCAPITAL(r.nextInt());
        entity.setaCAPITAL(r.nextInt());
        entity.setAnother(r.nextInt());
        entity.setA1(r.nextInt());
        entity.setB1(r.nextInt());
        entity.setA(r.nextInt());
        entity.setB(r.nextInt());
        entity.setaBoolean(true);
        entity.setBBoolean(true);
        entity.setBOOLEAN(true);
        entity.setBool(true);

        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();

        em.clear();
        PropertyAccessCapitalization persistentEntity = em.find(PropertyAccessCapitalization.class, entity.getId());
        assertEquals(entity, persistentEntity);
    }

    public void testOldCapitalization() {
        EntityManager em = emf.createEntityManager();

        PropertyAccessCapitalizationOldBehavior entity = new PropertyAccessCapitalizationOldBehavior();
        Random r = new Random();
        entity.setId(r.nextInt());
        entity.setWord(r.nextInt());
        entity.setAWord(r.nextInt());
        entity.setAaWord(r.nextInt());
        entity.setAaaWord(r.nextInt());
        entity.setCAPITAL(r.nextInt());
        entity.setACAPITAL(r.nextInt());
        entity.setAnother(r.nextInt());
        entity.setA1(r.nextInt());
        entity.setB1(r.nextInt());
        entity.setA(r.nextInt());
        entity.setB(r.nextInt());
        entity.setABoolean(true);
        entity.setBBoolean(true);
        entity.setBOOLEAN(true);
        entity.setBool(true);

        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();

        em.clear();
        PropertyAccessCapitalizationOldBehavior persistentEntity =
            em.find(PropertyAccessCapitalizationOldBehavior.class, entity.getId());
        assertEquals(entity, persistentEntity);
    }
}
