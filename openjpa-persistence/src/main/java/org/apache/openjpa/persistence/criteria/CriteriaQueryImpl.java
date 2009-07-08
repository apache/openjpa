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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
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
public class CriteriaQueryImpl<T> implements CriteriaQuery<T>, AliasContext {
    private final MetamodelImpl  _model;
    private Set<Root<?>>        _roots;
    private PredicateImpl       _where;
    private List<Order>         _orders;
    private LinkedHashMap<ParameterExpression<?>, Class<?>>           _paramTypes;
    private List<Selection<?>>  _selections;
    private List<Expression<?>> _groups;
    private PredicateImpl       _having;
    private List<Subquery<?>>   _subqueries;
    private Boolean             _distinct;
    private SubqueryImpl<?>     _delegator;
    private final Class<T>      _resultClass;
    
    // AliasContext
    private int aliasCount = 0;
    private static String ALIAS_BASE = "autoAlias";
    
    private Map<Selection<?>,Value> _variables = 
        new HashMap<Selection<?>, Value>();
    private Map<Selection<?>,Value> _values = 
        new HashMap<Selection<?>, Value>();
    private Map<Selection<?>,String> _aliases = null;

    // SubqueryContext
    //private Stack<Context> _contexts = null;
    
    public CriteriaQueryImpl(MetamodelImpl model, Class<T> resultClass) {
        this._model = model;
        this._resultClass = resultClass;
        _aliases = new HashMap<Selection<?>, String>(); 
    }
    
    public CriteriaQueryImpl(MetamodelImpl model, SubqueryImpl<T> delegator) {
        this._model = model;
        this._resultClass = delegator.getJavaType();
        _delegator = delegator;
        _aliases = getAliases();
    }

    public void setDelegator(SubqueryImpl<?> delegator) {
        _delegator = delegator;
    }
    
    public SubqueryImpl<?> getDelegator() {
        return _delegator;
    }
    
    public MetamodelImpl getMetamodel() {
        return _model;
    }
    
    //public Stack<Context> getContexts() {
    //    return _contexts;
    //}
    
    public CriteriaQuery<T> distinct(boolean distinct) {
        _distinct = distinct;
        return this;
    }

    public List<Order> getOrderList() {
        return _orders;
    }
    
