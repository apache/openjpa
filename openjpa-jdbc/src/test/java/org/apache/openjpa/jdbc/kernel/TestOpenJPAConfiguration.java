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
package org.apache.openjpa.jdbc.kernel;

import java.beans.IntrospectionException;
import java.beans.Introspector;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;

import junit.framework.TestCase;

/**
 * Test the OpenJPAConfiguration is complete.
 * 
 * @author William Dazey
 *
 */
public class TestOpenJPAConfiguration extends TestCase {

   /**
    * Simple test that will fail with an IntrospectionException if 
    * properties are missing from localizer.properties
    */
   public void testOpenJPAConfigurationProperties() {
       try {
           Introspector.getBeanInfo(org.apache.openjpa.conf.OpenJPAConfigurationImpl.class);
       } catch (IntrospectionException e) {
           throw new RuntimeException(e);
       }
   }
}
