/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.jdbc.kernel;

import java.io.IOException;
import java.io.ObjectOutput;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Strategy;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.SQLFactory;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.Id;

import junit.framework.TestCase;

/**
 * <p>
 * Tests AbstractUpdateManager flush's method exception return behavior.
 * </p>
 *
 * ================  IMPORTANT NOTE ======================================
 * This test is retired temporarily. This test declares a TestConnection
 * class which needs to be abstract for JDK6/JDBC4.
 * =======================================================================
 *
 * @author Albert Lee
 */

public class TestUpdateManagerFlushException extends /* Abstract */TestCase {

    private TestUpdateManager updMgr;

    @Override
    public void setUp() {
        updMgr = new TestUpdateManager();
    }

    public void testDummy() {

    }

    /**
     * Tests exception collection returns from UpdateManager flush method is in
     * the order the original exceptions are thrown.
     */
    public void xtestAddRetrieve() {

        Collection states = new ArrayList<OpenJPAStateManager>();
        states.add(new TestOpenJPAStateManager());

        Collection exceps = updMgr.flush(states, new TestJDBCStore());

        assertEquals(3, exceps.size());

        Iterator<Exception> itr = exceps.iterator();
        assertEquals(itr.next().getMessage(),
            "TestUpdateManager.populateRowManager");
        assertEquals(itr.next().getMessage(),
            "TestUpdateManager.flush");
        assertEquals(itr.next().getMessage(),
            "TestUpdateManager.customInsert");
    }

    /*
     * Scaffolding test update manager.
     */
    class TestUpdateManager extends AbstractUpdateManager {

        @Override
        protected Collection flush(RowManager rowMgr,
            PreparedStatementManager psMgr, Collection exceps) {

            exceps.add(new SQLException("TestUpdateManager.flush"));

            return exceps;
        }

        @Override
        protected PreparedStatementManager newPreparedStatementManager(
            JDBCStore store, Connection conn) {
            return new PreparedStatementManagerImpl(store, conn);
        }

        @Override
        protected RowManager newRowManager() {
            return null;
        }

        @Override
        public boolean orderDirty() {
            return false;
        }

        @Override
        protected Collection populateRowManager(OpenJPAStateManager sm,
            RowManager rowMgr, JDBCStore store, Collection exceps,
            Collection customs) {

            exceps.add(new SQLException(
                "TestUpdateManager.populateRowManager"));
            customs.add(new CustomMapping(CustomMapping.INSERT, sm,
                new Strategy() {
                    
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void customDelete(OpenJPAStateManager sm,
                        JDBCStore store) throws SQLException {
                    }

                    @Override
                    public void customInsert(OpenJPAStateManager sm,
                        JDBCStore store) throws SQLException {
                        throw new SQLException(
                            "TestUpdateManager.customInsert");
                    }

                    @Override
                    public void customUpdate(OpenJPAStateManager sm,
                        JDBCStore store) throws SQLException {
                    }

                    @Override
                    public void delete(OpenJPAStateManager sm, JDBCStore store,
                        RowManager rm) throws SQLException {
                    }

                    @Override
                    public String getAlias() {
                        return null;
                    }

                    @Override
                    public void initialize() {
                    }

                    @Override
                    public void insert(OpenJPAStateManager sm, JDBCStore store,
                        RowManager rm) throws SQLException {

                    }

                    @Override
                    public Boolean isCustomDelete(OpenJPAStateManager sm,
                        JDBCStore store) {
                        return null;
                    }

                    @Override
                    public Boolean isCustomInsert(OpenJPAStateManager sm,
                        JDBCStore store) {
                        return null;
                    }

                    @Override
                    public Boolean isCustomUpdate(OpenJPAStateManager sm,
                        JDBCStore store) {
                        return null;
                    }

                    @Override
                    public void map(boolean adapt) {
                    }

                    @Override
                    public void update(OpenJPAStateManager sm, JDBCStore store,
                        RowManager rm) throws SQLException {
                    }
                }));
            return exceps;
        }
    }

    /*
     * Scaffolding test state manager.
     */
    class TestOpenJPAStateManager implements OpenJPAStateManager {

