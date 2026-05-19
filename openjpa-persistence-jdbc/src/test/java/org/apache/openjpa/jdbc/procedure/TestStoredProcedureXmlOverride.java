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
package org.apache.openjpa.jdbc.procedure;

import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.procedure.entity.EntityWithStoredProcedure;
import org.apache.openjpa.meta.MultiQueryMetaData;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that XML mapping file (sp-orm.xml) correctly overrides
 * annotation-defined NamedStoredProcedureQuery and provides
 * constructor-result within sql-result-set-mapping.
 */
public class TestStoredProcedureXmlOverride extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp("openjpa.RuntimeUnenhancedClasses", "unsupported",
              "openjpa.DynamicEnhancementAgent", "false",
              "openjpa.MetaDataFactory",
              "Resources=org/apache/openjpa/jdbc/procedure/sp-orm.xml",
              EntityWithStoredProcedure.class, EntityWithStoredProcedure.Mapping2.class);
    }

    /**
     * Test that XML named-stored-procedure-query overrides the
     * annotation-defined one. The annotation maps "EntityWithStoredProcedure.simple"
     * to procedure "TESTSIMPLE", but the XML should override it to "TESTOVERRIDE".
     */
    public void testNamedStoredProcedureQueryXmlOverride() {
        QueryMetaData meta = emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getQueryMetaData(null, "EntityWithStoredProcedure.simple",
                null, true);
        assertNotNull("Query metadata should exist", meta);
        assertTrue("Should be MultiQueryMetaData",
            meta instanceof MultiQueryMetaData);
        MultiQueryMetaData mqm = (MultiQueryMetaData) meta;
        // The XML maps it to TESTOVERRIDE, not TESTSIMPLE
        assertEquals("TESTOVERRIDE", mqm.getProcedureName());
        // The XML defines 2 parameters (IN Integer, OUT String)
        assertEquals(2, mqm.getParameters().size());
    }

    /**
     * Test that XML constructor-result within sql-result-set-mapping is parsed.
     */
    public void testConstructorResultXmlParsing() {
        MappingRepository repos = (MappingRepository) emf.getConfiguration()
            .getMetaDataRepositoryInstance();
        QueryResultMapping mapping = repos.getQueryResultMapping(
            null, "constructorMapping", null, true);
        assertNotNull("Constructor result mapping should exist", mapping);
        QueryResultMapping.ConstructorResultInfo[] constructors =
            mapping.getConstructorResults();
        assertNotNull("Should have constructor results", constructors);
        assertEquals("Should have exactly one constructor result",
            1, constructors.length);
        assertEquals("Target class should be EntityWithStoredProcedure",
            EntityWithStoredProcedure.class,
            constructors[0].getTargetClass());
        assertEquals("Should have 2 columns",
            2, constructors[0].getColumns().size());
        assertEquals("First column should be ID",
            "ID", constructors[0].getColumns().get(0).getName());
        assertEquals("Second column should be NAME",
            "NAME", constructors[0].getColumns().get(1).getName());
    }
}
