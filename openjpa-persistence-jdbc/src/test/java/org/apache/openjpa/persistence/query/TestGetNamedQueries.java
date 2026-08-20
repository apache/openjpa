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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQueryReference;

import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.TypedQueryReferenceImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests the JPA 3.2 {@code EntityManagerFactory.getNamedQueries(Class)} and the
 * {@code @NamedQuery.resultClass()} support it relies on.
 */
public class TestGetNamedQueries extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(NamedQueryRefEntity.class, CLEAR_TABLES);
    }

    /**
     * Regression test for @NamedQuery.resultClass() being parsed at all.
     */
    public void testAnnotationResultClassParsed() {
        MetaDataRepository mdr = emf.getConfiguration().getMetaDataRepositoryInstance();

        QueryMetaData all = mdr.getQueryMetaData(null, "NQRef.all", null, true);
        assertEquals(NamedQueryRefEntity.class, all.getCandidateType());
        assertNull(all.getResultType());

        QueryMetaData names = mdr.getQueryMetaData(null, "NQRef.names", null, true);
        assertEquals(String.class, names.getResultType());
        assertNull(names.getCandidateType());

        // resultClass() defaults to void.class - it must not leak into the metadata
        QueryMetaData untyped = mdr.getQueryMetaData(null, "NQRef.untyped", null, true);
        assertNull(untyped.getCandidateType());
        assertNull(untyped.getResultType());
    }

    public void testEntityResultType() {
        Map<String, TypedQueryReference<NamedQueryRefEntity>> refs =
            emf.getNamedQueries(NamedQueryRefEntity.class);
        assertTrue(refs.containsKey("NQRef.all"));
        assertTrue(refs.containsKey("NQRef.native"));
        assertFalse(refs.containsKey("NQRef.names"));
        assertFalse(refs.containsKey("NQRef.untyped"));

        TypedQueryReference<NamedQueryRefEntity> ref = refs.get("NQRef.all");
        assertEquals("NQRef.all", ref.getName());
        assertEquals(NamedQueryRefEntity.class, ref.getResultType());
    }

    public void testScalarResultType() {
        Map<String, TypedQueryReference<String>> refs = emf.getNamedQueries(String.class);
        assertTrue(refs.containsKey("NQRef.names"));
        assertEquals(String.class, refs.get("NQRef.names").getResultType());
        assertFalse(refs.containsKey("NQRef.all"));
        assertFalse(refs.containsKey("NQRef.untyped"));
    }

    /**
     * Queries without a declared result type are never returned - not even for Object.class.
     */
    public void testSupertypeIsAssignable() {
        Map<String, TypedQueryReference<Object>> refs = emf.getNamedQueries(Object.class);
        assertTrue(refs.containsKey("NQRef.all"));
        assertTrue(refs.containsKey("NQRef.names"));
        assertTrue(refs.containsKey("NQRef.native"));
        assertFalse(refs.containsKey("NQRef.untyped"));
    }

    public void testHints() {
        Map<String, Object> hints = emf.getNamedQueries(NamedQueryRefEntity.class)
            .get("NQRef.all").getHints();
        assertEquals("2", hints.get("openjpa.FetchPlan.MaxFetchDepth"));
        try {
            hints.put("x", "y");
            fail("hints must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /**
     * The hints of a {@link TypedQueryReference} must keep the declaration order they have in the
     * metadata, because {@link jakarta.persistence.EntityManager#createQuery(TypedQueryReference)}
     * replays them in iteration order and OpenJPA has aliased hint keys writing the same setting.
     */
    public void testHintsKeepDeclarationOrder() {
        Map<String, Object> declared = new LinkedHashMap<>();
        declared.put("openjpa.FetchPlan.LockTimeout", "1000");
        declared.put("jakarta.persistence.lock.timeout", "2000");
        declared.put("openjpa.FetchPlan.MaxFetchDepth", "2");
        Map<String, Object> hints = new TypedQueryReferenceImpl<>(
            "NQRef.all", NamedQueryRefEntity.class, declared).getHints();
        assertEquals(new ArrayList<>(declared.keySet()), new ArrayList<>(hints.keySet()));
    }

    public void testReturnedMapIsACopy() {
        Map<String, TypedQueryReference<NamedQueryRefEntity>> refs =
            emf.getNamedQueries(NamedQueryRefEntity.class);
        refs.clear();
        assertTrue(emf.getNamedQueries(NamedQueryRefEntity.class).containsKey("NQRef.all"));
    }

    public void testNullResultTypeRejected() {
        try {
            emf.getNamedQueries(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * getNamedQueries() must work as the very first operation on a factory, i.e. before any
     * entity manager forced the metadata repository to load the persistent types.
     */
    public void testColdRepository() {
        OpenJPAEntityManagerFactorySPI emf2 = createEMF(NamedQueryRefEntity.class);
        try {
            assertTrue(emf2.getNamedQueries(NamedQueryRefEntity.class).containsKey("NQRef.all"));
        } finally {
            closeEMF(emf2);
        }
    }

    public void testDynamicallyAddedNamedQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery("select o from NamedQueryRefEntity o");
            emf.addNamedQuery("NQRef.dyn", q);
            assertTrue(emf.getNamedQueries(NamedQueryRefEntity.class).containsKey("NQRef.dyn"));
        } finally {
            em.close();
        }
    }

    /**
     * The declared result class must not break execution of the named queries.
     */
    public void testCreateNamedQueryWithDeclaredResultClassStillWorks() {
        EntityManager em = emf.createEntityManager();
        try {
            assertNotNull(em.createNamedQuery("NQRef.all").getResultList());
            assertNotNull(em.createNamedQuery("NQRef.names", String.class).getResultList());
            assertNotNull(em.createQuery(emf.getNamedQueries(NamedQueryRefEntity.class)
                .get("NQRef.all")).getResultList());
        } finally {
            em.close();
        }
    }
}
