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
package org.apache.openjpa.persistence.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Criteria query implementation.
 *
 * Collects clauses of criteria query (e.g. select projections, from/join,
 * where conditions, order by).
 * Eventually translates these clauses to a similar form of Expression tree
 * that can be interpreted and executed against a data store by OpenJPA kernel.
 *
 * @author Pinaki Poddar
 * @author Fay Wang
 *
 * @since 2.0.0
 */
class CriteriaQueryImpl<T> implements OpenJPACriteriaQuery<T>, AliasContext {
    private static final Localizer _loc = Localizer.forPackage(CriteriaQueryImpl.class);

    private final MetamodelImpl  _model;
    private Set<Root<?>>        _roots;
    private PredicateImpl       _where;
    private List<Order>         _orders;
    private OrderedMap<Object, Class<?>> _params = new OrderedMap<>();
    private Selection<? extends T> _selection;
    private List<Selection<?>>  _selections;
    private List<Expression<?>> _groups;
    private PredicateImpl       _having;
    private List<Subquery<?>>   _subqueries;
    private boolean             _distinct;
    private final SubqueryImpl<?> _delegator;
    private final Class<T>      _resultClass;
    private boolean             _compiled;

    // AliasContext
    private int aliasCount = 0;
    private static String ALIAS_BASE = "autoAlias";

    private Map<Selection<?>,Value> _variables = new HashMap<>();
    private Map<Selection<?>,Value> _values    = new HashMap<>();
    private Map<Selection<?>,String> _aliases  = null;
    private Map<Selection<?>,Value> _rootVariables = new HashMap<>();

    // SubqueryContext
    private ThreadLocal<Stack<Context>> _contexts = new  ThreadLocal<Stack<Context>>(){
        @Override
        protected Stack<Context> initialValue() {
            return new Stack<>();
        }
    };

    public CriteriaQueryImpl(MetamodelImpl model, Class<T> resultClass) {
        this._model = model;
        this._resultClass = resultClass;
        this._delegator = null;
        _aliases = new HashMap<>();
    }

    /**
     * Used by a subquery to delegate to this receiver.
     *
     * @param model the metamodel defines the scope of all persistent entity references.
     * @param delegator the subquery which will delegate to this receiver.
     */
    CriteriaQueryImpl(MetamodelImpl model, SubqueryImpl<T> delegator, OrderedMap<Object, Class<?>> params) {
        this._model = model;
        this._resultClass = delegator.getJavaType();
        _delegator = delegator;
        _aliases = getAliases();
        _params = params;
    }

    /**
     * Gets the subquery, if any, which is delegating to this receiver.
     */
    SubqueryImpl<?> getDelegator() {
        return _delegator;
    }

    /**
     * Gets the metamodel which defines the scope of all persistent entity references.
     */
    public MetamodelImpl getMetamodel() {
        return _model;
    }

    /**
     * Gets the stack of contexts used by this query.
     */
    Stack<Context> getContexts() {
        return _contexts.get();
    }

    /**
     * Sets whether this query as distinct.
     */
    @Override
    public CriteriaQuery<T> distinct(boolean distinct) {
        _distinct = distinct;
        return this;
    }

    /**
     * Gets the list of ordering elements.
     *
     * @return Empty list if there is no ordering elements.
     * The returned list if mutable but mutation has no impact on this query.
     */
    @Override
    public List<Order> getOrderList() {
        return Expressions.returnCopy(_orders);
    }

    /**
     * Return the selection of the query
     * @return the item to be returned in the query result
     */
    @Override
    public Selection<T> getSelection() {
        return (Selection<T>)_selection;
    }

