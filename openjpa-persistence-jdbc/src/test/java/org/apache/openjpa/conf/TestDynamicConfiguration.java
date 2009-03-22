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
package org.apache.openjpa.conf;

import org.apache.openjpa.lib.conf.Value;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests dynamic modification of configuration property.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestDynamicConfiguration extends SingleEMFTestCase {

    public void testConfigurationIsEqualByValueAndHashCode() {
		OpenJPAEntityManagerFactorySPI emf1 = createEMF();
		assertNotNull(emf1);
		OpenJPAConfiguration conf1 = emf1.getConfiguration();
		
		OpenJPAEntityManagerFactorySPI emf2 = createEMF();
		assertNotNull(emf2);
		OpenJPAConfiguration conf2 = emf2.getConfiguration();
		
		assertFalse(emf1==emf2);
		assertFalse(emf1.equals(emf2));
		assertFalse(conf1==conf2);
		assertEquals(conf1, conf2);
		assertEquals(conf1.hashCode(), conf2.hashCode());
		assertEquals(conf1.toProperties(false), conf2.toProperties(false));
	}
	
	public void testConfigurationIsReadOnlyAfterFirstConstruction() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertFalse(conf.isReadOnly());
		emf.createEntityManager();
		assertTrue(conf.isReadOnly());
	}
	
	public void testDynamicValuesCanNotBeChangedDirectly() {
		emf.createEntityManager();
		OpenJPAConfiguration conf = emf.getConfiguration();
		
		Value[] dynamicValues = conf.getDynamicValues();
		assertTrue(dynamicValues.length>0);
		assertTrue(conf.isDynamic("LockTimeout"));

		int oldValue = conf.getLockTimeout();
		int newValue = oldValue + 10;
		try {
			conf.setLockTimeout(newValue);
			fail("Expected exception to modify configuration directly");
		} catch (Exception ex) { // good
			assertEquals(oldValue, conf.getLockTimeout());
		}
	}
	
	public void testDynamicValuesCanBeChanged() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		
		Value[] dynamicValues = conf.getDynamicValues();
		assertTrue(dynamicValues.length>0);
		assertTrue(conf.isDynamic("LockTimeout"));

		int oldValue = conf.getLockTimeout();
		int newValue = oldValue + 10;
		
		conf.modifyDynamic("LockTimeout", newValue);
		assertEquals(newValue, conf.getLockTimeout());
	}

	public void testDynamicValuesAreCorrectlySet() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		
		Value[] dynamicValues = conf.getDynamicValues();
		assertTrue(dynamicValues.length>0);
		assertTrue(conf.isDynamic("LockTimeout"));
	}
	
	public void testDynamicChangeDoesNotChangeHashCode() {
		OpenJPAConfiguration conf1 = emf.getConfiguration();
		
		int oldValue = conf1.getLockTimeout();
		int newValue = oldValue+10;
		int oldHash = conf1.hashCode();
		conf1.modifyDynamic("LockTimeout", newValue);
		int newHash = conf1.hashCode();
		
		assertEquals(oldHash, newHash);
	}
}
