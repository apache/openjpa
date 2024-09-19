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
package org.apache.openjpa.jta;

import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.apache.openjpa.ee.ManagedRuntime;
/**
 * An implementation of {@linkplain ManagedRuntime managed runtime} for testing
 * JTA resources in a JSE environment without a <em>real</em> and often heavy
 * container. Uses a {@linkplain simple} (or basic) TransactionManager.
 *
 * @author Pinaki Poddar
 *
 */
public class JTAManagedRuntime implements ManagedRuntime {
    private SimpleTransactionManager txm = new SimpleTransactionManager();

    @Override
    public void doNonTransactionalWork(Runnable runnable) throws NotSupportedException {
        throw new NotSupportedException();
    }

    @Override
    public Throwable getRollbackCause() throws Exception {
        return txm.getTransaction().getRollbackCause();
    }

    @Override
    public Object getTransactionKey() throws Exception, SystemException {
        return getTransactionManager().getTransaction().hashCode();
    }

    @Override
    public TransactionManager getTransactionManager() throws Exception {
        return txm;
    }

    @Override
    public void setRollbackOnly(Throwable cause) throws Exception {
        txm.getTransaction().setRollbackOnly(cause);
    }
}
