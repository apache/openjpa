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
import org.apache.derby.jdbc.EmbeddedDriver;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSnakeCaseDDL {
    @Test
    public void ddlInSnakeCase() throws SQLException {
        final PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl();
        persistenceUnitInfo.setExcludeUnlistedClasses(true);
        persistenceUnitInfo.addManagedClassName(MyEntity1.class.getName());
        persistenceUnitInfo.addManagedClassName(MyEntity2.class.getName());
        final BasicDataSource ds = new BasicDataSource();
        ds.setDriver(new EmbeddedDriver());
        ds.setUrl("jdbc:derby:memory:ddlInSnakeCase;create=true");
        persistenceUnitInfo.setJtaDataSource(ds);
        persistenceUnitInfo.setProperty("openjpa.jdbc.DBDictionary", "derby(javaToDbColumnNameProcessing=snake_case)");
        new PersistenceProviderImpl().generateSchema(persistenceUnitInfo, new HashMap<>());
        final Collection<String> createdTables = new HashSet<>();
        final Map<String, Collection<String>> columns = new HashMap<>();
        try (final Connection connection = ds.getConnection()) {
            try (final ResultSet tables = connection.getMetaData()
                    .getTables(null, null, "TestSnakeCaseDDL$MyEntity%", null)) {
                while (tables.next()) {
                    final String table = tables.getString(3);
                    createdTables.add(table);
                }
            }
            for (final String table : createdTables) {
                try (final Statement statement = connection.createStatement()) {
                    try (final ResultSet rs = statement.executeQuery("select * from \"" + table + "\"")) {
                        final ResultSetMetaData metaData = rs.getMetaData();
                        final Set<String> columnNames = new HashSet<>();
                        columns.put(table, columnNames);
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
                    final MyEntity1 entity = new MyEntity1();
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
                    final MyEntity1 myEntity1 = em.find(MyEntity1.class, "1");
                    assertNotNull(myEntity1);
                    assertEquals("1", myEntity1.getFooBar());
                    assertEquals(123, myEntity1.getThisField());
                } finally {
                    em.close();
                }
            }
            try (final Connection connection = ds.getConnection();
                 final Statement statement = connection.createStatement();
                 final ResultSet rs = statement.executeQuery("select foo_bar, this_field from \"TestSnakeCaseDDL$MyEntity1\"")) {
                assertTrue (rs.next());
                assertEquals("1", rs.getString(1));
                assertEquals(123, rs.getInt(2));
                assertFalse(rs.next());
            }
        } finally {
            entityManagerFactory.close();
        }
        ds.close();
        assertEquals(2, columns.get("TestSnakeCaseDDL$MyEntity1").size());
        assertTrue(columns.get("TestSnakeCaseDDL$MyEntity1").contains("FOO_BAR"));
        assertTrue(columns.get("TestSnakeCaseDDL$MyEntity1").contains("THIS_FIELD"));
        assertEquals(singleton("ANOTHER_FIELD"), columns.get("TestSnakeCaseDDL$MyEntity2"));
    }

    @Entity
    public static class MyEntity1 {
        @Id
        private String fooBar;

        private int thisField;

        public int getThisField() {
            return thisField;
        }

        public void setThisField(int thisField) {
            this.thisField = thisField;
        }

        public String getFooBar() {
            return fooBar;
        }

        public void setFooBar(String fooBar) {
            this.fooBar = fooBar;
        }
    }

    @Entity
    public static class MyEntity2 {
        @Id
        private String anotherField;
    }
}
