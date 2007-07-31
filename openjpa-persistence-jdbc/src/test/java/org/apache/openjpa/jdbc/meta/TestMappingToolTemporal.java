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

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.simple.TemporalFieldTypes;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestMappingToolTemporal extends SQLListenerTestCase {

    public void setUp() {
        setUp(CLEAR_TABLES, TemporalFieldTypes.class);
    }

    public void testMappingToolTemporal() throws IOException, SQLException {

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new TemporalFieldTypes());
        em.getTransaction().commit();
        em.close();

        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*dateDefaultField TIMESTAMP.*)");
        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*dateDateField DATE.*)");
        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*dateTimeField TIME.*)");
        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*dateTimestampField TIMESTAMP.*)");

        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*calendarDefaultField TIMESTAMP.*)");
        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*calendarDateField DATE.*)");
        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*calendarTimeField TIME.*)");
        assertSQL("CREATE TABLE TemporalFieldTypes "
                + "(.*calendarTimestampField TIMESTAMP.*)");
    }
}