    /**
     * Specify the items that are to be returned in the query result.
     * Replaces the previously specified selection(s), if any.
     *
     * The type of the result of the query execution depends on
     * the specification of the criteria query object as well as the
     * arguments to the multiselect method as follows:
     *
     * If the type of the criteria query is CriteriaQuery<Tuple>,
     * a Tuple object corresponding to the arguments of the
     * multiselect method will be instantiated and returned for
     * each row that results from the query execution.
     *
     * If the type of the criteria query is CriteriaQuery<X> for
     * some user-defined class X, then the arguments to the
     * multiselect method will be passed to the X constructor and
     * an instance of type X will be returned for each row.
     * The IllegalStateException will be thrown if a constructor
     * for the given argument types does not exist.
     *
     * If the type of the criteria query is CriteriaQuery<X[]> for
     * some class X, an instance of type X[] will be returned for
     * each row.  The elements of the array will correspond to the
     * arguments of the multiselect method.    The
     * IllegalStateException will be thrown if the arguments to the
     * multiselect method are not of type X.
     *
     * If the type of the criteria query is CriteriaQuery<Object>,
     * and only a single argument is passed to the multiselect
     * method, an instance of type Object will be returned for
     * each row.
     *
     * If the type of the criteria query is CriteriaQuery<Object>,
     * and more than one argument is passed to the multiselect
     * method, an instance of type Object[] will be instantiated
     * and returned for each row.  The elements of the array will
     * correspond to the arguments to the multiselect method.
     *
     * @param selections  expressions specifying the items that
     *        are returned in the query result
     * @return the modified query
     */
    @Override
    public CriteriaQuery<T> multiselect(Selection<?>... selections) {
        _selections = Arrays.asList(selections); // do not telescope
        _selection  = new CompoundSelections.MultiSelection(_resultClass, selections);
        return this;
    }

    /**
     * Registers the given parameter.
     */
    void registerParameter(ParameterExpressionImpl<?> p) {
        for (Object k : _params.keySet()) {
            if (p.paramEquals(k)) {
                // If a named ParameterExpressin did already get registered
                // with that exact name, then we do ignore it.
                // If we do a query.setParameter("someParamName", Bla)
                // then it must uniquely identify a Parameter.
                return;
            }
        }

        p.setIndex(_params.size());
        _params.put(p, p.getJavaType());
    }

    @Override
    public Set<ParameterExpression<?>> getParameters() {
        collectParameters(new CriteriaExpressionVisitor.ParameterVisitor(this));
        return (Set) _params.keySet();
    }

    /**
     * Return the selection items of the query as a list
     * @return the selection items of the query as a list
     */
    public List<Selection<?>> getSelectionList() {
        return Expressions.returnCopy(_selections);
    }

    @Override
    public CriteriaQuery<T> groupBy(Expression<?>... grouping) {
        if (grouping == null) {
            _groups = null;
            return this;
        }
        _groups = new ArrayList<>();
        for (Expression<?> e : grouping) {
            _groups.add(e);
        }
        return this;
    }

    @Override
    public CriteriaQuery<T> groupBy(List<Expression<?>> grouping) {
        if (grouping == null) {
            _groups = null;
            return this;
        }

        _groups = new ArrayList<>();
        for (Expression<?> e : grouping) {
            _groups.add(e);
        }
        return this;
    }


    @Override
    public CriteriaQuery<T> having(Expression<Boolean> restriction) {
        _having = (PredicateImpl)restriction;
        return this;
    }

    @Override
    public CriteriaQuery<T> having(Predicate... restrictions) {
        if (restrictions == null) {
            _having = null;
            return this;
        }
        _having = new PredicateImpl.And();
        for (Predicate p : restrictions) {
            _having.add(p);
        }
        return this;
    }

    @Override
    public CriteriaQuery<T> orderBy(Order... orders) {
        if (orders == null) {
            _orders = null;
            return this;
        }
        _orders = new ArrayList<>();
        for (Order o : orders) {
            _orders.add(o);
        }
        return this;
    }

    @Override
    public CriteriaQuery<T> orderBy(List<Order> orders) {
        if (orders == null) {
            _orders = null;
            return this;
        }
        _orders = new ArrayList<>();
        for (Order o : orders) {
            _orders.add(o);
        }
        return this;
    }

    /**
     * Specify the item that is to be returned in the query result.
     * Replaces the previously specified selection(s), if any.
     * @param selection  selection specifying the item that
     *        is to be returned in the query result
     * @return the modified query
     */
    @Override
    public CriteriaQuery<T> select(Selection<? extends T> selection) {
        _selection = selection;
        _selections = new ArrayList<>();
        _selections.add(selection);
        return this;
    }

    @Override
    public CriteriaQuery<T> where(Expression<Boolean> restriction) {
        invalidateCompilation();
        if (restriction == null) {
            _where = null;
            return this;
        }
        _where = (PredicateImpl)restriction;
        return this;
    }

