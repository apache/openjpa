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
package org.apache.openjpa.lib.util;

import org.apache.openjpa.lib.conf.ImmutableBooleanValue;

import junit.framework.TestCase;

/**
 * Tests behavior of immutable boolean value.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestImmutableValue extends TestCase {
	
	public void testDefaultValueIsFalse() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a");
		assertEquals(Boolean.FALSE, value.get());
	}
	
	public void testValueCanBeSetOnce() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a");
		value.set(true);
		assertEquals(Boolean.TRUE, value.get());
	}
	
	public void testSameValueCanBeSetMoreThanOnce() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a");
		value.set(true);
		assertEquals(Boolean.TRUE, value.get());
		value.set(true);
		assertEquals(Boolean.TRUE, value.get());
	}
	
	public void testDifferentValueCanNotBeSetMoreThanOnce() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a");
		value.set(true);
		assertEquals(Boolean.TRUE, value.get());
		try {
			value.set(false);
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected exception
		}
		assertEquals(Boolean.TRUE, value.get());
	}
	
	public void testInitializedAsTrue() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a", true);
		assertEquals(Boolean.TRUE, value.get());
	}
	
	public void testInitializedAsFalse() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a", false);
		assertEquals(Boolean.FALSE, value.get());
	}
	
	public void testInitializedValueCanNotBeMutated() {
		ImmutableBooleanValue value = new ImmutableBooleanValue("a", true);
		assertEquals(Boolean.TRUE, value.get());
		try {
			value.set(false);
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected exception
		}
		assertEquals(Boolean.TRUE, value.get());
	}
}
