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
package org.apache.openjpa.slice;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

/**
 * Tests basic create, read, update and delete operations.
 * 
 * @author Pinaki Poddar 
 *
 */
public class TestBasic extends SliceTestCase {
    /**
     * Specify persistence unit name as System property <code>-Dunit</code> or
     * use the default value as <code>"slice"</code>.
     */
    protected String getPersistenceUnitName() {
        return System.getProperty("unit","slice");
    }


    public void setUp() throws Exception {
        super.setUp(PObject.class, Person.class, Address.class, Country.class, 
        	CLEAR_TABLES);
    }

    /**
     * Persist N independent objects.
     */
    List<PObject> create(int N) {
        List<PObject> pcs = new ArrayList<PObject>();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < N; i++) {
            PObject pc = new PObject();
            pcs.add(pc);
            em.persist(pc);
            pc.setValue(i);
        }
        em.getTransaction().commit();
        em.clear();
        return pcs;
    }

    /**
     * Create a single object.
     */
    PObject create() {
        return create(1).get(0);
    }

    /**
     * Delete a single object by EntityManager.remove()
     */
    public void testDelete() {
        int N = 10;
        create(N);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        int before = count(PObject.class);
        List all = em.createQuery("SELECT p FROM PObject p").getResultList();
        assertFalse(all.isEmpty());
        em.remove(all.get(0));
        em.getTransaction().commit();

        int after = count(PObject.class);
        assertEquals(before - 1, after);
    }

    /**
     * Delete in bulk by query.
     */
    public void testBulkDelete() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        int c = count(PObject.class);
        int d = em.createQuery("DELETE FROM PObject p").executeUpdate();
        assertEquals(c, d);
        em.getTransaction().commit();
        c = count(PObject.class);
        assertEquals(0, c);

    }

    /**
     * Store and find the same object.
     */
    public void testFind() {
        PObject pc = create();
        int value = pc.getValue();

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        PObject pc2 = em.find(PObject.class, pc.getId());
        assertNotNull(pc2);
        assertNotEquals(pc, pc2);
        assertEquals(pc.getId(), pc2.getId());
        assertEquals(value, pc2.getValue());
    }

    public void testPersistIndependentObjects() {
        int before = count(PObject.class);
        EntityManager em = emf.createEntityManager();
        int N = 2;
        em.getTransaction().begin();
        for (int i = 0; i < N; i++)
            em.persist(new PObject());
        em.getTransaction().commit();
        em.clear();
        int after = count(PObject.class);
        assertEquals(before + N, after);
    }

    public void testPersistConnectedObjectGraph() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Person p1 = new Person("A");
        Person p2 = new Person("B");
        Person p3 = new Person("C");
        Address a1 = new Address("Rome", 12345);
        Address a2 = new Address("San Francisco", 23456);
        Address a3 = new Address("New York", 34567);
        Country c1 = em.find(Country.class, "Italy");
        if (c1 == null) {
        	c1 = new Country();
        	c1.setName("Italy");
        	em.persist(c1);
        }
    	a1.setCountry(c1);
        p1.setAddress(a1);
        p2.setAddress(a2);
        p3.setAddress(a3);

        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        em.getTransaction().commit();

        em.clear();

        em = emf.createEntityManager();
        em.getTransaction().begin();
        List<Person> persons =
                em.createQuery("SELECT p FROM Person p WHERE p.name=?1")
                        .setParameter(1, "A").getResultList();
        List<Address> addresses =
                em.createQuery("SELECT a FROM Address a").getResultList();
        for (Address pc : addresses) {
            assertNotNull(pc.getCity());
            assertNotNull(pc.getOwner().getName());
        }
        for (Person pc : persons) {
            assertNotNull(pc.getName());
            assertNotNull(pc.getAddress().getCity());
        }
        em.getTransaction().rollback();
    }

    /**
     * Merge only works if the distribution policy assigns the correct slice
     * from which the instance was fetched.
     */
    public void testMerge() {
        PObject pc = create(1).get(0);
        int value = pc.getValue();
        pc.setValue(value + 1);
        assertNotNull(pc);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        PObject pc2 = em.merge(pc);
        em.getTransaction().commit();
        em.clear();

        assertNotNull(pc2);
        assertNotEquals(pc, pc2);
        assertEquals(pc.getId(), pc2.getId());
        assertEquals(value + 1, pc2.getValue());
    }
    
    public void testPersistReplicatedObjects() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        String[] names = {"USA", "India", "China"};
        for (String name : names) {
        	Country country = new Country();
        	country.setName(name);
        	em.persist(country);
        }
        em.getTransaction().commit();
        assertEquals(names.length, count(Country.class));
        
        Country india = em.find(Country.class, "India");
        assertNotNull(india);
        assertEquals("India", india.getName());
    }
    
    public void testUpdateReplicatedObjects() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        String[] names = {"USA", "India", "China"};
        long[] population = {300,1200,1400};
        for (int i = 0; i < names.length; i++) {
        	Country country = new Country();
        	country.setName(names[i]);
        	country.setPopulation(population[i]);
        	em.persist(country);
        }
        em.getTransaction().commit();
        
        assertEquals(names.length, count(Country.class));
        Country india = em.find(Country.class, "India");

        assertNotNull(india);
        assertEquals("India", india.getName());
        india.setPopulation(1201);
        em.getTransaction().begin();
        em.merge(india);
        em.getTransaction().commit();
    }

}
