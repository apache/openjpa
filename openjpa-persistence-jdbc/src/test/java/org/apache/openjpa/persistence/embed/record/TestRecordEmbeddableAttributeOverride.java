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
package org.apache.openjpa.persistence.embed.record;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests record embeddables with @AttributeOverride to map the same
 * record type to different column names within one entity.
 */
public class TestRecordEmbeddableAttributeOverride extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(WarehouseEntity.class, AddressRecord.class,
                CLEAR_TABLES);
    }

    public void testPersistAndLoadWithAttributeOverride() {
        final AddressRecord ship =
                new AddressRecord("10 Dock Rd", "PortCity", "30001");
        final AddressRecord bill =
                new AddressRecord("99 Finance Ave", "BankTown", "40002");
        final WarehouseEntity wh =
                new WarehouseEntity("Central Warehouse", ship, bill);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(wh);
        em.getTransaction().commit();
        final long id = wh.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final WarehouseEntity loaded =
                em2.find(WarehouseEntity.class, id);
        assertNotNull(loaded);
        assertEquals("Central Warehouse", loaded.getName());

        assertNotNull(loaded.getShippingAddress());
        assertEquals("10 Dock Rd", loaded.getShippingAddress().street());
        assertEquals("PortCity", loaded.getShippingAddress().city());
        assertEquals("30001", loaded.getShippingAddress().zip());

        assertNotNull(loaded.getBillingAddress());
        assertEquals("99 Finance Ave", loaded.getBillingAddress().street());
        assertEquals("BankTown", loaded.getBillingAddress().city());
        assertEquals("40002", loaded.getBillingAddress().zip());
        em2.close();
    }

    public void testUpdateOneOverriddenEmbeddable() {
        final WarehouseEntity wh = new WarehouseEntity("WH-Update",
                new AddressRecord("Old Ship St", "OldCity", "11111"),
                new AddressRecord("Old Bill St", "OldCity", "22222"));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(wh);
        em.getTransaction().commit();
        final long id = wh.getId();
        em.close();

        // Update only the shipping address
        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        final WarehouseEntity managed =
                em2.find(WarehouseEntity.class, id);
        managed.setShippingAddress(
                new AddressRecord("New Ship St", "NewCity", "33333"));
        em2.getTransaction().commit();
        em2.close();

        // Verify shipping changed, billing unchanged
        final EntityManager em3 = emf.createEntityManager();
        final WarehouseEntity updated =
                em3.find(WarehouseEntity.class, id);
        assertEquals("New Ship St", updated.getShippingAddress().street());
        assertEquals("NewCity", updated.getShippingAddress().city());
        assertEquals("33333", updated.getShippingAddress().zip());

        assertEquals("Old Bill St", updated.getBillingAddress().street());
        assertEquals("OldCity", updated.getBillingAddress().city());
        assertEquals("22222", updated.getBillingAddress().zip());
        em3.close();
    }

    public void testDeleteWithAttributeOverride() {
        final WarehouseEntity wh = new WarehouseEntity("WH-Delete",
                new AddressRecord("S", "C", "00000"),
                new AddressRecord("S2", "C2", "99999"));

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(wh);
        em.getTransaction().commit();
        final long id = wh.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        em2.createQuery(
                "DELETE FROM WarehouseEntity w WHERE w.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        em2.getTransaction().commit();
        em2.close();

        final EntityManager em3 = emf.createEntityManager();
        assertNull(em3.find(WarehouseEntity.class, id));
        em3.close();
    }

    public void testOneEmbeddableNull() {
        final WarehouseEntity wh = new WarehouseEntity("WH-Partial",
                new AddressRecord("Ship St", "ShipCity", "55555"),
                null);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(wh);
        em.getTransaction().commit();
        final long id = wh.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final WarehouseEntity loaded =
                em2.find(WarehouseEntity.class, id);
        assertNotNull(loaded);
        assertNotNull(loaded.getShippingAddress());
        assertEquals("Ship St", loaded.getShippingAddress().street());
        em2.close();
    }

    public void testRecordEqualityAcrossOverrides() {
        final AddressRecord same =
                new AddressRecord("Same St", "SameCity", "77777");
        final WarehouseEntity wh =
                new WarehouseEntity("WH-Same", same, same);

        final EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(wh);
        em.getTransaction().commit();
        final long id = wh.getId();
        em.close();

        final EntityManager em2 = emf.createEntityManager();
        final WarehouseEntity loaded =
                em2.find(WarehouseEntity.class, id);

        // Both addresses had the same values — loaded records should be equal
        assertEquals(loaded.getShippingAddress(), loaded.getBillingAddress());
        em2.close();
    }
}
