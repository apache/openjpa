package org.apache.openjpa.persistence.detachment;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import junit.framework.TestCase;


public class TestGetReferenceAndImplicitDetachment
    extends TestCase {

    private OpenJPAEntityManagerFactory emf;

    public void setUp() {
        String types = DetachmentOneManyParent.class.getName() + ";"
            + DetachmentOneManyChild.class.getName(); 
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");
        props.put("openjpa.DetachState", "fgs");
        emf = (OpenJPAEntityManagerFactory) Persistence.
            createEntityManagerFactory("test", props);
        deleteAll();
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            deleteAll();
            emf.close();
        } catch (Exception e) {
        }
    }
    
    private void deleteAll() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("delete from DetachmentOneManyChild").
            executeUpdate();
        em.createQuery("delete from DetachmentOneManyParent").
            executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    public void testNonexistentGetReferenceDetachmentInTxWithCommit() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        DetachmentOneManyParent o = 
            em.getReference(DetachmentOneManyParent.class, 0);
        em.getTransaction().commit();
        em.close();

        // the close detachment should leave these invalid objects in a 
        // transient state
        assertFalse(((PersistenceCapable) o).pcIsTransactional());
        assertFalse(((PersistenceCapable) o).pcIsPersistent());
        // pcIsDetached() will give a false positive in this configuration
        // assertFalse(((PersistenceCapable) o).pcIsDetached());
    }

    public void testNonexistentGetReferenceDetachmentOutsideTx() {
        EntityManager em = emf.createEntityManager();
        DetachmentOneManyParent o = 
            em.getReference(DetachmentOneManyParent.class, 0);
        em.close();

        // the close detachment should leave these invalid objects in a 
        // transient state
        assertFalse(((PersistenceCapable) o).pcIsTransactional());
        assertFalse(((PersistenceCapable) o).pcIsPersistent());
        // pcIsDetached() will give a false positive in this configuration
        // assertFalse(((PersistenceCapable) o).pcIsDetached());
    }

    public void testNonexistentGetReferenceDetachmentInTxWithRollback() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        DetachmentOneManyParent o = 
            em.getReference(DetachmentOneManyParent.class, 0);
        em.getTransaction().rollback();

        // the rollback should cause a detachment
        assertFalse(OpenJPAPersistence.cast(em).isTransactional(o));
        assertFalse(OpenJPAPersistence.cast(em).isPersistent(o));
        // pcIsDetached() will give a false positive in this configuration
        // assertFalse(OpenJPAPersistence.cast(em).isDetached(o));

        em.close();
    }

    public void testNonexistentGetReferenceDetachmentInTxWithFailedCommit() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        DetachmentOneManyParent o = 
            em.getReference(DetachmentOneManyParent.class, 0);
        em.getTransaction().setRollbackOnly();
        try {
            em.getTransaction().commit();
        } catch (RollbackException re) {
            // expected
        }

        // the failed commit should cause a detachment
        assertFalse(OpenJPAPersistence.cast(em).isTransactional(o));
        assertFalse(OpenJPAPersistence.cast(em).isPersistent(o));
        // pcIsDetached() will give a false positive in this configuration
        // assertFalse(OpenJPAPersistence.cast(em).isDetached(o));

        em.close();
    }
}
