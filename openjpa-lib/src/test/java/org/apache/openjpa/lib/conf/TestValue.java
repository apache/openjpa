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
package org.apache.openjpa.lib.conf;

import junit.framework.TestCase;

public class TestValue extends TestCase {
    
    private static class SimpleValue extends Value {

        protected String getInternalString() {
            return null;
        }

        public Class getValueType() {
            return null;
        }

        protected void setInternalObject(Object obj) {
            
        }

        protected void setInternalString(String str) {
        }
        
    }
    
    public void testSetAliasesByValue() {
        String alias = "alias";
        String aName = "Johnny";
        String bName = "Pete";
        String [] aStrings = { alias, aName };
        
        SimpleValue sValue = new SimpleValue();
        sValue.setAliases(aStrings);
        sValue.setAlias(alias, bName);
        assertEquals("Did not set the new alias", bName, 
                sValue.getAliases()[1]);
        assertEquals("Array of aliases not set by value", aName, aStrings[1]);
    }
}
