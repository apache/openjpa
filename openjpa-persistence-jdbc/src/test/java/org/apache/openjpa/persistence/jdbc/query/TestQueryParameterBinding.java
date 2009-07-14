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
package org.apache.openjpa.persistence.jdbc.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.jdbc.query.domain.Binder;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests validation of positional and named parameter binding for JPQL queries.
 *  
 *  
 * @author Pinaki Poddar
 *
 */
public class TestQueryParameterBinding extends SingleEMFTestCase {
	private static String JPQL = "SELECT p FROM Binder p ";
	
	private static int INT_VALUE    = 1;
	private static String STR_VALUE = "2";
	private static double DBL_VALUE = 3.0;
	
	private EntityManager em;
	
	@Override
	public void setUp() throws Exception {
		super.setUp(CLEAR_TABLES, Binder.class);
		
		em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(new Binder(INT_VALUE, STR_VALUE, DBL_VALUE));
		em.getTransaction().commit();
	}
	
	public void testPositionalParameterWithPositionalBindingSucceeds() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);
		q.setParameter(3, DBL_VALUE);
		
		assertEquals(1, q.getResultList().size());
	}
	
	public void testPositionalParameterWithNamedBindingFails() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);
		q.setParameter("p3", DBL_VALUE);
		
		fail(q);
	}
	
	public void testPositionalParameterWithInsufficientValuesFails() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);
		
		fail(q);
	}
	
	public void testPositionalParameterWithExtraValuesFails() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);
		q.setParameter(3, DBL_VALUE);
		q.setParameter(4, 4);
		
		fail(q);
	}

	public void testPositionalParameterWithRepeatedValuesSucceeds() {
		String jPQL_POSITIONAL_REPEATED_PARAM  = 
			JPQL + "WHERE p.p1=?1 OR p.p1=?1 AND p.p3=?2";
		Query q = em.createQuery(jPQL_POSITIONAL_REPEATED_PARAM);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  DBL_VALUE);
		
		assertEquals(1,q.getResultList().size());
	}
	
	public void testPositionalParameterWithGapSucceeds() {
		String JPQL_POSITIONAL_GAP_IN_PARAM  = 
			JPQL + "WHERE p.p1=?1 AND p.p2=?3";
		Query q = em.createQuery(JPQL_POSITIONAL_GAP_IN_PARAM);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(3,  STR_VALUE);
		
		assertEquals(1,q.getResultList().size());
	}
	
	public void testPositionalParameterWithGapFails() {
		String JPQL_POSITIONAL_GAP_IN_PARAM  = 
			JPQL + "WHERE p.p1=?1 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL_GAP_IN_PARAM);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  STR_VALUE);
		q.setParameter(3,  DBL_VALUE);
		
		fail(q);
	}
	
	public void testNamedParameterWithNamedBindingSucceeds() {
		String JPQL_NAMED  = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);
		q.setParameter("p3", DBL_VALUE);
		
		assertEquals(1, q.getResultList().size());
	}
	
	public void testNamedParameterWithPositionalBindingFails() {
		String JPQL_NAMED  = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);
		q.setParameter(3, DBL_VALUE);
		
		fail(q);
	}
	
	public void testNamedParameterWithInsufficientValuesFails() {
		String JPQL_NAMED  = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);
		
		fail(q);
	}
	
	public void testNamedParameterWithExtraValuesFails() {
		String JPQL_NAMED  = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);
		q.setParameter("p3", DBL_VALUE);
		q.setParameter("p4", 4);
		
		fail(q);
	}

	public void testNamedParameterWithRepeatedValuesSucceeds() {
		String jPQL_NAMED_REPEATED_PARAM  = 
			JPQL + "WHERE p.p1=:p1 OR p.p1=:p1 AND p.p3=:p2";
		Query q = em.createQuery(jPQL_NAMED_REPEATED_PARAM);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p2",  DBL_VALUE);
		
		assertEquals(1,q.getResultList().size());
	}
	
	public void testNamedParameterWithGapSucceeds() {
		String JPQL_NAMED_GAP_IN_PARAM  = 
			JPQL + "WHERE p.p1=:p1 AND p.p2=:p3";
		Query q = em.createQuery(JPQL_NAMED_GAP_IN_PARAM);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p3",  STR_VALUE);
		
		assertEquals(1,q.getResultList().size());
	}
	
	public void testNamedParameterWithGapFails() {
		String JPQL_NAMED_GAP_IN_PARAM  = 
			JPQL + "WHERE p.p1=:p1 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED_GAP_IN_PARAM);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p2",  STR_VALUE);
		q.setParameter("p3",  DBL_VALUE);
		
		fail(q);
	}
	
	public void testNamedParameterWithWrongType() {
		String JPQL_NAMED  = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p2",  DBL_VALUE);
		q.setParameter("p3",  STR_VALUE);
		
		fail(q);
	}
	
	public void testPositionalParameterWithWrongType() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  DBL_VALUE);
		q.setParameter(3,  STR_VALUE);
		
		fail(q);
	}
	
	public void testNamedParameterWithNullValue() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p2",  null);
		q.setParameter("p3",  null);
		
		fail(q);
	}
	
	public void testPositionalParameterWithNullValue() {
		String JPQL_POSITIONAL  = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  null);
		q.setParameter(3,  null);
		
		fail(q);
	}
	
	public void testPositionalParameterWithSingleResult() {
		Query q = em.createNamedQuery("JPQL_POSITIONAL");
		// "SELECT p FROM Binder p WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  null);
		q.setParameter(3,  null);
		
		fail(q, true);
	}
	
	public void testPositionalParameterWithNativeQuery() {
		Query q = em.createNamedQuery("SQL_POSITIONAL");
		// "SELECT p.id FROM Binder WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  STR_VALUE);
		q.setParameter(3,  DBL_VALUE);
		
		assertEquals(1,q.getResultList().size());
	}
	
	public void testPositionalParameterWithNativeQueryFails() {
		Query q = em.createNamedQuery("SQL_POSITIONAL");
		// "SELECT p.id FROM Binder WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  STR_VALUE);
		
		fail(q);
	}
	
//	@AllowFailure
//	public void testPositionalParameterWithNativeQueryFailsWithGap() {
//		Query q = em.createNamedQuery("SQL_POSITIONAL");
//		// "SELECT p.id FROM Binder WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
//		q.setParameter(1,  INT_VALUE);
//		q.setParameter(3,  DBL_VALUE);
//		
//		fail(q);
//	}
	
	
	void fail(Query q) {
		fail(q, false);
	}
	
	void fail(Query q, boolean single) {
		try {
			if (single) 
				q.getSingleResult();
			else 
				q.getResultList();
			fail("Expeceted " + ArgumentException.class.getName());
		} catch (IllegalArgumentException ex) {
		// good
			System.err.println("*** ERROR " + getName());
			System.err.println("*** ERROR " + ex.getMessage());
		}
	}
	
}
