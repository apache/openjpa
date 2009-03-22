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

import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestRefresh extends SingleEMTestCase {

    public void setUp() {
        super.setUp(CLEAR_TABLES, Item.class, "openjpa.AutoDetach", "commit");
    }

    public void testFlushRefreshNewInstance() {
        em.getTransaction().begin();
        Item item = new Item();
        item.setItemData("Test Data");
        em.persist(item);
        em.flush();
        em.refresh(item);
        em.getTransaction().commit();
        assertEquals("Test Data", item.getItemData());
    }
}
