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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Implementation of the {@link ManagedRuntime} interface that uses
 * the {@link TransactionSynchronizationRegistry} interface (new in JTA 1.1)
 * to create a {@link TransactionManager} facade for controlling transactions.
 *
 * @author Marc Prud'hommeaux
 * @since 1.0.0
 */
public class RegistryManagedRuntime
    implements ManagedRuntime {
    private String _registryName =
        "java:comp/TransactionSynchronizationRegistry";
    private TransactionManagerRegistryFacade _tm = null;

    private static Localizer _loc =
        Localizer.forPackage(RegistryManagedRuntime.class);

    /**
     * Return the cached TransactionManager instance.
     */
    @Override
    public TransactionManager getTransactionManager() throws Exception {
        if (_tm == null) {
            Context ctx = new InitialContext();
            try {
                _tm = new TransactionManagerRegistryFacade
                    ((TransactionSynchronizationRegistry) ctx.
                        lookup(_registryName));
            } finally {
                ctx.close();
            }
        }
        return _tm;
    }

    @Override
    public void setRollbackOnly(Throwable cause)
        throws Exception {
        // there is no generic support for setting the rollback cause
        getTransactionManager().getTransaction().setRollbackOnly();
    }

    @Override
    public Throwable getRollbackCause()
        throws Exception {
        // there is no generic support for setting the rollback cause
        return null;
    }

    public void setRegistryName(String registryName) {
        _registryName = registryName;
    }

    public String getRegistryName() {
        return _registryName;
    }

    @Override
    public Object getTransactionKey() throws Exception, SystemException {
        return _tm.getTransactionKey();
    }

    /**
     *  A {@link TransactionManager} and {@link Transaction} facade
     *  that delegates the appropriate methods to the internally-held
     *  {@link TransactionSynchronizationRegistry}. Since the
     *  registry is not able to start or end transactions, all transaction
     *  control methods will just throw a {@link SystemException}.
     *
     *  @author  Marc Prud'hommeaux
     */
    public class TransactionManagerRegistryFacade
        implements TransactionManager, Transaction {
        private final TransactionSynchronizationRegistry _registry;

        public TransactionManagerRegistryFacade
            (TransactionSynchronizationRegistry registry) {
            _registry = registry;
        }


        @Override
        public Transaction getTransaction()
            throws SystemException {
            return TransactionManagerRegistryFacade.this;
        }


        @Override
        public void registerSynchronization(Synchronization sync)
            throws RollbackException, IllegalStateException, SystemException {
            _registry.registerInterposedSynchronization(sync);
        }


        @Override
        public void setRollbackOnly()
            throws IllegalStateException, SystemException {
            _registry.setRollbackOnly();
        }


        @Override
        public int getStatus()
            throws SystemException {
            return _registry.getTransactionStatus();
        }

        public Object getTransactionKey() {
            return _registry.getTransactionKey();
        }

        //////////////////////////////
        // Unsupported methods follow
        //////////////////////////////

        @Override
        public void begin()
            throws NotSupportedException, SystemException {
            throw new NotSupportedException();
        }


        @Override
        public void commit()
            throws RollbackException, HeuristicMixedException, SystemException,
                HeuristicRollbackException, SecurityException,
                IllegalStateException {
            throw new SystemException();
        }


        @Override
        public void resume(Transaction tobj)
            throws InvalidTransactionException, IllegalStateException,
                SystemException {
            throw new SystemException();
        }


        @Override
        public void rollback()
            throws IllegalStateException, SecurityException, SystemException {
            throw new SystemException();
        }


        @Override
        public void setTransactionTimeout(int seconds)
            throws SystemException {
            throw new SystemException();
        }


        @Override
        public Transaction suspend()
            throws SystemException {
            throw new SystemException();
        }


        @Override
        public boolean delistResource(XAResource xaRes, int flag)
            throws IllegalStateException, SystemException {
            throw new SystemException();
        }


        @Override
        public boolean enlistResource(XAResource xaRes)
            throws RollbackException, IllegalStateException, SystemException {
            throw new SystemException();
        }
    }

    /**
     * <P>
     * RegistryManagedRuntime cannot suspend transactions.
     * </P>
     */
    @Override
    public void doNonTransactionalWork(Runnable runnable)
        throws NotSupportedException {
        throw new NotSupportedException(_loc.get("tsr-cannot-suspend")
            .getMessage());
    }
}

