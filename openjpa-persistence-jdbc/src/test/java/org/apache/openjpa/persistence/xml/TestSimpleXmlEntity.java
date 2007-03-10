package org.apache.openjpa.persistence.xml;

import java.util.Map;
import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMTest;
import org.apache.openjpa.persistence.ArgumentException;

public class TestSimpleXmlEntity
    extends SingleEMTest {

    public TestSimpleXmlEntity() {
        super(SimpleXmlEntity.class);
    }

    protected void setEMFProps(Map props) {
        super.setEMFProps(props);
    }

    public void testNamedQueryInXmlNamedEntity() {
        EntityManager em = emf.createEntityManager();
        em.createNamedQuery("SimpleXml.findAll").getResultList();
        em.close();
    }

    public void testNamedQueryInXmlUsingShortClassName() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createNamedQuery("SimpleXmlEntity.findAll").getResultList();
            fail("should not be able to execute query using short class name " +
                "for entity that has an entity name specified");
        } catch (ArgumentException ae) {
            // expected
        }
        em.close();
    }

    public void testNamedEntityInDynamicQuery() {
        EntityManager em = emf.createEntityManager();
        em.createQuery("select o from SimpleXml o").getResultList();
        em.close();
    }

    public void testShortClassNameInDynamicQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createQuery("select o from SimpleXmlEntity o").getResultList();
            fail("should not be able to execute query using short class name " +
                "for entity that has an entity name specified");
        } catch (ArgumentException ae) {
            // expected
        }
        em.close();
    }
}
