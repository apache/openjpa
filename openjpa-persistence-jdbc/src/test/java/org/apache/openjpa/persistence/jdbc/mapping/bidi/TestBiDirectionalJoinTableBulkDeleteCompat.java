/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc.mapping.bidi;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * The rows of a bi-directional join table are maintained by the inverse side,
 * so a candidate owning such a table can only be bulk deleted in SQL when the
 * cleanup of the owned tables is enabled. With
 * <code>CleanupOwnedTablesOnBulkDelete=false</code> the query falls back to the
 * in-memory path, which deletes the candidates one by one and leaves the join
 * table rows behind.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBiDirectionalJoinTableBulkDeleteCompat
    extends SQLListenerTestCase {

    private static final long SSN = 123456789;
    private static final String[] PHONES = {"+1-23-456", "+2-34-567"};

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, Person.class, Address.class,
            "openjpa.Compatibility", "CleanupOwnedTablesOnBulkDelete=false");
        createData();
        sql.clear();
    }

    public void testBulkDeleteFallsBackToTheInMemoryPath() {
        try (EntityManager em = emf.createEntityManager()) {
	        em.getTransaction().begin();
	        assertEquals(1, em.createQuery("delete from Person p where p.ssn=:ssn")
	            .setParameter("ssn", SSN).executeUpdate());
	        em.getTransaction().commit();
        }

        assertEquals(0, count(Person.class));
        // the candidates are loaded and removed one by one
        assertSQL("SELECT .*FROM .*J_PERSON.*");
        assertSQL("DELETE FROM .*J_PERSON WHERE .*");
        assertNotSQL("DELETE FROM .*IN \\(.*");
        // JPA spec section 4.10: a bulk delete does not cascade to entities
        assertEquals(PHONES.length, count(Address.class));
    }

    private void createData() {
        try (EntityManager em = emf.createEntityManager()) {
	        em.getTransaction().begin();
	        Person person = new Person();
	        person.setSsn(SSN);
	        person.setName("person");
	        for (String phone : PHONES) {
	            Address address = new Address();
	            address.setPhone(phone);
	            address.setCity("city");
	            person.addAddress(address);
	            em.persist(address);
	        }
	        em.persist(person);
	        em.getTransaction().commit();
        }
    }
}
