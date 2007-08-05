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

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

/**
 * Test case to ensure that the proper JPA merge semantics are processed.
 *
 * @author Kevin Sutter
 */
public class TestEntityManagerMerge
    extends SingleEMTestCase {

    public void setUp() {
        setUp(AllFieldTypes.class);
    }

    public void testMerge() {
        // Create EntityManager and Start a transaction (1)
        begin();

        // Insert a new object into the PC
        AllFieldTypes testObject = new AllFieldTypes();
        testObject.setStringField("new test object");
        persist(testObject);
        assertTrue("testObject not found in pc", em.contains(testObject));
        
        // Modify this object...
        testObject.setStringField("updated test object");

        // Attempt to merge this updated object into the PC.  Should be ignored.
        AllFieldTypes mergedObject = em.merge(testObject);
        assertTrue("mergedObject and testObject are not equal", 
                mergedObject.equals(testObject));
        assertTrue("mergedObject and testObject are not ==", 
                mergedObject == testObject);
        assertTrue("testObject not found in pc", em.contains(testObject));
        assertTrue("mergedObject not found in pc", em.contains(mergedObject));
        
        // And, once again...
        testObject.setStringField("yet another update");
        AllFieldTypes mergedObject2 = em.merge(testObject);
        assertTrue("mergedObject2 and testObject are not equal", 
                mergedObject2.equals(testObject));
        assertTrue("mergedObject2 and testObject are not ==", 
                mergedObject2 == testObject);
        assertTrue("testObject not found in pc", em.contains(testObject));
        assertTrue("mergedObject2 not found in pc", em.contains(mergedObject2));
        
        // Rollback
        rollback();
  
    }
    
    public static void main(String[] args) {
        TestRunner.run(TestEntityManagerMerge.class);
    }
}

