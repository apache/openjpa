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

package org.apache.openjpa.persistence.jest;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests parsing of JEST requests.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestRequestParsing extends TestCase {
    
    public void testRequestFindImplicitParam() throws IOException {
        GETRequest req = new GETRequest();
        req.read(Arrays.asList("/find?Person;1234"));
        
        assertEquals("find", req.getAction());
        assertTrue(req.hasParameter("Person"));
        assertEquals(null, req.getParameter("Person"));
        assertEquals("Person", req.getParameter(0).getKey());
        assertEquals(null, req.getParameter(0).getValue());
        assertTrue(req.hasParameter("1234"));
        assertEquals(null, req.getParameter("1234"));
        assertEquals("1234", req.getParameter(1).getKey());
        assertEquals(null, req.getParameter(1).getValue());
    }
    
    public void testRequestFindExplicitParam() throws IOException {
        GETRequest req = new GETRequest();
        req.read(Arrays.asList("/find?Person;ssn=1234"));
        
        assertEquals("find", req.getAction());
        assertTrue(req.hasParameter("Person"));
        assertEquals(null, req.getParameter("Person"));
        assertEquals("Person", req.getParameter(0).getKey());
        assertEquals(null, req.getParameter(0).getValue());
        assertTrue(req.hasParameter("ssn"));
        assertEquals("1234", req.getParameter("ssn"));
        assertEquals("ssn", req.getParameter(1).getKey());
        assertEquals("1234", req.getParameter(1).getValue());
    }
    
    public void testRequestQueryWithParameters() throws IOException {
        GETRequest req = new GETRequest();
        req.read(Arrays.asList("/query?select p from Person p where p.name=:name;name=xyz;age=20"));
        
        assertEquals("query", req.getAction());
        assertEquals("select p from Person p where p.name=:name", req.getParameter(0).getKey());
        assertEquals(null, req.getParameter(0).getValue());
        assertTrue(req.hasParameter("name"));
        assertEquals("xyz", req.getParameter("name"));
        assertEquals("name", req.getParameter(1).getKey());
        assertEquals("xyz", req.getParameter(1).getValue());
        assertTrue(req.hasParameter("age"));
        assertEquals("20", req.getParameter("age"));
        assertEquals("age", req.getParameter(2).getKey());
        assertEquals("20", req.getParameter(2).getValue());
    }
    
    public void testRequestQueryWithoutParameters() throws IOException {
        GETRequest req = new GETRequest();
        req.read(Arrays.asList("/query?select p from Person p where p.name='xyz'"));
        
        assertEquals("query", req.getAction());
        assertEquals("select p from Person p where p.name='xyz'", req.getParameter(0).getKey());
        assertEquals(null, req.getParameter(0).getValue());
    }
}
