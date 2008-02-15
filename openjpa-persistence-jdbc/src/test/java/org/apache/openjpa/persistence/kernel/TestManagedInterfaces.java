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

import java.util.List;


import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterface;
import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceEmbed;
import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceOwner;
import org.apache.openjpa.persistence.kernel.common.apps.ManagedInterfaceSup;
import org.apache.openjpa.persistence.kernel.common.apps.MixedInterface;
import org.apache.openjpa.persistence.kernel.common.apps.MixedInterfaceImpl;
import org.apache.openjpa.persistence.kernel.common.apps.NonMappedInterfaceImpl;
import org.apache.openjpa.persistence.kernel.common.apps.RuntimeTest1;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;

public class TestManagedInterfaces extends BaseKernelTest {

    public void setUp() {
        deleteAll(ManagedInterfaceSup.class);
        deleteAll(ManagedInterfaceOwner.class);
        deleteAll(MixedInterface.class);
        deleteAll(MixedInterfaceImpl.class);
        deleteAll(NonMappedInterfaceImpl.class);
        deleteAll(RuntimeTest1.class);
    }

    public void testManagedInterface() {
        OpenJPAEntityManager pm = getPM();
        startTx(pm);
        ManagedInterface pc =
            (ManagedInterface) pm.createInstance(ManagedInterface.class);
        pc.setIntFieldSup(3);
        pc.setIntField(4);
        pc.setEmbed((ManagedInterfaceEmbed) pm
            .createInstance(ManagedInterfaceEmbed.class));

        pc.getEmbed().setIntField(5);
        assertEquals(5, pc.getEmbed().getIntField());
        pm.persist(pc);
        Object oid = pm.getObjectId(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        pc = pm.find(ManagedInterface.class, oid);
        assertEquals(3, pc.getIntFieldSup());
        assertEquals(4, pc.getIntField());
        assertEquals(5, pc.getEmbed().getIntField());
        startTx(pm);
        pc.setIntField(14);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        startTx(pm);

//        Query query = pm.newQuery(ManagedInterface.class, "intField==14");
//        Collection c = (Collection) query.execute();

        OpenJPAQuery query = pm.createQuery(
            "SELECT o FROM ManagedInterface o WHERE o.intField = 14");
        List l = query.getResultList();

        assertEquals(1, l.size());
        pc = (ManagedInterface) l.iterator().next();
        assertEquals(14, pc.getIntField());
        pm.remove(pc);
        endTx(pm);
        endEm(pm);

        pm = getPM();
        try {
            pm.find(ManagedInterface.class, oid);
            fail();
        } catch (Exception onfe) {
        }

        endEm(pm);
    }

//    public void testInterfaceOwner() {
//        OpenJPAEntityManager pm = getPM();
//        ManagedInterfaceOwner pc = new ManagedInterfaceOwner();
//        pc.setIFace((ManagedInterfaceSup) pm.createInstance 
//            (ManagedInterfaceSup.class));
//        pc.setEmbed((ManagedInterfaceEmbed) pm.createInstance 
//            (ManagedInterfaceEmbed.class));
//        pc.getIFace().setIntFieldSup(3);
//        pc.getEmbed().setIntField(5);
//
//        startTx(pm);
//        pm.persist(pc);
//        Object oid = pm.getObjectId(pc);
//        endTx(pm);
//        pc = (ManagedInterfaceOwner) pm.getObjectById(oid, true);
//        assertEquals(3, pc.getIFace().getIntFieldSup());
//        assertEquals(5, pc.getEmbed().getIntField());
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterfaceOwner) pm.getObjectById(oid, true);
//        assertEquals(3, pc.getIFace().getIntFieldSup());
//        assertEquals(5, pc.getEmbed().getIntField());
//        endEm(pm);
//
//        pm = getPM();
//        startTx(pm);
//        Query q = pm.newQuery(ManagedInterfaceOwner.class, 
//            "iface.intFieldSup==3 && embed.intField==5");
//        Collection c = (Collection) q.execute();
//        pc = (ManagedInterfaceOwner) c.iterator().next();
//        assertEquals(3, pc.getIFace().getIntFieldSup());
//        assertEquals(5, pc.getEmbed().getIntField());
//
//        pc.getIFace().setIntFieldSup(13);
//        pc.getEmbed().setIntField(15);
//        assertEquals(13, pc.getIFace().getIntFieldSup());
//        assertEquals(15, pc.getEmbed().getIntField());
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterfaceOwner) pm.getObjectById(oid, true);
//        assertEquals(13, pc.getIFace().getIntFieldSup());
//        assertEquals(15, pc.getEmbed().getIntField());
//        endEm(pm);
//    }
//
//    public void testCollection() {
//        OpenJPAEntityManager pm = getPM();
//        startTx(pm);
//        ManagedInterface pc = (ManagedInterface) pm.createInstance
//            (ManagedInterface.class);
//        Set set = new HashSet();
//        set.add(new Integer(3));
//        set.add(new Integer(4));
//        set.add(new Integer(5));
//        pc.setSetInteger(set);
//        pm.persist(pc);
//        Object oid = pm.getObjectId(pc);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetInteger();
//        assertEquals(3, set.size());
//        assertTrue(set.contains(new Integer(3)));
//        assertTrue(set.contains(new Integer(4)));
//        assertTrue(set.contains(new Integer(5)));
//        startTx(pm);
//        set.remove(new Integer(4));
//        set.add(new Integer(14));
//        set.add(new Integer(15));
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetInteger();
//        assertEquals(4, set.size());
//        assertTrue(set.contains(new Integer(3)));
//        assertTrue(set.contains(new Integer(5)));
//        assertTrue(set.contains(new Integer(14)));
//        assertTrue(set.contains(new Integer(15)));
//        startTx(pm);
//        pc.setSetInteger(null);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetInteger();
//        assertTrue (set == null || set.size() == 0);
//        endEm(pm);
//    }
//
//    public void testCollectionPC() {
//        OpenJPAEntityManager pm = getPM();
//        startTx(pm);
//        ManagedInterface pc = (ManagedInterface) pm.createInstance
//            (ManagedInterface.class);
//        Set set = new HashSet();
//        set.add(new RuntimeTest1("a", 3));
//        set.add(new RuntimeTest1("b", 4));
//        set.add(new RuntimeTest1("c", 5));
//        pc.setSetPC(set);
//        pm.persist(pc);
//        Object oid = pm.getObjectId(pc);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetPC();
//        assertEquals(3, set.size());
//        Collection seen = new ArrayList();
//        RuntimeTest1 rel;
//        RuntimeTest1 toRem = null;
//        for (Iterator it = set.iterator(); it.hasNext();) {
//            rel = (RuntimeTest1) it.next();
//            seen.add(rel.getStringField());
//            if (rel.getIntField() == 4)
//                toRem = rel;
//        }
//        assertEquals(3, seen.size());
//        assertTrue(seen.contains("a"));
//        assertTrue(seen.contains("b"));
//        assertTrue(seen.contains("c"));
//        startTx(pm);
//        assertNotNull(toRem);
//        set.remove(toRem);
//        set.add(new RuntimeTest1("x", 14));
//        set.add(new RuntimeTest1("y", 15));
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetPC();
//        assertEquals(4, set.size());
//        seen.clear();
//        for (Iterator it = set.iterator(); it.hasNext();) {
//            rel = (RuntimeTest1) it.next();
//            seen.add(rel.getStringField());
//        }
//        assertEquals(4, seen.size());
//        assertTrue(seen.contains("a"));
//        assertTrue(seen.contains("c"));
//        assertTrue(seen.contains("x"));
//        assertTrue(seen.contains("y"));
//        startTx(pm);
//        pc.setSetPC(null);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetPC();
//        assertTrue (set == null || set.size() == 0);
//        endEm(pm);
//    }
//
//    public void testCollectionInterfaces() {
//        OpenJPAEntityManager pm = getPM();
//        startTx(pm);
//        ManagedInterface pc = (ManagedInterface) pm.createInstance
//            (ManagedInterface.class);
//        Set set = new HashSet();
//        set.add(createInstance(pm, 3));
//        set.add(createInstance(pm, 4));
//        set.add(createInstance(pm, 5));
//        pc.setSetI(set);
//        pm.persist(pc);
//        Object oid = pm.getObjectId(pc);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetI();
//        assertEquals(3, set.size());
//        Collection seen = new ArrayList();
//        ManagedInterface rel = null;
//        ManagedInterface toRem = null;
//        for (Iterator it = set.iterator(); it.hasNext();) {
//            rel = (ManagedInterface) it.next();
//            seen.add(new Integer(rel.getIntField()));
//            if (rel.getIntField() == 4)
//                toRem = rel;
//        }
//        assertEquals(3, seen.size());
//        assertTrue(seen.contains(new Integer(3)));
//        assertTrue(seen.contains(new Integer(4)));
//        assertTrue(seen.contains(new Integer(5)));
//        startTx(pm);
//        assertNotNull(toRem);
//        set.remove(toRem);
//        set.add(createInstance(pm, 14));
//        set.add(createInstance(pm, 15));
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetI();
//        assertEquals(4, set.size());
//        seen.clear();
//        for (Iterator it = set.iterator(); it.hasNext();) {
//            rel = (ManagedInterface) it.next();
//            seen.add(new Integer(rel.getIntField()));
//        }
//        assertEquals(4, seen.size());
//        assertTrue(seen.contains(new Integer(3)));
//        assertTrue(seen.contains(new Integer(5)));
//        assertTrue(seen.contains(new Integer(14)));
//        assertTrue(seen.contains(new Integer(15)));
//        startTx(pm);
//        pc.setSetPC(null);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        set = pc.getSetPC();
//        assertTrue (set == null || set.size() == 0);
//        endEm(pm);
//    }
//
//    public void testMixedQuery() {
//        createMixed();
//
//        OpenJPAEntityManager pm = getPM();
//        Query q = pm.newQuery(MixedInterface.class, "intField==4");
//        Collection c = (Collection) q.execute();
//        Set seen = new HashSet();
//        assertEquals(2, c.size());
//        MixedInterface pc;
//        for (Iterator it = c.iterator(); it.hasNext();) {
//            pc = (MixedInterface) it.next();
//            assertEquals(4, pc.getIntField());
//            seen.add(pc.getClass());
//        }
//        assertEquals(2, seen.size());
//        endEm(pm);
//    }
//
//    public void testMixedExtent() {
//        createMixed();
//
//        OpenJPAEntityManager pm = getPM();
//        Extent e = pm.getExtent(MixedInterface.class, true);
//        Set seen = new HashSet();
//        int size = 0;
//        for (Iterator it = e.iterator(); it.hasNext();) {
//            seen.add(it.next().getClass());
//            size++;
//        }
//        assertEquals(3, size);
//        assertEquals(2, seen.size());
//
//        e = pm.getExtent(MixedInterface.class, false);
//        seen = new HashSet();
//        size = 0;
//        for (Iterator it = e.iterator(); it.hasNext();) {
//            seen.add(it.next().getClass()); 
//            size++;
//        }
//        assertEquals(1, size);
//        assertNotEquals(MixedInterfaceImpl.class, seen.iterator().next());
//        endEm(pm);
//    }
//
//    private void createMixed() {
//        OpenJPAEntityManager pm = getPM();
//        startTx(pm);
//        MixedInterface pc = (MixedInterface) pm.createInstance
//            (MixedInterface.class);
//        pc.setIntField(4);
//        pm.persist(pc);
//        pc = new MixedInterfaceImpl();
//        pc.setIntField(4);
//        pm.persist(pc);
//        pc = new MixedInterfaceImpl();
//        pc.setIntField(8);
//        pm.persist(pc);
//        endTx(pm);
//        endEm(pm);
//    }
//
//    public void testUnimplementedThrowsException() {
//        OpenJPAEntityManager pm = getPM();
//        ManagedInterface pc = createInstance(pm, 1);
//        try {
//            pc.unimplemented();
//            fail("Exception expected.");
//        } catch (JDOUserException jdoe) {} // good
//        endEm(pm);
//    }
//
//    public void testNonMappedcreateInstanceException() {
//        OpenJPAEntityManager pm = getPM();
//        try {
//            pm.createInstance(NonMappedInterface.class);
//            fail("Exception expected");
//        } catch (JDOUserException jdoe) {} // good
//        endEm(pm);
//    }
//
//    public void testNonMappedPropertyAlias() 
//        throws Exception {
//        Object oid = createNonMappedImpl();
//        OpenJPAEntityManager pm = getPM();
//        Query q = pm.newQuery(NonMappedInterface.class, "intField==4");
//        Collection c = (Collection) q.execute();
//        assertEquals(1, c.size());
//        assertEquals(oid, pm.getObjectId(c.iterator().next()));
//        endEm(pm);
//    }
//
//    public void testNonMappedPropertyAliasInMemory() 
//        throws Exception {
//        Object oid = createNonMappedImpl();
//        OpenJPAEntityManager pm = getPM();
//        Query q = pm.newQuery(NonMappedInterface.class, "intField==4");
//        q.setCandidates((Collection) pm.newQuery(NonMappedInterfaceImpl.class).
//            execute());
//        Collection c = (Collection) q.execute();
//        assertEquals(1, c.size());
//        assertEquals(oid, pm.getObjectId(c.iterator().next()));
//        endEm(pm);
//    }
//
//    private Object createNonMappedImpl() 
//        throws Exception {
//        // load non-mapped-impl
//        Class.forName(NonMappedInterfaceImpl.class.getNametrue,
//            NonMappedInterfaceImpl.class.getClassLoader());
//
//     
//        OpenJPAEntityManager pm = getPM();
//        startTx(pm);
//        NonMappedInterface pc = new NonMappedInterfaceImpl();
//        pc.setIntField(4);
//        pm.persist(pc);
//        Object oid = pm.getObjectId(pc);
//        pc = new NonMappedInterfaceImpl();
//        pc.setIntField(8);
//        pm.persist(pc);
//        endTx(pm);
//        endEm(pm);
//        return oid;
//    }
//
//    public void testDetach() {
//        KodoOpenJPAEntityManager pm = getPM();
//        startTx(pm);
//        ManagedInterface pc = createInstance(pm, 4);
//        pm.persist(pc);
//        Object oid = pm.getObjectId(pc);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        ManagedInterface pcx = (ManagedInterface) pm.getObjectById(oid, true);
//        pc = (ManagedInterface) pm.detachCopy(pcx);
//        endEm(pm);
//
//        assertTrue(pm.isDetached(pc));
//        pc.setIntField(7);
//
//        pm = getPM();
//        startTx(pm);
//        pm.persist(pc);
//        endTx(pm);
//        endEm(pm);
//
//        pm = getPM();
//        pc = (ManagedInterface) pm.getObjectById(oid, true);
//        assertEquals(7, pc.getIntField());
//        endEm(pm);
//
//    }

    private ManagedInterface createInstance(OpenJPAEntityManager pm, int i) {
        ManagedInterface pc = (ManagedInterface) pm.createInstance
            (ManagedInterface.class);
        pc.setIntField(i);
        return pc;
    }
}
