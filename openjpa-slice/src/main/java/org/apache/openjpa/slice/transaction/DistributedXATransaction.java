package org.apache.openjpa.slice.transaction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
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
    private static ThreadLocal<Transaction> _trans = new ThreadLocal<Transaction>();
    private Set<XAResource> _slices = new HashSet<XAResource>();
    private Set<Synchronization> _syncs = new HashSet<Synchronization>();
    private final TransactionManager _tm;
    private final XID xid;
    private int _status;
    private boolean _rollbackOnly;
    
    /**
     * Construct with 
     * @param xid
     * @param tm
     */
    DistributedXATransaction(XID xid, TransactionManager tm) {
        this.xid = xid;
        this._tm = tm;
    }
    
    public XID getXID() {
        return xid;
    }
    
    public void commit() throws HeuristicMixedException,
            HeuristicRollbackException, RollbackException, SecurityException,
            SystemException {
        _tm.commit();
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
        _tm.rollback();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        _rollbackOnly = true;
    }
    
    Set<XAResource> getEnlistedResources() {
        return Collections.unmodifiableSet(_slices);
    }
}
