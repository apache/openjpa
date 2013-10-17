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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestEmbeddedWithQuery extends SingleEMFTestCase {
    public void setUp() {
        setUp(EParent.class, EChild.class, EGeneric.class,
            "openjpa.MaxFetchDepth", "1",
            "openjpa.jdbc.EagerFetchMode", "none",
            "openjpa.jdbc.SubclassFetchMode", "none",
            CLEAR_TABLES);
        populate();
    }
    
    public void testFullEmbeddableLoadByJPQLQuery() {
        EntityManager em = emf.createEntityManager();
        
        try {
            String queryStr = 
                    "SELECT id AS idparent, longVal AS idchild, CAST(NULL AS CHAR)  as missing FROM EGENERIC";
                    //"SELECT 1 as idparent, 2 as idchild,CAST(NULL AS CHAR) as missing " 
                    // FROM sysibm.sysdummy1";
            Query q1 = em.createNativeQuery(queryStr, EParent.class);
            
            List resultList = q1.getResultList(); 
            assertNotNull(resultList);
            
            List <EParent> pList = new ArrayList<EParent>();
            pList.addAll(resultList);
            em.clear();                      
            assertNotEquals(0, pList.size());
            
            EParent pFind = pList.get(0);
            
            assertNotNull(pFind);
            assertEquals(pFind.getIdParent(), new Integer(1));
            assertNotNull(pFind.getChildTo());
            assertEquals(pFind.getChildTo().getIdChild(), new Long(10));
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            
            em.close();
        }
    }
    
    public void testPartialEmbeddableLoadByJPQLQuery() {
        EntityManager em = emf.createEntityManager();
        
        try {
            String queryStr = "SELECT id AS idparent, longVal AS idchild FROM EGENERIC";
            Query q1 = em.createNativeQuery(queryStr, EParent.class);
            
            List resultList = q1.getResultList(); 
            assertNotNull(resultList);
            
            List <EParent> pList = new ArrayList<EParent>();
            pList.addAll(resultList);
            em.clear();                      
            assertNotEquals(0, pList.size());
            
            EParent pFind = pList.get(0);
            
            assertNotNull(pFind);
            assertEquals(pFind.getIdParent(), new Integer(1));
            assertNotNull(pFind.getChildTo());
            assertEquals(pFind.getChildTo().getIdChild(), new Long(10));
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            
            if (em.isOpen()) {
                em.close();
            }
        }
    }
    
    private void populate() {
        EntityManager em = emf.createEntityManager();
        
        try {
            EGeneric generic = new EGeneric();
            generic.setId(new Integer(1));
            generic.setLongVal(new Long(10));
            generic.setStrVal("Nope");
            
            em.getTransaction().begin();
            em.persist(generic);
            em.getTransaction().commit();       
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            
            em.close();
        }
    }
}
