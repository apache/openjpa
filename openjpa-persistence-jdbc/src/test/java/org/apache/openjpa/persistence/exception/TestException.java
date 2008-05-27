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
package org.apache.openjpa.persistence.exception;

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests proper JPA exceptions are raised by the implementation. 
 */
public class TestException extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(PObject.class, CLEAR_TABLES);
    }
    
	/**
	 * Tests that when Optimistic transaction consistency is violated, the
	 * exception thrown is an instance of javax.persistence.OptimisticException.
	 */
	public void testThrowsJPADefinedOptimisticException() {
		EntityManager em1 = emf.createEntityManager();
		EntityManager em2 = emf.createEntityManager();
		assertNotEquals(em1, em2);
		
		em1.getTransaction().begin();
		PObject pc = new PObject();
		em1.persist(pc);
		em1.getTransaction().commit();
		Object id = pc.getId();
		em1.clear();
		
		em1.getTransaction().begin();
		em2.getTransaction().begin();
		
		PObject pc1 = em1.find(PObject.class, id);
		PObject pc2 = em2.find(PObject.class, id);
		
		assertTrue(pc1 != pc2);
		
		pc1.setName("Modified in TXN1");
		em1.flush();
		try {
			pc2.setName("Modified in TXN2");
			em2.flush();
			fail("Expected optimistic exception on flush");
		} catch (Throwable t) {
			if (!isExpectedException(t, OptimisticLockException.class)) {
				print(t);
				fail(t.getCause().getClass() + " is not " + 
						OptimisticLockException.class);
			}
		}
		
		em1.getTransaction().commit();
		try {
			em2.getTransaction().commit();
			fail("Expected optimistic exception on commit");
		} catch (Throwable t) {
			if (!isExpectedException(t, OptimisticLockException.class)) {
				print(t);
				fail(t.getCause().getClass() + " is not " + 
						OptimisticLockException.class);
			}
		}
	}
	
	boolean isExpectedException(Throwable t, Class expectedType) {
		if (t == null) return false;
		if (expectedType.isAssignableFrom(t.getClass()))
				return true;
		if (t.getCause()==t) return false;
		return isExpectedException(t.getCause(), expectedType);
	}
	
	void print(Throwable t) {
		print(t, 0);
	}
	
	void print(Throwable t, int tab) {
		if (t == null) return;
		for (int i=0; i<tab*4;i++) System.out.print(" ");
		String sqlState = (t instanceof SQLException) ? 
			"(SQLState=" + ((SQLException)t).getSQLState() + ":" + t.getMessage() + ")":"";
		System.out.println(t.getClass().getName() + sqlState);
		if (t.getCause()==t) return;
		print(t.getCause(), tab+1);
	}
}
