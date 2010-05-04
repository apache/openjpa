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
package org.apache.openjpa.slice;

import java.util.Map;

import org.apache.openjpa.kernel.FinalizingBrokerImpl;
import org.apache.openjpa.kernel.OpCallbacks;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.lib.util.Localizer;

/**
 * A specialized Broker to associate slice identifiers with the StateManagers as
 * they are persisted in a cascade. This intervention helps the user to define
 * distribution policy only for root instances i.e. the instances that are
 * explicit argument to persist() call. The cascaded instances are assigned the
 * same slice to honor collocation constraint.
 * 
 * @author Pinaki Poddar
 * 
 */
@SuppressWarnings("serial")
public class DistributedBrokerImpl extends FinalizingBrokerImpl 
    implements DistributedBroker {
	private transient String _rootSlice;
	private transient DistributedConfiguration _conf;
	private final ReentrantSliceLock _lock;
	
	private static final Localizer _loc = Localizer.forPackage(DistributedBrokerImpl.class);

	public DistributedBrokerImpl() {
	    super();
	    _lock = new ReentrantSliceLock();
	}
	
    public DistributedConfiguration getConfiguration() {
    	if (_conf == null) {
    		_conf = (DistributedConfiguration)super.getConfiguration();
    	}
        return _conf;
    }
    
    public DistributedStoreManager getDistributedStoreManager() {
        return (DistributedStoreManager)getStoreManager().getInnermostDelegate();
    }
    
	/**
     * Assigns slice identifier to the resultant StateManager as initialized by
	 * the super class implementation. The slice identifier is decided by
	 * {@link DistributionPolicy} for given <code>pc</code> if it is a root
     * instance i.e. the argument of the user application's persist() call. The
	 * cascaded instances are detected by non-empty status of the current
	 * operating set. The slice is assigned only if a StateManager has never
	 * been assigned before.
	 */
	@Override
    public OpenJPAStateManager persist(Object pc, Object id, boolean explicit, OpCallbacks call) {
		OpenJPAStateManager sm = getStateManager(pc);
		SliceInfo info = null;
        boolean replicated = SliceImplHelper.isReplicated(pc,
                getConfiguration());
        if (getOperatingSet().isEmpty()	&& !SliceImplHelper.isSliceAssigned(sm))
        {
            info = SliceImplHelper.getSlicesByPolicy(pc, getConfiguration(),
				this);
			_rootSlice = info.getSlices()[0]; 
		}
		sm = super.persist(pc, id, explicit, call);
		if (!SliceImplHelper.isSliceAssigned(sm)) {
			if (info == null) {
			   info = replicated 
               ? SliceImplHelper.getSlicesByPolicy(pc, getConfiguration(), this)
			   : new SliceInfo(_rootSlice); 
			}
			info.setInto(sm);
		}
		return sm;
	}

	
	@Override
	public boolean endOperation() {
	    try {
	        return super.endOperation();
	    } catch (Exception ex) {
	        
	    }
	    return true;
	}
	
    /**
     * Create a new query.
     */
    protected QueryImpl newQueryImpl(String lang, StoreQuery sq) {
        return new DistributedQueryImpl(this, lang, sq);
    }
    
	/**
	 * Always uses lock irrespective of super's multi-threaded settings.
	 */
    @Override
    public void lock() {
        _lock.lock();
    }
    
    @Override
    public void unlock() {
        _lock.unlock();
    }
	
	/**
	 * A virtual datastore need not be opened.
	 */
	@Override
	public void beginStore() {
	}
}
