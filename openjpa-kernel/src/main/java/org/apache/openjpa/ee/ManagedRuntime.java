/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.ee;

import javax.transaction.TransactionManager;

/**
 * This interface must be implemented by concrete plugins to application
 * servers in order to integrate the OpenJPA runtime in a managed environment.
 *
 * @author Abe White
 */
public interface ManagedRuntime {

    /**
     * Return the TransactionManager for the managed runtime. This
     * manager is used to register synchronization listeners, to
     * map transactional PersistenceManagers to the current transaction,
     * and possibly to enlist XA resources.
     */
    public TransactionManager getTransactionManager() throws Exception;
}
