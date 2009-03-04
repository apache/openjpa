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

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestAssocOverrides  extends SQLListenerTestCase { 
    public int numEmployees = 2;
    public int numPhoneNumbers = numEmployees + 1;
    public int numEmployeesPerPhoneNumber = 2;
    public int numPhoneNumbersPerEmployee = 2;
    public int numJobsPerEmployee = 2;
    public int empId = 1;
    public int phoneId = 1;
    public int addrId = 1;
    public int pmId = 1;

    public void setUp() throws Exception {
        super.setUp(DROP_TABLES, Address.class, ContactInfo.class, 
            EmergencyContactInfo.class, Employee.class, JobInfo.class,
            PhoneNumber.class, ProgramManager.class, Zipcode.class);
    }

    public void testAssocOverride1() {
        sql.clear();
        createObj1();
        findObj1();
        queryObj1();
        assertMappingOverrides("EMPPHONES");
        assertMappingOverrides("EMP_ATTROVER");
        assertMappingOverrides("EMPLOYEE_JOBINFOS");
   }

	public void createObj1() {
		EntityManager em = emf.createEntityManager();
		EntityTransaction tran = em.getTransaction();
		for (int i = 0; i < numEmployees; i++)
		    createEmployee(em, empId++);
		tran.begin();
		em.flush();
		tran.commit();
        em.close();
	}

    public Employee createEmployee(EntityManager em, int id) {
        Employee e = new Employee();
        e.setEmpId(id);
        ContactInfo contactInfo = new ContactInfo();
        for (int i = 0; i < numPhoneNumbersPerEmployee; i++) { 
            PhoneNumber phoneNumber = createPhoneNumber(em);
            contactInfo.addPhoneNumber(phoneNumber);
            e.setContactInfo(contactInfo);
            phoneNumber.addEmployees(e);
            em.persist(phoneNumber);
        }
        Address addr = new Address();
        addr.setId(addrId++);
        addr.setCity("city_" + addr.getId());
        addr.setState("state_" + addr.getId());
        addr.setStreet("street_" + addr.getId());
        Zipcode zip = new Zipcode();
        zip.setZip("zip_" + addr.getId());
        zip.setPlusFour("+4_" + addr.getId());
        addr.setZipcode(zip);
        em.persist(addr);
        contactInfo.setAddress(addr);
                
        EmergencyContactInfo ecInfo = new EmergencyContactInfo();
        ecInfo.setFName("fName_" + id);
        ecInfo.setLName("lName_" + id);
        Address eaddr = new Address();
        eaddr.setId(addrId++);
        eaddr.setCity("city_" + eaddr.getId());
        eaddr.setState("state_" + eaddr.getId());
        eaddr.setStreet("street_" + eaddr.getId());
        Zipcode ezip = new Zipcode();
        ezip.setZip("zip_" + eaddr.getId());
        ezip.setPlusFour("+4_" + eaddr.getId());
        eaddr.setZipcode(ezip);
        ecInfo.setAddress(eaddr);
        contactInfo.setEmergencyContactInfo(ecInfo);
        PhoneNumber phoneNumber = createPhoneNumber(em);
        ecInfo.setPhoneNumber(phoneNumber);
        em.persist(eaddr);
        
        for (int i = 0; i < numJobsPerEmployee; i++) {
        	JobInfo job = new JobInfo();
        	job.setJobDescription("job_" + id + "_" + i);
        	ProgramManager pm = new ProgramManager();
        	pm.setId(pmId++);
        	pm.addManage(e);
        	em.persist(pm);
        	job.setProgramManager(pm);
        	e.addJobInfo(job);
        }
        em.persist(e);
        return e;
    }
	
    public PhoneNumber createPhoneNumber(EntityManager em) {
    	PhoneNumber p = new PhoneNumber();
    	p.setNumber(phoneId++);
    	em.persist(p);
    	return p;
    }    
    
	public void findObj1() {
        EntityManager em = emf.createEntityManager();
 	    Employee e = em.find(Employee.class, 1);
	    assertEmployee(e);
	    em.close();
	}
	
	public void queryObj1() {
	    EntityManager em = emf.createEntityManager();
	    EntityTransaction tran = em.getTransaction();
	    tran.begin();
	    Query q = em.createQuery("select e from Employee e");
	    List<Employee> es = q.getResultList();
	    for (Employee e : es){
	        assertEmployee(e);
	    }
	    tran.commit();
	    em.close();
	}

    public void assertEmployee(Employee e) {
        int id = e.getEmpId();
        ContactInfo c = e.getContactInfo();
        List<PhoneNumber> phones = c.getPhoneNumbers();
        for (PhoneNumber p : phones) {
            assertPhoneNumber(p, e.getEmpId());
        }
    }
	
    public void assertPhoneNumber(PhoneNumber p, int empId) {
        int number = p.getNumber();
        Collection<Employee> es = p.getEmployees();
        for (Employee e: es) {
            assertEquals(empId, e.getEmpId());
        }
    }

    public void assertMappingOverrides(String tableName) {
        for (String sqlStr : sql) {
            if (sqlStr.toUpperCase().indexOf("CREATE TABLE " + tableName + " (") != -1) {
                if (tableName.equals("EMPPHONES")) {
                    if (sqlStr.indexOf("EMP") == -1 ||
                        sqlStr.indexOf("PHONE") == -1)
                        fail();
                } else if (tableName.equals("EMP_ATTROVER")) {
                	System.out.println("sql=" + sqlStr);
                    if (sqlStr.indexOf("EMP_ADDR") == -1 ||
                        sqlStr.indexOf("EMERGENCY_FNAME") == -1 ||
                        sqlStr.indexOf("EMERGENCY_LNAME") == -1 ||
                        sqlStr.indexOf("EMERGENCY_ADDR") == -1 ||
                        sqlStr.indexOf("EMERGENCY_PHONE") == -1)
                        fail();
                } else if (tableName.equals("EMP_ATTROVER_jobInfos")) {
                    if (sqlStr.indexOf("JOB_KEY") == -1 ||
                        sqlStr.indexOf("JOB_DESC") == -1 ||
                        sqlStr.indexOf("PROGRAM_MGR") == -1)
                        fail();
                }
            }
        }
    }

}