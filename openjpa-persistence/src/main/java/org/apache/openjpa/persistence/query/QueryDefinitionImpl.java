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
package org.apache.openjpa.persistence.query;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.CaseExpression;
import javax.persistence.DomainObject;
import javax.persistence.Expression;
import javax.persistence.OrderByItem;
import javax.persistence.PathExpression;
import javax.persistence.Predicate;
import javax.persistence.QueryDefinition;
import javax.persistence.SelectItem;
import javax.persistence.Subquery;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Implements QueryDefinition.
 * 
 * @author Pinaki Poddar
 *
 */
public class QueryDefinitionImpl extends ExpressionImpl 
    implements QueryDefinition, Expression  {
	private final QueryBuilderImpl _builder;
	private List<AbstractDomainObject> _domains;
	private List<PathExpression> _groupBys;
	private List<Subquery> _subqueries;
	private List<OrderableItem> _orderBys;
	private List<Selectable>  _projections;
	private boolean  _distinct;
	private Predicate _where;
	private Predicate _having;
	
	protected static Localizer _loc = 
		Localizer.forPackage(QueryDefinitionImpl.class);
	
	protected QueryDefinitionImpl(QueryBuilderImpl builder) {
		_builder = builder;
	}
	
	/**
	 * Root domain object has no parent, no path but a non-null Class.
	 */
	public DomainObject addRoot(Class cls) {
		RootPath root = new RootPath(this, cls);
		addDomain(root);
		return root;
	}
	
	public DomainObject addSubqueryRoot(PathExpression path) {
		if (_domains != null && _domains.contains(path))
			throw new IllegalArgumentException(_loc.get("query-subroot-clash", 
					path).toString());
		AbstractPath impl = (AbstractPath)path;
		if (_subqueries == null) 
			_subqueries = new ArrayList<Subquery>();
		AbstractDomainObject newRoot = new NavigationPath(this, impl.getParent(), 
				impl.getLastSegment().toString());
		addDomain(newRoot);
		return newRoot;
	}
	
	protected void addDomain(AbstractDomainObject path) {
		if (_domains == null)
			_domains = new ArrayList<AbstractDomainObject>();
		_domains.add(path);
	}

	public Subquery all() {
		return new AllExpression(this);
	}

	public Subquery any() {
		return new AnyExpression(this);
	}

	public Expression coalesce(Expression... exp) {
		throw new UnsupportedOperationException();
	}

	public Expression coalesce(String... exp) {
		throw new UnsupportedOperationException();
	}

	public Expression coalesce(Date... exp) {
		throw new UnsupportedOperationException();
	}

	public Expression coalesce(Calendar... exp) {
		throw new UnsupportedOperationException();
	}

	public Expression currentDate() {
		return new CurrentTimeExpression(Date.class);
	}

	public Expression currentTime() {
		return new CurrentTimeExpression(Time.class);
	}

	public Expression currentTimestamp() {
		return new CurrentTimeExpression(Timestamp.class);
	}

	public Predicate exists() {
		return new ExistsExpression(this);
	}

	public CaseExpression generalCase() {
		return new CaseExpressionImpl();
	}

	public QueryDefinition groupBy(PathExpression... pathExprs) {
		if (_groupBys == null) {
			_groupBys = new ArrayList<PathExpression>();
		}
		for (PathExpression e : pathExprs)
			_groupBys.add(e);
		return this;
	}

	public QueryDefinition groupBy(List<PathExpression> pathExprList) {
		if (_groupBys == null) {
			_groupBys = new ArrayList<PathExpression>();
		}
		for (PathExpression e : pathExprList)
			_groupBys.add(e);
		return this;
	}

	public QueryDefinition having(Predicate predicate) {
		_having = predicate;
		return this;
	}

	public Expression literal(String s) {
		return new LiteralExpression(s);
	}

	public Expression literal(Number n) {
		return new LiteralExpression(n);
	}

	public Expression literal(boolean b) {
		return new LiteralExpression(b);
	}

	public Expression literal(Calendar c) {
		return new LiteralExpression(c);
	}

	public Expression literal(Date d) {
		return new LiteralExpression(d);
	}

	public Expression literal(char c) {
		return new LiteralExpression(c);
	}

	public Expression literal(Class cls) {
		return new LiteralExpression(cls);
	}

	public Expression literal(Enum<?> e) {
		return new LiteralExpression(e);
	}

	public Expression nullLiteral() {
		return new LiteralExpression(null);
	}

	public SelectItem newInstance(Class cls, SelectItem... args) {
		return new NewInstance(cls, args);
	}

	public Expression nullif(Expression exp1, Expression exp2) {
		throw new UnsupportedOperationException();
	}

	public Expression nullif(Number arg1, Number arg2) {
		throw new UnsupportedOperationException();
	}

	public Expression nullif(String arg1, String arg2) {
		throw new UnsupportedOperationException();
	}

	public Expression nullif(Date arg1, Date arg2) {
		throw new UnsupportedOperationException();
	}

	public Expression nullif(Calendar arg1, Calendar arg2) {
		throw new UnsupportedOperationException();
	}

	public Expression nullif(Class arg1, Class arg2) {
		throw new UnsupportedOperationException();
	}

	public Expression nullif(Enum<?> arg1, Enum<?> arg2) {
		throw new UnsupportedOperationException();
	}

	public QueryDefinition orderBy(OrderByItem... orderByItems) {
		if (_orderBys == null)
			_orderBys = new ArrayList<OrderableItem>();
		for (OrderByItem i : orderByItems) {
			if (i instanceof OrderableItem)
				_orderBys.add((OrderableItem)i);
			else
				_orderBys.add(new OrderableItem((ExpressionImpl)i, null));
		}
		return this;
	}

	public QueryDefinition orderBy(List<OrderByItem> orderByItemList) {
		if (_orderBys == null)
			_orderBys = new ArrayList<OrderableItem>();
		for (OrderByItem i : orderByItemList) {
			if (i instanceof OrderableItem)
				_orderBys.add((OrderableItem)i);
			else
				_orderBys.add(new OrderableItem((ExpressionImpl)i, null));
		}
		return this;
	}

	public Expression param(String name) {
		return new ParameterExpression(name);
	}

	public Predicate predicate(boolean b) {
		return null;
	}

	public QueryDefinition select(SelectItem... items) {
		return select(items == null ? null : Arrays.asList(items), false);
	}

	public QueryDefinition select(List<SelectItem> items) {
		return select(items, false);
	}

	public QueryDefinition selectDistinct(SelectItem... items) {
		return select(items == null ? null : Arrays.asList(items), true);
	}

	public QueryDefinition selectDistinct(List<SelectItem> items) {
		return select(items, false);
	}
	
	private QueryDefinition select(List<SelectItem> items, boolean isDistinct) {
		if (_projections == null) {
			_projections = new ArrayList<Selectable>();
		} else {
			_projections.clear();
		}
		_distinct = isDistinct;
		for (SelectItem item : items)
			_projections.add((Selectable)item);
		return this;
	}

	public CaseExpression simpleCase(Expression caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public CaseExpression simpleCase(Number caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public CaseExpression simpleCase(String caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public CaseExpression simpleCase(Date caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public CaseExpression simpleCase(Calendar caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public CaseExpression simpleCase(Class caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public CaseExpression simpleCase(Enum<?> caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	public Subquery some() {
		return new SomeExpression(this);
	}

	public QueryDefinition where(Predicate predicate) {
		_where = predicate;
		return this;
	}
	
	private List<Selectable> getProjections() {
		if (_projections == null) {
			List<Selectable> defaultProjection = new ArrayList<Selectable>();
			defaultProjection.add(_domains.get(0));
			return defaultProjection;
		}
		return _projections;
	}

	public String toJPQL() {
		return asExpression(new AliasContext());
	}
	
	/**
	 * 
	 */
	@Override
	public String asExpression(AliasContext ctx) {
		StringBuffer buffer = new StringBuffer();
		registerDomains(ctx);
		buffer.append("SELECT ");
		if (_distinct) 
			buffer.append("DISTINCT ");
		List<Selectable> projs = getProjections();
		for (int i=0; i < projs.size(); i++) {
			projs.get(i).setAlias(ctx);
			buffer.append(projs.get(i).asProjection(ctx));
			if (i != projs.size()-1)
				buffer.append(",");
		}
		buffer.append(" FROM ");
		for (int i=0; _domains != null && i < _domains.size(); i++) {
			buffer.append(_domains.get(i).asJoinable(ctx));
			List<JoinPath> joins = _domains.get(i).getJoins();
			if (joins != null) {
				for (int j = 0; j < joins.size(); j++) {
					buffer.append(joins.get(j).asJoinable(ctx));
				}
			}
			List<FetchPath> fetchJoins = _domains.get(i).getFetchJoins();
			if (fetchJoins != null) {
				for (int j = 0; j < fetchJoins.size(); j++) {
					buffer.append(fetchJoins.get(j).asExpression(ctx));
				}
			}
			
			if (i != _domains.size()-1)
				buffer.append(",");
		}
		if (_where != null) {
			buffer.append(" WHERE ").append(((Visitable)_where).asExpression(ctx));
		}
		
		if (_groupBys != null) {
			buffer.append(" GROUP BY ");
			for (int i = 0; i<_groupBys.size(); i++) {
				buffer.append(((ExpressionImpl)_groupBys.get(i)).asExpression(ctx));
				if (i != _groupBys.size()-1)
					buffer.append(",");
			}
		}
		if (_having != null) {
			buffer.append(" HAVING ").append(((Visitable)_having).asExpression(ctx));
		}
		if (_orderBys != null) {
			buffer.append(" ORDER BY ");
			for (int i = 0; i<_orderBys.size(); i++) {
				buffer.append(((OrderableItem)_orderBys.get(i)).toJPQL(ctx));
				if (i != _orderBys.size()-1)
					buffer.append(",");
			}
		}
		
		return buffer.toString();
	}
	
	public String asProjection(AliasContext ctx) {
		return asExpression(ctx);
	}
	
	/**
	 * Registers each domain with an alias.
	 * @param ctx
	 */
	private void registerDomains(AliasContext ctx) {
		if (_domains != null) {
			for (AbstractDomainObject domain : _domains) {
				domain.setAlias(ctx);
			}
		}
		if (_subqueries != null) {
			for (Subquery sub : _subqueries) {
				if (sub instanceof QueryDefinitionImpl)
					((QueryDefinitionImpl)sub).registerDomains(ctx);
				else
					((AbstractDomainObject)sub).setAlias(ctx);
			}
		}
	}
}
