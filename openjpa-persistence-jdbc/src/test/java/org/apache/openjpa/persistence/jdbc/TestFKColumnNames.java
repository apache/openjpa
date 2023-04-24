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
package org.apache.openjpa.persistence.jdbc;

import jakarta.persistence.Persistence;

import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

/**
 * Testcase that verifies the names for Foreign Key columns is as expected.
 */
public class TestFKColumnNames extends AbstractPersistenceTestCase {


    /**
     * <P>
     * If a Foreign Key field contains a SQL reserved word, the resulting column
     * should be named ${reservedWord}_ID, not ${reservedWord}<B>0</B>_ID.
     * </P>
     * <P>
     * This test does not take into account and DB specific reserved words and
     * can be run with any DBDictionary.
     * </P>
     */
    public void testSQLKeywords() {
        OpenJPAEntityManagerFactorySPI emf =
            (OpenJPAEntityManagerFactorySPI) Persistence
                    .createEntityManagerFactory("test");
        MappingRepository repos =
                (MappingRepository) emf.getConfiguration()
                        .getMetaDataRepositoryInstance();

        assertEquals("SELECT_ID", repos.getMapping(FKColumnNamesInner1Entity.class, null, true)
                .getFieldMapping("select").getColumns()[0].getName());

        assertEquals("FROM_ID", repos.getMapping(FKColumnNamesInner2Entity.class, null, true)
                .getFieldMapping("from").getColumns()[0].getName());
        closeEMF(emf);
    }

}
