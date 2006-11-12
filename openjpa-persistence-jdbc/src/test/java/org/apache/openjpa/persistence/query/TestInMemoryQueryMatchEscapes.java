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
package org.apache.openjpa.persistence.query;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.FlushModeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.simple.AllFieldTypes;

import junit.framework.TestCase;

public class TestInMemoryQueryMatchEscapes
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        Map options = new HashMap();

        // ensure that OpenJPA knows about our type, so that 
        // auto-schema-creation works
        options.put("openjpa.MetaDataFactory",
            "jpa(Types=" + AllFieldTypes.class.getName() + ")");

        emf = Persistence.createEntityManagerFactory("test", options);
    }
    
    public void testDatabaseEscape() {
        OpenJPAQuery q = escapeHelper(false);
        q.setFlushMode(FlushModeType.AUTO);
        q.getEntityManager().flush();
        AllFieldTypes aft = (AllFieldTypes) q.getSingleResult();
        assertEquals("foo_bar", aft.getStringField());
        q.getEntityManager().getTransaction().rollback();
    }
    
    public void testInMemoryEscape() {
        OpenJPAQuery q = escapeHelper(true);
        q.setFlushMode(FlushModeType.COMMIT);
        try {
            q.getSingleResult();
            fail("OpenJPA doesn't support escape syntax for in-mem queries");
        } catch (Exception e) {
            // expected
        }
    }
    
    private OpenJPAQuery escapeHelper(boolean inMem) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes aft = new AllFieldTypes();
        aft.setStringField("foo_bar");
        em.persist(aft);
        aft = new AllFieldTypes();
        aft.setStringField("foozbar");
        em.persist(aft);

        return (OpenJPAQuery) em.createQuery(
            "select e from AllFieldTypes e where e.stringField " +
            "like 'foox_bar' escape 'x'");
    }
}
