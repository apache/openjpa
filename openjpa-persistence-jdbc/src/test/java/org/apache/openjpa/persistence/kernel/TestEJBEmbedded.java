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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.kernel.common.apps.ComplexEmbeddedPC;
import org.apache.openjpa.persistence.kernel.common.apps.EmbeddedOwnerPC;
import org.apache.openjpa.persistence.kernel.common.apps.EmbeddedPC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestEJBEmbedded extends BaseKernelTest {

    private Object _oid1 = null;
    private Object _oid2 = null;

    EmbeddedOwnerPC.EmbKey id1;
    EmbeddedOwnerPC.EmbKey id2;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        deleteAll(EmbeddedOwnerPC.class);
        deleteAll(EmbeddedPC.class);

        EntityManager em = currentEntityManager();
        startTx(em);

        EmbeddedOwnerPC pc1 = new EmbeddedOwnerPC(1, 2);
        pc1.setStringField("string1");
        EmbeddedPC embed1 = new EmbeddedPC();
        embed1.setStringField("embedString1");
        embed1.setIntField(1);
        pc1.setEmbedded(embed1);

        EmbeddedPC embed2 = new EmbeddedPC();
        embed2.setStringField("embedString2");
        embed2.setIntField(2);

        EmbeddedOwnerPC pc2 = new EmbeddedOwnerPC(3, 4);

        em.persist(pc1);
        em.persist(pc2);

        endTx(em);

        id1 = new EmbeddedOwnerPC.EmbKey("1:2");

        id2 = new EmbeddedOwnerPC.EmbKey();
        id2.id1 = 3;
        id2.id2 = 4;

        endEm(em);
    }

    @Test
    public void testInsert() {
        EntityManager pm = currentEntityManager();
        startTx(pm);

        EmbeddedOwnerPC pc = pm.find(EmbeddedOwnerPC.class, id1);
        EmbeddedOwnerPC pc2 = pm.find(EmbeddedOwnerPC.class, id2);
        assertNotNull(pc, "pc is null in testInsert");
        assertNotNull(pc2, "p2 is null in testInsert");

        assertEquals("string1", pc.getStringField());
        assertNotNull(pc.getEmbedded());
        assertEquals("embedString1", pc.getEmbedded().getStringField());
        assertEquals(1, pc.getEmbedded().getIntField());

        endTx(pm);
        endEm(pm);
    }

    @Test
    public void testDelete() {
        EntityManager pm = currentEntityManager();
        startTx(pm);
        EmbeddedOwnerPC pc =
            (EmbeddedOwnerPC) pm.find(EmbeddedOwnerPC.class, id1);

        assertNotNull(pc, "pc is null in testDelete");

        EmbeddedPC embed = pc.getEmbedded();

        pc.setEmbedded(null);

        endTx(pm);

        endEm(pm);

        pm = currentEntityManager();
        startTx(pm);
        pc = (EmbeddedOwnerPC) pm.find(EmbeddedOwnerPC.class, id1);

        assertEquals("string1", pc.getStringField(),
            "the expt strng is not string1");
        assertNull(pc.getEmbedded(), "pc's embedded is null");
        endTx(pm);
        endEm(pm);
    }

    @Test
    public void testUpdateRetain() {
        EntityManager pm = currentEntityManager();
        startTx(pm);
        EmbeddedOwnerPC pc =
            (EmbeddedOwnerPC) pm.find(EmbeddedOwnerPC.class, id1);

        assertNotNull(pc, "pc is null testUpdateRetain");

        pc.setStringField("string2");
        pc.getEmbedded().setStringField("embedString2");

        endTx(pm);

        assertEquals("string2", pc.getStringField());
        assertNotNull(pc.getEmbedded());
        assertEquals("embedString2", pc.getEmbedded().getStringField());
        assertEquals(1, pc.getEmbedded().getIntField());

        endEm(pm);
    }

    @Test
    public void testReplace() {
        EntityManager pm = currentEntityManager();
        startTx(pm);

        EmbeddedOwnerPC pc = pm.find(EmbeddedOwnerPC.class, id1);

        EmbeddedPC newEmbed = new EmbeddedPC();
        newEmbed.setStringField("embedString2");
        pc.setEmbedded(newEmbed);

        ComplexEmbeddedPC newComplexEmbed = new ComplexEmbeddedPC();
        newEmbed = new EmbeddedPC();
        newEmbed.setStringField("embedString3");
        newComplexEmbed.setStringField("complexEmbedString3");
        newComplexEmbed.setOwnerField(new EmbeddedOwnerPC(5, 6));
        pc.setComplexEmbedded(newComplexEmbed);

        endTx(pm);
        endEm(pm);

        pm = currentEntityManager();
        startTx(pm);

        pc = pm.find(EmbeddedOwnerPC.class, id1);
        assertEquals("string1", pc.getStringField());
        assertNotNull(pc.getEmbedded());
        assertEquals("embedString1", pc.getEmbedded().getStringField(),
            "the exp strng is not embedString1");
        assertEquals(1, pc.getEmbedded().getIntField(), "intfield is not 1");

        endTx(pm);
        endEm(pm);
    }

    @Test
    public void testShare() {
        EntityManager pm = currentEntityManager();
        startTx(pm);

        EmbeddedOwnerPC pc1 = pm.find(EmbeddedOwnerPC.class, id1);
        assertNotNull(pc1, "pc1 is null in testshare");

        EmbeddedOwnerPC pc2 = pm.find(EmbeddedOwnerPC.class, id2);
        assertNotNull(pc2, "pc2 is null in testshare");

        EmbeddedPC embed1 = pc1.getEmbedded();

        pm.persist(embed1);
        Integer oid = 1;
        endTx(pm);

        assertEquals("embedString1", embed1.getStringField());
        assertEquals("embedString1", pc1.getEmbedded().getStringField());

        endEm(pm);

        // make sure the changes stick
        pm = currentEntityManager();
        startTx(pm);
        pc1 = pm.find(EmbeddedOwnerPC.class, id1);
        assertNotNull(pc1, "pc1 is null in testshare 2nd find");
        pc2 = pm.find(EmbeddedOwnerPC.class, id2);
        assertNotNull(pc2, "pc2 is null in testshare 2nd find");
        embed1 = pm.find(EmbeddedPC.class, oid);

        if (embed1 != null) {
            assertEquals("embedString1", embed1.getStringField());
            assertEquals("embedString1", pc1.getEmbedded().getStringField());
        }

        endTx(pm);
        endEm(pm);
    }

    @Test
    public void testOptimisticLocking2() {
        EntityManager pm1 = getPM(true, false);
        startTx(pm1);

        EmbeddedOwnerPC pc1 = pm1.find(EmbeddedOwnerPC.class, id1);
        assertNotNull(pc1, "pc1 is null in testoptlock2");

        EntityManager pm2 = currentEntityManager();

        startTx(pm2);
        EmbeddedOwnerPC pc2 = pm1.find(EmbeddedOwnerPC.class, id1);

        assertNotNull(pc2, "pc2 is null in testoptlock2");

        EmbeddedPC embed2 = pc2.getEmbedded();
        embed2.setStringField("xxxx");

        endTx(pm2);
        endEm(pm2);

        EmbeddedPC embed1 = pc1.getEmbedded();
        embed1.setStringField("yyyy");
        try {
            endTx(pm1);
            fail("Should have thrown an OL exception.");
        }
        catch (Exception ove) {
        }
        finally {
            endEm(pm1);
        }
    }
}
