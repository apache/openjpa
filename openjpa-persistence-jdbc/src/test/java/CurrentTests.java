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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.openjpa.kernel.TestEnhancedInstanceBrokerSerialization;
import org.apache.openjpa.kernel.TestEntityManagerFactoryPool;
import org.apache.openjpa.kernel.TestInstanceGraphBrokerSerialization;
import org.apache.openjpa.kernel.TestUnenhancedFieldAccessInstanceBrokerSerialization;
import org.apache.openjpa.kernel.TestUnenhancedFieldAccessWithRelationInstanceBrokerSerialization;
import org.apache.openjpa.kernel.TestUnenhancedPropertyAccessInstanceBrokerSerialization;
import org.apache.openjpa.kernel.TestUnenhancedPropertyAccessWithRelationInstanceBrokerSerialization;

public class CurrentTests extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite("Current Tests");
        suite.addTestSuite(TestEnhancedInstanceBrokerSerialization.class);
        suite.addTestSuite(TestInstanceGraphBrokerSerialization.class);
        suite.addTestSuite(
            TestUnenhancedFieldAccessWithRelationInstanceBrokerSerialization.class);
        suite.addTestSuite(
            TestUnenhancedPropertyAccessWithRelationInstanceBrokerSerialization.class);
        suite.addTestSuite(
            TestUnenhancedFieldAccessInstanceBrokerSerialization.class);
        suite.addTestSuite(
            TestUnenhancedPropertyAccessInstanceBrokerSerialization.class);

//        suite.addTestSuite(TestPCSubclassNameConversion.class);
//        suite.addTestSuite(ManagedCacheTest.class);
        suite.addTestSuite(TestEntityManagerFactoryPool.class);
        return suite;
    }
}
