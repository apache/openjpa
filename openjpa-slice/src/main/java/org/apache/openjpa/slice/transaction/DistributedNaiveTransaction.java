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

import org.apache.openjpa.slice.jdbc.SliceStoreManager;

public class DistributedNaiveTransaction implements Transaction {
    private Set<SliceStoreManager> _slices = new HashSet<SliceStoreManager>();
    private Set<Synchronization> _syncs = new HashSet<Synchronization>();
    private final TransactionManager _tm;
    private int _status;
    private boolean _rollbackOnly;

    DistributedNaiveTransaction(TransactionManager tm) {
        _tm = tm;
    }

    public void commit() throws HeuristicMixedException,
            HeuristicRollbackException, RollbackException, SecurityException,
            SystemException {
        throw new UnsupportedOperationException();
    }

    public boolean delistResource(XAResource arg0, int arg1)
            throws IllegalStateException, SystemException {
        return _slices.remove(arg0);
    }

    public boolean enlistResource(XAResource arg0)
            throws IllegalStateException, RollbackException, SystemException {
        throw new UnsupportedOperationException();
    }

    public boolean enlistResource(SliceStoreManager arg0)
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
    
    public boolean isRollbackOnly() {
        return _rollbackOnly;
    }

    Set<SliceStoreManager> getEnlistedResources() {
        return Collections.unmodifiableSet(_slices);
    }

}
