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
package org.apache.openjpa.persistence.entitygraph;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests for EntityGraph API (JPA 2.1 / 3.2).
 * Mirrors the TCK EntityGraph tests.
 */
public class TestEntityGraph extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(EGEmployee.class, EGEmployee2.class, EGEmployee3.class,
              EGDepartment.class, CLEAR_TABLES);
    }

    /**
     * Test addAttributeNodes(String...) and getAttributeNodes().
     * Mirrors TCK addAttributeNodesStringArrayTest.
     */
    public void testAddAttributeNodesString() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGEmployee> eg = em.createEntityGraph(EGEmployee.class);
            eg.addAttributeNodes("firstName", "lastName");

            List<AttributeNode<?>> nodes = eg.getAttributeNodes();
            List<String> names = new ArrayList<>();
            for (AttributeNode<?> n : nodes) {
                names.add(n.getAttributeName());
            }

            assertTrue(names.contains("firstName"));
            assertTrue(names.contains("lastName"));
            assertEquals(2, names.size());
        } finally {
            em.close();
        }
    }

    /**
     * Test that addAttributeNodes with invalid name throws IAE.
     * Mirrors TCK addAttributeNodesStringArrayIllegalArgumentExceptionTest.
     */
    public void testAddAttributeNodesStringIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGEmployee2> eg = em.createEntityGraph(EGEmployee2.class);
            try {
                eg.addAttributeNodes("doesnotexist");
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * Test addAttributeNodes(Attribute...) using metamodel.
     * Mirrors TCK addAttributeNodesAttributeArrayTest.
     */
    @SuppressWarnings("unchecked")
    public void testAddAttributeNodesAttribute() {
        EntityManager em = emf.createEntityManager();
        try {
            Metamodel mm = em.getMetamodel();
            ManagedType<EGEmployee> mt = mm.managedType(EGEmployee.class);

            Attribute<? super EGEmployee, ?> fnAttr = mt.getDeclaredAttribute("firstName");
            Attribute<? super EGEmployee, ?> lnAttr = mt.getDeclaredAttribute("lastName");

            EntityGraph<EGEmployee> eg = em.createEntityGraph(EGEmployee.class);
            eg.addAttributeNodes(fnAttr, lnAttr);

            List<AttributeNode<?>> nodes = eg.getAttributeNodes();
            List<String> names = new ArrayList<>();
            for (AttributeNode<?> n : nodes) {
                names.add(n.getAttributeName());
            }

            assertTrue(names.contains("firstName"));
            assertTrue(names.contains("lastName"));
            assertEquals(2, names.size());
        } finally {
            em.close();
        }
    }

    /**
     * Test createEntityGraph(String) for named graph and unknown name.
     * Mirrors TCK createEntityGraphStringTest.
     */
    public void testCreateEntityGraphString() {
        EntityManager em = emf.createEntityManager();
        try {
            // known named graph should return non-null
            EntityGraph<?> eg = em.createEntityGraph("first_last_graph");
            assertNotNull("Expected non-null EntityGraph for known name", eg);

            // unknown name should return null
            EntityGraph<?> eg2 = em.createEntityGraph("doesnotexist");
            assertNull("Expected null for unknown graph name", eg2);
        } finally {
            em.close();
        }
    }

    /**
     * Test getEntityGraph(String).
     * Mirrors TCK getEntityGraphStringTest and entityGraphGetNameTest.
     */
    public void testGetEntityGraphString() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<?> eg = em.getEntityGraph("first_last_graph");
            assertNotNull(eg);
            assertEquals("first_last_graph", eg.getName());
        } finally {
            em.close();
        }
    }

    /**
     * Test getEntityGraph with unknown name throws IAE.
     * Mirrors TCK getEntityGraphStringIllegalArgumentExceptionTest.
     */
    public void testGetEntityGraphStringIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            try {
                em.getEntityGraph("doesnotexist");
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * Test unnamed @NamedEntityGraph defaults to entity name.
     * Mirrors TCK entityGraphGetNameNoNameExistsTest.
     */
    public void testNameDefaultsToEntityName() {
        EntityManager em = emf.createEntityManager();
        try {
            List<EntityGraph<? super EGEmployee2>> egs =
                em.getEntityGraphs(EGEmployee2.class);
            assertEquals("Expected 1 named entity graph", 1, egs.size());
            assertEquals("EGEmployee2", egs.get(0).getName());
        } finally {
            em.close();
        }
    }

    /**
     * Test createEntityGraph(Class) returns graph with null name.
     * Mirrors TCK getNameTest.
     */
    public void testCreateEntityGraphClassName() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGEmployee2> eg = em.createEntityGraph(EGEmployee2.class);
            assertNull("Name should be null for dynamically created graph",
                eg.getName());
        } finally {
            em.close();
        }
    }

    /**
     * Test getEntityGraphs(Class) returns all named graphs.
     * Mirrors TCK getEntityGraphsClassTest.
     */
    public void testGetEntityGraphsClass() {
        EntityManager em = emf.createEntityManager();
        try {
            List<EntityGraph<? super EGEmployee3>> egs =
                em.getEntityGraphs(EGEmployee3.class);

            List<String> names = new ArrayList<>();
            for (EntityGraph<?> eg : egs) {
                names.add(eg.getName());
            }

            assertTrue(names.contains("first_last_graph"));
            assertTrue(names.contains("last_salary_graph"));
            assertTrue(names.contains("lastname_department_subgraphs"));
            assertEquals(3, names.size());
        } finally {
            em.close();
        }
    }

    /**
     * Test getEntityGraphs with non-entity class throws IAE.
     * Mirrors TCK getEntityGraphsClassIllegalArgumentExceptionTest.
     */
    public void testGetEntityGraphsClassIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            try {
                em.getEntityGraphs(TestEntityGraph.class);
                fail("Expected IllegalArgumentException for non-entity class");
            } catch (IllegalArgumentException e) {
                // expected
            }
        } finally {
            em.close();
        }
    }

    /**
     * Test addNamedEntityGraph via EMF, then retrieve, then override.
     * Mirrors TCK addNamedEntityGraphStringEntityGraphTest.
     */
    public void testAddNamedEntityGraph() {
        EntityManager em = emf.createEntityManager();
        try {
            String graphName = "new_named_entity_graph";

            // add named graph
            EntityGraph<EGEmployee> eg = em.createEntityGraph(EGEmployee.class);
            eg.addAttributeNodes("id");
            em.getEntityManagerFactory().addNamedEntityGraph(graphName, eg);

            // retrieve and verify
            EntityGraph<?> eg2 = em.getEntityGraph(graphName);
            assertNotNull(eg2);
            assertEquals(graphName, eg2.getName());

            List<AttributeNode<?>> nodes = eg2.getAttributeNodes();
            assertEquals(1, nodes.size());
            assertEquals("id", nodes.get(0).getAttributeName());

            // override
            EntityGraph<EGEmployee> eg3 = em.createEntityGraph(EGEmployee.class);
            eg3.addAttributeNodes("lastName");
            em.getEntityManagerFactory().addNamedEntityGraph(graphName, eg3);

            EntityGraph<?> eg4 = em.getEntityGraph(graphName);
            assertNotNull(eg4);
            assertEquals(graphName, eg4.getName());
            List<AttributeNode<?>> nodes2 = eg4.getAttributeNodes();
            assertEquals(1, nodes2.size());
            assertEquals("lastName", nodes2.get(0).getAttributeName());
        } finally {
            em.close();
        }
    }

    /**
     * Test annotation-parsed graph nodes.
     * Mirrors TCK annotationsTest.
     */
    public void testAnnotationAttributeNodes() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<?> eg = em.getEntityGraph("last_salary_graph");
            assertNotNull(eg);

            List<AttributeNode<?>> nodes = eg.getAttributeNodes();
            List<String> names = new ArrayList<>();
            for (AttributeNode<?> n : nodes) {
                names.add(n.getAttributeName());
            }

            assertTrue(names.contains("lastName"));
            assertTrue(names.contains("salary"));
            assertEquals(2, names.size());
        } finally {
            em.close();
        }
    }
}
