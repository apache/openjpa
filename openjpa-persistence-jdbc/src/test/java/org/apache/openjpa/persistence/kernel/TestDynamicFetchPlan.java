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
package org.apache.openjpa.persistence.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.apache.openjpa.meta.FetchGroup;
import org.apache.openjpa.persistence.FetchPlan;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.kernel.common.apps.FetchA;
import org.apache.openjpa.persistence.kernel.common.apps.FetchB;
import org.apache.openjpa.persistence.kernel.common.apps.FetchBase;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests behavior of FetchPlan constructed dynamically by adding fields.
 *
 * Originally reported by Michael Vorburger in
 * <A HREF="http://n2.nabble.com/Fetch-Group-questions-tc534861.html">OpenJPA
 * user group</A>
 *
 * @author Pinaki Poddar
 *
 */
public class TestDynamicFetchPlan extends SingleEMFTestCase {
	private static final String JPQL = "select a from FetchA a";

    @BeforeEach
    public void setUp() {
        super.setUp(CLEAR_TABLES, FetchBase.class, FetchA.class, FetchB.class);
		createData();
	}

	public void createData() {
		OpenJPAEntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		FetchA a1 = new FetchA();
		a1.setText("a1");
		FetchB b1 = new FetchB();
		b1.setText("b1");
		a1.setB(b1);
		em.persist(a1);
		em.persist(b1);
		em.getTransaction().commit();
	}

    @Test
    void fetchBySubClassFieldB() {
		OpenJPAEntityManager em = emf.createEntityManager();
		FetchPlan fp = em.getFetchPlan();
		fp.setExtendedPathLookup(true);
		fp.clearFetchGroups().removeFetchGroup(FetchGroup.NAME_DEFAULT);
		fp.clearFields();
		fp.addField(FetchA.class, "b");
		fp.addField(FetchB.class, "text");

		FetchA a = (FetchA) em.createQuery(JPQL).getSingleResult();
		em.close();

		FetchB  b = a.getB();
		assertNotNull(b);
		assertNull(a.getText());
		assertEquals("b1", b.getText());
	}

    @Test
    void fetchBySubClassFieldA() {
		OpenJPAEntityManager em = emf.createEntityManager();
		FetchPlan fp = em.getFetchPlan();
        fp.setExtendedPathLookup(true);
		fp.clearFetchGroups().removeFetchGroup(FetchGroup.NAME_DEFAULT);
		fp.clearFields();
		fp.addField(FetchA.class, "b");
		fp.addField(FetchA.class, "text");

		FetchA a = (FetchA) em.createQuery(JPQL).getSingleResult();
		em.close();

		FetchB  b = a.getB();
		assertNotNull(b);
		assertEquals("a1", a.getText());
		assertNull(b.getText());
	}

    @Test
    void fetchBySuperClassField() {
		OpenJPAEntityManager em = emf.createEntityManager();
		FetchPlan fp = em.getFetchPlan();
        fp.setExtendedPathLookup(true);
		fp.clearFetchGroups().removeFetchGroup(FetchGroup.NAME_DEFAULT);
		fp.clearFields();
		fp.addField(FetchA.class, "b");
		fp.addField(FetchBase.class, "text");

		FetchA a = (FetchA) em.createQuery(JPQL).getSingleResult();
		em.close();

		FetchB  b = a.getB();
		assertNotNull(b);
		assertEquals("a1", a.getText());
		assertEquals("b1", b.getText());
	}

    @Test
    void fetchBySubClassFieldNameB() {
		OpenJPAEntityManager em = emf.createEntityManager();
		FetchPlan fp = em.getFetchPlan();
        fp.setExtendedPathLookup(true);
		fp.clearFetchGroups().removeFetchGroup(FetchGroup.NAME_DEFAULT);
		fp.clearFields();
		fp.addField(FetchA.class.getName() + ".b");
		fp.addField(FetchB.class.getName() + ".text");

		FetchA a = (FetchA) em.createQuery(JPQL).getSingleResult();
		em.close();

		FetchB  b = a.getB();
		assertNotNull(b);
		assertNull(a.getText());
		assertEquals("b1", b.getText());
	}

    @Test
    void fetchBySubClassFieldNameA() {
		OpenJPAEntityManager em = emf.createEntityManager();
		FetchPlan fp = em.getFetchPlan();
        fp.setExtendedPathLookup(true);
		fp.clearFetchGroups().removeFetchGroup(FetchGroup.NAME_DEFAULT);
		fp.clearFields();
		fp.addField(FetchA.class.getName() + ".b");
		fp.addField(FetchA.class.getName() + ".text");

		FetchA a = (FetchA) em.createQuery(JPQL).getSingleResult();
		em.close();

		FetchB  b = a.getB();
		assertNotNull(b);
		assertEquals("a1", a.getText());
		assertNull(b.getText());
	}

    @Test
    void fetchBySuperClassFieldName() {
		OpenJPAEntityManager em = emf.createEntityManager();
		FetchPlan fp = em.getFetchPlan();
        fp.setExtendedPathLookup(true);
		fp.clearFetchGroups().removeFetchGroup(FetchGroup.NAME_DEFAULT);
		fp.clearFields();
		fp.addField(FetchA.class.getName() + ".b");
		fp.addField(FetchBase.class.getName() + ".text");

		FetchA a = (FetchA) em.createQuery(JPQL).getSingleResult();
		em.close();

		FetchB  b = a.getB();
		assertNotNull(b);
		assertEquals("a1", a.getText());
		assertEquals("b1", b.getText());
	}

    // OPENJPA-2413: FetchPlan.clearFetchGroups() does not retain "default" in list of active Fetch Groups.
    @Test
    void clearFetchPlan() {
	    OpenJPAEntityManager em = emf.createEntityManager();
	    FetchPlan fp = em.getFetchPlan();

	    // Make sure "default" is present in the list of active FetchGroups
	    Collection<String> fetchGroups = fp.getFetchGroups();
	    assertNotNull(fetchGroups);
	    assertTrue(fetchGroups.contains(FetchGroup.NAME_DEFAULT));

	    // Clear all active FetchGroups, only "default" should remain.
	    fp.clearFetchGroups();
	    Collection<String> fetchGroupsAfterClear = fp.getFetchGroups();
	    assertNotNull(fetchGroupsAfterClear);
	    assertTrue(fetchGroupsAfterClear.contains(FetchGroup.NAME_DEFAULT));

	    // Should still be able to remove the "default" FetchGroup
	    fp.removeFetchGroup(FetchGroup.NAME_DEFAULT);
	    Collection<String> fetchGroupsAfterRemove = fp.getFetchGroups();
	    assertNotNull(fetchGroupsAfterClear);
	    assertTrue(fetchGroupsAfterClear.isEmpty());
	}
}
