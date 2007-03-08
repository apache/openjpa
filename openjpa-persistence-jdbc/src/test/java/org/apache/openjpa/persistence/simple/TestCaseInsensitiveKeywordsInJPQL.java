package org.apache.openjpa.persistence.simple;

import javax.persistence.Query;
import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.PersistenceTestCase;

public class TestCaseInsensitiveKeywordsInJPQL
    extends PersistenceTestCase {

    public Class[] getEntityTypes() {
        return new Class[] { AllFieldTypes.class };
    }

    public void testCaseInsensitiveBooleans() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        AllFieldTypes aft = new AllFieldTypes();
        em.persist(aft);
        aft.setBooleanField(true);

        aft = new AllFieldTypes();
        em.persist(aft);
        aft.setBooleanField(false);

        em.flush();

        Query q = em.createQuery(
            "select count(o) from AllFieldTypes o where o.booleanField = TrUe");
        Number n = (Number) q.getSingleResult();
        assertEquals(1, n.intValue());

        q = em.createQuery("select count(o) from AllFieldTypes o "
            + "where o.booleanField = falSe");
        n = (Number) q.getSingleResult();
        assertEquals(1, n.intValue());
        
        em.getTransaction().rollback();
    }
}