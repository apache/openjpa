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
package org.apache.openjpa.persistence.jdbc.mapping;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestBooleanColumnTypeMapping extends SQLListenerTestCase  {
    public void setUp() { 
        setUp(BooleanTestEntity.class, 
                "openjpa.jdbc.DBDictionary", "(BooleanRepresentation=BOOLEAN)", 
                "openjpa.Log", "DefaultLevel=TRACE",
                "openjpa.ConnectionFactoryProperties", "PrintParameters=true"
                );
    }
    
    public void testBooleanColumnMapping() {
        EntityManager em = emf.createEntityManager();
        
        try {
            em.getTransaction().begin();
            final BooleanTestEntity bte = new BooleanTestEntity();
            bte.setbVal(false);
            em.persist(bte);;
            em.getTransaction().commit();
            
            for (String s : sql) {
                System.out.println(s);
            }
            em.clear();
            
            final BooleanTestEntity bteFind = em.find(BooleanTestEntity.class, bte.getId());
            assertFalse(bteFind.isbVal());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        
        
    }
}