        @Override
        public boolean assignObjectId(boolean flush) {
            return false;
        }

        @Override
        public boolean beforeRefresh(boolean refreshAll) {
            return false;
        }

        @Override
        public void dirty(int field) {
        }

        @Override
        public Object fetch(int field) {
            return null;
        }

        @Override
        public boolean fetchBoolean(int field) {
            return false;
        }

        @Override
        public byte fetchByte(int field) {
            return 0;
        }

        @Override
        public char fetchChar(int field) {
            return 0;
        }

        @Override
        public double fetchDouble(int field) {
            return 0;
        }

        @Override
        public Object fetchField(int field, boolean transitions) {
            return null;
        }

        @Override
        public float fetchFloat(int field) {
            return 0;
        }

        @Override
        public Object fetchInitialField(int field) {
            return null;
        }

        @Override
        public int fetchInt(int field) {
            return 0;
        }

        @Override
        public long fetchLong(int field) {
            return 0;
        }

        @Override
        public Object fetchObject(int field) {
            return null;
        }

        @Override
        public short fetchShort(int field) {
            return 0;
        }

        @Override
        public String fetchString(int field) {
            return null;
        }

        @Override
        public StoreContext getContext() {
            return null;
        }

        @Override
        public BitSet getDirty() {
            return null;
        }

        @Override
        public BitSet getFlushed() {
            return null;
        }

        @Override
        public Object getId() {
            return null;
        }

        @Override
        public Object getImplData() {
            return null;
        }

        @Override
        public Object getImplData(int field) {
            return null;
        }

        @Override
        public Object getIntermediate(int field) {
            return null;
        }

        @Override
        public BitSet getLoaded() {
            return null;
        }

        @Override
        public Object getLock() {
            return null;
        }

        @Override
        public Object getManagedInstance() {
            return null;
        }

        @Override
        public ClassMetaData getMetaData() {
            return null;
        }

        @Override
        public Object getObjectId() {
            return null;
        }

        @Override
        public OpenJPAStateManager getOwner() {
            return null;
        }

        @Override
        public int getOwnerIndex() {
            return 0;
        }

        @Override
        public PCState getPCState() {
            return null;
        }

        @Override
        public PersistenceCapable getPersistenceCapable() {
            return null;
        }

        @Override
        public BitSet getUnloaded(FetchConfiguration fetch) {
            return null;
        }

        @Override
        public Object getVersion() {
            return null;
        }

        @Override
        public void initialize(Class forType, PCState state) {
        }

        @Override
        public boolean isDefaultValue(int field) {
            return false;
        }

        @Override
        public boolean isEmbedded() {
            return false;
        }

        @Override
        public boolean isFlushed() {
            return false;
        }

        @Override
        public boolean isFlushedDirty() {
            return false;
        }

        @Override
        public boolean isImplDataCacheable() {
            return false;
        }

        @Override
        public boolean isImplDataCacheable(int field) {
            return false;
        }

        @Override
        public boolean isProvisional() {
            return false;
        }

        @Override
        public boolean isVersionCheckRequired() {
            return false;
        }

        @Override
        public boolean isVersionUpdateRequired() {
            return false;
        }

        @Override
        public void load(FetchConfiguration fetch) {
        }

        @Override
        public Object newFieldProxy(int field) {
            return null;
        }

        @Override
        public Object newProxy(int field) {
            return null;
        }

        @Override
        public void removed(int field, Object removed, boolean key) {
        }

        @Override
        public Object setImplData(Object data, boolean cacheable) {
            return null;
        }

        @Override
        public Object setImplData(int field, Object data) {
            return null;
        }

        @Override
        public void setIntermediate(int field, Object value) {
        }

        @Override
        public void setLock(Object lock) {
        }

        @Override
        public void setNextVersion(Object version) {
        }

        @Override
        public void setObjectId(Object oid) {
        }

        @Override
        public void setRemote(int field, Object value) {
        }

        @Override
        public void setVersion(Object version) {
        }

        @Override
        public void store(int field, Object value) {
        }

        @Override
        public void storeBoolean(int field, boolean externalVal) {
        }

        @Override
        public void storeByte(int field, byte externalVal) {
        }

