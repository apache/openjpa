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
package org.apache.openjpa.persistence.query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class Test1x1OrderByResultCollection extends SingleEMTestCase {
	
	private static final Logger logger = Logger.getLogger(Test1x1OrderByResultCollection.class.getCanonicalName());

    @Override
    public void setUp() {
        super.setUp(Hardware.class, Person.class, CLEAR_TABLES
            , "openjpa.jdbc.DBDictionary", "fullResultCollectionInOrderByRelation=true"
            );

        populateData();
    }

   public void testCollectionSizeNoOrderBy() {
       Query query = em.createQuery("select h from Hardware h where h.empNo = :empNo");
       query.setParameter("empNo", "Emp1");
       List<Hardware> results = (List<Hardware>) query.getResultList();

       printResults("UnSorted", results);

       assertEquals(4, results.size());
   }

   public void testCollectionSizeOrderBy() {
        Query query = em.createQuery("select h from Hardware h where h.empNo = :empNo order by h.techOwner.name");
       query.setParameter("empNo", "Emp1");
       List<Hardware> results = (List<Hardware>) query.getResultList();

       printResults("Sorted asc", results);

       assertOrderBy(results, true);
   }

   public void testCollectionSizeOrderByDesc() {
        Query query = em.createQuery("select h from Hardware h where h.empNo = :empNo order by h.techOwner.name desc");
       query.setParameter("empNo", "Emp1");
       List<Hardware> results = (List<Hardware>) query.getResultList();

       printResults("Sorted desc", results);

       assertOrderBy(results, false);
   }

   private void assertOrderBy(List<Hardware> results, boolean asc) {
       assertEquals(4, results.size());

       // remove null techOwner or techOwner.name entries in collection first
       List<Hardware> nonNullResults = new ArrayList<>(results.size());
       for (Hardware hw : results) {
           if (hw.getTechOwner() != null
                   && hw.getTechOwner().getName() != null) {
               nonNullResults.add(hw);
           }
       }
       // Asert the remain entries in the collection are sort accordingly.
       String curTechOwner = null;
       String lastTechOwner = null;
       for (Hardware hw : nonNullResults) {
           if (lastTechOwner == null) {
               lastTechOwner = hw.getTechOwner().getName();
           } else {
               curTechOwner = hw.getTechOwner().getName();
               if (asc) {
                   assertTrue(lastTechOwner.compareTo(curTechOwner) <= 0);
               } else {
                   assertTrue(lastTechOwner.compareTo(curTechOwner) >= 0);
               }
               lastTechOwner = curTechOwner;
           }
       }
   }

    private void populateData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Person p1 = new Person("p1", "Person One");
        persist(p1);
        Person p2 = new Person("p2", "Person Two");
        persist(p2);
        Person p3 = new Person("p3", "Person Three");
        persist(p3);
        Person p4 = new Person("p4", "Person Four");
        persist(p4);
        Person pn = new Person("pn", null);
        persist(pn);

        Hardware hwe1p2 = new Hardware(1, "Emp1");
        hwe1p2.setTechOwner(p2);
        persist(hwe1p2);
        Hardware hwe1p1 = new Hardware(2, "Emp1");
        hwe1p1.setTechOwner(p1);
        persist(hwe1p1);
        Hardware hwen   = new Hardware(3, null);
        hwen.setTechOwner(p4);
        persist(hwen);
        Hardware hwe1pn = new Hardware(4, "Emp1");
        hwe1pn.setTechOwner(pn);
        persist(hwe1pn);
        Hardware hwe2p3   = new Hardware(5, "Emp2");
        hwe2p3.setTechOwner(p3);
        persist(hwe2p3);
        Hardware hwe1np = new Hardware(6, "Emp1");
        persist(hwe1np);

        em.getTransaction().commit();
        em.close();
    }

   public void printResults(String heading, List<Hardware> results) {
        logger.fine(heading + ": collection size= " + ((results == null) ? "0" : results.size()));
        if (results != null) {
           for (Hardware hw : results) {
               Person to = hw.getTechOwner();
                   String n = (to == null) ? "technical-owner-is-null" : to.getName();
               logger.fine("    id=" + hw.getId() + " TechnicalOwner=" + n);
           }
        }
   }
}
