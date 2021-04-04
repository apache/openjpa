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
package org.apache.openjpa.jdbc.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;

/**
 * A logical union made up of multiple distinct selects whose results are
 * combined in memory.
 *
 * @author Abe White
 */
public class LogicalUnion
    implements Union {

    private static final Localizer _loc = Localizer.forPackage
        (LogicalUnion.class);

    protected final UnionSelect[] sels;
    protected final DBDictionary dict;
    protected final ClassMapping[] mappings;
    protected final BitSet desc = new BitSet();
    private boolean _distinct = true;


    /**
     * Constructor.
     *
     * @param conf system configuration
     * @param sels the number of SQL selects to union together
     */
    public LogicalUnion(JDBCConfiguration conf, int sels) {
        this(conf, sels, null);
    }

    /**
     * Constructor used to seed the internal selects.
     */
    public LogicalUnion(JDBCConfiguration conf, Select[] seeds) {
        this(conf, seeds.length, seeds);
    }

    /**
     * Delegate constructor.
     */
    protected LogicalUnion(JDBCConfiguration conf, int sels, Select[] seeds) {
        if (sels == 0)
            throw new InternalException("sels == 0");

        dict = conf.getDBDictionaryInstance();
        mappings = new ClassMapping[sels];
        this.sels = new UnionSelect[sels];

        SelectImpl seed;
        for (int i = 0; i < sels; i++) {
            seed = (seeds == null)
                ? (SelectImpl) conf.getSQLFactoryInstance().newSelect()
                : (SelectImpl) seeds[i];
            this.sels[i] = newUnionSelect(seed, i);
        }
    }

    /**
     * Create a new union select with the given delegate and union position.
     */
    protected UnionSelect newUnionSelect(SelectImpl seed, int pos) {
        return new UnionSelect(seed, pos);
    }

    @Override
    public Select[] getSelects() {
        return sels;
    }

    @Override
    public boolean isUnion() {
        return false;
    }

    @Override
    public void abortUnion() {
    }

    @Override
    public String getOrdering() {
        return null;
    }

    @Override
    public JDBCConfiguration getConfiguration() {
        return sels[0].getConfiguration();
    }

    public DBDictionary getDBDictionary() {
        return dict;
    }

    @Override
    public SQLBuffer toSelect(boolean forUpdate, JDBCFetchConfiguration fetch) {
        return dict.toSelect(sels[0], forUpdate, fetch);
    }

    @Override
    public SQLBuffer getSQL() {
        return sels.length == 1 ? sels[0].getSQL() : null;
    }

    @Override
    public SQLBuffer toSelectCount() {
        return dict.toSelectCount(sels[0]);
    }

    @Override
    public boolean getAutoDistinct() {
        return sels[0].getAutoDistinct();
    }

    @Override
    public void setAutoDistinct(boolean distinct) {
        for (int i = 0; i < sels.length; i++)
            sels[i].setAutoDistinct(distinct);
    }

    @Override
    public boolean isDistinct() {
        return _distinct;
    }

    @Override
    public void setDistinct(boolean distinct) {
        _distinct = distinct;
    }

    @Override
    public boolean isLRS() {
        return sels[0].isLRS();
    }

    @Override
    public void setLRS(boolean lrs) {
        for (int i = 0; i < sels.length; i++)
            sels[i].setLRS(lrs);
    }

    @Override
    public int getExpectedResultCount() {
        return sels[0].getExpectedResultCount();
    }

    @Override
    public void setExpectedResultCount(int expectedResultCount,
        boolean force) {
        for (int i = 0; i < sels.length; i++)
            sels[i].setExpectedResultCount(expectedResultCount, force);
    }

    @Override
    public int getJoinSyntax() {
        return sels[0].getJoinSyntax();
    }

    @Override
    public void setJoinSyntax(int syntax) {
        for (int i = 0; i < sels.length; i++)
            sels[i].setJoinSyntax(syntax);
    }

    @Override
    public boolean supportsRandomAccess(boolean forUpdate) {
        if (sels.length == 1)
            return sels[0].supportsRandomAccess(forUpdate);
        return false;
    }

    @Override
    public boolean supportsLocking() {
        if (sels.length == 1)
            return sels[0].supportsLocking();
        for (int i = 0; i < sels.length; i++)
            if (!sels[i].supportsLocking())
                return false;
        return true;
    }

    @Override
    public boolean hasMultipleSelects() {
        if (sels != null && sels.length > 1)
            return true;
        return sels[0].hasMultipleSelects();
    }

    @Override
    public int getCount(JDBCStore store)
        throws SQLException {
        int count = 0;
        for (int i = 0; i < sels.length; i++)
            count += sels[i].getCount(store);
        return count;
    }

    @Override
    public Result execute(JDBCStore store, JDBCFetchConfiguration fetch)
        throws SQLException {
        if (fetch == null)
            fetch = store.getFetchConfiguration();
        return execute(store, fetch, fetch.getReadLockLevel());
    }

    @Override
    public Result execute(JDBCStore store, JDBCFetchConfiguration fetch,
        int lockLevel)
        throws SQLException {
        if (fetch == null)
            fetch = store.getFetchConfiguration();

        if (sels.length == 1) {
            Result res = sels[0].execute(store, fetch, lockLevel);
            ((AbstractResult) res).setBaseMapping(mappings[0]);
            return res;
        }

        if (getExpectedResultCount() == 1) {
            AbstractResult res;
            for (int i = 0; i < sels.length; i++) {
                res = (AbstractResult) sels[i].execute(store, fetch,
                    lockLevel);
                res.setBaseMapping(mappings[i]);
                res.setIndexOf(i);

                // if we get to the last select, just return its result
                if (i == sels.length - 1)
                    return res;

                // return the first result that has a row
                try {
                    if (!res.next())
                        res.close();
                    else {
                        res.pushBack();
                        return res;
                    }
                }
                catch (SQLException se) {
                    res.close();
                    throw se;
                }
            }
        }

        // create a single result from each select in our fake union, merging
        // them as needed
        AbstractResult[] res = new AbstractResult[sels.length];
        List[] orderIdxs = null;
        try {
            List l;
            for (int i = 0; i < res.length; i++) {
                res[i] = (AbstractResult) sels[i].execute(store, fetch,
                    lockLevel);
                res[i].setBaseMapping(mappings[i]);
                res[i].setIndexOf(i);

                l = sels[i].getSelectedOrderIndexes();
                if (l != null) {
                    if (orderIdxs == null)
                        orderIdxs = new List[sels.length];
                    orderIdxs[i] = l;
                }
            }
        } catch (SQLException se) {
            for (int i = 0; res[i] != null; i++)
                res[i].close();
            throw se;
        }

        // if multiple selects have ordering, use a comparator to collate
        ResultComparator comp = null;
        if (orderIdxs != null)
            comp = new ResultComparator(orderIdxs, desc, dict);
        return new MergedResult(res, comp);
    }

    @Override
    public void select(Union.Selector selector) {
        for (int i = 0; i < sels.length; i++)
            selector.select(sels[i], i);
    }

    @Override
    public String toString() {
        return toSelect(false, null).getSQL();
    }

    /**
     * A callback used to create the selects in a SQL union.
     */
    public interface Selector {

        /**
         * Populate the <code>i</code>th select in the union.
         */
        void select(Select sel, int i);
    }

    /**
     * A select that is part of a logical union.
     */
    public class UnionSelect
        implements Select {

        protected final SelectImpl sel;
        protected final int pos;
        protected int orders = 0;
        protected List orderIdxs = null;

        public UnionSelect(SelectImpl sel, int pos) {
            this.sel = sel;
            this.pos = pos;
            sel.setRecordOrderedIndexes(true);
        }

        /**
         * Delegate select.
         */
        public SelectImpl getDelegate() {
            return sel;
        }

        /**
         * Return the indexes of the data in the select clause this query is
         * ordered by.
         */
        public List getSelectedOrderIndexes() {
            if (orderIdxs == null)
                orderIdxs = sel.getOrderedIndexes();
            return orderIdxs;
        }

        @Override
        public JDBCConfiguration getConfiguration() {
            return sel.getConfiguration();
        }

        @Override
        public int indexOf() {
            return pos;
        }

        @Override
        public SQLBuffer toSelect(boolean forUpdate,
            JDBCFetchConfiguration fetch) {
            return sel.toSelect(forUpdate, fetch);
        }

        @Override
        public SQLBuffer getSQL() {
            return sel.getSQL();
        }

        @Override
        public SQLBuffer toSelectCount() {
            return sel.toSelectCount();
        }

        @Override
        public boolean getAutoDistinct() {
            return sel.getAutoDistinct();
        }

        @Override
        public void setAutoDistinct(boolean distinct) {
            sel.setAutoDistinct(distinct);
        }

        @Override
        public boolean isDistinct() {
            return sel.isDistinct();
        }

        @Override
        public void setDistinct(boolean distinct) {
            sel.setDistinct(distinct);
        }

        @Override
        public boolean isLRS() {
            return sel.isLRS();
        }

        @Override
        public void setLRS(boolean lrs) {
            sel.setLRS(lrs);
        }

        @Override
        public int getJoinSyntax() {
            return sel.getJoinSyntax();
        }

        @Override
        public void setJoinSyntax(int joinSyntax) {
            sel.setJoinSyntax(joinSyntax);
        }

        @Override
        public boolean supportsRandomAccess(boolean forUpdate) {
            return sel.supportsRandomAccess(forUpdate);
        }

        @Override
        public boolean supportsLocking() {
            return sel.supportsLocking();
        }

        @Override
        public boolean hasMultipleSelects() {
            return sel.hasMultipleSelects();
        }

        @Override
        public int getCount(JDBCStore store)
            throws SQLException {
            return sel.getCount(store);
        }

        @Override
        public Result execute(JDBCStore store, JDBCFetchConfiguration fetch)
            throws SQLException {
            return sel.execute(store, fetch);
        }

        @Override
        public Result execute(JDBCStore store, JDBCFetchConfiguration fetch,
            int lockLevel)
            throws SQLException {
            return sel.execute(store, fetch, lockLevel);
        }

        @Override
        public List getSubselects() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public Select getParent() {
            return null;
        }

        @Override
        public String getSubselectPath() {
            return null;
        }

        @Override
        public void setParent(Select parent, String path) {
            throw new UnsupportedException(_loc.get("union-element"));
        }

        @Override
        public void setHasSubselect(boolean hasSub) {
            sel.setHasSubselect(hasSub);
        }

        @Override
        public boolean getHasSubselect() {
            return sel.getHasSubselect();
        }

        @Override
        public Select getFromSelect() {
            return null;
        }

        @Override
        public void setFromSelect(Select sel) {
            throw new UnsupportedException(_loc.get("union-element"));
        }

        @Override
        public boolean hasEagerJoin(boolean toMany) {
            return sel.hasEagerJoin(toMany);
        }

        @Override
        public boolean hasJoin(boolean toMany) {
            return sel.hasJoin(toMany);
        }

        @Override
        public boolean isSelected(Table table) {
            return sel.isSelected(table);
        }

        @Override
        public Collection getTableAliases() {
            return sel.getTableAliases();
        }

        @Override
        public List getSelects() {
            return sel.getSelects();
        }

        @Override
        public List getSelectAliases() {
            return sel.getSelectAliases();
        }

        @Override
        public List getIdentifierAliases() {
            return sel.getIdentifierAliases();
        }

        @Override
        public SQLBuffer getOrdering() {
            return sel.getOrdering();
        }

        @Override
        public SQLBuffer getGrouping() {
            return sel.getGrouping();
        }

        @Override
        public SQLBuffer getWhere() {
            return sel.getWhere();
        }

        @Override
        public SQLBuffer getHaving() {
            return sel.getHaving();
        }

        @Override
        public void addJoinClassConditions() {
            sel.addJoinClassConditions();
        }

        @Override
        public Joins getJoins() {
            return sel.getJoins();
        }

        @Override
        public Iterator getJoinIterator() {
            return sel.getJoinIterator();
        }

        @Override
        public long getStartIndex() {
            return sel.getStartIndex();
        }

        @Override
        public long getEndIndex() {
            return sel.getEndIndex();
        }

        @Override
        public void setRange(long start, long end) {
            sel.setRange(start, end);
        }

        @Override
        public String getColumnAlias(Column col) {
            return sel.getColumnAlias(col);
        }

        @Override
        public String getColumnAlias(Column col, Joins joins) {
            return sel.getColumnAlias(col, joins);
        }

        @Override
        public String getColumnAlias(Column col, Object alias) {
            return sel.getColumnAlias(col, alias);
        }

        @Override
        public String getColumnAlias(String col, Table table) {
            return sel.getColumnAlias(col, table);
        }

        @Override
        public String getColumnAlias(String col, Table table, Joins joins) {
            return sel.getColumnAlias(col, table, joins);
        }

        @Override
        public boolean isAggregate() {
            return sel.isAggregate();
        }

        @Override
        public void setAggregate(boolean agg) {
            sel.setAggregate(agg);
        }

        @Override
        public boolean isLob() {
            return sel.isLob();
        }

        @Override
        public void setLob(boolean lob) {
            sel.setLob(lob);
        }

        @Override
        public void selectPlaceholder(String sql) {
            sel.selectPlaceholder(sql);
        }

        @Override
        public void clearSelects() {
            sel.clearSelects();
        }

        @Override
        public boolean select(SQLBuffer sql, Object id) {
            return sel.select(sql, id);
        }

        @Override
        public boolean select(SQLBuffer sql, Object id, Joins joins) {
            return sel.select(sql, id, joins);
        }

        @Override
        public boolean select(String sql, Object id) {
            return sel.select(sql, id);
        }

        @Override
        public boolean select(String sql, Object id, Joins joins) {
            return sel.select(sql, id, joins);
        }

        @Override
        public boolean select(Column col) {
            return sel.select(col);
        }

        @Override
        public boolean select(Column col, Joins joins) {
            return sel.select(col, joins);
        }

        @Override
        public int select(Column[] cols) {
            return sel.select(cols);
        }

        @Override
        public int select(Column[] cols, Joins joins) {
            return sel.select(cols, joins);
        }

        @Override
        public void select(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch, int eager) {
            select(mapping, subclasses, store, fetch, eager, null, false);
        }

        @Override
        public void select(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch, int eager,
            Joins joins) {
            select(mapping, subclasses, store, fetch, eager, joins, false);
        }

        private void select(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch, int eager,
            Joins joins, boolean identifier) {
            // if this is the first (primary) mapping selected for this
            // SELECT, record it so we can figure out what the result type is
            // since the discriminator might not be selected
            if (mappings[pos] == null)
                mappings[pos] = mapping;

            sel.select(this, mapping, subclasses, store, fetch, eager,
                joins, identifier);
        }

        @Override
        public boolean selectIdentifier(Column col) {
            return sel.selectIdentifier(col);
        }

        @Override
        public boolean selectIdentifier(Column col, Joins joins) {
            return sel.selectIdentifier(col, joins);
        }

        @Override
        public int selectIdentifier(Column[] cols) {
            return sel.selectIdentifier(cols);
        }

        @Override
        public int selectIdentifier(Column[] cols, Joins joins) {
            return sel.selectIdentifier(cols, joins);
        }

        @Override
        public void selectIdentifier(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch, int eager) {
            select(mapping, subclasses, store, fetch, eager, null, true);
        }

        @Override
        public void selectIdentifier(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch, int eager,
            Joins joins) {
            select(mapping, subclasses, store, fetch, eager, joins, true);
        }

        @Override
        public int selectPrimaryKey(ClassMapping mapping) {
            return sel.selectPrimaryKey(mapping);
        }

        @Override
        public int selectPrimaryKey(ClassMapping mapping, Joins joins) {
            return sel.selectPrimaryKey(mapping, joins);
        }

        @Override
        public int orderByPrimaryKey(ClassMapping mapping, boolean asc,
            boolean select) {
            return orderByPrimaryKey(mapping, asc, null, select);
        }

        @Override
        public int orderByPrimaryKey(ClassMapping mapping, boolean asc,
            Joins joins, boolean select) {
            ClassMapping pks = mapping;
            while (!pks.isPrimaryKeyObjectId(true))
                pks = pks.getJoinablePCSuperclassMapping();
            Column[] cols = pks.getPrimaryKeyColumns();
            recordOrderColumns(cols, asc);
            return sel.orderByPrimaryKey(mapping, asc, joins, select,
                isUnion());
        }

        /**
         * Record that we're ordering by a SQL expression.
         */
        protected void recordOrder(Object ord, boolean asc) {
            if (ord == null)
                return;
            orderIdxs = null;

            int idx = orders++;
            if (desc.get(idx) && asc)
                throw new UserException(_loc.get("incompat-ordering"));
            if (!asc)
                desc.set(idx);
        }

        /**
         * Record that we're ordering by the given columns.
         */
        protected void recordOrderColumns(Column[] cols, boolean asc) {
            for (int i = 0; i < cols.length; i++)
                recordOrder(cols[i], asc);
        }

        @Override
        public boolean orderBy(Column col, boolean asc, boolean select) {
            return orderBy(col, asc, null, select);
        }

        @Override
        public boolean orderBy(Column col, boolean asc, Joins joins,
            boolean select) {
            recordOrder(col, asc);
            return sel.orderBy(col, asc, joins, select, isUnion());
        }

        @Override
        public int orderBy(Column[] cols, boolean asc, boolean select) {
            return orderBy(cols, asc, null, select);
        }

        @Override
        public int orderBy(Column[] cols, boolean asc, Joins joins,
            boolean select) {
            recordOrderColumns(cols, asc);
            return sel.orderBy(cols, asc, joins, select, isUnion());
        }

        @Override
        public boolean orderBy(SQLBuffer sql, boolean asc, boolean select,
            Value selAs) {
            return orderBy(sql, asc, null, select, selAs);
        }

        @Override
        public boolean orderBy(SQLBuffer sql, boolean asc, Joins joins,
            boolean select, Value selAs) {
            recordOrder(sql.getSQL(false), asc);
            return sel.orderBy(sql, asc, joins, select, isUnion(), selAs);
        }

        @Override
        public boolean orderBy(String sql, boolean asc, boolean select) {
            return orderBy(sql, asc, null, select);
        }

        @Override
        public boolean orderBy(String sql, boolean asc, Joins joins,
            boolean select) {
            recordOrder(sql, asc);
            return sel.orderBy(sql, asc, joins, select, isUnion());
        }

        @Override
        public void clearOrdering() {
            sel.clearOrdering();
        }

        @Override
        public void wherePrimaryKey(Object oid, ClassMapping mapping,
            JDBCStore store) {
            sel.wherePrimaryKey(oid, mapping, store);
        }

        @Override
        public void whereForeignKey(ForeignKey fk, Object oid,
            ClassMapping mapping, JDBCStore store) {
            sel.whereForeignKey(fk, oid, mapping, store);
        }

        @Override
        public void where(Joins joins) {
            sel.where(joins);
        }

        @Override
        public void where(SQLBuffer sql) {
            sel.where(sql);
        }

        @Override
        public void where(SQLBuffer sql, Joins joins) {
            sel.where(sql, joins);
        }

        @Override
        public void where(String sql) {
            sel.where(sql);
        }

        @Override
        public void where(String sql, Joins joins) {
            sel.where(sql, joins);
        }

        @Override
        public void having(SQLBuffer sql) {
            sel.having(sql);
        }

        @Override
        public void having(SQLBuffer sql, Joins joins) {
            sel.having(sql, joins);
        }

        @Override
        public void having(String sql) {
            sel.having(sql);
        }

        @Override
        public void having(String sql, Joins joins) {
            sel.having(sql, joins);
        }

        @Override
        public void groupBy(SQLBuffer sql) {
            sel.groupBy(sql);
        }

        @Override
        public void groupBy(SQLBuffer sql, Joins joins) {
            sel.groupBy(sql, joins);
        }

        @Override
        public void groupBy(String sql) {
            sel.groupBy(sql);
        }

        @Override
        public void groupBy(String sql, Joins joins) {
            sel.groupBy(sql, joins);
        }

        @Override
        public void groupBy(Column col) {
            sel.groupBy(col);
        }

        @Override
        public void groupBy(Column col, Joins joins) {
            sel.groupBy(col, joins);
        }

        @Override
        public void groupBy(Column[] cols) {
            sel.groupBy(cols);
        }

        @Override
        public void groupBy(Column[] cols, Joins joins) {
            sel.groupBy(cols, joins);
        }

        @Override
        public void groupBy(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch) {
            sel.groupBy(mapping, subclasses, store, fetch);
        }

        @Override
        public void groupBy(ClassMapping mapping, int subclasses,
            JDBCStore store, JDBCFetchConfiguration fetch, Joins joins) {
            sel.groupBy(mapping, subclasses, store, fetch, joins);
        }

        @Override
        public SelectExecutor whereClone(int sels) {
            return sel.whereClone(sels);
        }

        @Override
        public SelectExecutor fullClone(int sels) {
            return sel.fullClone(sels);
        }

        @Override
        public SelectExecutor eagerClone(FieldMapping key, int eagerType,
            boolean toMany, int sels) {
            SelectExecutor ex = sel.eagerClone(key, eagerType, toMany, sels);
            return (ex == sel) ? this : ex;
        }

        @Override
        public SelectExecutor getEager(FieldMapping key) {
            SelectExecutor ex = sel.getEager(key);
            return (ex == sel) ? this : ex;
        }

        @Override
        public Joins newJoins() {
            return sel.newJoins();
        }

        @Override
        public Joins newOuterJoins() {
            return sel.newOuterJoins();
        }

        @Override
        public void append(SQLBuffer buf, Joins joins) {
            sel.append(buf, joins);
        }

        @Override
        public Joins and(Joins joins1, Joins joins2) {
            return sel.and(joins1, joins2);
        }

        @Override
        public Joins or(Joins joins1, Joins joins2) {
            return sel.or(joins1, joins2);
        }

        @Override
        public Joins outer(Joins joins) {
            return sel.outer(joins);
        }

        @Override
        public String toString() {
            return sel.toString();
        }

        @Override
        public int getExpectedResultCount() {
            return sel.getExpectedResultCount();
        }

        @Override
        public void setExpectedResultCount(int expectedResultCount,
            boolean force) {
            sel.setExpectedResultCount(expectedResultCount, force);
        }

        @Override
        public void setContext(Context context) {
            sel.setContext(context);
        }

        @Override
        public Context ctx() {
            return sel.ctx();
        }

        @Override
        public void setSchemaAlias(String schemaAlias) {
            sel.setSchemaAlias(schemaAlias);
        }

        @Override
        public void logEagerRelations() {
            sel.logEagerRelations();
        }
        @Override
        public void setTablePerClassMeta(ClassMapping meta) {
        }

        @Override
        public ClassMapping getTablePerClassMeta() {
            return sel.getTablePerClassMeta();
        }

        @Override
        public void setJoinedTableClassMeta(List meta) {
            sel.setJoinedTableClassMeta(meta);
        }

        @Override
        public List getJoinedTableClassMeta() {
            return sel.getJoinedTableClassMeta();
        }

        @Override
        public void setExcludedJoinedTableClassMeta(List meta) {
            sel.setExcludedJoinedTableClassMeta(meta);
        }

        @Override
        public List getExcludedJoinedTableClassMeta() {
            return sel.getExcludedJoinedTableClassMeta();
        }

        @Override
        public DBDictionary getDictionary() {
            return dict;
        }
    }

    /**
     * Comparator for collating ordered results when faking a union.
     */
    private static class ResultComparator
        implements MergedResult.ResultComparator {

        private final List[] _orders;
        private final BitSet _desc;
        private final DBDictionary _dict;

        public ResultComparator(List[] orders, BitSet desc, DBDictionary dict) {
            _orders = orders;
            _desc = desc;
            _dict = dict;
        }

        @Override
        public Object getOrderingValue(Result res, int idx) {
            // if one value just return it
            ResultSet rs = ((ResultSetResult) res).getResultSet();
            if (_orders[idx].size() == 1)
                return getOrderingValue(rs, _orders[idx].get(0));

            // return array of values
            Object[] vals = new Object[_orders[idx].size()];
            for (int i = 0; i < vals.length; i++)
                vals[i] = getOrderingValue(rs, _orders[idx].get(i));
            return vals;
        }

        /**
         * Extract value at given index from result set.
         */
        private Object getOrderingValue(ResultSet rs, Object i) {
            try {
                return _dict.getObject(rs, (Integer) i + 1, null);
            } catch (SQLException se) {
                throw SQLExceptions.getStore(se, _dict);
            }
        }

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            if (o1 == null)
                return (_desc.get(0)) ? -1 : 1;
            if (o2 == null)
                return (_desc.get(0)) ? 1 : -1;

            int cmp;
            if (!(o1 instanceof Object[])) {
                if (!(o2 instanceof Object[])) {
                    cmp = ((Comparable) o1).compareTo(o2);
                    return (_desc.get(0)) ? -cmp : cmp;
                }

                cmp = ((Comparable) o1).compareTo(((Object[]) o2)[0]);
                if (cmp != 0)
                    return (_desc.get(0)) ? -cmp : cmp;
                return -1;
            }

            if (!(o2 instanceof Object[])) {
                cmp = ((Comparable) ((Object[]) o1)[0]).compareTo(o2);
                if (cmp != 0)
                    return (_desc.get(0)) ? -cmp : cmp;
                return 1;
            }

            Object[] a1 = (Object[]) o1;
            Object[] a2 = (Object[]) o2;
            for (int i = 0; i < a1.length; i++) {
                cmp = ((Comparable) a1[i]).compareTo(a2[i]);
                if (cmp != 0)
                    return (_desc.get(i)) ? -cmp : cmp;
            }
            return a1.length - a2.length;
        }
    }
}
