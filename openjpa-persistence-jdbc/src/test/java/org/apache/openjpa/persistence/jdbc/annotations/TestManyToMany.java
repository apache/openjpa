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
package org.apache.openjpa.persistence.jdbc.annotations;

import java.util.Set;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test for m-m
 *
 * @author Steve Kim
 */
public class TestManyToMany
    extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(AnnoTest1.class, AnnoTest2.class, Flat1.class, CLEAR_TABLES);
    }

    public void testManyToMany() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AnnoTest1 pc = new AnnoTest1(4);
        em.persist(pc);
        AnnoTest2 pc2;
        for (int i = 0; i < 3; i++) {
            pc2 = new AnnoTest2(5 + i, "foo" + i);
            pc.getManyMany().add(pc2);
            em.persist(pc2);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        pc = em.find(AnnoTest1.class, 4L);
        Set<AnnoTest2> many = pc.getManyMany();
        assertEquals(3, many.size());
        for (AnnoTest2 manyPc2 : many) {
            switch ((int) manyPc2.getPk1()) {
                case 5:
                    assertEquals("foo0", manyPc2.getPk2());
                    break;
                case 6:
                    assertEquals("foo1", manyPc2.getPk2());
                    break;
                case 7:
                    assertEquals("foo2", manyPc2.getPk2());
                    break;
                default:
                    fail("bad pk:" + manyPc2.getPk1());
            }
        }
        em.close();
    }

    public void testInverseOwnerManyToMany() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AnnoTest1 pc = new AnnoTest1(4);
        em.persist(pc);
        AnnoTest2 pc2;
        for (int i = 0; i < 3; i++) {
            pc2 = new AnnoTest2(5 + i, "foo" + i);
            pc2.getManyMany().add(pc);
            em.persist(pc2);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        pc = em.find(AnnoTest1.class, 4L);
        Set<AnnoTest2> many = pc.getInverseOwnerManyMany();
        assertEquals(3, many.size());
        for (AnnoTest2 manyPc2 : many) {
            assertTrue(manyPc2.getManyMany().contains(pc));
            switch ((int) manyPc2.getPk1()) {
                case 5:
                    assertEquals("foo0", manyPc2.getPk2());
                    break;
                case 6:
                    assertEquals("foo1", manyPc2.getPk2());
                    break;
                case 7:
                    assertEquals("foo2", manyPc2.getPk2());
                    break;
                default:
                    fail("bad pk:" + manyPc2.getPk1());
            }
        }
        em.close();
    }
}
