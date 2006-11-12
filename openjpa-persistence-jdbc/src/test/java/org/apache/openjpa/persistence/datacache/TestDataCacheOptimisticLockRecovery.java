/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.datacache;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.persistence.LockModeType;

import junit.framework.TestCase;
import java.util.HashMap;
import java.util.Map;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public class TestDataCacheOptimisticLockRecovery
    extends TestCase {

    private EntityManagerFactory emf;
    private int pk;

    public void setUp() {
        Map options = new HashMap();

        // turn on caching
        options.put("openjpa.DataCache", "true");
        options.put("openjpa.RemoteCommitProvider", "sjvm");

        // ensure that OpenJPA knows about our type, so that 
        // auto-schema-creation works
        options.put("openjpa.MetaDataFactory",
            "jpa(Types=" + OptimisticLockInstance.class.getName() + ")");

        emf = Persistence.createEntityManagerFactory("test", options);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("delete from OptimisticLockInstance");

        OptimisticLockInstance oli = new OptimisticLockInstance("foo");
        try {
            em.persist(oli);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }
        pk = oli.getPK();
        em.close();
    }

    public void tearDown() {
        emf.close();
    }

    public void testOptimisticLockRecovery() 
        throws SQLException {

        EntityManager em;
        
        // 1. get the oplock value for the instance after commit and
        // get a read lock to ensure that we check for the optimistic
        // lock column at tx commit.
        em = emf.createEntityManager();
        em.getTransaction().begin();
        OptimisticLockInstance oli = em.find(OptimisticLockInstance.class, pk);
        int firstOpLockValue = oli.getOpLock();
        em.lock(oli, LockModeType.READ);

        // 2. make a change to the instance's optimistic lock column
        // via direct SQL in a separate transaction
        int secondOpLockValue = firstOpLockValue + 1;

        DataSource ds = (DataSource) OpenJPAPersistence.cast(em)
            .getEntityManagerFactory().getConfiguration()
            .getConnectionFactory();
        Connection c = ds.getConnection();
        c.setAutoCommit(false);
        PreparedStatement ps = c.prepareStatement(
            "UPDATE OPTIMISTIC_LOCK_INSTANCE SET OPLOCK = ? WHERE PK = ?");
        ps.setInt(1, secondOpLockValue);
        ps.setInt(2, pk);
        assertEquals(1, ps.executeUpdate());
        c.commit();
        
        // 3. commit the transaction, catching the expected oplock
        // exception
        try {
            em.getTransaction().commit();
            fail("tx should have failed due to out-of-band oplock change");
        } catch (RollbackException re) {
            // expected
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }

        // 4. obtain the object in a new persistence context and
        // assert that the oplock column is set to the one that
        // happened in the out-of-band transaction
        em.close();
        em = emf.createEntityManager();
        oli = em.find(OptimisticLockInstance.class, pk);

        // If this fails, then the data cache has the wrong value.
        // This is what this test case is designed to exercise.
        assertEquals("data cache is not being cleared when oplock "
            + "violations occur", secondOpLockValue, oli.getOpLock());

        // 5. get a read lock on the instance and commit the tx; this
        // time it should go through
        em.getTransaction().begin();
        em.lock(oli, LockModeType.READ);
        try {
            em.getTransaction().commit();
        } catch (RollbackException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }
        em.close();
    }
    
    public void testExpectedOptimisticLockException() {
        EntityManager em;
        
        // 1. start a new tx
        em = emf.createEntityManager();
        em.getTransaction().begin();
        em.lock(em.find(OptimisticLockInstance.class, pk), LockModeType.READ);
        
        // 2. start another tx, and cause a version increment
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        em2.lock(em2.find(OptimisticLockInstance.class, pk), 
            LockModeType.WRITE);
        em2.getTransaction().commit();
        em2.close();
        
        // 3. try to commit. this should fail, as this is a regular optimistic
        // lock failure situation.
        try {
            em.getTransaction().commit();
            fail("write lock in em2 should trigger an optimistic lock failure");
        } catch (RollbackException pe) {
            // expected
        }
        em.close();
    }
}
