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

import java.lang.reflect.Method;

import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * {@link ManagedRuntime} implementation that directly accesses the
 * transaction manager via WebLogic Server helper classes.
 *
 * @author Arunabh Hazarika, Patrick Linskey
 */
public class WLSManagedRuntime extends AbstractManagedRuntime
    implements ManagedRuntime {

    private final Method _txHelperMeth;
    private final Method _txManagerMeth;

    public WLSManagedRuntime()
        throws ClassNotFoundException, NoSuchMethodException {
        Class<?> txHelper = Class.forName("weblogic.transaction.TransactionHelper");
        _txHelperMeth = txHelper.getMethod("getTransactionHelper");
        _txManagerMeth = txHelper.getMethod("getTransactionManager");
    }

    @Override
    public TransactionManager getTransactionManager()
        throws Exception {
        // return TransactionHelper.getTransactionHelper ().
        //	getTransactionManager ();
        Object o = _txHelperMeth.invoke(null);
        return (TransactionManager) _txManagerMeth.invoke(o);
    }

    @Override
    public void setRollbackOnly(Throwable cause)
        throws Exception {
        Transaction transaction = getTransactionManager().getTransaction();
        try {
            // try to use reflection to call the setRollbackOnly(Throwable)
            // method in weblogic.transaction.Transaction
            transaction.getClass().
                getMethod("setRollbackOnly", new Class[] { Throwable.class }).
                    invoke(transaction, new Object[] { cause });
        } catch (Throwable e) {
            // some problem occurred: fall back to the traditional call
            transaction.setRollbackOnly();
        }
    }

    @Override
    public Throwable getRollbackCause()
        throws Exception {
        Transaction transaction = getTransactionManager().getTransaction();
        try {
            // try to use reflection to call the getRollbackReason()
            // method in weblogic.transaction.Transaction
            return (Throwable) transaction.getClass().
                getMethod("getRollbackReason", new Class[0]).
                    invoke(transaction, new Object[0]);
        } catch (Throwable e) {
            // some problem occurred: just return null
            return null;
        }
    }
}
