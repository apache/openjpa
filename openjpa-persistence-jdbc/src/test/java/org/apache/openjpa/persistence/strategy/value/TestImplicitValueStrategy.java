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
package org.apache.openjpa.persistence.strategy.value;

import java.security.Principal;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestImplicitValueStrategy extends SQLListenerTestCase {
    public void setUp(){
        setUp(ImplicitValueStrategyEntity.class, DROP_TABLES,
                "openjpa.jdbc.MappingDefaults",
                "ForeignKeyDeleteAction=restrict, JoinForeignKeyDeleteAction=restrict, " +
                    "FieldStrategies='java.security.Principal=" +
                    "org.apache.openjpa.persistence.strategy.value.PrincipalValueStrategyHandler'",
                "openjpa.RuntimeUnenhancedClasses", "supported"
                );
        assertNotNull(emf);

        create();
    }

    public void testIt() {
        EntityManager em = emf.createEntityManager();
        ImplicitValueStrategyEntity se = em.find(ImplicitValueStrategyEntity.class, "id1");
        assertNotNull(se);
        assertNotNull(se.getUser());
        assertEquals("name1", se.getUser().getName());

        em.close();
    }

    private void create() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        ImplicitValueStrategyEntity stratEnt = new ImplicitValueStrategyEntity();
        stratEnt.setId("id1");
        PrincipalValueStrategyHandler.TestPrincipal user = new PrincipalValueStrategyHandler.TestPrincipal("name1");
        stratEnt.setUser(user);

        em.persist(stratEnt);

        em.getTransaction().commit();
        em.close();
    }
}
