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
package org.apache.openjpa.jdbc.kernel.exps;

import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.Expression;
import org.apache.openjpa.kernel.exps.QueryExpressions;

/**
 * Turns parsed queries into selects.
 *
 * @author Abe White
 */
class SelectConstructor {

    public static final int CACHE_NULL = 0;
    public static final int CACHE_JOINS = 1;
    public static final int CACHE_FULL = 2;

    // cache as much as we can for multiple executions of the same query
    private Select _template = null;
    private boolean _extent = false;
    private int _cacheLevel = -1;

    /**
     * Return true if we know the select to have on criteria; to be an extent.
     * Note that even if this method returns false, {@link #evaluate} may still
     * return null if we haven't cached whether the query is an extent yet.
     */
    public boolean isExtent() {
        return _extent;
    }

    /**
     * Evaluate the expression, returning a SQL select with the proper
     * conditions. Use {@link #select} to then select the data.
     */
    public Select evaluate(JDBCStore store, Select parent, String alias,
        QueryExpressions exps, Object[] params, int level,
        JDBCFetchConfiguration fetch) {
        // already know that this query is equivalent to an extent?
        Select sel;
        if (_extent) {
            sel = store.getSQLFactory().newSelect();
            sel.setAutoDistinct((exps.distinct & exps.DISTINCT_AUTO) != 0);
            return sel;
        }

        // already cached some SQL? if we're changing our cache level, we
        // have to abandon any already-cached data because a change means
        // different joins
        if (level != _cacheLevel)
            _template = null;
        _cacheLevel = level;

        if (_template != null && level == CACHE_FULL) {
            sel = (Select) _template.fullClone(1);
            sel.setParent(parent, alias);
        } else if (_template != null) {
            sel = (Select) _template.whereClone(1);
            sel.setParent(parent, alias);
        } else {
            // create a new select and initialize it with the joins needed for
            // the criteria of this query
            sel = newJoinsSelect(store, parent, alias, exps, params, fetch);
        }

        // if this select wasn't cloned from a full template,
        // build up sql conditions
        if (_template == null || level != CACHE_FULL) {
            // create where clause; if there are no where conditions and
            // no ordering or projections, we return null to signify that this
            // query should be treated like an extent
            Select inner = sel.getFromSelect();
            SQLBuffer where = buildWhere((inner != null) ? inner : sel,
                store, exps.filter, params, fetch);
            if (where == null && exps.projections.length == 0
                && exps.ordering.length == 0
                && (sel.getJoins() == null || sel.getJoins().isEmpty())) {
                _extent = true;
                sel = store.getSQLFactory().newSelect();
                sel.setAutoDistinct((exps.distinct & exps.DISTINCT_AUTO) != 0);
                return sel;
            }

            // if we're caching joins, do that now before we start setting sql.
            // we can't cache subselects because they are also held in the
            // where buffer
            if (_template == null && level == CACHE_JOINS
                && (inner == null || inner.getSubselects().isEmpty())
                && sel.getSubselects().isEmpty()) {
                _template = sel;
                sel = (Select) sel.whereClone(1);
                sel.setParent(parent, alias);
                inner = sel.getFromSelect();
            }

            // now set sql criteria; it goes on the inner select if present
            if (inner != null)
                inner.where(where);
            else
                sel.where(where);

            // apply grouping and having.  this does not select the grouping
            // columns, just builds the GROUP BY clauses.  we don't build the
            // ORDER BY clauses yet because if we decide to add this select
            // to a union, the ORDER BY values get aliased differently
            if (exps.having != null) {
                Exp havingExp = (Exp) exps.having;
                SQLBuffer buf = new SQLBuffer(store.getDBDictionary());
                havingExp.appendTo(buf, sel, store, params, fetch);
                sel.having(buf);
            }
            for (int i = 0; i < exps.grouping.length; i++)
                ((Val) exps.grouping[i]).groupBy(sel, store, params, fetch);

            // if template is still null at this point, must be a full cache
            if (_template == null && level == CACHE_FULL) {
                _template = sel;
                sel = (Select) _template.fullClone(1);
                sel.setParent(parent, alias);
            }
        }
        return sel;
    }

