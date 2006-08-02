/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.kernel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Discriminator;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.jdbc.sql.SQLFactory;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.jdbc.sql.Union;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.lib.jdbc.DelegatingConnection;
import org.apache.openjpa.lib.jdbc.DelegatingPreparedStatement;
import org.apache.openjpa.lib.jdbc.DelegatingStatement;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.Id;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.OpenJPAId;
import org.apache.openjpa.util.StoreException;
import org.apache.openjpa.util.UserException;

/**
 * StoreManager plugin that uses JDBC to store persistent data in a
 * relational data store.
 *
 * @author Abe White
 * @nojavadoc
 */
public class JDBCStoreManager 
    implements StoreManager, JDBCStore {

    private static final Localizer _loc = Localizer
        .forPackage(JDBCStoreManager.class);

    private StoreContext _ctx = null;
    private JDBCConfiguration _conf = null;
    private DBDictionary _dict = null;
    private SQLFactory _sql = null;
    private JDBCLockManager _lm = null;
    private DataSource _ds = null;
    private RefCountConnection _conn = null;
    private boolean _active = false;

    // track the pending statements so we can cancel them
    private Set _stmnts = Collections.synchronizedSet(new HashSet());

    public StoreContext getContext() {
        return _ctx;
    }

    public void setContext(StoreContext ctx) {
        _ctx = ctx;
        _conf = (JDBCConfiguration) ctx.getConfiguration();
        _dict = _conf.getDBDictionaryInstance();
        _sql = _conf.getSQLFactoryInstance();

        LockManager lm = ctx.getLockManager();
        if (lm instanceof JDBCLockManager)
            _lm = (JDBCLockManager) lm;

        if (!ctx.isManaged() && _conf.isConnectionFactoryModeManaged())
            _ds = _conf.getDataSource2(ctx);
        else
            _ds = _conf.getDataSource(ctx);

        if (_conf.getUpdateManagerInstance().orderDirty())
            ctx.setOrderDirtyObjects(true);
    }

    public JDBCConfiguration getConfiguration() {
        return _conf;
    }

    public DBDictionary getDBDictionary() {
        return _dict;
    }

    public SQLFactory getSQLFactory() {
        return _sql;
    }

    public JDBCLockManager getLockManager() {
        return _lm;
    }

    public JDBCFetchConfiguration getFetchConfiguration() {
        return (JDBCFetchConfiguration) _ctx.getFetchConfiguration();
    }

    public void beginOptimistic() {
    }

    public void rollbackOptimistic() {
    }

    public void begin() {
        _active = true;
        try {
            if ((!_ctx.isManaged() || !_conf.isConnectionFactoryModeManaged())
                && _conn.getAutoCommit())
                _conn.setAutoCommit(false);
        } catch (SQLException se) {
            _active = false;
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    public void commit() {
        try {
            if (!_ctx.isManaged() || !_conf.isConnectionFactoryModeManaged())
                _conn.commit();
        } catch (SQLException se) {
            try {
                _conn.rollback();
            } catch (SQLException se2) {
            }
            throw SQLExceptions.getStore(se, _dict);
        } finally {
            _active = false;
        }
    }

    public void rollback() {
        // already rolled back ourselves?
        if (!_active)
            return;

        try {
            if (_conn != null
                && (!_ctx.isManaged() || !_conf
                    .isConnectionFactoryModeManaged()))
                _conn.rollback();
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        } finally {
            _active = false;
        }
    }

    public void retainConnection() {
        connect(false);
        _conn.setRetain(true);
    }

    public void releaseConnection() {
        if (_conn != null)
            _conn.setRetain(false);
    }

    public Object getClientConnection() {
        return new ClientConnection(getConnection());
    }

    public Connection getConnection() {
        connect(true);
        return _conn;
    }

    public boolean exists(OpenJPAStateManager sm, Object context) {
        // add where conditions on base class to avoid joins if subclass
        // doesn't use oid as identifier
        ClassMapping mapping = (ClassMapping) sm.getMetaData();
        return exists(mapping, sm.getObjectId(), context);
    }

    private boolean exists(ClassMapping mapping, Object oid, Object context) {
        // add where conditions on base class to avoid joins if subclass
        // doesn't use oid as identifier
        Select sel = _sql.newSelect();
        while (mapping.getJoinablePCSuperclassMapping() != null)
            mapping = mapping.getJoinablePCSuperclassMapping();

        sel.wherePrimaryKey(oid, mapping, this);
        try {
            return sel.getCount(this) != 0;
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    public boolean syncVersion(OpenJPAStateManager sm, Object context) {
        ClassMapping mapping = (ClassMapping) sm.getMetaData();
        try {
            return mapping.getVersion().checkVersion(sm, this, true);
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    public int compareVersion(OpenJPAStateManager state, Object v1, Object v2) {
        ClassMapping mapping = (ClassMapping) state.getMetaData();
        return mapping.getVersion().compareVersion(v1, v2);
    }

    public boolean initialize(OpenJPAStateManager sm, PCState state,
        FetchConfiguration fetch, Object context) {
        ConnectionInfo info = (ConnectionInfo) context;
        try {
            return initializeState(sm, state, (JDBCFetchConfiguration)fetch, 
                info);
        } catch (ClassNotFoundException cnfe) {
            throw new UserException(cnfe);
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    /**
     * Initialize a newly-loaded instance.
     */
    private boolean initializeState(OpenJPAStateManager sm, PCState state,
        JDBCFetchConfiguration fetch, ConnectionInfo info)
        throws ClassNotFoundException, SQLException {
        Object oid = sm.getObjectId();
        ClassMapping mapping = (ClassMapping) sm.getMetaData();
        Result res = null;
        if (info != null && info.result != null) {
            res = info.result;
            info.sm = sm;
            if (info.mapping == null)
                info.mapping = mapping;
            mapping = info.mapping;
        } else if (oid instanceof OpenJPAId
            && !((OpenJPAId) oid).hasSubclasses()) {
            Boolean custom = customLoad(sm, mapping, state, fetch);
            if (custom != null)
                return custom.booleanValue();
            res = getInitializeStateResult(sm, mapping, fetch,
                Select.SUBS_EXACT);
            if (res == null && !selectPrimaryKey(sm, mapping, fetch))
                return false;
            if (res != null && !res.next())
                return false;
        } else {
            ClassMapping[] mappings = mapping.
                getIndependentAssignableMappings();
            if (mappings.length == 1) {
                mapping = mappings[0];
                Boolean custom = customLoad(sm, mapping, state, fetch);
                if (custom != null)
                    return custom.booleanValue();
                res = getInitializeStateResult(sm, mapping, fetch,
                    Select.SUBS_ANY_JOINABLE);
                if (res == null && !selectPrimaryKey(sm, mapping, fetch))
                    return false;
            } else
                res = getInitializeStateUnionResult(sm, mapping, mappings,
                    fetch);
            if (res != null && !res.next())
                return false;
        }

        try {
            // figure out what type of object this is; the state manager
            // only guarantees to provide a base class
            Class type;
            if (res == null)
                type = mapping.getDescribedType();
            else {
                if (res.getBaseMapping() != null)
                    mapping = res.getBaseMapping();
                res.startDataRequest(mapping.getDiscriminator());
                try {
                    type = mapping.getDiscriminator().getClass(this, mapping,
                        res);
                } finally {
                    res.endDataRequest();
                }
            }

            // initialize the state manager; this may change the mapping
            // and the object id instance if the type as determined
            // from the indicator is a subclass of expected type
            sm.initialize(type, state);

            // load the selected mappings into the given state manager
            if (res != null) {
                // re-get the mapping in case the instance was a subclass
                mapping = (ClassMapping) sm.getMetaData();
                load(mapping, sm, fetch, res);
                mapping.getVersion().afterLoad(sm, this);
            }
            return true;
        } finally {
            if (res != null && (info == null || res != info.result))
                res.close();
        }
    }

    /**
     * Allow the mapping to custom load data. Return null if the mapping
     * does not use custom loading.
     */
    private Boolean customLoad(OpenJPAStateManager sm, ClassMapping mapping,
        PCState state, JDBCFetchConfiguration fetch)
        throws ClassNotFoundException, SQLException {
        // check to see if the mapping takes care of initialization
        if (!mapping.customLoad(sm, this, state, fetch))
            return null;
        if (sm.getManagedInstance() != null) {
            mapping.getVersion().afterLoad(sm, this);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Select the data for the given instance and return the result. Return
     * null if there is no data in the current fetch groups to select.
     */
    private Result getInitializeStateResult(OpenJPAStateManager sm,
        ClassMapping mapping, JDBCFetchConfiguration fetch, int subs)
        throws SQLException {
        Select sel = _sql.newSelect();
        if (!select(sel, mapping, subs, sm, null, fetch,
            JDBCFetchConfiguration.EAGER_JOIN, true))
            return null;

        sel.wherePrimaryKey(sm.getObjectId(), mapping, this);
        return sel.execute(this, fetch);
    }

    /**
     * Select a union of the data for the given instance from possible concrete
     * mappings and return the result.
     */
    private Result getInitializeStateUnionResult(final OpenJPAStateManager sm,
        ClassMapping mapping, final ClassMapping[] mappings,
        final JDBCFetchConfiguration fetch) throws SQLException {
        final JDBCStoreManager store = this;
        final int eager = Math.min(fetch.getEagerFetchMode(),
            JDBCFetchConfiguration.EAGER_JOIN);

        Union union = _sql.newUnion(mappings.length);
        union.setSingleResult(true);
        if (fetch.getSubclassFetchMode(mapping) != fetch.EAGER_JOIN)
            union.abortUnion();
        union.select(new Union.Selector() {
            public void select(Select sel, int i) {
                sel.select(mappings[i], Select.SUBS_ANY_JOINABLE, store, fetch,
                    eager);
                sel.wherePrimaryKey(sm.getObjectId(), mappings[i], store);
            }
        });
        return union.execute(this, fetch);
    }

    /**
     * Select primary key data to make sure the given instance exists, locking
     * if needed.
     */
    private boolean selectPrimaryKey(OpenJPAStateManager sm,
        ClassMapping mapping, JDBCFetchConfiguration fetch)
        throws SQLException {
        // select pks from base class record to ensure it exists and lock
        // it if needed
        ClassMapping base = mapping;
        while (base.getJoinablePCSuperclassMapping() != null)
            base = base.getJoinablePCSuperclassMapping();

        Select sel = _sql.newSelect();
        sel.select(base.getPrimaryKeyColumns());
        sel.wherePrimaryKey(sm.getObjectId(), base, this);
        Result exists = sel.execute(this, fetch);
        try {
            if (!exists.next())
                return false;

            // record locked?
            if (_active && _lm != null && exists.isLocking())
                _lm.loadedForUpdate(sm);
            return true;
        } finally {
            exists.close();
        }
    }

    public boolean load(OpenJPAStateManager sm, BitSet fields,
        FetchConfiguration fetch, int lockLevel, Object context) {
        JDBCFetchConfiguration jfetch = (JDBCFetchConfiguration) fetch;

        // get a connection, or reuse current one
        ConnectionInfo info = (ConnectionInfo) context;
        Result res = null;
        if (info != null) {
            // if initialize() fails to load required fields, then this method
            // is called; make sure not to try to use the given result if it's
            // the same one we just failed to completely initialize() with
            if (info.sm != sm)
                res = info.result;
            info.sm = null;
        }
        try {
            // if there's an existing result, load all we can from it
            ClassMapping mapping = (ClassMapping) sm.getMetaData();
            if (res != null) {
                load(mapping, sm, jfetch, res);
                removeLoadedFields(sm, fields);
            }

            // if the instance is hollow and there's a customized
            // get by id method, use it
            if (sm.getLoaded().length() == 0)
                if (mapping.customLoad(sm, this, null, jfetch))
                    removeLoadedFields(sm, fields);

            //### select is kind of a big object, and in some cases we don't
            //### use it... would it be worth it to have a small shell select
            //### object that only creates a real select when actually used?

            Select sel = _sql.newSelect();
            if (select(sel, mapping, sel.SUBS_EXACT, sm, fields, jfetch,
                EagerFetchModes.EAGER_JOIN, true)) {
                sel.wherePrimaryKey(sm.getObjectId(), mapping, this);
                res = sel.execute(this, jfetch, lockLevel);
                try {
                    if (!res.next())
                        return false;
                    load(mapping, sm, jfetch, res);
                } finally {
                    res.close();
                }
            }

            // now allow the fields to load themselves individually too
            FieldMapping[] fms = mapping.getFieldMappings();
            for (int i = 0; i < fms.length; i++)
                if (fields.get(i) && !sm.getLoaded().get(i))
                    fms[i].load(sm, this, jfetch.traverseJDBC(fms[i]));
            mapping.getVersion().afterLoad(sm, this);
            return true;
        } catch (ClassNotFoundException cnfe) {
            throw new StoreException(cnfe);
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    /**
     * Return a list formed by removing all loaded fields from the given one.
     */
    private void removeLoadedFields(OpenJPAStateManager sm, BitSet fields) {
        for (int i = 0, len = fields.length(); i < len; i++)
            if (fields.get(i) && sm.getLoaded().get(i))
                fields.clear(i);
    }

    public Collection loadAll(Collection sms, PCState state, int load,
        FetchConfiguration fetch, Object context) {
        return ImplHelper.loadAll(sms, this, state, load, fetch, context);
    }

    public void beforeStateChange(OpenJPAStateManager sm, PCState fromState,
        PCState toState) {
    }

    public Collection flush(Collection sms) {
        return _conf.getUpdateManagerInstance().flush(sms, this);
    }

    public boolean cancelAll() {
        // note that this method does not lock the context, since
        // we want to allow a different thread to be able to cancel the
        // outstanding statement on a different context

        Collection stmnts;
        synchronized (_stmnts) {
            if (_stmnts.isEmpty())
                return false;
            stmnts = new ArrayList(_stmnts);
        }

        try {
            for (Iterator itr = stmnts.iterator(); itr.hasNext();)
                ((Statement) itr.next()).cancel();
            return true;
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    public boolean assignObjectId(OpenJPAStateManager sm, boolean preFlush) {
        ClassMetaData meta = sm.getMetaData();
        if (meta.getIdentityType() == ClassMetaData.ID_APPLICATION)
            return ApplicationIds.assign(sm, this, preFlush);

        // datastore identity
        Object val = ImplHelper.generateIdentityValue(_ctx, meta,
            JavaTypes.LONG);
        if (val == null && meta.getIdentityStrategy() != ValueStrategies.NATIVE)
            return false;
        if (val == null)
            val = getDataStoreIdSequence(meta).next(_ctx, meta);
        sm.setObjectId(newDataStoreId(val, meta));
        return true;
    }

    public boolean assignField(OpenJPAStateManager sm, int field,
        boolean preFlush) {
        FieldMetaData fmd = sm.getMetaData().getField(field);
        Object val = ImplHelper.generateFieldValue(_ctx, fmd);
        if (val == null)
            return false;
        sm.store(field, val);
        return true;
    }

    public Class getManagedType(Object oid) {
        if (oid instanceof Id)
            return ((Id) oid).getType();
        return null;
    }

    public Class getDataStoreIdType(ClassMetaData meta) {
        return Id.class;
    }

    public Object copyDataStoreId(Object oid, ClassMetaData meta) {
        Id id = (Id) oid;
        return new Id(meta.getDescribedType(), id.getId(), id.hasSubclasses());
    }

    public Object newDataStoreId(Object val, ClassMetaData meta) {
        return Id.newInstance(meta.getDescribedType(), val);
    }

    public Id newDataStoreId(long id, ClassMapping mapping, boolean subs) {
        return new Id(mapping.getDescribedType(), id, subs);
    }

    public ResultObjectProvider executeExtent(ClassMetaData meta,
        final boolean subclasses, FetchConfiguration fetch) {
        ClassMapping mapping = (ClassMapping) meta;
        final ClassMapping[] mappings;
        if (subclasses)
            mappings = mapping.getIndependentAssignableMappings();
        else
            mappings = new ClassMapping[] { mapping };

        ResultObjectProvider[] rops = null;
        final JDBCFetchConfiguration jfetch = (JDBCFetchConfiguration) fetch;
        if (jfetch.getSubclassFetchMode(mapping) != jfetch.EAGER_JOIN)
            rops = new ResultObjectProvider[mappings.length];

        try {
            // check for custom loads
            ResultObjectProvider rop;
            for (int i = 0; i < mappings.length; i++) {
                rop = mappings[i].customLoad(this, subclasses, jfetch, 0,
                    Long.MAX_VALUE);
                if (rop != null) {
                    if (rops == null)
                        rops = new ResultObjectProvider[mappings.length];
                    rops[i] = rop;
                }
            }

            // if we're selecting independent mappings separately or have
            // custom loads, do individual selects for each class
            rop = null;
            if (rops != null) {
                for (int i = 0; i < mappings.length; i++) {
                    if (rops[i] != null)
                        continue;

                    Select sel = _sql.newSelect();
                    sel.setLRS(true);
                    BitSet paged = selectExtent(sel, mappings[i], jfetch,
                        subclasses);
                    if (paged == null)
                        rops[i] = new InstanceResultObjectProvider(sel,
                            mappings[i], this, jfetch);
                    else
                        rops[i] = new PagingResultObjectProvider(sel,
                            mappings[i], this, jfetch, paged, Long.MAX_VALUE);
                }
                if (rops.length == 1)
                    return rops[0];
                return new MergedResultObjectProvider(rops);
            }

            // perform a union on all independent classes
            Union union = _sql.newUnion(mappings.length);
            union.setLRS(true);
            final BitSet[] paged = new BitSet[mappings.length];
            union.select(new Union.Selector() {
                public void select(Select sel, int idx) {
                    paged[idx] = selectExtent(sel, mappings[idx], jfetch,
                        subclasses);
                }
            });

            // using paging rop if any union element has paged fields
            for (int i = 0; i < paged.length; i++) {
                if (paged[i] != null)
                    return new PagingResultObjectProvider(union, mappings,
                        JDBCStoreManager.this, jfetch, paged, Long.MAX_VALUE);
            }
            return new InstanceResultObjectProvider(union, mappings[0], this,
                jfetch);
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    /**
     * Select the given mapping for use in an extent, returning paged fields.
     */
    private BitSet selectExtent(Select sel, ClassMapping mapping,
        JDBCFetchConfiguration fetch, boolean subclasses) {
        int subs = (subclasses) ? Select.SUBS_JOINABLE : Select.SUBS_NONE;
        // decide between paging and standard iteration
        BitSet paged = PagingResultObjectProvider.getPagedFields(sel, mapping,
            this, fetch, JDBCFetchConfiguration.EAGER_PARALLEL,
            Long.MAX_VALUE);
        if (paged == null)
            sel.selectIdentifier(mapping, subs, this, fetch,
                JDBCFetchConfiguration.EAGER_PARALLEL);
        else
            sel.selectIdentifier(mapping, subs, this, fetch,
                JDBCFetchConfiguration.EAGER_JOIN);
        return paged;
    }

    public StoreQuery newQuery(String language) {
        ExpressionParser ep = QueryLanguages.parserForLanguage(language);
        if (ep != null)
            return new JDBCStoreQuery(this, ep);
        if (QueryLanguages.LANG_SQL.equals(language))
            return new SQLStoreQuery(this);
        return null;
    }

    public FetchConfiguration newFetchConfiguration() {
        return new JDBCFetchConfigurationImpl();
    }

    public Seq getDataStoreIdSequence(ClassMetaData meta) {
        if (meta.getIdentityStrategy() == ValueStrategies.NATIVE
            || meta.getIdentityStrategy() == ValueStrategies.NONE)
            return _conf.getSequenceInstance();
        return null;
    }

    public Seq getValueSequence(FieldMetaData fmd) {
        return null;
    }

    public void close() {
        if (_conn != null)
            _conn.free();
    }

    /////////////
    // Utilities
    /////////////

    /**
     * Connect to the db.
     */
    private void connect(boolean ref) {
        _ctx.lock();
        try {
            // connect if the connection is currently null, or if
            // the connection has been closed out from under us
            if (_conn == null)
                _conn = connectInternal();
            if (ref)
                _conn.ref();
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        } finally {
            _ctx.unlock();
        }
    }

    /**
     * Connect to the database. This method is separated out so that it
     * can be profiled.
     */
    private RefCountConnection connectInternal() throws SQLException {
        return new RefCountConnection(_ds.getConnection());
    }

    /**
     * Find the object with the given oid.
     */
    public Object find(Object oid, ValueMapping vm, 
        JDBCFetchConfiguration fetch) {
        if (oid == null)
            return null;
        Object pc = _ctx.find(oid, fetch, null, null, 0);
        if (pc == null && vm != null) {
            OrphanedKeyAction action = _conf.getOrphanedKeyActionInstance();
            pc = action.orphan(oid, null, vm);
        }
        return pc;
    }

    /**
     * Load the object in the current row of the given result.
     */
    public Object load(ClassMapping mapping, JDBCFetchConfiguration fetch,
        BitSet exclude, Result result) throws SQLException {
        if (!mapping.isMapped())
            throw new InvalidStateException(_loc
                .get("virtual-mapping", mapping));

        // get the object id for the row; base class selects pk columns
        ClassMapping base = mapping;
        while (base.getJoinablePCSuperclassMapping() != null)
            base = base.getJoinablePCSuperclassMapping();
        Object oid = base.getObjectId(this, result, null, true, null);
        if (oid == null)
            return null;

        ConnectionInfo info = new ConnectionInfo();
        info.result = result;
        info.mapping = mapping;
        return _ctx.find(oid, fetch, exclude, info, 0);
    }

    /**
     * Load the given state manager with data from the result set. Only
     * mappings originally selected will be loaded.
     */
    private void load(ClassMapping mapping, OpenJPAStateManager sm,
        JDBCFetchConfiguration fetch, Result res) throws SQLException {
        FieldMapping eagerToMany = load(mapping, sm, fetch, res, null);
        if (eagerToMany != null)
            eagerToMany.loadEagerJoin(sm, this, fetch.traverseJDBC(eagerToMany),
                res);
        if (_active && _lm != null && res.isLocking())
            _lm.loadedForUpdate(sm);
    }

    /**
     * Load the fields of the given mapping. Return any to-many eager field
     * without loading it.
     */
    private FieldMapping load(ClassMapping mapping, OpenJPAStateManager sm,
        JDBCFetchConfiguration fetch, Result res, FieldMapping eagerToMany)
        throws SQLException {
        if (mapping.customLoad(sm, this, fetch, res))
            return eagerToMany;

        // load superclass data; base class loads version
        ClassMapping parent = mapping.getJoinablePCSuperclassMapping();
        if (parent != null)
            eagerToMany = load(parent, sm, fetch, res, eagerToMany);
        else if (sm.getVersion() == null)
            mapping.getVersion().load(sm, this, res);

        // load unloaded fields
        FieldMapping[] fms = mapping.getDefinedFieldMappings();
        Object eres, processed;
        for (int i = 0; i < fms.length; i++) {
            if (fms[i].isPrimaryKey() || sm.getLoaded().get(fms[i].getIndex()))
                continue;
            
            // check for eager result, and if not present do standard load
            eres = res.getEager(fms[i]);
            res.startDataRequest(fms[i]);
            try {
               if (eres == res) {
                    if (eagerToMany == null && fms[i].isEagerSelectToMany())
                        eagerToMany = fms[i];
                    else
                        fms[i].loadEagerJoin(sm, this, 
                        	fetch.traverseJDBC(fms[i]), res);
                } else if (eres != null) {
                    processed = fms[i].loadEagerParallel(sm, this, 
                    	fetch.traverseJDBC(fms[i]), eres);
                    if (processed != eres)
                        res.putEager(fms[i], processed);
                } else
                    fms[i].load(sm, this, fetch.traverseJDBC(fms[i]), res);
            } finally {
                res.endDataRequest();
            }
        }
        return eagerToMany;
    }

    /**
     * For implementation use only.
     * Return a select for the proper mappings. Return null if no select is
     * needed. The method is designed to be complementary to the load methods.
     *
     * @param sel select to build on
     * @param mapping the mapping for the base type to select for
     * @param subs whether the select might include subclasses of the
     * given mapping
     * @param sm state manager if an instance is being loaded or
     * initialized, else null
     * @param fields if a state manager is being loaded, the set of
     * fields that must be loaded in order, else null
     * @param fetch the fetch configuration; used if no specific fields
     * must be loaded, and used when selecting relations
     * @param eager eager fetch mode to use
     * @param ident whether to select primary key columns as distinct
     * identifiers
     * @return true if the select is required, false otherwise
     */
    public boolean select(Select sel, ClassMapping mapping, int subs,
        OpenJPAStateManager sm, BitSet fields, JDBCFetchConfiguration fetch,
        int eager, boolean ident) {
        // add class conditions so that they're cloned for any batched selects
        boolean joinedSupers = false;
        if ((sm == null || sm.getPCState() == PCState.TRANSIENT)
            && (subs == sel.SUBS_JOINABLE || subs == sel.SUBS_NONE))
            joinedSupers = addClassConditions(sel, mapping,
                subs == sel.SUBS_JOINABLE, null);

        // create all our eager selects so that those fields are reserved
        // and cannot be reused during the actual eager select process,
        // preventing infinite recursion
        eager = Math.min(eager, fetch.getEagerFetchMode());
        FieldMapping eagerToMany = createEagerSelects(sel, mapping, sm, fields,
            fetch, eager);

        // select all base class mappings; do this after batching so that
        // the joins needed by these selects don't get in the WHERE clause
        // of the batched selects
        int seld = selectBaseMappings(sel, mapping, mapping, sm, fields,
            fetch, eager, eagerToMany, ident, joinedSupers);

        // select eager to-many relations last because during load they
        // advance the result set and could exhaust it, so no other mappings
        // can load afterwords
        if (eagerToMany != null)
            eagerToMany.selectEagerJoin(sel, sm, this, 
                fetch.traverseJDBC(eagerToMany), eager);

        // optionally select subclass mappings
        if (subs == sel.SUBS_JOINABLE || subs == sel.SUBS_ANY_JOINABLE)
            selectSubclassMappings(sel, mapping, sm, fetch);
        if (sm != null)
            sel.setDistinct(false);
        return seld > 0;
    }

    /**
     * Mark the fields of this mapping as reserved so that eager fetches can't
     * get into infinite recursive situations.
     */
    private FieldMapping createEagerSelects(Select sel, ClassMapping mapping,
        OpenJPAStateManager sm, BitSet fields, JDBCFetchConfiguration fetch,
        int eager) {
        if (mapping == null || eager == JDBCFetchConfiguration.EAGER_NONE)
            return null;

        FieldMapping eagerToMany = createEagerSelects(sel, 
            mapping.getJoinablePCSuperclassMapping(), sm, fields, fetch, eager);

        FieldMapping[] fms = mapping.getDefinedFieldMappings();
        boolean inEagerJoin = sel.hasEagerJoin(false);
        int sels;
        int jtype;
        int mode;
        for (int i = 0; i < fms.length; i++) {
            mode = fms[i].getEagerFetchMode();
            if (mode == fetch.EAGER_NONE)
                continue;
            if (!requiresSelect(fms[i], sm, fields, fetch))
                continue;

            // try to select with join first
            jtype = (fms[i].getNullValue() == fms[i].NULL_EXCEPTION) ? sel.EAGER_INNER
                : sel.EAGER_OUTER;
            if (mode != fetch.EAGER_PARALLEL && !fms[i].isEagerSelectToMany()
                && fms[i].supportsSelect(sel, jtype, sm, this, fetch) > 0
                && sel.eagerClone(fms[i], jtype, false, 1) != null)
                continue;

            boolean hasJoin = fetch.hasJoin(fms[i].getFullName());

            // if the field declares a preferred select mode of join or does not
            // have a preferred mode and we're doing a by-id lookup, try
            // to use a to-many join also.  currently we limit eager
            // outer joins to non-LRS, non-ranged selects that don't already
            // have an eager to-many join
            if ((hasJoin || mode == fetch.EAGER_JOIN || (mode == fetch.DEFAULT && sm != null))
                && fms[i].isEagerSelectToMany()
                && !inEagerJoin
                && !sel.hasEagerJoin(true)
                && (!sel.getAutoDistinct() || (!sel.isLRS()
                    && sel.getStartIndex() == 0 && sel.getEndIndex() == Long.MAX_VALUE))
                && fms[i].supportsSelect(sel, jtype, sm, this, fetch) > 0) {
                if (sel.eagerClone(fms[i], jtype, true, 1) != null)
                    eagerToMany = fms[i];
                else
                    continue;
            }

            // finally, try parallel
            if (eager == fetch.EAGER_PARALLEL
                && (sels = fms[i].supportsSelect(sel, sel.EAGER_PARALLEL, sm,
                    this, fetch)) != 0)
                sel.eagerClone(fms[i], Select.EAGER_PARALLEL, fms[i]
                    .isEagerSelectToMany(), sels);
        }
        return eagerToMany;
    }

    /**
     * Determine if the given field needs to be selected.
     */
    private static boolean requiresSelect(FieldMapping fm,
        OpenJPAStateManager sm, BitSet fields, JDBCFetchConfiguration fetch) {
        if (fields != null)
            return fields.get(fm.getIndex());
        if (sm != null && sm.getPCState() != PCState.TRANSIENT
            && sm.getLoaded().get(fm.getIndex()))
            return false;
        return fetch.requiresFetch(fm);
    }

    /**
     * Select the field mappings of the given class and all its superclasses.
     *
     * @param sel the select to use
     * @param mapping the most-derived type to select for
     * @param orig the original mapping type selected
     * @param sm the instance being selected for, or null if none
     * @param fields the fields to load
     * @param fetch fetch configuration to use for loading relations
     * @param eager the eager fetch mode to use
     * @param joined whether the class has already been joined down to
     * its base class
     * @return &gt; 0 if the select is required, 0 if data was
     * selected but is not required, and &lt; 0 if nothing was selected
     */
    private int selectBaseMappings(Select sel, ClassMapping mapping,
        ClassMapping orig, OpenJPAStateManager sm, BitSet fields,
        JDBCFetchConfiguration fetch, int eager, FieldMapping eagerToMany,
        boolean ident, boolean joined) {
        ClassMapping parent = mapping.getJoinablePCSuperclassMapping();
        if (parent == null && !mapping.isMapped())
            throw new InvalidStateException(_loc.get("virtual-mapping", mapping
                .getDescribedType()));

        int seld = -1;
        int pseld = -1;

        // base class selects pks, etc
        if (parent == null) {
            // if no instance, select pks
            if (sm == null) {
                if (ident)
                    sel.selectIdentifier(mapping.getPrimaryKeyColumns());
                else
                    sel.select(mapping.getPrimaryKeyColumns());
                seld = 1;
            }

            // if no instance or not initialized and not exact oid, select type
            if ((sm == null || (sm.getPCState() == PCState.TRANSIENT && (!(sm
                .getObjectId() instanceof OpenJPAId) || ((OpenJPAId) sm
                .getObjectId()).hasSubclasses())))
                && mapping.getDiscriminator().select(sel, orig))
                seld = 1;

            // if no instance or no version, select version
            if ((sm == null || sm.getVersion() == null)
                && mapping.getVersion().select(sel, orig))
                seld = 1;
        } else {
            // recurse on parent
            pseld = selectBaseMappings(sel, parent, orig, sm, fields,
                fetch, eager, eagerToMany, ident, joined);
        }

        // select the mappings in the given fields set, or based on fetch
        // configuration if no fields given
        FieldMapping[] fms = mapping.getDefinedFieldMappings();
        SelectExecutor esel;
        int fseld;
        for (int i = 0; i < fms.length; i++) {
            // skip eager to-many select; we do that separately in calling
            // method
            if (fms[i] == eagerToMany)
                continue;

            // check for eager select
            esel = sel.getEager(fms[i]);
            if (esel != null) {
                if (esel == sel)
                    fms[i].selectEagerJoin(sel, sm, this, 
                    	fetch.traverseJDBC(fms[i]), eager);
                else
                    fms[i].selectEagerParallel(esel, sm, this, 
                    	fetch.traverseJDBC(fms[i]), eager);
                seld = Math.max(0, seld);
            } else if (requiresSelect(fms[i], sm, fields, fetch)) {
                fseld = fms[i].select(sel, sm, this, 
                	fetch.traverseJDBC(fms[i]), eager);
                seld = Math.max(fseld, seld);
            } else if (optSelect(fms[i], sel, sm, fetch)) {
                fseld = fms[i].select(sel, sm, this, 
                	fetch.traverseJDBC(fms[i]), fetch.EAGER_NONE);

                // don't upgrade seld to > 0 based on these fields, since
                // they're not in the calculated field set
                if (fseld >= 0 && seld < 0)
                    seld = 0;
            }
        }

        // join to parent table if the parent / any ancestors have selected
        // anything
        if (!joined && pseld >= 0 && parent.getTable() != mapping.getTable())
            sel.where(mapping.joinSuperclass(sel.newJoins(), false));

        // return the highest value
        return Math.max(pseld, seld);
    }

    /**
     * When selecting fieldes, a special case is made for mappings that use
     * 2-part selects that aren't explicitly *not* in the dfg so that they
     * can get their primary table data. This method tests for that special
     * case as an optimization.
     */
    private boolean optSelect(FieldMapping fm, Select sel,
        OpenJPAStateManager sm, JDBCFetchConfiguration fetch) {
        return !fm.isDefaultFetchGroupExplicit()
            && (sm == null || sm.getPCState() == PCState.TRANSIENT 
            || !sm.getLoaded().get(fm.getIndex()))
            && fm.supportsSelect(sel, sel.TYPE_TWO_PART, sm, this, fetch) > 0;
    }

    /**
     * Select field mappings that match the given fetch configuration for
     * subclasses of the given type.
     *
     * @param sel the select to use
     * @param mapping the type whose subclasses to select
     * @param sm the instance being selected for, or null if none
     * @param fetch the fetch configuration
     */
    private void selectSubclassMappings(Select sel, ClassMapping mapping,
        OpenJPAStateManager sm, JDBCFetchConfiguration fetch) {
        loadSubclasses(mapping);
        ClassMapping[] subMappings = mapping.getJoinablePCSubclassMappings();
        if (subMappings.length == 0)
            return;

        // select all subclass mappings that match the fetch configuration
        // and whose table is in the list of those selected so far; this
        // way we select the max possible without selecting any tables that
        // aren't present in all possible query matches; a special case
        // is made for mappings that use 2-part selects that aren't
        // explicitly *not* in the default so that they can get their
        // primary table data
        FieldMapping[] fms;
        boolean joined;
        boolean canJoin = _dict.joinSyntax != JoinSyntaxes.SYNTAX_TRADITIONAL
            && fetch.getSubclassFetchMode(mapping) != fetch.EAGER_NONE;
        for (int i = 0; i < subMappings.length; i++) {
            if (!subMappings[i].supportsEagerSelect(sel, sm, this, mapping,
                fetch))
                continue;

            // initialize so that if we can't join, we pretend we already have
            joined = !canJoin;
            fms = subMappings[i].getDefinedFieldMappings();
            for (int j = 0; j < fms.length; j++) {
                // make sure in one of configured fetch groups
            	if (fetch.requiresFetch(fms[j]) 
            	  || fms[j].supportsSelect(sel, sel.TYPE_TWO_PART, sm, this, 
                    fetch) <= 0) 
            		continue;

                // if we can join to the subclass, do so; much better chance
                // that the field will be able to select itself without joins
                if (!joined) {
                    // mark joined whether or not we join, so we don't have to
                    // test conditions again for this subclass
                    joined = true;
                    sel.where(joinSubclass(sel, mapping, subMappings[i], null));
                }

                // if can select with tables already selected, do it
                if (fms[j].supportsSelect(sel, sel.TYPE_JOINLESS, sm, this,
                    fetch) > 0 && fetch.requiresFetch(fms[j])) {
                    fms[j].select(sel, null, this, fetch.traverseJDBC (fms[j]),
                        fetch.EAGER_NONE);
                }
            }
        }
    }

    /**
     * Helper method to join from class to its subclass. Recursive to allow
     * for multiple hops, starting from the base class.
     */
    private static Joins joinSubclass(Select sel, ClassMapping base,
        ClassMapping sub, Joins joins) {
        if (sub == base || sub.getTable() == base.getTable()
            || sel.isSelected(sub.getTable()))
            return null;

        // recurse first so we go least->most derived
        ClassMapping sup = sub.getJoinablePCSuperclassMapping();
        joins = joinSubclass(sel, base, sup, joins);
        if (joins == null)
            joins = sel.newJoins();
        return sub.joinSuperclass(joins, true);
    }

    /**
     * Makes sure all subclasses of the given type are loaded in the JVM.
     * This is usually done automatically.
     */
    public void loadSubclasses(ClassMapping mapping) {
        Discriminator dsc = mapping.getDiscriminator();
        if (dsc.getSubclassesLoaded())
            return;

        // if the subclass list is set, no need to load subs
        if (mapping.getRepository().getPersistentTypeNames(false,
            _ctx.getClassLoader()) != null) {
            dsc.setSubclassesLoaded(true);
            return;
        }

        try {
            dsc.loadSubclasses(this);
        } catch (ClassNotFoundException cnfe) {
            throw new StoreException(cnfe);
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, _dict);
        }
    }

    /**
     * Add WHERE conditions to the given select limiting the returned results
     * to the given mapping type, possibly including subclasses.
     *
     * @return true if the mapping was joined down to its base class
     * in order to add the conditions
     */
    public boolean addClassConditions(Select sel, ClassMapping mapping,
        boolean subs, Joins joins) {
        loadSubclasses(mapping);
        if (mapping.getJoinablePCSuperclassMapping() == null
            && mapping.getJoinablePCSubclassMappings().length == 0)
            return false;

        // join down to base class where the conditions will be added
        ClassMapping from = mapping;
        ClassMapping sup = mapping.getJoinablePCSuperclassMapping();
        for (; sup != null; from = sup, sup = from
            .getJoinablePCSuperclassMapping()) {
            if (from.getTable() != sup.getTable()) {
                if (joins == null)
                    joins = sel.newJoins();
                joins = from.joinSuperclass(joins, false);
            }
        }

        Discriminator dsc = mapping.getDiscriminator();
        SQLBuffer buf = dsc.getClassConditions(this, sel, joins, mapping, subs);
        if (buf != null) {
            sel.where(buf, joins);
            return true;
        }
        return false;
    }

    /**
     * Make the statement a candidate for cancellation.
     */
    private void beforeExecuteStatement(Statement stmnt) {
        _stmnts.add(stmnt);
    }

    /**
     * Remove the statement from the cancellable set.
     */
    private void afterExecuteStatement(Statement stmnt) {
        _stmnts.remove(stmnt);
    }

    /**
     * Connection returned to client code. Makes sure its wrapped connection
     * ref count is decremented on finalize.
     */
    private static class ClientConnection extends DelegatingConnection {

        private boolean _closed = false;

        public ClientConnection(Connection conn) {
            super(conn);
        }

        public void close() throws SQLException {
            _closed = true;
            super.close();
        }

        protected void finalize() throws SQLException {
            if (!_closed)
                close();
        }
    }

    /**
     * Connection wrapper that keeps an internal ref count so that it knows
     * when to really close.
     */
    private class RefCountConnection extends DelegatingConnection {

        private boolean _retain = false;
        private int _refs = 0;
        private boolean _freed = false;

        public RefCountConnection(Connection conn) {
            super(conn);
        }

        public boolean getRetain() {
            return _retain;
        }

        public void setRetain(boolean retain) {
            if (_retain && !retain && _refs <= 0)
                free();
            _retain = retain;
        }

        public void ref() {
            // don't have to lock; called from connect(), which is locked
            _refs++;
        }

        public void close() throws SQLException {
            // lock at broker level to avoid deadlocks
            _ctx.lock();
            try {
                _refs--;
                if (_refs <= 0 && !_retain)
                    free();
            } finally {
                _ctx.unlock();
            }
        }

        public void free() {
            // ensure that we do not close the underlying connection
            // multiple times; this could happen if someone (e.g., an
            // Extent) holds a RefConnection, and then closes it (e.g., in
            // the finalizer) after the StoreManager has already been closed.
            if (_freed)
                return;

            try {
                getDelegate().close();
            } catch (SQLException se) {
            }
            _freed = true;
            _conn = null;
        }

        protected Statement createStatement(boolean wrap) throws SQLException {
            return new CancelStatement(super.createStatement(false),
                RefCountConnection.this);
        }

        protected Statement createStatement(int rsType, int rsConcur,
            boolean wrap) throws SQLException {
            return new CancelStatement(super.createStatement(rsType, rsConcur,
                false), RefCountConnection.this);
        }

        protected PreparedStatement prepareStatement(String sql, boolean wrap)
            throws SQLException {
            return new CancelPreparedStatement(super.prepareStatement(sql,
                false), RefCountConnection.this);
        }

        protected PreparedStatement prepareStatement(String sql, int rsType,
            int rsConcur, boolean wrap) throws SQLException {
            return new CancelPreparedStatement(super.prepareStatement(sql,
                rsType, rsConcur, false), RefCountConnection.this);
        }
    }

    /**
     * Statement type that adds and removes itself from the set of active
     * statements so that it can be canceled.
     */
    private class CancelStatement extends DelegatingStatement {

        public CancelStatement(Statement stmnt, Connection conn) {
            super(stmnt, conn);
        }

        public int executeUpdate(String sql) throws SQLException {
            beforeExecuteStatement(this);
            try {
                return super.executeUpdate(sql);
            } finally {
                afterExecuteStatement(this);
            }
        }

        protected ResultSet executeQuery(String sql, boolean wrap)
            throws SQLException {
            beforeExecuteStatement(this);
            try {
                return super.executeQuery(sql, wrap);
            } finally {
                afterExecuteStatement(this);
            }
        }
    }

    /**
     * Statement type that adds and removes itself from the set of active
     * statements so that it can be canceled.
     */
    private class CancelPreparedStatement extends DelegatingPreparedStatement {

        public CancelPreparedStatement(PreparedStatement stmnt, Connection conn) {
            super(stmnt, conn);
        }

        public int executeUpdate() throws SQLException {
            beforeExecuteStatement(this);
            try {
                return super.executeUpdate();
            } finally {
                afterExecuteStatement(this);
            }
        }

        protected ResultSet executeQuery(boolean wrap) throws SQLException {
            beforeExecuteStatement(this);
            try {
                return super.executeQuery(wrap);
            } finally {
                afterExecuteStatement(this);
            }
        }

        public int[] executeBatch() throws SQLException {
            beforeExecuteStatement(this);
            try {
                return super.executeBatch();
            } finally {
                afterExecuteStatement(this);
            }
        }
    }
}
