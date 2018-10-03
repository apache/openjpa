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

package org.apache.openjpa.persistence.enhance;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.enhance.common.apps.EmbeddableEntityWithIDAnnotation;
import org.apache.openjpa.persistence.enhance.common.apps.IDOwningClassTestEntity;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestEmbeddableEntityWithIDAnnotation extends SQLListenerTestCase{

    @Override
    public void setUp() {
        setUp(EmbeddableEntityWithIDAnnotation.class, IDOwningClassTestEntity.class, CLEAR_TABLES);
    }

    //make sure no exception is thrown here.
    public void testpcNewObjectIdInstanceMethod(){
        EntityManager em = emf.createEntityManager();
        try{
            IDOwningClassTestEntity e = new IDOwningClassTestEntity();
            em.getTransaction().begin();
            em.persist(e);
            em.getTransaction().commit();
            assertTrue(em.createQuery("select count(c) from IDOwningClassTestEntity c", Long.class)
                    .getSingleResult().longValue() > 0);
        }finally{
            em.close();
        }
    }

}
