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
package org.apache.openjpa.slice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestXA extends SliceTestCase {
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(PObject.class, Person.class, Address.class, CLEAR_TABLES);
    }

    @Test
    void emptyCommit() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.getTransaction().commit();
    }

    @Test
    void emptyRollback() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.getTransaction().rollback();
    }

    @Test
    void persistIndependentObjects() {
		EntityManager em = emf.createEntityManager();
        int before = count(PObject.class);
		int N = 2;
		em.getTransaction().begin();
		for (int i=0; i<N; i++)
			em.persist(new PObject());
		em.getTransaction().commit();
		em.clear();
		int after = count(PObject.class);
		assertEquals(before+N, after);
	}

    @Override
    protected String getPersistenceUnitName() {
        return "XA";
    }


}
