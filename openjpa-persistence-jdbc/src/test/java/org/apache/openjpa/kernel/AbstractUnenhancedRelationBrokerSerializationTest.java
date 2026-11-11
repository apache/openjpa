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
package org.apache.openjpa.kernel;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.UnenhancedSubtype;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.util.ImplHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractUnenhancedRelationBrokerSerializationTest<T>
    extends AbstractBrokerSerializationTest<T> {

    @Test
    public void testNewUnenhancedSMsRegisteredGlobally() {
        OpenJPAEntityManager em = emf.createEntityManager();
        OpenJPAEntityManager em2 = null;
        try {
            em.getTransaction().begin();
            UnenhancedSubtype newe = (UnenhancedSubtype) newManagedInstance();
            em.persist(newe);
            em2 = deserializeEM(serialize(em));

            for (Object o : em2.getManagedObjects()) {
                Assertions.assertFalse(o instanceof PersistenceCapable);
                Assertions.assertNotNull(ImplHelper.toPersistenceCapable(o,
                    emf.getConfiguration()));
                if (o instanceof UnenhancedSubtype)
                    Assertions.assertNotNull(ImplHelper.toPersistenceCapable(
                        ((UnenhancedSubtype) o).getRelated(),
                            emf.getConfiguration()));
            }
        } finally {
            close(em);
            close(em2);
        }
    }
}
