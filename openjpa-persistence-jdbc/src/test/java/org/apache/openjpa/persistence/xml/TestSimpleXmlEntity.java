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
