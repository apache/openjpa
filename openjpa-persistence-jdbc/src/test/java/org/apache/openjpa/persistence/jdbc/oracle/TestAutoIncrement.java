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
package org.apache.openjpa.persistence.jdbc.oracle;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests identity value assignment with IDENTITY strategy specifically for
 * Oracle database. IDENTITY strategy for most database platform is supported
 * with auto-increment capabilities. As Oracle does not natively support
 * auto-increment, the same effect is achieved by a combination of a database
 * sequence and a pre-insert database trigger [1].
 * 
 * This test verifies that a persistence entity using IDENTITY generation type
 * is allocated identities in monotonic sequence on Oracle platform.
 * 
 * [1] http://jen.fluxcapacitor.net/geek/autoincr.html
 * 
 * @author Pinaki Poddar
 * 
 */
public class TestAutoIncrement extends SingleEMFTestCase {
	private static String PLATFORM = "oracle";

	public void setUp() throws Exception {
	    // run with -Dplatform= ${PLATFORM} to activate
		if (!isTargetPlatform(PLATFORM)) {
			return;
		}
		if ("testAutoIncrementIdentityWithNamedSequence".equals(getName())) {
			super.setUp(CLEAR_TABLES, PObject.class,
			    "openjpa.jdbc.DBDictionary",
			    "oracle(UseTriggersForAutoAssign=true," + 
			    "autoAssignSequenceName=autoIncrementSequence)");
		} else {
			super.setUp(CLEAR_TABLES, PObjectNative.class,
					"openjpa.jdbc.DBDictionary",
					"oracle(UseTriggersForAutoAssign=true)",
					"openjpa.Log", "SQL=TRACE");
		}
	}

	public void testAutoIncrementIdentityWithNamedSequence() {
		if (!isTargetPlatform(PLATFORM))
			return;

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		PObject pc1 = new PObject();
		PObject pc2 = new PObject();
		em.persist(pc1);
		em.persist(pc2);
		em.getTransaction().commit();

		assertEquals(1, Math.abs(pc1.getId() - pc2.getId()));
	}

	public void testAutoIncrementIdentityWithNativeSequence() {
		if (!isTargetPlatform(PLATFORM))
			return;

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		PObjectNative pc1 = new PObjectNative();
		PObjectNative pc2 = new PObjectNative();
		em.persist(pc1);
		em.persist(pc2);
		em.getTransaction().commit();

		assertEquals(1, Math.abs(pc1.getId() - pc2.getId()));
	}
}
