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
package org.apache.openjpa.persistence.embed.attrOverrides;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestAttrOverridesXml extends SQLListenerTestCase {
   
    public int numPersons = 4;
    public List<String> namedQueries = new ArrayList<String>();
    public int eId = 1;
    
    public void setUp() {
        setUp(DROP_TABLES);
    }
    
    @Override
    protected String getPersistenceUnitName() {
        return "embed-pu";
    }
    
    public void testAttrOverride1() {
        sql.clear();
    	createObj1();
    	findObj1();
    	queryObj1();
        assertAttrOverrides("CustomerXml1");
    }
    
    public void createObj1() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        for (int i = 0; i < numPersons; i++)
        	createCustomer1(em, eId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public CustomerXml createCustomer1(EntityManager em, int id) {
    	CustomerXml p = new CustomerXml();
    	p.setId(id);
    	AddressXml addr = new AddressXml();
    	addr.setCity("city_" + id);
    	addr.setState("state_" + id);
    	addr.setStreet("street_" + id);
    	p.setAddress(addr);
    	p.setName("name_" + id);
        em.persist(p);
        return p;
    }

    public void findObj1() {
        EntityManager em = emf.createEntityManager();
        CustomerXml p = em.find(CustomerXml.class, 1);
        Assert.assertEquals(p.getId(), new Integer(1));
        Assert.assertEquals(p.getAddress().getCity(), "city_1");
        Assert.assertEquals(p.getAddress().getStreet(), "street_1");
        Assert.assertEquals(p.getAddress().getState(), "state_1");
        Assert.assertEquals(p.getName(), "name_1");
    }

    public void queryObj1() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        String jpql = "select p from CustomerXml1 p";
        Query q = em.createQuery(jpql);
        List<CustomerXml> ps = q.getResultList();
        Assert.assertEquals(ps.size(), numPersons);
        tran.commit();
        em.close();
    }

    public void assertAttrOverrides(String tableName) {
        boolean found = false;
        for (String sqlStr : sql) {
            if (sqlStr.indexOf("CREATE TABLE " + tableName) != -1) {
                if (tableName.equals("CustomerXml1")) {
                    found = true;
                    if (sqlStr.indexOf("ADDR_STATE") == -1 ||
                        sqlStr.indexOf("ADDR_ZIP") == -1)
                        fail();
                } 
            }
        }
        if (!found)
            fail();
    }
}
