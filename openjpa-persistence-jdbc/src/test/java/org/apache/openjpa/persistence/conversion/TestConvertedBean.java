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
package org.apache.openjpa.persistence.conversion;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.RestoreStateType;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * The value of a converted attribute is the object the converter returned.
 * The provider does not know its structure, so it neither replaces it by a
 * proxy nor copies it bean by bean, and finds a change made to it in place
 * by converting it again at flush.
 */
public class TestConvertedBean extends SQLListenerTestCase {

    @Override
    public void setUp() {
        setUp(ConvertedBeanEntity.class, SettingsHolder.class, CLEAR_TABLES);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity e = new ConvertedBeanEntity();
            e.setId(1L);
            e.setSettings(newSettings("crit", "user1"));
            e.setPoint(new Point(1, 2));
            e.getHolder().setSettings(newSettings("embedded", "user2"));
            em.persist(e);
            em.getTransaction().commit();
        }
    }

    static StaticSettings newSettings(String criteria, String user) {
        StaticSettings settings = new StaticSettings();
        settings.setCriteria(criteria);
        settings.getUsers().put(user, "pw");
        return settings;
    }

    /**
     * State without a setter is kept, and the value is of the class the
     * converter returned.
     */
    public void testGetterOnlyStateSurvivesLoad() {
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            assertSettings(e.getSettings(), "crit", "user1");
            assertSettings(e.getHolder().getSettings(), "embedded", "user2");
            assertSame(Point.class, e.getPoint().getClass());
            assertEquals(2, e.getPoint().getY());
        }
    }

    public void testValueChangedInPlace() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            e.getSettings().setCriteria("changed");
            ((StaticSettings) e.getSettings()).getUsers().put("user3", "pw");
            e.getPoint().setX(5);
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            assertEquals("changed", e.getSettings().getCriteria());
            assertEquals(2, ((StaticSettings) e.getSettings()).getUsers().size());
            assertEquals(5, e.getPoint().getX());
        }
    }

    public void testEmbeddedValueChangedInPlace() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            ((StaticSettings) e.getHolder().getSettings()).getUsers().put("user3", "pw");
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            assertEquals(2, ((StaticSettings) e.getHolder().getSettings()).getUsers().size());
        }
    }

    /**
     * A change made after a flush is written by the next flush.
     */
    public void testValueChangedInPlaceAfterFlush() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            e.getSettings().setCriteria("first");
            em.flush();
            e.getSettings().setCriteria("second");

            ConvertedBeanEntity n = new ConvertedBeanEntity();
            n.setId(2L);
            n.setSettings(newSettings("new", "user"));
            em.persist(n);
            em.flush();
            ((StaticSettings) n.getSettings()).getUsers().clear();
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals("second", em.find(ConvertedBeanEntity.class, 1L).getSettings().getCriteria());
            assertEquals(0,
                ((StaticSettings) em.find(ConvertedBeanEntity.class, 2L).getSettings()).getUsers().size());
        }
    }

    public void testUnchangedValueIsNotWritten() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals("crit", em.find(ConvertedBeanEntity.class, 1L).getSettings().getCriteria());
            resetSQL();
            em.getTransaction().commit();
        }
        assertNotSQL("UPDATE .*");
    }

    /**
     * A value changed in place on a detached instance is written by merge,
     * while an unchanged detached instance is merged without an update.
     */
    public void testDetachedValueChangedInPlace() {
        ConvertedBeanEntity d;
        try (EntityManager em = emf.createEntityManager()) {
            d = em.find(ConvertedBeanEntity.class, 1L);
        }
        assertSettings(d.getSettings(), "crit", "user1");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(d);
            resetSQL();
            em.getTransaction().commit();
        }
        assertNotSQL("UPDATE .*");

        ((StaticSettings) d.getSettings()).getUsers().put("user3", "pw");
        d.getPoint().setX(5);
        d.getHolder().getSettings().setCriteria("changed");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity m = em.merge(d);
            assertEquals(2, ((StaticSettings) m.getSettings()).getUsers().size());
            assertNotSame(d.getSettings(), m.getSettings());
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            assertEquals(2, ((StaticSettings) e.getSettings()).getUsers().size());
            assertEquals(5, e.getPoint().getX());
            assertEquals("changed", e.getHolder().getSettings().getCriteria());
        }
    }

    /**
     * A value changed in place on a serialized detached instance is written
     * by merge, and the merged instance gets its own copy of it.
     */
    public void testSerializedValueChangedInPlace() throws Exception {
        ConvertedBeanEntity d;
        try (EntityManager em = emf.createEntityManager()) {
            d = em.find(ConvertedBeanEntity.class, 1L);
        }
        d = roundtrip(d);
        assertSettings(d.getSettings(), "crit", "user1");
        ((StaticSettings) d.getSettings()).getUsers().put("user3", "pw");
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ConvertedBeanEntity m = em.merge(d);
            assertNotSame(d.getSettings(), m.getSettings());
            em.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(2, ((StaticSettings) em.find(ConvertedBeanEntity.class, 1L).getSettings())
                .getUsers().size());
        }
    }

    /**
     * A detached copy holds a copy of the value made by the converter, so it
     * keeps all its state and is not shared with the managed instance.
     */
    public void testDetachCopy() {
        try (EntityManager em = emf.createEntityManager()) {
            OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
            ConvertedBeanEntity e = oem.find(ConvertedBeanEntity.class, 1L);
            ConvertedBeanEntity d = oem.detachCopy(e);
            assertSettings(d.getSettings(), "crit", "user1");
            assertNotSame(e.getSettings(), d.getSettings());
            assertEquals(1, d.getPoint().getX());
            assertNotSame(e.getPoint(), d.getPoint());
        }
    }

    /**
     * On rollback, a replaced value is restored from the copy that was saved
     * when the field was first changed in the transaction.
     */
    public void testValueRestoredOnRollback() {
        try (EntityManager em = emf.createEntityManager()) {
            OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
            oem.setRestoreState(RestoreStateType.ALL);
            ConvertedBeanEntity e = oem.find(ConvertedBeanEntity.class, 1L);
            oem.getTransaction().begin();
            e.setSettings(newSettings("other", "user3"));
            e.setPoint(new Point(7, 7));
            oem.getTransaction().rollback();
            assertSettings(e.getSettings(), "crit", "user1");
            assertEquals(1, e.getPoint().getX());
        }
    }

    /**
     * On rollback, a value changed in place is restored to the value it had
     * when it was loaded, whether or not a flush found the change.
     */
    public void testValueChangedInPlaceRestoredOnRollback() {
        for (boolean flush : new boolean[] { false, true }) {
            ConvertedBeanEntity e = changeInPlaceAndRollback(RestoreStateType.ALL, flush);
            assertSettings(e.getSettings(), "crit", "user1");
            assertEquals(1, e.getPoint().getX());
            assertSettings(e.getHolder().getSettings(), "embedded", "user2");
        }
    }

    /**
     * With the default restore state, a value changed in place and not yet
     * flushed is restored to the value it had when it was loaded. Once a
     * flush found the change, the field is unloaded on rollback, as any
     * other mutable field that was changed.
     */
    public void testValueChangedInPlaceUndoneOnRollback() {
        ConvertedBeanEntity e = changeInPlaceAndRollback(RestoreStateType.IMMUTABLE, false);
        assertSettings(e.getSettings(), "crit", "user1");
        assertEquals(1, e.getPoint().getX());
        assertSettings(e.getHolder().getSettings(), "embedded", "user2");

        e = changeInPlaceAndRollback(RestoreStateType.IMMUTABLE, true);
        assertNull(e.getSettings());
        assertNull(e.getPoint());
    }

    /**
     * Change the converted values of instance 1 in place, optionally flush,
     * roll back, and return the instance.
     */
    private ConvertedBeanEntity changeInPlaceAndRollback(RestoreStateType restore, boolean flush) {
        try (EntityManager em = emf.createEntityManager()) {
            OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
            oem.setRestoreState(restore);
            ConvertedBeanEntity e = oem.find(ConvertedBeanEntity.class, 1L);
            oem.getTransaction().begin();
            e.getSettings().setCriteria("changed");
            ((StaticSettings) e.getSettings()).getUsers().put("user3", "pw");
            e.getPoint().setX(5);
            ((StaticSettings) e.getHolder().getSettings()).getUsers().clear();
            if (flush) {
                oem.flush();
            }
            oem.getTransaction().rollback();
            return e;
        }
    }

    /**
     * Rollback to a savepoint restores a value changed in place after the
     * savepoint, and keeps a change made before it.
     */
    public void testValueChangedInPlaceRestoredOnSavepointRollback() {
        try (EntityManager em = emf.createEntityManager()) {
            OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
            ConvertedBeanEntity e = oem.find(ConvertedBeanEntity.class, 1L);
            oem.getTransaction().begin();
            e.getSettings().setCriteria("before");
            oem.setSavepoint("sp");
            e.getSettings().setCriteria("after");
            ((StaticSettings) e.getSettings()).getUsers().put("user3", "pw");
            e.getPoint().setX(5);
            oem.rollbackToSavepoint("sp");
            assertSettings(e.getSettings(), "before", "user1");
            assertEquals(1, e.getPoint().getX());
            oem.getTransaction().commit();
        }
        try (EntityManager em = emf.createEntityManager()) {
            ConvertedBeanEntity e = em.find(ConvertedBeanEntity.class, 1L);
            assertSettings(e.getSettings(), "before", "user1");
            assertEquals(1, e.getPoint().getX());
        }
    }

    /**
     * Rollback to a savepoint restores a value changed in place on an
     * instance that was not yet part of the transaction.
     */
    public void testValueChangedInPlaceRestoredOnSavepointRollbackOfCleanInstance() {
        try (EntityManager em = emf.createEntityManager()) {
            OpenJPAEntityManager oem = OpenJPAPersistence.cast(em);
            ConvertedBeanEntity e = oem.find(ConvertedBeanEntity.class, 1L);
            oem.getTransaction().begin();
            oem.setSavepoint("sp");
            e.getSettings().setCriteria("after");
            oem.rollbackToSavepoint("sp");
            assertSettings(e.getSettings(), "crit", "user1");
            resetSQL();
            oem.getTransaction().commit();
            assertNotSQL("UPDATE .*");
        }
    }

    static void assertSettings(Settings settings, String criteria, String user) {
        assertSame(StaticSettings.class, settings.getClass());
        assertEquals(criteria, settings.getCriteria());
        assertEquals(1, ((StaticSettings) settings).getUsers().size());
        assertEquals("pw", ((StaticSettings) settings).getUsers().get(user));
    }
}