    @Override
    public CriteriaQuery<T> where(Predicate... restrictions) {
        invalidateCompilation();
        if (restrictions == null) {
            _where = null;
            return this;
        }
        _where = new PredicateImpl.And(restrictions);
        return this;
    }

    @Override
    public <X> Root<X> from(EntityType<X> entity) {
        RootImpl<X> root = new RootImpl<>((Types.Entity<X>)entity);
        addRoot(root);
        return root;
    }


    @Override
    public <X> Root<X> from(Class<X> cls) {
        EntityType<X> entity = _model.entityImpl(cls);
        if (entity == null)
            throw new IllegalArgumentException(_loc.get("root-non-entity", cls).getMessage());
        return from(entity);
    }

    @Override
    public List<Expression<?>> getGroupList() {
        return Expressions.returnCopy(_groups);
    }

    @Override
    public PredicateImpl getGroupRestriction() {
        return _having;
    }

    @Override
    public PredicateImpl getRestriction() {
        return _where;
    }

    @Override
    public Set<Root<?>> getRoots() {
        return Expressions.returnCopy(_roots);
    }

    public Root<?> getRoot() {
        assertRoot();
        return _roots.iterator().next();
    }

    Root<?> getRoot(boolean mustExist) {
        if (mustExist) {
            return getRoot();
        }
        return _roots == null || _roots.isEmpty() ? null : _roots.iterator().next();
    }

    void addRoot(RootImpl<?> root) {
        if (_roots == null) {
            _roots = new LinkedHashSet<>();
        }
        _roots.add(root);
    }

    /**
     * Affirms if selection of this query is distinct.
     */
    @Override
    public boolean isDistinct() {
        return _distinct;
    }

    @Override
    public <U> Subquery<U> subquery(Class<U> type) {
        if (_subqueries == null)
            _subqueries = new ArrayList<>();
        Subquery<U> subquery = new SubqueryImpl<>(type, this);
        _subqueries.add(subquery);
        return subquery;
    }

    /**
     * Return map where key is the parameter expression itself and value is the expected type.
     * Empty map if no parameter has been declared.
     */
    public OrderedMap<Object, Class<?>> getParameterTypes() {
        collectParameters(new CriteriaExpressionVisitor.ParameterVisitor(this));
        return _params;
    }

    /**
     * Populate a kernel expression tree by translating the components of this
     * receiver with the help of the given {@link ExpressionFactory}.
     */
    QueryExpressions getQueryExpressions(ExpressionFactory factory) {
        Context context = new Context(null, null, null);
        _contexts.get().push(context);
        try {
            return new CriteriaExpressionBuilder().getQueryExpressions(factory, this);
        }finally{
            _contexts.remove();
        }
    }

    public void assertRoot() {
        if (_roots == null || _roots.isEmpty())
            throw new IllegalStateException(_loc.get("root-undefined").getMessage());
    }

    public void assertSelection() {
        if (_selection == null && !isDefaultProjection())
            throw new IllegalStateException(_loc.get("select-undefined").getMessage());
    }

    //
    // SubqueryContext
    //
    void setContexts(Stack<Context> contexts) {
        _contexts.set(contexts);
    }

    /**
     * Gets either this query itself if this is not a captive query for
     * a subquery. Otherwise gets the parent query of the delegating
     * subquery.
     */
    CriteriaQueryImpl<?> getAncestor() {
        if (_delegator == null)
            return this;
        AbstractQuery<?> parent = _delegator.getParent();
        if (parent instanceof CriteriaQueryImpl)
            return (CriteriaQueryImpl<?>)parent;
        // parent is a SubqueryImpl
        return ((SubqueryImpl<?>)parent).getDelegate().getAncestor();
    }

    public Map<Selection<?>,String> getAliases() {
        CriteriaQueryImpl<?> c = getAncestor();
        if (c._aliases == null)
            c._aliases = new HashMap<>();
        return c._aliases;
    }

    /**
     * Gets the current context.
     */
    Context ctx() {
        Stack<Context> ctxt = _contexts.get();
        return ctxt == null || ctxt.isEmpty() ? null :  ctxt.peek();
    }

    //
    // AliasContext management
    //

