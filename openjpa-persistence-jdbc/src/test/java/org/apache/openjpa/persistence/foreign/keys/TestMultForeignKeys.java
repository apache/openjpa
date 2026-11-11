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
package org.apache.openjpa.persistence.foreign.keys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test multiple keys that have the same Entity as
 * the type.
 */
class TestMultForeignKeys extends SQLListenerTestCase {
    @BeforeEach
    public void setUp() {
        setUp(SimpleEntity.class,
            ContainsMultSimpleEntity.class,
            TopLevel.class,
            DROP_TABLES);
        assertNotNull(emf);

        create();
    }

    @Test
    void created() {

        EntityManager em = emf.createEntityManager();

        TopLevel tl = em.find(TopLevel.class, 1);
        assertNotNull(tl);

        ArrayList<ContainsMultSimpleEntity> contMultSEColl =
                (ArrayList<ContainsMultSimpleEntity>)tl.getContMultSEColl();
        assertNotNull(contMultSEColl);
        assertEquals(1, contMultSEColl.size());

        ContainsMultSimpleEntity contMultSE = contMultSEColl.get(0);
        assertNotNull(contMultSE);

        SimpleEntity se1 = contMultSE.getSimpleEntity1();
        assertNotNull(se1);
        assertEquals(1, se1.getId());
        assertEquals("name1", se1.getName());

        SimpleEntity se2 = contMultSE.getSimpleEntity2();
        assertNotNull(se2);
        assertEquals(2, se2.getId());
        assertEquals("name2", se2.getName());

        SimpleEntity se3 = contMultSE.getSimpleEntity3();
        assertNotNull(se3);
        assertEquals(3, se3.getId());
        assertEquals("name3", se3.getName());

        em.close();
    }

    private void create() {
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        em.getTransaction().begin();

        ContainsMultSimpleEntity contMultSE = new ContainsMultSimpleEntity();

        SimpleEntity se3 = new SimpleEntity();
        se3.setId(3);
        se3.setName("name3");

        SimpleEntity se1 = new SimpleEntity();
        se1.setId(1);
        se1.setName("name1");

        SimpleEntity se2 = new SimpleEntity();
        se2.setId(2);
        se2.setName("name2");

        contMultSE.setSimpleEntity3(se3);
        contMultSE.setSimpleEntity1(se1);
        contMultSE.setSimpleEntity2(se2);

        Collection<ContainsMultSimpleEntity> contMultSEColl = new ArrayList<>();
        contMultSEColl.add(contMultSE);

        TopLevel tl = new TopLevel();
        tl.setId(1);
        tl.setContMultSEColl(contMultSEColl);

        em.persist(tl);

        em.getTransaction().commit();
        em.close();
    }

}
