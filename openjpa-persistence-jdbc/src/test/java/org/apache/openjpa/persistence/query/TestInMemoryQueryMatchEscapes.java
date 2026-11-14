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
package org.apache.openjpa.persistence.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestInMemoryQueryMatchEscapes
    extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, AllFieldTypes.class);
    }

    @Test
    void databaseEscape() {
        OpenJPAQuery q = escapeHelper(false);
        q.setFlushMode(FlushModeType.AUTO);
        q.getEntityManager().flush();
        AllFieldTypes aft = (AllFieldTypes) q.getSingleResult();
        assertEquals("foo_bar", aft.getStringField());
        q.getEntityManager().getTransaction().rollback();
    }

    @Test
    void inMemoryEscape() {
        OpenJPAQuery q = escapeHelper(true);
        q.setFlushMode(FlushModeType.COMMIT);
        try {
            q.getSingleResult();
            fail("OpenJPA doesn't support escape syntax for in-mem queries");
        } catch (Exception e) {
            // expected
        }
    }

    private OpenJPAQuery escapeHelper(boolean inMem) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes aft = new AllFieldTypes();
        aft.setStringField("foo_bar");
        em.persist(aft);
        aft = new AllFieldTypes();
        aft.setStringField("foozbar");
        em.persist(aft);

        return (OpenJPAQuery) em.createQuery(
            "select e from AllFieldTypes e where e.stringField " +
            "like 'foox_bar' escape 'x'");
    }
}
