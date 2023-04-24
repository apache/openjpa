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
package org.apache.openjpa.persistence.pudefaults;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/*
 * OPENJPA-2494: This test verifies that a schema defined in an orm's
 * persistence-unit-default is used in certain scenarios, and overridden
 * in other scenarios.
 */
public class TestSchemaPUDefault extends SQLListenerTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp(PUDefaultSchemaEntity.class, PUSchemaInSequenceAnnotationEntity.class,
            PUSchemaInTableAnnotationEntity.class, PUSchemaInTableMappingEntity.class,
            PUSchemaInSequenceMappingEntity.class);
        setSupportedDatabases(org.apache.openjpa.jdbc.sql.DB2Dictionary.class);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected String getPersistenceUnitName() {
        return "puDefault";
    }

    public void testSchemaInPUDefault() {
        persist(new PUDefaultSchemaEntity());

        // The Sequence and Table SQL should use the PU default schema
        assertContainsSQL("ALTER SEQUENCE schemaInPUDefaults.SeqName_4DefaultSchema");
        assertContainsSQL("INSERT INTO schemaInPUDefaults.PUDefaultSchemaEntity");
    }

    public void testSchemaInSequenceAnnotation() {
        persist(new PUSchemaInSequenceAnnotationEntity());

        // The Sequence SQL should use the schema defined in the annotation
        assertContainsSQL("ALTER SEQUENCE schemaInSequenceAnnotation.SeqName_4AnnoSequenceSchema");
        // The Table SQL should use the schema defined in the PU default schema
        assertContainsSQL("INSERT INTO schemaInPUDefaults.PUSchemaInSequenceAnnotationEntity");
    }

    public void testSchemaInTableAnnotation() {
        persist(new PUSchemaInTableAnnotationEntity());

        // The Sequence SQL should use the schema defined in the PU default schema
        assertContainsSQL("ALTER SEQUENCE schemaInPUDefaults.SeqName_4AnnoTableSchema");
        // The Table SQL should use the schema defined in the annotation
        assertContainsSQL("INSERT INTO schemaInTableAnnotation.PUSchemaInTable");
    }

    public void testSchemaInTableMapping() {
        persist(new PUSchemaInTableMappingEntity());

        // The Sequence SQL should use the schema defined in the PU default schema
        assertContainsSQL("ALTER SEQUENCE schemaInPUDefaults.SeqName_4TableMappingSchema");
        // The Table SQL should use the schema defined in the mapping file
        assertContainsSQL("INSERT INTO schemaInTableMapping.PUSchemaInTableMapping");
    }

    public void testSchemaInSequenceMapping() {
        persist(new PUSchemaInSequenceMappingEntity());

        // The Sequence SQL should use the schema defined in the mapping file
        assertContainsSQL("ALTER SEQUENCE schemaInSequenceMapping.SeqName_4SequenceMappingSchema");
        // The Table SQL should use the schema defined in the PU default schema
        assertContainsSQL("INSERT INTO schemaInPUDefaults.PUSchemaInSequenceMappingEntity");
    }

    public void persist(Object ent){
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(ent);
        tx.commit();
        em.close();
    }
}
