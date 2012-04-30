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
package org.apache.openjpa.jdbc.oracle;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;


/**
 * This test case demonstrates that we currently do way too much sub selects
 * on Oracle if &#064;Embedded fields with a &#064;Lob are involved.
 *
 * For running the test you can use the following commandline in
 * openjpa-persistence-jdbc:
 *
 *
 * <pre>
 * mvn clean test -Dtest=TestOracleDistinctJoin -Doracle.artifactid=ojdbc14 -Doracle.version=10.2.0.4.0
 *                -Dopenjpa.oracle.url="jdbc:oracle:thin:@192.168.1.6/XE"
 *                -Dopenjpa.oracle.username=username -Dopenjpa.oracle.password=yourpwd
 *                -Dopenjpa.Log=DefaultLevel=TRACE -Dtest-oracle
 * </pre>
 *
 * Of course you need to set the correct IP address, username and password of your Oracle server.
 * This also assumes that you have downloaded the oracle JDBC driver and locally installed it to maven.
 */
public class TestOracleDistinctJoin extends AbstractPersistenceTestCase {

    private static String projectStr = "project";

    private Log log;

    private boolean skipTest(DBDictionary dict) {
        return !(dict instanceof OracleDictionary);
    }

    public void setUp() throws SQLException {
        OpenJPAEntityManagerFactorySPI emf = createEMF();

        JDBCConfiguration conf = ((JDBCConfiguration) emf.getConfiguration());
        DBDictionary dict = conf.getDBDictionaryInstance();

        if (skipTest(dict)) {
            emf.close();
            return;
        }
        log = emf.getConfiguration().getLog("Tests");

        emf.close();
    }

    public void testJoinOnly() throws SQLException {
        OpenJPAEntityManagerFactorySPI emf =
            createEMF(Course.class, Lecturer.class, SomeEmbeddable.class,
                "openjpa.jdbc.SchemaFactory", "native",
                "openjpa.jdbc.SynchronizeMappings",  "buildSchema(ForeignKeys=true)",
                "openjpa.jdbc.QuerySQLCache", "false",
                "openjpa.DataCache", "false" );

        JDBCConfiguration conf = ((JDBCConfiguration) emf.getConfiguration());
        DBDictionary dict = conf.getDBDictionaryInstance();

        if (skipTest(dict)) {
            emf.close();
            return;
        }

        Long id = null;

        {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tran = em.getTransaction();
            tran.begin();
            em.createQuery("DELETE from Lecturer as l").executeUpdate();
            em.createQuery("DELETE from Course as c").executeUpdate();
            tran.commit();
            em.close();

        }

        {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tran = em.getTransaction();
            tran.begin();

            Course course = new Course();
            SomeEmbeddable emb = new SomeEmbeddable();
            emb.setValA("a");
            emb.setValB("b");
            course.setAnEmbeddable(emb);
            course.setLobColumn("oh this could be a very looooong text...");
            course.setCourseNumber("4711");

            em.persist(course);

            Lecturer l1 = new Lecturer();
            l1.setCourse(course);
            course.addLecturer(l1);

            id = course.getId();
            tran.commit();
            em.close();
        }

        {
            EntityManager em = emf.createEntityManager();
            Course course = em.find(Course.class, id);
            assertNotNull(course);

            em.close();
        }

        {
            log.info("\n\nDistinct and Join"); // this one does sub-selects for LocalizedString and changeLog
            EntityManager em = emf.createEntityManager();
            EntityTransaction tran = em.getTransaction();
            tran.begin();

            Query q = em.createQuery("select distinct c from Course c join  c.lecturers l ");
            List<Course> courses = q.getResultList();
            assertFalse(courses.isEmpty());
            assertNotNull(courses.get(0));

            tran.commit();
            em.close();
        }
        
        {
            log.info("\n\nDistinct"); // creates NO sub-query!
            EntityManager em = emf.createEntityManager();

            Query q = em.createQuery("select distinct c from Course c");
            List<Course> courses = q.getResultList();
            assertFalse(courses.isEmpty());
            assertNotNull(courses.get(0));

            em.close();
        }
        
        {
            log.info("\n\nJoin"); // creates NO sub-query!
            EntityManager em = emf.createEntityManager();

            Query q = em.createQuery("select c from Course c join c.lecturers l ");
            List<Course> courses = q.getResultList();
            assertFalse(courses.isEmpty());
            assertNotNull(courses.get(0));

            em.close();
        }
        
        {
            log.info("\n\nDistinct inverse join"); // this one does sub-selects for LocalizedString and changeLog
            EntityManager em = emf.createEntityManager();

            Query q = em.createQuery("select distinct c from Lecturer l join l.course c");
            List<Course> courses = q.getResultList();
            assertFalse(courses.isEmpty());
            assertNotNull(courses.get(0));

            em.close();
        }
        
        {
            log.info("\n\nInverse join"); // this one does sub-selects for LocalizedString and changeLog
            EntityManager em = emf.createEntityManager();

            Query q = em.createQuery("select c from Lecturer l join l.course c");
            List<Course> courses = q.getResultList();
            assertFalse(courses.isEmpty());
            assertNotNull(courses.get(0));

            em.close();
        }


        emf.close();
    }
}
