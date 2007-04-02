package org.apache.openjpa.persistence.xml;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestXmlOverrideEntity extends SingleEMFTestCase {

    public void setUp() {
        setUp(XmlOverrideEntity.class);
    }

    /**
     * Tests that the optional attribute on a basic field can be overrided by
     * an xml descriptor. 
     * 
     * XmlOverrideEntity.name is annotated with optional=false
     * XmlOverrideEntity.description is annotated with optional=true. 
     * 
     * The optional attributes are reversed in orm.xml. 
     */
    public void testOptionalAttributeOverride() {
        EntityManager em = emf.createEntityManager();

        XmlOverrideEntity optional = new XmlOverrideEntity();

        optional.setName(null);
        optional.setDescription("description");

        em.getTransaction().begin();
        em.persist(optional);
        em.getTransaction().commit();

        try {
            em.getTransaction().begin();
            optional.setDescription(null);
            em.getTransaction().commit();
            fail("XmlOrverrideEntity.description should not be optional. "
                    + "Expecting an InvalidStateException.");
        } catch (InvalidStateException e) {
        }

        em.getTransaction().begin();
        em.remove(em.find(XmlOverrideEntity.class, optional.getId()));
        em.getTransaction().commit();
    }
}

