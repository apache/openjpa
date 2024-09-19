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
package org.apache.openjpa.persistence.merge;

import java.sql.Date;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/*
 * See OPENJPA-2603 for details.
 */
public class TestMultipleMerge extends SingleEMFTestCase {

    @Override
    public void setUp() {
        //Since a JPA 1.0 p.xml file is used the property "NonDefaultMappingAllowed=true" is
        //needed for this test.  This is needed since Order uses an @JoinColumn; something
        //not allowed in 1.0 (or at least a grey area in the spec) on an @OneToMany.
        setUp("openjpa.Compatibility", "NonDefaultMappingAllowed=true",
            CLEAR_TABLES, Order.class, LineItemPK.class, LineItem.class);
    }

    public void testMultiMerge() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Order order = new Order(1L);

        LineItem item = new LineItem( "my product", 44, 4.99f );
        order.addItem(item);

        //NOTE: Notice that throughout the rest of the test the unmanaged order is merged.
        //Throughout the rest of the test we should do a 'order = em.merge(order)', or
        //something to that effect (i.e. use the 'managed' order).  However, technically
        //speaking merging the unmanaged order is not wrong, albeit odd and potentially
        //error prone.
        em.merge(order);
        em.getTransaction().commit();

        em.getTransaction().begin();
        LineItem additional = new LineItem( "My second product", 1, 999.95f );
        order.addItem(additional);
        order.setOrderEntry( new Date( System.currentTimeMillis() ) );
        em.merge(order);
        //NOTE: do a flush here and all works fine:
        //em.flush();
        em.merge(order);
        //Prior to fix, an exception occurred on the commit.
        em.getTransaction().commit();

        em.clear();

        //OK, good, we no longer get an exception, to be double certain
        //all is well, lets verify that the expected LineItems are in the DB.
        LineItemPK liPK = new LineItemPK();
        liPK.setItemId(1L);
        liPK.setOrderId(1L);
        LineItem li = em.find(LineItem.class, liPK);

        assertNotNull(li);
        assertEquals(item.getProductName(), li.getProductName());

        liPK.setItemId(2L);
        liPK.setOrderId(1L);
        li = em.find(LineItem.class, liPK);
        assertNotNull(li);
        assertEquals(additional.getProductName(), li.getProductName());

        em.close();
  }
}
