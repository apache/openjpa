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
package org.apache.openjpa.persistence.generationtype;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestUuidGeneratedEntity extends SingleEMFTestCase {
    
    DBDictionary _dict;

    @Override
    public void setUp() {
        setUp(UuidGeneratedEntity.class, CLEAR_TABLES);
        _dict = ((JDBCConfiguration)emf.getConfiguration()).getDBDictionaryInstance();
    }

    public void testMapping() {
        ClassMapping cm = getMapping(UuidGeneratedEntity.class);
        Column[] cols = cm.getPrimaryKeyColumns();
        assertEquals(1, cols.length);

        Column col = cols[0];
        if (_dict.supportsUuidType) {
            assertEquals(JavaTypes.UUID_OBJ, col.getJavaType());
        } else {
            assertEquals(JavaTypes.STRING, col.getJavaType());
        }
    }

    public void testDefaultValues() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity gv1 = new UuidGeneratedEntity();
        UuidGeneratedEntity gv2 = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(gv1);
        em.persist(gv2);
        em.getTransaction().commit();

        em.refresh(gv1);
        em.refresh(gv2);

        assertNotNull(gv1.getId());
        assertNotNull(gv2.getId());
        assertFalse(gv1.getId().compareTo(gv2.getId()) == 0);
        assertNotNull(gv1.getNativeUuid());
        assertNotNull(gv2.getNativeUuid());
        assertFalse(gv1.getNativeUuid().compareTo(gv2.getNativeUuid()) == 0);
        assertTrue(isHexUUID(gv1.getStringUUID(), 4));
        assertTrue(isHexUUID(gv2.getStringUUID(), 4));
        closeEM(em);
    }

    public void testFindByUUIDProperty() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity gv = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(gv);        
        em.getTransaction().commit();

        UUID nid = gv.getNativeUuid();

        String query = "SELECT u FROM UuidGeneratedEntity AS u WHERE u.nativeUuid = :nid";

        List<UuidGeneratedEntity> list = em
            .createQuery(query, UuidGeneratedEntity.class)
            .setParameter("nid", nid)
            .getResultList();
        
        assertEquals(1, list.size());
        assertEquals(nid, list.get(0).getNativeUuid());

        closeEM(em);
    }

    public void testUpdateUUIDProperty() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity gv = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(gv);        
        em.getTransaction().commit();

        UUID nid = gv.getNativeUuid();

        String query = "SELECT u FROM UuidGeneratedEntity AS u WHERE u.nativeUuid = :nid";

        List<UuidGeneratedEntity> list = em
            .createQuery(query, UuidGeneratedEntity.class)
            .setParameter("nid", nid)
            .getResultList();
        assertEquals(1, list.size());
        UUID changed = UUID.randomUUID();

        em.getTransaction().begin();
        list.get(0).setNativeUuid(changed);
        em.merge(list.get(0));
        em.getTransaction().commit();

        list = em.createQuery(query, UuidGeneratedEntity.class)
                .setParameter("nid", nid)
                .getResultList();
        assertEquals(0, list.size());
        list = em.createQuery(query, UuidGeneratedEntity.class)
                .setParameter("nid", changed)
                .getResultList();
        assertEquals(1, list.size());

        closeEM(em);

    }

    public void testFindByStringUUIDProperty() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity gv = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(gv);        
        em.getTransaction().commit();

        String sid = gv.getStringUUID();

        String query = "SELECT u FROM UuidGeneratedEntity AS u WHERE u.stringUUID = :sid";

        List<UuidGeneratedEntity> list = em
            .createQuery(query, UuidGeneratedEntity.class)
            .setParameter("sid", sid)
            .getResultList();
        
        assertEquals(1, list.size());
        assertEquals(sid, list.get(0).getStringUUID());

        closeEM(em);

    }

    public void testFindByUUID() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity gv = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(gv);        
        em.getTransaction().commit();

        UUID id = gv.getId();

        UuidGeneratedEntity fv = em.find(UuidGeneratedEntity.class, id);
        
        assertNotNull(fv);
        assertEquals(gv.getId(), fv.getId());
        assertEquals(gv.getStringUUID(), fv.getStringUUID());
        assertEquals(gv.getNativeUuid(), fv.getNativeUuid());

        closeEM(em);
    }

    public void testRemoveById() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity gv = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(gv);        
        em.getTransaction().commit();

        UUID id = gv.getId();

        UuidGeneratedEntity fv = em.find(UuidGeneratedEntity.class, id);
        
        em.getTransaction().begin();
        em.remove(fv);
        em.getTransaction().commit();

        fv = em.find(UuidGeneratedEntity.class, id);
        assertNull(fv);

        closeEM(em);
    }

    public void testParentRelationshipById() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity parent = new UuidGeneratedEntity();
        UuidGeneratedEntity child = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(parent);
        child.setParent(parent);
        em.persist(child);        
        em.getTransaction().commit();

        assertEquals(parent, child.getParent());
        assertEquals(parent.getId(), child.getParent().getId());

        UUID parentId = parent.getId();
        UUID childId = child.getId();

        String query = "SELECT u FROM UuidGeneratedEntity AS u WHERE u.parent.id = :pid";
        
        List<UuidGeneratedEntity> list = em
            .createQuery(query, UuidGeneratedEntity.class)
            .setParameter("pid", parentId)
            .getResultList();
        assertEquals(1, list.size());
        assertEquals(childId, list.get(0).getId());

        closeEM(em);
    }

    public void testParentRelationshipByEntity() {
        EntityManager em = emf.createEntityManager();

        UuidGeneratedEntity parent = new UuidGeneratedEntity();
        UuidGeneratedEntity child = new UuidGeneratedEntity();

        em.getTransaction().begin();
        em.persist(parent);
        child.setParent(parent);
        em.persist(child);        
        em.getTransaction().commit();

        UUID childId = child.getId();

        String query = "SELECT u FROM UuidGeneratedEntity AS u WHERE u.parent = :parent";
        
        List<UuidGeneratedEntity> list = em
            .createQuery(query, UuidGeneratedEntity.class)
            .setParameter("parent", parent)
            .getResultList();
        assertEquals(1, list.size());
        assertEquals(childId, list.get(0).getId());

        closeEM(em);
    }

    /*
     * Verify a uuid hex string value is 32 characters long, consists entirely
     * of hex digits and is the correct version.
     */
    private boolean isHexUUID(String value, int type) {
        if (value.length() != 32)
            return false;
        char[] chArr = value.toCharArray();
        for (int i = 0; i < 32; i++)
        {
            char ch = chArr[i];
            if (!(Character.isDigit(ch) ||
                (ch >= 'a' && ch <= 'f') ||
                (ch >= 'A' && ch <= 'F')))
                return false;
            if (i == 12) {
                if (type == 1 && ch != '1')
                    return false;
                if (type == 4 && ch != '4')
                    return false;
            }
        }
        return true;
    }

}
