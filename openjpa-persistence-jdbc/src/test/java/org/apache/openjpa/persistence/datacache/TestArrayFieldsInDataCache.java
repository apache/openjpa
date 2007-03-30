package org.apache.openjpa.persistence.datacache;

import java.util.Map;
import java.util.Arrays;
import javax.persistence.EntityManager;

import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.kernel.PCData;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestArrayFieldsInDataCache
    extends SingleEMFTestCase {

    private static final String[] STRINGS = new String[]{ "a", "b", "c" };
    private static final int[] INTS = new int[]{ 1, 2, 3 };

    private Object jpaOid;
    private Object internalOid;

    public void setUp() {
        setUp("openjpa.DataCache", "true", 
            "openjpa.RemoteCommitProvider", "sjvm", 
            AllFieldTypes.class);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes aft = new AllFieldTypes();
        aft.setArrayOfStrings(STRINGS);
        aft.setArrayOfInts(INTS);
        em.persist(aft);
        em.getTransaction().commit();

        // get the external and internal forms of the ID for cache
        // interrogation and data validation
        jpaOid = OpenJPAPersistence.cast(em).getObjectId(aft);
        internalOid = OpenJPAPersistence.toBroker(em).getObjectId(aft);

        em.close();
    }

    public void testArrayOfStrings() {
        // check that the data cache contains an efficient representation
        DataCache cache = OpenJPAPersistence.cast(emf).getStoreCache()
            .getDelegate();
        PCData data = cache.get(internalOid);
        ClassMetaData meta = OpenJPAPersistence.getMetaData(emf,
            AllFieldTypes.class);
        Object cachedFieldData =
            data.getData(meta.getField("arrayOfStrings").getIndex());
        assertTrue(cachedFieldData.getClass().isArray());
        assertEquals(String.class,
            cachedFieldData.getClass().getComponentType());

        // make sure that the returned results are correct
        EntityManager em = emf.createEntityManager();
        AllFieldTypes aft = em.find(AllFieldTypes.class, jpaOid);
        assertTrue(Arrays.equals(STRINGS, aft.getArrayOfStrings()));
        assertNotSame(STRINGS, aft.getArrayOfStrings());
        em.close();
    }

    public void testArrayOfInts() {
        // check that the data cache contains an efficient representation
        DataCache cache = OpenJPAPersistence.cast(emf).getStoreCache()
            .getDelegate();
        PCData data = cache.get(internalOid);
        ClassMetaData meta = OpenJPAPersistence.getMetaData(emf,
            AllFieldTypes.class);
        Object cachedFieldData =
            data.getData(meta.getField("arrayOfInts").getIndex());
        assertTrue(cachedFieldData.getClass().isArray());
        assertEquals(int.class, cachedFieldData.getClass().getComponentType());

        // make sure that the returned results are correct
        EntityManager em = emf.createEntityManager();
        AllFieldTypes aft = em.find(AllFieldTypes.class, jpaOid);
        assertTrue(Arrays.equals(INTS, aft.getArrayOfInts()));
        assertNotSame(INTS, aft.getArrayOfInts());
        em.close();
    }
}
