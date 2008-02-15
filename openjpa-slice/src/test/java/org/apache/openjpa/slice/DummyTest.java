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
package org.apache.openjpa.slice;

import junit.framework.TestCase;

public class DummyTest extends TestCase {
    public void testDeactive() {
        System.err.println("\t\t============================= WARNING ====================================");
        System.err.println("\t\tTest for distributed database is deactivated");
        System.err.println("\t\tTo activate: ");
        System.err.println("\t\t   1. create databases as per META-INF/persistence.xml in openjpa-slice module");
        System.err.println("\t\t   2. uncomment the SureFire plugin in pom.xml");
        System.err.println("\t\t==========================================================================");
    }
}
