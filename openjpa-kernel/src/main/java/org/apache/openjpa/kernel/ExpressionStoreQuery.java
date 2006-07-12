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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.exps.InMemoryExpressionFactory;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Resolver;
import org.apache.openjpa.kernel.exps.StringContains;
import org.apache.openjpa.kernel.exps.Val;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.kernel.exps.WildcardMatch;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;

/**
 * <p>Implementation of an expression-based query, which can handle
 * String-based query expressions such as JPQL and JDOQL.
 * This implementation is suitable for in-memory operation.
 * Override the following methods to also support
 * datastore operation:
 * <ul>
 * <li>Override {@link #supportsDataStoreExecution} to return
 * <code>true</code>.</li>
 * <li>Override {@link #executeQuery}, {@link #executeDelete}, and
 * {@link #executeUpdate} to execute the query against the data store.
 * Keep in mind that the parameters passed to this method might be in use
 * by several threads in different query instances.  Thus components like
 * the expression factory must either be thread safe, or this method must
 * synchronize on them.</li>
 * <li>Override {@link #getDataStoreActions} to return a representation of
 * the actions that will be taken on the data store.  For use in visual
 * tools.</li>
 * <li>Override {@link #getExpressionFactory} to return a factory for creating
 * expressions in the datastore's language.  The factory must be
 * cachable.</li>
 * </ul></p>
 *
 * @author Abe White
 */