        @Override
        public void storeChar(int field, char externalVal) {
        }

        @Override
        public void storeDouble(int field, double externalVal) {
        }

        @Override
        public void storeField(int field, Object value) {
        }

        @Override
        public void storeFloat(int field, float externalVal) {
        }

        @Override
        public void storeInt(int field, int externalVal) {
        }

        @Override
        public void storeLong(int field, long externalVal) {
        }

        @Override
        public void storeObject(int field, Object externalVal) {
        }

        @Override
        public void storeShort(int field, short externalVal) {
        }

        @Override
        public void storeString(int field, String externalVal) {
        }

        @Override
        public void accessingField(int idx) {
        }

        @Override
        public void dirty(String field) {
        }

        @Override
        public Object fetchObjectId() {
            return null;
        }

        @Override
        public Object getGenericContext() {
            return null;
        }

        @Override
        public Object getPCPrimaryKey(Object oid, int field) {
            return null;
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public boolean isDetached() {
            return false;
        }

        @Override
        public boolean isDirty() {
            return false;
        }

        @Override
        public boolean isNew() {
            return false;
        }

        @Override
        public boolean isPersistent() {
            return false;
        }

        @Override
        public boolean isTransactional() {
            return false;
        }

        @Override
        public void providedBooleanField(PersistenceCapable pc, int idx,
            boolean cur) {
        }

        @Override
        public void providedByteField(PersistenceCapable pc, int idx,
            byte cur) {
        }

        @Override
        public void providedCharField(PersistenceCapable pc, int idx,
            char cur) {
        }

        @Override
        public void providedDoubleField(PersistenceCapable pc, int idx,
            double cur) {
        }

        @Override
        public void providedFloatField(PersistenceCapable pc, int idx,
            float cur) {
        }

        @Override
        public void providedIntField(PersistenceCapable pc, int idx,
            int cur) {
        }

        @Override
        public void providedLongField(PersistenceCapable pc, int idx,
            long cur) {
        }

        @Override
        public void providedObjectField(PersistenceCapable pc, int idx,
            Object cur) {
        }

        @Override
        public void providedShortField(PersistenceCapable pc, int idx,
            short cur) {
        }

        @Override
        public void providedStringField(PersistenceCapable pc, int idx,
            String cur) {
        }

        @Override
        public void proxyDetachedDeserialized(int idx) {
        }

        @Override
        public boolean replaceBooleanField(PersistenceCapable pc, int idx) {
            return false;
        }

        @Override
        public byte replaceByteField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public char replaceCharField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public double replaceDoubleField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public float replaceFloatField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public int replaceIntField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public long replaceLongField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public Object replaceObjectField(PersistenceCapable pc, int idx) {
            return null;
        }

        @Override
        public short replaceShortField(PersistenceCapable pc, int idx) {
            return 0;
        }

        @Override
        public StateManager replaceStateManager(StateManager sm) {
            return null;
        }

        @Override
        public String replaceStringField(PersistenceCapable pc, int idx) {
            return null;
        }

        @Override
        public boolean serializing() {
            return false;
        }

        @Override
        public void settingBooleanField(PersistenceCapable pc, int idx,
            boolean cur, boolean next, int set) {
        }

        @Override
        public void settingByteField(PersistenceCapable pc, int idx, byte cur,
            byte next, int set) {
        }

        @Override
        public void settingCharField(PersistenceCapable pc, int idx, char cur,
            char next, int set) {
        }

        @Override
        public void settingDoubleField(PersistenceCapable pc, int idx,
            double cur, double next, int set) {
        }

        @Override
        public void settingFloatField(PersistenceCapable pc, int idx,
            float cur, float next, int set) {
        }

        @Override
        public void settingIntField(PersistenceCapable pc, int idx, int cur,
            int next, int set) {
        }

        @Override
        public void settingLongField(PersistenceCapable pc, int idx, long cur,
            long next, int set) {
        }

        @Override
        public void settingObjectField(PersistenceCapable pc, int idx,
            Object cur, Object next, int set) {
        }

        @Override
        public void settingShortField(PersistenceCapable pc, int idx,
            short cur, short next, int set) {
        }

        @Override
        public void settingStringField(PersistenceCapable pc, int idx,
            String cur, String next, int set) {
        }

        @Override
        public boolean writeDetached(ObjectOutput out) throws IOException {
            return false;
        }

        @Override
        public void storeBooleanField(int fieldIndex, boolean value) {
        }

        @Override
        public void storeByteField(int fieldIndex, byte value) {
        }

        @Override
        public void storeCharField(int fieldIndex, char value) {
        }

        @Override
        public void storeDoubleField(int fieldIndex, double value) {
        }

        @Override
        public void storeFloatField(int fieldIndex, float value) {
        }

        @Override
        public void storeIntField(int fieldIndex, int value) {
        }

        @Override
        public void storeLongField(int fieldIndex, long value) {
        }

        @Override
        public void storeObjectField(int fieldIndex, Object value) {
        }

        @Override
        public void storeShortField(int fieldIndex, short value) {
        }

        @Override
        public void storeStringField(int fieldIndex, String value) {
        }

        @Override
        public boolean fetchBooleanField(int fieldIndex) {
            return false;
        }

        @Override
        public byte fetchByteField(int fieldIndex) {
            return 0;
        }

        @Override
        public char fetchCharField(int fieldIndex) {
            return 0;
        }

        @Override
        public double fetchDoubleField(int fieldIndex) {
            return 0;
        }

        @Override
        public float fetchFloatField(int fieldIndex) {
            return 0;
        }

        @Override
        public int fetchIntField(int fieldIndex) {
            return 0;
        }

        @Override
        public long fetchLongField(int fieldIndex) {
            return 0;
        }

        @Override
        public Object fetchObjectField(int fieldIndex) {
            return null;
        }

        @Override
        public short fetchShortField(int fieldIndex) {
            return 0;
        }

        @Override
        public String fetchStringField(int fieldIndex) {
            return null;
        }

        @Override
        public boolean isDelayed(int field) {
            return false;
        }

        @Override
        public void setDelayed(int field, boolean delay) {
        }

        @Override
        public void loadDelayedField(int field) {
        }
    }

