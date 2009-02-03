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


import junit.framework.TestCase;
/**
 * Test basics of Specification object.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestSpecification extends TestCase {
    public void testStaticConstruction() {
        Specification spec1 = Specification.create("JPA 2.3");
        assertEquals("JPA", spec1.getName());
        assertEquals(2, spec1.getMajorVersion());
        assertEquals("3", spec1.getMinorVersion());

        Specification spec2 = Specification.create("JPA", "1.1");
        assertEquals("JPA", spec2.getName());
        assertEquals(1, spec2.getMajorVersion());
        assertEquals("1", spec2.getMinorVersion());
        
        Specification spec3 = Specification.create("JDO", 3, "ED");
        assertEquals("JDO", spec3.getName());
        assertEquals(3, spec3.getMajorVersion());
        assertEquals("ED", spec3.getMinorVersion());
        
        Specification spec4 = Specification.create("JDO", 3, 5);
        assertEquals("JDO", spec4.getName());
        assertEquals(3, spec4.getMajorVersion());
        assertEquals("5", spec4.getMinorVersion());
    }
    
    public void testEquality() {
        Specification spec1 = Specification.create("JPA 2.3");
        Specification spec2 = Specification.create("JPA 1.0");
        Specification spec3 = Specification.create("JDO 3.1");
        
        assertEquals(spec1, spec2);
        assertTrue(spec1.equals("jpa"));
        assertTrue(spec1.equals("JPA"));
        assertTrue(spec1.equals("JPA "));
        
        assertTrue(spec2.equals("jpa"));
        assertTrue(spec2.equals("JPA"));
        assertTrue(spec2.equals("JPA "));
        
        assertFalse(spec1.equals(spec3));
    }
    
    public void testVersionCompare() {
        Specification spec1 = Specification.create("JPA 1.1");
        Specification spec2 = Specification.create("JPA 2.2");
        assertTrue(spec1.compareVersion(spec2) < 0);
        assertTrue(spec2.compareVersion(spec1) > 0);
        assertTrue(spec1.compareVersion(spec1) == 0);
    }
    
    public void testFormat() {
        assertEquals("<name> [<major>[.<minor>]]", Specification.getFormat());
        assertEquals("<major>[.<minor>]", Specification.getVersionFormat());
    }
}
