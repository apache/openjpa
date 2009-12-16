package org.apache.openjpa.persistence.datacache;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.datacache.DataCachePCData;
import org.apache.openjpa.datacache.DataCacheStoreManager;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.EntityManagerImpl;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/*
 * See OpenJPA-1407 for discussion of this test case.
 */
public class TestDataCacheObjectId extends SingleEMFTestCase {

    private OpenJPAEntityManager em;

    private MockDataCacheStoreManager mdsm;
    private TestOpenJPAStateManager sm;

    public void setUp() {
        setUp("openjpa.DataCache", "true", "openjpa.RemoteCommitProvider",
            "sjvm", CLEAR_TABLES, CascadeChild.class);
        em = emf.createEntityManager();
        sm = new TestOpenJPAStateManager();
        sm.setMetaData(emf.getConfiguration().getMetaDataRepositoryInstance()
            .addMetaData(CascadeChild.class));
        mdsm =
            new MockDataCacheStoreManager(((EntityManagerImpl) em).getBroker()
                .getStoreManager());
    }

    /*
     * If the DataCacheStoreManager's code regresses, these test cases will
     * cause an error rather than a failure.  Although a failure is generally 
     * preferred, in this case, the error is more informative
     * as it tells us where the class cast exception arises.
     */
    public void testAddObjectIdToOIDList() {
        mdsm.addObjectIdToOIDList(new ArrayList(), sm);
    }

    public void testGetDataFromDataMap() {
        mdsm.getDataFromDataMap(new HashMap(), sm);
    }

    class MockDataCacheStoreManager extends DataCacheStoreManager {

        public MockDataCacheStoreManager(StoreManager sm) {
            super(sm);
        }
        
        // Override required so test code can call this method
        protected void addObjectIdToOIDList(List oidList, OpenJPAStateManager sm) {
            super.addObjectIdToOIDList(oidList, sm);
        }
        
        // Override required so test code can call this method
        protected DataCachePCData getDataFromDataMap(Map dataMap,
            OpenJPAStateManager sm) {
            return super.getDataFromDataMap(dataMap, sm);
        }
    }

    class TestOpenJPAStateManager implements OpenJPAStateManager {

        private ClassMetaData meta;

        public void setMetaData(ClassMetaData meta) {
            this.meta = meta;
        }

        public ClassMetaData getMetaData() {
            return this.meta;
        }

        // In OpenJPA this method normally returns an OpenJPAId.
        // Our tests require that it return something else.
        public Object getObjectId() {
            return new Object();
        }

        public boolean assignObjectId(boolean flush) {
            return false;
        }

        public boolean beforeRefresh(boolean refreshAll) {
            // Default implementation
            return false;
        }

        public void dirty(int field) {
            // Default implementation
        }

        public Object fetch(int field) {
            // Default implementation
            return null;
        }

        public boolean fetchBoolean(int field) {
            // Default implementation
            return false;
        }

        public byte fetchByte(int field) {
            // Default implementation
            return 0;
        }

        public char fetchChar(int field) {
            // Default implementation
            return 0;
        }

        public double fetchDouble(int field) {
            // Default implementation
            return 0;
        }

        public Object fetchField(int field, boolean transitions) {
            // Default implementation
            return null;
        }

        public float fetchFloat(int field) {
            // Default implementation
            return 0;
        }

        public Object fetchInitialField(int field) {
            // Default implementation
            return null;
        }

        public int fetchInt(int field) {
            // Default implementation
            return 0;
        }

        public long fetchLong(int field) {
            // Default implementation
            return 0;
        }

        public Object fetchObject(int field) {
            // Default implementation
            return null;
        }

        public short fetchShort(int field) {
            // Default implementation
            return 0;
        }

        public String fetchString(int field) {
            // Default implementation
            return null;
        }

        public StoreContext getContext() {
            // Default implementation
            return null;
        }

        public BitSet getDirty() {
            // Default implementation
            return null;
        }

        public BitSet getFlushed() {
            // Default implementation
            return null;
        }

        public Object getId() {
            // Default implementation
            return null;
        }

        public Object getImplData() {
            // Default implementation
            return null;
        }

        public Object getImplData(int field) {
            // Default implementation
            return null;
        }

        public Object getIntermediate(int field) {
            // Default implementation
            return null;
        }

        public BitSet getLoaded() {
            // Default implementation
            return null;
        }

