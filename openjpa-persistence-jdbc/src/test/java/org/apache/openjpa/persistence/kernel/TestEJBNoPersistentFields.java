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
package org.apache.openjpa.persistence.kernel;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.common.utils.AbstractTestCase;

public class TestEJBNoPersistentFields extends AbstractTestCase {

    private TestEJBNoPersistentFieldsNholderEntity holder;

    public TestEJBNoPersistentFields(String test) {
        super(test, "kernelcactusapp");
    }

    @Override
    public void setUp() throws Exception {
        deleteAll(TestEJBNoPersistentFieldsNholderEntity.class);
    }

    public void testNoPersistentFields() {
        EntityManager em = currentEntityManager();
        startTx(em);

        holder = new TestEJBNoPersistentFieldsNholderEntity();
        holder.setNpf(new EJBNoPersistentFieldsNoPersistentFieldsPCEntity());
        holder.setIdKey(1);

        em.persist(holder);
        endTx(em);

        TestEJBNoPersistentFieldsNholderEntity holder2 = em.find(TestEJBNoPersistentFieldsNholderEntity.class, 1);
        assertEquals(1, holder2.getIdKey());
        assertNotNull(holder2);
        assertNotNull(holder2.getNpf());

        endEm(em);
    }

}
