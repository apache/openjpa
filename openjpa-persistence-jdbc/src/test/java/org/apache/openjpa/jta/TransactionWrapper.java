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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;

import jakarta.transaction.TransactionManager;

import org.apache.openjpa.ee.ManagedRuntime;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Simulates a container transaction around a test method.
 * <br>
 * The concrete tests derived from this class must adhere to the following guidelines:
 * <LI>They must configure openjpa.ManagedRuntime in setUp() properties as
 * to <code>org.apache.openjpa.jta.JTAManagedRuntime</code>
 *
 * <LI>The tests themselves must not try to access the transaction directly
 * i.e. not call EntityManager.getTransaction(). Attempt to do so will raise
 * exception.
 *
 * <LI>The test methods must use  EntityManager.joinTransaction() to use a transaction.
 * This test harness ensures that a transaction is active before running each
 * test method.
 *
 * <LI>The test methods may call commit() and rollback() methods of this class
 * to demarcate transaction.
 *
 * @author Pinaki Poddar
 *
 */
public class TransactionWrapper implements InvocationInterceptor {
    ManagedRuntime runtime;

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable
    {
        SingleEMFTestCase test = (SingleEMFTestCase)extensionContext.getRequiredTestInstance();
        OpenJPAEntityManagerFactorySPI emf = test.getEmf();
        runtime = emf.getConfiguration().getManagedRuntimeInstance();
        assertNotNull(runtime);

        TransactionManager txm = runtime.getTransactionManager();
        txm.begin();
        try {
            invocation.proceed();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (emf != null && emf.isOpen()) {
                // can't use AbstractPersistenceTestCase.closeEMF() due to using managed transactions
                // closeAllOpenEMs(emf);
                emf.close();
            }
        }
    }

    public void commit() {
        try {
            runtime.getTransactionManager().commit();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void rollback() {
        try {
            runtime.getTransactionManager().rollback();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
