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
package org.apache.openjpa.persistence.results.cls;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestResultClsAnnotation extends SQLListenerTestCase {
    @Override
    public void setUp() {
        setUp(ResultClsAnnotation.class, DROP_TABLES);
        assertNotNull(emf);

        populate();
    }

    public void testIt() {
        EntityManager em = emf.createEntityManager();

        try {
            Query q = getQuery(em);
            List<ResultClsAnnotation> result = q.getResultList();
            assertEquals(1, result.size());

            for (ResultClsAnnotation resultClsAnnotation : result) {
                Object obj = (Object) resultClsAnnotation;
                ResultClsAnnotation ct = (ResultClsAnnotation) obj;
                assertEquals("id1", ct.getId());
                assertEquals("description1", ct.getDescription());
            }
        } catch (Exception ex) {
            fail("unexpected exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            em.close();
        }
    }

    private void populate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        ResultClsAnnotation ct = new ResultClsAnnotation();
        ct.setId("id1");
        ct.setDescription("description1");
        em.persist(ct);

        em.getTransaction().commit();
        em.close();
    }

    private Query getQuery(EntityManager em) {
        DBDictionary dict = getDBDictionary();
        Query query = null;
        if (dict.getLeadingDelimiter().equals("\"") && dict.getTrailingDelimiter().equals("\"")) {
            query = em.createNamedQuery("ResultClsQueryDoubleQuotes");
        } else if (dict.getLeadingDelimiter().equals("`") && dict.getTrailingDelimiter().equals("`")) {
            query = em.createNamedQuery("ResultClsQueryBackTicks");
        } else if (dict.getLeadingDelimiter().equals("[") && dict.getTrailingDelimiter().equals("]")) {
            query = em.createNamedQuery("ResultClsQueryBrackets");
        } else {
            query = em.createNamedQuery("ResultClsQueryDefault");
        }
        return query;
    }
}
