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
package org.apache.openjpa.persistence.detachment;

import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.detachment.model.DMCustomer;
import org.apache.openjpa.persistence.detachment.model.DMCustomerInventory;
import org.apache.openjpa.persistence.detachment.model.DMItem;

public class TestDetachLite extends TestDetach {
    public void setUp() {
        super.setUp(
            "openjpa.DetachState", "loaded(LiteAutoDetach=true)", 
            DMCustomer.class, DMCustomerInventory.class, DMItem.class, 
            CLEAR_TABLES
            );

        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        compat.setCopyOnDetach(false);
        compat.setFlushBeforeDetach(false);
        em = emf.createEntityManager();
        root = createData();
    }

    public void testCloseDetach() {
        root = em.merge(root);
        PersistenceCapable pc = (PersistenceCapable) root;
        assertFalse(pc.pcIsDetached());
        em.close();
        assertTrue(pc.pcIsDetached());
        // Make sure everything is detached and we can still use the Entity
        for (DMCustomerInventory c : root.getCustomerInventories()) {
            pc = (PersistenceCapable) c;
            assertTrue(pc.pcIsDetached());
            pc = (PersistenceCapable) c.getItem();
            assertTrue(pc.pcIsDetached());

        }
    }
}
