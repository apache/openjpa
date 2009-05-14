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
import java.util.LinkedHashSet;
import java.util.List;
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

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Criteria query implementation.
 * 
 * @author Pinaki Poddar
 *
 */
public class CriteriaQueryImpl implements CriteriaQuery {
    private final MetamodelImpl  _model;
    private Set<Root<?>>        _roots;
    private PredicateImpl       _where;
    private List<Order>         _orders;
    private Set<Parameter<?>>   _params;
    private List<Selection<?>>  _selections;
    private List<Expression<?>> _groups;
    private PredicateImpl       _having;
    private Boolean             _distinct;
    
    public CriteriaQueryImpl(MetamodelImpl model) {
        this._model = model;
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
        Root<X> root = new RootImpl<X>(entity);
        if (_roots == null) {
            _roots = new LinkedHashSet<Root<?>>();
        }
        _roots.add(root);
        return root;
    }

    public <X> Root<X> from(Class<X> entityClass) {
        return from(_model.entity(entityClass));
    }

    public List<Expression<?>> getGroupList() {
        return _groups;
    }

    public Predicate getGroupRestriction() {
        return _having;
    }

    public PredicateImpl getRestriction() {
        return _where;
    }

    public Set<Root<?>> getRoots() {
        return _roots;
    }
    
    public Root<?> getRoot() {
        if (_roots == null || _roots.isEmpty())
            throw new IllegalStateException("no root is set");
        return _roots.iterator().next();
    }

    public boolean isDistinct() {
        return _distinct;
    }

    public <U> Subquery<U> subquery(Class<U> type) {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * Populate kernel expressions.
     */
    QueryExpressions getQueryExpressions(ExpressionFactory factory) {
	    QueryExpressions exps = new QueryExpressions();
	    
	    if (_roots != null) {
	    	exps.accessPath = new ClassMetaData[_roots.size()];
	    	int i = 0;
	    	for (Root<?> r : _roots)
	    	   exps.accessPath[i++] = ((Types.Managed<?>)r.getModel()).meta;
	    }
	//    exps.alias = null;      // String   
	    exps.ascending = new boolean[]{false};
	    exps.distinct = _distinct == null ? QueryExpressions.DISTINCT_AUTO :
	    	_distinct ? QueryExpressions.DISTINCT_TRUE 
	    			  : QueryExpressions.DISTINCT_FALSE;
	//    exps.fetchInnerPaths = null; // String[]
	//    exps.fetchPaths = null;      // String[]
	    exps.filter = _where == null ? factory.emptyExpression() 
	    		: _where.toKernelExpression(factory, _model);
	    
	    evalGrouping(exps, factory);
	    exps.having = _having == null ? factory.emptyExpression() 
	    		: _having.toKernelExpression(factory, _model);
	    
	    evalOrdering(exps, factory);
	//    exps.operation = QueryOperations.OP_SELECT;
	    
	//    exps.parameterTypes = null; // LinkedMap<>
	//    exps.projectionAliases = null; // String[]
	//    exps.projectionClauses = null; // String[]
	      exps.projections = toValues(factory, getSelectionList());
	//    exps.range = null; // Value[]
	//    exps.resultClass = null; // Class
	    return exps;
    }

    void evalOrdering(QueryExpressions exps, ExpressionFactory factory) {
        if (_orders == null) 
            return;
        int ordercount = _orders.size();
        exps.ordering = new Value[ordercount];
        exps.orderingClauses = new String[ordercount];
        exps.orderingAliases = new String[ordercount];
        exps.ascending = new boolean[ordercount];
        for (int i = 0; i < ordercount; i++) {
            OrderImpl order = (OrderImpl)_orders.get(i);
            //Expression<? extends Comparable> expr = order.getExpression();
            //exps.ordering[i] = Expressions.toValue(
            //    (ExpressionImpl<?>)expr, factory, _model);
            
            //exps.orderingClauses[i] = assemble(firstChild);
            //exps.orderingAliases[i] = firstChild.text;
            exps.ascending[i] = order.isAscending();
        }
    }
    
    void evalGrouping(QueryExpressions exps, ExpressionFactory factory) {
        if (_groups == null) 
            return;
        int groupByCount = _groups.size();
        exps.grouping = new Value[groupByCount];

        for (int i = 0; i < groupByCount; i++) {
             Expression<?> groupBy = _groups.get(i);    
             exps.grouping[i] = Expressions.toValue(
                 (ExpressionImpl<?>)groupBy, factory, _model);;
        }
    }
    
    
    

    Value[] toValues(ExpressionFactory factory, List<Selection<?>> sels) {
    	if (sels == null || (sels.size() == 1 && sels.get(0) == getRoot()))
    			return new Value[0];
    	Value[] result = new Value[sels.size()];
    	int i = 0;
    	for (Selection<?> s : sels) {
    		result[i++] = ((ExpressionImpl<?>)s).toValue(factory, _model);
    	}
    	return result;
    }

}
