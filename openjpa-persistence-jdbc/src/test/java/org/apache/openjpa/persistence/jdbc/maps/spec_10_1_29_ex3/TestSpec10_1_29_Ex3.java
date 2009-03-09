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
package org.apache.openjpa.persistence.jdbc.maps.spec_10_1_29_ex3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestSpec10_1_29_Ex3 extends SingleEMFTestCase {
    public int numStudents = 2;
    public int numCoursesPerStudent = 2;
    public List<String> namedQueries = new ArrayList<String>();

    public int studentId = 1;
    public int courseId = 1;
    public int semesterId = 1;

    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    public void setUp() {
        super.setUp(CLEAR_TABLES,
            Course.class,
            Semester.class,
            Student.class,
            "openjpa.jdbc.JDBCListeners", 
            new JDBCListener[] { this.new Listener() });
        createObj(emf);
    }

    public void testQueryObj() throws Exception {
        queryObj(emf);
    }

    public void testQueryQualifiedId() throws Exception {
        EntityManager em = emf.createEntityManager();

        String query = "select KEY(e), e from Student s, " +
            " in (s.enrollment) e order by s.id";
        List rs = em.createQuery(query).getResultList();
        Course c = (Course) ((Object[]) rs.get(0))[0];
        Semester s = (Semester) ((Object[]) rs.get(0))[1];

        em.clear();
        String query4 = "select ENTRY(e) from Student s, " +
            " in (s.enrollment) e order by s.id";
        List rs4 = em.createQuery(query4).getResultList();
        Map.Entry me = (Map.Entry) rs4.get(0);

        assertTrue(c.equals(me.getKey()));
        assertEquals(s.getId(), ((Semester) me.getValue()).getId());

        em.clear();
        query = "select KEY(e), e from Student s " +
            " left join s.enrollment e order by s.id";
        rs = em.createQuery(query).getResultList();
        c = (Course) ((Object[]) rs.get(0))[0];
        s = (Semester) ((Object[]) rs.get(0))[1];

        em.clear();
        query4 = "select ENTRY(e) from Student s " +
            " left join s.enrollment e order by s.id";
        rs4 = em.createQuery(query4).getResultList();
        me = (Map.Entry) rs4.get(0);

        assertTrue(c.equals(me.getKey()));
        assertEquals(s.getId(), ((Semester) me.getValue()).getId());

        em.close();
    }

    public List<String> getSql() {
        return sql;
    }

    public int getSqlCount() {
        return sqlCount;
    }

    public void createObj(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        for (int i = 0; i < numStudents; i++)
            createStudent(em, studentId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createStudent(EntityManager em, int id) {
        Student s = new Student();
        s.setId(id);
        for (int i = 0; i < numCoursesPerStudent; i++) {
            Course c = createCourse(em, courseId++);
            Semester semester = createSemester(em, semesterId++);
            s.addToEnrollment(c, semester);
            em.persist(c);
            em.persist(semester);
        }
        em.persist(s);
    }

    public Course createCourse(EntityManager em, int id) {
        Course c = new Course();
        c.setId(id);
        c.setName("s" + id);
        return c;
    }

    public Semester createSemester(EntityManager em, int id) {
        Semester s = new Semester();
        s.setId(id);
        s.setName("s" + id);
        return s;
    }

    public void findObj(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        Student s = em.find(Student.class, 1);
        assertStudent(s);

        Course c = em.find(Course.class, 1);
        assertCourse(c);

        Semester sm = em.find(Semester.class, 1);
        assertSemester(sm);

        em.close();
    }

    public void assertStudent(Student s) {
        int id = s.getId();
        Map enrollment = s.getEnrollment();
        Assert.assertEquals(2, enrollment.size());
    }

    public void assertCourse(Course c) {
        long id = c.getId();
        String name = c.getName();
    }

    public void assertSemester(Semester s) {
        long id = s.getId();
        String name = s.getName();
    }

    public void queryObj(EntityManagerFactory emf) {
        queryStudent(emf);
        queryCourse(emf);
        querySemester(emf);
    }

    public void queryStudent(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select s from Student s");
        List<Student> ss = q.getResultList();
        for (Student s : ss){
            assertStudent(s);
        }
        tran.commit();
        em.close();
    }

    public void queryCourse(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select c from Course c");
        List<Course> cs = q.getResultList();
        for (Course c : cs){
            assertCourse(c);
        }
        tran.commit();
        em.close();
    }

    public void querySemester(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select s from Semester s");
        List<Semester> ss = q.getResultList();
        for (Semester s : ss){
            assertSemester(s);
        }
        tran.commit();
        em.close();
    }

    public class Listener extends AbstractJDBCListener {
        @Override
        public void beforeExecuteStatement(JDBCEvent event) {
            if (event.getSQL() != null && sql != null) {
                sql.add(event.getSQL());
                sqlCount++;
            }
        }
    }
}

