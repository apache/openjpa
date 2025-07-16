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
package org.apache.openjpa.persistence;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;

public class TestEntityManagerFactoryTransactions {
	
	private EntityManagerFactory emf;
	
	@Before
	public void before() {
		PersistenceConfiguration conf = new PersistenceConfiguration("test");
		conf.provider(PersistenceProviderImpl.class.getCanonicalName());
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");
		conf.managedClass(Country.class);
		
		emf = Persistence.createEntityManagerFactory(conf);
	}
	
	public void after() {
		if (emf.isOpen()) {
			emf.close();
		}
	}

	@Test
	public void testCallInTransaction() {
		Country country = emf.callInTransaction(em -> {
			Country c = new Country(250l, "France");
			em.persist(c);
			return c;
		});
		EntityManager em = emf.createEntityManager();
		Country c1 = em.find(Country.class, 250l);
		assertNotNull(c1);
		assertEquals(country.getId(), c1.getId());
		assertEquals("France", c1.getName());
		em.close();
	}
	
	@Test
	public void testRunInTransaction() {
		Country a = new Country(36, "Australia");
		emf.runInTransaction(em -> em.persist(a));
		EntityManager em = emf.createEntityManager();
		assertNotNull(em.find(Country.class, 36));
		em.close();
	}
	
	@Test
	public void testCallInTransactionException() {
		try {
			Country b = new Country(44, "Bahamas");
			emf.runInTransaction(em -> {
				em.persist(b);
				throw new IllegalStateException("some-exception");
			});
			fail("Should have rethrown exception");
		} catch (Throwable t) {
			assertTrue(t.getMessage().startsWith("some-exception"));
		}
	}

}