    /**
     * Gets the alias of the given node. Creates an automatic alias, if necessary.
     */
    @Override
    public String getAlias(Selection<?> selection) {
        String alias = selection.getAlias();
        if (alias != null) {
            _aliases.put(selection, alias);
            return alias;
        }
        alias = ALIAS_BASE + (++aliasCount);
        while (_aliases.containsValue(alias))
            alias = ALIAS_BASE + (++aliasCount);
        ((SelectionImpl<?>)selection).setAutoAlias(alias);
        _aliases.put(selection, alias);
        return _aliases.get(selection);
    }

    /**
     * Register the given variable of given path value against the given node.
     * If the given node has no alias then an alias is set to the given node.
     * If the variable or the path has non-null alias, then that alias must
     * be equal to the alias of the given node. Otherwise, the node alias is set
     * on the variable and path.
     */
    @Override
    public void registerVariable(Selection<?> node, Value var, Value path) {
        if (isRegistered(node)) {
            return;
            //throw new RuntimeException(node + " is already bound");
        }
        if (!var.isVariable())
            throw new RuntimeException(var.getClass() + " is not a variable");
        if (var.getPath() != path)
            throw new RuntimeException(var + " does not match given " + path + " Variable path is " + var.getPath());
        String alias = getAlias(node);

        if (!alias.equals(var.getAlias())) {
            if (var.getAlias() == null)
                var.setAlias(alias);
            else
                throw new RuntimeException("Variable alias " + var.getAlias() +
                " does not match expected selection alias " + alias);
        }
        if (!alias.equals(path.getAlias())) {
            if (path.getAlias() == null)
                path.setAlias(alias);
            else
                throw new RuntimeException("Path alias " + path.getAlias() +
                " does not match expected selection alias " + alias);
        }
        _variables.put(node, var);
        _values.put(node, path);
        _aliases.put(node, alias);
        // add to context
        ctx().addSchema(alias, var.getMetaData());
        ctx().addVariable(alias, var);
    }

    @Override
    public boolean isRegistered(Selection<?> selection) {
        if (_variables.containsKey(selection))
            return true;
        SubqueryImpl<?> delegator = getDelegator();
        return (delegator == null) ? false : getDelegatorParent().isRegistered(selection);
    }

    @Override
    public Value getRegisteredVariable(Selection<?> selection) {
        Value var = _variables.get(selection);
        if (var != null)
            return var;
        SubqueryImpl<?> delegator = getDelegator();
        return (delegator == null) ? null : getDelegatorParent().getRegisteredVariable(selection);
    }

    @Override
    public Value getRegisteredValue(Selection<?> selection) {
        Value val = _values.get(selection);
        if (val != null)
            return val;
        SubqueryImpl<?> delegator = getDelegator();
        return (delegator == null) ? null : getDelegatorParent().getRegisteredValue(selection);
    }

    /**
     * Registers a variable for the given root expression.
     * A root expression is registered only for cross join.
     * @param root
     * @param var
     */
    void registerRoot(Root<?> root, Value var) {
        if (var == null || !var.isVariable())
            throw new IllegalArgumentException("Attempt to register non-variable " + var);
        _rootVariables.put(root, var);
        String alias = var.getName();
        ctx().addSchema(alias, var.getMetaData());
        ctx().addVariable(alias, var);
    }

    /**
     * Gets the registered variable for the given root.
     */
    @Override
    public Value getRegisteredRootVariable(Root<?> root) {
        Value var = _rootVariables.get(root);
        if (var != null)
            return var;
        SubqueryImpl<?> delegator = getDelegator();
        return (delegator == null) ? null : getDelegatorParent().getRegisteredRootVariable(root);
    }

    CriteriaQueryImpl<?> getDelegatorParent() {
        AbstractQuery<?> parent = _delegator.getParent();
        if (parent instanceof CriteriaQueryImpl)
            return ((CriteriaQueryImpl<?>)parent);
        // parent is a SubqueryImpl
        return ((SubqueryImpl<?>)parent).getDelegate();
    }

    @Override
    public Class<T> getResultType() {
        return _resultClass;
    }

    @Override
    public CriteriaQuery<T> multiselect(List<Selection<?>> list) {
        return multiselect(list.toArray(new Selection<?>[list.size()]));
    }

    boolean isMultiselect() {
        return _selection instanceof CompoundSelections.MultiSelection;
    }

