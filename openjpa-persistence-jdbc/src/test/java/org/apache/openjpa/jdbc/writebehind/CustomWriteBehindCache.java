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
package org.apache.openjpa.jdbc.writebehind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.writebehind.AbstractWriteBehindCache;
import org.apache.openjpa.writebehind.WriteBehindCacheKey;
import org.apache.openjpa.writebehind.WriteBehindCacheManager;

/**
 * Skeleton WriteBehindCache used to verify that the configuration options for WriteBehind are working properly. 
 */
public class CustomWriteBehindCache extends AbstractWriteBehindCache {

    public List<Exception> add(Collection<OpenJPAStateManager> sms) {
        List<Exception> exceptions = new ArrayList<Exception>(); 
        return exceptions;
    }

    public void add(StateManagerImpl sm) {
    }

    public boolean contains(Object o) {
        return false;
    }

    public void flush() {
    }

    public WriteBehindCacheKey getKey(OpenJPAStateManager sm) {
        return null;
    }

    public String getName() {
        return null;
    }

    public OpenJPAStateManager getSateManager(WriteBehindCacheKey key) {
        return null;
    }

    public int getSize() {
        return 0;
    }

    public Collection<OpenJPAStateManager> getStateManagers() {
        return null;
    }

    public void initialize(WriteBehindCacheManager manager) {
    }

    public void setName(String name) {
    }

    public void clear() {
    }

    public boolean isEmpty() {
        return false;
    }
}

