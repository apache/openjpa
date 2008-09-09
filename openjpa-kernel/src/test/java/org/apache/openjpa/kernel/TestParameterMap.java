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
package org.apache.openjpa.kernel;

import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.util.ParameterMap;

import junit.framework.TestCase;

/**
 * Test the properties of ParameterMap.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestParameterMap extends TestCase {
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testTypeIsDeterminedOnFirstInsertion() {
		ParameterMap m1 = new ParameterMap();
		assertFalse(m1.isNamed());
		assertFalse(m1.isPositional());
		m1.put("key", "value");
		assertTrue(m1.isNamed());
		assertFalse(m1.isPositional());
		
		ParameterMap m2 = new ParameterMap();
		assertFalse(m2.isNamed());
		assertFalse(m2.isPositional());
		m2.put(1, "value");
		assertFalse(m2.isNamed());
		assertTrue(m2.isPositional());
	}
	
	public void testTypeIsDeterminedOnConstruction() {
		assertTrue(new ParameterMap(ParameterMap.Type.NAMED).isNamed());
		assertTrue(new ParameterMap(ParameterMap.Type.POSITIONAL).isPositional());
		
	}
	
	public void testConvertNamedToPositional() {
		ParameterMap m = new ParameterMap();
		m.put("key1", "value1");
		m.put("key2", "value2");
		
		ParameterMap positional = m.toPositional();
		assertTrue(positional.isPositional());
		assertEquals("value1", positional.get(1));
		assertEquals("value2", positional.get(2));
	}
	
	public void testConvertPositionalToPositional() {
		ParameterMap m = new ParameterMap();
		m.put(2, "value2");
		m.put(1, "value1");
		
		ParameterMap positional = m.toPositional();
		assertTrue(positional.isPositional());
		assertEquals("value1", positional.get(1));
		assertEquals("value2", positional.get(2));
	}
	
	public void testNamedKeysAreInInsertionOrder() {
		ParameterMap m = new ParameterMap();
		m.put("key1", "value1");
		m.put("key2", "value2");
		
		Iterator keys = m.keySet().iterator();
		assertEquals("key1", keys.next());
		assertEquals("key2", keys.next());
		
		// Update an old key and insert a new one
		m.put("key1", "new value in old key");
		m.put("key3", "new value");
		keys = m.keySet().iterator();
		assertEquals("key1", keys.next());
		assertEquals("key2", keys.next());
		assertEquals("key3", keys.next());
		
	}
	
	public void testNamedValuesInInsertionOrder() {
		ParameterMap map = new ParameterMap();
		map.put("key1", "value1");
		map.put("key2", "value2");
		Iterator values = map.values().iterator();
		assertEquals("value1", values.next());
		assertEquals("value2", values.next());

		// Update an old key and insert a new one
		map.put("key1", "new value in old key");
		map.put("key3", "new value");
		values = map.values().iterator();
		assertEquals("new value in old key", values.next());
		assertEquals("value2", values.next());
		assertEquals("new value", values.next());
	}

	public void testNamedMapInInsertionOrder() {
		ParameterMap map = new ParameterMap();
		map.setValueType("key1", int.class);
		map.setValueType("key2", String.class);
		
		map.put("key1", 5);
		map.put("key2", "value2");
		
		Iterator entries = map.getMap().entrySet().iterator();
		for (int i = 0; i < 2; i++) {
			Map.Entry entry = (Map.Entry)entries.next();
			if (i == 0) assertTrue("key1".equals(entry.getKey()) && entry.getValue().equals(5));
			if (i == 1) assertTrue("key2".equals(entry.getKey()) && entry.getValue().equals("value2"));
		}

		map.put("key1", 10);
		map.put("key3", "value3");
		entries = map.getMap().entrySet().iterator();
		for (int i = 0; i < 3; i++) {
			Map.Entry entry = (Map.Entry)entries.next();
			if (i == 0) assertTrue("key1".equals(entry.getKey()) && entry.getValue().equals(10));
			if (i == 1) assertTrue("key2".equals(entry.getKey()) && entry.getValue().equals("value2"));
			if (i == 2) assertTrue("key3".equals(entry.getKey()) && entry.getValue().equals("value3"));
		}
	}
	
	public void testNamedParameterWithWrongTypeBefore() {
		ParameterMap map = new ParameterMap();
		map.setValueType("key1", int.class);
		map.setValueType("key2", String.class);
		map.setValueType("key3", Integer.class);
		map.setValueType("key4", Integer.class);
		
		map.put("key1", 5);
		try {
			map.put("key2", 7);
			fail();
		} catch (IllegalArgumentException ex) {
			// good
		}
		map.put("key3", new Integer(7));
		map.put("key4", 8);
		map.put("key1", new Integer(9));
	}

	public void testNamedParameterWithWrongTypeAfter() {
		ParameterMap map = new ParameterMap();
		map.put("key1", 5);
		map.put("key2", 7);
		map.setValueType("key1", int.class);
		
		try {
			map.setValueType("key2", String.class);
			fail();
		} catch (IllegalArgumentException ex) {
			
		}
		
		map.put("key3", new Integer(7));
		map.put("key4", 8);
		map.setValueType("key3", Integer.class);
		map.setValueType("key4", Integer.class);
	}
	
	public void testPositionalParameterWithoutSpecificType() {
		ParameterMap map = new ParameterMap();
		map.put(1, "value1");
		map.put(2, "value2");
		assertFalse(map.isNamed());
		assertEquals(2, map.size());
		Iterator values = map.values().iterator();
		assertEquals("value1", values.next());
		assertEquals("value2", values.next());

		map.put(1, "new value in old key");
		values = map.values().iterator();
		assertEquals("new value in old key", values.next());
		assertEquals("value2", values.next());
	}

	public void testPositionalParameterWithSpecificType() {
		ParameterMap map = new ParameterMap();
		map.setValueType(1, int.class);
		map.setValueType(2, String.class);
		
		map.put(1, 5);
		map.put(2, "value2");
		assertFalse(map.isNamed());
		assertEquals(2, map.size());
		Iterator values = map.values().iterator();
		assertEquals(5, values.next());
		assertEquals("value2", values.next());

		map.put(1, 10);
		values = map.values().iterator();
		assertEquals(10, values.next());
		assertEquals("value2", values.next());
	}
	
	public void testPositionalParameterWithWrongTypeBefore() {
		ParameterMap map = new ParameterMap();
		map.setValueType(1, int.class);
		map.setValueType(2, String.class);
		map.setValueType(3, Integer.class);
		map.setValueType(4, Integer.class);
		
		map.put(1, 5);
		try {
			map.put(2, 7);
			fail();
		} catch (IllegalArgumentException ex) {
			
		}
		map.put(3, new Integer(7));
		map.put(4, 8);
		map.put(1, new Integer(9));
	}

	public void testPositionalParameterWithWrongTypeAfter() {
		ParameterMap map = new ParameterMap();
		map.put(1, 5);
		map.put(2, 7);
		map.setValueType(1, int.class);
		
		try {
			map.setValueType(2, String.class);
			fail();
		} catch (IllegalArgumentException ex) {
			
		}
		
		map.put(3, new Integer(7));
		map.put(4, 8);
		map.setValueType(3, Integer.class);
		map.setValueType(4, Integer.class);
	}
	
	public void testPositionalParameterInsertedInRandomOrder() {
		ParameterMap map = new ParameterMap();
		map.put(3, 30);
		map.put(1, 10);
		map.put(2, 20);
		map.put(5, 50);
		map.put(4, 40);
		Iterator values = map.values().iterator();
		assertEquals(10, values.next());
		assertEquals(20, values.next());
		assertEquals(30, values.next());
		assertEquals(40, values.next());
		assertEquals(50, values.next());
	}

	public void testPositionalParameterInsertedWithGap() {
		ParameterMap map = new ParameterMap();
		map.put(3, 30);
		map.put(2, 20);
		map.put(5, 50);
		Iterator values = map.values().iterator();
		assertEquals(20, values.next());
		assertEquals(30, values.next());
		assertEquals(50, values.next());
	}

}
