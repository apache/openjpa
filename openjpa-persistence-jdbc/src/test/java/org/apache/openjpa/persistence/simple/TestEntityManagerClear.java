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
package org.apache.openjpa.persistence.simple;

import javax.persistence.EntityManager;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

/**
 * Test case to ensure that the proper JPA clear semantics are processed.
 *
 * @author Kevin Sutter
 */
public class TestEntityManagerClear
    extends SingleEMTestCase {

    public void setUp() {
        setUp(AllFieldTypes.class);
    }

    public void testClear() {
        // Create EntityManager and Start a transaction (1)
        begin();

        // Insert a new object and flush
        AllFieldTypes testObject1 = new AllFieldTypes();
        testObject1.setStringField("my test object1");
        persist(testObject1);
        em.flush();

        // Clear the PC for new object 2
        AllFieldTypes testObject2 = new AllFieldTypes();
        testObject1.setStringField("my test object2");
        persist(testObject2);
        em.clear();

        // Commit the transaction (only object 1 should be in database)
        commit();

        // Start a new transaction
        begin();

        // Attempt retrieve of Object1 from previous PC (should exist)
        assertEquals(1, query("select x from AllFieldTypes x "
            + "where x.stringField = 'my test object1'").
                getResultList().size());

        // Attempt retrieve of Object2 from previous PC (should not exist)
        assertEquals(0, query("select x from AllFieldTypes x "
            + "where x.stringField = 'my test object2'").
                getResultList().size());

        // Rollback the transaction and close everything
        rollback();
    }

    public static void main(String[] args) {
        TestRunner.run(TestEntityManagerClear.class);
    }
}