public class ExpressionStoreQuery
    extends AbstractStoreQuery {

    private static final Localizer _loc = Localizer.forPackage
        (ExpressionStoreQuery.class);

    // maintain support for a couple of deprecated extensions
    private static final FilterListener[] _listeners = new FilterListener[]{
        new StringContains(), new WildcardMatch(),
    };

    private final ExpressionParser _parser;
    private transient Object _parsed;

    /**
     * Construct a query with a parser for the language.
     */
    public ExpressionStoreQuery(ExpressionParser parser) {
        _parser = parser;
    }

    /**
     * Resolver used in parsing.
     */
    public Resolver getResolver() {
        return new Resolver() {
            public Class classForName(String name, String[] imports) {
                return ctx.classForName(name, imports);
            }

            public FilterListener getFilterListener(String tag) {
                return ctx.getFilterListener(tag);
            }

            public AggregateListener getAggregateListener(String tag) {
                return ctx.getAggregateListener(tag);
            }

            public OpenJPAConfiguration getConfiguration() {
                return ctx.getStoreContext().getConfiguration();
            }
        };
    }

    /**
     * Allow direct setting of parsed state for facades that do parsing.
     * The facade should call this method twice: once with the query string,
     * and again with the parsed state.
     */
    public boolean setQuery(Object query) {
        _parsed = query;
        return true;
    }

    public FilterListener getFilterListener(String tag) {
        for (int i = 0; i < _listeners.length; i++)
            if (_listeners[i].getTag().equals(tag))
                return _listeners[i];
        return null;
    }

    public Object newCompilation() {
        if (_parsed != null)
            return _parsed;
        return _parser.parse(ctx.getQueryString(), this);
    }

    public void populateFromCompilation(Object comp) {
        _parser.populate(comp, this);
    }

    public void invalidateCompilation() {
        _parsed = null;
    }

    public boolean supportsInMemoryExecution() {
        return true;
    }

    public Executor newInMemoryExecutor(ClassMetaData meta, boolean subs) {
        return new InMemoryExecutor(this, meta, subs, _parser,
            ctx.getCompilation());
    }

    public Executor newDataStoreExecutor(ClassMetaData meta, boolean subs) {
        return new DataStoreExecutor(this, meta, subs, _parser,
            ctx.getCompilation());
    }

    ////////////////////////
    // Methods for Override
    ////////////////////////

    /**
     * Execute the given expression against the given candidate extent.
     *
     * @param    ex            current executor
     * @param    base        the base type the query should match
     * @param    types        the independent candidate types
     * @param    subclasses    true if subclasses should be included in the
     * results
     * @param    facts        the expression factory used to build the query for
     * each base type
     * @param    parsed        the parsed query values
     * @param    params        parameter values, or empty array
     * @param    lrs            whether the result will be handled as a potentially
     * large result set, or will be consumed greedily
     * @param    startIdx    0-based inclusive index for first result to return
     * from result object provider
     * @param    endIdx        0-based exclusive index for last result to return
     * from result object provider, or
     * {@link Long#MAX_VALUE} for no max
     * @return a provider for matching objects
     */
    protected ResultObjectProvider executeQuery(Executor ex,
        ClassMetaData base, ClassMetaData[] types, boolean subclasses,
        ExpressionFactory[] facts, QueryExpressions[] parsed, Object[] params,
        boolean lrs, long startIdx, long endIdx) {
        throw new UnsupportedException();
    }

    /**
     * Execute the given expression against the given candidate extent
     * and delete the instances.
     *
     * @param    ex            current executor
     * @param    base        the base type the query should match
     * @param    types        the independent candidate types
     * @param    subclasses    true if subclasses should be included in the
     * results
     * @param    facts        the expression factory used to build the query for
     * each base type
     * @param    parsed        the parsed query values
     * @param    params        parameter values, or empty array
     * @return a number indicating the number of instances deleted,
     * or null to execute the delete in memory
     */
    protected Number executeDelete(Executor ex, ClassMetaData base,
        ClassMetaData[] types, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] parsed, Object[] params) {
        return null;
    }

    /**
     * Execute the given expression against the given candidate extent
     * and updates the instances.
     *
     * @param    ex            current executor
     * @param    base        the base type the query should match
     * @param    types        the independent candidate types
     * @param    subclasses    true if subclasses should be included in the
     * results
     * @param    facts        the expression factory used to build the query for
     * each base type
     * @param    parsed        the parsed query values
     * @param    params        parameter values, or empty array
     * @return a number indicating the number of instances updated,
     * or null to execute the update in memory.
     */
    protected Number executeUpdate(Executor ex, ClassMetaData base,
        ClassMetaData[] types, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] parsed, Object[] params) {
        return null;
    }

    /**
     * Return the commands that will be sent to the datastore in order
     * to execute the query, typically in the database's native language.
     *
     * @param    ex            current executor
     * @param    base        the base type the query should match
     * @param    types        the independent candidate types
     * @param    subclasses    true if subclasses should be included in the
     * results
     * @param    facts        the expression factory used to build the query for
     * each base type
     * @param    parsed        the parsed query values
     * @param    params        parameter values, or empty array
     * @param    startIdx    0-based inclusive index for first result to return
     * from result object provider
     * @param    endIdx        0-based exclusive index for last result to return
     * from result object provider, or
     * {@link Long#MAX_VALUE} for no max
     * @return a textual description of the query to execute
     */
    protected String[] getDataStoreActions(Executor ex, ClassMetaData base,
        ClassMetaData[] types, boolean subclasses, ExpressionFactory[] facts,
        QueryExpressions[] parsed, Object[] params, long startIdx,
        long endIdx) {
        return StoreQuery.EMPTY_STRINGS;
    }

    /**
     * Return the assignable types for the given metadata whose expression
     * trees must be compiled independently.
     */
    protected ClassMetaData[] getIndependentExpressionCandidates
        (ClassMetaData type, boolean subclasses) {
        return new ClassMetaData[]{ type };
    }

    /**
     * Return an {@link ExpressionFactory} to use to create an expression to
     * be executed against an extent.  Each factory will be used to compile
     * one filter only.  The factory must be cachable.
     */
    protected ExpressionFactory getExpressionFactory(ClassMetaData type) {
        throw new UnsupportedException();
    }

    /**
     * Provides support for queries that hold query information
     * in a {@link QueryExpressions} instance.
     *
     * @author Marc Prud'hommeaux
     */
    private static abstract class AbstractExpressionExecutor
        extends AbstractExecutor
        implements Executor {

        abstract QueryExpressions[] getQueryExpressions();

        private QueryExpressions assertQueryExpression() {
            QueryExpressions[] exp = getQueryExpressions();
            if (exp == null || exp.length < 1)
                throw new InvalidStateException(_loc.get("no-expressions"));

            return exp[0];
        }

        /**
         * Throw proper exception if given value is a collection/map/array.
         */
        protected void assertNotContainer(Value val, StoreQuery q) {
            Class type;
            if (val instanceof Path) {
                FieldMetaData fmd = ((Path) val).last();
                type = (fmd == null) ? val.getType() : fmd.getDeclaredType();
            } else
                type = val.getType();

            switch (JavaTypes.getTypeCode(type)) {
                case JavaTypes.ARRAY:
                case JavaTypes.COLLECTION:
                case JavaTypes.MAP:
                    throw new UserException(_loc.get("container-projection",
                        q.getContext().getQueryString()));
            }
        }

        public final Class getResultClass(StoreQuery q) {
            return assertQueryExpression().resultClass;
        }

        public final boolean[] getAscending(StoreQuery q) {
            return assertQueryExpression().ascending;
        }

        public final String getAlias(StoreQuery q) {
            return assertQueryExpression().alias;
        }

        public final String[] getProjectionAliases(StoreQuery q) {
            return assertQueryExpression().projectionAliases;
        }

        public final int getOperation(StoreQuery q) {
            return assertQueryExpression().operation;
        }

        public final boolean isAggregate(StoreQuery q) {
            return assertQueryExpression().aggregate;
        }

        public final boolean hasGrouping(StoreQuery q) {
            return assertQueryExpression().grouping.length > 0;
        }

        public final LinkedMap getParameterTypes(StoreQuery q) {
            return assertQueryExpression().parameterTypes;
        }

        public final Map getUpdates(StoreQuery q) {
            return assertQueryExpression().updates;
        }

        public final ClassMetaData[] getAccessPathMetaDatas(StoreQuery q) {
            QueryExpressions[] exps = getQueryExpressions();
            if (exps.length == 1)
                return exps[0].accessPath;

            List metas = null;
            for (int i = 0; i < exps.length; i++)
                metas = Filters.addAccessPathMetaDatas(metas,
                    exps[i].accessPath);
            if (metas == null)
                return StoreQuery.EMPTY_METAS;
            return (ClassMetaData[]) metas.toArray
                (new ClassMetaData[metas.size()]);
        }

        public boolean isPacking(StoreQuery q) {
            return false;
        }
    }

    /**
     * Runs the expression query in memory.
     */
    private static class InMemoryExecutor
        extends AbstractExpressionExecutor
        implements Executor {

        private final ClassMetaData _meta;
        private final boolean _subs;
        private final InMemoryExpressionFactory _factory;
        private final QueryExpressions[] _exps;
        private final Class[] _projTypes;

        public InMemoryExecutor(ExpressionStoreQuery q,
            ClassMetaData candidate, boolean subclasses,
            ExpressionParser parser, Object parsed) {
            _meta = candidate;
            _subs = subclasses;
            _factory = new InMemoryExpressionFactory();

            _exps = new QueryExpressions[]{
                parser.eval(parsed, q, _factory, _meta)
            };
            if (_exps[0].projections.length == 0)
                _projTypes = StoreQuery.EMPTY_CLASSES;
            else {
                _projTypes = new Class[_exps[0].projections.length];
                for (int i = 0; i < _exps[0].projections.length; i++) {
                    _projTypes[i] = _exps[0].projections[i].getType();
                    assertNotContainer(_exps[0].projections[i], q);
                    assertNotVariable((Val) _exps[0].projections[i],
                        q.getContext());
                }
                for (int i = 0; i < _exps[0].grouping.length; i++)
                    assertNotVariable((Val) _exps[0].grouping[i],
                        q.getContext());
            }
        }

        QueryExpressions[] getQueryExpressions() {
            return _exps;
        }

        /**
         * We can't handle in-memory projections or grouping that uses
         * variables.
         */
        private static void assertNotVariable(Val val, QueryContext ctx) {
            // we can't handle in-mem results that use variables
            if (val.hasVariables())
                throw new UnsupportedException(_loc.get("inmem-agg-proj-var",
                    ctx.getCandidateType(), ctx.getQueryString()));
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Object[] params, boolean lrs, long startIdx, long endIdx) {
            // execute in memory for candidate collection;
            // also execute in memory for transactional extents
            Collection coll = q.getContext().getCandidateCollection();
            Iterator itr;
            if (coll != null)
                itr = coll.iterator();
            else
                itr = q.getContext().getStoreContext().
                    extentIterator(_meta.getDescribedType(), _subs,
                        q.getContext().getFetchConfiguration(),
                        q.getContext().getIgnoreChanges());

            // find matching objects
            List results = new ArrayList();
            StoreContext ctx = q.getContext().getStoreContext();
            try {
                Object obj;
                while (itr.hasNext()) {
                    obj = itr.next();
                    if (_factory.matches(_exps[0], _meta, _subs, obj, ctx,
                        params))
                        results.add(obj);
                }
            }
            finally {
                ImplHelper.close(itr);
            }

            // group results
            results = _factory.group(_exps[0], results, ctx, params);

            // apply having to filter groups
            if (_exps[0].having != null) {
                List matches = new ArrayList(results.size());
                Collection c;
                itr = results.iterator();
                while (itr.hasNext()) {
                    c = (Collection) itr.next();
                    if (_factory.matches(_exps[0], c, ctx, params))
                        matches.add(c);
                }
                results = matches;
            }

            // apply projections, order results, and filter duplicates
            results = _factory.project(_exps[0], results, ctx, params);
            results = _factory.order(_exps[0], results, ctx, params);
            results = _factory.distinct(_exps[0], coll == null, results);

            ResultObjectProvider rop = new ListResultObjectProvider(results);
            if (startIdx != 0 || endIdx != Long.MAX_VALUE)
                rop = new RangeResultObjectProvider(rop, startIdx, endIdx);
            return rop;
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Map params, boolean lrs, long startIdx, long endIdx) {
            return executeQuery(q, q.getContext().toParameterArray
                (getParameterTypes(q), params), lrs, startIdx, endIdx);
        }

        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            long startIdx, long endIdx) {
            // in memory queries have no datastore actions to perform
            return StoreQuery.EMPTY_STRINGS;
        }

        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int orderIndex) {
            // if this is a projection, then we have to order on something
            // we selected
            if (_exps[0].projections.length > 0) {
                String ordering = _exps[0].orderingClauses[orderIndex];
                for (int i = 0; i < _exps[0].projectionClauses.length; i++)
                    if (ordering.equals(_exps[0].projectionClauses[i]))
                        return ((Object[]) resultObject)[i];

                throw new InvalidStateException(_loc.get
                    ("merged-order-with-result", q.getContext().getLanguage(),
                        q.getContext().getQueryString(), ordering));
            }

            // use the parsed ordering expression to extract the ordering value
            Val val = (Val) _exps[0].ordering[orderIndex];
            return val.evaluate(resultObject, resultObject, q.getContext().
                getStoreContext(), params);
        }

        public Class[] getProjectionTypes(StoreQuery q) {
            return _projTypes;
        }
    }

    /**
     *  The DataStoreExecutor executes the query against the
     *  implementation's overridden {@link #executeQuery} method.
     *
     *  @author Marc Prud'hommeaux
     */
    private static class DataStoreExecutor
        extends AbstractExpressionExecutor
        implements Executor {

        private final ClassMetaData _meta;
        private final ClassMetaData[] _metas;
        private final boolean _subs;
        private final ExpressionParser _parser;
        private final ExpressionFactory[] _facts;
        private final QueryExpressions[] _exps;
        private Value[] _inMemOrdering;
        private Class[] _projTypes;

        public DataStoreExecutor(ExpressionStoreQuery q,
            ClassMetaData meta, boolean subclasses,
            ExpressionParser parser, Object parsed) {
            _meta = meta;
            _metas = q.getIndependentExpressionCandidates(meta, subclasses);
            _subs = subclasses;
            _parser = parser;

            _facts = new ExpressionFactory[_metas.length];
            for (int i = 0; i < _facts.length; i++)
                _facts[i] = q.getExpressionFactory(_metas[i]);

            _exps = new QueryExpressions[_metas.length];
            for (int i = 0; i < _exps.length; i++) {
                _exps[i] = parser.eval(parsed, q, _facts[i], _metas[i]);
                for (int j = 0; j < _exps[i].projections.length; j++)
                    assertNotContainer(_exps[i].projections[j], q);
            }
        }

        QueryExpressions[] getQueryExpressions() {
            return _exps;
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Object[] params, boolean lrs, long startIdx, long endIdx) {
            lrs = lrs && !isAggregate(q) && !hasGrouping(q);
            return ((ExpressionStoreQuery) q).executeQuery(this, _meta, _metas,
                _subs, _facts, _exps, params, lrs, startIdx, endIdx);
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Map params, boolean lrs, long startIdx, long endIdx) {
            return executeQuery(q, q.getContext().toParameterArray
                (getParameterTypes(q), params), lrs, startIdx, endIdx);
        }

        public Number executeDelete(StoreQuery q, Object[] params) {
            Number num = ((ExpressionStoreQuery) q).executeDelete(this, _meta,
                _metas, _subs, _facts, _exps, params);
            if (num == null)
                return q.getContext().deleteInMemory(this, params);
            return num;
        }

        public Number executeDelete(StoreQuery q, Map params) {
            return executeDelete(q, q.getContext().toParameterArray
                (getParameterTypes(q), params));
        }

        public Number executeUpdate(StoreQuery q, Object[] params) {
            Number num = ((ExpressionStoreQuery) q).executeUpdate(this, _meta,
                _metas, _subs, _facts, _exps, params);
            if (num == null)
                return q.getContext().updateInMemory(this, params);
            return num;
        }

        public Number executeUpdate(StoreQuery q, Map params) {
            return executeUpdate(q, q.getContext().toParameterArray
                (getParameterTypes(q), params));
        }

        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            long startIdx, long endIdx) {
            return ((ExpressionStoreQuery) q).getDataStoreActions(this, _meta,
                _metas, _subs, _facts, _exps, params, startIdx, endIdx);
        }

        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int orderIndex) {
            // if this is a projection, then we have to order on something
            // we selected
            if (_exps[0].projections.length > 0) {
                String ordering = _exps[0].orderingClauses[orderIndex];
                for (int i = 0; i < _exps[0].projectionClauses.length; i++)
                    if (ordering.equals(_exps[0].projectionClauses[i]))
                        return ((Object[]) resultObject)[i];

                throw new InvalidStateException(_loc.get
                    ("merged-order-with-result", q.getContext().getLanguage(),
                        q.getContext().getQueryString(), ordering));
            }

            // need to parse orderings?
            synchronized (this) {
                if (_inMemOrdering == null) {
                    ExpressionFactory factory = new InMemoryExpressionFactory();
                    _inMemOrdering = _parser.eval(_exps[0].orderingClauses,
                        (ExpressionStoreQuery) q, factory, _meta);
                }

                // use the parsed ordering expression to extract the ordering
                // value
                Val val = (Val) _inMemOrdering[orderIndex];
                return val.evaluate(resultObject, resultObject,
                    q.getContext().getStoreContext(), params);
            }
        }

        public Class[] getProjectionTypes(StoreQuery q) {
            if (_exps[0].projections.length == 0)
                return StoreQuery.EMPTY_CLASSES;

            synchronized (this) {
                if (_projTypes == null) {
                    // delay creating this array until it is requested b/c
                    // before execution the types might not be initialized
                    _projTypes = new Class[_exps[0].projections.length];
                    for (int i = 0; i < _exps[0].projections.length; i++)
                        _projTypes[i] = _exps[0].projections[i].getType();
				}
				return _projTypes;
			}
		}
	}
}
