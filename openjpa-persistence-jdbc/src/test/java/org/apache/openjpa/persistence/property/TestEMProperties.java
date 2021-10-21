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
 * Unless required by applicable law or agEmployee_Last_Name to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.property;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * <b>TestEMProperties</b> is used to test various persistence properties set through EntityManager.setProperty() API
 * to ensure no errors are thrown.
 */
public class TestEMProperties extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(EntityContact.class,
              EmbeddableAddress.class,
              DROP_TABLES, "javax.persistence.query.timeout", 23456);
    }

    public void testQueryTimeoutPropertyDefault() {
        EntityManager em = emf.createEntityManager();

        String sql = "select * from EntityContact";
        OpenJPAQuery<?> query = OpenJPAPersistence.cast(em.createNativeQuery(sql));
        assertEquals(23456, query.getFetchPlan().getQueryTimeout());

        em.clear();
        em.close();
    }

    public void testQueryTimeoutPropertyOnEntityManagerCreation() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("javax.persistence.query.timeout", "12345");
        // Setting a value of type String should convert if possible and not return an error
        EntityManager em = emf.createEntityManager(properties);

        String sql = "select * from EntityContact";
        OpenJPAQuery<?> query = OpenJPAPersistence.cast(em.createNativeQuery(sql));
        assertEquals(12345, query.getFetchPlan().getQueryTimeout());

        em.clear();
        em.close();
    }

    public void testQueryTimeoutPropertySetOnEntityManager() {
        EntityManager em = emf.createEntityManager();

        // Setting a value of type String should convert if possible and not return an error
        em.setProperty("javax.persistence.query.timeout", "12345");

        String sql = "select * from EntityContact";
        OpenJPAQuery<?> query = OpenJPAPersistence.cast(em.createNativeQuery(sql));
        assertEquals(12345, query.getFetchPlan().getQueryTimeout());

        em.clear();
        em.close();
    }
}
