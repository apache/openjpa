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

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.exps.AbstractExpressionBuilder;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
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
    private LinkedMap           _parameterTypes;
    private Class               _resultClass;
    private Value[]             _projections;
    private int                 _aliasCount = 0;
    
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
    
    public LinkedMap getParameterTypes() {
        return _parameterTypes;
    }
    
    public void setParameterTypes(LinkedMap parameterTypes) {
        _parameterTypes = parameterTypes;
    }
    
    public void setResultClass(Class resultClass) {
        _resultClass = resultClass;
    }

    public void setProjections(Value[] projections) {
        _projections = projections;
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
        exps.distinct = _distinct == null ? QueryExpressions.DISTINCT_FALSE :
            _distinct ?
            QueryExpressions.DISTINCT_TRUE | QueryExpressions.DISTINCT_AUTO : 
            QueryExpressions.DISTINCT_FALSE;
    //    exps.fetchInnerPaths = null; // String[]
	//    exps.fetchPaths = null;      // String[]
	    exps.filter = _where == null ? factory.emptyExpression() 
	    		: _where.toKernelExpression(factory, _model, this);
	    
	    evalGrouping(exps, factory);
	    exps.having = _having == null ? factory.emptyExpression() 
	    		: _having.toKernelExpression(factory, _model, this);
	    
	    evalOrdering(exps, factory);
	//    exps.operation = QueryOperations.OP_SELECT;
	    
	    evalProjection(exps, factory);
	    
	//    exps.range = null; // Value[]
	//    exps.resultClass = null; // Class
	    if (_parameterTypes != null)
	        exps.parameterTypes = _parameterTypes;
	    exps.resultClass = _resultClass;
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
            //    (ExpressionImpl<?>)expr, factory, _model, this);
            
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
                 (ExpressionImpl<?>)groupBy, factory, _model, this);;
        }
    }

    void evalProjection(QueryExpressions exps, ExpressionFactory factory) {
        Value [] projs = toValues(exps, factory, getSelectionList());
        if (projs.length == 1 && projs[0] == null)
            exps.projections = _projections;
        else 
            exps.projections = projs;
        //exps.projectionClauses = String[];
    }    

    Value[] toValues(QueryExpressions exps, ExpressionFactory factory, 
        List<Selection<?>> sels) {
    	if (sels == null || (sels.size() == 1 && sels.get(0) == getRoot()))
    			return new Value[0];
    	Value[] result = new Value[sels.size()];
    	String[] aliases = new String[sels.size()];
    	int i = 0;
    	for (Selection<?> s : sels) {
            result[i] = ((SelectionImpl<?>)s).toValue(factory, _model, 
    		    this);
            aliases[i] = nextAlias();
            i++;
        }
        exps.projectionAliases = aliases;
    	
    	return result;
    }

    void setImplicitTypes(Value val1, Value val2, Class<?> expected) {
        Class<?> c1 = val1.getType();
        Class<?> c2 = val2.getType();
        boolean o1 = c1 == AbstractExpressionBuilder.TYPE_OBJECT;
        boolean o2 = c2 == AbstractExpressionBuilder.TYPE_OBJECT;

        if (o1 && !o2) {
            val1.setImplicitType(c2);
            if (val1.getMetaData() == null && !val1.isXPath())
                val1.setMetaData(val2.getMetaData());
        } else if (!o1 && o2) {
            val2.setImplicitType(c1);
            if (val2.getMetaData() == null && !val1.isXPath())
                val2.setMetaData(val1.getMetaData());
        } else if (o1 && o2 && expected != null) {
            // we never expect a pc type, so don't bother with metadata
            val1.setImplicitType(expected);
            val2.setImplicitType(expected);
        } else if (AbstractExpressionBuilder.isNumeric(val1.getType()) 
            != AbstractExpressionBuilder.isNumeric(val2.getType())) {
            AbstractExpressionBuilder.convertTypes(val1, val2);
        }

        // as well as setting the types for conversions, we also need to
        // ensure that any parameters are declared with the correct type,
        // since the JPA spec expects that these will be validated
        org.apache.openjpa.kernel.exps.Parameter param =
            val1 instanceof org.apache.openjpa.kernel.exps.Parameter ? 
            (org.apache.openjpa.kernel.exps.Parameter) val1
            : val2 instanceof org.apache.openjpa.kernel.exps.Parameter ? 
            (org.apache.openjpa.kernel.exps.Parameter) val2 : null;
        Path path = val1 instanceof Path ? (Path) val1
            : val2 instanceof Path ? (Path) val2 : null;

        // we only check for parameter-to-path comparisons
        if (param == null || path == null || _parameterTypes == null)
            return;

        FieldMetaData fmd = path.last();
        if (fmd == null)
            return;

        //TODO:
        //if (expected == null)
        //    checkEmbeddable(path);

        Class<?> type = path.getType();
        if (type == null)
            return;

        Object paramKey = param.getParameterKey();
        if (paramKey == null)
            return;

        // make sure we have already declared the parameter
        if (_parameterTypes.containsKey(paramKey))
            _parameterTypes.put(paramKey, type);
    }
    
    private String nextAlias() {
        return "jpqlalias" + (++_aliasCount);
    }
    
}
