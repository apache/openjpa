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
package org.apache.openjpa.persistence.jdbc.maps.spec_10_1_27_ex3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestSpec10_1_27_Ex3 extends SingleEMFTestCase {
    public int numCompany = 2;
    public int numDivisionsPerCo = 2;
    public List<String> namedQueries = new ArrayList<String>();

    public int compId = 1;
    public int divId = 1;
    public int vpId = 1;
    public int newDivId = 100;
    public int newVpId = 100;

    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    public void setUp() {
        super.setUp(CLEAR_TABLES,
            Company.class,
            Division.class,
            VicePresident.class,
            "openjpa.jdbc.JDBCListeners", 
            new JDBCListener[] { this.new Listener() });
        createObj(emf);
    }

    public void testQueryObj() throws Exception {
        queryObj(emf);
    }

    public void testQueryQualifiedId() throws Exception {
        EntityManager em = emf.createEntityManager();

        String query = "select KEY(e) from Company c, " +
            " in (c.orgs) e order by c.id";
        List rs = em.createQuery(query).getResultList();
        Division d = (Division) rs.get(0);

        em.clear();
        String query4 = "select ENTRY(e) from Company c, " +
            " in (c.orgs) e order by c.id";
        List rs4 = em.createQuery(query4).getResultList();
        Map.Entry me = (Map.Entry) rs4.get(0);

        assertTrue(d.equals(me.getKey()));

        em.clear();
        query = "select KEY(e) from Company c " +
            " left join c.orgs e order by c.id";
        rs = em.createQuery(query).getResultList();
        d = (Division) rs.get(0);

        em.clear();
        query4 = "select ENTRY(e) from Company c " +
            " left join c.orgs e order by c.id";
        rs4 = em.createQuery(query4).getResultList();
        me = (Map.Entry) rs4.get(0);

        assertTrue(d.equals(me.getKey()));

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
        for (int i = 0; i < numCompany; i++)
            createCompany(em, compId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createCompany(EntityManager em, int id) {
        Company c = new Company();
        c.setId(id);
        for (int i = 0; i < numDivisionsPerCo; i++) {
            Division d = createDivision(em, divId++);
            VicePresident vp = createVicePresident(em, vpId++);
            c.addToOrganization(d, vp);
            vp.setCompany(c);
            em.persist(d);
            em.persist(vp);
        }
        em.persist(c);
    }

    public Division createDivision(EntityManager em, int id) {
        Division d = new Division();
        d.setId(id);
        d.setName("d" + id);
        return d;
    }

    public VicePresident createVicePresident(EntityManager em, int id) {
        VicePresident vp = new VicePresident();
        vp.setId(id);
        vp.setName("vp" + id);
        return vp;
    }

    public void findObj(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        Company c = em.find(Company.class, 1);
        assertCompany(c);

        Division d = em.find(Division.class, 1);
        assertDivision(d);

        VicePresident vp = em.find(VicePresident.class, 1);
        assertVicePresident(vp);

        updateObj(em, c);
        em.close();

        em = emf.createEntityManager();
        c = em.find(Company.class, 1);
        assertCompany(c);
        deleteObj(em, c);
        em.close();
    }

    public void updateObj(EntityManager em, Company c) {
        EntityTransaction tran = em.getTransaction();
        // remove an element
        tran.begin();
        Map orgs = c.getOrganization();
        Set keys = orgs.keySet();
        for (Object key : keys) {
            Division d = (Division) key;
            VicePresident vp = c.getOrganization(d);
            vp.setCompany(null);
            em.persist(vp);
            c.removeFromOrganization(d);
            break;
        }
        em.persist(c);
        em.flush();
        tran.commit();

        // add an element
        tran.begin();
        Division d = createDivision(em, newDivId++);
        VicePresident vp = createVicePresident(em, newVpId++);
        c.addToOrganization(d, vp);
        vp.setCompany(c);
        em.persist(d);
        em.persist(vp);
        em.persist(c);
        em.flush();
        tran.commit();

        // modify an element
        tran.begin();
        orgs = c.getOrganization();
        vp = c.getOrganization(d);
        vp.setName("newNameAgain");
        em.persist(c);
        em.persist(vp);
        em.flush();
        tran.commit();
    }       

    public void deleteObj(EntityManager em, Company c) {
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        em.remove(c);
        tran.commit();
    }

    public void assertCompany(Company c) {
        int id = c.getId();
        Map organization = c.getOrganization();
        Assert.assertEquals(2,organization.size());
    }

    public void assertDivision(Division d) {
        int id = d.getId();
        String name = d.getName();
    }

    public void assertVicePresident(VicePresident vp) {
        int id = vp.getId();
        String name = vp.getName();
    }


    public void queryObj(EntityManagerFactory emf) {
        queryCompany(emf);
        queryDivision(emf);
        queryVicePresident(emf);
    }

    public void queryCompany(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select c from Company c");
        List<Company> cs = q.getResultList();
        for (Company c : cs){
            assertCompany(c);
        }
        tran.commit();
        em.close();
    }

    public void queryDivision(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Division d");
        List<Division> ds = q.getResultList();
        for (Division d : ds){
            assertDivision(d);
        }
        tran.commit();
        em.close();
    }

    public void queryVicePresident(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select vp from VicePresident vp");
        List<VicePresident> vps = q.getResultList();
        for (VicePresident vp : vps){
            assertVicePresident(vp);
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
