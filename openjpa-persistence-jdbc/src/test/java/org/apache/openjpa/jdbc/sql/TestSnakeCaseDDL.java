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
package org.apache.openjpa.jdbc.sql;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSnakeCaseDDL {

    @Test
    public void ddlInSnakeCase() throws SQLException {

        Driver derbyDriver;
        try {
            Class derbyClazz = Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            derbyDriver = (Driver) derbyClazz.newInstance();
        }
        catch (Exception e) {
            // all fine
            System.out.println("Skipping Derby specific test because Derby cannot be found in ClassPath");
            return;
        }

        final PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl();
        persistenceUnitInfo.setExcludeUnlistedClasses(true);
        persistenceUnitInfo.addManagedClassName(SnakeCaseDDLMy1Entity.class.getName());
        persistenceUnitInfo.addManagedClassName(SnakeCaseDDLMy2Entity.class.getName());
        final BasicDataSource ds = new BasicDataSource();
        ds.setDriver(derbyDriver);
        ds.setUrl("jdbc:derby:memory:ddlInSnakeCase;create=true");
        persistenceUnitInfo.setJtaDataSource(ds);
        persistenceUnitInfo.setProperty("openjpa.jdbc.DBDictionary", "derby(javaToDbColumnNameProcessing=snake_case)");
        new PersistenceProviderImpl().generateSchema(persistenceUnitInfo, new HashMap<>());
        final Map<String, String> createdTables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, Collection<String>> columns = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (final Connection connection = ds.getConnection()) {
            try (final ResultSet tables = connection.getMetaData()
                    .getTables(null, null, "%", null)) {
                while (tables.next()) {
                    final String table = tables.getString(3);
                    if (table.toUpperCase(Locale.ROOT).startsWith("SNAKE")) {
                        createdTables.put(table.toUpperCase(Locale.ROOT), table);
                    }
                }
            }
            for (final Map.Entry<String, String> table : createdTables.entrySet()) {
                try (final Statement statement = connection.createStatement()) {
                    try (final ResultSet rs = statement.executeQuery("select * from \"" + table.getValue() + "\"")) {
                        final ResultSetMetaData metaData = rs.getMetaData();
                        final Set<String> columnNames = new HashSet<>();
                        columns.put(table.getValue(), columnNames);
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            columnNames.add(metaData.getColumnName(i));
                        }
                    }
                }
            }
        }
        final EntityManagerFactory entityManagerFactory = new PersistenceProviderImpl()
                .createContainerEntityManagerFactory(persistenceUnitInfo, new HashMap());
        try {
            {
                final EntityManager em = entityManagerFactory.createEntityManager();
                em.getTransaction().begin();
                try {
                    final SnakeCaseDDLMy1Entity entity = new SnakeCaseDDLMy1Entity();
                    entity.setFooBar("1");
                    entity.setThisField(123);
                    em.persist(entity);
                    em.getTransaction().commit();
                } catch (final RuntimeException re) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    throw re;
                } finally {
                    em.close();
                }
            }
            {
                final EntityManager em = entityManagerFactory.createEntityManager();
                try {
                    final SnakeCaseDDLMy1Entity myEntity1 = em.find(SnakeCaseDDLMy1Entity.class, "1");
                    assertNotNull(myEntity1);
                    assertEquals("1", myEntity1.getFooBar());
                    assertEquals(123, myEntity1.getThisField());
                } finally {
                    em.close();
                }
            }
            final String tableName = createdTables.get("SnakeCaseDDLMy1Entity".toUpperCase(Locale.ROOT));
            try (final Connection connection = ds.getConnection();
                 final Statement statement = connection.createStatement();
                 final ResultSet rs = statement.executeQuery("select foo_bar, this_field from \"" + tableName + "\"")) {
                assertTrue (rs.next());
                assertEquals("1", rs.getString(1));
                assertEquals(123, rs.getInt(2));
                assertFalse(rs.next());
            }
        } finally {
            entityManagerFactory.close();
        }
        ds.close();
        assertEquals(2, columns.get("SnakeCaseDDLMy1Entity").size());
        assertTrue(columns.get("SnakeCaseDDLMy1Entity").contains("FOO_BAR"));
        assertTrue(columns.get("SnakeCaseDDLMy1Entity").contains("THIS_FIELD"));
        assertEquals(singleton("ANOTHER_FIELD"), columns.get("SnakeCaseDDLMy2Entity"));
    }

}
