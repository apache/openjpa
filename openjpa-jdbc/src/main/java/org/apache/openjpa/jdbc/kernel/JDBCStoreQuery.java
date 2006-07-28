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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.jdbc.kernel.exps.GetColumn;
import org.apache.openjpa.jdbc.kernel.exps.JDBCExpressionFactory;
import org.apache.openjpa.jdbc.kernel.exps.JDBCStringContains;
import org.apache.openjpa.jdbc.kernel.exps.JDBCWildcardMatch;
import org.apache.openjpa.jdbc.kernel.exps.SQLEmbed;
import org.apache.openjpa.jdbc.kernel.exps.SQLExpression;
import org.apache.openjpa.jdbc.kernel.exps.SQLValue;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.strats.VerticalClassStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.jdbc.sql.Union;
import org.apache.openjpa.kernel.ExpressionStoreQuery;
import org.apache.openjpa.kernel.OrderingMergedResultObjectProvider;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.lib.rop.MergedResultObjectProvider;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.UserException;
import serp.util.Numbers;

/**
 * JDBC query implementation.
 *
 * @author Abe White
 * @nojavadoc
 */
public class JDBCStoreQuery 
    extends ExpressionStoreQuery {

    private static final Table INVALID = new Table();

    // add all standard filter and aggregate listeners to these maps
    private static final Map _listeners = new HashMap();

    static {
        // deprecated extensions
        _listeners.put(JDBCStringContains.TAG, new JDBCStringContains());
        _listeners.put(JDBCWildcardMatch.TAG, new JDBCWildcardMatch());
        _listeners.put(SQLExpression.TAG, new SQLExpression());
        _listeners.put(SQLValue.TAG, new SQLValue());

        // jdbc-specific extensions
        _listeners.put(GetColumn.TAG, new GetColumn());
        _listeners.put(SQLEmbed.TAG, new SQLEmbed());
    }

    private final transient JDBCStore _store;

    /**
     * Constructor. Supply store manager.
     */
    public JDBCStoreQuery(JDBCStore store, ExpressionParser parser) {
        super(parser);
        _store = store;
    }

    /**
     * Return the store.
     */
    public JDBCStore getStore() {
        return _store;
    }

    public FilterListener getFilterListener(String tag) {
        return (FilterListener) _listeners.get(tag);
    }

    public Object newCompilationKey() {
        JDBCFetchConfiguration fetch = (JDBCFetchConfiguration) ctx
            .getFetchConfiguration();
        return Numbers.valueOf(fetch.getJoinSyntax());
    }

    public boolean supportsDataStoreExecution() {
        return true;
    }

    protected ClassMetaData[] getIndependentExpressionCandidates(
        ClassMetaData meta, boolean subclasses) {
        if (!subclasses)
            return new ClassMapping[] { (ClassMapping) meta };
        return ((ClassMapping) meta).getIndependentAssignableMappings();
    }

    protected ExpressionFactory getExpressionFactory(ClassMetaData meta) {
        return new JDBCExpressionFactory((ClassMapping) meta);
    }

    protected ResultObjectProvider executeQuery(Executor ex,
        ClassMetaData base, ClassMetaData[] metas, boolean subclasses,
        ExpressionFactory[] facts, QueryExpressions[] exps, Object[] params,
        boolean lrs, long startIdx, long endIdx) {
        if (metas.length > 1 && exps[0].aggregate)
            throw new UserException(Localizer.forPackage(JDBCStoreQuery.class)
                .get("mult-mapping-aggregate", Arrays.asList(metas)));

        ClassMapping[] mappings = (ClassMapping[]) metas;
        JDBCFetchConfiguration fetch = (JDBCFetchConfiguration) 
            ctx.getFetchConfiguration();
        if (exps[0].fetchPaths != null) {
            fetch.addFields(Arrays.asList(exps[0].fetchPaths));
            fetch.addJoins(Arrays.asList(exps[0].fetchPaths));
        }

        int eager = calculateEagerMode(exps[0], startIdx, endIdx);
        int subclassMode = fetch.getSubclassFetchMode((ClassMapping) base);
        DBDictionary dict = _store.getDBDictionary();
        long start = (mappings.length == 1 && dict.supportsSelectStartIndex) 
            ? startIdx : 0L;
        long end = (dict.supportsSelectEndIndex) ? endIdx : Long.MAX_VALUE;

        // add selects with populate WHERE conditions to list
        List sels = new ArrayList(mappings.length);
        List selMappings = new ArrayList(mappings.length);
        BitSet subclassBits = new BitSet();
        BitSet nextBits = new BitSet();
        boolean unionable = createWhereSelects(sels, mappings, selMappings,
            subclasses, subclassBits, nextBits, facts, exps, params,
            fetch, subclassMode)
            && subclassMode == JDBCFetchConfiguration.EAGER_JOIN
            && start == 0
            && end == Long.MAX_VALUE;

        // we might want to use lrs settings if we can't use the range
        if (sels.size() > 1)
            start = 0L;
        lrs = lrs || (fetch.getFetchBatchSize() >= 0 && (start != startIdx 
            || end != endIdx));

        ResultObjectProvider[] rops = null;
        ResultObjectProvider rop = null;
        if (unionable) {
            Union union = _store.getSQLFactory().newUnion(
                (Select[]) sels.toArray(new Select[sels.size()]));
            BitSet[] paged = populateUnion(union, mappings, subclasses, facts,
                exps, params, fetch, lrs, eager, start, end);
            union.setLRS(lrs);
            rop = executeUnion(union, mappings, exps, paged, fetch);
        } else {
            if (sels.size() > 1)
                rops = new ResultObjectProvider[sels.size()];

            Select sel;
            BitSet paged;
            for (int i = 0, idx = 0; i < sels.size(); i++) {
                sel = (Select) sels.get(i);
                paged = populateSelect(sel, (ClassMapping) selMappings.get(i),
                    subclassBits.get(i), (JDBCExpressionFactory) facts[idx],
                    exps[idx], params, fetch, lrs, eager, start, end);

                rop = executeSelect(sel, (ClassMapping) selMappings.get(i),
                    exps[idx], paged, fetch, start, end);
                if (rops != null)
                    rops[i] = rop;

                if (nextBits.get(i))
                    idx++;
            }
        }

        if (rops != null) {
            if (exps[0].ascending.length == 0)
                rop = new MergedResultObjectProvider(rops);
            else {
                rop = new OrderingMergedResultObjectProvider(rops,
                    exps[0].ascending, ex, this, params);
            }
        }

        // need to fake result range?
        if ((rops != null && endIdx != Long.MAX_VALUE) || start != startIdx
            || end != endIdx)
            rop = new RangeResultObjectProvider(rop, startIdx, endIdx);
        return rop;
    }

    /**
     * Select data for the given union, returning paged fields.
     */
    private BitSet[] populateUnion(Union union, final ClassMapping[] mappings,
        final boolean subclasses, final ExpressionFactory[] facts,
        final QueryExpressions[] exps, final Object[] params,
        final JDBCFetchConfiguration fetch, final boolean lrs, final int eager,
        final long start, final long end) {
        final BitSet[] paged = (exps[0].projections.length > 0) ? null
            : new BitSet[mappings.length];
        union.select(new Union.Selector() {

            public void select(Select sel, int idx) {
                BitSet bits = populateSelect(sel, mappings[idx], subclasses,
                    (JDBCExpressionFactory) facts[idx], exps[idx], params,
                    fetch, lrs, eager, start, end);
                if (paged != null)
                    paged[idx] = bits;
            }
        });
        return paged;
    }

    /**
     * Select data for the given select, returning paged fields.
     */
    private BitSet populateSelect(Select sel, ClassMapping mapping,
        boolean subclasses, JDBCExpressionFactory fact, QueryExpressions exps,
        Object[] params, JDBCFetchConfiguration fetch, boolean lrs, int eager,
        long start, long end) {
        sel.setLRS(lrs);
        sel.setRange(start, end);

        BitSet paged = null;
        if (exps.projections.length == 0) {
            paged = PagingResultObjectProvider.getPagedFields(sel, mapping,
                _store, fetch, eager, end - start);
            if (paged != null)
                eager = JDBCFetchConfiguration.EAGER_JOIN;
        }

        fact.select(this, mapping, subclasses, sel, exps, params, fetch,
            eager);
        return paged;
    }

    /**
     * Execute the given union.
     */
    private ResultObjectProvider executeUnion(Union union,
        ClassMapping[] mappings, QueryExpressions[] exps, BitSet[] paged,
        JDBCFetchConfiguration fetch) {
        if (exps[0].projections.length > 0)
            return new ProjectionResultObjectProvider(union, _store,
                fetch, exps);

        if (paged != null)
            for (int i = 0; i < paged.length; i++)
                if (paged[i] != null)
                    return new PagingResultObjectProvider(union, mappings,
                        _store, fetch, paged, Long.MAX_VALUE);

        return new InstanceResultObjectProvider(union, mappings[0], _store,
            fetch);
    }

    /**
     * Execute the given select.
     */
    private ResultObjectProvider executeSelect(Select sel,
        ClassMapping mapping, QueryExpressions exps, BitSet paged,
        JDBCFetchConfiguration fetch, long start, long end) {
        if (exps.projections.length > 0)
            return new ProjectionResultObjectProvider(sel, _store, fetch, exps);
        if (paged != null)
            return new PagingResultObjectProvider(sel, mapping, _store, fetch, 
                paged, end - start);
        return new InstanceResultObjectProvider(sel, mapping, _store, fetch);
    }

    /**
     * Generate the selects with WHERE conditions needed to execute the query
     * for the given mappings.
     */
    private boolean createWhereSelects(List sels, ClassMapping[] mappings,
        List selMappings, boolean subclasses, BitSet subclassBits,
        BitSet nextBits, ExpressionFactory[] facts, QueryExpressions[] exps,
        Object[] params, JDBCFetchConfiguration fetch, int subclassMode) {
        Select sel;
        ClassMapping[] verts;
        boolean unionable = true;
        for (int i = 0; i < mappings.length; i++) {
            // determine vertical mappings to select separately
            verts = getVerticalMappings(mappings[i], subclasses, exps[i],
                subclassMode);
            if (verts.length == 1 && subclasses)
                subclassBits.set(sels.size());

            // create criteria select and clone for each vert mapping
            sel = ((JDBCExpressionFactory) facts[i]).evaluate(this, fetch,
                exps[i], params);
            for (int j = 0; j < verts.length; j++) {
                selMappings.add(verts[j]);
                if (j == verts.length - 1) {
                    nextBits.set(sels.size());
                    sels.add(sel);
                } else
                    sels.add(sel.fullClone(1));
            }

            // turn off unioning if a given independent mapping requires
            // multiple selects, or if we're using FROM selects
            if (verts.length > 1 || sel.getFromSelect() != null)
                unionable = false;
        }
        return unionable;
    }

    /**
     * Return all the vertical mappings to select separately. Depends on
     * subclass fetch mode and the type of query.
     */
    private ClassMapping[] getVerticalMappings(ClassMapping mapping,
        boolean subclasses, QueryExpressions exps, int subclassMode) {
        if (!subclasses || exps.projections.length > 0)
            return new ClassMapping[] { mapping };

        if (subclassMode != JDBCFetchConfiguration.EAGER_PARALLEL
            || !hasVerticalSubclasses(mapping))
            return new ClassMapping[] { mapping };

        List subs = new ArrayList(4);
        addSubclasses(mapping, subs);
        return (ClassMapping[]) subs.toArray(new ClassMapping[subs.size()]);
    }

    /**
     * Recursive helper to add mappings for subclasses to the given list.
     */
    private void addSubclasses(ClassMapping mapping, Collection subs) {
        // possible future optimizations:
        // - if no fields in meta or its subclasses (and not in an
        //   already-selected table) are in the current fetch
        //   configuration, stop creating new executors
        // - allow an executor to select a range of subclasses, rather
        //   than just all subclasses / no subclasses; this would
        //   allow us to do just one query per actual vertically-mapped
        //   subclass, rather than one per mapped subclass, as is happening now

        subs.add(mapping);
        if (!hasVerticalSubclasses(mapping))
            return;

        // recurse on immediate subclasses
        ClassMapping[] subMappings = mapping.getJoinablePCSubclassMappings();
        for (int i = 0; i < subMappings.length; i++)
            if (subMappings[i].getJoinablePCSuperclassMapping() == mapping)
                addSubclasses(subMappings[i], subs);
    }

    /**
     * Return whether the given class has any vertical subclasses.
     */
    private static boolean hasVerticalSubclasses(ClassMapping mapping) {
        ClassMapping[] subs = mapping.getJoinablePCSubclassMappings();
        for (int i = 0; i < subs.length; i++)
            if (subs[i].getStrategy() instanceof VerticalClassStrategy)
                return true;
        return false;
    }

    /**
     * The eager mode depends on the unique setting and range. If the range
     * produces 0 results, use eager setting of none. If it produces 1 result
     * or the query is unique, use an eager setting of single. Otherwise use
     * an eager mode of multiple.
     */
    private int calculateEagerMode(QueryExpressions exps, long startIdx,
        long endIdx) {
        if (exps.projections.length > 0 || startIdx >= endIdx)
            return EagerFetchModes.EAGER_NONE;
        if (endIdx - startIdx == 1 || ctx.isUnique())
            return EagerFetchModes.EAGER_JOIN;
        return EagerFetchModes.EAGER_PARALLEL;
    }

    protected Number executeDelete(Executor ex, ClassMetaData base,
        ClassMetaData[] metas, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] exps, Object[] params) {
        return executeBulkOperation(ex, base, metas, subclasses, facts, exps,
            params, null);
    }

    protected Number executeUpdate(Executor ex, ClassMetaData base,
        ClassMetaData[] metas, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] exps, Object[] params) {
        return executeBulkOperation(ex, base, metas, subclasses, facts, exps,
            params, exps[0].updates);
    }

    private Number executeBulkOperation(Executor ex, ClassMetaData base,
        ClassMetaData[] metas, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] exps, Object[] params, Map updates) {
        // we cannot execute a bulk delete statement when have mappings in
        // multiple tables, so indicate we want to use in-memory with null
        ClassMapping[] mappings = (ClassMapping[]) metas;
        boolean isUpdate = updates != null && updates.size() > 0;
        for (int i = 0; i < mappings.length; i++) {
            if (!isSingleTableMapping(mappings[i], subclasses) && !isUpdate)
                return null;

            if (!isUpdate) {
                // if there are any delete callbacks, we need to
                // execute in-memory so the callbacks are invoked
                LifecycleEventManager mgr = ctx.getStoreContext().getBroker()
                    .getLifecycleEventManager();
                if (mgr.hasDeleteListeners(null, mappings[i]))
                    return null;
            }
        }

        JDBCFetchConfiguration fetch = (JDBCFetchConfiguration) 
            ctx.getFetchConfiguration();
        DBDictionary dict = _store.getDBDictionary();

        SQLBuffer[] sql = new SQLBuffer[mappings.length];
        JDBCExpressionFactory jdbcFactory;
        Select sel;
        for (int i = 0; i < mappings.length; i++) {
            jdbcFactory = (JDBCExpressionFactory) facts[i];
            sel = jdbcFactory.evaluate(this, fetch, exps[i], params);
            jdbcFactory.select(this, mappings[i], subclasses, sel, exps[i],
                params, fetch, JDBCFetchConfiguration.EAGER_NONE);

            // specification of the "udpates" map indicates that this is
            // an update query; otherwise, this is a delete statement
            // The bulk operation will return null to indicate that the database
            // does not support the request bulk delete operation; in
            // this case, we need to perform the query in-memory and
            // manually delete the instances
            if (updates == null)
                sql[i] = dict.toDelete(mappings[i], sel, _store, params);
            else
                sql[i] = dict.toUpdate(mappings[i], sel, _store, params,
                    updates);

            if (sql[i] == null)
                return null;
        }

        // we need to make sure we have an active store connection
        ctx.getStoreContext().beginStore();

        Connection conn = _store.getConnection();
        long count = 0;
        try {
            PreparedStatement stmnt;
            for (int i = 0; i < sql.length; i++) {
                stmnt = null;
                try {
                    stmnt = sql[i].prepareStatement(conn);
                    count += stmnt.executeUpdate();
                } finally {
                    if (stmnt != null)
                        try {
                            stmnt.close();
                        } catch (SQLException se) {
                        }
                }
            }
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, ctx, _store.getDBDictionary());
        } finally {
            try {
                conn.close();
            } catch (SQLException se) {
            }
        }
        return Numbers.valueOf(count);
    }

    /**
     * Whether the given mapping occupies only one table.
     */
    private boolean isSingleTableMapping(ClassMapping mapping,
        boolean subclasses) {
        ClassMapping root = mapping;
        while (root.getJoinablePCSuperclassMapping() != null)
            root = root.getJoinablePCSuperclassMapping();
        if (hasVerticalSubclasses(root))
            return false;

        // we cannot execute a bulk delete if any of the
        // field mappings for the candidates have columns
        // in any other table, since bulk deleting just from the
        // class will leave dangling relations; we might be able
        // to issue bulk deletes separately for the joins (possibly
        // using a temporary table to select the primary keys for
        // all the related tables and then issing a delete against those
        // keys), but that logic is not currently implemented
        Table table = getTable(mapping.getFieldMappings(), null);
        if (table == INVALID)
            return false;

        if (subclasses) {
            // if we are including subclasses, we also need to gather
            // all the mappings for all known subclasses
            ClassMapping[] subs = mapping.getJoinablePCSubclassMappings();
            for (int i = 0; subs != null && i < subs.length; i++) {
                table = getTable(subs[i].getDefinedFieldMappings(), table);
                if (table == INVALID)
                    return false;
            }
        }
        return true;
    }

    /**
     * Return the single table for the given fields, or INVALID if they
     * use multiple tables.
     */
    private Table getTable(FieldMapping[] fields, Table table) {
        for (int i = 0; i < fields.length; i++) {
            table = getTable(fields[i], table);
            if (table == INVALID)
                break;
        }
        return table;
    }

    /**
     * Return the table for the field if the given table hasn't been set
     * yet, or if the tables match. If the field uses a different table,
     * returns INVALID. Also returns INVALID if field is dependent.
     */
    private Table getTable(FieldMapping fm, Table table) {
        if (fm.getCascadeDelete() != ValueMetaData.CASCADE_NONE)
            return INVALID;

        Column[] columns = fm.getColumns();
        for (int i = 0; columns != null && i < columns.length; i++) {
            if (table == null)
                table = columns[i].getTable();
            else if (table != columns[i].getTable())
                return INVALID;
        }
        return table;
    }

    protected Number executeUpdate(ClassMetaData base, ClassMetaData[] metas,
        boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] parsed, Object[] params) {
        return null;
    }

    protected String[] getDataStoreActions(ClassMetaData base,
        ClassMetaData[] metas, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] exps, Object[] params, long startIdx, long endIdx) {
        ClassMapping[] mappings = (ClassMapping[]) metas;
        JDBCFetchConfiguration fetch = (JDBCFetchConfiguration) ctx
            .getFetchConfiguration();
        if (exps[0].fetchPaths != null) {
            fetch.addFields(Arrays.asList(exps[0].fetchPaths));
            fetch.addJoins(Arrays.asList(exps[0].fetchPaths));
        }

        int eager = calculateEagerMode(exps[0], startIdx, endIdx);
        eager = Math.min(eager, JDBCFetchConfiguration.EAGER_JOIN);
        int subclassMode = fetch.getSubclassFetchMode((ClassMapping) base);
        DBDictionary dict = _store.getDBDictionary();
        long start = (mappings.length == 1 && dict.supportsSelectStartIndex) 
            ? startIdx : 0L;
        long end = (dict.supportsSelectEndIndex) ? endIdx : Long.MAX_VALUE;

        // add selects with populate WHERE conditions to list
        List sels = new ArrayList(mappings.length);
        List selMappings = new ArrayList(mappings.length);
        BitSet subclassBits = new BitSet();
        BitSet nextBits = new BitSet();
        boolean unionable = createWhereSelects(sels, mappings, selMappings,
            subclasses, subclassBits, nextBits, facts, exps, params, fetch, 
            subclassMode) && subclassMode == JDBCFetchConfiguration.EAGER_JOIN;
        if (sels.size() > 1)
            start = 0L;

        if (unionable) {
            Union union = _store.getSQLFactory().newUnion(
                (Select[]) sels.toArray(new Select[sels.size()]));
            populateUnion(union, mappings, subclasses, facts, exps, params,
                fetch, false, eager, start, end);
            if (union.isUnion())
                return new String[] {union.toSelect(false, fetch).getSQL(true)};
            sels = Arrays.asList(union.getSelects());
        } else {
            Select sel;
            for (int i = 0, idx = 0; i < sels.size(); i++) {
                sel = (Select) sels.get(i);
                populateSelect(sel, (ClassMapping) selMappings.get(i),
                    subclassBits.get(i), (JDBCExpressionFactory) facts[idx],
                    exps[idx], params, fetch, false, eager, start, end);
                if (nextBits.get(i))
                    idx++;
            }
        }

        String[] sql = new String[sels.size()];
        for (int i = 0; i < sels.size(); i++)
            sql[i] = ((Select) sels.get(i)).toSelect(false, fetch).getSQL(true);
        return sql;
    }
}
