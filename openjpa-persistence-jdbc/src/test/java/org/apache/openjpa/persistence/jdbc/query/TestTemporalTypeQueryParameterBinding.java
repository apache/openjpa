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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.jdbc.query.domain.TimeKeeper;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that queries can convert to temporal types.
 * Also tests that parameter validation for mismatch between named parameter
 * and positional binding or vice versa.
 *  
 * Originally reported in 
 * <A HRE="http://issues.apache.org/jira/browse/OPENJPA-112>OPENJPA-497</A>
 *  
 * @author Pinaki Poddar
 *
 */
public class TestTemporalTypeQueryParameterBinding extends SingleEMFTestCase {
	private static Calendar PARAM_CALENDAR = Calendar.getInstance();
	private  static long T1 = PARAM_CALENDAR.getTimeInMillis();
	private  static long T2 = T1 + 2000; 
	private  static long T3 = T1 + 3000;
	
	private static Date     VALUE_DATE     = new Date(T1);
	private static Time     VALUE_TIME     = new Time(T2);
	private static Timestamp VALUE_TSTAMP   = new Timestamp(T3);
	
	
	private static String JPQL_NAMED  = 
		"SELECT p FROM TimeKeeper p " + 
		"WHERE p.date=:d AND p.time=:t AND p.tstamp=:ts";
	private static String JPQL_POSITIONAL  = 
		"SELECT p FROM TimeKeeper p " + 
		"WHERE p.date=?1 AND p.time=?2 AND p.tstamp=?3";
	
	private EntityManager em;
	@Override
	public void setUp() throws Exception {
		super.setUp(CLEAR_TABLES, TimeKeeper.class);
		em = emf.createEntityManager();
		
		TimeKeeper pc = new TimeKeeper();
		pc.setDate(VALUE_DATE);
		pc.setTime(VALUE_TIME);
		pc.setTstamp(VALUE_TSTAMP);
		
		em.getTransaction().begin();
		em.persist(pc);
		em.getTransaction().commit();
	}
	
	public void testNamedParameterConvertedFromCalendarValue() {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		Calendar c3 = Calendar.getInstance();
		c1.setTimeInMillis(T1);
		c2.setTimeInMillis(T2);
		c3.setTimeInMillis(T3);
		
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("d",  c1, TemporalType.DATE);
		q.setParameter("t",  c2, TemporalType.TIME);
		q.setParameter("ts", c3, TemporalType.TIMESTAMP);
		
		assertEquals(1, q.getResultList().size());
	}
	
	public void testPositionalParameterConvertedFromCalendarValue() {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		Calendar c3 = Calendar.getInstance();
		c1.setTimeInMillis(T1);
		c2.setTimeInMillis(T2);
		c3.setTimeInMillis(T3);
		
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  c1, TemporalType.DATE);
		q.setParameter(2,  c2, TemporalType.TIME);
		q.setParameter(3,  c3, TemporalType.TIMESTAMP);
		
		assertEquals(1, q.getResultList().size());
	}
	public void testNamedParameterConvertedFromDateValue() {
		Date d1 = new Date(T1);
		Date d2 = new Date(T2);
		Date d3 = new Date(T3);
		
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("d",  d1, TemporalType.DATE);
		q.setParameter("t",  d2, TemporalType.TIME);
		q.setParameter("ts", d3, TemporalType.TIMESTAMP);
		
		assertEquals(1, q.getResultList().size());
	}
	
	public void testPositionalParameterConvertedFromDateValue() {
		Date d1 = new Date(T1);
		Date d2 = new Date(T2);
		Date d3 = new Date(T3);
		
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  d1, TemporalType.DATE);
		q.setParameter(2,  d2, TemporalType.TIME);
		q.setParameter(3,  d3, TemporalType.TIMESTAMP);
		
		assertEquals(1, q.getResultList().size());
	}
	
	
	public void testNamedParameterWithMismatchedValue() {
		Date d1 = new Date(T1);
		Date d2 = new Date(T2);
		Date d3 = new Date(T3);
		
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("d",  d1, TemporalType.TIME);
		q.setParameter("ts",  d2, TemporalType.TIMESTAMP);
		
		try {
	        q.setParameter("t",  d3, TemporalType.DATE);
			fail("Expeceted " + ArgumentException.class.getName());
		} catch (IllegalArgumentException ex) {
			// good
		}
	}
	
	public void testPositionalParameterWithMismatchedValue() {
		Date d1 = new Date(T1);
		Date d2 = new Date(T2);
		Date d3 = new Date(T3);
		
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  d1, TemporalType.TIME);
		
		try {
	        q.setParameter(2,  d2, TemporalType.TIMESTAMP);
			fail("Expeceted " + ArgumentException.class.getName());
		} catch (IllegalArgumentException ex) {
		    // expected.
		}
        try {
            q.setParameter(3,  d3, TemporalType.DATE);
            fail("Expeceted " + ArgumentException.class.getName());
        } catch (IllegalArgumentException ex) {
            // expected.
        }
	}
	
	void verifyParams(String jpql, Class<? extends Exception> error,
        Object... params) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
		Query query = em.createNativeQuery(jpql);
		for (int i=0; params != null && i<params.length; i=+2) {
			try {
				if (params[i] instanceof Number) {
                    query.setParameter(((Number) params[i]).intValue(),
                        params[i + 1]);
                } else {
                    query.setParameter(params[i].toString(), params[i+1]);
				}
				if (error != null)
					fail("Expected " + error.getName());
			} catch (Exception e) {
				if (!error.isAssignableFrom(e.getClass())) {
				    // let the test harness handle the exception
                    throw new RuntimeException("An unexpected exception " +
                            "occurred see initCause for details", e);
				}
			} 
		}
		em.getTransaction().commit();
	}
}
