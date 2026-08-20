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
package org.apache.openjpa.persistence.simple;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.ExceptionInfo;

/**
 * Tests JPA 3.2 @EnumeratedValue for code types other than String, and for the
 * error cases: a database value that maps to no constant, and an enum whose
 * constants share a code.
 */
public class TestEnumeratedValueTypes extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES,
            EnumeratedValueIntEntity.class,
            EnumeratedValueShortEntity.class,
            EnumeratedValueLongEntity.class,
            EnumeratedValueCharEntity.class);
    }

    /**
     * An int code round-trips, and the column really holds the code, not the
     * ordinal and not the constant name.
     */
    public void testIntCodeRoundTrip() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < EnumeratedValueIntStatus.values().length; i++) {
            EnumeratedValueIntEntity e = new EnumeratedValueIntEntity();
            e.setId(i);
            e.setStatus(EnumeratedValueIntStatus.values()[i]);
            em.persist(e);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        for (int i = 0; i < EnumeratedValueIntStatus.values().length; i++) {
            EnumeratedValueIntEntity loaded =
                em.find(EnumeratedValueIntEntity.class, (long) i);
            assertNotNull(loaded);
            assertEquals(EnumeratedValueIntStatus.values()[i], loaded.getStatus());
        }
        em.close();
    }

    /**
     * The stored column value is the declared int code (20), not the ordinal
     * (1) and not the name.
     */
    public void testIntCodeStoredAsCode() {
        persistInt(100, EnumeratedValueIntStatus.INACTIVE);

        EntityManager em = emf.createEntityManager();
        Query q = em.createNativeQuery(
            "SELECT STATUS FROM EnumeratedValueIntEntity WHERE ID = 100");
        Object result = q.getSingleResult();
        assertTrue("expected a numeric column value but got " + result,
            result instanceof Number);
        assertEquals(EnumeratedValueIntStatus.INACTIVE.getCode(),
            ((Number) result).intValue());
        em.close();
    }

    /**
     * Regression test for the Short-key / Integer-column mismatch: the codes
     * are declared as short, the column is an int column, so the value read
     * back is an Integer.  Without the normalization of the lookup keys the
     * reverse lookup misses and the load fails.
     */
    public void testShortCodeRoundTrip() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < EnumeratedValueShortStatus.values().length; i++) {
            EnumeratedValueShortEntity e = new EnumeratedValueShortEntity();
            e.setId(i);
            e.setStatus(EnumeratedValueShortStatus.values()[i]);
            em.persist(e);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        for (int i = 0; i < EnumeratedValueShortStatus.values().length; i++) {
            EnumeratedValueShortEntity loaded =
                em.find(EnumeratedValueShortEntity.class, (long) i);
            assertNotNull(loaded);
            assertEquals(EnumeratedValueShortStatus.values()[i], loaded.getStatus());
        }
        em.close();
    }

    /**
     * The stored column value is the declared short code (22), not the ordinal
     * (1) and not the name.
     */
    public void testShortCodeStoredAsCode() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueShortEntity e = new EnumeratedValueShortEntity();
        e.setId(100);
        e.setStatus(EnumeratedValueShortStatus.INACTIVE);
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        Query q = em.createNativeQuery(
            "SELECT STATUS FROM EnumeratedValueShortEntity WHERE ID = 100");
        Object result = q.getSingleResult();
        assertTrue("expected a numeric column value but got " + result,
            result instanceof Number);
        assertEquals(EnumeratedValueShortStatus.INACTIVE.getCode(),
            ((Number) result).shortValue());
        em.close();
    }

    /**
     * A long code round-trips and keeps its full range, i.e. it is neither
     * narrowed to an int nor stored as a string.
     */
    public void testLongCodeRoundTrip() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < EnumeratedValueLongStatus.values().length; i++) {
            EnumeratedValueLongEntity e = new EnumeratedValueLongEntity();
            e.setId(i);
            e.setStatus(EnumeratedValueLongStatus.values()[i]);
            em.persist(e);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        for (int i = 0; i < EnumeratedValueLongStatus.values().length; i++) {
            EnumeratedValueLongEntity loaded =
                em.find(EnumeratedValueLongEntity.class, (long) i);
            assertNotNull(loaded);
            assertEquals(EnumeratedValueLongStatus.values()[i], loaded.getStatus());
        }
        em.close();
    }

    /**
     * The stored column value is the declared long code, in a numeric column.
     */
    public void testLongCodeStoredAsCode() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueLongEntity e = new EnumeratedValueLongEntity();
        e.setId(100);
        e.setStatus(EnumeratedValueLongStatus.INACTIVE);
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        Query q = em.createNativeQuery(
            "SELECT STATUS FROM EnumeratedValueLongEntity WHERE ID = 100");
        Object result = q.getSingleResult();
        assertTrue("expected a numeric column value but got " + result,
            result instanceof Number);
        assertEquals(EnumeratedValueLongStatus.INACTIVE.getCode(),
            ((Number) result).longValue());
        em.close();
    }

    /**
     * Regression test for the Character-key / string-column mismatch: the codes
     * are declared as char, the column is a string column, so the value read
     * back is a String.  Without the normalization of the lookup keys the
     * reverse lookup misses and the load fails.
     */
    public void testCharCodeRoundTrip() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < EnumeratedValueCharStatus.values().length; i++) {
            EnumeratedValueCharEntity e = new EnumeratedValueCharEntity();
            e.setId(i);
            e.setStatus(EnumeratedValueCharStatus.values()[i]);
            em.persist(e);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        for (int i = 0; i < EnumeratedValueCharStatus.values().length; i++) {
            EnumeratedValueCharEntity loaded =
                em.find(EnumeratedValueCharEntity.class, (long) i);
            assertNotNull(loaded);
            assertEquals(EnumeratedValueCharStatus.values()[i], loaded.getStatus());
        }
        em.close();
    }

    /**
     * The stored column value is the declared char code ("I"), not the ordinal
     * and not the name.
     */
    public void testCharCodeStoredAsCode() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueCharEntity e = new EnumeratedValueCharEntity();
        e.setId(100);
        e.setStatus(EnumeratedValueCharStatus.INACTIVE);
        em.persist(e);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        Query q = em.createNativeQuery(
            "SELECT STATUS FROM EnumeratedValueCharEntity WHERE ID = 100");
        Object result = q.getSingleResult();
        assertEquals(String.valueOf(EnumeratedValueCharStatus.INACTIVE.getCode()),
            result.toString().trim());
        em.close();
    }

    /**
     * A JPQL predicate on an int coded enum must use the code, i.e. the write
     * side and the read side of the handler have to agree.
     */
    public void testIntCodeInJPQLPredicate() {
        persistInt(200, EnumeratedValueIntStatus.ACTIVE);
        persistInt(201, EnumeratedValueIntStatus.PENDING);

        EntityManager em = emf.createEntityManager();
        List<EnumeratedValueIntEntity> res = em.createQuery(
            "SELECT e FROM EnumeratedValueIntEntity e WHERE e.status = :s",
            EnumeratedValueIntEntity.class)
            .setParameter("s", EnumeratedValueIntStatus.PENDING)
            .getResultList();
        assertEquals(1, res.size());
        assertEquals(201, res.get(0).getId());
        assertEquals(EnumeratedValueIntStatus.PENDING, res.get(0).getStatus());
        em.close();
    }

    /**
     * A JPQL predicate on a char coded enum must use the code.
     */
    public void testCharCodeInJPQLPredicate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueCharEntity a = new EnumeratedValueCharEntity();
        a.setId(200);
        a.setStatus(EnumeratedValueCharStatus.ACTIVE);
        em.persist(a);
        EnumeratedValueCharEntity p = new EnumeratedValueCharEntity();
        p.setId(201);
        p.setStatus(EnumeratedValueCharStatus.PENDING);
        em.persist(p);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        List<EnumeratedValueCharEntity> res = em.createQuery(
            "SELECT e FROM EnumeratedValueCharEntity e WHERE e.status = :s",
            EnumeratedValueCharEntity.class)
            .setParameter("s", EnumeratedValueCharStatus.PENDING)
            .getResultList();
        assertEquals(1, res.size());
        assertEquals(201, res.get(0).getId());
        assertEquals(EnumeratedValueCharStatus.PENDING, res.get(0).getStatus());
        em.close();
    }

    /**
     * A database value that maps to no constant must fail loudly instead of
     * silently producing an entity with a null enum field.
     */
    public void testUnknownDatabaseValueFails() {
        persistInt(300, EnumeratedValueIntStatus.ACTIVE);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery(
            "UPDATE EnumeratedValueIntEntity SET STATUS = 999 WHERE ID = 300")
            .executeUpdate();
        em.getTransaction().commit();
        em.clear();
        em.close();

        em = emf.createEntityManager();
        try {
            EnumeratedValueIntEntity loaded =
                em.find(EnumeratedValueIntEntity.class, 300L);
            // some paths may defer the load, so touch the field as well
            fail("expected the unmapped database value to be rejected, but got "
                + (loaded == null ? "null" : String.valueOf(loaded.getStatus())));
        } catch (RuntimeException re) {
            // bad data, not a bad call: the internal
            // org.apache.openjpa.util.StoreException has to be translated into
            // a jakarta.persistence.PersistenceException, not into an
            // IllegalArgumentException
            assertNested(PersistenceException.class, re);
            assertFalse("a corrupt column value must not be reported as an "
                + "IllegalArgumentException, but got " + re,
                re instanceof IllegalArgumentException);
            // and it has to name the offending value and the enum type, so
            // that this cannot pass on some unrelated failure
            assertMessageContains(re, "999");
            assertMessageContains(re, "EnumeratedValueIntStatus");
        } finally {
            em.close();
        }
    }

    /**
     * Two constants sharing one code is a mapping error and must be reported
     * when the mapping is built.
     */
    public void testDuplicateCodesRejected() {
        EntityManagerFactory dupEMF = null;
        try {
            dupEMF = createEMF(CLEAR_TABLES, EnumeratedValueDupEntity.class);
            EntityManager em = dupEMF.createEntityManager();
            em.createQuery("SELECT e FROM EnumeratedValueDupEntity e")
                .getResultList();
            em.close();
            fail("expected the duplicate @EnumeratedValue code to be rejected");
        } catch (RuntimeException re) {
            // the internal org.apache.openjpa.util.MetaDataException is
            // translated into the JPA facing ArgumentException
            assertNested(ArgumentException.class, re);
            // ArgumentException is the catch all bucket of the translation, so
            // the message has to show that it really is the duplicate code
            // check that rejected the mapping
            assertMessageContains(re, "EnumeratedValueDupStatus");
            assertMessageContains(re, "\"X\"");
        } finally {
            if (dupEMF != null) {
                closeEMF(dupEMF);
            }
        }
    }

    /**
     * A @EnumeratedValue field whose type is neither a string nor an integral
     * type is a mapping error and must be reported when the mapping is built,
     * instead of being silently stored as the result of toString().
     */
    public void testUnsupportedCodeTypeRejected() {
        EntityManagerFactory badEMF = null;
        try {
            badEMF = createEMF(CLEAR_TABLES, EnumeratedValueBadTypeEntity.class);
            EntityManager em = badEMF.createEntityManager();
            em.createQuery("SELECT e FROM EnumeratedValueBadTypeEntity e")
                .getResultList();
            em.close();
            fail("expected the unsupported @EnumeratedValue type to be rejected");
        } catch (RuntimeException re) {
            assertNested(ArgumentException.class, re);
            assertMessageContains(re, "EnumeratedValueBadTypeStatus");
            assertMessageContains(re, "double");
        } finally {
            if (badEMF != null) {
                closeEMF(badEMF);
            }
        }
    }

    private void persistInt(long id, EnumeratedValueIntStatus status) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EnumeratedValueIntEntity e = new EnumeratedValueIntEntity();
        e.setId(id);
        e.setStatus(status);
        em.persist(e);
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Assert that the given throwable is, or nests, an instance of the expected
     * type.  Only the type is asserted; the message text is not part of the
     * contract.
     */
    private void assertNested(Class<? extends Throwable> expected, Throwable actual) {
        if (!findNested(expected, actual, new IdentityHashMap<>())) {
            throw new AssertionError("expected a nested " + expected.getName()
                + " but got " + actual, actual);
        }
    }

    private boolean findNested(Class<? extends Throwable> expected, Throwable actual,
        Map<Throwable, Throwable> seen) {
        if (actual == null || seen.put(actual, actual) != null) {
            return false;
        }
        if (expected.isInstance(actual)) {
            return true;
        }
        if (findNested(expected, actual.getCause(), seen)) {
            return true;
        }
        if (actual instanceof ExceptionInfo) {
            Throwable[] nested = ((ExceptionInfo) actual).getNestedThrowables();
            for (int i = 0; nested != null && i < nested.length; i++) {
                if (findNested(expected, nested[i], seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Assert that the given throwable, or one of the throwables it nests,
     * mentions the given text.  This keeps the error tests from passing on any
     * unrelated failure that happens to be translated into the same type,
     * without pinning down the whole message text.
     */
    private void assertMessageContains(Throwable actual, String expected) {
        List<String> msgs = new ArrayList<>();
        collectMessages(actual, msgs, new IdentityHashMap<>());
        for (String msg : msgs) {
            if (msg.contains(expected)) {
                return;
            }
        }
        throw new AssertionError("expected a message containing \"" + expected
            + "\" but got " + msgs, actual);
    }

    private void collectMessages(Throwable actual, List<String> msgs,
        Map<Throwable, Throwable> seen) {
        if (actual == null || seen.put(actual, actual) != null) {
            return;
        }
        if (actual.getMessage() != null) {
            msgs.add(actual.getMessage());
        }
        collectMessages(actual.getCause(), msgs, seen);
        if (actual instanceof ExceptionInfo) {
            Throwable[] nested = ((ExceptionInfo) actual).getNestedThrowables();
            for (int i = 0; nested != null && i < nested.length; i++) {
                collectMessages(nested[i], msgs, seen);
            }
        }
    }
}
