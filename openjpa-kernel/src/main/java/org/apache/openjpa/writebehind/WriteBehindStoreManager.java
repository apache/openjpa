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
package org.apache.openjpa.writebehind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.openjpa.kernel.DelegatingStoreManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.kernel.StoreManager;

public class WriteBehindStoreManager extends DelegatingStoreManager {

    protected Set<Class<?>> includedTypes;
    protected Set<Class<?>> excludedTypes;
    
    protected boolean _cacheInserts = true;
    protected boolean _cacheUpdates = true;
    protected boolean _cacheDeletes = true;
    
    public static final int OP_INSERT = 1;
    public static final int OP_DELETE = 2;
    public static final int OP_UPDATE = 3; 

    WriteBehindCache _cache;
    Collection<OpenJPAStateManager> inFlightSMs = new ArrayList<OpenJPAStateManager>();

    public WriteBehindStoreManager(StoreManager store) {
        this(store, null);
    }

    public WriteBehindStoreManager(StoreManager store, 
        WriteBehindCache wbCache) {
        super(store);
        _cache = wbCache;
    }

    public Collection<Exception> flush(Collection<OpenJPAStateManager> sms) {
        LinkedHashSet<OpenJPAStateManager> passingThrough = null;
        for (OpenJPAStateManager sm : sms) {
            if (cacheAble(sm)) {
                PCState newState = sm.getPCState();
                if (newState == PCState.PDELETEDFLUSHED) { 
                    newState = PCState.PDELETED; // effectively reset the flush
                }
                inFlightSMs.add(new StateManagerImpl((StateManagerImpl)sm, newState));
            } else {
                if (passingThrough == null) { 
                    passingThrough = new LinkedHashSet<OpenJPAStateManager>();
                }
                passingThrough.add(sm);
            }
        }
        
        Collection<Exception> rval;
        if (passingThrough != null) { 
            rval = getDelegate().flush(passingThrough);
        } else { 
            rval = new ArrayList<Exception>();
        }
        return rval;
    }

    public void commit() {
        try {
            super.commit();
            _cache.add(inFlightSMs);
        } finally {
            inFlightSMs.clear();
        }
    }

    public void rollback() {
        try {
            super.rollback();
        } finally {
            inFlightSMs.clear();
        }
    }

    public Collection<Exception> flushBehind(Collection<OpenJPAStateManager> sms) {
        return super.flush(sms);
    }

    public boolean cacheAble(OpenJPAStateManager sm) {
        boolean rval = false;
        switch (getOperation(sm)) {
        case OP_INSERT:
            if (_cacheInserts) {
                rval = true;
            }
            break;
        case OP_DELETE:
            if (_cacheDeletes) {
                rval = true;
            }
            break;
        case OP_UPDATE:
            if (_cacheUpdates) {
                rval = true;
            }
            break;
            default:
                rval = false; // not sure what this is. Don't mess with it.
        }
        
        // TODO Check includedTypes
        // Like the DataCache this information should be contained in the 
        // Mapping repository
        // Check after checking operations since unlisted types will pre-empt 
        // operations.

        return rval;
    }
    
    protected int getOperation(OpenJPAStateManager sm) {
        int rval = -1; // TODO define me
        if (sm.isDirty()) {
            if (sm.isNew()) {
                rval = OP_INSERT;
            } else if (sm.isDeleted()) {
                rval = OP_DELETE;
            } else {
                rval = OP_UPDATE;
            }
        }
        return rval;
    }

}