    /**
     * Initialize the given select's joins.
     */
    private Select newJoinsSelect(JDBCStore store, Select parent,
        String alias, QueryExpressions exps, Object[] params,
        JDBCFetchConfiguration fetch) {
        Select sel = store.getSQLFactory().newSelect();
        sel.setAutoDistinct((exps.distinct & exps.DISTINCT_AUTO) != 0);
        sel.setJoinSyntax(fetch.getJoinSyntax());
        sel.setParent(parent, alias);
        initializeJoins(sel, store, exps, params);

        if (!sel.getAutoDistinct()) {
            if ((exps.distinct & exps.DISTINCT_TRUE) != 0)
                sel.setDistinct(true);
            else if ((exps.distinct & exps.DISTINCT_FALSE) != 0)
                sel.setDistinct(false);
        } else if (exps.projections.length > 0) {
            if (!sel.isDistinct() && (exps.distinct & exps.DISTINCT_TRUE) != 0)
            {
                // if the select is not distinct but the query is, force
                // the select to be distinct
                sel.setDistinct(true);
            } else if (sel.isDistinct()) {
                // when aggregating data or making a non-distinct projection
                // from a distinct select, we have to select from a tmp
                // table formed by a distinct subselect in the from clause;
                // this subselect selects the pks of the candidate (to
                // get unique candidate values) and needed field values and
                // applies the where conditions; the outer select applies
                // ordering, grouping, etc
                if (exps.aggregate || (exps.distinct & exps.DISTINCT_TRUE) == 0)
                {
                    DBDictionary dict = store.getDBDictionary();
                    dict.assertSupport(dict.supportsSubselect,
                        "SupportsSubselect");

                    Select inner = sel;
                    sel = store.getSQLFactory().newSelect();
                    sel.setParent(parent, alias);
                    sel.setDistinct(exps.aggregate
                        && (exps.distinct & exps.DISTINCT_TRUE) != 0);
                    sel.setFromSelect(inner);
                }
            }
        }
        return sel;
    }

    /**
     * Initialize the joins for all expressions. This only has to be done
     * once for the template select, since each factory is only used for a
     * single filter + projections + grouping + having + ordering combination.
     * By initializing the joins once, we speed up subsequent executions
     * because the relation traversal logic, etc is cached.
     */
    private void initializeJoins(Select sel, JDBCStore store,
        QueryExpressions exps, Object[] params) {
        Map contains = null;
        if (((Exp) exps.filter).hasContainsExpression()
            || (exps.having != null
            && ((Exp) exps.having).hasContainsExpression()))
            contains = new HashMap(7);

        // initialize filter and having expressions
        Exp filterExp = (Exp) exps.filter;
        filterExp.initialize(sel, store, params, contains);
        Exp havingExp = (Exp) exps.having;
        if (havingExp != null)
            havingExp.initialize(sel, store, params, contains);

        // get the top-level joins and null the expression's joins
        // at the same time so they aren't included in the where/having SQL
        Joins filterJoins = filterExp.getJoins();
        Joins havingJoins = (havingExp == null) ? null : havingExp.getJoins();
        Joins joins = sel.and(filterJoins, havingJoins);

        // initialize result values
        Val resultVal;
        for (int i = 0; i < exps.projections.length; i++) {
            resultVal = (Val) exps.projections[i];
            resultVal.initialize(sel, store, false);

            // have to join through to related type for pc object projections;
            // this ensures that we have all our joins cached
            if (resultVal instanceof PCPath)
                ((PCPath) resultVal).joinRelation();

            joins = sel.and(joins, resultVal.getJoins());
        }

        // initialize grouping
        Val groupVal;
        for (int i = 0; i < exps.grouping.length; i++) {
            groupVal = (Val) exps.grouping[i];
            groupVal.initialize(sel, store, false);
            joins = sel.and(joins, groupVal.getJoins());
        }

        // initialize ordering
        Val orderVal;
        for (int i = 0; i < exps.ordering.length; i++) {
            orderVal = (Val) exps.ordering[i];
            orderVal.initialize(sel, store, false);
            joins = sel.and(joins, orderVal.getJoins());
        }

        sel.where(joins);
    }

