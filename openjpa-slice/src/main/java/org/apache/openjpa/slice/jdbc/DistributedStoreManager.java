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
package org.apache.openjpa.slice.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.ConfigurationException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.ConnectionInfo;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.JDBCStoreManager;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.ResultSetResult;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.slice.DistributionPolicy;
import org.apache.openjpa.slice.transaction.DistributedNaiveTransaction;
import org.apache.openjpa.slice.transaction.DistributedTransactionManager;
import org.apache.openjpa.slice.transaction.NaiveTransactionManager;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UserException;

/**
 * A Store manager for multiple physical databases referred as <em>slice</em>.
 * This receiver behaves like a Transaction Manager as it implements two-phase
 * commit protocol if all the component slices is XA-complaint. The actions are
 * delegated to the underlying slices. The actions are executed in parallel
 * threads whenever possible such as flushing or query. <br>
 * 
 * @author Pinaki Poddar
 * 
 */
class DistributedStoreManager extends JDBCStoreManager {
    private final List<SliceStoreManager> _slices;
    private JDBCStoreManager _master;
    private boolean isXA;
    private TransactionManager _tm;
    private final DistributedJDBCConfiguration _conf;
    private boolean _active = false;
    private Log _log;
    private static final Localizer _loc =
            Localizer.forPackage(DistributedStoreManager.class);
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Constructs a set of child StoreManagers each connected to a physical
     * DataSource.
     * 
     * The supplied configuration carries multiple URL for underlying physical
     * slices. The first slice is referred as <em>master</em> and is used to
     * get Sequence based entity identifiers.
     */
    public DistributedStoreManager(DistributedJDBCConfiguration conf) {
        super();
        _conf = conf;
        _log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        _slices = new ArrayList<SliceStoreManager>();
        for (String name : conf.getActiveSliceNames()) {
            SliceStoreManager slice = new SliceStoreManager
                (conf.getSlice(name));
            _slices.add(slice);
            if (slice.getName().equals(conf.getMaster().getName()))
                _master = slice;
        }
    }

    public DistributedJDBCConfiguration getConfiguration() {
        return _conf;
    }

    /**
     * Decides the index of the StoreManager by first looking at the
     * implementation data. If not found then {@link DistributionPolicy
     * DistributionPolicy} determines the target store for new instances and
     * additional connection info is used to estimate for the existing
     * instances.
     */
    protected String findSliceName(OpenJPAStateManager sm, Object info) {
        boolean hasIndex = hasSlice(sm);
        if (hasIndex)
            return sm.getImplData().toString();
        String slice = estimateSlice(sm, info);
        if (slice == null)
            return assignSlice(sm);
        return slice;
    }

    private boolean hasSlice(OpenJPAStateManager sm) {
        return sm.getImplData() != null;
    }

    private String assignSlice(OpenJPAStateManager sm) {
        PersistenceCapable pc = sm.getPersistenceCapable();
        String slice =
                _conf.getDistributionPolicyInstance().distribute(pc,
                        _conf.getActiveSliceNames(), getContext());
        if (!_conf.getActiveSliceNames().contains(slice)) {
            throw new UserException(_loc.get("bad-policy-slice", new Object[] {
                    _conf.getDistributionPolicyInstance().getClass().getName(),
                    slice, sm.getPersistenceCapable(), 
                    _conf.getActiveSliceNames() }));
        }
        sm.setImplData(slice, true);
        return slice;
    }

    /**
     * The additional edata is used, if possible, to find the StoreManager
     * managing the given StateManager. If the additional data is unavailable
     * then return null.
     * 
     */
    private String estimateSlice(OpenJPAStateManager sm, Object edata) {
        if (edata == null || !(edata instanceof ConnectionInfo))
            return null;

        Result result = ((ConnectionInfo) edata).result;
        if (result instanceof ResultSetResult) {
            JDBCStore store = ((ResultSetResult) result).getStore();
            for (SliceStoreManager slice : _slices) {
                if (slice == store) {
                    String sliceId = slice.getName();
                    sm.setImplData(sliceId, true);
                    return sliceId;
                }
            }
        }
        return null;
    }

    /**
     * Selects a child StoreManager where the given instance resides.
     */
    private StoreManager selectStore(OpenJPAStateManager sm, Object edata) {
        String name = findSliceName(sm, edata);
        SliceStoreManager slice = lookup(name);
        if (slice == null)
            throw new InternalException(_loc.get("wrong-slice", name, sm));
        return slice;
    }

    public boolean assignField(OpenJPAStateManager sm, int field,
            boolean preFlush) {
        return selectStore(sm, null).assignField(sm, field, preFlush);
    }

    public boolean assignObjectId(OpenJPAStateManager sm, boolean preFlush) {
        return _master.assignObjectId(sm, preFlush);
    }

