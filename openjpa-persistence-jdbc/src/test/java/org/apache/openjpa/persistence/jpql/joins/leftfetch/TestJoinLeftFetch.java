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
package org.apache.openjpa.persistence.jpql.joins.leftfetch;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestJoinLeftFetch extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(DROP_TABLES, DepartmentTest.class, PersonTest.class);
        createTestData();
    }

    /*
     * This test fails (prior to OJ-2475) because the
     * DepartmentTests are not populated with the correct
     * number of PersonTests
     */
    public void testReadDepartmentsWithLeftJoinFetch() {

        EntityManager em = emf.createEntityManager();

        String qStrDIST = "SELECT DISTINCT dept FROM DepartmentTest "
            + "dept LEFT JOIN FETCH dept.persons";

        Query query = em.createQuery(qStrDIST);
        List<DepartmentTest> depts = query.getResultList();
        verifySize(depts);
        em.close();
    }

    public void verifySize(List<DepartmentTest> depts){
        for (DepartmentTest department : depts) {
            if (department.getPrimaryKey().equals("001")) {
//                System.out.println("Dept: " + department.getName());
  //              Iterator i = department.getPersons().iterator();
    //            while (i.hasNext()){
      //              System.out.println("i.next() = " + i.next());
        //        }
                assertEquals("Size should be 3", 3, department.getPersons().size());
            }
            if (department.getPrimaryKey().equals("002")) {
//                System.out.println("Dept: " + department.getName());
  //              Iterator i = department.getPersons().iterator();
    //            while (i.hasNext()){
      //              System.out.println("i.next() = " + i.next());
        //        }
                assertEquals("Size should be 2", 2, department.getPersons().size());
            }
        }
    }

    /*
     * This test works as expected.
     */
    public void testReadDepartmentsWithFetchPlan() {

        EntityManager em = emf.createEntityManager();

        OpenJPAQuery<DepartmentTest> query = OpenJPAPersistence.cast(em.createQuery(" SELECT dept FROM "
            + " DepartmentTest dept "));
        query.getFetchPlan().addField(DepartmentTest.class, "persons");

        verifySize(query.getResultList());

        em.close();
    }

    /*
     * This test works as expected.
     */
    public void testReadDepartmentsWithLeftJoinFetchAndOrderBy() {

        EntityManager em = emf.createEntityManager();

        Query query = em.createQuery(" SELECT dept FROM " + " DepartmentTest dept "
            + " LEFT JOIN FETCH dept.persons ORDER BY dept.primaryKey");
        verifySize(query.getResultList());

        em.close();
    }

    public void createTestData() {
        // NOTE: This test depends upon the the PersonTest
        // to be un-ordered w.r.t the DepartmentTest FK.
        // I've executed a flush after each entity creation
        // in an attempt that the FKs will not be ordered.
        // @OrderBy is used in the DepartmentTest in order
        // to ensure things aren't orderd by the FK.

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        DepartmentTest dt1 = new DepartmentTest();
        dt1.setPrimaryKey("001");
        dt1.setName("Dept001");
        em.persist(dt1);

        DepartmentTest dt2 = new DepartmentTest();
        dt2.setPrimaryKey("002");
        dt2.setName("Dept002");
        em.persist(dt2);

        PersonTest pt = new PersonTest();
        pt.setPrimaryKey("1");
        pt.setName("John");
        pt.setDepartmentTest(dt1);
        em.persist(pt);
        em.flush();

        pt = new PersonTest();
        pt.setPrimaryKey("2");
        pt.setName("Mark");
        pt.setDepartmentTest(dt1);
        em.persist(pt);
        em.flush();

        pt = new PersonTest();
        pt.setPrimaryKey("3");
        pt.setName("Stuart");
        pt.setDepartmentTest(dt2);
        em.persist(pt);
        em.flush();

        pt = new PersonTest();
        pt.setPrimaryKey("4");
        pt.setName("Jim");
        pt.setDepartmentTest(dt1);
        em.persist(pt);
        em.flush();

        pt = new PersonTest();
        pt.setPrimaryKey("5");
        pt.setName("Fred");
        pt.setDepartmentTest(dt2);
        em.persist(pt);
        em.flush();

        em.getTransaction().commit();
        em.close();
    }
}




