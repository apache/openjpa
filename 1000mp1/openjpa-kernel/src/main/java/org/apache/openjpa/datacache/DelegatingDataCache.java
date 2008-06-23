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
package org.apache.openjpa.datacache;

import java.util.BitSet;
import java.util.Collection;

import org.apache.commons.lang.ObjectUtils;
import org.apache.openjpa.util.RuntimeExceptionTranslator;

/**
 * Delegating data cache that can also perform exception translation for
 * use in facades. This cache allows its delegate to be null, in which
 * case it returns default values for all operations.
 *
 * @author Abe White
 * @nojavadoc
 */
public class DelegatingDataCache
    implements DataCache {

    private static final BitSet EMPTY_BITSET = new BitSet(0);

    private final DataCache _cache;
    private final DelegatingDataCache _del;
    private final RuntimeExceptionTranslator _trans;

    /**
     * Constructor. Supply delegate.
     */
    public DelegatingDataCache(DataCache cache) {
        this(cache, null);
    }

    public DelegatingDataCache(DataCache cache,
        RuntimeExceptionTranslator trans) {
        _cache = cache;
        _trans = trans;
        if (cache instanceof DelegatingDataCache)
            _del = (DelegatingDataCache) _cache;
        else
            _del = null;
    }

    /**
     * Return the direct delegate.
     */
    public DataCache getDelegate() {
        return _cache;
    }

    /**
     * Return the native delegate.
     */
    public DataCache getInnermostDelegate() {
        return (_del == null) ? _cache : _del.getInnermostDelegate();
    }

    public int hashCode() {
        if (_cache == null)
            return super.hashCode();
        return getInnermostDelegate().hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingDataCache)
            other = ((DelegatingDataCache) other).getInnermostDelegate();
        return ObjectUtils.equals(getInnermostDelegate(), other);
    }

    /**
     * Translate the OpenJPA exception.
     */
    protected RuntimeException translate(RuntimeException re) {
        return (_trans == null) ? re : _trans.translate(re);
    }

    public String getName() {
        if (_cache == null)
            return null;
        try {
            return _cache.getName();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void setName(String name) {
        if (_cache == null)
            return;
        try {
            _cache.setName(name);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void initialize(DataCacheManager manager) {
        if (_cache == null)
            return;
        try {
            _cache.initialize(manager);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void commit(Collection additions, Collection newUpdates,
        Collection existingUpdates, Collection deletes) {
        if (_cache == null)
            return;
        try {
            _cache.commit(additions, newUpdates, existingUpdates, deletes);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public boolean contains(Object oid) {
        if (_cache == null)
            return false;
        try {
            return _cache.contains(oid);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public BitSet containsAll(Collection oids) {
        if (_cache == null)
            return EMPTY_BITSET;
        try {
            return _cache.containsAll(oids);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public DataCachePCData get(Object oid) {
        if (_cache == null)
            return null;
        try {
            return _cache.get(oid);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public DataCachePCData put(DataCachePCData value) {
        if (_cache == null)
            return null;
        try {
            return _cache.put(value);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void update(DataCachePCData value) {
        if (_cache == null)
            return;
        try {
            _cache.update(value);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public DataCachePCData remove(Object oid) {
        if (_cache == null)
            return null;
        try {
            return _cache.remove(oid);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public BitSet removeAll(Collection oids) {
        if (_cache == null)
            return EMPTY_BITSET;
        try {
            return _cache.removeAll(oids);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void removeAll(Class cls, boolean subclasses) {
        if (_cache == null)
            return;
        try {
            _cache.removeAll(cls, subclasses);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void clear() {
        if (_cache == null)
            return;
        try {
            _cache.clear();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public boolean pin(Object oid) {
        if (_cache == null)
            return false;
        try {
            return _cache.pin(oid);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public BitSet pinAll(Collection oids) {
        if (_cache == null)
            return EMPTY_BITSET;
        try {
            return _cache.pinAll(oids);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void pinAll(Class cls, boolean subs) {
        if (_cache == null)
            return;
        try {
            _cache.pinAll(cls, subs);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public boolean unpin(Object oid) {
        if (_cache == null)
            return false;
        try {
            return _cache.unpin(oid);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public BitSet unpinAll(Collection oids) {
        if (_cache == null)
            return EMPTY_BITSET;
        try {
            return _cache.unpinAll(oids);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void unpinAll(Class cls, boolean subs) {
        if (_cache == null)
            return;
        try {
            _cache.unpinAll(cls, subs);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void writeLock() {
        if (_cache == null)
            return;
        try {
            _cache.writeLock();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void writeUnlock() {
        if (_cache == null)
            return;
        try {
            _cache.writeUnlock();
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void addExpirationListener(ExpirationListener listen) {
        if (_cache == null)
            return;
        try {
            _cache.addExpirationListener(listen);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public boolean removeExpirationListener(ExpirationListener listen) {
        if (_cache == null)
            return false;
        try {
            return _cache.removeExpirationListener(listen);
        } catch (RuntimeException re) {
            throw translate(re);
        }
    }

    public void close() {
        if (_cache == null)
            return;
        try {
            _cache.close();
        } catch (RuntimeException re) {
            throw translate(re);
		}
	}
}
