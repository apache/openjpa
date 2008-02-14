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
package org.apache.openjpa.slice.transaction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

/**
 * Internal implementation of a Transaction with an internal global transaction
 * identifier scheme.
 * 
 * @author Pinaki Poddar 
 *
 */
class DistributedXATransaction implements Transaction {
    private Set<XAResource> _slices = new HashSet<XAResource>();
    private Set<Synchronization> _syncs = new HashSet<Synchronization>();
//    private final TransactionManager _tm;
    private final XID xid;
    private int _status;
    
    /**
     * Construct with 
     * @param xid
     * @param tm
     */
    DistributedXATransaction(XID xid, TransactionManager tm) {
        this.xid = xid;
        _status = Status.STATUS_ACTIVE;
    }
    
    public XID getXID() {
        return xid;
    }
    
    public void commit() throws HeuristicMixedException,
            HeuristicRollbackException, RollbackException, SecurityException,
            SystemException {
        _status = Status.STATUS_COMMITTED;
        _slices.clear();
    }

    public boolean delistResource(XAResource arg0, int arg1)
            throws IllegalStateException, SystemException {
        return _slices.remove(arg0);
    }

    public boolean enlistResource(XAResource arg0)
            throws IllegalStateException, RollbackException, SystemException {
        return _slices.add(arg0);
    }

    public int getStatus() throws SystemException {
        return _status;
    }

    public void registerSynchronization(Synchronization arg0)
            throws IllegalStateException, RollbackException, SystemException {
        _syncs.add(arg0);
    }

    public void rollback() throws IllegalStateException, SystemException {
        _status = Status.STATUS_ROLLEDBACK;
        _slices.clear();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        _status = Status.STATUS_MARKED_ROLLBACK;
    }
    
    Set<XAResource> getEnlistedResources() {
        return Collections.unmodifiableSet(_slices);
    }
}
