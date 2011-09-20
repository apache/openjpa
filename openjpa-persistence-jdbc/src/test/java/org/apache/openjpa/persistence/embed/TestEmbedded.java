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

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestEmbedded extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(BaseEntity.class, Address.class, Geocode.class, NestedEmbeddable.class,
                CLEAR_TABLES);
    }

    public void testInsertEmbedded() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Address a = new Address();
        a.setStreetAddress("123 Main St");
        a.setCity("Chicago");
        a.setState("IL");
        a.setZip(60606);
        Geocode g = new Geocode();
        g.setLatitude(1.0f);
        g.setLongtitude(2.0f);
        a.setGeocode(g);
        em.persist(a);
        em.getTransaction().commit();
    }

    /*
     * This variation verifies that an embedded entity can be accessed after
     * being detached. An entity /w embedded is persisted and then queried. The
     * em is closed, detaching the entities, and then a getter is called on the
     * embeddeded. If the embedded is still attached (it should not be) an
     * IllegalStateException will be thrown.
     * 
     * JIRA Ref: OPENJPA-733 Authors: Chris Tillman, Jeremy Bauer
     */
    public void testDetachedQueryEmbedded() {
        Address a = new Address();
        a.setStreetAddress("456 Main St");
        a.setCity("New York");
        a.setState("NY");
        a.setZip(12955);
        Geocode g = new Geocode();
        g.setLatitude(1.0f);
        g.setLongtitude(2.0f);
        a.setGeocode(g);

        persistAddress(a);

        Address a2 =
            queryAddresses(
                "select address from Address address"
                    + " where address.streetAddress = '456 Main St'").get(0);

        assertEquals(a2.getGeocode().getLatitude(), 1.0f);
    }
    
    /**
     * JIRA Ref: OPENJPA-1226: Create and persist a NestedEmbeddable, then
     * update and merge the NestedEmbeddable in order to verify an 
     * 'ArgumentException' is not caused during the merge.
     */
    public void testNestedEmbeddable_UpdateExistingEmbed() {
        Address a = new Address();
        a.setStreetAddress("456 Main St");
        Geocode g = new Geocode();
        g.setLatitude(1.0f);
        NestedEmbeddable ne = new NestedEmbeddable();
        ne.setField1("test");
        g.setNestedEmbed(ne);
        a.setGeocode(g);

        persistAddress(a);

        g = a.getGeocode();       
        ne = g.getNestedEmbed();
        ne.setField1("test2");
        ne.setField2("test3");
        
        g.setNestedEmbed(ne);
        a.setGeocode(g);
        
        mergeAddress(a);      
 	
        a =
                 queryAddresses(
                "select address from Address address"
                    + " where address.streetAddress = '456 Main St'").get(0);
        
        ne = a.getGeocode().getNestedEmbed();
        
        assertEquals(ne.getField1(),"test2");
        assertEquals(ne.getField2(),"test3");
    }
     
    /**
     * JIRA Ref: OPENJPA-1226: Create a NestedEmbeddable, and add it to an
     * existing Address/Geocode, and then merge the NestedEmbeddable in
     * order to verify an 'ArgumentException' is not caused during the merge.
     */
    public void testNestedEmbeddable_AddNewEmbed() {
        Address a = new Address();
        a.setStreetAddress("456 Main St");
        Geocode g = new Geocode();
        g.setLatitude(1.0f);
        a.setGeocode(g);

        persistAddress(a);

        g = a.getGeocode();       

        NestedEmbeddable ne = new NestedEmbeddable();
        ne.setField1("test");
        
        g.setNestedEmbed(ne);       
        a.setGeocode(g);
        
        mergeAddress(a);      
 	
        a =
            queryAddresses(
                "select address from Address address"
                    + " where address.streetAddress = '456 Main St'").get(0);
        
        ne = a.getGeocode().getNestedEmbed();
        
        assertEquals(ne.getField1(),"test");
        assertEquals(ne.getField2(),null);
    }    

    private void persistAddress(Address address) {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();

        tx.begin();

        try {
            em.persist(address);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
        assertEquals(address.getGeocode().getLatitude(), 1.0f);
    }

    private void mergeAddress(Address address) {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();

        tx.begin();

        try {
            em.merge(address);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }
    
    private List<Address> queryAddresses(String query) {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();

        tx.begin();

        try {
            final List<Address> list =
                (List<Address>) em.createQuery(query).getResultList();
            tx.commit();
            return list;
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

}