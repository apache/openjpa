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
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.rop.EagerResultList;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.rop.SimpleResultList;
import org.apache.openjpa.lib.rop.WindowResultList;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FetchGroup;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.NoTransactionException;
import org.apache.openjpa.util.UserException;

/**
 * Allows configuration and optimization of how objects are loaded from
 * the data store.
 *
 * @since 0.3.0
 * @author Abe White
 * @author Pinaki Poddar
 * @nojavadoc
 */
@SuppressWarnings("serial")
public class FetchConfigurationImpl
    implements FetchConfiguration, Cloneable {

    private static final Localizer _loc = Localizer.forPackage
        (FetchConfigurationImpl.class);

    /**
     * Configurable state shared throughout a traversal chain.
     */
    protected static class ConfigurationState
        implements Serializable
    {
        public transient StoreContext ctx = null;
        public int fetchBatchSize = 0;
        public int maxFetchDepth = 1;
        public boolean queryCache = true;
        public int flushQuery = 0;
        public int lockTimeout = -1;
        public int queryTimeout = -1;
        public int lockScope = LOCKSCOPE_NORMAL;
        public int readLockLevel = LOCK_NONE;
        public int writeLockLevel = LOCK_NONE;
        public Set<String> fetchGroups = null;
        public Set<String> fields = null;
        public Set<Class<?>> rootClasses;
        public Set<Object> rootInstances;
        public Map<String,Object> hints = null;
        public boolean fetchGroupContainsDefault = false;
        public boolean fetchGroupContainsAll = false;
        public boolean extendedPathLookup = false;
        public DataCacheRetrieveMode cacheRetrieveMode;
        public DataCacheStoreMode cacheStoreMode;
    }

    private final ConfigurationState _state;
    private FetchConfigurationImpl _parent;
    private String _fromField;
    private Class<?> _fromType;
    private String _directRelationOwner;
    private boolean _load = true;
    private int _availableRecursion;
    private int _availableDepth;
    private FetchConfigurationHintHandler _hintHandler;

    public FetchConfigurationImpl() {
        this(null);
    }

    protected FetchConfigurationImpl(ConfigurationState state) {
        _state = (state == null) ? new ConfigurationState() : state;
        _availableDepth = _state.maxFetchDepth;
        _hintHandler = new FetchConfigurationHintHandler(this);
    } 

    public StoreContext getContext() {
        return _state.ctx;
    }

    public void setContext(StoreContext ctx) {
        // can't reset non-null context to another context
        if (ctx != null && _state.ctx != null && ctx != _state.ctx)
            throw new InternalException();
        _state.ctx = ctx;
        if (ctx == null)
            return;

        // initialize to conf info
        OpenJPAConfiguration conf = ctx.getConfiguration();
        setFetchBatchSize(conf.getFetchBatchSize());
        setFlushBeforeQueries(conf.getFlushBeforeQueriesConstant());
        setLockTimeout(conf.getLockTimeout());
        setQueryTimeout(conf.getQueryTimeout());
        clearFetchGroups();
        addFetchGroups(Arrays.asList(conf.getFetchGroupsList()));
        setMaxFetchDepth(conf.getMaxFetchDepth());
    }

    /**
     * Clone this instance.
     */
    public Object clone() {
        FetchConfigurationImpl clone = newInstance(null);
        clone._state.ctx = _state.ctx;
        clone._parent = _parent;
        clone._fromField = _fromField;
        clone._fromType = _fromType;
        clone._directRelationOwner = _directRelationOwner;
        clone._load = _load;
        clone._availableRecursion = _availableRecursion;
        clone._availableDepth = _availableDepth;
        clone.copy(this);
        return clone;
    }

    /**
     * Return a new hollow instance.
     */
    protected FetchConfigurationImpl newInstance(ConfigurationState state) {
        return new FetchConfigurationImpl(state);
    }

    public void copy(FetchConfiguration fetch) {
        setFetchBatchSize(fetch.getFetchBatchSize());
        setMaxFetchDepth(fetch.getMaxFetchDepth());
        setQueryCacheEnabled(fetch.getQueryCacheEnabled());
        setFlushBeforeQueries(fetch.getFlushBeforeQueries());
        setExtendedPathLookup(fetch.getExtendedPathLookup());
        setLockTimeout(fetch.getLockTimeout());
        setQueryTimeout(fetch.getQueryTimeout());
        setLockScope(fetch.getLockScope());
        clearFetchGroups();
        addFetchGroups(fetch.getFetchGroups());
        clearFields();
        copyHints(fetch);
        addFields(fetch.getFields());

        // don't use setters because require active transaction
        _state.readLockLevel = fetch.getReadLockLevel();
        _state.writeLockLevel = fetch.getWriteLockLevel();
    }

    
    void copyHints(FetchConfiguration fetch) {
        if (fetch instanceof FetchConfigurationImpl == false)
            return;
        FetchConfigurationImpl from = (FetchConfigurationImpl)fetch;
        if (from._state == null || from._state.hints == null)
            return;
        if (this._state == null)
            return;
        if (this._state.hints == null)
            this._state.hints = new HashMap<String,Object>();
        this._state.hints.putAll(from._state.hints);
    }
    
    public int getFetchBatchSize() {
        return _state.fetchBatchSize;
    }

    public FetchConfiguration setFetchBatchSize(int fetchBatchSize) {
        if (fetchBatchSize == DEFAULT && _state.ctx != null)
            fetchBatchSize = _state.ctx.getConfiguration().getFetchBatchSize();
        if (fetchBatchSize != DEFAULT)
            _state.fetchBatchSize = fetchBatchSize;
        return this;
    }

    public int getMaxFetchDepth() {
        return _state.maxFetchDepth;
    }

    public FetchConfiguration setMaxFetchDepth(int depth) {
        if (depth == DEFAULT && _state.ctx != null)
            depth = _state.ctx.getConfiguration().getMaxFetchDepth();
        if (depth != DEFAULT)
        {
            _state.maxFetchDepth = depth;
            if (_parent == null)
                _availableDepth = depth;
        }
        return this;
    }

    public boolean getQueryCacheEnabled() {
        return _state.queryCache;
    }

    public FetchConfiguration setQueryCacheEnabled(boolean cache) {
        _state.queryCache = cache;
        return this;
    }

    public int getFlushBeforeQueries() {
        return _state.flushQuery;
    }
    
    public boolean getExtendedPathLookup() {
        return _state.extendedPathLookup;
    }
    
    public FetchConfiguration setExtendedPathLookup(boolean flag) {
        _state.extendedPathLookup = flag;
        return this;
    }

    public FetchConfiguration setFlushBeforeQueries(int flush) {
        if (flush != DEFAULT
            && flush != QueryFlushModes.FLUSH_TRUE
            && flush != QueryFlushModes.FLUSH_FALSE
            && flush != QueryFlushModes.FLUSH_WITH_CONNECTION)
            throw new IllegalArgumentException(_loc.get(
                "bad-flush-before-queries", new Integer(flush)).getMessage());

        if (flush == DEFAULT && _state.ctx != null)
            _state.flushQuery = _state.ctx.getConfiguration().
                getFlushBeforeQueriesConstant();
        else if (flush != DEFAULT)
            _state.flushQuery = flush;
        return this;
    }

    public Set<String> getFetchGroups() {
        if (_state.fetchGroups == null) return Collections.emptySet();
        return _state.fetchGroups;
    }

    public boolean hasFetchGroup(String group) {
        return _state.fetchGroups != null
            && (_state.fetchGroups.contains(group)
            || _state.fetchGroups.contains(FetchGroup.NAME_ALL));
    }

     public boolean hasFetchGroupDefault() {
         // Fetch group All includes fetch group Default by definition
         return _state.fetchGroupContainsDefault || 
             _state.fetchGroupContainsAll;
     }
     
     public boolean hasFetchGroupAll() {
         return _state.fetchGroupContainsAll;
     }
     
    public FetchConfiguration addFetchGroup(String name) {
        if (StringUtils.isEmpty(name))
            throw new UserException(_loc.get("null-fg"));

        lock();
        try {
            if (_state.fetchGroups == null)
                _state.fetchGroups = new HashSet<String>();
            _state.fetchGroups.add(name);
            if (FetchGroup.NAME_ALL.equals(name))
                _state.fetchGroupContainsAll = true;
            else if (FetchGroup.NAME_DEFAULT.equals(name))
                _state.fetchGroupContainsDefault = true;
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration addFetchGroups(Collection<String> groups) {
        if (groups == null || groups.isEmpty())
            return this;
        for (String group : groups)
            addFetchGroup(group);
        return this;
    }

    public FetchConfiguration removeFetchGroup(String group) {
        lock();
        try {
            if (_state.fetchGroups != null) {
                _state.fetchGroups.remove(group);
                if (FetchGroup.NAME_ALL.equals(group))
                    _state.fetchGroupContainsAll = false;
                else if (FetchGroup.NAME_DEFAULT.equals(group))
                    _state.fetchGroupContainsDefault = false;
            }
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration removeFetchGroups(Collection<String> groups) {
        lock();
        try {
            if (_state.fetchGroups != null && groups != null)
                for (String group : groups)
                    removeFetchGroup(group);
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration clearFetchGroups() {
        lock();
        try {
            if (_state.fetchGroups != null) {
                _state.fetchGroups.clear();
                _state.fetchGroupContainsAll = false;
                _state.fetchGroupContainsDefault = true;
            }
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration resetFetchGroups() {
        clearFetchGroups();
        if (_state.ctx != null)
            addFetchGroups(Arrays.asList(_state.ctx.getConfiguration().
                getFetchGroupsList()));
        return this;
    }

    public Set<String> getFields() {
        if (_state.fields == null) return Collections.emptySet();
        return _state.fields;
    }

    public boolean hasField(String field) {
        return _state.fields != null && _state.fields.contains(field);
    }

    public FetchConfiguration addField(String field) {
        if (StringUtils.isEmpty(field))
            throw new UserException(_loc.get("null-field"));

        lock();
        try {
            if (_state.fields == null)
                _state.fields = new HashSet<String>();
            _state.fields.add(field);
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration addFields(Collection<String> fields) {
        if (fields == null || fields.isEmpty())
            return this;

        lock();
        try {
            if (_state.fields == null)
                _state.fields = new HashSet<String>();
            _state.fields.addAll(fields);
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration removeField(String field) {
        lock();
        try {
            if (_state.fields != null)
                _state.fields.remove(field);
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration removeFields(Collection<String> fields) {
        lock();
        try {
            if (_state.fields != null)
                _state.fields.removeAll(fields);
        } finally {
            unlock();
        }
        return this;
    }

    public FetchConfiguration clearFields() {
        lock();
        try {
            if (_state.fields != null)
                _state.fields.clear();
        } finally {
            unlock();
        }
        return this;
    }

    public DataCacheRetrieveMode getCacheRetrieveMode() {
        return _state.cacheRetrieveMode;
    }

    public DataCacheStoreMode getCacheStoreMode() {
        return _state.cacheStoreMode;
    }

    public void setCacheRetrieveMode(DataCacheRetrieveMode mode) {
        _state.cacheRetrieveMode = mode;
    }

    public void setCacheStoreMode(DataCacheStoreMode mode) {
        _state.cacheStoreMode = mode;
    }

    public int getLockTimeout() {
        return _state.lockTimeout;
    }

    public FetchConfiguration setLockTimeout(int timeout) {
        if (timeout == DEFAULT && _state.ctx != null)
            _state.lockTimeout = _state.ctx.getConfiguration().getLockTimeout();
        else if (timeout != DEFAULT) {
            if (timeout < -1) {
                throw new IllegalArgumentException(_loc.get("invalid-timeout", 
                    timeout).getMessage());
            } else {
                _state.lockTimeout = timeout;
            }
        }
        return this;
    }
    
    public int getQueryTimeout() {
        return _state.queryTimeout;
    }

    public FetchConfiguration setQueryTimeout(int timeout) {
        if (timeout == DEFAULT && _state.ctx != null)
            _state.queryTimeout = _state.ctx.getConfiguration().
                getQueryTimeout();
        else if (timeout != DEFAULT) {
            if (timeout < -1) {
                throw new IllegalArgumentException(_loc.get("invalid-timeout", 
                    timeout).getMessage());
            } else {
                _state.queryTimeout = timeout;
            }
        }
        return this;
    }

    public int getLockScope() {
        return _state.lockScope;
    }

    public FetchConfiguration setLockScope(int scope) {
        if (scope != DEFAULT
                && scope != LockScopes.LOCKSCOPE_NORMAL
                && scope != LockScopes.LOCKSCOPE_EXTENDED)
                throw new IllegalArgumentException(_loc.get(
                    "bad-lock-scope", new Integer(scope)).getMessage());
        if (scope == DEFAULT )
            _state.lockScope = LOCKSCOPE_NORMAL;
        else
            _state.lockScope = scope;
        return this;
    }

    public int getReadLockLevel() {
        String lockModeKey = "openjpa.FetchPlan.ReadLockMode";
        String deferLockModeKey = lockModeKey + ".Defer";
        Integer value = (Integer)getHint(deferLockModeKey);
        if (value != null) {
            if (isActiveTransaction()) {
                removeHint(deferLockModeKey);
                setReadLockLevel(value);
            } else
                return value;
        }
        return _state.readLockLevel;
    }

    public FetchConfiguration setReadLockLevel(int level) {
        if (_state.ctx == null)
            return this;

        if (level != DEFAULT
            && level != MixedLockLevels.LOCK_NONE
            && level != MixedLockLevels.LOCK_READ
            && level != MixedLockLevels.LOCK_OPTIMISTIC
            && level != MixedLockLevels.LOCK_WRITE
            && level != MixedLockLevels.LOCK_OPTIMISTIC_FORCE_INCREMENT
            && level != MixedLockLevels.LOCK_PESSIMISTIC_READ
            && level != MixedLockLevels.LOCK_PESSIMISTIC_WRITE
            && level != MixedLockLevels.LOCK_PESSIMISTIC_FORCE_INCREMENT)
            throw new IllegalArgumentException(_loc.get(
                "bad-lock-level", new Integer(level)).getMessage());

        lock();
        try {
            if (level != MixedLockLevels.LOCK_NONE)
                assertActiveTransaction();
            if (level == DEFAULT)
                _state.readLockLevel = _state.ctx.getConfiguration().
                    getReadLockLevelConstant();
            else
                _state.readLockLevel = level;
        } finally {
            unlock();
        }
        return this;
    }

    public int getWriteLockLevel() {
        String lockModeKey = "openjpa.FetchPlan.WriteLockMode";
        String deferLockModeKey = lockModeKey + ".Defer";
        Integer value = (Integer)getHint(deferLockModeKey);
        if (value != null) {
            if (isActiveTransaction()) {
                removeHint(deferLockModeKey);
                setWriteLockLevel(value);
            } else
                return value;
        }
        return _state.writeLockLevel;
    }

    public FetchConfiguration setWriteLockLevel(int level) {
        if (_state.ctx == null)
            return this;

        if (level != DEFAULT
            && level != MixedLockLevels.LOCK_NONE
            && level != MixedLockLevels.LOCK_READ
            && level != MixedLockLevels.LOCK_OPTIMISTIC
            && level != MixedLockLevels.LOCK_WRITE
            && level != MixedLockLevels.LOCK_OPTIMISTIC_FORCE_INCREMENT
            && level != MixedLockLevels.LOCK_PESSIMISTIC_READ
            && level != MixedLockLevels.LOCK_PESSIMISTIC_WRITE
            && level != MixedLockLevels.LOCK_PESSIMISTIC_FORCE_INCREMENT)
            throw new IllegalArgumentException(_loc.get(
                "bad-lock-level", new Integer(level)).getMessage());

        lock();
        try {
            assertActiveTransaction();
            if (level == DEFAULT)
                _state.writeLockLevel = _state.ctx.getConfiguration().
                    getWriteLockLevelConstant();
            else
                _state.writeLockLevel = level;
        } finally {
            unlock();
        }
        return this;
    }

    public ResultList<?> newResultList(ResultObjectProvider rop) {
        if (rop instanceof ListResultObjectProvider)
            return new SimpleResultList(rop);
        if (_state.fetchBatchSize < 0)
            return new EagerResultList(rop);
        if (rop.supportsRandomAccess())
            return new SimpleResultList(rop);
        return new WindowResultList(rop);
    }

    /**
     * Throw an exception if no transaction is active.
     */
    private void assertActiveTransaction() {
        if (!isActiveTransaction())
            throw new NoTransactionException(_loc.get("not-active"));
    }
    
    private boolean isActiveTransaction() {
        return (_state.ctx != null && _state.ctx.isActive());
    }

    public void setHint(String name, Object value) {
        setHint(name, value, false);
    }

    public void setHint(String name, Object value, boolean validThrowException) {
        if (_hintHandler.setHint(name, value, validThrowException))
            addHint(name, value);
    }

    public void addHint(String name, Object value) {
        lock();
        try {
            if (_state.hints == null)
                _state.hints = new HashMap<String,Object>();
            _state.hints.put(name, value);
        } finally {
            unlock();
        }
    }

    public Object getHint(String name) {
        return (_state.hints == null) ? null : _state.hints.get(name);
    }

    public Object removeHint(String name) {
        return (_state.hints == null) ? null : _state.hints.remove(name);
    }
    
    public Map<String, Object> getHints() {
        if (_state.hints == null)
            return Collections.emptyMap();
        Map<String, Object> result = new TreeMap<String, Object>();
        for (Object key : _state.hints.keySet()) {
            result.put(key.toString(), _state.hints.get(key));
        }
        return result;
    }

    public Set<Class<?>> getRootClasses() {
        if (_state.rootClasses == null) return Collections.emptySet(); 
        return _state.rootClasses;
    }

    public FetchConfiguration setRootClasses(Collection<Class<?>> classes) {
        lock();
        try {
            if (_state.rootClasses != null)
                _state.rootClasses.clear();
            if (classes != null && !classes.isEmpty()) {
                if (_state.rootClasses == null)
                    _state.rootClasses = new HashSet<Class<?>>(classes);
                else 
                    _state.rootClasses.addAll(classes);
            }
        } finally {
            unlock();
        }
        return this;
    }

    public Set<Object> getRootInstances() {
        if (_state.rootInstances == null) return Collections.emptySet(); 
        return _state.rootInstances;
    }

    public FetchConfiguration setRootInstances(Collection<?> instances) {
        lock();
        try {
            if (_state.rootInstances != null)
                _state.rootInstances.clear();
            if (instances != null && !instances.isEmpty()) {
                if (_state.rootInstances == null) {
                    _state.rootInstances = new HashSet<Object>(instances);
                } else { 
                    _state.rootInstances.addAll(instances);
                }
            }
        } finally {
            unlock();
        }
        return this;
    }

    public void lock() {
        if (_state.ctx != null)
            _state.ctx.lock();
    }

    public void unlock() {
        if (_state.ctx != null)
            _state.ctx.unlock();
    }

    /////////////
    // Traversal
    /////////////
    
    public int requiresFetch(FieldMetaData fm) {
        if (!includes(fm))
            return FETCH_NONE;
        
        Class<?> type = getRelationType(fm);
        if (type == null)
            return FETCH_LOAD;
        if (_availableDepth == 0)
            return FETCH_NONE;

        // we can skip calculating recursion depth if this is a top-level conf:
        // the field is in our fetch groups, so can't possibly not select
        if (_parent == null) 
            return FETCH_LOAD;
        
        String fieldName = fm.getFullName(false);
        int rdepth = getAvailableRecursionDepth(fm, type, fieldName, false);
        if (rdepth != FetchGroup.DEPTH_INFINITE && rdepth <= 0)
            return FETCH_NONE;

        if (StringUtils.equals(_directRelationOwner, fieldName))
            return FETCH_REF;
        return FETCH_LOAD;
    }

    public boolean requiresLoad() {
        return _load;
    }

    public FetchConfiguration traverse(FieldMetaData fm) {
        Class<?> type = getRelationType(fm);
        if (type == null)
            return this;

        FetchConfigurationImpl clone = newInstance(_state);
        clone._parent = this;
        clone._availableDepth = reduce(_availableDepth);
        clone._fromField = fm.getFullName(false);
        clone._fromType = type;
        clone._availableRecursion = getAvailableRecursionDepth(fm, type, fm.getFullName(false), true);
        if (StringUtils.equals(_directRelationOwner, fm.getFullName(false)))
            clone._load = false;
        else
            clone._load = _load;

        FieldMetaData owner = fm.getMappedByMetaData();
        if (owner != null && owner.getTypeCode() == JavaTypes.PC)
            clone._directRelationOwner = owner.getFullName(false);
        
        return clone;
    }

    /**
     * Whether our configuration state includes the given field.
     */
    private boolean includes(FieldMetaData fmd) {
        if ((hasFetchGroupDefault() && fmd.isInDefaultFetchGroup()) 
        || hasFetchGroupAll()
        || hasField(fmd.getFullName(false))
        || hasExtendedLookupPath(fmd))
            return true;
        String[] fgs = fmd.getCustomFetchGroups();
        for (int i = 0; i < fgs.length; i++)
            if (hasFetchGroup(fgs[i]))
                return true;
        return false; 
    }
    
    private boolean hasExtendedLookupPath(FieldMetaData fmd) {
        return getExtendedPathLookup()
            && (hasField(fmd.getRealName())
                || (_fromField != null 
                && hasField(_fromField + "." + fmd.getName())));
    }

    /**
     * Return the available recursion depth via the given field for the
     * given type.
     *
     * @param traverse whether we're traversing the field
     */
    private int getAvailableRecursionDepth(FieldMetaData fm, Class<?> type, String fromField,
        boolean traverse) {
        // see if there's a previous limit
        int avail = Integer.MIN_VALUE;
        for (FetchConfigurationImpl f = this; f != null; f = f._parent) {
            if (StringUtils.equals(f._fromField, fromField) 
                && ImplHelper.isAssignable(f._fromType, type)) {
                avail = f._availableRecursion;
                if (traverse)
                    avail = reduce(avail);
                break;
            }
        }
        if (avail == 0)
            return 0;
        
        // calculate fetch groups max
        ClassMetaData meta = fm.getDefiningMetaData();
        int max = Integer.MIN_VALUE;
        if (fm.isInDefaultFetchGroup())
            max = meta.getFetchGroup(FetchGroup.NAME_DEFAULT).
                getRecursionDepth(fm);
        String[] groups = fm.getCustomFetchGroups();
        int cur;
        for (int i = 0; max != FetchGroup.DEPTH_INFINITE 
            && i < groups.length; i++) {
            // ignore custom groups that are inactive in this configuration
            if (!this.hasFetchGroup(groups[i])) continue;
            cur = meta.getFetchGroup(groups[i]).getRecursionDepth(fm);
            if (cur == FetchGroup.DEPTH_INFINITE || cur > max) 
                max = cur;
        }
        // reduce max if we're traversing a self-type relation
        if (traverse && max != Integer.MIN_VALUE
            && ImplHelper.isAssignable(meta.getDescribedType(), type))
            max = reduce(max);

        // take min/defined of previous avail and fetch group max
        if (avail == Integer.MIN_VALUE && max == Integer.MIN_VALUE) {
            int def = FetchGroup.RECURSION_DEPTH_DEFAULT;
            return (traverse && ImplHelper.isAssignable(
                    meta.getDescribedType(), type)) ? def - 1 : def;
        }
        if (avail == Integer.MIN_VALUE || avail == FetchGroup.DEPTH_INFINITE)
            return max;
        if (max == Integer.MIN_VALUE || max == FetchGroup.DEPTH_INFINITE)
            return avail;
        return Math.min(max, avail);
    }

    /**
     * Return the relation type of the given field.
     */
    private static Class<?> getRelationType(FieldMetaData fm) {
        if (fm.isDeclaredTypePC())
            return fm.getDeclaredType();
        if (fm.getElement().isDeclaredTypePC())
            return fm.getElement().getDeclaredType();
        if (fm.getKey().isDeclaredTypePC())
            return fm.getKey().getDeclaredType();
        return null;
    }

    /**
     * Reduce the given logical depth by 1.
     */
    private static int reduce(int d) {
        if (d == 0)
            return 0;
        if (d != FetchGroup.DEPTH_INFINITE)
            d--;
        return d;
    }

    /////////////////
    // Debug methods
    /////////////////

    FetchConfiguration getParent() {
        return _parent;
    }
    
    boolean isRoot() {
        return _parent == null;
    }
    
    FetchConfiguration getRoot() {
        return (isRoot()) ? this : _parent.getRoot();
    }

    int getAvailableFetchDepth() {
        return _availableDepth;
    }

    int getAvailableRecursionDepth() {
        return _availableRecursion;
    }

    String getTraversedFromField() {
        return _fromField;
    }

    Class<?> getTraversedFromType() {
        return _fromType;
    }

    List<FetchConfigurationImpl> getPath() {
        if (isRoot())
            return Collections.emptyList();
        return trackPath(new ArrayList<FetchConfigurationImpl>());
    }
    
    List<FetchConfigurationImpl> trackPath(List<FetchConfigurationImpl> path) {
        if (_parent != null)
            _parent.trackPath(path);
        path.add(this);
        return path;
    }
       
    public String toString() {
        return "FetchConfiguration@" + System.identityHashCode(this) 
            + " (" + _availableDepth + ")" + getPathString();
    }
    
    private String getPathString() {
        List<FetchConfigurationImpl> path = getPath();
        if (path.isEmpty())
            return "";
        StringBuffer buf = new StringBuffer().append (": ");
        for (Iterator<FetchConfigurationImpl> itr = path.iterator(); itr.hasNext();) {
            buf.append(itr.next().getTraversedFromField());
            if (itr.hasNext())
                buf.append("->");
        }
        return buf.toString();
    }

}