    public void beforeStateChange(OpenJPAStateManager sm, PCState fromState,
            PCState toState) {
        _master.beforeStateChange(sm, fromState, toState);
    }

    public void begin() {
        if (_active)
            return;
        _active = true;
        TransactionManager tm = getTransactionManager();
        for (SliceStoreManager slice : _slices) {
            try {
                Transaction txn = tm.getTransaction();
                if (isXA) {
                    txn.enlistResource(slice.getXAConnection().getXAResource());
                } else { // This is the only place where casting to our
                         // internal implementation classes become necessary
                    ((DistributedNaiveTransaction) txn).enlistResource(slice);
                }
            } catch (Exception e) {
                throw new InternalException(e);
            }
        }

        try {
            tm.begin();
        } catch (Exception e) {
            throw new StoreException(e);
        }
    }

    Log getLog(SliceStoreManager slice) {
        return slice.getConfiguration()
                .getLog(OpenJPAConfiguration.LOG_RUNTIME);
    }

    public void beginOptimistic() {
        for (SliceStoreManager slice : _slices)
            slice.beginOptimistic();
    }

    public boolean cancelAll() {
        boolean ret = true;
        for (SliceStoreManager slice : _slices)
            ret = slice.cancelAll() & ret;
        return ret;
    }

    public void close() {
        _active = false;
        for (SliceStoreManager slice : _slices)
            slice.close();
    }

    public void commit() {
        if (!_active) 
            return;
        TransactionManager tm = getTransactionManager();
        try {
            tm.commit();
        } catch (Exception e) {
            throw new StoreException(e);
        } finally {
            _active = false;
        }
    }

    public int compareVersion(OpenJPAStateManager sm, Object v1, Object v2) {
        return selectStore(sm, null).compareVersion(sm, v1, v2);
    }

    public Object copyDataStoreId(Object oid, ClassMetaData meta) {
        return _master.copyDataStoreId(oid, meta);
    }

    public ResultObjectProvider executeExtent(ClassMetaData meta,
            boolean subclasses, FetchConfiguration fetch) {
        ResultObjectProvider[] tmp = new ResultObjectProvider[_slices.size()];
        int i = 0;
        for (SliceStoreManager slice : _slices) {
            tmp[i++] = slice.executeExtent(meta, subclasses, fetch);
        }
        return new MergedResultObjectProvider(tmp);
    }

    public boolean exists(OpenJPAStateManager sm, Object edata) {
        for (SliceStoreManager slice : _slices) {
            if (slice.exists(sm, edata)) {
                sm.setImplData(slice.getName(), true);
                return true;
            }
        }
        return false;
    }

    
    /**
     * Flush the given StateManagers after binning them to respective physical
     * slices.
     */
    public Collection flush(Collection sms) {
        Collection exceptions = new ArrayList();
        List<Future<Collection>> futures = new ArrayList<Future<Collection>>();
        Map<String, List<OpenJPAStateManager>> subsets = bin(sms, null);
        for (SliceStoreManager slice : _slices) {
            List<OpenJPAStateManager> subset = subsets.get(slice.getName());
            if (subset.isEmpty())
                continue;
            futures.add(threadPool.submit(new Flusher(slice, subset)));
        }
        for (Future<Collection> future : futures) {
            Collection error;
            try {
                error = future.get();
                if (!(error == null || error.isEmpty())) {
                    exceptions.addAll(error);
                }
            } catch (InterruptedException e) {
                throw new StoreException(e);
            } catch (ExecutionException e) {
                throw new StoreException(e.getCause());
            }
        }
        return exceptions;
    }
    
    /**
     * Separate the given list of StateManagers in separate lists for each slice 
     * by the associated slice identifier of each StateManager.
     * @param sms
     * @param edata
     * @return
     */
    private Map<String, List<OpenJPAStateManager>> bin(
            Collection/*<StateManage>*/ sms, Object edata) {
        Map<String, List<OpenJPAStateManager>> subsets =
                new HashMap<String, List<OpenJPAStateManager>>();
        for (SliceStoreManager slice : _slices)
            subsets.put(slice.getName(), new ArrayList<OpenJPAStateManager>());
        for (Object x : sms) {
            OpenJPAStateManager sm = (OpenJPAStateManager) x;
            String slice = findSliceName(sm, edata);
            subsets.get(slice).add(sm);
        }
        return subsets;
    }

    public Object getClientConnection() {
        throw new UnsupportedOperationException();
    }

    public Seq getDataStoreIdSequence(ClassMetaData forClass) {
        return _master.getDataStoreIdSequence(forClass);
    }

    public Class getDataStoreIdType(ClassMetaData meta) {
        return _master.getDataStoreIdType(meta);
    }

    public Class getManagedType(Object oid) {
        return _master.getManagedType(oid);
    }

