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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Implements QueryDefinition.
 *
 * @author Pinaki Poddar
 *
 */
public class QueryDefinitionImpl extends ExpressionImpl
    implements QueryDefinition, Expression  {
	
    private static final long serialVersionUID = 1L;
    private final QueryBuilderImpl _builder;
	private List<AbstractDomainObject> _domains;
	private List<PathExpression> _groupBys;
	private List<OrderableItem> _orderBys;
	private List<SelectItem>  _projections;
	private boolean  _distinct;
	private Predicate _where;
	private Predicate _having;

	private static enum Visit {PROJECTION, EXPRESSION, JOINABLE};

	protected static Localizer _loc =
		Localizer.forPackage(QueryDefinitionImpl.class);

	/**
	 *
	 * @param builder
	 */
	protected QueryDefinitionImpl(QueryBuilderImpl builder) {
		_builder = builder;
	}

	/**
	 * Root domain object has no parent, no path but a non-null Class.
	 */
	@Override
    public DomainObject addRoot(Class cls) {
		RootPath root = new RootPath(this, cls);
		addDomain(root);
		return root;
	}

	@Override
    public DomainObject addSubqueryRoot(PathExpression path) {
		AbstractPath impl = (AbstractPath)path;
		LinkedList<AbstractPath> paths = impl.split();
		QueryDefinitionImpl owner = impl.getOwner();
		int i = 0;
		while (i < paths.size() && owner.hasDomain(paths.get(i))) {
			i++;
		}

		AbstractPath next = paths.get(i);
		DomainObject newRoot = new NavigationPath(this,
                next.getParent(), next.getLastSegment().toString());
		addDomain((AbstractDomainObject)newRoot);
		i++;
		for (; i < paths.size(); i++) {
			next = paths.get(i);
            newRoot = newRoot.join(next.getLastSegment().toString());
		}
		return newRoot;
	}

	boolean hasDomain(PathExpression path) {
		return _domains != null && _domains.contains(path);
	}

	protected <T extends AbstractDomainObject> T addDomain(T path) {
		if (_domains == null)
			_domains = new ArrayList<>();
		_domains.add(path);
		return path;
	}

	@Override
    public Subquery all() {
		return new AllExpression(this);
	}

	@Override
    public Subquery any() {
		return new AnyExpression(this);
	}

	@Override
    public Expression coalesce(Expression... exp) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression coalesce(String... exp) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression coalesce(Date... exp) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression coalesce(Calendar... exp) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression currentDate() {
		return new CurrentTimeExpression(Date.class);
	}

	@Override
    public Expression currentTime() {
		return new CurrentTimeExpression(Time.class);
	}

	@Override
    public Expression currentTimestamp() {
		return new CurrentTimeExpression(Timestamp.class);
	}

	@Override
    public Predicate exists() {
		return new ExistsExpression(this);
	}

	@Override
    public CaseExpression generalCase() {
		return new CaseExpressionImpl();
	}

	@Override
    public QueryDefinition groupBy(PathExpression... pathExprs) {
		if (_groupBys == null) {
			_groupBys = new ArrayList<>();
		} else {
			_groupBys.clear();
		}
		for (PathExpression e : pathExprs)
			_groupBys.add(e);
		return this;
	}

	@Override
    public QueryDefinition groupBy(List<PathExpression> pathExprList) {
		if (_groupBys == null) {
			_groupBys = new ArrayList<>();
		} else {
			_groupBys.clear();
		}
		for (PathExpression e : pathExprList)
			_groupBys.add(e);
		return this;
	}

	@Override
    public QueryDefinition having(Predicate predicate) {
		_having = predicate;
		return this;
	}

	@Override
    public Expression literal(String s) {
		return new LiteralExpression(s);
	}

	@Override
    public Expression literal(Number n) {
		return new LiteralExpression(n);
	}

	@Override
    public Expression literal(boolean b) {
		return new LiteralExpression(b);
	}

	@Override
    public Expression literal(Calendar c) {
		return new LiteralExpression(c);
	}

	@Override
    public Expression literal(Date d) {
		return new LiteralExpression(d);
	}

	@Override
    public Expression literal(char c) {
		return new LiteralExpression(c);
	}

	@Override
    public Expression literal(Class cls) {
		return new LiteralExpression(cls);
	}

	@Override
    public Expression literal(Enum<?> e) {
		return new LiteralExpression(e);
	}

	@Override
    public Expression nullLiteral() {
		return new LiteralExpression(null);
	}

	@Override
    public SelectItem newInstance(Class cls, SelectItem... args) {
		return new NewInstance(cls, args);
	}

	@Override
    public Expression nullif(Expression exp1, Expression exp2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression nullif(Number arg1, Number arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression nullif(String arg1, String arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression nullif(Date arg1, Date arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression nullif(Calendar arg1, Calendar arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression nullif(Class arg1, Class arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public Expression nullif(Enum<?> arg1, Enum<?> arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
    public QueryDefinition orderBy(OrderByItem... orderByItems) {
		if (_orderBys == null)
			_orderBys = new ArrayList<>();
		else
			_orderBys.clear();
		for (OrderByItem i : orderByItems) {
			if (i instanceof OrderableItem)
				_orderBys.add((OrderableItem)i);
			else
                _orderBys.add(new OrderableItem((ExpressionImpl)i));
		}
		return this;
	}

	@Override
    public QueryDefinition orderBy(List<OrderByItem> orderByItemList) {
		if (_orderBys == null)
			_orderBys = new ArrayList<>();
		else
			_orderBys.clear();
		for (OrderByItem i : orderByItemList) {
			if (i instanceof OrderableItem)
				_orderBys.add((OrderableItem)i);
			else
                _orderBys.add(new OrderableItem((ExpressionImpl)i, null));
		}
		return this;
	}

	@Override
    public Expression param(String name) {
		return new ParameterExpression(name);
	}

	@Override
    public Predicate predicate(boolean b) {
		return null;
	}

	@Override
    public QueryDefinition select(SelectItem... items) {
        return select(items == null ? null : Arrays.asList(items), false);
	}

	@Override
    public QueryDefinition select(List<SelectItem> items) {
		return select(items, false);
	}

	@Override
    public QueryDefinition selectDistinct(SelectItem... items) {
        return select(items == null ? null : Arrays.asList(items), true);
	}

	@Override
    public QueryDefinition selectDistinct(List<SelectItem> items) {
		return select(items, true);
	}

    private QueryDefinition select(List<SelectItem> items, boolean isDistinct) {
		if (_projections == null) {
			_projections = new ArrayList<>();
		} else {
			_projections.clear();
		}
		_distinct = isDistinct;
		for (SelectItem item : items)
			_projections.add(item);
		return this;
	}

	@Override
    public CaseExpression simpleCase(Expression caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public CaseExpression simpleCase(Number caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public CaseExpression simpleCase(String caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public CaseExpression simpleCase(Date caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public CaseExpression simpleCase(Calendar caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public CaseExpression simpleCase(Class caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public CaseExpression simpleCase(Enum<?> caseOperand) {
		return new CaseExpressionImpl(caseOperand);
	}

	@Override
    public Subquery some() {
		return new SomeExpression(this);
	}

	@Override
    public QueryDefinition where(Predicate predicate) {
		_where = predicate;
		return this;
	}

	private List<SelectItem> getProjections() {
		if (_projections == null) {
            List<SelectItem> defaultProjection = new ArrayList<>();
			defaultProjection.add(_domains.get(0));
			return defaultProjection;
		}
		return _projections;
	}

	@Override
	public String asExpression(AliasContext ctx) {
		ctx.push(this);
		StringBuilder buffer = new StringBuilder();
		registerDomains(ctx);
		String select = _distinct ? "SELECT DISTINCT " : "SELECT ";
        fillBuffer(select, buffer, ctx, getProjections(), Visit.PROJECTION);
		fillBuffer(" FROM ", buffer, ctx, _domains, Visit.JOINABLE);
		fillBuffer(" WHERE ", buffer, ctx, _where);
        fillBuffer(" GROUP BY ", buffer, ctx, _groupBys, Visit.EXPRESSION);
		fillBuffer(" HAVING ", buffer, ctx, _having);
        fillBuffer(" ORDER BY ", buffer, ctx, _orderBys, Visit.EXPRESSION);

		return buffer.toString();
	}

	@Override
    public String asProjection(AliasContext ctx) {
		return asExpression(ctx);
	}

    public void fillBuffer(String header, StringBuilder buffer, AliasContext ctx,
		List list, Visit visit) {
		if (list == null || list.isEmpty())
			return;
		buffer.append(header);
		for (int i = 0; i < list.size(); i++) {
			Visitable v = (Visitable)list.get(i);
			switch(visit) {
			case PROJECTION : buffer.append(v.asProjection(ctx))
                       .append(i != list.size()-1 ? ", " : " ");
				break;
			case EXPRESSION : buffer.append(v.asExpression(ctx))
                        .append(i != list.size()-1 ? ", " : " ");
				break;
            case JOINABLE   : buffer.append(i > 0 && v instanceof RootPath ?
                        ", " : " ").append(v.asJoinable(ctx));
				break;
			}
		}
	}

    public void fillBuffer(String header, StringBuilder buffer, AliasContext ctx,
			Predicate p) {
		if (p == null)
			return;
		Visitable v = (Visitable)p;
		buffer.append(header);
		buffer.append(v.asExpression(ctx));
	}

	/**
     * Registers each domain with an alias. Also set alias for order by items
	 * that are projected.
	 */
	private void registerDomains(AliasContext ctx) {
		if (_domains != null) {
			Collections.sort(_domains, new DomainSorter());
			for (AbstractDomainObject domain : _domains) {
				ctx.setAlias(domain);
			}
		}
		if (_orderBys != null) {
			for (OrderableItem o : _orderBys) {
				ExpressionImpl e = o.getExpression();
                if (_projections != null && _projections.contains(e))
					ctx.setAlias(e);
			}
		}
	}

	static class DomainSorter implements Comparator<AbstractDomainObject> {
		static List<Class> _order = Arrays.asList(new Class[] {
                RootPath.class, NavigationPath.class, OperatorPath.class,
				JoinPath.class, FetchPath.class, } );

        @Override
        public int compare(AbstractDomainObject a, AbstractDomainObject b) {
            return _order.indexOf(a.getClass()) - _order.indexOf(b.getClass());
		}
	}
}
