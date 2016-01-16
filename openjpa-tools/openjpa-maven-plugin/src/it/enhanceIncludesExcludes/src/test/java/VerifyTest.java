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

import static org.junit.Assert.fail;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.junit.Test;

import enhance.exclude.TestEntityA;
import enhance.include.TestEntityB;
import foo.TestEntityC;

public class VerifyTest {
    
    @Test
    public void testTestEntityBIsEnhanced() {
        if (!PersistenceCapable.class.isAssignableFrom(TestEntityB.class)) {
            fail("TestEntityB has not been enhanced");
        }
    }

    @Test
    public void testTestEntityAIsNotEnhanced() {
        if (PersistenceCapable.class.isAssignableFrom(TestEntityA.class)) {
            fail("TestEntityA has been enhanced");
        }
    }

    @Test
    public void testTestEntityCIsNotEnhanced() {
        if (PersistenceCapable.class.isAssignableFrom(TestEntityC.class)) {
            fail("TestEntityC has been enhanced");
        }
    }
}