    /**
     * Return the selection of the query
     * @return the item to be returned in the query result
     */
    public Selection<T> getSelection() {
        throw new AbstractMethodError();
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
    public CriteriaQuery<T> multiselect(Selection<?>... selections) {
        return select(selections);
    }

    /**
     * Registers the given parameter.
     * 
     * @param p
     */
    public void registerParameter(ParameterImpl<?> p) {
        if (_paramTypes == null) {
            _paramTypes = new LinkedHashMap<ParameterExpression<?>, Class<?>>();
        }
        _paramTypes.put(p, p.getJavaType());
        if (p.getPosition() == null)
            p.setPosition(_paramTypes.size());
    }
    
    public Set<ParameterExpression<?>> getParameters() {
        return _paramTypes.keySet();
    }

    /**
     * Return the selection items of the query as a list
     * @return the selection items of the query as a list
     */
    public List<Selection<?>> getSelectionList() {
        return _selections;
    }

    public CriteriaQuery<T> groupBy(Expression<?>... grouping) {
    	_groups = new ArrayList<Expression<?>>();
    	for (Expression<?> e : grouping)
    		_groups.add(e);
        return this;
    }

    public CriteriaQuery<T> having(Expression<Boolean> restriction) {
        _having = new PredicateImpl().add(restriction);
        return this;
    }

    public CriteriaQuery<T> having(Predicate... restrictions) {
        _having = new PredicateImpl();
        for (Predicate p : restrictions)
        	_having.add(p);
        return this;
    }

    public CriteriaQuery<T> orderBy(Order... o) {
        _orders = Arrays.asList(o);
        return this;
    }
    
    /**
     * Specify the item that is to be returned in the query result.
     * Replaces the previously specified selection(s), if any.
     * @param selection  selection specifying the item that
     *        is to be returned in the query result
     * @return the modified query
     */
    public CriteriaQuery<T> select(Selection<? extends T> selection) {
        return select(new Selection<?>[]{selection});
    }

    public CriteriaQuery<T> select(Selection<?>... selections) {
        _selections = Arrays.asList(selections);
        return this;
    }

    public CriteriaQuery<T> where(Expression<Boolean> restriction) {
        _where = new PredicateImpl().add(restriction);
        return this;
    }

    public CriteriaQuery<T> where(Predicate... restrictions) {
        _where = new PredicateImpl();
        for (Predicate p : restrictions)
        	_where.add(p);
        return this;
    }

    public <X> Root<X> from(EntityType<X> entity) {
        Root<X> root = new RootImpl<X>((Types.Entity<X>)entity);
        if (_roots == null) {
            _roots = new LinkedHashSet<Root<?>>();
        }
        _roots.add(root);
        return root;
    }

    
    public <X> Root<X> from(Class<X> cls) {
        EntityType<X> entity = _model.entity(cls);
        if (entity == null)
            throw new IllegalArgumentException(cls + " is not an entity");
        return from(entity);
    }

    public List<Expression<?>> getGroupList() {
        return _groups;
    }

    public PredicateImpl getGroupRestriction() {
        return _having;
    }

    public PredicateImpl getRestriction() {
        return _where;
    }

    public Set<Root<?>> getRoots() {
        return _roots;
    }
    
    public void setRoots (Set<Root<?>> roots) {
        this._roots = roots;
    }

    public Root<?> getRoot() {
        assertRoot();
        return _roots.iterator().next();
    }

    public boolean isDistinct() {
        return _distinct;
    }

    public Boolean getDistinct() {
        return _distinct;
    }

    public <U> Subquery<U> subquery(Class<U> type) {
        if (_subqueries == null)
            _subqueries = new ArrayList<Subquery<?>>();
        Subquery<U> subquery = new SubqueryImpl<U>(type, this);
        _subqueries.add(subquery);
        return subquery;
    }
    
    public LinkedMap getParameterTypes() {
        if (_paramTypes == null)
            return StoreQuery.EMPTY_PARAMS;
        LinkedMap  parameterTypes = new LinkedMap();
        for (ParameterExpression<?> p : _paramTypes.keySet()) {
            if (p.getName() == null && p.getPosition() == null)
                throw new RuntimeException(p + " is not set");
            Object paramKey = p.getName() == null 
               ? p.getPosition() : p.getName();
            parameterTypes.put(paramKey, p.getJavaType());
        }
        return parameterTypes;
    }
    
    /**
     * Populate a kernel expression tree by translating the components of this
     * receiver with the help of the given {@link ExpressionFactory}.
     */
    QueryExpressions getQueryExpressions(ExpressionFactory factory) {
        //_contexts = new Stack<Context>();
        //Context context = new Context(null, null, null);
        //    _contexts.push(context);
        return new CriteriaExpressionBuilder()
             .getQueryExpressions(factory, this);
    }    
    
    public void assertRoot() {
        if (_roots == null || _roots.isEmpty())
            throw new IllegalStateException("no root is set");
    }
    
    //
    // SubqueryContext
    //
    //public void setContexts(Stack<Context> contexts) {
    //    _contexts = contexts;
    //}
    
    public CriteriaQueryImpl<?> getInnermostParent() {
        if (_delegator == null)
            return this;
        AbstractQuery<?> parent = _delegator.getParent();
        if (parent instanceof CriteriaQueryImpl) 
            return (CriteriaQueryImpl<?>)parent;
        // parent is a SubqueryImpl    
        return ((SubqueryImpl<?>)parent).getDelegate().getInnermostParent();
    }
    
    public Map<Selection<?>,String> getAliases() {
        CriteriaQueryImpl<?> c = getInnermostParent();
        if (c._aliases == null)
            c._aliases = new HashMap<Selection<?>, String>();
        return c._aliases;
    }
    
    //
    // AliasContext management
    //
    
    /**
     * Gets the alias of the given node. Creates if necessary.
     */
    public String getAlias(Selection<?> selection) {
        String alias = selection.getAlias();
        if (alias != null) {
            _aliases.put(selection, alias);
            return alias;
        }
        alias = ALIAS_BASE + (++aliasCount);
        while (_aliases.containsValue(alias))
            alias = ALIAS_BASE + (++aliasCount);
        selection.alias(alias);
        _aliases.put(selection, alias);
        return _aliases.get(selection);
    }
    
    public Value getVariable(Selection<?> selection) {
        return _variables.get(selection);
    }
    
    public Value getValue(Selection<?> selection) {
        return _values.get(selection);
    }
    
    /**
     * Register the given variable of given path value against the given node.
     * If the given node has no alias then an alias is set to the given node.
     * If the variable or the path has non-null alias, then that alias must
     * be equal to the alias of the given node. Otherwise, the node alias is set
     * on the variable and path.  
     */
    public void registerVariable(Selection<?> node, Value var, Value path) {
        if (isRegistered(node))
            throw new RuntimeException(node + " is already bound");
        if (!var.isVariable())
            throw new RuntimeException(var.getClass() + " is not a variable");
        if (var.getPath() != path)
            throw new RuntimeException(var + " and " + path);
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
        //_contexts.peek().addSchema(alias, var.getMetaData());
        //_contexts.peek().addVariable(alias, var);
    }
    
    public boolean isRegistered(Selection<?> selection) {
        return _variables.containsKey(selection);
    }

    public Class getResultType() {
        // TODO Auto-generated method stub
        return null;
    }

    public CriteriaQuery<T> multiselect(List<Selection<?>> arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
