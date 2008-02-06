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

import static javax.transaction.xa.XAResource.TMJOIN;
import static javax.transaction.xa.XAResource.TMNOFLAGS;
import static javax.transaction.xa.XAResource.TMSUCCESS;

import java.util.Set;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.apache.openjpa.lib.util.Localizer;

/**
 * A simple (naive?) implementation for distributed transaction across 
 * XA-complaint data sources. 
 * Assumes begin() and commit() being called on the same thread.
 * 
 * @author Pinaki Poddar
 * 
 */
public class DistributedTransactionManager implements TransactionManager {
    private final ThreadLocal<DistributedXATransaction> txns =
            new ThreadLocal<DistributedXATransaction>();
    private static final Localizer _loc =
            Localizer.forPackage(DistributedTransactionManager.class);

    public void begin() throws NotSupportedException, SystemException {
        DistributedXATransaction txn = getTransaction(false);
        int i = 1;
        Set<XAResource> resources = txn.getEnlistedResources();
        for (XAResource resource : resources) {
            try {
                XAResource existing = isSame(resource, resources);
                XID branch = txn.getXID().branch(i++);
                int flag = (existing == null) ? TMNOFLAGS : TMJOIN;
                resource.start(branch, flag);
            } catch (Exception e) {
                throw new SystemException(e.toString());
            }
        }
    }

    public void commit() throws HeuristicMixedException,
            HeuristicRollbackException, IllegalStateException,
            RollbackException, SecurityException, SystemException {
        DistributedXATransaction txn = getTransaction(true);
        Set<XAResource> resources = txn.getEnlistedResources();
        int branchId = 1;
        boolean nextPhase = true;
        for (XAResource resource : resources) {
            XID branch = txn.getXID().branch(branchId++);
            try {
                resource.end(branch, TMSUCCESS);
                resource.prepare(branch);
            } catch (XAException e) {
                nextPhase = false;
            }
        }

        branchId = 1; // reset
        if (!nextPhase) {
            for (XAResource resource : resources) {
                try {
                    XID branch = txn.getXID().branch(branchId++);
                    resource.forget(branch);
                } catch (XAException e) {
                    // ignore
                }
                throw new SystemException(_loc.get("prepare-failed")
                        .getMessage());
            }
        }

        branchId = 1; // reset
        for (XAResource resource : resources) {
            XID branch = txn.getXID().branch(branchId++);
            try {
                resource.commit(branch, false);
            } catch (XAException e) {
                throw new SystemException(e.getMessage());
            }
        }
    }

    public int getStatus() throws SystemException {
        return getTransaction().getStatus();
    }

    public Transaction getTransaction() throws SystemException {
        return getTransaction(false);
    }

    public void resume(Transaction arg0) throws IllegalStateException,
            InvalidTransactionException, SystemException {
        throw new UnsupportedOperationException();
    }

    public void rollback() throws IllegalStateException, SecurityException,
            SystemException {
        DistributedXATransaction txn = getTransaction(true);
        Set<XAResource> slices = txn.getEnlistedResources();
        int branchId = 1;
        for (XAResource slice : slices) {
            XID branch = txn.getXID().branch(branchId++);
            try {
                slice.end(branch, XAResource.TMFAIL);
                slice.rollback(branch);
            } catch (XAException e) {
            }
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        getTransaction().setRollbackOnly();
    }

    public void setTransactionTimeout(int arg0) throws SystemException {
        throw new UnsupportedOperationException();
    }

    public Transaction suspend() throws SystemException {
        throw new UnsupportedOperationException();
    }

    XAResource isSame(XAResource rm, Set<XAResource> others) {
        for (XAResource other : others)
            try {
                if (rm != other && other.isSameRM(rm))
                    return other;
            } catch (XAException e) {
                e.printStackTrace();
            }
        return null;
    }

    String toString(Object o) {
        return o.getClass().getSimpleName() + "@"
                + Long.toHexString(System.identityHashCode(o));
    }

    /**
     * Gets the transaction associated with the current thread. 
     * 
     * @param mustExist if true, a transaction must be associated with the 
     * current thread a priori. If false, the current thread has no associated
     * transaction, a new transaction is created with a global identifier 
     * and associated with the current thread.
     */
    DistributedXATransaction getTransaction(boolean mustExist) {
        DistributedXATransaction txn = txns.get();
        if (txn == null) {
            if (mustExist)
                throw new IllegalStateException(_loc.get("no-txn-on-thread",
                        Thread.currentThread().getName()).getMessage());
            byte[] global =
                    Long.toHexString(System.currentTimeMillis()).getBytes();
            XID xid = new XID(0, global, new byte[] { 0x1 });
            txn = new DistributedXATransaction(xid, this);
            txns.set(txn);
        }
        return txn;
    }

}
