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
package org.apache.openjpa.persistence.nullity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.RollbackException;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.jdbc.sql.SybaseDictionary;
import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test @Basic(optional=true|false) and @Column(nullable=true|false)
 * specification is honored.
 * Note: null constraint violation manifests as different exception types
 * for option and nullable condition.
 *
 * @author Pinaki Poddar
 */
class TestBasicFieldNullity extends AbstractNullityTestCase {

    private DBDictionary dict = null;

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, RETAIN_DATA, NullValues.class);
        dict = ((JDBCConfiguration)emf.getConfiguration()).getDBDictionaryInstance();
    }

    @Test
    void nullOnOptionalFieldIsAllowed() {
    	NullValues pc = new NullValues();
    	pc.setOptional(null);
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void nullOnNonOptionalFieldIsDisallowed() {
    	NullValues pc = new NullValues();
    	pc.setNotOptional(null);
    	assertCommitFails(pc, NEW, InvalidStateException.class);
    }

    @Test
    void notNullOnOptionalFieldIsAllowed() {
    	NullValues pc = new NullValues();
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void notNullOnNonOptionalFieldIsAllowed() {
    	NullValues pc = new NullValues();
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void nullOnNullableColumnAllowed() {
    	NullValues pc = new NullValues();
    	pc.setNullable(null);
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void nullOnNonNullableColumnIsDisallowed() {
    	NullValues pc = new NullValues();
    	pc.setNotNullable(null);
    	assertCommitFails(pc, NEW, RollbackException.class);
    }

    @Test
    void notNullOnNullableColumnIsAllowed() {
    	NullValues pc = new NullValues();
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void notNullOnNonNullableColumnIsAllowed() {
    	NullValues pc = new NullValues();
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void nullOnOptionalBlobFieldIsAllowed() {
    	NullValues pc = new NullValues();
    	pc.setOptionalBlob(null);
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void nullOnNonOptionalBlobFieldIsDisallowed() {
    	NullValues pc = new NullValues();
    	pc.setNotOptionalBlob(null);
    	assertCommitFails(pc, NEW, InvalidStateException.class);
    }

    @Test
    void nullOnNullableBlobColumnAllowed() {
    	NullValues pc = new NullValues();
    	pc.setNullableBlob(null);
    	assertCommitSucceeds(pc, NEW);
    }

    @Test
    void nullOnNonNullableBlobColumnIsDisallowed() {
    	NullValues pc = new NullValues();
    	pc.setNotNullableBlob(null);
    	assertCommitFails(pc, NEW, RollbackException.class);
    }

    @Test
    void x() {
    	NullValues pc = new NullValues();
    	assertCommitSucceeds(pc, NEW);
    	OpenJPAPersistence.getEntityManager(pc).close();

    	pc.setNotNullableBlob(null);
    	assertCommitFails(pc, !NEW, RollbackException.class);
    }


    @Test
    void uniqueStringColumnCanBeNull() {
        if (!isUniqueColumnNullable()) {
            return;
        }
        NullValues pc = new NullValues();
        pc.setUniqueNullable(null);
        assertCommitSucceeds(pc, NEW);
    }

    @Test
    void uniqueStringColumnAsNull() {
        if (!isUniqueColumnNullable()) {
            return;
        }
        NullValues pc = new NullValues();
        pc.setUniqueNullable(null);
        assertCommitSucceeds(pc, NEW);

        String jpql = "select n from NullValues n where n.uniqueNullable = :p";
        EntityManager em = emf.createEntityManager();
        List<NullValues> result = em.createQuery(jpql, NullValues.class)
                                    .setParameter("p", null)
                                    .getResultList();
        assertFalse(result.isEmpty());
        for (NullValues n : result)
            assertNull(n.getUniqueNullable());
    }

    @Test
    void uniqueStringColumnAsEmpty() {
        String EMPTY_STRING = "";
        NullValues pc = new NullValues();
        pc.setUniqueNullable(EMPTY_STRING);
        assertCommitSucceeds(pc, NEW);

        String jpql = "select n from NullValues n where n.uniqueNullable = :p";
        if (dict instanceof OracleDictionary)
            jpql = "select n from NullValues n where n.uniqueNullable IS NULL";
        EntityManager em = emf.createEntityManager();
        Query  query = em.createQuery(jpql, NullValues.class);
        if (!(dict instanceof OracleDictionary))
            query.setParameter("p", EMPTY_STRING);
        List<NullValues> result = query.getResultList();
        assertFalse(result.isEmpty());
        for (NullValues n : result) {
            if (dict instanceof OracleDictionary) {
                assertNull(n.getUniqueNullable());
            }
            else if (dict instanceof SybaseDictionary) {
                // Sybase converts empty strings to ""
                assertEquals(" ", n.getUniqueNullable());
            }
            else {
                assertEquals(EMPTY_STRING, n.getUniqueNullable());
            }
        }
    }

    boolean isUniqueColumnNullable() {
        return ((JDBCConfiguration)emf.getConfiguration()).getDBDictionaryInstance().supportsNullUniqueColumn;
    }
}

