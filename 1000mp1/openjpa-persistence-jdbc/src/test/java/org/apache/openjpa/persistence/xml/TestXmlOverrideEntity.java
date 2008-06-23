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
package org.apache.openjpa.persistence.xml;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestXmlOverrideEntity extends SingleEMFTestCase {

    public void setUp() {
        setUp(XmlOverrideEntity.class);
    }

    /**
     * Tests that the optional attribute on a basic field can be overrided by
     * an xml descriptor. 
     * 
     * XmlOverrideEntity.name is annotated with optional=false
     * XmlOverrideEntity.description is annotated with optional=true. 
     * 
     * The optional attributes are reversed in orm.xml. 
     */
    public void testOptionalAttributeOverride() {
        EntityManager em = emf.createEntityManager();

        XmlOverrideEntity optional = new XmlOverrideEntity();

        optional.setName(null);
        optional.setDescription("description");

        em.getTransaction().begin();
        em.persist(optional);
        em.getTransaction().commit();

        try {
            em.getTransaction().begin();
            optional.setDescription(null);
            em.getTransaction().commit();
            fail("XmlOrverrideEntity.description should not be optional. "
                    + "Expecting an InvalidStateException.");
        } catch (InvalidStateException e) {
        }

        em.getTransaction().begin();
        em.remove(em.find(XmlOverrideEntity.class, optional.getId()));
        em.getTransaction().commit();
    }
}

