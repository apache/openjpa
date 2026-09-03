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

import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

// This test case extends TestCase directly instead of SingleEMTestCase with the
// corresponding setup() method because that scheme goes down a different code
// path and doesn't test the intended code change.
public class TestMissingMetaData extends TestCase {
    private OpenJPAEntityManagerFactory emf;

    @Override
    public void setUp() {
        Map<String, String> props = new HashMap<>();
        props.put("openjpa.RuntimeUnenhancedClasses", "supported");
        // This test case uses a different persistence xml file because
        // modifying the current persistence.xml file with a bad class would
        // cause the TestEnhancementWithMultiplePUs test case to fail.
        emf = OpenJPAPersistence.createEntityManagerFactory(
            "test-missing-metadata", "persistence2.xml", props);
    }

    /**
     * Verify that a class listed in persistence.xml without persistence
     * metadata is skipped rather than failing entity manager creation.
     * <p>
     * Jakarta Persistence 3.2 chapter 8 does not say what a provider must do
     * with such a class, so the behaviour is provider defined; Hibernate and
     * EclipseLink both skip, and the Jakarta Persistence TCK requires it,
     * since its own persistence units list plain classes alongside entities.
     * The skip is logged, so a forgotten annotation stays diagnosable; see
     * MetamodelImpl and JDBCBrokerFactory.
     */
    public void testMissingMetaData() {
        emf.createEntityManager().close();
    }

    /**
     * The metamodel is built over the same class list, and must skip the
     * unmanaged class rather than fail.
     */
    public void testMissingMetaDataInMetamodel() {
        assertNotNull(emf.getMetamodel());
    }

    @Override
    public void tearDown() {
        emf.close();
        emf = null;
    }

    public static void main(String[] args) {
        TestRunner.run(TestMissingMetaData.class);

    }

}
