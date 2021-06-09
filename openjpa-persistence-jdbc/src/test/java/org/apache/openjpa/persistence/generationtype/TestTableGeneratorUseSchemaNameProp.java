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
package org.apache.openjpa.persistence.generationtype;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/*
 * Test for JIRA OPENJPA-2650.
 */
public class TestTableGeneratorUseSchemaNameProp extends SQLListenerTestCase {

    public void setUp() {
        setUp(Dog.class, "openjpa.jdbc.SchemaFactory",
            "native(ForeignKeys=true)", "openjpa.jdbc.DBDictionary",
            "useSchemaName=false", DROP_TABLES);
    }

    /*
     * This test verifies that when useSchemaName=false, and SchemaFactory is
     * set, the schema name is not added to the table sequence queries.
     */
    public void testNoSchemaAndSchemaFactory() {

        EntityManager em = emf.createEntityManager();

        Dog dog = new Dog();
        dog.setName("Fido");

        em.getTransaction().begin();

        em.persist(dog);

        em.getTransaction().commit();

        // Make sure a schema name isn't used when updating the ID_Gen table
        // or getting the value from ID_Gen table.
        assertContainsSQL("UPDATE ID_Gen");
        assertContainsSQL("SELECT GEN_VAL FROM ID_Gen");

        closeEM(em);
    }
}
