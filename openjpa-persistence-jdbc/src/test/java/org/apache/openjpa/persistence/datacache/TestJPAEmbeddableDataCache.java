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
package org.apache.openjpa.persistence.datacache;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.openjpa.persistence.datacache.entities.ContactInfo;
import org.apache.openjpa.persistence.datacache.entities.Person;
import org.apache.openjpa.persistence.datacache.entities.Phone;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestJPAEmbeddableDataCache extends SingleEMFTestCase {
    Object[] params = new Object[] { Person.class, ContactInfo.class, Phone.class, "openjpa.DataCache", "true",
        CLEAR_TABLES };

    @Override
    protected void setUp(Object... props) {
        super.setUp(params);

    }

    public void test() {
        EntityManager em = emf.createEntityManager();
        try {
            Person p = loadPerson();
            String sql = "SELECT p.ci FROM Person p WHERE p.id = :id";

            TypedQuery<ContactInfo> query = em.createQuery(sql, ContactInfo.class);
            query.setParameter("id", p.getId());
            query.getSingleResult();

            em.clear();
            query = em.createQuery(sql, ContactInfo.class);
            query.setParameter("id", p.getId());
            query.getSingleResult();

        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    Person loadPerson() {
        Person p = null;
        EntityManager em = emf.createEntityManager();
        try {
            p = new Person();
            ContactInfo ci = new ContactInfo();
            Phone phone = new Phone();

            p.setCi(ci);
            ci.setPhone(phone);
            phone.setOwner(p);

            p.setFirst("first");
            p.setMiddle("middle");
            p.setLast("last");

            phone.setNumber("507-555-1076");
            phone.setSomethingElse("something-" + System.currentTimeMillis());

            ci.setCity("cittttY");
            ci.setStreet("street-" + System.currentTimeMillis());
            ci.setZip("90210");

            em.getTransaction().begin();
            em.persist(p);
            em.persist(phone);
            em.getTransaction().commit();

        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }

        return p;
    }
}
