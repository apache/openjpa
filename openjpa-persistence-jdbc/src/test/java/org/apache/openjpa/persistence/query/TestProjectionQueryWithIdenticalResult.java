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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.jdbc.common.apps.
        UnidirectionalOneToOneOwned;
import org.apache.openjpa.persistence.jdbc.common.apps.
        UnidirectionalOneToOneOwner;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * The query uses projection and result contains the same instance once as
 * a direct projection and again as a fetch group of the other projection.
 * Does the query return two separate instances or one identical instance?
 *
 * Originally reported as two different failures:
 * <A HREF="https://issues.apache.org/jira/browse/OPENJPA-209">OPENJPA-209</A>
 * <A HREF="https://issues.apache.org/jira/browse/OPENJPA-210">OPENJPA-210</A>
 *
 * @author Pinaki Poddar
 */
class TestProjectionQueryWithIdenticalResult extends SingleEMFTestCase {
	private static boolean USE_TXN = true;

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES,
        	  UnidirectionalOneToOneOwned.class,
        	  UnidirectionalOneToOneOwner.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        UnidirectionalOneToOneOwner owner = new UnidirectionalOneToOneOwner();
        owner.setMarker("Owner");
        UnidirectionalOneToOneOwned owned = new UnidirectionalOneToOneOwned();
        owned.setMarker("Owned");
        owner.setOwned(owned);
        em.persist(owner);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void duplicateResultInProjection1() {
        String jpql = "SELECT p.owned, p FROM UnidirectionalOneToOneOwner p";
		List<Object[]> result = executeQuery(jpql, USE_TXN);
		for (Object[] row : result) {
            assertInstanceOf(UnidirectionalOneToOneOwned.class, row[0]);
            assertInstanceOf(UnidirectionalOneToOneOwner.class, row[1]);
            assertSame(((UnidirectionalOneToOneOwner)row[1]).getOwned(),
                row[0]);
		}
	}

    @Test
    void duplicateResultInProjection2() {
        String jpql = "SELECT p, p.owned FROM UnidirectionalOneToOneOwner p";
		List<Object[]> result = executeQuery(jpql, USE_TXN);
		for (Object[] row : result) {
            assertInstanceOf(UnidirectionalOneToOneOwned.class, row[1]);
            assertInstanceOf(UnidirectionalOneToOneOwner.class, row[0]);
            assertSame(((UnidirectionalOneToOneOwner)row[0]).getOwned(),
                row[1]);
		}
	}

    @Test
    void duplicateResultInProjection3() {
        String jpql = "SELECT p, q FROM UnidirectionalOneToOneOwner p, " +
                      "UnidirectionalOneToOneOwned q WHERE p.owned = q";
		List<Object[]> result = executeQuery(jpql, USE_TXN);
		for (Object[] row : result) {
            assertInstanceOf(UnidirectionalOneToOneOwner.class, row[0]);
            assertInstanceOf(UnidirectionalOneToOneOwned.class, row[1]);
            assertSame(((UnidirectionalOneToOneOwner)row[0]).getOwned(),
                row[1]);
		}
	}

    @Test
    void duplicateResultInProjection4() {
        String jpql = "SELECT q, p FROM UnidirectionalOneToOneOwner p, " +
                      "UnidirectionalOneToOneOwned q WHERE p.owned = q";
		List<Object[]> result = executeQuery(jpql, USE_TXN);
		for (Object[] row : result) {
            assertInstanceOf(UnidirectionalOneToOneOwned.class, row[0]);
            assertInstanceOf(UnidirectionalOneToOneOwner.class, row[1]);
            assertSame(((UnidirectionalOneToOneOwner)row[1]).getOwned(),
                row[0]);
		}
	}

	private List executeQuery(String jpql, boolean useTxn) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		List result = em.createQuery(jpql).getResultList();
		em.getTransaction().rollback();
		return result;
	}
}
