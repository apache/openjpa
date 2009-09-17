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
package org.apache.openjpa.persistence.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.conf.Specification;
import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.jdbc.SQLSniffer;
import org.apache.openjpa.persistence.test.AbstractCachedEMFTestCase;

public class TestSpecCompatibilityOptions 
extends AbstractCachedEMFTestCase {
    
    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    /*
     * Verifies compatibility options and spec level are appropriate
     * for a version 2 persistence.xml
     */
    public void testJPA1CompatibilityOptions() {
        OpenJPAEntityManagerFactorySPI emf =
        (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("persistence_1_0",
                "org/apache/openjpa/persistence/compat/" +
                "persistence_1_0.xml");

        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        assertTrue(compat.getFlushBeforeDetach());
        assertTrue(compat.getCopyOnDetach());
        assertTrue(compat.getPrivatePersistentProperties());
        String vMode = emf.getConfiguration().getValidationMode();
        assertEquals("NONE", vMode);
        Specification spec = emf.getConfiguration().getSpecificationInstance();
        assertEquals("JPA", spec.getName().toUpperCase());
        assertEquals(spec.getVersion(), 1);
        
        emf.close();

    }

    /*
     * Verifies compatibility options and spec level are appropriate
     * for a version 2 persistence.xml
     */
    public void testJPA2CompatibilityOptions() {
        OpenJPAEntityManagerFactorySPI emf =
        (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("persistence_2_0",
                "org/apache/openjpa/persistence/compat/" +
                "persistence_2_0.xml");

        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        assertFalse(compat.getFlushBeforeDetach());
        assertFalse(compat.getCopyOnDetach());
        assertFalse(compat.getPrivatePersistentProperties());
        String vMode = emf.getConfiguration().getValidationMode();
        assertEquals("AUTO", vMode);
        Specification spec = emf.getConfiguration().getSpecificationInstance();
        assertEquals("JPA", spec.getName().toUpperCase());
        assertEquals(spec.getVersion(), 2);
        
        emf.close();
    }

    /*
     * Per JPA 2.0, Relationships in mapped superclass must be unidirectional.
     * An exceptioin will be thrown when a bi-directional relation is detected in
     * a mapped superclass. 
     */
    public void testMappedSuperClass() {
        List<Class<?>> types = new ArrayList<Class<?>>();
        types.add(EntityA.class);
        types.add(EntityB.class);
        types.add(MappedSuper.class);
        OpenJPAEntityManagerFactorySPI emf = createEMF2_0(types);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            EntityA a = new EntityA();
            a.setId(1);
            EntityB b = new EntityB();
            b.setId(1);
            a.setEntityB(b);
            b.setEntityA(a);
            em.getTransaction().begin();
            em.persist(a);
            em.persist(b);
            em.getTransaction().commit();
            em.close();
            fail("An exceptioin will be thrown for a bi-directional relation declared in mapped superclass");
        } catch (org.apache.openjpa.persistence.ArgumentException e) {
            if (em != null) {
                em.getTransaction().rollback();
                em.close();
            }
        } finally {
            emf.close();
        }
    }

    /*
     * Per JPA 2.0, JoinColumn annotation is allowed on OneToMany relations.
     */
    public void testJoinColumnOnToManyRelation() {
        List<Class<?>> types = new ArrayList<Class<?>>();
        types.add(EntityC.class);
        types.add(Bi_1ToM_FK.class);
        types.add(Uni_1ToM_FK.class);
        types.add(Uni_1ToM_JT.class);

        OpenJPAEntityManagerFactorySPI emf = createEMF2_0(types);
        EntityManager em = emf.createEntityManager();

        try {
            // trigger table creation
            em.getTransaction().begin();
            em.getTransaction().commit();
            em.close();
            emf.close();
            if (!SQLSniffer.matches(sql, "CREATE TABLE JnCol_C", "Bi1MFK_ColA"))
                fail("JoinColumn annotation fails to be with OneToMany relation");
        } catch (Exception e) {
            fail("JoinColumn annotation fails to be with OneToMany relation");
        }
    }

    /*
     * Per JPA 2.0, non-default mapping of uni-directional OneToMany using
     * foreign key strategy is allowed.
     */
    public void testNonDefaultUniOneToManyRelationUsingForeignKey() {
        List<Class<?>> types = new ArrayList<Class<?>>();
        types.add(EntityC.class);
        types.add(Bi_1ToM_FK.class);
        types.add(Uni_1ToM_FK.class);
        types.add(Uni_1ToM_JT.class);
        OpenJPAEntityManagerFactorySPI emf = createEMF2_0(types);
        EntityManager em = emf.createEntityManager();
        
        try {
            // trigger table creation
            Uni_1ToM_FK uni1mfk = new Uni_1ToM_FK();
            uni1mfk.setName("test");
            EntityC c = new EntityC();
            c.setName("c");
            List cs = new ArrayList();
            cs.add(c);
            uni1mfk.setEntityAs(cs);
            em.persist(uni1mfk);
            em.persist(c);
            em.getTransaction().begin();
            em.getTransaction().commit();
            em.close();
            emf.close();
            if (!SQLSniffer.matches(sql, "CREATE TABLE JnCol_C", "Uni1MFK_ColA"))
                fail("JoinColumn annotation fails to be with OneToMany relation");
        } catch (Exception e) {
            fail("Non-default uni-directional OneToMany Using foreign key fails");
        }
    }

    private OpenJPAEntityManagerFactorySPI createEMF2_0(List<Class<?>> types) {
        Map<Object,Object> map = new HashMap<Object,Object>();
        map.put("openjpa.jdbc.JDBCListeners", 
                new JDBCListener[] { 
                    this.new Listener() 
                });
        map.put("openjpa.jdbc.SynchronizeMappings", 
            "buildSchema(ForeignKeys=true,SchemaAction='drop,add')");

        StringBuffer buf = new StringBuffer();
        for (Class<?> c : types) {
            if (buf.length() > 0) {
                buf.append(";");
            }
            buf.append(c.getName());
        }
        String oldValue =
            map.containsKey("openjpa.MetaDataFactory") ? "," + map.get("openjpa.MetaDataFactory").toString() : "";
        map.put("openjpa.MetaDataFactory", "jpa(Types=" + buf.toString() + oldValue + ")");
        return (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
                createEntityManagerFactory("persistence_2_0",
                    "org/apache/openjpa/persistence/compat/" +
                    "persistence_2_0.xml", map);        
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
