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
package org.apache.openjpa.meta;

import javax.persistence.EntityManagerFactory;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.test.PersistenceTestCase;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.persistence.JPAFacadeHelper;

public class TestMetaDataInheritanceComparator extends PersistenceTestCase {

    public void testMetaDataInheritanceComparator() {
        InheritanceComparator comp = new MetaDataInheritanceComparator();
        comp.setBase(AbstractThing.class);

        EntityManagerFactory emf = createEMF(A.class, B.class, C.class,
            AbstractThing.class);

        ClassMetaData a = JPAFacadeHelper.getMetaData(emf, A.class);
        ClassMetaData b = JPAFacadeHelper.getMetaData(emf, B.class);
        ClassMetaData c = JPAFacadeHelper.getMetaData(emf, C.class);

        assertEquals(-1, comp.compare(a, b));
        assertEquals(-1, comp.compare(b, c));
        assertEquals(-1, comp.compare(a, c));
    }

    public void testInheritanceComparator() {
        InheritanceComparator comp = new InheritanceComparator();
        comp.setBase(AbstractThing.class);

        assertEquals(-1, comp.compare(A.class, B.class));
        assertEquals(-1, comp.compare(B.class, C.class));
        assertEquals(-1, comp.compare(A.class, C.class));
    }
}
