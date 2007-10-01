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
package org.apache.openjpa.persistence.fetchgroups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestFetchGroup extends SingleEMTestCase {
    public void setUp() {
        setUp(FGEmployee.class, FGDepartment.class, FGManager.class,
                FGAddress.class);
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        // Populate database as denoted in Entity Data
        boolean errors = initializeDatabase(oem);
        assertFalse(errors);
    }

    // Test no fetch group is added.
    public void testFetchGroup001() {
        // System.out.println("***********************************************");
        // System.out.println("******** 001 ==> test no fetch group is added
        // thru API");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        FGEmployee emp = findEmployee(oem, 1, false, null);

        // Examine Employee(id=1).rating, data should be available
        // System.out.println("Assert Employee(id=1).rating should be null.
        // Result ==>");
        assertNull(emp.getRating());
        // Examine Employee(id=1).description, data should NOT be available
        // System.out.println("Assert Employee(id=1).description is null. Result
        // ==>");
        assertNull(emp.getDescription());
        // Examine Employee(id=1).address, data should not be available
        // System.out.println("Assert Employee(id=1).address is null");
        assertNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoTbe available
        // System.out.println("Assert Employee(id=1).dept is null ");
        assertNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // System.out.println("Assert Employee(id=1).manager is null ");
        assertNull(emp.getManager());

    }

    // Test no fetch group is added and restFetchGroup is called.
    public void testFetchGroup002() {
        // System.out.println("***********************************************");
        // System.out.println("*****************002 ==> test no fetch group is
        // added thru API and ");
        // System.out.println(" resetFetchGroup is called.");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        FGEmployee emp = findEmployee(oem, 1, true, null);
        // Examine Employee(id=1).rating, data should not be available
        // System.out.println("Assert Employee(id=1).rating is null.");
        assertNull(emp.getRating());
        // Examine Employee(id=1).description, data should NOT be available
        // System.out.println("Assert Employee(id=1).description is null. Result
        // ==>");
        assertNull(emp.getDescription());
        // Examine Employee(id=1).address, data should not be available
        // System.out.println("Assert Employee(id=1).address is null ");
        assertNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoT be available
        // System.out.println("Assert Employee(id=1).dept is null ");
        assertNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // System.out.println("Assert Employee(id=1).manager is null ");
        assertNull(emp.getManager());

    }

    // Test Rating fetch group is added and restFetchGroup is called.
    public void testFetchGroup003() {
        // System.out.println("***********************************************");
        // System.out.println("****************003 ==> test RatingFetchGroup is
        // added thru API and ");
        // System.out.println(" resetFetchGroup is called.");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        String[] arr = { "RatingFetchGroup" };
        FGEmployee emp = findEmployee(oem, 1, true, arr);
        // Examine Employee(id=1).rating, data should be available
        // System.out.println("Assert Employee(id=1).rating is not null. Result
        // ==> ");
        assertNotNull(emp.getRating());
        // Examine Employee(id=1).description, data should NOT be available
        // System.out.println("Assert Employee(id=1).description is null. Result
        // ==>");
        assertNull(emp.getDescription());
        // Examine Employee(id=1).address, data should be available
        // System.out.println("Assert Employee(id=1).address is not null because
        // of @LoadFetchGroup on Rating ==> ");
        assertNotNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoTbe available
        // System.out.println("Assert Employee(id=1).dept is null = ");
        assertNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // System.out.println("Assert Employee(id=1).manager is null =");
        assertNull(emp.getManager());
        // em.getTransaction().commit();

    }

    // Test Address and Rating fetch groups are added and restFetchGroup is
    // called.
    public void testFetchGroup004() {
        // System.out.println("***********************************************");
        // System.out.println("***************004 ==> test RatingFetchGroup and
        // AddressFetchGroup" +
        // "are added thru API and " +
        // "resetFetchGroup is called.");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        String[] arr = { "RatingFetchGroup", "AddressFetchGroup" };
        FGEmployee emp = findEmployee(oem, 1, true, arr);
        // Examine Employee(id=1).rating, data should be available
        // System.out.println("Assert Employee(id=1).rating is not null. Result
        // ==> ");
        assertNotNull(emp.getRating());
        // Examine Employee(id=1).description, data should NOT be available
        // System.out.println("Assert Employee(id=1).description is null. Result
        // ==>");
        assertNull(emp.getDescription());
        // Examine Employee(id=1).address, data should be available
        // System.out.println("Assert Employee(id=1).address is not null ==> ");
        assertNotNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoTbe available
        // System.out.println("Assert Employee(id=1).dept is null = ");
        assertNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // System.out.println("Assert Employee(id=1).manager is null =");
        assertNull(emp.getManager());

    }

    // Test aggregateEmployeeFetchGroup2 only
    public void testFetchGroup005() {
        // System.out.println("***********************************************");
        // System.out.println("***************005 ==> test
        // aggregateEmployeeFetchGroup2 only");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        String[] arr = { "AggregateEmployeeFetchGroup2" };
        FGEmployee emp = findEmployee(oem, 1, true, arr);
        // Examine Employee(id=1).address, data should be available

        // System.out.println("Assert Employee(id=1).address is not null ");
        assertNotNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoTbe available
        // System.out.println("Assert Employee(id=1).dept is not null = ");
        assertNotNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // System.out.println("Assert Employee(id=1).manager is not null =");
        assertNotNull(emp.getManager());

    }

    public void testFetchGroup006() {
        // System.out.println("***********************************************");
        // System.out.println("************006 ==> test
        // aggregateEmployeeFetchGroup1 and "+
        // " aggregateEmployeeFetchGroup2 - expect address, dept and manager are
        // not null");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
        // em.getTransaction().begin();

        String[] arr = { "AggregateEmployeeFetchGroup1",
                "AggregateEmployeeFetchGroup2" };
        FGEmployee emp = findEmployee(oem, 1, true, arr);
        // Examine Employee(id=1).address, data should be available
        // System.out.println("Assert Employee(id=1).address is not null ");
        assertNotNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoTbe available
        // System.out.println("Assert Employee(id=1).dept is not null ");
        assertNotNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // FGManager mgr = emp.getManager();
        // System.out.println("assert manager is not null");
        assertNotNull(emp.getManager());
        assertNotNull(emp.getManager().getId());
        assertNotNull(emp.getManager().getFirstName());

        // Verify that Manager(id=101).manager is not available, as the
        // recursion depth should have retrieved only the Employee and its
        // manager.
        // System.out.println("Verify that Manager(id=101).manager is no
        // available, as the recursion depth should have retrieved only the
        // Employee and its manager.");
        // System.out.println("Assert Employee(id=1).manager.manager == null
        // ==>"+ mgrMgr);
        assertNull(emp.getManager().getManager());

        // System.out.println("Verify that Manager(id=201).manager is not
        // available, as the recursion depth should have retrieved only the
        // Employee and its manager.");
        // assertNull(emp.getManager().getManager().getManager());

    }

    public void testFetchGroup007() {
        // System.out.println("***********************************************");
        // System.out.println("***********007 ==> test one fetch group attribute
        // is associated"+
        // "multiple fetch groups");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);

        String[] arr = { "ManagerFetchGroup1A" };
        FGEmployee emp = findEmployee(oem, 1, true, arr);
        // Examine Employee(id=1).address, data should be available
        // FGAddress addr = emp.getAddress();
        // System.out.println("Assert Employee(id=1).address is null ");
        assertNull(emp.getAddress());
        // Examine Employee(id=1).dept, data should NoTbe available
        // System.out.println("Assert Employee(id=1).dept is null");
        assertNull(emp.getDept());
        // Examine Employee(id=1).manager, data should NOT be available.
        // FGManager mgr = emp.getManager();

        // System.out.println("Assert manager is not null");
        assertNotNull(emp.getManager());
        assertNotNull(emp.getManager().getId());
        assertNotNull(emp.getManager().getFirstName());

        // Verify that Manager(id=101).manager is not available, as the
        // recursion depth should have retrieved only the Employee and its
        // manager.
        // System.out.println("Verify that Manager(id=101).manager is not
        // available, as the recursion depth should have retrieved only the
        // Employee and its manager.");
        assertNull(emp.getManager().getManager());

        // System.out.println("Verify that Manager(id=201).manager is not
        // available, as the recursion depth should have retrieved only the
        // Employee and its manager.");
        // assertNull(emp.getManager().getManager().getManager());

    }

    public void testFetchGroup008() {
        // System.out.println("***********************************************");
        // System.out.println("***********007 ==> test one fetch group attribute
        // is associated"+
        // "multiple fetch groups");
        EntityManager em = emf.createEntityManager();
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
        OpenJPAEntityManager oem1 = OpenJPAPersistence.cast(em);
        Query q = oem1.createQuery("SELECT e FROM FGEmployee e WHERE e.id = 1");
        OpenJPAQuery oq = (OpenJPAQuery) q;
        oem1.clear();

        // use the default, address and description should be null
        FGEmployee emp = findEmployeeForQuery(oem, oq, 1, true, null, null);
        oem1.clear();
        assertNull(emp.getAddress());
        assertNull(emp.getDescription());
        assertNull(emp.getManager());

        // add fetch fields to the fetch plan - address and description should
        // not be null
        String[] str = {
                "org.apache.openjpa.persistence.query.FGEmployee.description",
                "org.apache.openjpa.persistence.query.FGEmployee.address" };
        FGEmployee emp2 = findEmployeeForQuery(oem, oq, 1, true, str, null);
        oem1.clear();
        assertNotNull(emp2.getAddress());
        assertNotNull(emp2.getDescription());
        assertNull(emp2.getManager());

        // remove fetch fields again - address and description should be null
        FGEmployee emp3 = findEmployeeForQuery(oem, oq, 1, false, null, str);
        oem1.clear();
        assertNull(emp3.getAddress());
        assertNull(emp3.getDescription());
        assertNull(emp3.getManager());
    }

    private FGEmployee findEmployee(OpenJPAEntityManager oem, Object id,
            boolean reset, String[] fetchGroups) {
        oem.getTransaction().begin();
        // System.out.println("findEmployoee starts and check the fetchGroup
        // info:");
        // int sz = oem.getFetchPlan().getFetchGroups().size();
        // String arr =
        // Arrays.toString(oem.getFetchPlan().getFetchGroups().toArray());
        // System.out.println("fetchGroup = "+arr+ " and fetch Group size
        // ="+sz);
        // reset fetchGroup if necessary:
        if (reset) {
            oem.getFetchPlan().resetFetchGroups();
            // assertEquals(1, oem.getFetchPlan().getFetchGroups().size());
            // arr =
            // Arrays.toString(oem.getFetchPlan().getFetchGroups().toArray());
            // System.out.println("after resetFetchGroup, fetchGroup="+arr);
            // assertEquals("[default]",arr);
        }
        if (fetchGroups != null) {
            // System.out.println("input fetchGroup = "+fetchGroups);
            for (String fg : fetchGroups)
                oem.getFetchPlan().addFetchGroup(fg);
            // arr =
            // Arrays.toString(oem.getFetchPlan().getFetchGroups().toArray());
            // System.out.println("after addFetchGroup, fetchGroups = "+arr);
        }
        // System.out.println("Finding Employee(id=1)...");
        FGEmployee emp = oem.find(FGEmployee.class, id);
        // System.out.println("Employee found ="+emp);
        oem.getTransaction().commit();
        // oem.clear();
        oem.close();
        return emp;

    }

    private static FGEmployee findEmployeeForQuery(OpenJPAEntityManager oem,
            OpenJPAQuery oq, Object id, boolean reset, String[] fetchGroups,
            String[] removes) {
        oem.getTransaction().begin();
        // reset fetchGroup if necessary:
        if (reset) {
            oem.getFetchPlan().resetFetchGroups();
            oq.getFetchPlan().resetFetchGroups();
        }
        if (fetchGroups != null) {
            for (String fg : fetchGroups)
                oq.getFetchPlan().addField(fg);
            // arr =
            // Arrays.toString(oq.getFetchPlan().getFetchGroups().toArray());
            // arr = Arrays.toString(oq.getFetchPlan().getFields().toArray());
            // System.out.println("after addFetchfields, fetch fields = "+arr);
        }
        if (removes != null) {
            oq.getFetchPlan().removeFields(removes);
            // arr = Arrays.toString(oq.getFetchPlan().getFields().toArray());
            // System.out.println("after removeFetchGroup, fetch fields =
            // "+arr);
        }
        // System.out.println("Finding Employee(id=1)...");
        FGEmployee emp = (FGEmployee) oq.getSingleResult();
        oem.getTransaction().commit();
        oem.clear();
        // oem.close();
        return emp;

    }

    private static void cleanDatabase(EntityManager em) {
        // Clean out the database
        em.clear();

        String entityNames[] = { "FGEmployee", "FGAddress", "FGDepartment" };

        // System.out.println("Cleaning database.");
        try {
            // System.out.println("Starting transaction...");
            em.getTransaction().begin();
            // if (persistenceContextType == PERSISTENCECONTEXTTYPE_APPMGD)
            // em.joinTransaction();

            for (int index = 0; index < entityNames.length; index++) {
                String query = "SELECT a FROM " + entityNames[index] + " a";
                List entityAList = em.createQuery(query).getResultList();

                // Nothing returned, go to the next entity
                if (entityAList.size() == 0)
                    continue;

                // System.out.println("Removing " + entityNames[index] + " data
                // from the database...");

                Iterator i = entityAList.iterator();
                while (i.hasNext()) {
                    Object entity = i.next();
                    // System.out.println("Removing entity " + entity.toString()
                    // + " ...");
                    em.remove(entity);
                }
            }

            // System.out.println("Committing transaction...");
            em.getTransaction().commit();
        } catch (Throwable t) {
            System.out.println("Caught exception during db cleanup" + t);
        } finally {
            try {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
            } catch (Throwable t) {
                System.out
                        .println("Caught exception transaction rollback in db cleanup failure recovery"
                                + t);
                // throw t;
            }
        }

        // System.out.println("Done cleaning database.");
    }

    private static boolean initializeDatabase(EntityManager em) {
        // Clean the database first
        cleanDatabase(em);

        // System.out.println("Creating entities...");
        boolean errors = false;
        try {
            // Persist all entities to the database
            // System.out.println("Starting transaction...");
            em.getTransaction().begin();
            // if (persistenceContextType == PERSISTENCECONTEXTTYPE_APPMGD)
            // em.joinTransaction();

            // Addreesses
            FGAddress[] addresses = new FGAddress[11];
            addresses[0] = new FGAddress(1, "1010 29th Ave NW", "Rochester",
                    "MN", 55901);
            addresses[1] = new FGAddress(2, "2020 29th Ave NW", "Rochester",
                    "MN", 55901);
            addresses[2] = new FGAddress(3, "5000 Pilot Knob", "Rochester",
                    "MN", 55902);
            addresses[3] = new FGAddress(4, "8192 Galaxie Avenue",
                    "Apple Valley", "MN", 55209);
            addresses[4] = new FGAddress(5, "9100 Knight Drive", "Fargo", "ND",
                    58202);
            addresses[5] = new FGAddress(6, "312 Sioux Lane", "Bismarck", "ND",
                    58102);
            addresses[6] = new FGAddress(7, "5124 Grinch Circle", "Mason City",
                    "IA", 24241);
            addresses[7] = new FGAddress(8, "1201 Citrus Lane", "Raleigh",
                    "NC", 12345);
            addresses[8] = new FGAddress(9, "1501 Lemon Lane", "Raleigh", "NC",
                    12345);
            addresses[9] = new FGAddress(10, "2903 Orange Drive", "Raleigh",
                    "NC", 12345);
            addresses[10] = new FGAddress(11, "1511 Kiwi Circle", "Raleigh",
                    "NC", 12345);

            // System.out.println("Persisting Address entities...");
            for (int index = 0; index < addresses.length; index++) {
                em.persist(addresses[index]);
            }

            // Departments
            FGDepartment[] departments = new FGDepartment[7];
            for (int index = 0; index < 7; index++) {
                departments[index] = new FGDepartment(index + 1, "Department "
                        + (index + 1));
            }

            // System.out.println("Persisting Department entities...");
            for (int index = 0; index < departments.length; index++) {
                em.persist(departments[index]);
            }

            // Managers
            Collection<FGEmployee> emptyCollection = new ArrayList<FGEmployee>();
            FGManager[] managers = new FGManager[6];
            managers[0] = new FGManager(301, "Elric", "Scotch",
                    "Description MMM1", departments[6], addresses[10],
                    (FGManager) null, "Good", emptyCollection, "MData301");
            managers[1] = new FGManager(202, "Cedric", "Clue",
                    "Description MM2", departments[5], addresses[9],
                    managers[0], "Good", emptyCollection, "MData202");
            managers[2] = new FGManager(201, "Bill", "Editor",
                    "Description MM1", departments[5], addresses[8],
                    managers[0], "Good", emptyCollection, "MData201");
            managers[3] = new FGManager(103, "Sue", "Taylor", "Description M3",
                    departments[4], addresses[8], managers[1], "Good",
                    emptyCollection, "MData103");
            managers[4] = new FGManager(102, "Alfred", "Newmann",
                    "Description M2", departments[3], addresses[7],
                    managers[2], "Good", emptyCollection, "MData102");
            managers[5] = new FGManager(101, "Jim", "Mitternacht",
                    "Description M1", departments[3], addresses[6],
                    managers[2], "Good", emptyCollection, "MData101");

            // System.out.println("Persisting Manager entities...");
            for (int index = 0; index < managers.length; index++) {
                em.persist(managers[index]);
            }

            // Employees
            FGEmployee[] employees = new FGEmployee[8];
            employees[0] = new FGEmployee(1, "John", "Doe", "Description 1",
                    departments[0], addresses[0], managers[5], "Good");
            employees[1] = new FGEmployee(2, "Jane", "Doe", "Description 2",
                    departments[0], addresses[0], managers[5], "Good");
            employees[2] = new FGEmployee(3, "Steve", "Martin",
                    "Description 3", departments[0], addresses[1], managers[5],
                    "Good");
            employees[3] = new FGEmployee(4, "Mark", "Scrabble",
                    "Description 4", departments[1], addresses[2], managers[4],
                    "Good");
            employees[4] = new FGEmployee(5, "Stacy", "Life", "Description 5",
                    departments[1], addresses[3], managers[4], "Good");
            employees[5] = new FGEmployee(6, "Alx", "Indigo", "Description 6",
                    departments[2], addresses[5], managers[3], "Good");
            employees[6] = new FGEmployee(7, "John", "Einstein",
                    "Description 7", departments[2], addresses[5], managers[3],
                    "Good");
            employees[7] = new FGEmployee(8, "Max", "Headroom",
                    "Description 7", departments[5], addresses[3], managers[2],
                    "Good");

            // System.out.println("Persisting Employee entities...");
            for (int index = 0; index < employees.length; index++) {
                em.persist(employees[index]);
            }

            // System.out.println("Committing transaction...");
            em.getTransaction().commit();
        } catch (Throwable t) {
            // System.out.println("Caught exception during db populating"+ t);
            errors = true;
        } finally {
            try {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
            } catch (Throwable t) {
                // System.out.println("Caught exception transaction rollback in
                // db population failure recovery"+ t);
            }
        }

        return errors;
    }

} // end of TestFetchGroup
