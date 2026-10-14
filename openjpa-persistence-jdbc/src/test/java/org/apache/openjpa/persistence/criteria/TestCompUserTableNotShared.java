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
package org.apache.openjpa.persistence.criteria;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

/**
 * Affirms that the two unrelated {@code CompUser} hierarchies in the test suite map to
 * different tables.
 * <p>
 * {@code org.apache.openjpa.persistence.criteria.CompUser} and
 * {@code org.apache.openjpa.persistence.common.apps.CompUser} both used to default to a
 * table called {@code CompUser}, with incompatible column types. Whichever package ran
 * second therefore made {@code SynchronizeMappings=buildSchema(SchemaAction='drop,add')}
 * drop and re-add every column of that table on every EntityManagerFactory it created,
 * which on MariaDB fails with "Row size too large" once the server has recorded about a
 * dozen such rotations - the criteria tests create one factory per test method, so a single
 * test class is enough (OPENJPA-3016).
 */
public class TestCompUserTableNotShared extends CriteriaTest {

    public void testTheTwoCompUserHierarchiesDoNotShareATable() {
        String criteriaTable = tableOf(getEntityManagerFactory(), CompUser.class);

        OpenJPAEntityManagerFactorySPI other = createNamedEMF(
            org.apache.openjpa.persistence.common.apps.CompUser.class,
            org.apache.openjpa.persistence.common.apps.MaleUser.class,
            org.apache.openjpa.persistence.common.apps.FemaleUser.class,
            org.apache.openjpa.persistence.common.apps.Address.class);
        String commonAppsTable;
        try {
            commonAppsTable = tableOf(other, org.apache.openjpa.persistence.common.apps.CompUser.class);
        } finally {
            other.close();
        }

        assertFalse("org.apache.openjpa.persistence.criteria.CompUser and "
            + "org.apache.openjpa.persistence.common.apps.CompUser must not share the table \""
            + criteriaTable + "\": whichever test package runs second rewrites every column of "
            + "it on every EntityManagerFactory, which MariaDB rejects after about a dozen "
            + "rotations with \"Row size too large\"",
            criteriaTable.equalsIgnoreCase(commonAppsTable));
    }

    private String tableOf(OpenJPAEntityManagerFactorySPI factory, Class<?> entity) {
        ClassMapping mapping = (ClassMapping) ((JDBCConfiguration) factory.getConfiguration())
            .getMappingRepositoryInstance().getMetaData(entity, entity.getClassLoader(), true);
        assertNotNull("no mapping for " + entity.getName(), mapping);
        return mapping.getTable().getIdentifier().getName();
    }
}
