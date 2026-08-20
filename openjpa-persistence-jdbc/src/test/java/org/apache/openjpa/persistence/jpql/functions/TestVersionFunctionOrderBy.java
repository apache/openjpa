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
package org.apache.openjpa.persistence.jpql.functions;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.common.apps.Address;
import org.apache.openjpa.persistence.common.apps.CompUser;
import org.apache.openjpa.persistence.common.apps.CompVerUser;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests that VERSION(x) in an ORDER BY clause orders by the version
 * column(s), and that VERSION(x) on an unversioned type is rejected with a
 * meaningful user exception instead of a NullPointerException.
 */
public class TestVersionFunctionOrderBy extends SQLListenerTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES, CompUser.class, CompVerUser.class, Address.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new CompVerUser("Bob", "n/a", null, 21));
        em.persist(new CompVerUser("Alice", "n/a", null, 22));
        em.persist(new CompUser("Ugo", "n/a", null, 23));
        em.getTransaction().commit();
        em.close();
    }

    public void testOrderByVersionUsesVersionColumn() {
        EntityManager em = emf.createEntityManager();
        resetSQL();

        em.createQuery("SELECT VERSION(u) FROM CompVerUser u ORDER BY VERSION(u)").getResultList();

        assertContainsSQL("ORDER BY t0.version ASC");
        assertNotSQL(".* ORDER BY t0.userid .*");
        em.close();
    }

    public void testOrderByVersionDescendingUsesVersionColumn() {
        EntityManager em = emf.createEntityManager();
        resetSQL();

        em.createQuery("SELECT u FROM CompVerUser u ORDER BY VERSION(u) DESC").getResultList();

        assertContainsSQL("ORDER BY t0.version DESC");
        assertNotSQL(".* ORDER BY t0.userid .*");
        em.close();
    }

    public void testOrderByVersionAmongOtherOrderings() {
        EntityManager em = emf.createEntityManager();
        resetSQL();

        em.createQuery("SELECT u.name, VERSION(u) FROM CompVerUser u ORDER BY VERSION(u), u.name")
            .getResultList();

        assertContainsSQL("ORDER BY t0.version ASC, t0.name ASC");
        em.close();
    }

    public void testWhereVersionStillUsesVersionColumn() {
        EntityManager em = emf.createEntityManager();
        resetSQL();

        List<?> result = em.createQuery(
            "SELECT u FROM CompVerUser u WHERE u.name = :name AND VERSION(u) = :v")
            .setParameter("name", "Bob").setParameter("v", 1).getResultList();

        assertEquals(1, result.size());
        assertContainsSQL("t0.version = ?");
        em.close();
    }

    public void testVersionOnUnversionedTypeThrowsUserException() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createQuery("SELECT u FROM CompUser u ORDER BY VERSION(u)").getResultList();
            fail("VERSION() on an unversioned type should be rejected");
        } catch (RuntimeException e) {
            // must be a meaningful message, not a NullPointerException
            String msg = getNestedMessages(e);
            assertTrue("Unexpected failure: " + msg,
                msg.contains("does not") && msg.contains("version"));
            assertTrue("Message should name the offending type: " + msg,
                msg.contains(CompUser.class.getName()));
            assertFalse("Should not fail with a NullPointerException: " + msg,
                hasNPE(e));
        } finally {
            em.close();
        }
    }

    private String getNestedMessages(Throwable t) {
        StringBuilder buf = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            buf.append(c).append(' ');
            if (c.getCause() == c) {
                break;
            }
        }
        return buf.toString();
    }

    private boolean hasNPE(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof NullPointerException) {
                return true;
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return false;
    }
}
