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
package org.apache.openjpa.persistence.enhance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.apache.openjpa.persistence.enhance.common.apps.
        BackingFieldNameMismatchInstance;
import org.apache.openjpa.persistence.enhance.common.apps.BaseEntity;
import org.apache.openjpa.persistence.enhance.common.apps.BasicSubclassInstance;
import org.apache.openjpa.persistence.enhance.common.apps.DerivedEntity;
import org.apache.openjpa.persistence.enhance.common.apps.Entity1;
import org.apache.openjpa.persistence.enhance.common.apps.
        ManagedInverseTestInstance;
import org.apache.openjpa.persistence.enhance.common.apps.
        ManagedInverseTestInstance2;
import org.apache.openjpa.persistence.enhance.common.apps.SubclassTestInstance;
import org.apache.openjpa.util.ExceptionInfo;
import org.apache.openjpa.util.ImplHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class TestSubclassedBehavior extends AbstractTestCase {

    public TestSubclassedBehavior() {
        super("enhancecactusapp");
    }


    @BeforeEach
    void setUp() {
        deleteAll(BasicSubclassInstance.class);
        deleteAll(BackingFieldNameMismatchInstance.class);
        deleteAll(BaseEntity.class);
        deleteAll(ManagedInverseTestInstance.class);
        deleteAll(ManagedInverseTestInstance2.class);
    }

    @Test
    void inheritance() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        DerivedEntity de = (DerivedEntity) newInstance(pm, DerivedEntity.class);
        BasicSubclassInstance basic = (BasicSubclassInstance) newInstance(pm,
            BasicSubclassInstance.class);
        basic.setStringField("basic one-to-one");
        de.setOneToOne(basic);
        Object oid = persistenceOperations(pm, de, false);

        // ##### need a test case for JDOHelper.createEntityManager() for
        // subclass
        ClassMetaData meta = JPAFacadeHelper.getMetaData(pm, de.getClass());
        assertEquals(BaseEntity.class, meta.getPCSuperclass());

        pm = (OpenJPAEntityManager) currentEntityManager();

        Object o = pm.find(DerivedEntity.class, oid);
        assertInstanceOf(DerivedEntity.class, o);
        de = (DerivedEntity) o;
        Broker b = JPAFacadeHelper.toBroker(pm);
        OpenJPAStateManager sm = b.getStateManager(de);
        // we use getLoaded() here because isLoaded() always returns true.
        assertFalse(sm.getLoaded().get(
            sm.getMetaData().getField("oneToOne").getIndex()));
        assertEquals("basic one-to-one", de.getOneToOne().getStringField());
        assertTrue(sm.getLoaded().get(sm.getMetaData()
            .getField("oneToOne").getIndex()));

        startTx(pm);
        pm.remove(de);
        endTx(pm);
        endEm(pm);
    }

    @Test
    void basicSubclassPersistenceOperations()
        throws Exception {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BasicSubclassInstance o = (BasicSubclassInstance) newInstance(pm,
            BasicSubclassInstance.class);
        persistenceOperations(pm, o, true);
    }

    @Test
    void backingFieldNameMismatch() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BackingFieldNameMismatchInstance o = (BackingFieldNameMismatchInstance)
            newInstance(pm, BackingFieldNameMismatchInstance.class);
        persistenceOperations(pm, o, true);
    }

    private Object newInstance(OpenJPAEntityManager pm, Class cls) {
        return pm.createInstance(cls);
    }

    private Object createInstance(EntityManager em, Class cls) {
        return ((OpenJPAEntityManager) em).createInstance(cls);
    }

    private Object persistenceOperations(OpenJPAEntityManager pm,
        SubclassTestInstance o, boolean delete) {
        startTx(pm);
        pm.persist(o);
        o.setStringField("new persistent instance");
        endTx(pm);
        Object oid = pm.getObjectId(o);
        endEm(pm);

        pm = (OpenJPAEntityManager) currentEntityManager();
        o = (SubclassTestInstance) pm.find(SubclassTestInstance.class, oid);

        assertEquals("new persistent instance", o.getStringField());
        startTx(pm);
        o.setStringField("modified persistent instance");
        endTx(pm);
        endEm(pm);

        if (delete) {
            pm = (OpenJPAEntityManager) currentEntityManager();
            o = (SubclassTestInstance) pm.find(SubclassTestInstance.class, oid);
            assertEquals("modified persistent instance", o.getStringField());
            startTx(pm);
            pm.remove(o);
            endTx(pm);
            endEm(pm);
            return null;
        } else {
            return oid;
        }
    }

    @Test
    void polymorphicQueries() {
        deleteAll(BaseEntity.class);
        deleteAll(BasicSubclassInstance.class);

        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        startTx(pm);

        BaseEntity be = (BaseEntity) newInstance(pm, BaseEntity.class);
        be.setShortField((short) 0);
        pm.persist(be);

        be = (BaseEntity) newInstance(pm, BaseEntity.class);
        be.setShortField((short) 1);
        pm.persist(be);

        DerivedEntity de = (DerivedEntity) newInstance(pm, DerivedEntity.class);
        de.setShortField((short) 2);
        de.setOneToOne((BasicSubclassInstance) newInstance(pm,
            BasicSubclassInstance.class));
        pm.persist(de);

        de = (DerivedEntity) newInstance(pm, DerivedEntity.class);
        de.setShortField((short) 3);
        de.setOneToOne((BasicSubclassInstance) newInstance(pm,
            BasicSubclassInstance.class));
        pm.persist(de);

        endTx(pm);
        endEm(pm);

        pm = (OpenJPAEntityManager) currentEntityManager();
        OpenJPAQuery q =
            pm.createQuery("SELECT a FROM BaseEntity a "
                + "ORDER BY a.shortField ASC");
        List l = (List) q.getResultList();
        assertEquals(4, l.size());
        assertEquals(0, ((BaseEntity) l.get(0)).getShortField());
        assertEquals(1, ((BaseEntity) l.get(1)).getShortField());
        assertEquals(2, ((BaseEntity) l.get(2)).getShortField());
        assertEquals(3, ((BaseEntity) l.get(3)).getShortField());
        assertInstanceOf(DerivedEntity.class, l.get(2));
        assertInstanceOf(DerivedEntity.class, l.get(3));
        endEm(pm);
    }

    @Test
    void enhancedClassChangesOutsideTxWithoutNTW() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        startTx(pm);
        deleteAll(Entity1.class);
        endTx(pm);
        Entity1 o = new Entity1(8, "pk 8", 4);
        startTx(pm);
        pm.persist(o);
        endTx(pm);
        Object oid = pm.getObjectId(o);
        endEm(pm);

        pm = (OpenJPAEntityManager) currentEntityManager();
        o = (Entity1) pm.find(Entity1.class, oid);

        try {
            o.setStringField("hello");
            fail("non-transactional write should not be allowed");
        } catch (Exception e) {
            // expected
        } finally {
            endEm(pm);
        }
    }

    @Test
    void subclassChangesOutsideTxWithoutNTW() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BasicSubclassInstance basic = (BasicSubclassInstance) newInstance(pm,
            BasicSubclassInstance.class);
        basic.setStringField("foo");
        startTx(pm);
        pm.persist(basic);
        endTx(pm);
        Object oid = pm.getObjectId(basic);
        endEm(pm);

        pm = (OpenJPAEntityManager) currentEntityManager();
        basic =
            (BasicSubclassInstance) pm.find(BasicSubclassInstance.class, oid);

        try {
            basic.setStringField("hello");
            fail("non-transactional write should not be allowed");
        } catch (Exception e) {
            // expected
        } finally {
            endEm(pm);
        }
    }

    @Test
    void basicPMUses() {
        // retain so we don't reload in the reads after the tx commit
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        Broker broker = JPAFacadeHelper.toBroker(pm);
        startTx(pm);

        // register a new instance with the PM
        BasicSubclassInstance basic = (BasicSubclassInstance) newInstance
            (pm, BasicSubclassInstance.class);
        assertTrue(ImplHelper.isManageable(basic));
        basic.setStringField("foo");
        pm.persist(basic);
        assertTrue(broker.isNew(basic));
        assertTrue(broker.isPersistent(basic));

        // commit. this should cause the data to be written.
        // ### should check SQL count
        endTx(pm);

        assertFalse(broker.isNew(basic));

        OpenJPAStateManager sm = broker.getStateManager(basic);
        assertNotNull(sm);
        assertEquals(sm.getManagedInstance(), basic);

        FieldMetaData fmd = sm.getMetaData().getField("stringField");
        assertEquals("foo", sm.fetch(fmd.getIndex()));
        assertTrue(sm.getLoaded().get(fmd.getIndex()));

        pm.evict(basic);
        assertFalse(sm.getLoaded().get(fmd.getIndex()));
        // lazy loading
        assertNotNull(basic.getStringField());
        assertEquals("foo", sm.fetch(fmd.getIndex()));
        assertEquals("foo", basic.getStringField());
        assertTrue(sm.getLoaded().get(fmd.getIndex()));

        startTx(pm);
        basic.setStringField("bar");
        assertTrue(broker.isDirty(basic));
        endTx(pm);
        Object oid = broker.getObjectId(basic);
        assertNotNull(oid);
        endEm(pm);

        pm = (OpenJPAEntityManager) currentEntityManager();
        basic =
            (BasicSubclassInstance) pm.find(BasicSubclassInstance.class, oid);
        assertEquals("bar", basic.getStringField());

        startTx(pm);
        pm.remove(basic);
        assertTrue(JPAFacadeHelper.toBroker(pm).isDeleted(basic));
        endTx(pm);
        endEm(pm);
    }

    @Test
    void getObjectId() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BasicSubclassInstance basic = new BasicSubclassInstance();
        basic.setStringField("foo");
        startTx(pm);
        pm.persist(basic);
        endTx(pm);
        Object oid = null;
        try {
            assertNotNull(oid = pm.getObjectId(basic));
        } catch (Exception e) {
            fail("object id lookup failed: " + e.getMessage());
        }

        startTx(pm);
        pm.remove(basic);
        // before committing, id should exist still
        assertNotNull(pm.getObjectId(basic));

        endTx(pm);
        assertNull(pm.getObjectId(basic));
        endEm(pm);

        // looking up the instance by id in a new PM should fail.
        pm = (OpenJPAEntityManager) currentEntityManager();
        try {
            pm.find(BasicSubclassInstance.class, oid);
            fail("instance should have been deleted!");
        } catch (Exception e) {
            // expected
        }
        endEm(pm);
    }

    @Test
    void changesOutsideTxWithNTW() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BasicSubclassInstance basic = new BasicSubclassInstance();
        basic.setStringField("foo");
        startTx(pm);
        pm.persist(basic);
        endTx(pm);
        Object oid = pm.getObjectId(basic);
        endEm(pm);

        pm = getNTWPM();
        basic =
            (BasicSubclassInstance) pm.find(BasicSubclassInstance.class, oid);
        basic.setStringField("hello");
        startTx(pm);
        endTx(pm);
        endEm(pm);

        pm = (OpenJPAEntityManager) currentEntityManager();
        basic =
            (BasicSubclassInstance) pm.find(BasicSubclassInstance.class, oid);
        try {
            assertEquals("hello", basic.getStringField());
        } catch (AssertionFailedError afe) {
            bug(1205, afe, "JDO 2-style NTW not supported.");
        }
    }

    @Test
    void changesOutsideTxWithoutNTW() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BasicSubclassInstance basic = new BasicSubclassInstance();
        basic.setStringField("foo");
        startTx(pm);
        pm.persist(basic);
        endTx(pm);

        try {
            basic.setStringField("hello");
            fail("should not be able to write outside tx without NTW");
        } catch (RuntimeException re) {
            // expected case
            Object failed = ((ExceptionInfo) re).getFailedObject();
            assertNotNull(failed);
            assertSame(basic, failed);
        } finally {
            endEm(pm);
        }
    }

    private OpenJPAEntityManager getNTWPM() {
        EntityManagerFactory pmf = getEmf();
        OpenJPAEntityManager em =
            (OpenJPAEntityManager) pmf.createEntityManager();
        em.setNontransactionalWrite(true);
        return em;
    }

    @Test
    void versionIncrementAndIdField() {
        // make sure that version increments happen correctly, and are
        // visible in the user-visible instance.
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        BasicSubclassInstance basic = new BasicSubclassInstance();
        basic.setStringField("foo");
        startTx(pm);
        pm.persist(basic);
        endTx(pm);

        assertEquals(1, basic.getVersion());
        long id = basic.getId();
        assertNotEquals(0, id);

        startTx(pm);
        basic.setStringField("bar");
        endTx(pm);
        assertEquals(2, basic.getVersion());
        endEm(pm);
    }

    @Test
    void jpaBasics() {
        EntityManager em = currentEntityManager();
        BasicSubclassInstance basic = (BasicSubclassInstance) createInstance(
            em, BasicSubclassInstance.class);
        basic.setStringField("hello");
        startTx(em);
        em.persist(basic);
        endTx(em);
        endEm(em);
    }

    @Test
    void singleValuedInverseManagement() {
        Map map = new HashMap();
        map.put("openjpa.InverseManager", "true");
        OpenJPAEntityManager pm = (OpenJPAEntityManager)
            getEmf(map).createEntityManager();
        ManagedInverseTestInstance managed = (ManagedInverseTestInstance)
            newInstance(pm, ManagedInverseTestInstance.class);
        ManagedInverseTestInstance2 managed2 = (ManagedInverseTestInstance2)
            newInstance(pm, ManagedInverseTestInstance2.class);
        managed.setStringField("managed");
        managed2.setStringField("managed2");
        managed.setManaged2(managed2);

        startTx(pm);
        pm.persist(managed);
        endTx(pm);

        assertSame(managed, managed2.getManaged());
    }

    @Test
    void backingFieldConfigurationWithTwoFactories() {
        OpenJPAEntityManager pm =
            (OpenJPAEntityManager) currentEntityManager();
        // this causes DerivedEntity.class to get loaded into PCRegistry
        newInstance(pm, DerivedEntity.class);
        Map map = new HashMap();
        map.put("openjpa.Log", "DiagnosticContext=subclass-two-factories-test");
        pm = (OpenJPAEntityManager) getEmf(map).createEntityManager();
        newInstance(pm, DerivedEntity.class);

        // this second new-instance creation will result in the metadata
        // defaults being loaded from the PCRegistry instead of via reflection.
        // Make sure that things still work as expected from the
        // registry-parsing code.
        ClassMetaData meta = getConfiguration()
            .getMetaDataRepositoryInstance().
            getMetaData(DerivedEntity.class, null, false);
        assertTrue(AccessCode.isProperty(meta.getAccessType()),
        		"meta's access should be ACCESS_PROPERTY");
        FieldMetaData[] fmds = meta.getFields();
        for (FieldMetaData fmd : fmds) {
            assertEquals(Method.class, fmd.getBackingMember().getClass());

            // make sure that the fields are defined in the right part of the
            // hierarchy
            if (fmd.getName().equals("intField") ||
                    fmd.getName().equals("oneToOne")) {
                assertEquals(DerivedEntity.class,
                        fmd.getDefiningMetaData().getDescribedType());
            }
            else {
                assertEquals(BaseEntity.class,
                        fmd.getDefiningMetaData().getDescribedType());
            }
        }
    }
}
