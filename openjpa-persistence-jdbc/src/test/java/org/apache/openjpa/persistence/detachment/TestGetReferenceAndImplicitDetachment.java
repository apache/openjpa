package org.apache.openjpa.persistence.detachment;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import junit.framework.TestCase;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestGetReferenceAndImplicitDetachment
    extends SingleEMFTestCase {

    public void setUp() {
        setUp("openjpa.DetachState", "fgs",
            DetachmentOneManyParent.class, DetachmentOneManyChild.class);
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
