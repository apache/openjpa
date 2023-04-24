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

import static junit.framework.TestCase.assertEquals;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;

public class TestDelimitIdentifiers {

    public static class LowercaseSchemaDerbyDBDictionary extends DerbyDictionary {

        public LowercaseSchemaDerbyDBDictionary() {
            super();
            schemaCase = SCHEMA_CASE_LOWER;
            delimitedCase = SCHEMA_CASE_PRESERVE;
            setDelimitIdentifiers(true);
            setSupportsDelimitedIdentifiers(true);
        }
    }

    @Test
    public void testDelimitIdentifiers() throws SQLException {

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
        persistenceUnitInfo.addManagedClassName(DelimitedIdentifiersAllFieldTypesEntity.class.getName());
        final BasicDataSource ds = new BasicDataSource();
        ds.setDriver(derbyDriver);
        ds.setUrl("jdbc:derby:memory:TestDelimitIdentifiers;create=true");
        persistenceUnitInfo.setNonJtaDataSource(ds);
        // reproducer for OPENJPA-2818 delimitIdentifiers=true,delimitedCase=lower,schemaCase=lower
        persistenceUnitInfo.setProperty("openjpa.jdbc.DBDictionary", LowercaseSchemaDerbyDBDictionary.class.getName());        
        new PersistenceProviderImpl().generateSchema(persistenceUnitInfo, new HashMap<>());
        // try rebuild the schema
        new PersistenceProviderImpl().generateSchema(persistenceUnitInfo, new HashMap<>());
        
        final Map<String, Collection<String>> columns = new HashMap<>();
        final Collection<String> createdTables = new HashSet<>();
        try (final Connection connection = ds.getConnection()) {
            try (final ResultSet tables = connection.getMetaData()
                    .getTables(null, null, "TestDelimitIdentifiers$AllFieldTypes%", null)) {
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
            final DelimitedIdentifiersAllFieldTypesEntity entity = new DelimitedIdentifiersAllFieldTypesEntity();
            final DelimitedIdentifiersAllFieldTypesEntity entity2 = new DelimitedIdentifiersAllFieldTypesEntity();
            {
                final EntityManager em = entityManagerFactory.createEntityManager();
                em.getTransaction().begin();
                try {
                    em.persist(entity2);
                    entity.setArrayOfStrings(new String[]{"a", "b"});
                    entity.setStringField("foo");
                    entity.setIntField(10);
                    entity.setSelfOneOne(entity2);
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
                    assertEquals(2, em.createQuery("select x from DelimitedIdentifiersAllFieldTypesEntity x").
                            getResultList().size());
                    assertEquals(1, em.createQuery("select x from DelimitedIdentifiersAllFieldTypesEntity x where x.stringField = 'foo'").
                            getResultList().size());
                    assertEquals(0, em.createQuery("select x from DelimitedIdentifiersAllFieldTypesEntity x where x.stringField = 'bar'").
                            getResultList().size());
                    assertEquals(1, em.createQuery("select x from DelimitedIdentifiersAllFieldTypesEntity x where x.intField >= 10").
                            getResultList().size());
                } finally {
                    em.close();
                }
            }
        } finally {
            entityManagerFactory.close();
        }
        ds.close();
    }

}
