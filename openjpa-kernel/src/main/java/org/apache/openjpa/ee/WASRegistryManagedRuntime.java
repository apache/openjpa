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

import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

/**
 * WASRegistryManagedRuntime provides WebSphere specific extensions to
 * {@link RegistryManagedRuntime}. Currently, these extensions consist of using
 * the WebSphere UOWManager interface to submit non-transactional work.
 */
public class WASRegistryManagedRuntime extends RegistryManagedRuntime {

    // value taken from com.ibm.websphere.uow.UOWSynchronizationRegistry
    private static final int WEBSPHERE_UOW_TYPE_LOCAL_TRANSACTION = 0;


    public WASRegistryManagedRuntime() {
    }

    /**
     * <P>
     * RegistryManagedRuntime cannot suspend transactions, but WebSphere
     * provides an interface to submit work outside of the current tran.
     * </P>
     */
    @Override
    public void doNonTransactionalWork(Runnable runnable) throws RuntimeException {
        try {
            UOWManager uowManager = UOWManagerFactory.getUOWManager();
            uowManager.runUnderUOW(WEBSPHERE_UOW_TYPE_LOCAL_TRANSACTION, false, new DelegatingUOWAction(runnable));
        }
        catch(Exception e ) {
            RuntimeException re = new RuntimeException(e.getMessage(), e);
            throw re;
        }
    }


    /**
     * Delegate for the WebSphere proprietary UOWAction interface. Enables a
     * {@link Runnable} to be passed in to the WebSphere UOWManager.
     */
    class DelegatingUOWAction implements UOWAction {
        Runnable _del;

        public DelegatingUOWAction(Runnable delegate) {
            _del = delegate;
        }

        @Override
        public void run() throws Exception {
            _del.run();
        }
    }
}
