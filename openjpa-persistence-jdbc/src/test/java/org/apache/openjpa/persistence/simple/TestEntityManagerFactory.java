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
package org.apache.openjpa.persistence.simple;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;

import org.apache.openjpa.persistence.OpenJPAPersistence;

import junit.framework.TestCase;

public class TestEntityManagerFactory extends TestCase {

    /*
     * This test uses a mis-configured persistence unit to verify that we will
     * not connect to the database when an unused emf is closed.
     */
    public void testCloseUnusedEMF() {
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("invalid");
        emf.close();
    }
	
	public void testEMFCreation() {
		PersistenceConfiguration conf = new PersistenceConfiguration("dynamicaly-created-pu");
		conf.managedClass(AllFieldTypes.class);
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");
		
		EntityManagerFactory emf = Persistence.createEntityManagerFactory(conf);
		assertNotNull(emf);
		EntityManager em = emf.createEntityManager();
		assertNotNull(em);
		
		String countJPQL = "SELECT COUNT(DISTINCT a) FROM AllFieldTypes AS a";
		long count = em.createQuery(countJPQL, Long.class).getSingleResult();
		assertEquals(0L, count);
		
		AllFieldTypes aft = new AllFieldTypes();
		em.getTransaction().begin();
		em.persist(aft);
		em.getTransaction().commit();
		
		assertNotNull(aft.getUniqueId());
		count = em.createQuery(countJPQL, Long.class).getSingleResult();
		assertEquals(1l, count);
		
		em.close();
		emf.close();
	}

	public void testEMFCreationDoesNotMutateConfiguration() {
		PersistenceConfiguration conf = new PersistenceConfiguration("dynamicaly-created-pu");
		conf.managedClass(AllFieldTypes.class);
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");
		Map<String, Object> before = new HashMap<>(conf.properties());

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(conf);
		emf.close();

		assertEquals(before, conf.properties());
	}

	public void testRepeatedEMFCreationFromSameConfiguration() {
		PersistenceConfiguration conf = new PersistenceConfiguration("dynamicaly-created-pu");
		conf.managedClass(AllFieldTypes.class);
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");

		EntityManagerFactory emf1 = Persistence.createEntityManagerFactory(conf);
		String mdf1 = OpenJPAPersistence.cast(emf1).getConfiguration().getMetaDataFactory();
		emf1.close();
		EntityManagerFactory emf2 = Persistence.createEntityManagerFactory(conf);
		String mdf2 = OpenJPAPersistence.cast(emf2).getConfiguration().getMetaDataFactory();
		assertCount(emf2, 0L);
		emf2.close();

		assertEquals(mdf1, mdf2);
	}

	public void testEMFCreationMergesUserMetaDataFactory() {
		PersistenceConfiguration conf = new PersistenceConfiguration("dynamicaly-created-pu");
		conf.managedClass(AllFieldTypes.class);
		conf.property("openjpa.MetaDataFactory", "jpa");
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(conf);
		assertEquals("jpa(Types=" + AllFieldTypes.class.getName() + ")",
				OpenJPAPersistence.cast(emf).getConfiguration().getMetaDataFactory());
		assertCount(emf, 0L);
		emf.close();
	}

	public void testEMFCreationHonoursMappingFiles() {
		PersistenceConfiguration conf = new PersistenceConfiguration("dynamicaly-created-pu");
		conf.managedClass(AllFieldTypes.class);
		conf.mappingFile("org/apache/openjpa/persistence/simple/persistence-configuration-orm.xml");
		conf.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");

		EntityManagerFactory emf = Persistence.createEntityManagerFactory(conf);
		EntityManager em = emf.createEntityManager();
		long count = em.createNamedQuery("AllFieldTypes.countFromMappingFile", Long.class).getSingleResult();
		assertEquals(0L, count);
		em.close();
		emf.close();
	}

	private void assertCount(EntityManagerFactory emf, long expected) {
		EntityManager em = emf.createEntityManager();
		long count = em.createQuery("SELECT COUNT(a) FROM AllFieldTypes AS a", Long.class).getSingleResult();
		assertEquals(expected, count);
		em.close();
	}

}