    /**
     * Create the where sql.
     */
    private SQLBuffer buildWhere(Select sel, JDBCStore store,
        Expression filter, Object[] params, JDBCFetchConfiguration fetch) {
        // create where buffer
        SQLBuffer where = new SQLBuffer(store.getDBDictionary());
        where.append("(");
        Exp filterExp = (Exp) filter;
        filterExp.appendTo(where, sel, store, params, fetch);

        if (where.sqlEquals("(") || where.sqlEquals("(1 = 1"))
            return null;
        return where.append(")");
    }

    /**
     * Select the data for this query.
     */
    public void select(JDBCStore store, ClassMapping mapping,
        boolean subclasses, Select sel, QueryExpressions exps,
        Object[] params, JDBCFetchConfiguration fetch, int eager) {
        Select inner = sel.getFromSelect();
        Val val;
        Joins joins = null;
        if (sel.getSubselectPath() != null)
            joins = sel.newJoins().setSubselect(sel.getSubselectPath());

        // build ordering clauses before select so that any eager join
        // ordering gets applied after query ordering
        for (int i = 0; i < exps.ordering.length; i++)
            ((Val) exps.ordering[i]).orderBy(sel, store, params,
                exps.ascending[i], fetch);

        // if no result string set, select matching objects like normal
        if (exps.projections.length == 0 && sel.getParent() == null) {
            int subs = (subclasses) ? sel.SUBS_JOINABLE : sel.SUBS_NONE;
            sel.selectIdentifier(mapping, subs, store, fetch, eager);
        } else if (exps.projections.length == 0) {
            // subselect for objects; we really just need the primary key values
            sel.select(mapping.getPrimaryKeyColumns(), joins);
        } else {
            // if we have an inner select, we need to select the candidate
            // class' pk columns to guarantee unique instances
            if (inner != null)
                inner.select(mapping.getPrimaryKeyColumns(), joins);

            // select each result value; no need to pass on the eager mode since
            // under projections we always use EAGER_NONE
            boolean pks = sel.getParent() != null;
            for (int i = 0; i < exps.projections.length; i++) {
                val = (Val) exps.projections[i];
                if (inner != null)
                    val.selectColumns(inner, store, params, pks, fetch);
                val.select(sel, store, params, pks, fetch);
            }

            // make sure grouping and having columns are selected since it
            // is required by most DBs.  put them last so they don't affect
            // result processing
            if (exps.having != null && inner != null)
                ((Exp) exps.having).selectColumns(inner, store, params, true,
                    fetch);
            for (int i = 0; i < exps.grouping.length; i++) {
                val = (Val) exps.grouping[i];
                if (inner != null)
                    val.selectColumns(inner, store, params, true, fetch);
                val.select(sel, store, params, true, fetch);
            }
        }

        // select order data last so it doesn't affect result processing
        for (int i = 0; i < exps.ordering.length; i++) {
            val = (Val) exps.ordering[i];
            if (inner != null)
                val.selectColumns(inner, store, params, true, fetch);
            val.select(sel, store, params, true, fetch);
        }

        // add conditions limiting the projections to the proper classes; if
        // this isn't a projection then they will already be added
        if (exps.projections.length > 0) {
            Select indSel = (inner == null) ? sel : inner;
            store.addClassConditions(indSel, mapping, subclasses, joins);
        }
    }
}
