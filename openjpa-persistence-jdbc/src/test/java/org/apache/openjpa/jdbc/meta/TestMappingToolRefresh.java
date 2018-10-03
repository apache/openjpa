/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.meta;

import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;
import org.junit.Test;

/**
 * Test that a {@link MappingTool#ACTION_REFRESH} uses the right
 * types for new columns and takes any mapping in DBDictionary into account.
 */
public class TestMappingToolRefresh extends AbstractPersistenceTestCase {

    /**
     * First we create a schema mapping with boolean representation as CHAR(1).
     * Then we create an entry.
     * After that we create a diff from the entity to the current DB.
     * This should result in an empty diff.
     */
    @Test
    public void testSchemaCreation() throws Exception {
        Map<String, Object> emfProps = getPropertiesMap(EntityBoolChar.class,
                "openjpa.jdbc.SynchronizeMappings",
                "buildSchema(ForeignKeys=true, SchemaAction='add,deleteTableContents')",
                "openjpa.jdbc.DBDictionary",
                "(BitTypeName=CHAR(1),BooleanTypeName=CHAR(1),BooleanRepresentation=STRING_10)");


        {
            // stage 1. Create the DB and insert a line into it
            OpenJPAEntityManagerFactorySPI openjpaEmf = createNamedOpenJPAEMF("test", null, emfProps);

            OpenJPAEntityManagerSPI em = openjpaEmf.createEntityManager();
            assertNotNull(em);
            em.getTransaction().begin();
            EntityBoolChar val = new EntityBoolChar();
            val.setDummy(true);
            em.persist(val);

            em.getTransaction().commit();
            int id = val.getId();
            em.close();

            OpenJPAEntityManagerSPI em2 = openjpaEmf.createEntityManager();
            assertNotNull(em2);

            EntityBoolChar val2 = em2.find(EntityBoolChar.class, id);
            assertNotNull(val2);
            assertNotEquals(val, val2);

            openjpaEmf.close();
        }

        {
            // now we create a 2nd EntityManagerFactory but with a different configuration
            // we switch the boolean representation to CHAR(1)
            OpenJPAEntityManagerFactorySPI openjpaEmf = createNamedOpenJPAEMF("test", null, emfProps);
            String metaDataFactory = openjpaEmf.getConfiguration().getMetaDataFactory();

            JDBCConfiguration jdbcConf = new JDBCConfigurationImpl();
            jdbcConf.setMetaDataFactory(metaDataFactory);

            String[] entityClassFiles = new String[1];
            URL entityClassUrl = this.getClass().getClassLoader().
                    getResource(EntityBoolChar.class.getName().replace(".", "/") + ".class");
            entityClassFiles[0] = entityClassUrl.getFile();

            MappingTool.Flags flags = new MappingTool.Flags();
            flags.mappingWriter = new StringWriter();
            flags.action = MappingTool.ACTION_REFRESH;
            flags.schemaAction = SchemaTool.ACTION_REFRESH;
            flags.sqlWriter = new StringWriter();
            flags.schemaWriter = new StringWriter();

            boolean ok = MappingTool.run(jdbcConf, entityClassFiles, flags, this.getClass().getClassLoader());
            assertTrue(ok);
            assertTrue(flags.sqlWriter.toString().isEmpty());


        }

    }


}
