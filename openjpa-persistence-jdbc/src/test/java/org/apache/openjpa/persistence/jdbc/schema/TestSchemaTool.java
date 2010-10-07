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
package org.apache.openjpa.persistence.jdbc.schema;

import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.query.Employee;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestSchemaTool extends SingleEMFTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, Employee.class);
    }

    private void createSecondPU() {
        createNamedEMF("mdr-pu", "openjpa.Log","SQL=trace","openjpa.jdbc.SynchronizeMappings",
            "buildSchema(SchemaAction='add,refresh,deleteTableContents')").createEntityManager().close();
    }

    public void tearDown() throws Exception {

    }

    public void testRefreshDeleteTableContents() throws Exception {
        String sql = "SELECT COUNT (e) from Employee e";
        OpenJPAEntityManagerSPI em = emf.createEntityManager();

        long before = em.createQuery(sql, Long.class).getSingleResult();

        em.getTransaction().begin();
        Employee e = new Employee();
        e.setEmpId(System.currentTimeMillis());
        em.persist(e);
        em.getTransaction().commit();
        // Make sure that we created a new one.
        long update = em.createQuery(sql, Long.class).getSingleResult();
        assertEquals(Long.valueOf(before + 1), Long.valueOf(update));
        em.clear();
        // Create the second PU, see if that cleans up tables it shouldn't be.
        createSecondPU();

        long after = em.createQuery(sql, Long.class).getSingleResult();
        assertEquals(update, after);

    }
}
