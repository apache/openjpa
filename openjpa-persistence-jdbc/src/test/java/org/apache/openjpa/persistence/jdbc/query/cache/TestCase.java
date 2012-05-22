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

package org.apache.openjpa.persistence.jdbc.query.cache;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestCase extends SQLListenerTestCase {

	private static EntityManager em;
	
	public void setUp() {
        setUp(CatalogEntry.class, CatalogEntryDescription.class, CLEAR_TABLES);
        em = emf.createEntityManager();
        boolean isDataPoulated =  em.createQuery("select count(c) from CatalogEntry c", Long.class)
        		.getSingleResult().longValue() > 0;
        if(!isDataPoulated){
            em.getTransaction().begin();
            CatalogEntry catalogEntry1 = new CatalogEntry(10001,"SKU10001");
            CatalogEntry catalogEntry2 = new CatalogEntry(10002,"SKU10002");
            CatalogEntry catalogEntry3 = new CatalogEntry(10003,"SKU10003");
            CatalogEntry catalogEntry4 = new CatalogEntry(10004,"SKU10004");
            em.persist(catalogEntry1);
            em.persist(catalogEntry2);
            em.persist(catalogEntry3);
            em.persist(catalogEntry4);
            CatalogEntryDescription catalogEntryDescription1 = 
            		new CatalogEntryDescription(10001,"Description for SKU10001");
            CatalogEntryDescription catalogEntryDescription2 = 
            		new CatalogEntryDescription(10002,"Description for SKU10002");
            CatalogEntryDescription catalogEntryDescription3 = 
            		new CatalogEntryDescription(10003,"Description for SKU10003");
            CatalogEntryDescription catalogEntryDescription4 = 
            		new CatalogEntryDescription(10004,"Description for SKU10004");
            em.persist(catalogEntryDescription1);
            em.persist(catalogEntryDescription2);
            em.persist(catalogEntryDescription3);
            em.persist(catalogEntryDescription4);
    		em.getTransaction().commit();
        }
	}
	
	private void printCatentries(List<CatalogEntry> catalogEntries){
		if(catalogEntries != null && !catalogEntries.isEmpty()){
			for (CatalogEntry catalogEntry : catalogEntries) {
				System.out.println("catalogEntryId: " + catalogEntry.getCatalogEntryId() 
						+ ", partNumber: " + catalogEntry.getPartNumber());
			}
		}
	}
	
	private void printDescriptions(List<CatalogEntryDescription> descriptions){
		if(descriptions != null && !descriptions.isEmpty()){
			for (CatalogEntryDescription description : descriptions) {
				System.out.println("catalogEntryId: " + description.getCatalogEntryId() 
						+ ", description: " + description.getDescription());
			}
		}
	}
	
	public void testCase1(){// IN clause with primitive types in the list
		{
			System.out.println("<First execution - works fine>");
			Query jpql = em.createQuery("SELECT catentry FROM CatalogEntry catentry " +
					"WHERE catentry.catalogEntryId IN(:catalogEntryId1) " +
					"and catentry.catalogEntryId IN(:catalogEntryId2) " +
					"and catentry.partNumber IN(:catalogPartNum) " +
					"and catentry.quantity = (:quantity) " +
					"and catentry.catalogEntryId IN(:catalogEntryId3)");
			List<Long> catalogEntryIds = new ArrayList<Long>();
			catalogEntryIds.add(new Long(10001));
			catalogEntryIds.add(new Long(10002));
			catalogEntryIds.add(new Long(10003));
			List<String> catalogPartNum = new ArrayList<String>();
			catalogPartNum.add("SKU10001");
			catalogPartNum.add("SKU10002");
			jpql.setParameter("catalogEntryId1", catalogEntryIds);
			jpql.setParameter("catalogEntryId2", catalogEntryIds);
			jpql.setParameter("catalogEntryId3", catalogEntryIds);
			jpql.setParameter("catalogPartNum", catalogPartNum);
			jpql.setParameter("quantity", 50);
			List<CatalogEntry> catalogEntries = jpql.getResultList();
			printCatentries(catalogEntries);
			assertEquals(2, catalogEntries.size());
		}
		// at this point, the prepared statements are assumed to be cached, that is the default behaviour.

		{	
			System.out.println("<Subsequent execution based on query cache>");
			Query jpql = em.createQuery("SELECT catentry FROM CatalogEntry catentry " +
					"WHERE catentry.catalogEntryId IN(:catalogEntryId1) " +
					"and catentry.catalogEntryId IN(:catalogEntryId2) " +
					"and catentry.partNumber IN(:catalogPartNum) " +
					"and catentry.quantity = (:quantity) " +
					"and catentry.catalogEntryId IN(:catalogEntryId3)");
			List<Long> catalogEntryIds = new ArrayList<Long>();
			catalogEntryIds.add(new Long(10001));
			catalogEntryIds.add(new Long(10002));
			catalogEntryIds.add(new Long(10003));
			List<String> catalogPartNum = new ArrayList<String>();
			catalogPartNum.add("SKU10001");
			catalogPartNum.add("SKU10002");
			jpql.setParameter("catalogEntryId1", catalogEntryIds);
			jpql.setParameter("catalogEntryId2", catalogEntryIds);
			jpql.setParameter("catalogEntryId3", catalogEntryIds);
			jpql.setParameter("catalogPartNum", catalogPartNum);
			jpql.setParameter("quantity", 50);
			List<CatalogEntry> catalogEntries = jpql.getResultList();
			printCatentries(catalogEntries);
			assertEquals(2, catalogEntries.size());
		}
	}
	
	public void testCase2(){// IN clause with entities in the list
		List<CatalogEntry> catalogEntries = em.createQuery("SELECT catentry FROM CatalogEntry catentry " +
				"WHERE catentry.catalogEntryId IN(10001,10002)").getResultList();
		{
			System.out.println("<First execution - works fine>");
			Query jpql = em.createQuery("SELECT catentdesc FROM CatalogEntryDescription catentdesc " +
					"WHERE catentdesc.CatalogEntryForCatalogEntryDescription IN(:catalogEntry)");
			jpql.setParameter("catalogEntry", catalogEntries);
			printDescriptions(jpql.getResultList());
			System.out.println("</First execution - works fine>");
			System.out.println();
		}
		// at this point, the prepared statements are assumed to be cached, ast that is the default behaviour.

		{	
			System.out.println("<Subsequent execution - throws exception>");
			Query jpql = em.createQuery("SELECT catentdesc FROM CatalogEntryDescription catentdesc " +
					"WHERE catentdesc.CatalogEntryForCatalogEntryDescription IN(:catalogEntry)");
			try{
				jpql.setParameter("catalogEntry", catalogEntries);
				printDescriptions(jpql.getResultList());
			}catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("</Subsequent execution - throws exception>");
			System.out.println();
		}

	}
}

