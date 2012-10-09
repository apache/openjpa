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
package org.apache.openjpa.ee;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import javax.transaction.TransactionManager;

import junit.framework.TestCase;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.DelegatingStoreManager;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FetchConfigurationImpl;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;

public class TestNullTransactionManagerFromRuntime extends TestCase {
    public void test() {
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        conf.setMetaDataFactory("org.apache.openjpa.meta.NoneMetaDataFactory");
        conf.setManagedRuntime(new ManagedRuntimeNullTransactionManager());
        MangagedRuntimeTestBrokerFactory fact = new MangagedRuntimeTestBrokerFactory(conf);
        BrokerImpl b = new BrokerImpl();
        try {
            b.initialize(fact, new ManagedRuntimeNoopDelegatingStoreManager(null), true, 0, false);
            fail("Calling syncWithManagedTransaction should have thrown an exception.");
        } catch (RuntimeException re) {
            // expected
        }
    }

    class ManagedRuntimeNullTransactionManager extends AbstractManagedRuntime {
        @Override
        public Throwable getRollbackCause() throws Exception {

            return null;
        }

        @Override
        public TransactionManager getTransactionManager() throws Exception {

            return null;
        }

        @Override
        public void setRollbackOnly(Throwable cause) throws Exception {

        }
    }

    class MangagedRuntimeTestBrokerFactory extends AbstractBrokerFactory {
        public MangagedRuntimeTestBrokerFactory(OpenJPAConfiguration conf) {
            super(conf);
        }

        @Override
        protected StoreManager newStoreManager() {

            return null;
        }
    }

    class ManagedRuntimeNoopDelegatingStoreManager extends DelegatingStoreManager {
        public ManagedRuntimeNoopDelegatingStoreManager(StoreManager store) {
            super(store);
        }

        @Override
        public StoreManager getDelegate() {

            return super.getDelegate();
        }

        @Override
        public StoreManager getInnermostDelegate() {

            return super.getInnermostDelegate();
        }

        @Override
        public int hashCode() {

            return super.hashCode();
        }

        @Override
        public boolean equals(Object other) {

            return super.equals(other);
        }

        @Override
        public void setContext(StoreContext ctx) {
        }

        @Override
        public void beginOptimistic() {

            super.beginOptimistic();
        }

        @Override
        public void rollbackOptimistic() {

            super.rollbackOptimistic();
        }

        @Override
        public void begin() {

            super.begin();
        }

        @Override
        public void commit() {

            super.commit();
        }

        @Override
        public void rollback() {

            super.rollback();
        }

        @Override
        public boolean exists(OpenJPAStateManager sm, Object context) {

            return super.exists(sm, context);
        }

        @Override
        public boolean syncVersion(OpenJPAStateManager sm, Object context) {

            return super.syncVersion(sm, context);
        }

        @Override
        public boolean initialize(OpenJPAStateManager sm, PCState state, FetchConfiguration fetch, Object context) {

            return super.initialize(sm, state, fetch, context);
        }

        @Override
        public boolean load(OpenJPAStateManager sm, BitSet fields, FetchConfiguration fetch, int lockLevel,
            Object context) {

            return super.load(sm, fields, fetch, lockLevel, context);
        }

        @Override
        public Collection<Object> loadAll(Collection<OpenJPAStateManager> sms, PCState state, int load,
            FetchConfiguration fetch, Object context) {

            return super.loadAll(sms, state, load, fetch, context);
        }

        @Override
        public void beforeStateChange(OpenJPAStateManager sm, PCState fromState, PCState toState) {

            super.beforeStateChange(sm, fromState, toState);
        }

        @Override
        public Collection<Exception> flush(Collection<OpenJPAStateManager> sms) {

            return super.flush(sms);
        }

        @Override
        public boolean assignObjectId(OpenJPAStateManager sm, boolean preFlush) {

            return super.assignObjectId(sm, preFlush);
        }

        @Override
        public boolean assignField(OpenJPAStateManager sm, int field, boolean preFlush) {

            return super.assignField(sm, field, preFlush);
        }

        @Override
        public Class<?> getManagedType(Object oid) {

            return super.getManagedType(oid);
        }

        @Override
        public Class<?> getDataStoreIdType(ClassMetaData meta) {

            return super.getDataStoreIdType(meta);
        }

        @Override
        public Object copyDataStoreId(Object oid, ClassMetaData meta) {

            return super.copyDataStoreId(oid, meta);
        }

        @Override
        public Object newDataStoreId(Object oidVal, ClassMetaData meta) {

            return super.newDataStoreId(oidVal, meta);
        }

        @Override
        public Object getClientConnection() {

            return super.getClientConnection();
        }

        @Override
        public void retainConnection() {

            super.retainConnection();
        }

        @Override
        public void releaseConnection() {

            super.releaseConnection();
        }

        @Override
        public ResultObjectProvider executeExtent(ClassMetaData meta, boolean subclasses, FetchConfiguration fetch) {

            return super.executeExtent(meta, subclasses, fetch);
        }

        @Override
        public StoreQuery newQuery(String language) {

            return super.newQuery(language);
        }

        @Override
        public FetchConfiguration newFetchConfiguration() {

            return new FetchConfigurationImpl();
        }

        @Override
        public void close() {

            super.close();
        }

        @Override
        public int compareVersion(OpenJPAStateManager state, Object v1, Object v2) {

            return super.compareVersion(state, v1, v2);
        }

        @Override
        public Seq getDataStoreIdSequence(ClassMetaData forClass) {

            return super.getDataStoreIdSequence(forClass);
        }

        @Override
        public Seq getValueSequence(FieldMetaData fmd) {

            return super.getValueSequence(fmd);
        }

        @Override
        public boolean cancelAll() {

            return super.cancelAll();
        }

        @Override
        public boolean isCached(List<Object> oids, BitSet edata) {

            return super.isCached(oids, edata);
        }

    }

}
