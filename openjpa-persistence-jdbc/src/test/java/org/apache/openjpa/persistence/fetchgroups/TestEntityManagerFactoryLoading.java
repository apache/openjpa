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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.metamodel.Attribute;

public class TestEntityManagerFactoryLoading {
	
	private static final int MANAGERS_COUNT = 3;
	
	private static final int EMPLOYEES_PER_MANAGER = 5;
	
	private static final int EMPLOYEES_COUNT = MANAGERS_COUNT * EMPLOYEES_PER_MANAGER;
	
	private EntityManagerFactory emf;
	
	private Map<Integer, FGManager> managers = new HashMap<>();
	
	private Set<FGEmployee> employees = new HashSet<FGEmployee>();
	
	@Before
	public void beforeEach() {
		PersistenceConfiguration conf = new PersistenceConfiguration("test");
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");
		conf.managedClass(FGManager.class);
		conf.managedClass(FGDepartment.class);
		conf.managedClass(FGEmployee.class);
		conf.managedClass(FGAddress.class);
		
		emf = Persistence.createEntityManagerFactory(conf);
		emf.runInTransaction(this::loadTestData);
	}
	
	@After
	public void afterEach() {
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
	}

	@Test
	public void testLoadingLazyAttributeByName() {
		assertNotNull(emf);
		EntityManager em = null;
		try {
			em = emf.createEntityManager();
			
			FGManager manager = em.find(FGManager.class, 1);
			
			assertFalse(emf.getPersistenceUnitUtil().isLoaded(manager, "employees"));
			
			emf.getPersistenceUnitUtil().load(manager, "employees");
			
			assertTrue(emf.getPersistenceUnitUtil().isLoaded(manager, "employees"));
			
			assertEquals(EMPLOYEES_PER_MANAGER, manager.getEmployees().size());
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
	}
	
	@Test
	public void testLoadingLazyAttribute() {
		assertNotNull(emf);
		EntityManager em = null;
		try {
			em = emf.createEntityManager();
			
			FGManager manager = em.find(FGManager.class, 1);
			Attribute<? super FGManager,?> attribute = em.getMetamodel().entity(FGManager.class).getCollection("employees"); 
			
			assertFalse(emf.getPersistenceUnitUtil().isLoaded(manager, attribute));
			
			emf.getPersistenceUnitUtil().load(manager, attribute);
			
			assertTrue(emf.getPersistenceUnitUtil().isLoaded(manager, attribute));
			
			assertEquals(EMPLOYEES_PER_MANAGER, manager.getEmployees().size());
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
	}

	@Test
	public void testLoadingUnmanagedEntity() {
		FGManager manager = new FGManager();
		manager.setId(1);
		
		try {
			emf.getPersistenceUnitUtil().load(manager);
			fail("Should have thrown IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("not persistent"));
		}
		
	}
	
	@Test
	public void testLoadingEntity() {
		EntityManager em = null;
		try {
			em = emf.createEntityManager();
			FGManager manager = em.find(FGManager.class, 1);
			String originalName = manager.getFirstName();
			manager.setFirstName("Changed name");
			assertNotEquals(originalName, manager.getFirstName());
			emf.getPersistenceUnitUtil().load(manager);
			assertEquals(originalName, manager.getFirstName());			
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
		
	}

    private void loadTestData(EntityManager em) {
    	
    	managers.clear();
    	employees.clear();

    	int empIdIndex = 1;

        // Create Managers
        for (int i = 0; i < MANAGERS_COUNT; i++) {
            int id = empIdIndex++;

            FGAddress addr = createAddress(id);
            em.persist(addr);

            FGDepartment dept = createDepartment(id);
            em.persist(dept);

            FGManager mgr = new FGManager();
            mgr.setId(id);
            mgr.setFirstName("First-" + id);
            mgr.setLastName("Last-" + id);
            mgr.setMData("MData-" + id);
            mgr.setRating("Rating-" + id);
            mgr.setDescription("Manager-" + id);
            mgr.setAddress(addr);
            mgr.setDept(dept);

            em.persist(mgr);

            managers.put(mgr.getId(), mgr);
        }

        // Create Employees
        for (int i = 0; i < EMPLOYEES_COUNT; i++) {
            int id = empIdIndex++;
            int mgrId = (id % MANAGERS_COUNT) + 1;

            FGAddress addr = createAddress(id);
            em.persist(addr);

            FGDepartment dept = createDepartment(id);
            em.persist(dept);

            FGEmployee emp = new FGEmployee();
            emp.setId(id);
            emp.setFirstName("First-" + id);
            emp.setLastName("Last-" + id);
            emp.setRating("Rating-" + id);
            emp.setDescription("Employee-" + id);
            emp.setAddress(addr);
            emp.setDept(dept);
            emp.setManager(managers.get(mgrId));

            em.persist(emp);

            employees.add(emp);
        }

    }

    private FGAddress createAddress(int id) {
        FGAddress addr = new FGAddress();
        addr.setId(id);
        addr.setStreet("Street-" + id);
        addr.setCity("City-" + id);
        addr.setState("State-" + id);
        addr.setZip(id);

        return addr;
    }

    private FGDepartment createDepartment(int id) {
        FGDepartment dept = new FGDepartment();
        dept.setId(id);
        dept.setName("Department-" + id);

        return dept;
    }

}