    /*
     * Scaffolding test connection.
     */
    abstract class TestConnection implements Connection {

        @Override
        public void clearWarnings() throws SQLException {
        }

        @Override
        public void close() throws SQLException {
        }

        @Override
        public void commit() throws SQLException {
        }

        @Override
        public Statement createStatement() throws SQLException {
            return null;
        }

        @Override
        public Statement createStatement(int resultSetType,
            int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
            return null;
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return false;
        }

        @Override
        public String getCatalog() throws SQLException {
            return null;
        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return null;
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return 0;
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return null;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public boolean isClosed() throws SQLException {
            return false;
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return false;
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql)
            throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql,
            int autoGeneratedKeys) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql,
            int[] columnIndexes) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql,
            String[] columnNames) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql,
            int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql,
            int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        }

        @Override
        public void rollback() throws SQLException {
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return null;
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        }
    }

    /*
     * Scaffolding test store manager.
     */
    class TestJDBCStore implements JDBCStore {

        @Override
        public Object find(Object oid, ValueMapping vm,
            JDBCFetchConfiguration fetch) {
            return null;
        }

        @Override
        public JDBCConfiguration getConfiguration() {
            return null;
        }

        @Override
        public Connection getConnection() {
            throw new RuntimeException("TestConnection is abstract for JDK6");
//            return new TestConnection();
        }

        @Override
        public Connection getNewConnection() {
            return getConnection();
        }

        @Override
        public StoreContext getContext() {
            return null;
        }

        @Override
        public DBDictionary getDBDictionary() {
            return null;
        }

        @Override
        public JDBCFetchConfiguration getFetchConfiguration() {
            return null;
        }

        @Override
        public JDBCLockManager getLockManager() {
            return null;
        }

        @Override
        public SQLFactory getSQLFactory() {
            return null;
        }

        @Override
        public void loadSubclasses(ClassMapping mapping) {

        }

        @Override
        public Id newDataStoreId(long id, ClassMapping mapping, boolean subs) {
            return null;
        }
    }
}
