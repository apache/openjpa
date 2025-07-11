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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.common.apps.CompUser;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.apache.openjpa.persistence.jdbc.common.apps.BuildSchemaPC;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Test that a {@link MappingTool#ACTION_REFRESH} uses the right
 * types for new columns and takes any mapping in DBDictionary into account.
 */
public class TestJDBCSchemaManager extends AbstractPersistenceTestCase {
	
    /**
     * First we create a schema mapping with boolean representation as CHAR(1).
     * Then we create an entry.
     * After that we create a diff from the entity to the current DB.
     * This should result in an empty diff.
     */
    @Test
    public void testSchemaReset() throws Exception {
    	
        EntityManagerFactory emf = createEMF(UUIDEntity.class, DROP_TABLES);
        EntityManager em = emf.createEntityManager();
        
        assertNotNull(em);

        em.getTransaction().begin();
        UUIDEntity ue1 = new UUIDEntity();
        ue1.setValue("Something");
        em.persist(ue1);

        em.getTransaction().commit();
        UUID id = ue1.getId();
        closeEM(em);

        EntityManager em2 = emf.createEntityManager();
        assertNotNull(em2);

        UUIDEntity ue2 = em2.find(UUIDEntity.class, id);
        assertNotNull(ue2);
        assertNotEquals(ue1, ue2);
        
        closeEM(em2);
        
        closeEMF(emf);
    }

    @Test
    public void testBuildSchema() throws IOException, SQLException {
        EntityManagerFactory emf = createEMF(UUIDEntity.class, EntityBoolChar.class, DROP_TABLES);
        emf.createEntityManager();

        emf.getSchemaManager().create(false);
        emf.getSchemaManager().drop(false);

    }
    
    @Test
    public void testTruncate() {
        EntityManagerFactory emf = createEMF(UUIDEntity.class, EntityBoolChar.class, DROP_TABLES);
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        UUIDEntity ue1 = new UUIDEntity();
        ue1.setValue("Something");
        em.persist(ue1);

        em.getTransaction().commit();
        UUIDEntity ue2 = em.find(UUIDEntity.class, ue1.getId());
        assertNotNull(ue2);
        
        closeEM(em);
        
        emf.getSchemaManager().truncate();
        
        em = emf.createEntityManager();
        assertNull(em.find(UUIDEntity.class, ue1.getId()));
        closeEM(em);
    }

}