        public Object getLock() {
            // Default implementation
            return null;
        }

        public Object getManagedInstance() {
            // Default implementation
            return null;
        }

        public OpenJPAStateManager getOwner() {
            // Default implementation
            return null;
        }

        public int getOwnerIndex() {
            // Default implementation
            return 0;
        }

        public PCState getPCState() {
            // Default implementation
            return null;
        }

        public PersistenceCapable getPersistenceCapable() {
            // Default implementation
            return null;
        }

        public BitSet getUnloaded(FetchConfiguration fetch) {
            // Default implementation
            return null;
        }

        public Object getVersion() {
            // Default implementation
            return null;
        }

        public void initialize(Class forType, PCState state) {
            // Default implementation
        }

        public boolean isDefaultValue(int field) {
            // Default implementation
            return false;
        }

        public boolean isEmbedded() {
            // Default implementation
            return false;
        }

        public boolean isFlushed() {
            // Default implementation
            return false;
        }

        public boolean isFlushedDirty() {
            // Default implementation
            return false;
        }

        public boolean isImplDataCacheable() {
            // Default implementation
            return false;
        }

        public boolean isImplDataCacheable(int field) {
            // Default implementation
            return false;
        }

        public boolean isProvisional() {
            // Default implementation
            return false;
        }

        public boolean isVersionCheckRequired() {
            // Default implementation
            return false;
        }

        public boolean isVersionUpdateRequired() {
            // Default implementation
            return false;
        }

        public void load(FetchConfiguration fetch) {
            // Default implementation
        }

        public Object newFieldProxy(int field) {
            // Default implementation
            return null;
        }

        public Object newProxy(int field) {
            // Default implementation
            return null;
        }

        public void removed(int field, Object removed, boolean key) {
            // Default implementation
        }

        public Object setImplData(Object data, boolean cacheable) {
            // Default implementation
            return null;
        }

        public Object setImplData(int field, Object data) {
            // Default implementation
            return null;
        }

        public void setIntermediate(int field, Object value) {
            // Default implementation
        }

        public void setLock(Object lock) {
            // Default implementation
        }

        public void setNextVersion(Object version) {
            // Default implementation
        }

        public void setObjectId(Object oid) {
            // Default implementation
        }

        public void setRemote(int field, Object value) {
            // Default implementation
        }

        public void setVersion(Object version) {
            // Default implementation
        }

        public void store(int field, Object value) {
            // Default implementation
        }

        public void storeBoolean(int field, boolean externalVal) {
            // Default implementation
        }

        public void storeByte(int field, byte externalVal) {
            // Default implementation
        }

        public void storeChar(int field, char externalVal) {
            // Default implementation
        }

        public void storeDouble(int field, double externalVal) {
            // Default implementation
        }

        public void storeField(int field, Object value) {
            // Default implementation
        }

        public void storeFloat(int field, float externalVal) {
            // Default implementation
        }

        public void storeInt(int field, int externalVal) {
            // Default implementation
        }

        public void storeLong(int field, long externalVal) {
            // Default implementation
        }

        public void storeObject(int field, Object externalVal) {
            // Default implementation
        }

        public void storeShort(int field, short externalVal) {
            // Default implementation
        }

        public void storeString(int field, String externalVal) {
            // Default implementation
        }

        public void accessingField(int idx) {
            // Default implementation
        }

        public void dirty(String field) {
            // Default implementation
        }

        public Object fetchObjectId() {
            // Default implementation
            return null;
        }

        public Object getGenericContext() {
            // Default implementation
            return null;
        }

        public Object getPCPrimaryKey(Object oid, int field) {
            // Default implementation
            return null;
        }

        public boolean isDeleted() {
            // Default implementation
            return false;
        }

        public boolean isDetached() {
            // Default implementation
            return false;
        }

        public boolean isDirty() {
            // Default implementation
            return false;
        }

        public boolean isNew() {
            // Default implementation
            return false;
        }

        public boolean isPersistent() {
            // Default implementation
            return false;
        }

        public boolean isTransactional() {
            // Default implementation
            return false;
        }

        public void providedBooleanField(PersistenceCapable pc, int idx,
            boolean cur) {
            // Default implementation

        }

        public void providedByteField(PersistenceCapable pc, int idx, byte cur) {
            // Default implementation
        }

        public void providedCharField(PersistenceCapable pc, int idx, char cur) {
            // Default implementation
        }

