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
package org.apache.openjpa.writebehind;

import java.util.Collection;

import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.kernel.StoreManager;

public class SimpleWriteBehindCallback extends AbstractWriteBehindCallback {

    private int _sleepTime = 2000;
    private boolean done = false;

    private WriteBehindCache _cache = null;
    private Broker _broker;

    public void initialize(Broker broker, WriteBehindCache cache) {
        _cache = cache;
        _broker = broker;
    }

    public Collection<Exception> flush() {
        Collection<Exception> errors = null;
        // skip past any delegating store managers.
        StoreManager storeManager =
            _broker.getStoreManager().getInnermostDelegate();
        Collection<StateManagerImpl> sms = null;
        if (_cache != null && !_cache.isEmpty()) {
            // TODO lock or switch the cache
            sms = _cache.getStateManagers();
            _cache.clear();
        }
        if (sms != null && !sms.isEmpty()) {
            storeManager.retainConnection();
            storeManager.begin();
            errors = storeManager.flush(sms);
            if(errors != null && !errors.isEmpty() ) {
                for(Exception e : errors) { 
                    e.printStackTrace();
                }
            }
            storeManager.commit();
            storeManager.releaseConnection();
        }
        return errors;
    }

    public void run() {
        while (!done) {
            try {
                Thread.sleep(_sleepTime);
                handleExceptions(flush());
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void handleExceptions(Collection<Exception> exceptions) {
        if (exceptions != null && !exceptions.isEmpty()) {
            done = true;
            for (Exception e : exceptions) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void close() {
        done = true;
        flush();
        _broker.close();
    }
}
