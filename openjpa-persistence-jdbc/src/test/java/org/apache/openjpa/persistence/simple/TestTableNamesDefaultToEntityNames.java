/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.simple;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestTableNamesDefaultToEntityNames
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(NamedEntity.class);
    }

    public void testEntityNames() {
        ClassMapping cm = (ClassMapping) OpenJPAPersistence.getMetaData(
            emf, NamedEntity.class);
        assertEquals("named", cm.getTable().getName());
    }
}
