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
package org.apache.openjpa.persistence.kernel;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.RestoreStateType;
import org.apache.openjpa.persistence.kernel.common.apps.ModRuntimeTest1;
import org.apache.openjpa.persistence.kernel.common.apps.ModRuntimeTest2;
import org.apache.openjpa.persistence.kernel.common.apps.PersistenceAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestTransition2 extends BaseKernelTest {

    private boolean supportsPessimistic = true;

    private int _id = 0;

    @BeforeEach
    void setUp() throws Exception {
        super.setUp(ModRuntimeTest1.class, ModRuntimeTest2.class, PersistenceAware.class);
        try {
            OpenJPAEntityManager pm = getPM(false, false);
            supportsPessimistic = !pm.getOptimistic();
            pm.close();
        }
        catch (Exception e) {
            supportsPessimistic = false;
        }

        OpenJPAEntityManager pm = getPM();
        startTx(pm);

        ModRuntimeTest2 b = createTest2();
        pm.persist(b);
        _id = b.getId();

        endTx(pm);
        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions from transient to
     * persistent-transactional and back to transient after rollback.
     */
    @Test
    void flagTransitions1()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();
        assertTransient(b);
        assertTransient(b.getSelfOneOne());

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);
        pm.persist(b);

        assertPersistent(b, true, true, false, true);

        rollbackTx(pm);

        assertTransient(b);
        assertTransient(b.getSelfOneOne());
        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions from transient to
     * persistent-transactional to persistent-nontransactional after commit.
     */
    @Test
    void flagTransitions2()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();
        assertTransient(b);
        assertTransient(b.getSelfOneOne());

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);
        pm.persist(b);

        assertPersistent(b, true, true, false, true);

        endTx(pm);

        assertPersistent(b, false, false, false, false);
        assertPersistent(b.getSelfOneOne(), false, false, false, false);
        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions when finding a transactional instance by
     * id, then committing.
     */
    @Test
    void flagTransitions3() throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertPersistent(b, true, false, false, false);
        assertPersistent(b.getSelfOneOne(), true, false, false, false);

        endTx(pm);

        assertPersistent(b, false, false, false, false);
        assertPersistent(b.getSelfOneOne(), false, false, false, false);
        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions when finding a transactional
     * instance by id, then rolling back.
     */
    @Test
    void flagTransitions4()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertPersistent(b, true, false, false, false);
        assertPersistent(b.getSelfOneOne(), true, false, false, false);

        rollbackTx(pm);
        //pm.getTransaction().rollback();

        assertPersistent(b, false, false, false, false);
        assertPersistent(b.getSelfOneOne(), false, false, false, false);
        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions when finding a non-transactional
     * instance by id.
     */
    @Test
    void flagTransitions5()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);

        assertPersistent(b, false, false, false, false);
        assertPersistent(b.getSelfOneOne(), false, false, false, false);

        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions from persistent-transactional
     * to transient after delete and commit.
     */
    @Test
    void flagTransitions6()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        ModRuntimeTest1 parent = b.getSelfOneOne();
        pm.remove(b);
        assertPersistent(b, true, false, true, true);
        assertPersistent(parent, true, false, false, false);

        endTx(pm);

        assertTransient(b);
        assertPersistent(parent, false, false, false, false);
        endEm(pm);
    }

    /**
     * Tests basic jdo flag transitions from persistent-transactional
     * to persistent-nontransactional after delete and rollback.
     */
    @Test
    void flagTransitions7()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        ModRuntimeTest1 parent = b.getSelfOneOne();
        pm.remove(b);
        assertPersistent(b, true, false, true, true);
        assertPersistent(parent, true, false, false, false);

        rollbackTx(pm);

        assertPersistent(b, false, false, false, false);
        assertPersistent(parent, false, false, false, false);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional and back to transient after rollback.
     */
    @Test
    void stateTransitions1()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        pm.persist(b);
        pm.persist(b.getSelfOneOne());
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional to persistent-nontransactional after commit.
     */
    @Test
    void stateTransitions2()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        pm.persist(b);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, false);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then committing.
     */
    @Test
    void stateTransitions3()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, false);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then committing.
     */
    @Test
    void stateTransitions3a()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, false);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then rolling back.
     */
    @Test
    void stateTransitions4()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then rolling back.
     */
    @Test
    void stateTransitions4a()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to transient after delete and commit.
     */
    @Test
    void stateTransitions5()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        ModRuntimeTest1 parent = b.getSelfOneOne();
        assertNotNull(parent, "parent is null");
        pm.remove(b);

        endTx(pm);

        // parent should be valid
        assertEquals("PARENT", parent.getStringField());

        // 'b' should be cleared
        assertNull(b.getStringField());
        assertEquals(0, b.getIntField());
        assertNull(b.getSelfOneOne());
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to persistent-nontransactional after delete and rollback.
     */
    @Test
    void stateTransitions6()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        pm.remove(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional and back to transient after rollback.
     */
    @Test
    void optStateTransitions1()
        throws Exception {
        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        pm.persist(b);
        pm.persist(b.getSelfOneOne());
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional to persistent-nontransactional after commit.
     */
    @Test
    void optStateTransitions2()
        throws Exception {
        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        pm.persist(b);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, false);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then committing.
     */
    @Test
    void optStateTransitions3()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, false);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then rolling back.
     */
    @Test
    void optStateTransitions4()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to transient after delete and commit.
     */
    @Test
    void optStateTransitions5()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        ModRuntimeTest1 parent = b.getSelfOneOne();
        assertNotNull(parent, "parent is null");
        pm.remove(b);

        endTx(pm);

        // parent should be valid
        assertEquals("PARENT", parent.getStringField());

        // 'b' should not be cleared JPA 2.0 "3.2.3 Removal"
        assertNotNull(b.getStringField());
        assertNotEquals(0, b.getIntField());
        assertNotNull(b.getSelfOneOne());
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to persistent-nontransactional after delete and rollback.
     */
    @Test
    void optStateTransitions6()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        pm.remove(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional and back to transient after rollback.
     */
    @Test
    void optRetainStateTransitions1()
        throws Exception {
        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(true, true);
        startTx(pm);

        pm.persist(b);
        pm.persist(b.getSelfOneOne());
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional to persistent-nontransactional after commit.
     */
    @Test
    void optRetainStateTransitions2()
        throws Exception {
        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(true, true);
        startTx(pm);

        pm.persist(b);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, true);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then committing.
     */
    @Test
    void optRetainStateTransitions3()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, true);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then rolling back.
     */
    @Test
    void optRetainStateTransitions4()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to transient after delete and commit.
     */
    @Test
    void optRetainStateTransitions5()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        ModRuntimeTest1 parent = b.getSelfOneOne();

        assertNotNull(parent, "parent is null");
        pm.remove(b);

        endTx(pm);

        // parent should be valid
        assertEquals("PARENT", parent.getStringField());

        // 'b' should not be cleared JPA 2.0 "3.2.3 Removal"
        assertNotNull(b.getStringField());
        assertNotEquals(0, b.getIntField());
        assertNotNull(b.getSelfOneOne());
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to persistent-nontransactional after delete and rollback.
     */
    @Test
    void optRetainStateTransitions6()
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        pm.remove(b);

        rollbackTx(pm);

        if (pm.getRestoreState() != RestoreStateType.NONE)
            assertTest2Orig(b);
        else
            assertNull(b.getStringField());
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional and back to transient after rollback.
     */
    @Test
    void retainStateTransitions1()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, true);
        startTx(pm);

        pm.persist(b);
        pm.persist(b.getSelfOneOne());
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * persistent-transactional to persistent-nontransactional after commit.
     */
    @Test
    void retainStateTransitions2()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, true);
        startTx(pm);

        pm.persist(b);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, true);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then committing.
     */
    @Test
    void retainStateTransitions3()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, true);
        endEm(pm);
    }

    /**
     * Tests basic state transitions when finding a transactional
     * instance by id, then rolling back.
     */
    @Test
    void retainStateTransitions4()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        assertTest2Orig(b);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to transient after delete and commit.
     */
    @Test
    void retainStateTransitions5() throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        ModRuntimeTest1 parent = b.getSelfOneOne();
        assertNotNull(parent, "parent is null");
        pm.remove(b);

        endTx(pm);

        // parent should be valid
        assertEquals("PARENT", parent.getStringField());

        // 'b' should be cleared
        assertNull(b.getStringField());
        assertEquals(0, b.getIntField());
        assertNull(b.getSelfOneOne());
    }

    /**
     * Tests basic state transitions from persistent-transactional
     * to persistent-nontransactional after delete and rollback.
     */
    @Test
    void retainStateTransitions6()
        throws Exception {
        if (!supportsPessimistic)
            return;

        OpenJPAEntityManager pm = getPM(false, true);
        startTx(pm);

        ModRuntimeTest2 b = pm.find(ModRuntimeTest2.class, _id);
        pm.remove(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * transient-dirty and back to transient after rollback.
     */
    @Test
    void transientStateTransitions1()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        pm.transactional(b, true);
        pm.transactional(b.getSelfOneOne(), true);
        changeTest2(b);

        rollbackTx(pm);

        assertTest2Orig(b);
        endEm(pm);
    }

    /**
     * Tests basic state transitions from transient to
     * transient-transactional and stick on commit.
     */
    @Test
    void transientStateTransitions2()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);

        pm.transactional(b, true);
        pm.transactional(b.getSelfOneOne(), true);
        changeTest2(b);

        endTx(pm);

        assertTest2Changed(b, false);
        endEm(pm);
    }

    /**
     * Tests state transitions from PClean to transient.
     */
    @Test
    void transientStateTransitions3()
        throws Exception {
        if (!supportsPessimistic)
            return;

        ModRuntimeTest2 b = createTest2();

        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);
        pm.persist(b);
        int oid = b.getId();
        endTx(pm);
        endEm(pm);

        pm = getPM(false, false);
        //FIXME jthomas
        b = pm.find(ModRuntimeTest2.class, oid);
        pm.retrieve(b);
        //FIXME jthomas
        endEm(pm);

        // note that at this point, parent is not transient, just retrieved.
        assertNotNull(b.getSelfOneOne(), "b is null");
    }

    private ModRuntimeTest2 createTest2() {
        return createTest2("NAME", 50);
    }

    private ModRuntimeTest2 createTest2(String str, int i) {
        return createTest2(new ModRuntimeTest2(str, i));
    }

    private ModRuntimeTest2 createTest2(ModRuntimeTest2 b) {
        ModRuntimeTest1 parent = new ModRuntimeTest1("PARENT", 70);
        b.setSelfOneOne(parent);
        return b;
    }

    private void changeTest2(ModRuntimeTest2 b) {
        PersistenceAware.setModTransString(b, "999");
        b.setStringField("CHANGED");
        b.setIntField(1000);
        b.getSelfOneOne().setStringField("PCHANGED");
        b.setSelfOneOne(null);
    }

    private void assertTest2Changed(ModRuntimeTest2 b, boolean retainValues) {
        if (retainValues)
            assertEquals("999", PersistenceAware.getModTransString(b));
        assertEquals("CHANGED", b.getStringField());
        assertEquals(1000, b.getIntField());
        assertNull(b.getSelfOneOne());
    }

    private void assertTest2Orig(ModRuntimeTest2 b) {
        assertEquals("NAME", b.getStringField());
        assertEquals(50, b.getIntField());
        assertNotNull(b.getSelfOneOne());
        assertEquals("PARENT", b.getSelfOneOne().getStringField());

        assertNull(PersistenceAware.getModTransString(b),
            "transactional field 'transString' was not the "
                + "same as it was originally. Ensure that "
                + "openjpa.kernel.PersistenceAware is enhanced. "
                +
                "It is persistence-aware, so will not be enhanced by commands "
                + "like jdoc $(find test -name '*.jdo').");
    }

    /**
     * Assert that the given object is persistent.
     */
    public void assertTransient(Object a) {
        OpenJPAEntityManager pm = currentEntityManager();
        assertFalse(pm.isPersistent(a));
        assertFalse(pm.isTransactional(a));
        assertFalse(pm.isNewlyPersistent(a));
        assertFalse(pm.isDirty(a));
        assertFalse(pm.isRemoved(a));
        assertNull(pm.getObjectId(a));
        assertNull(OpenJPAPersistence.getEntityManager(a));
        endEm(pm);
    }

    /**
     * Assert that the given object is persistent and is in the given state.
     */
    public void assertPersistent(Object a, boolean isTrans, boolean isNew,
        boolean isDeleted, boolean isDirty) {
        OpenJPAEntityManager pm = currentEntityManager();
        assertTrue(pm.isPersistent(a));

        assertInstanceOf(PersistenceCapable.class, a);

        assertEquals(isTrans, pm.isTransactional(a));
		assertEquals(isNew, pm.isNewlyPersistent(a));
		assertEquals(isDeleted, pm.isRemoved(a));
		assertEquals(isDirty || isNew || isDeleted, pm.isDirty(a));
		assertNotNull(pm.getObjectId(a));
		assertNotNull(OpenJPAPersistence.getEntityManager(a));

		endEm(pm);
	}

}
