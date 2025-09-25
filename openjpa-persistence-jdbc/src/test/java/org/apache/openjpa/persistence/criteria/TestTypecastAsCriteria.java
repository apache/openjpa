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
package org.apache.openjpa.persistence.criteria;

import java.time.LocalDateTime;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;

import org.apache.openjpa.jdbc.sql.DerbyDictionary;

public class TestTypecastAsCriteria extends CriteriaTest {

    public void testTypecastAsString() {
    	if (getDictionary() instanceof DerbyDictionary) {
    		// Derby does not support these CASTs
    		return;
    	}
    	String jpql = "SELECT c FROM Customer c JOIN c.orders o WHERE CAST(o.quantity AS String) = '0'";
    	CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
    	Root<Customer> c = q.from(Customer.class);
    	SetJoin<Customer, Order> o = c.joinSet("orders");
    	q.where(cb.equal(o.get("quantity").cast(String.class), "0"));
    	q.select(c);
    	
    	assertEquivalence(q, jpql);
    }

    public void testTypecastAsInteger() {
    	if (getDictionary() instanceof DerbyDictionary) {
    		// Derby does not support these CASTs
    		return;
    	}
    	String jpql = "SELECT c FROM Customer c JOIN c.orders o WHERE CAST(CAST(o.quantity AS STRING) AS INTEGER) = 0";
    	CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
    	Root<Customer> c = q.from(Customer.class);
    	SetJoin<Customer, Order> o = c.joinSet("orders");
    	q.where(cb.equal(o.get("quantity").cast(String.class).cast(Integer.class), 0));
    	q.select(c);
    	
    	assertEquivalence(q, jpql);
    }

    public void testTypecastAsLong() {
    	if (getDictionary() instanceof DerbyDictionary) {
    		// Derby does not support these CASTs
    		return;
    	}
    	String jpql = "SELECT c FROM Customer c JOIN c.orders o WHERE CAST(CAST(o.quantity AS STRING) AS LONG) = 0";
    	CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
    	Root<Customer> c = q.from(Customer.class);
    	SetJoin<Customer, Order> o = c.joinSet("orders");
    	q.where(cb.equal(o.get("quantity").cast(String.class).cast(Long.class), 0l));
    	q.select(c);
    	
    	assertEquivalence(q, jpql);
    }

    public void testTypecastAsFloat() {
    	if (getDictionary() instanceof DerbyDictionary) {
    		// Derby does not support these CASTs
    		return;
    	}
    	String jpql = "SELECT c FROM Customer c JOIN c.orders o WHERE CAST(CAST(o.quantity AS STRING) AS float) = 0";
    	CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
    	Root<Customer> c = q.from(Customer.class);
    	SetJoin<Customer, Order> o = c.joinSet("orders");
    	q.where(cb.equal(o.get("quantity").cast(String.class).cast(Float.class), 0f));
    	q.select(c);
    	
    	assertEquivalence(q, jpql);
    }

    public void testTypecastAsDouble() {
    	if (getDictionary() instanceof DerbyDictionary) {
    		// Derby does not support these CASTs
    		return;
    	}
    	String jpql = "SELECT c FROM Customer c JOIN c.orders o WHERE CAST(cast(o.quantity as string) AS double) = 0";
    	CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
    	Root<Customer> c = q.from(Customer.class);
    	SetJoin<Customer, Order> o = c.joinSet("orders");
    	q.where(cb.equal(o.get("quantity").cast(String.class).cast(Double.class), 0d));
    	q.select(c);
    	
    	assertEquivalence(q, jpql);
    }
    
    public void testTypecastAsInvalid() {
    	if (getDictionary() instanceof DerbyDictionary) {
    		// Derby does not support these CASTs
    		return;
    	}
    	CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
    	Root<Customer> c = q.from(Customer.class);
    	SetJoin<Customer, Order> o = c.joinSet("orders");
    	try {
    		q.where(cb.equal(o.get("quantity").cast(LocalDateTime.class), LocalDateTime.now()));
    		q.select(c);
    		fail("Should have thrown IllegalTargetException");
    	} catch (Throwable t) {
    		assertTrue(t.getClass() == IllegalArgumentException.class);
    	}
    }
}
