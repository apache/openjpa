/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.enhance;

import org.apache.openjpa.persistence.enhance.common.apps.MixedAccessPerson;
import org.apache.openjpa.persistence.test.SingleEMTestCase;
import org.junit.Test;

/**
 * Test subclass enhancement on the fly with mixed access strategy
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class TestMixedAccessSubclassing extends SingleEMTestCase {

    @Override
    public void setUp() {
        super.setUp(
                "openjpa.RuntimeUnenhancedClasses", "supported",
                "openjpa.DynamicEnhancementAgent", "false",
                MixedAccessPerson.class);
    }

    @Test
    public void testDynamicEnhancement() {
        int pk = -1;
        {
            em.getTransaction().begin();
            // step 1: create the person
            MixedAccessPerson p = new MixedAccessPerson();
            p.setFirstName("Han");
            p.setLastName("Solo");
            assertEquals("changed_Solo", p.getLastName());
            em.persist(p);
            em.flush();
            pk = p.getId();
            em.clear();
            em.getTransaction().commit();
        }

        {
            em.getTransaction().begin();
            // step 2: read back the person
            MixedAccessPerson p2 = em.find(MixedAccessPerson.class, pk);
            assertNotNull(p2);

            assertEquals(pk, p2.getId());
            assertEquals("Han", p2.getFirstName());

            // since we use property access the setter will get invoked again -> 2x changed_
            assertEquals("changed_changed_Solo", p2.getLastName());
            em.getTransaction().commit();
        }
    }
}
