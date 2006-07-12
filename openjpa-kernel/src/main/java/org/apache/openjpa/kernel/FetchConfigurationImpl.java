/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.rop.EagerResultList;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.rop.SimpleResultList;
import org.apache.openjpa.lib.rop.WindowResultList;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.NoTransactionException;

/**
 * <p>Allows configuration and optimization of how objects are loaded from
 * the data store.</p>
 *
 * @since 3.0
 * @author Abe White
 * @nojavadoc
 */
public class FetchConfigurationImpl
    implements FetchConfiguration, Cloneable {

    private static final Localizer _loc = Localizer.forPackage
        (FetchConfigurationImpl.class);

    // transient state
    private transient StoreContext _ctx = null;

    private int _fetchBatchSize = 0;
    private int _maxFetchDepth = 1;
    private boolean _queryCache = true;
    private int _flushQuery = 0;
    private int _lockTimeout = -1;
    private int _readLockLevel = LOCK_NONE;
    private int _writeLockLevel = LOCK_NONE;
    private Set _fetchGroups = null;
    private Set _fields = null;
    private Set _rootClasses;
    private Set _rootInstances;
    private Map _hints = null;

    private static final String[] EMPTY_STRINGS = new String[0];

    public StoreContext getContext() {
        return _ctx;
    }

    public void setContext(StoreContext ctx) {
        // can't reset non-null context to another context
        if (ctx != null && _ctx != null && ctx != _ctx)
            throw new InternalException();
        _ctx = ctx;
        if (ctx == null)
            return;

        OpenJPAConfiguration conf = ctx.getConfiguration();

        // initialize to conf info
        setFetchBatchSize(conf.getFetchBatchSize());
        setFlushBeforeQueries(conf.getFlushBeforeQueriesConstant());
        clearFetchGroups();
        addFetchGroups(Arrays.asList(conf.getFetchGroupsList()));
    }

    /**
     * Clone this instance.
     */
    public Object clone() {
        FetchConfigurationImpl clone = newInstance();
        clone._ctx = _ctx;
        clone.copy(this);
        return clone;
    }

    /**
     * Return a new hollow instance.  Subclasses should override to return
     * a new instance of their type, with cached permissions set appropriately.
     */
    protected FetchConfigurationImpl newInstance() {
        return new FetchConfigurationImpl();
    }

    public void copy(FetchConfiguration fetch) {
        setFetchBatchSize(fetch.getFetchBatchSize());
        setQueryCache(fetch.getQueryCache());
        setFlushBeforeQueries(fetch.getFlushBeforeQueries());
        setLockTimeout(fetch.getLockTimeout());
        clearFetchGroups();
        addFetchGroups(fetch.getFetchGroups());
        clearFields();
        addFields(fetch.getFields());

        // don't use setters because require active transaction
        _readLockLevel = fetch.getReadLockLevel();
        _writeLockLevel = fetch.getWriteLockLevel();
    }

    public int getFetchBatchSize() {
        return _fetchBatchSize;
    }

    public FetchConfiguration setFetchBatchSize(int fetchBatchSize) {
        if (fetchBatchSize == DEFAULT && _ctx != null)
            fetchBatchSize = _ctx.getConfiguration().getFetchBatchSize();
        if (fetchBatchSize != DEFAULT)
            _fetchBatchSize = fetchBatchSize;

        return this;
    }

    public int getMaxFetchDepth() {
        return _maxFetchDepth;
    }

    public FetchConfiguration setMaxFetchDepth(int depth) {
        _maxFetchDepth = depth;

        return this;
    }

    public boolean getQueryCache() {
        return _queryCache;
    }

    public FetchConfiguration setQueryCache(boolean cache) {
        _queryCache = cache;
        return this;
    }

    public int getFlushBeforeQueries() {
        return _flushQuery;
    }

    public FetchConfiguration setFlushBeforeQueries(int flush) {
        if (flush == DEFAULT && _ctx != null)
            _flushQuery = _ctx.getConfiguration().
                getFlushBeforeQueriesConstant();
        else if (flush != DEFAULT)
            _flushQuery = flush;
        return this;
    }

    public synchronized Set getFetchGroups() {
        return getImmutableSet(_fetchGroups);
    }

    public synchronized boolean hasFetchGroup(String group) {
        return _fetchGroups != null
            && ((group != null && _fetchGroups.contains(group))
            || _fetchGroups.contains(FETCH_GROUP_ALL));
    }

    public synchronized boolean hasFetchGroup(Set groups) {
        if (_fetchGroups != null && groups != null) {
            Iterator iter = groups.iterator();
            while (iter.hasNext()) {
                Object fg = iter.next();
                if (fg != null && hasFetchGroup(fg.toString()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Adds a fetch group of the given name to this receiver.
     * Checks if license allows for adding custom fetch groups. Makes
     * an exception if the given name matches with the default fetch group
     * name.
     *
     * @param name must not be null or empty.
     */
    public synchronized FetchConfiguration addFetchGroup(String name) {
        if (StringUtils.isEmpty(name))
            throw new MetaDataException(_loc.get("null-fg", name));

        if (_fetchGroups == null)
            _fetchGroups = new HashSet();
        _fetchGroups.add(name);
        return this;
    }

    public synchronized FetchConfiguration addFetchGroups(Collection groups) {
        if (groups == null || groups.isEmpty())
            return this;

        Iterator iter = groups.iterator();
        while (iter.hasNext()) {
            Object group = iter.next();
            if (group instanceof String)
                addFetchGroup((String) group);
        }
        return this;
    }

    public synchronized FetchConfiguration removeFetchGroup(String group) {
        if (_fetchGroups != null)
            _fetchGroups.remove(group);
        return this;
    }

    public synchronized FetchConfiguration removeFetchGroups(
        Collection groups) {
        if (_fetchGroups != null)
            _fetchGroups.removeAll(groups);
        return this;
    }

    public synchronized FetchConfiguration clearFetchGroups() {
        if (_fetchGroups != null)
            _fetchGroups.clear();
        return this;
    }

    public synchronized FetchConfiguration resetFetchGroups() {
        clearFetchGroups();
        if (_ctx != null)
            addFetchGroups(Arrays.asList(_ctx.getConfiguration().
                getFetchGroupsList()));
        return this;
    }

    public synchronized Set getFields() {
        return getImmutableSet(_fields);
    }

    public synchronized boolean hasField(String field) {
        return _fields != null && field != null && _fields.contains(field);
    }

    public synchronized FetchConfiguration addField(String field) {
        if (_fields == null)
            _fields = new HashSet();
        _fields.add(field);
        return this;
    }

    public synchronized FetchConfiguration addFields(Collection fields) {
        if (fields.isEmpty())
            return this;

        if (_fields == null)
            _fields = new HashSet();
        _fields.addAll(fields);
        return this;
    }

    public synchronized FetchConfiguration removeField(String field) {
        if (_fields != null)
            _fields.remove(field);
        return this;
    }

    public synchronized FetchConfiguration removeFields(Collection fields) {
        if (_fields != null)
            _fields.removeAll(fields);
        return this;
    }

    public synchronized FetchConfiguration clearFields() {
        if (_fields != null)
            _fields.clear();
        return this;
    }

    public int getLockTimeout() {
        return _lockTimeout;
    }

    public FetchConfiguration setLockTimeout(int timeout) {
        if (timeout == DEFAULT && _ctx != null)
            _lockTimeout = _ctx.getConfiguration().getLockTimeout();
        else if (timeout != DEFAULT)
            _lockTimeout = timeout;
        return this;
    }

    public int getReadLockLevel() {
        return _readLockLevel;
    }

    public FetchConfiguration setReadLockLevel(int level) {
        if (_ctx == null)
            return this;
        assertActiveTransaction();
        if (level == DEFAULT)
            _readLockLevel = _ctx.getConfiguration().
                getReadLockLevelConstant();
        else
            _readLockLevel = level;
        return this;
    }

    public int getWriteLockLevel() {
        return _writeLockLevel;
    }

    public FetchConfiguration setWriteLockLevel(int level) {
        if (_ctx == null)
            return this;
        assertActiveTransaction();
        if (level == DEFAULT)
            _writeLockLevel = _ctx.getConfiguration().
                getWriteLockLevelConstant();
        else
            _writeLockLevel = level;
        return this;
    }

    public ResultList newResultList(ResultObjectProvider rop) {
        if (rop instanceof ListResultObjectProvider)
            return new SimpleResultList(rop);
        if (_fetchBatchSize < 0)
            return new EagerResultList(rop);
        if (rop.supportsRandomAccess())
            return new SimpleResultList(rop);
        return new WindowResultList(rop);
    }

    public FetchState newFetchState() {
        return new FetchStateImpl(this);
    }

    /**
     * Throw an exception if no transaction is active.
     */
    private void assertActiveTransaction() {
        if (_ctx != null && !_ctx.isActive())
            throw new NoTransactionException(_loc.get("not-active"));
    }

    public String toString() {
        if ((_fetchGroups == null || _fetchGroups.isEmpty())
            && (_fields == null || _fields.isEmpty()))
            return "Default";

        StringBuffer buf = new StringBuffer();
        if (_fetchGroups != null && !_fetchGroups.isEmpty()) {
            for (Iterator itr = _fetchGroups.iterator(); itr.hasNext();) {
                if (buf.length() > 0)
                    buf.append(", ");
                buf.append(itr.next());
            }
        }
        if (_fields != null && !_fields.isEmpty()) {
            for (Iterator itr = _fields.iterator(); itr.hasNext();) {
                if (buf.length() > 0)
                    buf.append(", ");
                buf.append(itr.next());
            }
        }
        return buf.toString();
    }

    public synchronized void setHint(String name, Object value) {
        if (_hints == null)
            _hints = new HashMap();

        synchronized (_hints) {
            _hints.put(name, value);
        }
    }

    public Object getHint(String name) {
        if (_hints == null)
            return null;

        synchronized (_hints) {
            return _hints.get(name);
        }
    }

    public Set getRootClasses() {
        return getImmutableSet(_rootClasses);
    }

    public FetchConfiguration setRootClasses(Collection classes) {
        if (classes == null || classes.isEmpty())
            return this;

        if (_rootClasses == null)
            _rootClasses = new HashSet(classes.size());

        _rootClasses.addAll(classes);

        return this;
    }

    public Set getRootInstances() {
        return getImmutableSet(_rootInstances);
    }

    public FetchConfiguration setRootInstances(Collection roots) {
        if (roots == null)
            return this;
        if (_rootInstances == null)
            _rootInstances = new HashSet(roots.size());

        _rootInstances.addAll(roots);

        return this;
    }

    private Set getImmutableSet(Set input) {
        return (input == null) ? Collections.EMPTY_SET
            : Collections.unmodifiableSet(input);
	}
}
