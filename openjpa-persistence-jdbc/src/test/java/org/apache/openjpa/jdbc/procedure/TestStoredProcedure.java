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
package org.apache.openjpa.jdbc.procedure;

import org.apache.openjpa.jdbc.procedure.derby.Procedures;
import org.apache.openjpa.jdbc.procedure.entity.EntityWithStoredProcedure;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import java.util.Iterator;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.StoredProcedureQuery;

@DatabasePlatform("org.apache.derby.jdbc.EmbeddedDriver")
public class TestStoredProcedure extends SingleEMFTestCase {
    @Override
    public void setUp() {
        setUp(
                "openjpa.RuntimeUnenhancedClasses", "unsupported",
                "openjpa.DynamicEnhancementAgent", "false",
                EntityWithStoredProcedure.class, EntityWithStoredProcedure.Mapping2.class);
        setSupportedDatabases(DerbyDictionary.class);
    }

    public void testSimple() {
        Procedures.simpleCalled = false;

        EntityManager em = emf.createEntityManager();
        exec(em, "CREATE PROCEDURE TESTSIMPLE() " +
                "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                "'" + Procedures.class.getName() + ".simple'");
        StoredProcedureQuery procedure = em.createNamedStoredProcedureQuery("EntityWithStoredProcedure.simple");
        assertFalse(procedure.execute());
        em.close();
        assertTrue(Procedures.simpleCalled);
    }

    public void testInParams() {
        Procedures.inParamsInteger = -1;
        Procedures.inParamsString = null;

        EntityManager em = emf.createEntityManager();
        exec(em, "CREATE PROCEDURE TESTINS(SOME_NUMBER INTEGER,SOME_STRING VARCHAR(255)) " +
                "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                "'" + Procedures.class.getName() + ".inParams'");
        StoredProcedureQuery procedure = em.createNamedStoredProcedureQuery("EntityWithStoredProcedure.inParams");
        procedure.setParameter("some_number", 2015);
        procedure.setParameter("some_string", "openjpa");
        assertFalse(procedure.execute());
        assertEquals(2015, Procedures.inParamsInteger);
        assertEquals("openjpa", Procedures.inParamsString);

        // null case
        Procedures.inParamsInteger = -1;
        Procedures.inParamsString = null;
        StoredProcedureQuery procedure2 = em.createNamedStoredProcedureQuery("EntityWithStoredProcedure.inParams");
        procedure2.setParameter("some_number", 20152);
        assertFalse(procedure2.execute());
        em.close();
        assertEquals(20152, Procedures.inParamsInteger);
        assertNull(Procedures.inParamsString);
    }

    public void testOut() {
        EntityManager em = emf.createEntityManager();
        exec(em, "CREATE PROCEDURE XTWO(IN SOME_NUMBER INTEGER,OUT x2 INTEGER) " +
                "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                "'" + Procedures.class.getName() + ".x2'");
        StoredProcedureQuery procedure = em.createNamedStoredProcedureQuery("EntityWithStoredProcedure.x2");
        procedure.setParameter("some_number", 5);
        assertFalse(procedure.execute());
        // assertEquals(10, procedure.getOutputParameterValue("result")); //  not impl by derby
        assertEquals(10, procedure.getOutputParameterValue(2));
        em.close();
    }

    public void testInOut() {
        EntityManager em = emf.createEntityManager();
        exec(em, "CREATE PROCEDURE XINOUT(INOUT P INTEGER) " +
                "PARAMETER STYLE JAVA LANGUAGE JAVA EXTERNAL NAME " +
                "'" + Procedures.class.getName() + ".inout'");
        StoredProcedureQuery procedure = em.createNamedStoredProcedureQuery("EntityWithStoredProcedure.inout");
        procedure.setParameter("p", 5);
        assertFalse(procedure.execute());
        // assertEquals(10, procedure.getOutputParameterValue("p")); //  not impl by derby
        assertEquals(10, procedure.getOutputParameterValue(1));
        em.close();
    }

    public void testMapping() {
        EntityManager em = emf.createEntityManager();
        {
            em.getTransaction().begin();
            for (int i = 0; i < 2; i++) {
                final EntityWithStoredProcedure entity = new EntityWithStoredProcedure();
                entity.setId(1 + i);
                entity.setName("#" + entity.getId());
                em.persist(entity);
            }
            em.getTransaction().commit();
            em.clear();
        }

        exec(em, "CREATE PROCEDURE MAPPING() " +
                "PARAMETER STYLE JAVA LANGUAGE JAVA DYNAMIC RESULT SETS 2 EXTERNAL NAME " +
                "'" + Procedures.class.getName() + ".mapping'");
        StoredProcedureQuery procedure = em.createNamedStoredProcedureQuery("EntityWithStoredProcedure.mapping");
        assertTrue(procedure.execute());
        final Iterator r1 = procedure.getResultList().iterator();
        final EntityWithStoredProcedure next1 = EntityWithStoredProcedure.class.cast(r1.next());
        assertEquals(1, next1.getId());
        assertEquals("#1", next1.getName());
        assertNotNull(next1);
        final EntityWithStoredProcedure next2 = EntityWithStoredProcedure.class.cast(r1.next());
        assertNotNull(next2);
        assertEquals(2, next2.getId());
        assertEquals("#2", next2.getName());
        assertFalse(r1.hasNext());
        assertTrue(procedure.hasMoreResults());
        final Iterator r2 = procedure.getResultList().iterator();
        final EntityWithStoredProcedure.Mapping2 next3 = EntityWithStoredProcedure.Mapping2.class.cast(r2.next());
        assertNotNull(next3);
        assertFalse(r2.hasNext());
        assertEquals(next2.getId(), next3.getId());
        assertEquals(next2.getName(), next3.getName());

        {
            em.getTransaction().begin();
            for (int i = 0; i < 2; i++) {
                em.remove(em.find(EntityWithStoredProcedure.class, i + 1L));
            }
            em.getTransaction().commit();
            em.clear();
        }
        em.close();
    }

    private void exec(final EntityManager em, final String proc) {
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.createNativeQuery(proc).executeUpdate();
            tx.commit();
        } catch (final Exception e) { // already exists or another error
            e.printStackTrace();
            try {
                tx.rollback();
            } catch (final Exception ignored) {
                // no-op
            }
        }
    }
}