        public void providedDoubleField(PersistenceCapable pc, int idx,
            double cur) {
            // Default implementation
        }

        public void providedFloatField(PersistenceCapable pc, int idx, float cur) {
            // Default implementation
        }

        public void providedIntField(PersistenceCapable pc, int idx, int cur) {
            // Default implementation
        }

        public void providedLongField(PersistenceCapable pc, int idx, long cur) {
            // Default implementation
        }

        public void providedObjectField(PersistenceCapable pc, int idx,
            Object cur) {
            // Default implementation
        }

        public void providedShortField(PersistenceCapable pc, int idx, short cur) {
            // Default implementation
        }

        public void providedStringField(PersistenceCapable pc, int idx,
            String cur) {
            // Default implementation
        }

        public void proxyDetachedDeserialized(int idx) {
            // Default implementation
        }

        public boolean replaceBooleanField(PersistenceCapable pc, int idx) {
            // Default implementation
            return false;
        }

        public byte replaceByteField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public char replaceCharField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public double replaceDoubleField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public float replaceFloatField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public int replaceIntField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public long replaceLongField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public Object replaceObjectField(PersistenceCapable pc, int idx) {
            // Default implementation
            return null;
        }

        public short replaceShortField(PersistenceCapable pc, int idx) {
            // Default implementation
            return 0;
        }

        public StateManager replaceStateManager(StateManager sm) {
            // Default implementation
            return null;
        }

        public String replaceStringField(PersistenceCapable pc, int idx) {
            // Default implementation
            return null;
        }

        public boolean serializing() {
            // Default implementation
            return false;
        }

        public void settingBooleanField(PersistenceCapable pc, int idx,
            boolean cur, boolean next, int set) {
            // Default implementation
        }

        public void settingByteField(PersistenceCapable pc, int idx, byte cur,
            byte next, int set) {
            // Default implementation
        }

        public void settingCharField(PersistenceCapable pc, int idx, char cur,
            char next, int set) {
            // Default implementation
        }

        public void settingDoubleField(PersistenceCapable pc, int idx,
            double cur, double next, int set) {
            // Default implementation
        }

        public void settingFloatField(PersistenceCapable pc, int idx,
            float cur, float next, int set) {
            // Default implementation
        }

        public void settingIntField(PersistenceCapable pc, int idx, int cur,
            int next, int set) {
            // Default implementation
        }

        public void settingLongField(PersistenceCapable pc, int idx, long cur,
            long next, int set) {
            // Default implementation
        }

        public void settingObjectField(PersistenceCapable pc, int idx,
            Object cur, Object next, int set) {
            // Default implementation
        }

        public void settingShortField(PersistenceCapable pc, int idx,
            short cur, short next, int set) {
            // Default implementation
        }

        public void settingStringField(PersistenceCapable pc, int idx,
            String cur, String next, int set) {
            // Default implementation
        }

        public boolean writeDetached(ObjectOutput out) throws IOException {
            // Default implementation
            return false;
        }

        public void storeBooleanField(int fieldIndex, boolean value) {
            // Default implementation
        }

        public void storeByteField(int fieldIndex, byte value) {
            // Default implementation
        }

        public void storeCharField(int fieldIndex, char value) {
            // Default implementation
        }

        public void storeDoubleField(int fieldIndex, double value) {
            // Default implementation
        }

        public void storeFloatField(int fieldIndex, float value) {
            // Default implementation
        }

        public void storeIntField(int fieldIndex, int value) {
            // Default implementation
        }

        public void storeLongField(int fieldIndex, long value) {
            // Default implementation
        }

        public void storeObjectField(int fieldIndex, Object value) {
            // Default implementation
        }

        public void storeShortField(int fieldIndex, short value) {
            // Default implementation
        }

        public void storeStringField(int fieldIndex, String value) {
            // Default implementation
        }

        public boolean fetchBooleanField(int fieldIndex) {
            // Default implementation
            return false;
        }

        public byte fetchByteField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public char fetchCharField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public double fetchDoubleField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public float fetchFloatField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public int fetchIntField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public long fetchLongField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public Object fetchObjectField(int fieldIndex) {
            // Default implementation
            return null;
        }

        public short fetchShortField(int fieldIndex) {
            // Default implementation
            return 0;
        }

        public String fetchStringField(int fieldIndex) {
            // Default implementation
            return null;
        }
    }

}
