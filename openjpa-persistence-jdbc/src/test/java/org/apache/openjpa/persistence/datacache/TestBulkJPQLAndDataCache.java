package org.apache.openjpa.persistence.datacache;

import java.util.List;
import java.util.Map;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTest;

public class TestBulkJPQLAndDataCache
    extends SingleEMFTest {

    private Object oid;

    public TestBulkJPQLAndDataCache() {
        super(AllFieldTypes.class);
    }

    @Override
    protected boolean clearDatabaseInSetUp() {
        return true;
    }

    protected void setEMFProps(Map props) {
        super.setEMFProps(props);
        props.put("openjpa.DataCache", "true");
        props.put("openjpa.RemoteCommitProvider", "sjvm");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        OpenJPAEntityManager em =
            OpenJPAPersistence.cast(emf.createEntityManager());
        em.getTransaction().begin();
        AllFieldTypes pc = new AllFieldTypes();
        pc.setStringField("DeleteMe");
        em.persist(pc);
        oid = em.getObjectId(pc);
        em.getTransaction().commit();
        em.close();
    }

    public void testBulkDelete() {
        OpenJPAEntityManager em =
            OpenJPAPersistence.cast(emf.createEntityManager());

        em.getTransaction().begin();
        List result = em.createQuery("SELECT o FROM AllFieldTypes o")
            .getResultList();
        assertEquals(1, result.size());
        em.createQuery("DELETE FROM AllFieldTypes o").executeUpdate();
        em.getTransaction().commit();
        em.close();

        em = OpenJPAPersistence.cast(emf.createEntityManager());

        // this assumes that we invalidate the cache, rather than update it
        // according to the bulk rule.
        assertFalse(OpenJPAPersistence.cast(emf).getStoreCache()
            .contains(AllFieldTypes.class, oid));

        assertNull(em.find(AllFieldTypes.class, oid));
        em.close();
    }

    public void testBulkUpdate() {
        OpenJPAEntityManager em =
            OpenJPAPersistence.cast(emf.createEntityManager());

        em.getTransaction().begin();
        List result = em.createQuery("SELECT o FROM AllFieldTypes o "
            + "WHERE o.intField = 0").getResultList();
        assertEquals(1, result.size());
        em.createQuery("UPDATE AllFieldTypes o SET o.intField = 10")
            .executeUpdate();
        em.getTransaction().commit();
        em.close();

        em = OpenJPAPersistence.cast(emf.createEntityManager());

        // this assumes that we invalidate the cache, rather than update it
        // according to the bulk rule.
        assertFalse(OpenJPAPersistence.cast(emf).getStoreCache()
            .contains(AllFieldTypes.class, oid));

        em.close();
    }
}
