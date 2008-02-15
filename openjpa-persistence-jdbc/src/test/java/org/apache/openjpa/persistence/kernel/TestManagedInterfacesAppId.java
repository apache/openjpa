/*
 * TestManagedInterfacesAppId.java
 *
 * Created on October 16, 2006, 4:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;



import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceAppId;
import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceEmbed;
import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceOwnerAppId;
import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceSupAppId;
import org.apache.openjpa.persistence.kernel.common.apps.RuntimeTest1;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;

public class TestManagedInterfacesAppId extends BaseKernelTest {

    /**
     * Creates a new instance of TestManagedInterfacesAppId
     */
    public TestManagedInterfacesAppId(String name) {
        super(name);
    }

    public void setUp() {
        deleteAll(ManagedInterfaceSupAppId.class);
        deleteAll(ManagedInterfaceOwnerAppId.class);
        deleteAll(RuntimeTest1.class);
    }

    public void testManagedInterface() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        ManagedInterfaceAppId pc = (ManagedInterfaceAppId) pm.createInstance(
            (ManagedInterfaceAppId.class));
        pc.setId1(9);
        pc.setId2(19);
        pc.setIntFieldSup(3);
        pc.setIntField(4);

        pc.setEmbed((ManagedInterfaceEmbed) pm.createInstance
            (ManagedInterfaceEmbed.class));

        pc.getEmbed().setIntField(5);
        assertEquals(5, pc.getEmbed().getIntField());
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        assertEquals(9, pc.getId1());
        assertEquals(19, pc.getId2());
        assertEquals(3, pc.getIntFieldSup());
        assertEquals(4, pc.getIntField());
        assertEquals(5, pc.getEmbed().getIntField());
        startTx(pm);
        pc.setIntField(14);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        Object newId = new ManagedInterfaceSupAppId.Id("9,19");
        pc =
            (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, newId);
        assertEquals(9, pc.getId1());
        assertEquals(19, pc.getId2());
        assertEquals(3, pc.getIntFieldSup());
        assertEquals(14, pc.getIntField());
        assertEquals(5, pc.getEmbed().getIntField());
        endEm(pm);

        pm = getPM();
        startTx(pm);
        OpenJPAQuery query =
            pm.createNativeQuery("intField==14", ManagedInterfaceAppId.class);
        Collection c = (Collection) query.getResultList();
        assertEquals(1, c.size());
        pc = (ManagedInterfaceAppId) c.iterator().next();
        assertEquals(14, pc.getIntField());
        pm.remove(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        try {
            pm.find(ManagedInterfaceAppId.class, oid);
            fail();
        } catch (Exception onfe) {
        }

        endEm(pm);
    }

    public void testInterfaceOwner() {
        OpenJPAEntityManager pm = getPM();
        ManagedInterfaceOwnerAppId pc = new ManagedInterfaceOwnerAppId();
        pc.setIFace((ManagedInterfaceSupAppId) pm.createInstance
            (ManagedInterfaceSupAppId.class));
        pc.getIFace().setIntFieldSup(3);

        startTx(pm);
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        pc = (ManagedInterfaceOwnerAppId) pm
            .find(ManagedInterfaceOwnerAppId.class, oid);
        assertEquals(3, pc.getIFace().getIntFieldSup());
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceOwnerAppId) pm
            .find(ManagedInterfaceOwnerAppId.class, oid);
        assertEquals(3, pc.getIFace().getIntFieldSup());
        endEm(pm);

        pm = getPM();
        startTx(pm);
        OpenJPAQuery q = pm.createNativeQuery("iface.intFieldSup==3",
            ManagedInterfaceOwnerAppId.class);
        Collection c = (Collection) q.getResultList();
        pc = (ManagedInterfaceOwnerAppId) c.iterator().next();
        assertEquals(3, pc.getIFace().getIntFieldSup());

        pc.getIFace().setIntFieldSup(13);
        assertEquals(13, pc.getIFace().getIntFieldSup());
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceOwnerAppId) pm
            .find(ManagedInterfaceOwnerAppId.class, oid);
        assertEquals(13, pc.getIFace().getIntFieldSup());
        endEm(pm);
    }

    public void testCollection() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        ManagedInterfaceAppId pc = (ManagedInterfaceAppId) pm.createInstance
            (ManagedInterfaceAppId.class);
        Set set = new HashSet();
        set.add(new Integer(3));
        set.add(new Integer(4));
        set.add(new Integer(5));
        pc.setSetInteger(set);
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetInteger();
        assertEquals(3, set.size());
        assertTrue(set.contains(new Integer(3)));
        assertTrue(set.contains(new Integer(4)));
        assertTrue(set.contains(new Integer(5)));
        startTx(pm);
        set.remove(new Integer(4));
        set.add(new Integer(14));
        set.add(new Integer(15));
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetInteger();
        assertEquals(4, set.size());
        assertTrue(set.contains(new Integer(3)));
        assertTrue(set.contains(new Integer(5)));
        assertTrue(set.contains(new Integer(14)));
        assertTrue(set.contains(new Integer(15)));
        startTx(pm);
        pc.setSetInteger(null);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetInteger();
        assertTrue(set == null || set.size() == 0);
        endEm(pm);
    }

    public void testCollectionPC() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        ManagedInterfaceAppId pc = (ManagedInterfaceAppId) pm.createInstance
            (ManagedInterfaceAppId.class);
        Set set = new HashSet();
        set.add(new RuntimeTest1("a", 3));
        set.add(new RuntimeTest1("b", 4));
        set.add(new RuntimeTest1("c", 5));
        pc.setSetPC(set);
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetPC();
        assertEquals(3, set.size());
        Collection seen = new ArrayList();
        RuntimeTest1 rel;
        RuntimeTest1 toRem = null;
        for (Iterator it = set.iterator(); it.hasNext();) {
            rel = (RuntimeTest1) it.next();
            seen.add(rel.getStringField());
            if (rel.getIntField() == 4)
                toRem = rel;
        }
        assertEquals(3, seen.size());
        assertTrue(seen.contains("a"));
        assertTrue(seen.contains("b"));
        assertTrue(seen.contains("c"));
        startTx(pm);
        assertNotNull(toRem);
        set.remove(toRem);
        set.add(new RuntimeTest1("x", 14));
        set.add(new RuntimeTest1("y", 15));
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetPC();
        assertEquals(4, set.size());
        seen.clear();
        for (Iterator it = set.iterator(); it.hasNext();) {
            rel = (RuntimeTest1) it.next();
            seen.add(rel.getStringField());
        }
        assertEquals(4, seen.size());
        assertTrue(seen.contains("a"));
        assertTrue(seen.contains("c"));
        assertTrue(seen.contains("x"));
        assertTrue(seen.contains("y"));
        startTx(pm);
        pc.setSetPC(null);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetPC();
        assertTrue(set == null || set.size() == 0);
        endEm(pm);
    }

    public void testCollectionInterfaces() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        ManagedInterfaceAppId pc = (ManagedInterfaceAppId) pm.createInstance
            (ManagedInterfaceAppId.class);
        Set set = new HashSet();
        set.add(createInstance(pm, 3));
        set.add(createInstance(pm, 4));
        set.add(createInstance(pm, 5));
        pc.setSetI(set);
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetI();
        assertEquals(3, set.size());
        Collection seen = new ArrayList();
        ManagedInterfaceAppId rel = null;
        ManagedInterfaceAppId toRem = null;
        for (Iterator it = set.iterator(); it.hasNext();) {
            rel = (ManagedInterfaceAppId) it.next();
            seen.add(new Integer(rel.getIntField()));
            if (rel.getIntField() == 4)
                toRem = rel;
        }
        assertEquals(3, seen.size());
        assertTrue(seen.contains(new Integer(3)));
        assertTrue(seen.contains(new Integer(4)));
        assertTrue(seen.contains(new Integer(5)));
        startTx(pm);
        assertNotNull(toRem);
        set.remove(toRem);
        set.add(createInstance(pm, 14));
        set.add(createInstance(pm, 15));
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetI();
        assertEquals(4, set.size());
        seen.clear();
        for (Iterator it = set.iterator(); it.hasNext();) {
            rel = (ManagedInterfaceAppId) it.next();
            seen.add(new Integer(rel.getIntField()));
        }
        assertEquals(4, seen.size());
        assertTrue(seen.contains(new Integer(3)));
        assertTrue(seen.contains(new Integer(5)));
        assertTrue(seen.contains(new Integer(14)));
        assertTrue(seen.contains(new Integer(15)));
        startTx(pm);
        pc.setSetPC(null);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        set = pc.getSetPC();
        assertTrue(set == null || set.size() == 0);
        endEm(pm);
    }

    public void testUnimplementedThrowsException() {
        OpenJPAEntityManager pm = getPM();
        ManagedInterfaceAppId pc = createInstance(pm, 1);
        try {
            pc.unimplemented();
            fail("Exception expected.");
        } catch (Exception jdoe) {
        } // good
        endEm(pm);
    }

    public void testDetach() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        ManagedInterfaceAppId pc = createInstance(pm, 4);
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        pc = (ManagedInterfaceAppId) pm.detach(pc);
        endEm(pm);

        assertTrue(pm.isDetached(pc));
        pc.setIntField(7);

        pm = getPM();
        startTx(pm);
        pm.persist(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = (ManagedInterfaceAppId) pm.find(ManagedInterfaceAppId.class, oid);
        assertEquals(7, pc.getIntField());
        endEm(pm);
    }

    private ManagedInterfaceAppId createInstance(OpenJPAEntityManager pm,
        int i) {
        ManagedInterfaceAppId pc = (ManagedInterfaceAppId) pm
            .createInstance(ManagedInterfaceAppId.class);
        pc.setId1(i * 10);
        pc.setId2(i * -10);
        pc.setIntField(i);
        return pc;
    }
}
