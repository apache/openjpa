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
package org.apache.openjpa.persistence.kernel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.apache.openjpa.persistence.kernel.common.apps.CalendarFields;

/**
 * OPENJPA-2789 connection didn't get closed properly
 */
public class TestBulkDelete extends AbstractTestCase {


    @Override
    public void setUp() throws Exception {
        super.setUp(CalendarFields.class);
    }

    public void testConnectionClosing() throws Exception {
        for (int i = 0; i < 30; i++) {
            EntityManager em = getEmf().createEntityManager();
            em.getTransaction().begin();
            final TypedQuery<CalendarFields> qry
                        = em.createQuery("delete from CalendarFields e where e.id = :val", CalendarFields.class);
            qry.setParameter("val", 12345);
            qry.executeUpdate();

            em.getTransaction().commit();
            em.close();
        }
    }
}
