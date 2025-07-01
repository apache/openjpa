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

import java.util.logging.Logger;

import org.apache.openjpa.persistence.common.utils.AbstractTestCase;

public class TestEJBLobs extends AbstractTestCase {
	
	private static final Logger logger = Logger.getLogger(TestEJBLobs.class.getCanonicalName());

    private EntityManager _pm = null;
    private EJBLobsInnerEntity _inner = null;

    public TestEJBLobs(String name) {
        super(name, "kernelcactusapp");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp(EJBLobsInnerEntity.class, EJBLobsInner2Entity.class);

        EntityManager em = currentEntityManager();
        startTx(em);

        endTx(em);
        endEm(em);

        EJBLobsInnerEntity inner = new EJBLobsInnerEntity();
        inner.setString("string");
        inner.setClob("clobField");
        inner.setEBlob("eblob");

        EJBLobsInner2Entity inner2 = new EJBLobsInner2Entity();
        inner2.string = "inner2";
        inner.setBlob(inner2);

        _pm = currentEntityManager();
        startTx(_pm);
        _pm.persist(inner);
        try {
            endTx(_pm);
        }
        catch (Exception jdoe) {
            logger.warning("An exception was thrown while persisting the entity : \n" + getStackTrace(jdoe));
        }
        endEm(_pm);

        _pm = currentEntityManager();
        _inner = _pm.find(EJBLobsInnerEntity.class, "string");
    }

    public void testOtherFields() {
        assertEquals("string", _inner.getString());
    }

    public void testClob() {
        assertEquals("clobField", _inner.getClob());
    }

    public void testBlob() {
        assertNotNull(_inner.getBlob());
        assertEquals("inner2", _inner.getBlob().string);
    }

    public void testSetNull() {
        startTx(_pm);
        _inner.setClob(null);
        _inner.setBlob(null);
        endTx(_pm);

        assertEquals(null, _inner.getBlob());
        assertEquals(null, _inner.getClob());
    }

    public void testDelete() {
        deleteAll(EJBLobsInnerEntity.class);
    }

    public void testUpdate() {
        startTx(_pm);
        _inner.setClob("newvalue");
        EJBLobsInner2Entity inner2 = new EJBLobsInner2Entity();
        inner2.string = "newinner2";
        _inner.setBlob(inner2);
        endTx(_pm);

        assertEquals("newvalue", _inner.getClob());
        assertEquals("newinner2", _inner.getBlob().string);
    }

}
