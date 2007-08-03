/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openjpa.ee;

import javax.transaction.SystemException;

/*
 * AbstractManagedRuntime.java
 *
 * Created on August 2, 2007, 2:38 PM
 *
 */
public abstract class AbstractManagedRuntime implements ManagedRuntime {
    
    /**
     * Returns a transaction key that can be used to associate transactions
     * and Brokers.
     * The default implementation returns the Transaction associated 
     * with the current thread's transaction.
     * @return the transaction key
     */
    public Object getTransactionKey() throws Exception, SystemException {
        return getTransactionManager().getTransaction();
    }

}
