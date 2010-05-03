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
package org.apache.openjpa.persistence.enhance.identity;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestCountDistinctMultiCols extends SingleEMFTestCase {

    public void setUp() throws Exception {
        super.setUp(DROP_TABLES,Employee.class,EmployeeId.class,Dependent.class,DependentId.class);
    }
    
    public void testCountDistinctMultiCols() {
        EntityManager em = emf.createEntityManager(); 

        Employee emp1 = new Employee();
        EmployeeId empId1 = new EmployeeId();
        empId1.setFirstName("James");
        empId1.setLastName("Bond");
        emp1.setEmpId(empId1);
        
        Employee emp2 = new Employee();
        EmployeeId empId2 = new EmployeeId();
        empId2.setFirstName("James");
        empId2.setLastName("Obama");
        emp2.setEmpId(empId2);
        
        Dependent dep1 = new Dependent();
        DependentId depId1 = new DependentId();
        dep1.setEmp(emp1);
        depId1.setName("Alan");
        dep1.setId(depId1);
        
        Dependent dep2 = new Dependent();
        DependentId depId2 = new DependentId();
        dep2.setEmp(emp2);
        depId2.setName("Darren");
        dep2.setId(depId2);
        
        em.persist(emp1);
        em.persist(emp2);
        em.persist(dep1);
        em.persist(dep2);
        
        em.getTransaction().begin();
        em.flush();        
        em.getTransaction().commit();
        
        String[] jpqls = {
            "SELECT COUNT (DISTINCT d2.emp) FROM Dependent d2",
            "SELECT COUNT (DISTINCT e2.dependents) FROM Employee e2",
            "select count (DISTINCT d2) from Dependent d2",
        };
        
        for (int i = 0; i < jpqls.length; i++) {
            Query q = em.createQuery(jpqls[i]) ;
            Long o = (Long)q.getSingleResult();
            int count = (int)o.longValue();
            assertEquals(2, count);
        }
    }
}
