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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/*
 * OPENJPA-2704: These tests expand on TestSchemaPUDefault to verify
 * that a schema defined in an orm's persistence-unit-default is
 * overriden by the "openjpa.jdbc.Schema" property .
 */
public class TestOpenJPASchemaPUDefault extends SQLListenerTestCase {

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
        return "overrideMappingSchema";
    }

    public void testOpenJPASchemaOverridesORM() {
        persist(new PUDefaultSchemaEntity());

        // The Sequence and Table SQL should use the PU default schema
        assertContainsSQL("ALTER SEQUENCE PUSCHEMA.SeqName_4DefaultSchema");
        assertContainsSQL("INSERT INTO PUSCHEMA.PUDefaultSchemaEntity");
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