    public Seq getValueSequence(FieldMetaData forField) {
        return _master.getValueSequence(forField);
    }

    public boolean initialize(OpenJPAStateManager sm, PCState state,
            FetchConfiguration fetch, Object edata) {
        if (edata instanceof ConnectionInfo) {
            String slice = findSliceName(sm, (ConnectionInfo) edata);
            if (slice != null)
                return lookup(slice).initialize(sm, state, fetch, edata);
        }
        // not a part of Query result load. Look into the slices till found
        for (SliceStoreManager slice : _slices) {
            if (slice.initialize(sm, state, fetch, edata)) {
                sm.setImplData(slice.getName(), true);
                return true;
            }
        }
        return false;

    }

    public boolean load(OpenJPAStateManager sm, BitSet fields,
            FetchConfiguration fetch, int lockLevel, Object edata) {
        return selectStore(sm, edata).load(sm, fields, fetch, lockLevel, edata);
    }

    public Collection loadAll(Collection sms, PCState state, int load,
            FetchConfiguration fetch, Object edata) {
        Map<String, List<OpenJPAStateManager>> subsets = bin(sms, edata);
        Collection result = new ArrayList();
        for (SliceStoreManager slice : _slices) {
            List<OpenJPAStateManager> subset = subsets.get(slice.getName());
            if (subset.isEmpty())
                continue;
            Collection tmp = slice.loadAll(subset, state, load, fetch, edata);
            if (tmp != null && !tmp.isEmpty())
                result.addAll(tmp);
        }
        return result;
    }

    public Object newDataStoreId(Object oidVal, ClassMetaData meta) {
        return _master.newDataStoreId(oidVal, meta);
    }

    public FetchConfiguration newFetchConfiguration() {
        return _master.newFetchConfiguration();
    }

    /**
     * Construct a distributed query to be executed against all the slices.
     */
    public StoreQuery newQuery(String language) {
        ExpressionParser parser = QueryLanguages.parserForLanguage(language);
        DistributedStoreQuery ret = new DistributedStoreQuery(this, parser);
        for (SliceStoreManager slice : _slices) {
            ret.add(slice.newQuery(language));
        }
        return ret;
    }

    public void releaseConnection() {
        for (SliceStoreManager slice : _slices)
            slice.releaseConnection();

    }

    public void retainConnection() {
        for (SliceStoreManager slice : _slices)
            slice.retainConnection();
    }

    public void rollback() {
        if (!_active)
            return;
        TransactionManager tm = getTransactionManager();
        try {
            tm.rollback();
        } catch (Exception e) {
            throw new StoreException(e);
        } finally {
            _active = false;
        }
    }

    public void rollbackOptimistic() {
        for (SliceStoreManager slice : _slices)
            slice.rollbackOptimistic();
    }

    /**
     * Sets the context for this receiver and all its underlying slices.
     */
    public void setContext(StoreContext ctx) {
        super.setContext(ctx);
        isXA = true;
        for (SliceStoreManager store : _slices) {
            store.setContext(ctx, 
                    (JDBCConfiguration)store.getSlice().getConfiguration());
            isXA &= store.isXAEnabled();
        }
        _tm = getTransactionManager();
    }

    private SliceStoreManager lookup(String name) {
        for (SliceStoreManager slice : _slices)
            if (slice.getName().equals(name))
                return slice;
        return null;
    }

    public boolean syncVersion(OpenJPAStateManager sm, Object edata) {
        return selectStore(sm, edata).syncVersion(sm, edata);
    }

    protected TransactionManager getTransactionManager() {
        if (_tm == null) {
            _tm = getConfiguration().getTransactionManagerInstance();
            String alias = getConfiguration().getTransactionManager();
            boolean is2pc = !(_tm instanceof NaiveTransactionManager);
            if (isXA) { 
                if (!is2pc) { 
                    _log.warn(_loc.get("resource-xa-tm-not-2pc", alias));
                    isXA = false;
                }
            } else if (is2pc) {
                throw new UserException(_loc.get("resource-not-xa-tm-2pc", 
                        alias));
            } 
        }
        return _tm;
    }
    
    @Override
    protected RefCountConnection connectInternal() throws SQLException {
        List<Connection> list = new ArrayList<Connection>();
        for (SliceStoreManager slice : _slices)
            list.add(slice.getConnection());
        DistributedConnection con = new DistributedConnection(list);
        return new RefCountConnection(con);
    }

    private static class Flusher implements Callable<Collection> {
        final SliceStoreManager store;
        final Collection toFlush;

        Flusher(SliceStoreManager store, Collection toFlush) {
            this.store = store;
            this.toFlush = toFlush;
        }

        public Collection call() throws Exception {
            return store.flush(toFlush);
        }
    }

}
