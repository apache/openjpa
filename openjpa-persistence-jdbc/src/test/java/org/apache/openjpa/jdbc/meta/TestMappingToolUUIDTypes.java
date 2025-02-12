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
package org.apache.openjpa.jdbc.meta;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.simple.UUIDTypes;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestMappingToolUUIDTypes extends SingleEMTestCase {

        @Override
        public void setUp() {
                setUp(UUIDTypes.class);
        }

        public void testMappingToolTemporal() throws IOException, SQLException {
                ClassMapping mapping = (ClassMapping) ((OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.cast(emf))
                                .getConfiguration().getMetaDataRepositoryInstance()
                                .getMetaData("UUIDTypes", getClass().getClassLoader(),
                                                true);
                DBDictionary dict = getDBDictionary();

                if (dict.supportsUuidType) {
                        assertEquals(JavaTypes.UUID_OBJ,
                                        mapping.getFieldMapping("basicUuid").getValueMapping().getColumns()[0]
                                                        .getJavaType());

                        assertEquals(java.sql.Types.BLOB,
                                        mapping.getFieldMapping("basicUuid").getValueMapping().getColumns()[0]
                                                        .getType());

                } else {
                        assertEquals(JavaTypes.STRING,
                                        mapping.getFieldMapping("basicUuid").getValueMapping().getColumns()[0]
                                                        .getJavaType());

                        assertEquals(java.sql.Types.VARCHAR,
                                        mapping.getFieldMapping("basicUuid").getValueMapping().getColumns()[0]
                                                        .getType());

                }

        }
}
