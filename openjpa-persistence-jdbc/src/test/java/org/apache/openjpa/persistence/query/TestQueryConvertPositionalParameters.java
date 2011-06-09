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
package org.apache.openjpa.persistence.query;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * This test was added for OPENJPA-1999. 'Add' support for allowing positional parameters to start at something other
 * than 1 and allow for missing parameters.
 */
public class TestQueryConvertPositionalParameters extends SingleEMFTestCase {
    EntityManager _em;
    long _id;
    String _name;

    @Override
    public void setUp() {
        super.setUp(SimpleEntity.class, "openjpa.Compatibility", "ConvertPositionalParametersToNamed=true");
        _em = emf.createEntityManager();
        
        _em.getTransaction().begin();
        SimpleEntity se = new SimpleEntity();
        _name = "name--" + System.currentTimeMillis();
        se.setName(_name);
        _em.persist(se);
        _em.getTransaction().commit();
        _id = se.getId();
        _em.clear();
    }

    @Override
    public void tearDown() throws Exception {
        if (_em.getTransaction().isActive()) {
            _em.getTransaction().rollback();
        }
        _em.close();
        // TODO Auto-generated method stub
        super.tearDown();
    }

    public void testNamedPositionalStartAtNonOne() {
        SimpleEntity se =
            _em.createNamedQuery("SelectWithPositionalParameterNonOneStart", SimpleEntity.class).setParameter(900, _id)
                .setParameter(2, _name).getSingleResult();
        assertNotNull(se);
    }

    public void testJPQLPositionalStartAtNonOne() {
        int idPos = 7;
        int namePos = 908;
        SimpleEntity se =
            _em.createQuery("Select s FROM simple s where s.id=?" + idPos + " and s.name=?" + namePos,
                SimpleEntity.class).setParameter(idPos, _id).setParameter(namePos, _name).getSingleResult();
        assertNotNull(se);
    }

}
