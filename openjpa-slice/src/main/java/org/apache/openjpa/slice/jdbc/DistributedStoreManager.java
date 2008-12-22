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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.ConnectionInfo;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.JDBCStoreManager;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.ResultSetResult;
import org.apache.openjpa.kernel.BrokerImpl;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.slice.ProductDerivation;
import org.apache.openjpa.slice.SliceImplHelper;
import org.apache.openjpa.slice.SliceInfo;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.StoreException;

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
    private final DistributedJDBCConfiguration _conf;
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
        _slices = new ArrayList<SliceStoreManager>();
        List<String> sliceNames = conf.getActiveSliceNames();
        for (String name : sliceNames) {
            SliceStoreManager slice = new SliceStoreManager
                (conf.getSlice(name));
            _slices.add(slice);
            if (name.equals(conf.getMaster().getName()))
                _master = slice;
        }
    }

    public DistributedJDBCConfiguration getConfiguration() {
        return _conf;
    }
    
    public SliceStoreManager getSlice(int i) {
    	return _slices.get(i);
    }

    /**
     * Decides the index of the StoreManager by first looking at the
     * implementation data. If no implementation data is found, then estimates 
     * targets slices by using additional connection info. If no additional
     * connection info then calls back to user-defined policy. 
     */
    protected SliceInfo findSliceNames(OpenJPAStateManager sm, Object edata) {
        if (SliceImplHelper.isSliceAssigned(sm))
            return SliceImplHelper.getSliceInfo(sm);
        SliceInfo result = null;
        PersistenceCapable pc = sm.getPersistenceCapable();
        Object ctx = getContext();
        if (SliceImplHelper.isReplicated(sm)) {
            result = SliceImplHelper.getSlicesByPolicy(pc, _conf, ctx);
        } else {
            String origin = estimateSlice(sm, edata);
            if (origin == null) {
                result = SliceImplHelper.getSlicesByPolicy(pc, _conf, ctx);
            } else {
                result = new SliceInfo(origin);
            }
        }
        return result;
    }
    
    private void assignSlice(OpenJPAStateManager sm, String hint) {
        if (SliceImplHelper.isReplicated(sm)) {
            SliceImplHelper.getSlicesByPolicy(sm, _conf, getContext())
                .setInto(sm);
            return;
        }
        new SliceInfo(hint).setInto(sm);
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
                    return slice.getName();
                }
            }
        }
        return null; 
    }

    /**
     * Selects child StoreManager(s) where the given instance resides.
     */
    private StoreManager selectStore(OpenJPAStateManager sm, Object edata) {
        String[] targets = findSliceNames(sm, edata).getSlices();
        for (String target : targets) {
        	SliceStoreManager slice = lookup(target);
        	if (slice == null)
        		throw new InternalException(_loc.get("wrong-slice", target, sm));
        	return slice;
        }
        return null;
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

    public int compareVersion(OpenJPAStateManager sm, Object v1, Object v2) {
        return selectStore(sm, null).compareVersion(sm, v1, v2);
    }

    public Object copyDataStoreId(Object oid, ClassMetaData meta) {
        return _master.copyDataStoreId(oid, meta);
    }

    public ResultObjectProvider executeExtent(ClassMetaData meta,
            boolean subclasses, FetchConfiguration fetch) {
        int i = 0;
        List<SliceStoreManager> targets = getTargets(fetch);
        ResultObjectProvider[] tmp = new ResultObjectProvider[targets.size()];
        for (SliceStoreManager slice : targets) {
            tmp[i++] = slice.executeExtent(meta, subclasses, fetch);
        }
        return new MergedResultObjectProvider(tmp);
    }

    public boolean exists(OpenJPAStateManager sm, Object edata) {
    	String origin = null;
        for (SliceStoreManager slice : _slices) {
            if (slice.exists(sm, edata)) {
            	origin = slice.getName();
            	break;
            }
        }
        if (origin != null)
            assignSlice(sm, origin);
        return origin != null;
    }

    
    /**
     * Flush the given StateManagers after binning them to respective physical
     * slices.
     */
    public Collection flush(Collection sms) {
        Collection exceptions = new ArrayList();
        List<Future<Collection>> futures = new ArrayList<Future<Collection>>();
        Map<String, List<OpenJPAStateManager>> subsets = bin(sms, null);
        
        boolean parallel = !getConfiguration().getMultithreaded();
        for (int i = 0; i < _slices.size(); i++) {
            SliceStoreManager slice = _slices.get(i);
            List<OpenJPAStateManager> subset = subsets.get(slice.getName());
            if (subset.isEmpty())
                continue;
            if (containsReplicated(subset)) {
                Map<OpenJPAStateManager, Object> versions = cacheVersion(subset);
            	collectException(slice.flush(subset), exceptions);
            	if (i != _slices.size()-1)
            	    rollbackVersion(subset, versions);
            } else {
            	if (parallel) {
            		futures.add(threadPool.submit(new Flusher(slice, subset)));
            	} else {
                	collectException(slice.flush(subset), exceptions);
            	}
            }
        }
        if (parallel) {
	        for (Future<Collection> future : futures) {
	            try {
	            	collectException(future.get(), exceptions);
	            } catch (InterruptedException e) {
	                throw new StoreException(e);
	            } catch (ExecutionException e) {
	                throw new StoreException(e.getCause());
	            }
	        }
        }
	    return exceptions;
    }
    
    private void collectException(Collection error,  Collection holder) {
        if (!(error == null || error.isEmpty())) {
        	holder.addAll(error);
        }
    }
    
    boolean containsReplicated(List<OpenJPAStateManager> sms) {
    	for (OpenJPAStateManager sm : sms)
    		if (sm.getMetaData().isReplicated())
    			return true;
    	return false;
    }
    
    private Map<OpenJPAStateManager, Object> cacheVersion
        (List<OpenJPAStateManager> sms) {
        Map<OpenJPAStateManager, Object> result = 
               new HashMap<OpenJPAStateManager, Object>();
        for (OpenJPAStateManager sm : sms)
            if (sm.getMetaData().isReplicated())
                result.put(sm, sm.getVersion());
        return result;
    }
    
    private void rollbackVersion(List<OpenJPAStateManager> sms, 
        Map<OpenJPAStateManager, Object> result) {
        for (OpenJPAStateManager sm : sms)
            if (sm.getMetaData().isReplicated())
                sm.setVersion(result.get(sm));
    }
    
    /**
     * Separate the given list of StateManagers in separate lists for each slice 
     * by the associated slice identifier of each StateManager.
     */
    private Map<String, List<OpenJPAStateManager>> bin(
            Collection/*<StateManage>*/ sms, Object edata) {
        Map<String, List<OpenJPAStateManager>> subsets =
                new HashMap<String, List<OpenJPAStateManager>>();
        for (SliceStoreManager slice : _slices)
            subsets.put(slice.getName(), new ArrayList<OpenJPAStateManager>());
        for (Object x : sms) {
            OpenJPAStateManager sm = (OpenJPAStateManager) x;
            String[] targets = findSliceNames(sm, edata).getSlices();
           	for (String slice : targets) {
            	subsets.get(slice).add(sm);
            }
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
            String origin = estimateSlice(sm, edata);
            if (origin != null) {
                if (lookup(origin).initialize(sm, state, fetch, edata)) {
                    assignSlice(sm, origin);
                    return true;
                }
            }
        }
        // not a part of Query result load. Look into the slices till found
        List<SliceStoreManager> targets = getTargets(fetch);
        for (SliceStoreManager slice : targets) {
            if (slice.initialize(sm, state, fetch, edata)) {
                assignSlice(sm, slice.getName());
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

    /**
     * Sets the context for this receiver and all its underlying slices.
     */
    public void setContext(StoreContext ctx) {
        super.setContext(ctx);
        for (SliceStoreManager store : _slices) {
            store.setContext(ctx, 
                    (JDBCConfiguration)store.getSlice().getConfiguration());
        }
    }

    private SliceStoreManager lookup(String name) {
        for (SliceStoreManager slice : _slices)
            if (slice.getName().equals(name))
                return slice;
        return null;
    }

    public boolean syncVersion(OpenJPAStateManager sm, Object edata) {
    	String[] targets = findSliceNames(sm, edata).getSlices();
    	boolean sync = true;
    	for (String replica : targets) {
    		SliceStoreManager slice = lookup(replica);
    		sync &= slice.syncVersion(sm, edata);
    	}
    	return sync;
    }

    @Override
    protected RefCountConnection connectInternal() throws SQLException {
        List<Connection> list = new ArrayList<Connection>();
        for (SliceStoreManager slice : _slices)
            list.add(slice.getConnection());
        DistributedConnection con = new DistributedConnection(list);
        return new RefCountConnection(con);
    }
    
    /**
     * Gets the list of slices mentioned as  
     * {@link ProductDerivation#HINT_TARGET hint} of the given
     * {@link FetchConfiguration#getHint(String) fetch configuration}. 
     * 
     * @return all active slices if a) the hint is not specified or b) a null 
     * value or c) a non-String or d) matches no active slice.
     */
    List<SliceStoreManager> getTargets(FetchConfiguration fetch) {
        if (fetch == null)
            return _slices;
        Object hint = fetch.getHint(ProductDerivation.HINT_TARGET);
        if (hint == null || !(hint instanceof String)) 
            return _slices;
        List<String> targetNames = Arrays.asList(hint.toString().split("\\,"));
        List<SliceStoreManager> targets = new ArrayList<SliceStoreManager>();
        for (SliceStoreManager slice : _slices) {
           if (targetNames.contains(slice.getName()))
              targets.add(slice);
           }
          if (targets.isEmpty())
            return _slices;
        return targets;
    }
    
    void log(String s) {
        System.out.println("["+Thread.currentThread().getName()+"] " + this + s);
    }

    private static class Flusher implements Callable<Collection> {
        final SliceStoreManager store;
        final Collection toFlush;

        Flusher(SliceStoreManager store, Collection toFlush) {
            this.store = store;
            this.toFlush = toFlush;
        }

        public Collection call() throws Exception {
        	((BrokerImpl)store.getContext()).startLocking();
        	try {
        		return store.flush(toFlush);
        	} finally {
            	((BrokerImpl)store.getContext()).stopLocking();
        	}
        }
    }

}
