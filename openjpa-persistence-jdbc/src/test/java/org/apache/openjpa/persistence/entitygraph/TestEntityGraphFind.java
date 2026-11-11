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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Proxy;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.ImplHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@code EntityManager.find(EntityGraph, Object, FindOption...)} (JPA 3.2).
 * The graph is interpreted as a <em>load graph</em>: attributes named by the graph are fetched eagerly,
 * attributes outside the graph keep their declared fetch behaviour.
 */
class TestEntityGraphFind extends SingleEMFTestCase {

    private static final int DEPT_ID = 1;
    private static final int EMP_ID = 10;

    @BeforeEach
    public void setUp() {
        setUp(EGEmployee.class, EGEmployee2.class, EGEmployee3.class,
              EGDepartment.class, CLEAR_TABLES);
        createData();
    }

    private void createData() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            EGDepartment dept = new EGDepartment(DEPT_ID, "Engineering");
            em.persist(dept);
            for (int i = 0; i < 2; i++) {
                EGEmployee emp = new EGEmployee();
                emp.setId(EMP_ID + i);
                emp.setFirstName("First" + i);
                emp.setLastName("Last" + i);
                emp.setSalary(1000 + i);
                emp.setDepartment(dept);
                dept.getEmployees().add(emp);
                em.persist(emp);
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Direct, non-brittle load state check: looks at the state manager's loaded bit for the given attribute
     * rather than counting SQL statements.
     */
    private boolean isLoaded(Object entity, String attribute) {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(entity, emf.getConfiguration());
        assertNotNull(pc, "not a persistence capable instance");
        OpenJPAStateManager sm = (OpenJPAStateManager) pc.pcGetStateManager();
        assertNotNull(sm, "entity is not managed");
        FieldMetaData fmd = sm.getMetaData().getField(attribute);
        assertNotNull(fmd, "no such persistent attribute: " + attribute);
        return sm.getLoaded().get(fmd.getIndex());
    }

    /**
     * Baseline: without a graph the lazy collection is not loaded. Without this control the positive
     * assertion below would be vacuous.
     */
    @Test
    void findWithoutGraphLeavesLazyAttributeUnloaded() {
        EntityManager em = emf.createEntityManager();
        try {
            EGDepartment dept = em.find(EGDepartment.class, DEPT_ID);
            assertNotNull(dept);
            assertFalse(isLoaded(dept, "employees"), "employees must be lazy without a graph");
        } finally {
            em.close();
        }
    }

    /**
     * The load bearing test: the very same lookup with a graph naming the lazy collection loads it eagerly.
     */
    @Test
    void findWithEntityGraphLoadsGraphAttribute() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGDepartment> graph = em.createEntityGraph(EGDepartment.class);
            graph.addAttributeNodes("employees");
            EGDepartment dept = em.find(graph, DEPT_ID);
            assertNotNull(dept);
            assertEquals(DEPT_ID, dept.getId());
            assertTrue(isLoaded(dept, "employees"), "employees must be loaded by the load graph");
            assertEquals(2, dept.getEmployees().size());
        } finally {
            em.close();
        }
    }

    /**
     * Load graph, not fetch graph: attributes outside the graph keep their declared (eager) behaviour.
     */
    @Test
    void findWithEntityGraphKeepsNonGraphAttributesDefault() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGEmployee> graph = em.createEntityGraph(EGEmployee.class);
            graph.addAttributeNodes("firstName");
            EGEmployee emp = em.find(graph, EMP_ID);
            assertNotNull(emp);
            assertEquals("First0", emp.getFirstName());
            assertTrue(isLoaded(emp, "lastName"), "lastName is outside the graph but eager by default");
            assertNotNull(emp.getDepartment());
        } finally {
            em.close();
        }
    }

    /**
     * Subgraphs are applied recursively: the department reached from the employee has its lazy collection
     * loaded. The control below proves the plain find() does not.
     */
    @Test
    void findWithSubgraph() {
        EntityManager control = emf.createEntityManager();
        try {
            EGEmployee emp = control.find(EGEmployee.class, EMP_ID);
            assertNotNull(emp);
            assertFalse(isLoaded(emp.getDepartment(), "employees"));
        } finally {
            control.close();
        }

        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGEmployee> graph = em.createEntityGraph(EGEmployee.class);
            graph.addSubgraph("department").addAttributeNodes("employees");
            EGEmployee emp = em.find(graph, EMP_ID);
            assertNotNull(emp);
            assertNotNull(emp.getDepartment());
            assertTrue(isLoaded(emp.getDepartment(), "employees"), "the subgraph must load department.employees");
            assertEquals(2, emp.getDepartment().getEmployees().size());
        } finally {
            em.close();
        }
    }

    /**
     * A graph that references itself must not send the graph traversal into an endless recursion.
     */
    @Test
    void findWithCyclicGraph() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGDepartment> graph = em.createEntityGraph(EGDepartment.class);
            jakarta.persistence.Subgraph<EGEmployee> emps = graph.addSubgraph("employees", EGEmployee.class);
            emps.addSubgraph("department", EGDepartment.class).addAttributeNodes("employees");
            EGDepartment dept = em.find(graph, DEPT_ID);
            assertNotNull(dept);
            assertTrue(isLoaded(dept, "employees"));
        } finally {
            em.close();
        }
    }

    /**
     * FindOptions are honoured on the graph based overload as well, sharing the option parsing with
     * {@code find(Class, Object, FindOption...)}.
     */
    @Test
    void findWithGraphAndFindOptions() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGDepartment> graph = em.createEntityGraph(EGDepartment.class);
            graph.addAttributeNodes("employees");
            EGDepartment dept = em.find(graph, DEPT_ID, CacheStoreMode.BYPASS, CacheRetrieveMode.BYPASS);
            assertNotNull(dept);
            assertTrue(isLoaded(dept, "employees"));
        } finally {
            em.close();
        }
    }

    @Test
    void findWithGraphReturnsNullWhenMissing() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGDepartment> graph = em.createEntityGraph(EGDepartment.class);
            graph.addAttributeNodes("employees");
            assertNull(em.find(graph, 9999));
        } finally {
            em.close();
        }
    }

    @Test
    void findWithNullGraphThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            em.find((EntityGraph<EGDepartment>) null, DEPT_ID);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        } finally {
            em.close();
        }
    }

    @Test
    void findWithNullPrimaryKeyThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGDepartment> graph = em.createEntityGraph(EGDepartment.class);
            em.find(graph, null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void findWithForeignEntityGraphThrowsIAE() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<EGDepartment> foreign = (EntityGraph<EGDepartment>) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { EntityGraph.class }, (p, m, a) -> null);
            em.find(foreign, DEPT_ID);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(String.valueOf(expected.getMessage()).contains("Unknown EntityGraph implementation"));
        } finally {
            em.close();
        }
    }
}
