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
package org.apache.openjpa.persistence.inheritance;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.kernel.AbstractBrokerFactory;

/**
 * Test that entities, mapped superclasses, and embeddables can all share
 * the same short names without any collisions.
 */
public class TestSharedUnqualifiedClassNames
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(org.apache.openjpa.persistence.inheritance.mappedsuperclass
                .SharedName1.class,
            org.apache.openjpa.persistence.inheritance.entity
                .SharedName1.class,
            org.apache.openjpa.persistence.inheritance.embeddable
                .SharedName2.class,
            org.apache.openjpa.persistence.inheritance.entity
                .SharedName2.class);
        emf.createEntityManager().close();
    }

        public void testMappedSuperclass() {
        ClassMetaData meta = emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData("SharedName1", getClass().getClassLoader(), true);
        assertEquals(
            org.apache.openjpa.persistence.inheritance.entity.SharedName1.class,
            meta.getDescribedType());
    }

    public void testEmbeddable() {
        ClassMetaData meta = emf.getConfiguration()
            .getMetaDataRepositoryInstance()
            .getMetaData("SharedName2", getClass().getClassLoader(), true);
        assertEquals(
            org.apache.openjpa.persistence.inheritance.entity.SharedName2.class,
            meta.getDescribedType());
    }
}
