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
import java.util.Arrays;
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

    /**
     * Per JPA 2.0, the following one-to-many mappings are supported.
     * (1) uni-/OneToMany/foreign key strategy
     * (2) uni-/OneToMany/join table strategy (default)
     * (3) bi-/OneToMany/foreign key strategy (default)
     * (4) bi-/OneToMany/join table strategy
     * The JoinColumn and JoinTable annotations or corresponding XML 
     * elements must be used to specify such non-default mappings
     * 
     * For (1), the spec provides the following example (Sec 11.1.36):
     * Example 3: Unidirectional One-to-Many association using a foreign 
     * key mapping:
     * In Customer class:
     * @OneToMany(orphanRemoval=true)
     * @JoinColumn(name="CUST_ID") // join column is in table for Order
     * public Set<Order> getOrders() {return orders;}
     * 
     * For (4), Bi-directional One-t-Many association using the join 
     * table mapping:
     * In Customer class:
     * @OneToMany(mappedBy="customer")
     * @JoinTable(
     *   name="Customer_Orders",
     *   joinColumns=
     *     @JoinColumn(name="Order_ID", referencedColumnName="ID"),
     *    inverseJoinColumns=
     *     @JoinColumn(name="Cust_ID", referencedColumnName="ID")
     *  )
     *  public Set<Order> getOrders() {return orders;}
     *  
     *  Note that in this scenario, @JoinTable is required. Simply applying @JoinColumn 
     *  without @JoinTable will result in an exception thrown by openjpa.
     * 
     */
    public void testOneToManyRelation() {
        List<Class<?>> types = new ArrayList<Class<?>>();
        types.add(EntityC.class);
        types.add(EntityC_B1MFK.class);
        types.add(EntityC_B1MJT.class);
        types.add(EntityC_U1MFK.class);
        types.add(Bi_1ToM_FK.class);
        types.add(Bi_1ToM_JT.class);
        types.add(Uni_1ToM_FK.class);
        types.add(Uni_1ToM_JT.class);
        OpenJPAEntityManagerFactorySPI emf = createEMF2_0(types);
        EntityManager em = emf.createEntityManager();
        
        try {
            // trigger table creation
            em.getTransaction().begin();
            em.getTransaction().commit();
            assertSQLFragnments(sql, "CREATE TABLE Bi1MJT_C", "C_ID", "Bi1MJT_ID");
            assertSQLFragnments(sql, "CREATE TABLE EntityC_B1MFK", "BI1MFK_ID");
            assertSQLFragnments(sql, "CREATE TABLE Uni1MJT_C", "Uni1MJT_ID", "C_ID");
            assertSQLFragnments(sql, "CREATE TABLE EntityC_B1MFK", "BI1MFK_ID");
            assertSQLFragnments(sql, "CREATE TABLE EntityC_U1MFK", "Uni1MFK_ID");
            crudUni1MFK(em);
            crudUni1MJT(em);
            crudBi1MFK(em);
            crudBi1MJT(em);
        } catch (Exception e) {
            e.printStackTrace();
            fail("OneToMany mapping failed with exception message: " + e.getMessage());
        } finally {
            em.close();
            emf.close();            
        }
    }
    
    // non default
    public void crudUni1MFK(EntityManager em) {
        //create
        Uni_1ToM_FK u = new Uni_1ToM_FK();
        u.setName("uni1mfk");
        List<EntityC_U1MFK> cs = new ArrayList<EntityC_U1MFK>();
        EntityC_U1MFK c = new EntityC_U1MFK();
        c.setName("c");
        cs.add(c);
        u.setEntityCs(cs);
        em.persist(u);
        em.persist(c);
        em.getTransaction().begin();
        em.getTransaction().commit();

        //update
        em.getTransaction().begin();
        cs = u.getEntityCs();
        u.setName("newName");
        EntityC_U1MFK c1 = new EntityC_U1MFK();
        c1.setName("c1");
        cs.add(c1);
        em.persist(c1);
        em.getTransaction().commit();
        
        // update by removing a c and then add this c to a new u
        em.getTransaction().begin();
        EntityC_U1MFK c2 = cs.remove(0);
        
        Uni_1ToM_FK u2 = new Uni_1ToM_FK();
        u2.setName("uni1mfk2");
        List<EntityC_U1MFK> cs2 = new ArrayList<EntityC_U1MFK>();
        cs2.add(c2);
        u2.setEntityCs(cs2);
        em.persist(u2);
        em.getTransaction().commit();
        em.clear();
        
        //query
        Query q = em.createQuery("SELECT u FROM Uni_1ToM_FK u where u.name = 'newName'");
        Uni_1ToM_FK u1 = (Uni_1ToM_FK)q.getSingleResult();
        assertEquals(u, u1);
        em.clear();

        //find
        long id = u1.getId();
        Uni_1ToM_FK findU1 = em.find(Uni_1ToM_FK.class, id);
        assertEquals(findU1, u1);
        
        //remove
        em.getTransaction().begin();
        em.remove(findU1);
        em.getTransaction().commit();
        em.clear();
    }
    
    // default
    public void crudUni1MJT(EntityManager em) {
        Uni_1ToM_JT u = new Uni_1ToM_JT();
        u.setName("uni1mjt");
        List<EntityC> cs = new ArrayList<EntityC>();
        EntityC c = new EntityC();
        c.setName("c");
        cs.add(c);
        u.setEntityCs(cs);
        em.persist(u);
        em.persist(c);
        em.getTransaction().begin();
        em.getTransaction().commit();
        
        //update
        em.getTransaction().begin();
        cs = u.getEntityCs();
        u.setName("newName");
        EntityC c1 = new EntityC();
        c1.setName("c1");
        cs.add(c1);
        em.persist(c1);
        em.getTransaction().commit();
        em.clear();
        
        //query
        Query q = em.createQuery("SELECT u FROM Uni_1ToM_JT u");
        Uni_1ToM_JT u1 = (Uni_1ToM_JT)q.getSingleResult();
        assertEquals(u, u1);
        em.clear();

        //find
        long id = u1.getId();
        Uni_1ToM_JT u2 = em.find(Uni_1ToM_JT.class, id);
        assertEquals(u, u2);
        
        //remove
        em.getTransaction().begin();
        em.remove(u2);
        em.getTransaction().commit();
        em.clear();
    }
    
    //default
    public void crudBi1MFK(EntityManager em) {
        Bi_1ToM_FK b = new Bi_1ToM_FK();
        b.setName("bi1mfk");
        List<EntityC_B1MFK> cs = new ArrayList<EntityC_B1MFK>();
        EntityC_B1MFK c = new EntityC_B1MFK();
        c.setName("c");
        c.setBi1mfk(b);
        cs.add(c);
        b.setEntityCs(cs);
        em.persist(b);
        em.persist(c);
        em.getTransaction().begin();
        em.getTransaction().commit();
        
        //update
        em.getTransaction().begin();
        cs = b.getEntityCs();
        b.setName("newName");
        EntityC_B1MFK c1 = new EntityC_B1MFK();
        c1.setName("c1");
        cs.add(c1);
        c1.setBi1mfk(b);
        em.persist(c1);
        em.getTransaction().commit();
        em.clear();
        
        //query
        Query q = em.createQuery("SELECT u FROM Bi_1ToM_FK u");
        Bi_1ToM_FK b1 = (Bi_1ToM_FK)q.getSingleResult();
        assertEquals(b, b1);
        em.clear();

        //find
        long id = b1.getId();
        Bi_1ToM_FK b2 = em.find(Bi_1ToM_FK.class, id);
        assertEquals(b, b2);
        
        //remove
        em.getTransaction().begin();
        em.remove(b2);
        em.getTransaction().commit();
        em.clear();
    }

    // non default
    public void crudBi1MJT(EntityManager em) {
        Bi_1ToM_JT b = new Bi_1ToM_JT();
        b.setName("bi1mfk");
        List<EntityC_B1MJT> cs = new ArrayList<EntityC_B1MJT>();
        EntityC_B1MJT c = new EntityC_B1MJT();
        c.setName("c");
        c.setBi1mjt(b);
        cs.add(c);
        b.setEntityCs(cs);
        em.persist(b);
        em.persist(c);
        em.getTransaction().begin();
        em.getTransaction().commit();

        //update
        em.getTransaction().begin();
        cs = b.getEntityCs();
        b.setName("newName");
        EntityC_B1MJT c1 = new EntityC_B1MJT();
        c1.setName("c1");
        cs.add(c1);
        c1.setBi1mjt(b);
        em.persist(c1);
        em.getTransaction().commit();
        em.clear();
        
        //query
        Query q = em.createQuery("SELECT u FROM Bi_1ToM_JT u");
        Bi_1ToM_JT b1 = (Bi_1ToM_JT)q.getSingleResult();
        assertEquals(b, b1);
        em.clear();

        //find
        long id = b1.getId();
        Bi_1ToM_JT b2 = em.find(Bi_1ToM_JT.class, id);
        assertEquals(b, b2);
        
        //remove
        em.getTransaction().begin();
        em.remove(b2);
        em.getTransaction().commit();
        em.clear();
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
    
    void assertSQLFragnments(List<String> list, String... keys) {
        if (SQLSniffer.matches(list, keys))
            return;
        fail("None of the following " + sql.size() + " SQL \r\n" + 
                toString(sql) + "\r\n contains all keys \r\n"
                + toString(Arrays.asList(keys)));
    }

    public String toString(List<String> list) {
        StringBuffer buf = new StringBuffer();
        for (String s : list)
            buf.append(s).append("\r\n");
        return buf.toString();
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
