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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests MapJoin queries with @OneToMany(mappedBy=...) @MapKey(name=...)
 * relationships using property-based access (like TCK entities).
 */
public class TestMapJoinQuery extends SingleEMFTestCase {

    @Entity
    @Table(name = "MJ_DEPT")
    public static class MJDepartment implements java.io.Serializable {
        private int id;
        private String name;
        private Map<String, MJEmployee> lastNameEmployees;

        public MJDepartment() {}
        public MJDepartment(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Id
        @Column(name = "ID")
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        @Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @OneToMany(mappedBy = "department")
        @MapKey(name = "lastName")
        public Map<String, MJEmployee> getLastNameEmployees() { return lastNameEmployees; }
        public void setLastNameEmployees(Map<String, MJEmployee> m) { this.lastNameEmployees = m; }
    }

    @Entity
    @Table(name = "MJ_EMP")
    public static class MJEmployee implements java.io.Serializable {
        private int id;
        private String firstName;
        private String lastName;
        private MJDepartment department;

        public MJEmployee() {}
        public MJEmployee(int id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Id
        @Column(name = "ID")
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        @Column(name = "FIRSTNAME")
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }

        @Column(name = "LASTNAME")
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }

        @ManyToOne
        @JoinColumn(name = "FK_DEPT")
        public MJDepartment getDepartment() { return department; }
        public void setDepartment(MJDepartment d) { this.department = d; }
    }

    @Override
    public void setUp() {
        setUp(MJDepartment.class, MJEmployee.class,
            CLEAR_TABLES,
            "openjpa.Log", "SQL=TRACE",
            "openjpa.RuntimeUnenhancedClasses", "supported");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        MJDepartment dept1 = new MJDepartment(1, "Marketing");
        MJDepartment dept2 = new MJDepartment(2, "Administration");

        MJEmployee emp1 = new MJEmployee(1, "Alan", "Frechette");
        emp1.setDepartment(dept1);
        MJEmployee emp2 = new MJEmployee(2, "Arthur", "Frechette");
        emp2.setDepartment(dept2);
        MJEmployee emp3 = new MJEmployee(3, "Shelly", "McGowan");
        emp3.setDepartment(dept1);

        Map<String, MJEmployee> link = new HashMap<>();
        link.put(emp1.getLastName(), emp1);
        link.put(emp3.getLastName(), emp3);
        dept1.setLastNameEmployees(link);

        Map<String, MJEmployee> link2 = new HashMap<>();
        link2.put(emp2.getLastName(), emp2);
        dept2.setLastNameEmployees(link2);

        em.persist(dept1);
        em.persist(dept2);
        em.persist(emp1);
        em.persist(emp2);
        em.persist(emp3);

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Test: SELECT d FROM MJDepartment d JOIN d.lastNameEmployees e WHERE (e.id = 1)
     */
    public void testJoinMapAttribute() {
        EntityManager em = emf.createEntityManager();

        // JPQL equivalent
        List<?> jpqlResults = em.createQuery(
            "SELECT d FROM TestMapJoinQuery$MJDepartment d JOIN d.lastNameEmployees e WHERE e.id = 1")
            .getResultList();
        assertEquals("JPQL should return 1 result", 1, jpqlResults.size());

        // Criteria API with MapJoin
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MJDepartment> cq = cb.createQuery(MJDepartment.class);
        Root<MJDepartment> dept = cq.from(MJDepartment.class);
        MapJoin<MJDepartment, String, MJEmployee> emp = dept.joinMap("lastNameEmployees");
        cq.where(cb.equal(emp.get("id"), "1")).select(dept);

        List<MJDepartment> results = em.createQuery(cq).getResultList();
        assertEquals("MapJoin criteria should return 1 result", 1, results.size());
        assertEquals(1, results.get(0).getId());

        em.close();
    }

    /**
     * Test: SELECT d.id, d.name, e.lastName FROM MJDepartment d JOIN d.lastNameEmployees e WHERE (e.id = 1)
     */
    public void testJoinMapString() {
        EntityManager em = emf.createEntityManager();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<MJDepartment> dept = cq.from(MJDepartment.class);
        MapJoin<MJDepartment, String, MJEmployee> emp = dept.joinMap("lastNameEmployees");
        cq.where(cb.equal(emp.get("id"), "1"));
        cq.multiselect(dept.get("id"), dept.get("name"),
            emp.value().<String>get("lastName"));

        List<Tuple> results = em.createQuery(cq).getResultList();
        assertEquals("Should return 1 result", 1, results.size());
        Tuple t = results.get(0);
        assertEquals(1, t.get(0));
        assertEquals("Marketing", t.get(1));
        assertEquals("Frechette", t.get(2));

        em.close();
    }
}