    protected boolean isDefaultProjection() {
        if (_selections == null) {
            return getRoots().size() == 1
               && (getRoot().getModel().getJavaType() == _resultClass ||
                   _resultClass == Object.class);
        }
        if (_selections.size() != 1) {
            return false;
        }
        Selection<?> sel = _selections.get(0);
        if (!getRoots().isEmpty() && sel == getRoot()) {
            return true;
        }
        if ((sel instanceof From<?,?>) && ((From<?,?>)sel).isCorrelated()) {
            return true;
        }
        return false;
    }

    void invalidateCompilation() {
        _compiled = false;
        _params.clear();
    }

    /**
     * Compiles to verify that at least one root is defined, a selection term is present
     * and, most importantly, collects all the parameters so that they can be bound to
     * the executable query.
     */
    @Override
    public OpenJPACriteriaQuery<T> compile() {
        if (_compiled)
            return this;
        assertRoot();
        assertSelection();
        collectParameters(new CriteriaExpressionVisitor.ParameterVisitor(this));
        _compiled = true;
        return this;
    }

    private void collectParameters(CriteriaExpressionVisitor visitor) {
        if (_compiled)
            return;
        if (_where != null) {
            _where.acceptVisit(visitor);
        }
        if (_having != null) {
            _having.acceptVisit(visitor);
        }

        if (_subqueries != null) {
            for (Subquery<?> subq : _subqueries) {
                ((SubqueryImpl<?>)subq).getDelegate().collectParameters(visitor);
            }
        }
    }

    /**
     * Gets the string representation of the query.
     */
    @Override
    public String toCQL() {
        StringBuilder buffer = new StringBuilder();
        render(buffer, _roots, null);
        return buffer.toString().trim();
    }

    void render(StringBuilder buffer, Set<Root<?>> roots, List<Join<?,?>> correlatedJoins) {
        buffer.append("SELECT ");
        if (isDistinct()) buffer.append(" DISTINCT ");
        buffer.append(_selection != null ? ((CriteriaExpression)_selection).asProjection(this) : "*");
        buffer.append(" FROM ");
        renderRoots(buffer, roots);
        renderJoins(buffer, correlatedJoins);
        if (_where != null) {
            buffer.append(" WHERE ").append(_where.asValue(this));
        }
        renderList(buffer, " ORDER BY ", getOrderList());
        renderList(buffer, " GROUP BY ", getGroupList());
        if (_having != null) {
            buffer.append(" HAVING ");
            buffer.append(_having.asValue(this));
        }
    }

    private void renderList(StringBuilder buffer, String clause, Collection<?> coll) {
        if (coll == null || coll.isEmpty()) {
            return;
        }

        buffer.append(clause);
        for (Iterator<?> i = coll.iterator(); i.hasNext(); ) {
            buffer.append(((CriteriaExpression)i.next()).asValue(this));
            if (i.hasNext()) buffer.append(", ");
        }
    }

    private void renderJoins(StringBuilder buffer, Collection<Join<?,?>> joins) {
        if (joins == null) {
            return;
        }

        for (Join j : joins) {
            buffer.append(((CriteriaExpression)j).asVariable(this)).append(" ");
            renderJoins(buffer, j.getJoins());
            renderFetches(buffer, j.getFetches());
        }
    }

    private void renderRoots(StringBuilder buffer, Collection<Root<?>> roots) {
        if (roots == null) {
            return;
        }
        int i = 0;
        for (Root r : roots) {
            buffer.append(((ExpressionImpl<?>)r).asVariable(this));
            if (++i != roots.size()) buffer.append(", ");
            renderJoins(buffer, r.getJoins());
            renderFetches(buffer, r.getFetches());
        }
    }
    private void renderFetches(StringBuilder buffer, Set<Fetch> fetches) {
        if (fetches == null) {
            return;
        }
        for (Fetch j : fetches) {
            buffer.append(((ExpressionImpl<?>)j).asValue(this)).append(" ");
        }
    }

    /**
     * Returns a JPQL-like string, if this receiver is populated. Otherwise
     * returns <code>Object.toString()</code>.
     */
    @Override
    public String toString() {
        try {
            return toCQL();
        } catch (Throwable t) {
            return super.toString();
        }
    }

    @Override
    public boolean equals(Object other) {
        if(other == null) {
            return false;
        }

        if (toString().equals(other.toString()))
            return true;
        return false;
    }
}
