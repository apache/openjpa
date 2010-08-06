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
package org.apache.openjpa.persistence.generationtype;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestMultipleSchemaNames extends SingleEMFTestCase {
    @Override
    public void setUp() {
        setUp(Dog1.class, Dog2.class, DogTable.class, DogTable2.class,
                DogTable3.class, DogTable4.class);

        if(getDBDictionary(emf) instanceof MySQLDictionary) { 
            return;
        }
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        // cleanup database first
        Query qry = em.createQuery("select d from Dog1 d");
        List result = qry.getResultList();

        for (int index = 0; index < result.size(); index++) {
            Dog1 Obj = (Dog1) result.get(index);
            em.remove(Obj);
        }
        Query qry2 = em.createQuery("select d from Dog2 d");
        List result2 = qry2.getResultList();

        for (int index = 0; index < result2.size(); index++) {
            Dog2 Obj = (Dog2) result2.get(index);
            em.remove(Obj);
        }
        Query qry3 = em.createQuery("select d from DogTable d");
        List result3 = qry3.getResultList();

        for (int index = 0; index < result3.size(); index++) {
            DogTable Obj = (DogTable) result3.get(index);
            em.remove(Obj);
        }
        Query qry4 = em.createQuery("select d from DogTable2 d");
        List result4 = qry4.getResultList();

        for (int index = 0; index < result4.size(); index++) {
            DogTable2 Obj = (DogTable2) result4.get(index);
            em.remove(Obj);
        }

        Query qry5 = em.createQuery("select d from DogTable3 d");
        List result5 = qry5.getResultList();

        for (int index = 0; index < result5.size(); index++) {
            DogTable3 Obj = (DogTable3) result5.get(index);
            em.remove(Obj);
        }

        Query qry6 = em.createQuery("select d from DogTable4 d");
        List result6 = qry6.getResultList();

        for (int index = 0; index < result6.size(); index++) {
            DogTable4 Obj = (DogTable4) result6.get(index);
            em.remove(Obj);
        }

        Query delschema1 = em
                .createNativeQuery("delete from schema1.openjpa_sequence_table");
        delschema1.executeUpdate();
        Query delschema2 = em
                .createNativeQuery("delete from schema2.openjpa_sequence_table");
        delschema2.executeUpdate();
        Query delgentable = em.createNativeQuery("delete from schema1.id_gen1");
        delgentable.executeUpdate();
        Query delgentable2 = em
                .createNativeQuery("delete from schema2.id_gen2");
        delgentable2.executeUpdate();
        Query delgentable3 = em
                .createNativeQuery("delete from schema3g.id_gen3");
        delgentable3.executeUpdate();
        Query delgentable4 = em
                .createNativeQuery("delete from schema4g.id_gen4");
        delgentable4.executeUpdate();

        em.getTransaction().commit();
        em.close();

    }

    public void testGeneratedAUTO() {
        if(getDBDictionary(emf) instanceof MySQLDictionary) { 
            return;
        }
        
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager kem = OpenJPAPersistence.cast(em);
        em.getTransaction().begin();

        Dog1 dog1 = new Dog1();
        dog1.setName("helloDog1");
        dog1.setPrice(12000);

        em.persist(dog1);
        Dog1 dog1a = new Dog1();
        dog1a.setName("helloDog2");
        dog1a.setPrice(22000);
        em.persist(dog1a);
        // add dog2
        Dog2 dog2 = new Dog2();
        dog2.setName("helloDog3");
        dog2.setPrice(15000);
        em.persist(dog2);

        Dog2 dog2a = new Dog2();
        dog2a.setName("helloDog4");
        dog2a.setPrice(25000);
        em.persist(dog2a);
        em.getTransaction().commit();

        Dog1 dog1x = em.find(Dog1.class, kem.getObjectId(dog1));
        // Derby can't guarantee the order of the generated value, therefore,
        // we can't assert the id based on the order. For db2, we see the id 
        // value in the right order
        assertTrue(dog1x.getId2() == 1 || dog1x.getId2() == 2);
        assertEquals(dog1x.getName(), "helloDog1");
        dog1x.setName("Dog1");
        dog1x.setDomestic(true);
        Dog1 dog11 = em.find(Dog1.class, kem.getObjectId(dog1a));
        assertTrue(dog11.getId2() == 1 || dog11.getId2() == 2);
        assertEquals(dog11.getName(), "helloDog2");
        dog11.setName("Dog2");
        dog11.setDomestic(true);
        // update dog2
        Dog2 dog2x = em.find(Dog2.class, kem.getObjectId(dog2));
        assertTrue(dog2x.getId2() == 1 || dog2x.getId2() == 2);
        assertEquals(dog2x.getName(), "helloDog3");
        dog2x.setName("Dog3");
        dog2x.setDomestic(true);
        Dog2 dog21 = em.find(Dog2.class, kem.getObjectId(dog2a));
        assertTrue(dog21.getId2() == 1 || dog21.getId2() == 2);
        assertEquals(dog21.getName(), "helloDog4");
        dog21.setName("Dog4");
        dog21.setDomestic(true);

        // get the update dog name

        em.getTransaction().begin();
        Query qry1 = em.createQuery("select d from Dog1 d order by d.name");
        List result1 = qry1.getResultList();
        for (int index = 0; index < result1.size(); index++) {
            Dog1 dog4 = (Dog1) result1.get(index);
            int i = index + 1;
            assertTrue(dog4.getId2() == 1 || dog4.getId2() == 2);
            assertEquals(dog4.getName(), "Dog" + i);
        }

        Query qry2 = em.createQuery("select d from Dog2 d order by d.name");
        List result2 = qry2.getResultList();

        for (int index = 0; index < result2.size(); index++) {
            Dog2 dog5 = (Dog2) result2.get(index);
            assertTrue(dog5.getId2() == 1 || dog5.getId2() == 2);
            int j = index + 3;
            assertEquals(dog5.getName(), "Dog" + j);
        }

        em.getTransaction().commit();
        em.close();
    }

    public void testGeneratedTABLE() {
        if(getDBDictionary(emf) instanceof MySQLDictionary) { 
            return;
        }
        
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager kem = OpenJPAPersistence.cast(em);
        em.getTransaction().begin();

        DogTable dog1 = new DogTable();
        dog1.setName("helloDog1");
        dog1.setPrice(12000);

        em.persist(dog1);
        DogTable dog1a = new DogTable();
        dog1a.setName("helloDog2");
        dog1a.setPrice(22000);
        em.persist(dog1a);
        // add dog2
        DogTable2 dog2 = new DogTable2();
        dog2.setName("helloDog3");
        dog2.setPrice(15000);
        em.persist(dog2);

        DogTable2 dog2a = new DogTable2();
        dog2a.setName("helloDog4");
        dog2a.setPrice(25000);
        em.persist(dog2a);

        // add dog3
        DogTable3 dog3 = new DogTable3();
        dog3.setName("helloDog5");
        dog3.setPrice(15001);
        em.persist(dog3);

        DogTable3 dog3a = new DogTable3();
        dog3a.setName("helloDog6");
        dog3a.setPrice(25001);
        em.persist(dog3a);

        // add dog4
        DogTable4 dog4 = new DogTable4();
        dog4.setName("helloDog7");
        dog4.setPrice(15002);
        em.persist(dog4);

        DogTable4 dog4a = new DogTable4();
        dog4a.setName("helloDog8");
        dog4a.setPrice(25002);
        em.persist(dog4a);
        em.getTransaction().commit();        
        
        DogTable dog1x = em.find(DogTable.class, kem.getObjectId(dog1));
        assertTrue(dog1x.getId2() == 20 || dog1x.getId2() == 21);
        assertEquals(dog1x.getName(), "helloDog1");
        dog1x.setName("Dog1");
        dog1x.setDomestic(true);
        DogTable dog11 = em.find(DogTable.class, kem.getObjectId(dog1a));
        assertTrue(dog11.getId2() == 20 || dog11.getId2() == 21);
        assertEquals(dog11.getName(), "helloDog2");
        dog11.setName("Dog2");
        dog11.setDomestic(true);
        
        // update dog2
        DogTable2 dog2x = em.find(DogTable2.class, kem.getObjectId(dog2));
        assertTrue(dog2x.getId2() == 100 || dog2x.getId2() == 101);
        assertEquals(dog2x.getName(), "helloDog3");
        dog2x.setName("Dog3");
        dog2x.setDomestic(true);
        DogTable2 dog21 = em.find(DogTable2.class, kem.getObjectId(dog2a));
        assertTrue(dog21.getId2() == 100 || dog21.getId2() == 101);
        assertEquals(dog21.getName(), "helloDog4");
        dog21.setName("Dog4");
        dog21.setDomestic(true);

        // update dog3
        DogTable3 dog3x = em.find(DogTable3.class, kem.getObjectId(dog3));
        assertTrue(dog3x.getId2() == 100 || dog3x.getId2() == 101);
        assertEquals(dog3x.getName(), "helloDog5");
        dog3x.setName("Dog5");
        dog3x.setDomestic(true);
        DogTable3 dog31 = em.find(DogTable3.class, kem.getObjectId(dog3a));
        assertTrue(dog31.getId2() == 100 || dog31.getId2() == 101);
        assertEquals(dog31.getName(), "helloDog6");
        dog31.setName("Dog6");
        dog31.setDomestic(true);

        // update dog4
        DogTable4 dog4x = em.find(DogTable4.class, kem.getObjectId(dog4));
        assertTrue(dog4x.getId2() == 100 || dog4x.getId2() == 101);
        assertEquals(dog4x.getName(), "helloDog7");
        dog4x.setName("Dog7");
        dog4x.setDomestic(true);
        DogTable4 dog41 = em.find(DogTable4.class, kem.getObjectId(dog4a));
        assertTrue(dog41.getId2() == 100 || dog41.getId2() == 101);
        assertEquals(dog41.getName(), "helloDog8");
        dog41.setName("Dog8");
        dog41.setDomestic(true);

        // get the update dog name

        em.getTransaction().begin();
        Query qry1 = em.createQuery("select d from DogTable d order by d.name");
        List result1 = qry1.getResultList();
        for (int index = 0; index < result1.size(); index++) {
            DogTable dog1xx = (DogTable) result1.get(index);
            assertTrue(dog1xx.getId2() == 20 || dog1xx.getId2() == 21);
            int j = index + 1;
            assertEquals(dog1xx.getName(), "Dog" + j);

        }

        Query qry2 = em
                .createQuery("select d from DogTable2 d order by d.name");
        List result2 = qry2.getResultList();

        for (int index = 0; index < result2.size(); index++) {
            DogTable2 dog2xx = (DogTable2) result2.get(index);
            assertTrue(dog2xx.getId2() == 100 || dog2xx.getId2() == 101);
            int j = index + 3;
            assertEquals(dog2xx.getName(), "Dog" + j);
        }

        Query qry3 = em
                .createQuery("select d from DogTable3 d order by d.name");
        List result3 = qry3.getResultList();

        for (int index = 0; index < result3.size(); index++) {
            DogTable3 dog3xx = (DogTable3) result3.get(index);
            assertTrue(dog3xx.getId2() == 100 || dog3xx.getId2() == 101);
            int j = index + 5;
            assertEquals(dog3xx.getName(), "Dog" + j);
        }

        Query qry4 = em
                .createQuery("select d from DogTable4 d order by d.name");
        List result4 = qry4.getResultList();

        for (int index = 0; index < result4.size(); index++) {
            DogTable4 dog4xx = (DogTable4) result4.get(index);
            assertTrue(dog4xx.getId2() == 100 || dog4xx.getId2() == 101);
            int j = index + 7;
            assertEquals(dog4xx.getName(), "Dog" + j);
        }

        em.getTransaction().commit();
        em.close();
    }
} // end of TestMultipleSchemaNames
