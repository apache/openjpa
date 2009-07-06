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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.meta.ClassMetaData;

public class SimpleWriteBehindCache extends AbstractWriteBehindCache {

    private Map<WriteBehindCacheKey, StateManagerImpl> _cache = new HashMap<WriteBehindCacheKey, StateManagerImpl>();

    public List<Exception> add(Collection<StateManagerImpl> sms) {
        List<Exception> exceptions = new ArrayList<Exception>();
        for (StateManagerImpl sm : sms) {
            try {
                add(sm);
            } catch (WriteBehindException wbe) {
                exceptions.add(wbe);
            }
        }
        return exceptions;
    }

    protected void add(StateManagerImpl sm) {
        WriteBehindCacheKey key = getKey(sm);
        synchronized (_cache) {
            if (_cache.containsKey(key) && _cache.get(key) != sm) {
                _cache.put(key, merge(_cache.get(key), sm));
            } else { 
                _cache.put(key, sm);
            }
        }

    }

    public boolean contains(Object o) {
        boolean rval = false;
        StateManagerImpl sm = getStateManager(o);
        if(sm != null) {
            rval =  _cache.containsKey(getKey(sm));    
        }
        return rval;
    }

    public WriteBehindCacheKey getKey(StateManagerImpl sm) {
        ClassMetaData md = sm.getMetaData();
        Class<?> cls = md.getDescribedType();
        String className = cls.getCanonicalName();
        Object id = sm.getId();
        
        SimpleWriteBehindCacheKey key =
            new SimpleWriteBehindCacheKey(className, id);
        return key;
    }

    public int getSize() {
        return _cache.size();
    }

    public Collection<StateManagerImpl> getStateManagers() {
        return new ArrayList<StateManagerImpl>(_cache.values());
    }

    public void initialize(WriteBehindCacheManager manager) {
        // TODO Auto-generated method stub
    }

    protected StateManagerImpl getStateManager(Object o) {
        StateManagerImpl rval = null;
        if (o instanceof StateManagerImpl) {
            rval = (StateManagerImpl) o;
        } else if (o instanceof PersistenceCapable) {
            rval = (StateManagerImpl) ((PersistenceCapable) o).pcGetStateManager();
        }
        return rval;
    }

    public void clear() {
        synchronized(_cache) { 
            _cache.clear();
        }
    }

    public boolean isEmpty() {
        return _cache.isEmpty();
    }
    
    // Might be better in smImpl. 
    protected StateManagerImpl merge(StateManagerImpl from,
        StateManagerImpl into) {
        for (int i = 0; i < into.getMetaData().getFields().length; i++) {
            if (from.getDirty().get(i)) {
                into.dirty(i);
            }
        }
        return into;
    }

}
