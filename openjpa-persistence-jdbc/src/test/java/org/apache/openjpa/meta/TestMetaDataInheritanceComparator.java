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

import org.apache.openjpa.persistence.test.PersistenceTestCase;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

public class TestMetaDataInheritanceComparator extends PersistenceTestCase {

    public void testInheritanceComparatorWithBase() {
        inheritanceComparatorHelper(true);
    }

    private void inheritanceComparatorHelper(boolean base) {
        InheritanceComparator comp = new InheritanceComparator();
        if (base)
            comp.setBase(AbstractThing.class);

        assertEquals(-1, comp.compare(A.class, B.class));
        assertEquals(-1, comp.compare(B.class, C.class));
        assertTrue(comp.compare(A.class, C.class) < 0);

        assertEquals(-1, comp.compare(AbstractThing.class, A.class));
        assertEquals(-1, comp.compare(AbstractThing.class, B.class));
        assertTrue(comp.compare(AbstractThing.class, C.class) < 0);
    }

    public void testMetaDataInheritanceComparatorWithBase() {
        metaDataInheritanceComparatorHelper(true);
    }

    private void metaDataInheritanceComparatorHelper(boolean base) {
        InheritanceComparator comp = new MetaDataInheritanceComparator();
        if (base)
            comp.setBase(AbstractThing.class);

        OpenJPAEntityManagerFactory emf = createEMF(A.class, B.class, C.class,
            AbstractThing.class);

        MetaDataRepository mr = emf.getConfiguration().
            getMetaDataRepositoryInstance();
        ClassMetaData a = mr.getMetaData(A.class, null, false);
        ClassMetaData b = mr.getMetaData(B.class, null, false);
        ClassMetaData c = mr.getMetaData(C.class, null, false);
        ClassMetaData at = mr.getMetaData(AbstractThing.class, null, false);

        emf.close();

        assertEquals(-1, comp.compare(a, b));
        assertEquals(-1, comp.compare(b, c));
        assertTrue(comp.compare(a, c) < 0);

        assertEquals(1, comp.compare(b, a));
        assertEquals(1, comp.compare(c, b));
        assertTrue(comp.compare(c, a) > 0);

        assertEquals(-1, comp.compare(at, a));
        assertEquals(-1, comp.compare(at, b));
        assertEquals(-1, comp.compare(at, c));
    }

    public void testEndToEnd() {
        // make sure we can get things fully instantiated
        EntityManagerFactory emf = createEMF(A.class, B.class, C.class,
            AbstractThing.class);
        emf.createEntityManager().close();
        emf.close();
    }
}
