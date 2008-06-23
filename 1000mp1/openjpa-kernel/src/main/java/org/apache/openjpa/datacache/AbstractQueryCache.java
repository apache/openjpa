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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.event.RemoteCommitEvent;
import org.apache.openjpa.event.RemoteCommitListener;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.concurrent.AbstractConcurrentEventManager;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashSet;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.Id;

/**
 * Abstract {@link QueryCache} implementation that provides various
 * statistics, logging, and timeout functionality common across cache
 * implementations.
 *
 * @author Patrick Linskey
 * @author Abe White
 */
public abstract class AbstractQueryCache
    extends AbstractConcurrentEventManager 
    implements QueryCache, Configurable {

    private static final Localizer s_loc =
        Localizer.forPackage(AbstractQueryCache.class);

    /**
     * The configuration set by the system.
     */
    protected OpenJPAConfiguration conf;

    /**
     * The log to use.
     */
    protected Log log;

    private boolean _closed = false;

    public void initialize(DataCacheManager manager) {
    }

    public void onTypesChanged(TypesChangedEvent ev) {
        writeLock();
        Collection keys = null;
        try {
            if (hasListeners())
                fireEvent(ev);
            keys = keySet();
        } finally {
            writeUnlock();
        }

        QueryKey qk;
        List removes = null;
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            qk = (QueryKey) iter.next();
            if (qk.changeInvalidatesQuery(ev.getTypes())) {
                if (removes == null)
                    removes = new ArrayList();
                removes.add(qk);
            }
        }
        if (removes != null)
            removeAllInternal(removes);
    }

    public QueryResult get(QueryKey key) {
        QueryResult o = getInternal(key);
        if (o != null && o.isTimedOut()) {
            o = null;
            removeInternal(key);
            if (log.isTraceEnabled())
                log.trace(s_loc.get("cache-timeout", key));
        }

        if (log.isTraceEnabled()) {
            if (o == null)
                log.trace(s_loc.get("cache-miss", key));
            else
                log.trace(s_loc.get("cache-hit", key));
        }
        return o;
    }

    public QueryResult put(QueryKey qk, QueryResult oids) {
        QueryResult o = putInternal(qk, oids);
        if (log.isTraceEnabled())
            log.trace(s_loc.get("cache-put", qk));
        return (o == null || o.isTimedOut()) ? null : o;
    }

    public QueryResult remove(QueryKey key) {
        QueryResult o = removeInternal(key);
        if (o != null && o.isTimedOut())
            o = null;
        if (log.isTraceEnabled()) {
            if (o == null)
                log.trace(s_loc.get("cache-remove-miss", key));
            else
                log.trace(s_loc.get("cache-remove-hit", key));
        }
        return o;
    }

    public boolean pin(QueryKey key) {
        boolean bool = pinInternal(key);
        if (log.isTraceEnabled()) {
            if (bool)
                log.trace(s_loc.get("cache-pin-hit", key));
            else
                log.trace(s_loc.get("cache-pin-miss", key));
        }
        return bool;
    }

    public boolean unpin(QueryKey key) {
        boolean bool = unpinInternal(key);
        if (log.isTraceEnabled()) {
            if (bool)
                log.trace(s_loc.get("cache-unpin-hit", key));
            else
                log.trace(s_loc.get("cache-unpin-miss", key));
        }
        return bool;
    }

    public void clear() {
        clearInternal();
        if (log.isTraceEnabled())
            log.trace(s_loc.get("cache-clear", "<query-cache>"));
    }

    public void close() {
        close(true);
    }

    protected void close(boolean clear) {
        if (!_closed) {
            if (clear)
                clearInternal();
            _closed = true;
        }
    }

    public boolean isClosed() {
        return _closed;
    }

    public void addTypesChangedListener(TypesChangedListener listen) {
        addListener(listen);
    }

    public boolean removeTypesChangedListener(TypesChangedListener listen) {
        return removeListener(listen);
    }

    /**
     * This method is part of the {@link RemoteCommitListener} interface. If
     * your cache subclass relies on OpenJPA for clustering support, make it
     * implement <code>RemoteCommitListener</code>. This method will take
     * care of invalidating entries from remote commits, by delegating to
     * {@link #onTypesChanged}.
     */
    public void afterCommit(RemoteCommitEvent event) {
        if (_closed)
            return;

        // drop all committed classes
        Set classes = Caches.addTypesByName(conf,
            event.getPersistedTypeNames(), null);
        if (event.getPayloadType() == RemoteCommitEvent.PAYLOAD_EXTENTS) {
            classes = Caches.addTypesByName(conf, event.getUpdatedTypeNames(),
                classes);
            classes = Caches.addTypesByName(conf, event.getDeletedTypeNames(),
                classes);
        } else {
            classes = addTypes(event.getUpdatedObjectIds(), classes);
            classes = addTypes(event.getDeletedObjectIds(), classes);
        }
        if (classes != null)
            onTypesChanged(new TypesChangedEvent(this, classes));
    }

    /**
     * Build up a set of classes for the given oids.
     */
    private Set addTypes(Collection oids, Set classes) {
        if (oids.isEmpty())
            return classes;
        if (classes == null)
            classes = new HashSet();

        MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
        ClassMetaData meta;
        Object oid;
        for (Iterator itr = oids.iterator(); itr.hasNext();) {
            oid = itr.next();
            if (oid instanceof Id)
                classes.add(((Id) oid).getType());
            else {
                // ok if no metadata for oid; that just means the pc type
                // probably hasn't been loaded into this JVM yet, and therefore
                // there's no chance that it's in the cache anyway
                meta = repos.getMetaData(oid, null, false);
                if (meta != null)
                    classes.add(meta.getDescribedType());
            }
        }
        return classes;
    }

    /**
     * Return a threadsafe view of the keys in this cache. This collection
     * must be iterable without risk of concurrent modification exceptions.
     * It does not have to implement contains() efficiently or use set
     * semantics.
     */
    protected abstract Collection keySet();

    /**
     * Return the list for the given key.
     */
    protected abstract QueryResult getInternal(QueryKey qk);

    /**
     * Add the given result to the cache, returning the old result under the
     * given key.
     */
    protected abstract QueryResult putInternal(QueryKey qk, QueryResult oids);

    /**
     * Remove the result under the given key from the cache.
     */
    protected abstract QueryResult removeInternal(QueryKey qk);

    /**
     * Remove all results under the given keys from the cache.
     */
    protected void removeAllInternal(Collection qks) {
        for (Iterator iter = qks.iterator(); iter.hasNext();)
            removeInternal((QueryKey) iter.next());
    }

    /**
     * Clear the cache.
     */
    protected abstract void clearInternal();

    /**
     * Pin an object to the cache.
     */
    protected abstract boolean pinInternal(QueryKey qk);

    /**
     * Unpin an object from the cache.
     */
    protected abstract boolean unpinInternal(QueryKey qk);

    // ---------- Configurable implementation ----------

    public void setConfiguration(Configuration conf) {
        this.conf = (OpenJPAConfiguration) conf;
        this.log = conf.getLog(OpenJPAConfiguration.LOG_DATACACHE);
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }

    // ---------- AbstractEventManager implementation ----------

    protected void fireEvent(Object event, Object listener) {
        TypesChangedListener listen = (TypesChangedListener) listener;
        TypesChangedEvent ev = (TypesChangedEvent) event;
        try {
            listen.onTypesChanged(ev);
        } catch (Exception e) {
            if (log.isWarnEnabled())
                log.warn(s_loc.get("exp-listener-ex"), e);
        }
    }

    /**
     * Individual query results will be registered as types changed
     * listeners. We want such query results to be gc'd once
     * the only reference is held by the list of expiration listeners.
     */
    protected Collection newListenerCollection() {
        return new ConcurrentReferenceHashSet (ConcurrentReferenceHashSet.WEAK);
	}
}
