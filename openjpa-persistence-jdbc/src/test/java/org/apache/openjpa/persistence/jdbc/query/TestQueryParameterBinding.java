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
package org.apache.openjpa.persistence.jdbc.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.jdbc.query.domain.Binder;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests validation of positional and named parameter binding for JPQL queries.
 *
 *
 * @author Pinaki Poddar
 *
 */
class TestQueryParameterBinding extends SingleEMFTestCase {
	private static String JPQL = "SELECT p FROM Binder p ";

	private static int INT_VALUE    = 1;
	private static String STR_VALUE = "2";
	private static double DBL_VALUE = 3.0;

	private EntityManager em;

    @BeforeEach
    public void setUp() throws Exception {
		super.setUp(CLEAR_TABLES, Binder.class);

		em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(new Binder(INT_VALUE, STR_VALUE, DBL_VALUE));
		em.getTransaction().commit();
	}

    @Test
    void positionalParameterWithPositionalBindingSucceeds() {
        String JPQL_POSITIONAL = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);
		q.setParameter(3, DBL_VALUE);

		assertEquals(1, q.getResultList().size());
	}

    @Test
    void positionalParameterWithNamedBindingFails() {
        String JPQL_POSITIONAL = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		assertSetParameterFails(q, "p1", INT_VALUE);
	}

    @Test
    void positionalParameterWithInsufficientValuesFails() {
        String JPQL_POSITIONAL = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);

		fail(q);
	}

    @Test
    void positionalParameterWithExtraValuesFails() {
        String JPQL_POSITIONAL = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1, INT_VALUE);
		q.setParameter(2, STR_VALUE);
		q.setParameter(3, DBL_VALUE);
		assertSetParameterFails(q, 4, 4);
	}

    @Test
    void positionalParameterWithRepeatedValuesSucceeds() {
		String jPQL_POSITIONAL_REPEATED_PARAM  =
			JPQL + "WHERE p.p1=?1 OR p.p1=?1 AND p.p3=?2";
		Query q = em.createQuery(jPQL_POSITIONAL_REPEATED_PARAM);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  DBL_VALUE);

		assertEquals(1,q.getResultList().size());
	}

    @AllowFailure
    @Test
    void positionalParameterWithGapSucceeds() {
		String JPQL_POSITIONAL_GAP_IN_PARAM  =
			JPQL + "WHERE p.p1=?1 AND p.p2=?3";
		Query q = em.createQuery(JPQL_POSITIONAL_GAP_IN_PARAM);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(3,  STR_VALUE);

		assertEquals(1,q.getResultList().size());
	}

    @Test
    void positionalParameterWithGapFails() {
		String JPQL_POSITIONAL_GAP_IN_PARAM  =
			JPQL + "WHERE p.p1=?1 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL_GAP_IN_PARAM);
		q.setParameter(1,  INT_VALUE);
        q.setParameter(3,  DBL_VALUE);
		assertSetParameterFails(q, 2,  STR_VALUE);

		fail(q);
	}

    @Test
    void namedParameterWithNamedBindingSucceeds() {
        String JPQL_NAMED = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);
		q.setParameter("p3", DBL_VALUE);

		assertEquals(1, q.getResultList().size());
	}

    @Test
    void namedParameterWithPositionalBindingFails() {
        String JPQL_NAMED = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		assertSetParameterFails(q, 1, INT_VALUE);
	}

    @Test
    void namedParameterWithInsufficientValuesFails() {
        String JPQL_NAMED = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);

		fail(q);
	}

    @Test
    void namedParameterWithExtraValuesFails() {
        String JPQL_NAMED = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1", INT_VALUE);
		q.setParameter("p2", STR_VALUE);
		q.setParameter("p3", DBL_VALUE);
		assertSetParameterFails(q, "p4", 4);
	}

    @Test
    void namedParameterWithRepeatedValuesSucceeds() {
		String jPQL_NAMED_REPEATED_PARAM  =
			JPQL + "WHERE p.p1=:p1 OR p.p1=:p1 AND p.p3=:p2";
		Query q = em.createQuery(jPQL_NAMED_REPEATED_PARAM);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p2",  DBL_VALUE);

		assertEquals(1,q.getResultList().size());
	}

    @Test
    void namedParameterWithGapSucceeds() {
		String JPQL_NAMED_GAP_IN_PARAM  =
			JPQL + "WHERE p.p1=:p1 AND p.p2=:p3";
		Query q = em.createQuery(JPQL_NAMED_GAP_IN_PARAM);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p3",  STR_VALUE);

		assertEquals(1,q.getResultList().size());
	}

    @Test
    void namedParameterWithGapFails() {
		String JPQL_NAMED_GAP_IN_PARAM  =
			JPQL + "WHERE p.p1=:p1 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED_GAP_IN_PARAM);
		q.setParameter("p1",  INT_VALUE);
        q.setParameter("p3",  DBL_VALUE);
		assertSetParameterFails(q, "p2",  STR_VALUE);
	}

    @Test
    void namedParameterWithWrongType() {
        String JPQL_NAMED = JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_NAMED);
		q.setParameter("p1",  INT_VALUE);
		assertSetParameterFails(q, "p2",  DBL_VALUE);
		assertSetParameterFails(q, "p3",  STR_VALUE);
	}

    @Test
    void positionalParameterWithWrongType() {
        String JPQL_POSITIONAL = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  INT_VALUE);
		assertSetParameterFails(q, 2,  DBL_VALUE);
	}

    @Test
    void namedParameterWithNullValue() {
        String JPQL_POSITIONAL =
            JPQL + "WHERE p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter("p1",  INT_VALUE);
		q.setParameter("p2",  null);
		assertSetParameterFails(q, "p3",  null); // Parameter<double> can not be set to null
	}

    @Test
    void positionalParameterWithNullValue() {
        String JPQL_POSITIONAL = JPQL + "WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3";
		Query q = em.createQuery(JPQL_POSITIONAL);
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  null);             // Parameter<String> can be set to null
		assertSetParameterFails(q, 3,  null); // Parameter<double> can not be set to null
	}

    @Test
    void positionalParameterWithSingleResult() {
		Query q = em.createNamedQuery("JPQL_POSITIONAL");
        // "SELECT p FROM Binder p WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  null);              // Parameter<String> can be set to null
		assertSetParameterFails(q, 3,  null); // Parameter<double> can not be set to null
	}

    @Test
    void positionalParameterWithNativeQuery() {
		Query q = em.createNamedQuery("SQL_POSITIONAL");
        // "SELECT p.id FROM Binder WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  STR_VALUE);
		q.setParameter(3,  DBL_VALUE);

		assertEquals(1,q.getResultList().size());
	}

    @Test
    void positionalParameterWithNativeQueryFails() {
		Query q = em.createNamedQuery("SQL_POSITIONAL");
        // "SELECT p.id FROM Binder WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(2,  STR_VALUE);

		fail(q);
	}

    @Test
    void positionalParameterWithNativeQueryFailsWithGap() {
		Query q = em.createNamedQuery("SQL_POSITIONAL");
        // "SELECT p.id FROM Binder WHERE p.p1=?1 AND p.p2=?2 AND p.p3=?3"
		q.setParameter(1,  INT_VALUE);
		q.setParameter(3,  DBL_VALUE);

		fail(q);
	}

    @Test
    void repeatedNamedParameters() {
        String JPQL_POSITIONAL =
            JPQL + "WHERE p.p1 in (select max(p.p1) from Binder p where p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3) "
            + "AND p.p1=:p1 AND p.p2=:p2 AND p.p3=:p3";
        Query q = em.createQuery(JPQL_POSITIONAL);
        q.setParameter("p1",  INT_VALUE);
        q.setParameter("p2",  STR_VALUE);
        q.setParameter("p3",  DBL_VALUE);
        List list = q.getResultList();
        assertEquals(1, list.size());
    }


    void assertSetParameterFails(Query q, String name, Object v) {
        try {
            q.setParameter(name, v);
            Assertions.fail("Expected to fail in setting named parameter [" + name + "] to a value of " + v);
        } catch (IllegalArgumentException e) {
            // good
        }
    }

    void assertSetParameterFails(Query q, int pos, Object v) {
        try {
            q.setParameter(pos, v);
            Assertions.fail("Expected to fail in setting positional parameter [" + pos + "] to a value of " + v);
        } catch (IllegalArgumentException e) {
            // good
        }
    }

	void fail(Query q) {
		fail(q, false);
	}

	void fail(Query q, boolean single) {
		try {
			if (single)
				q.getSingleResult();
			else
				q.getResultList();
			Assertions.fail("Expeceted " + ArgumentException.class.getName());
		} catch (IllegalArgumentException ex) {
		    // good
		}
	}

}
