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
package org.apache.openjpa.persistence.xml;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestSimpleXmlEntity
    extends SingleEMTestCase {

    public void setUp() {
        setUp(SimpleXmlEntity.class);
    }

    public void testNamedQueryInXmlNamedEntity() {
        em.createNamedQuery("SimpleXml.findAll").getResultList();
    }

    public void testNamedQueryInXmlUsingShortClassName() {
        try {
            em.createNamedQuery("SimpleXmlEntity.findAll").getResultList();
            fail("should not be able to execute query using short class name " +
                "for entity that has an entity name specified");
        } catch (ArgumentException ae) {
            // expected
        }
    }

    public void testNamedEntityInDynamicQuery() {
        em.createQuery("select o from SimpleXml o").getResultList();
    }

    public void testShortClassNameInDynamicQuery() {
        try {
            em.createQuery("select o from SimpleXmlEntity o").getResultList();
            fail("should not be able to execute query using short class name " +
                "for entity that has an entity name specified");
        } catch (ArgumentException ae) {
            // expected
        }
    }
}
