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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Parameter;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.Entity;

import org.apache.commons.collections.map.LinkedMap;
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
public class CriteriaQueryImpl implements CriteriaQuery, AliasContext {
    private final MetamodelImpl  _model;
    private Set<Root<?>>        _roots;
    private PredicateImpl       _where;
    private List<Order>         _orders;
    private Set<Parameter<?>>   _params;
    private List<Selection<?>>  _selections;
    private List<Expression<?>> _groups;
    private PredicateImpl       _having;
    private List<Subquery<?>>   _subqueries;
    private Boolean             _distinct;
    private LinkedMap           _parameterTypes;
    private SubqueryImpl<?>     _context;
    
    // AliasContext
    private int aliasCount = 0;
    private static String ALIAS_BASE = "autoAlias";
    
    private Map<Selection<?>,Value> _variables = 
        new HashMap<Selection<?>, Value>();
    private Map<Selection<?>,Value> _values = 
        new HashMap<Selection<?>, Value>();
    private Map<Selection<?>,String> _aliases = 
        new HashMap<Selection<?>, String>();
    
    public CriteriaQueryImpl(MetamodelImpl model) {
        this._model = model;
    }
    
    public void setContext(SubqueryImpl<?> context) {
        _context = context;
    }
    
    public SubqueryImpl<?> getContext() {
        return _context;
    }
    
    public MetamodelImpl getMetamodel() {
        return _model;
    }
    
    public CriteriaQuery distinct(boolean distinct) {
        _distinct = distinct;
        return this;
    }

    public List<Order> getOrderList() {
        return _orders;
    }

    public Set<Parameter<?>> getParameters() {
        return _params;
    }

    public List<Selection<?>> getSelectionList() {
        return _selections;
    }

    public CriteriaQuery groupBy(Expression<?>... grouping) {
    	_groups = new ArrayList<Expression<?>>();
    	for (Expression<?> e : grouping)
    		_groups.add(e);
        return this;
    }

    public CriteriaQuery having(Expression<Boolean> restriction) {
        _having = new PredicateImpl().add(restriction);
        return this;
    }

    public CriteriaQuery having(Predicate... restrictions) {
        _having = new PredicateImpl();
        for (Predicate p : restrictions)
        	_having.add(p);
        return this;
    }

    public CriteriaQuery orderBy(Order... o) {
        _orders = Arrays.asList(o);
        return this;
    }

    public CriteriaQuery select(Selection<?>... selections) {
        _selections = Arrays.asList(selections);
        return this;
    }

    public CriteriaQuery where(Expression<Boolean> restriction) {
        _where = new PredicateImpl().add(restriction);
        return this;
    }

    public CriteriaQuery where(Predicate... restrictions) {
        _where = new PredicateImpl();
        for (Predicate p : restrictions)
        	_where.add(p);
        return this;
    }

    public <X> Root<X> from(Entity<X> entity) {
        Root<X> root = new RootImpl<X>((Types.Entity<X>)entity);
        if (_roots == null) {
            _roots = new LinkedHashSet<Root<?>>();
        }
        _roots.add(root);
        return root;
    }

    public <X> Root<X> from(Class<X> cls) {
        Entity<X> entity = _model.entity(cls);
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
        return _parameterTypes;
    }
    
    public void setParameterTypes(LinkedMap parameterTypes) {
        _parameterTypes = parameterTypes;
    }
    
    /**
     * Populate a kernel expression tree by translating the components of this
     * receiver with the help of the given {@link ExpressionFactory}.
     */
    QueryExpressions getQueryExpressions(ExpressionFactory factory) {
        return new CriteriaExpressionBuilder()
              .getQueryExpressions(factory, this);
    }    
    
    public void assertRoot() {
        if (_roots == null || _roots.isEmpty())
            throw new IllegalStateException("no root is set");
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
        selection.setAlias(alias);
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
        if (isRegistered(node))
            throw new RuntimeException(node + " is already bound");
        _variables.put(node, var);
        _values.put(node, path);
        _aliases.put(node, alias);
    }
    
    public boolean isRegistered(Selection<?> selection) {
        return _variables.containsKey(selection);
    }
}
