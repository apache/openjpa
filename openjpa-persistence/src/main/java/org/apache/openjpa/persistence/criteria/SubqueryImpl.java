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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.kernel.jpql.JPQLExpressionBuilder;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.persistence.meta.AbstractManagedType;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Subquery is an expression which itself is a query and always appears in the
 * context of a parent query. A subquery delegates to a captive query for most
 * of the operations but also maintains its own joins and correlated joins.
 *
 * @param <T> the type selected by this subquery.
 *
 * @author Pinaki Poddar
 * @author Fay Wang
 *
 * @since 2.0.0
 */
class SubqueryImpl<T> extends ExpressionImpl<T> implements Subquery<T> {
    private final AbstractQuery<?> _parent;
    private final CriteriaQueryImpl<T> _delegate;
    private final MetamodelImpl  _model;
    private org.apache.openjpa.kernel.exps.Subquery _subq;
    private List<Join<?,?>> _corrJoins = null;

    /**
     * Construct a subquery always in the context of a parent query.
     *
     * @param cls the result type of this subquery
     * @param parent the non-null parent query which itself can be a subquery.
     */
    SubqueryImpl(Class<T> cls, AbstractQuery<?> parent) {
        super(cls);
        _parent = parent;
        OrderedMap<Object, Class<?>> params;
        if (parent instanceof CriteriaQueryImpl) {
            _model = ((CriteriaQueryImpl<?>)parent).getMetamodel();
            params = ((CriteriaQueryImpl<?>)parent).getParameterTypes();
        } else if (parent instanceof SubqueryImpl) {
            _model = ((SubqueryImpl<?>)parent).getMetamodel();
            params = ((SubqueryImpl<?>)parent).getInnermostParent().getParameterTypes();
        } else {
            _model = null;
            params = null;
        }
        _delegate = new CriteriaQueryImpl<>(_model, this, params);
    }

    /**
     * Gets the parent query of this subquery.
     * Can be a query or another subquery.
     */
    @Override
    public AbstractQuery<?> getParent() {
        return _parent;
    }

    @Override
    public CommonAbstractCriteria getContainingQuery() {
        return getParent();
    }

    /**
     * Gets the captive query to which this subquery delegates.
     */
    CriteriaQueryImpl<T> getDelegate() {
        return _delegate;
    }

    public MetamodelImpl getMetamodel() {
        return _model;
    }

    Stack<Context> getContexts() {
        return getInnermostParent().getContexts();
    }

    /**
     * Gets the 'root' query for this subquery.
     */
    public CriteriaQueryImpl<?> getInnermostParent() {
        return (CriteriaQueryImpl<?>)(((_parent instanceof CriteriaQueryImpl)) ?
            _parent : ((SubqueryImpl<?>)_parent).getInnermostParent());
    }

    @Override
    public Subquery<T> select(Expression<T> expression) {
        _delegate.select(expression);
        return this;
    }

    @Override
    public Expression<T> getSelection() {
        return (Expression<T>)_delegate.getSelection();
    }

    @Override
    public <X> Root<X> from(EntityType<X> entity) {
        return _delegate.from(entity);
    }

    @Override
    public <X> Root<X> from(Class<X> entityClass) {
        return _delegate.from(entityClass);
    }

    @Override
    public Set<Root<?>> getRoots() {
        return _delegate.getRoots();
    }

    public Root<?> getRoot() {
        return _delegate.getRoot(false);
    }

    @Override
    public Subquery<T> where(Expression<Boolean> restriction) {
        _delegate.where(restriction);
        return this;
    }

    @Override
    public Subquery<T> where(Predicate... restrictions) {
        _delegate.where(restrictions);
        return this;
    }

    @Override
    public Subquery<T> groupBy(Expression<?>... grouping) {
        _delegate.groupBy(grouping);
        return this;
    }

    @Override
    public Subquery<T> groupBy(List<Expression<?>> grouping) {
        _delegate.groupBy(grouping);
        return this;
    }

    @Override
    public Subquery<T> having(Expression<Boolean> restriction) {
        _delegate.having(restriction);
        return this;
    }

    @Override
    public Subquery<T> having(Predicate... restrictions) {
        _delegate.having(restrictions);
        return this;
    }

    @Override
    public Subquery<T> distinct(boolean distinct) {
        _delegate.distinct(distinct);
        return this;
    }

    @Override
    public List<Expression<?>> getGroupList() {
        return _delegate.getGroupList();
    }

    @Override
    public Predicate getRestriction() {
        return _delegate.getRestriction();
    }

    @Override
    public Predicate getGroupRestriction() {
        return _delegate.getGroupRestriction();
    }

    @Override
    public boolean isDistinct() {
        return _delegate.isDistinct();
    }

    @Override
    public <U> Subquery<U> subquery(Class<U> type) {
        return new SubqueryImpl<>(type, this);
    }

    /**
     * Correlate this subquery with the given root.
     */
    @Override
    public <Y> Root<Y> correlate(Root<Y> root) {
        Types.Entity<Y> entity = (Types.Entity<Y>)root.getModel();
        RootImpl<Y> corrRoot = new RootImpl<>(entity);
        corrRoot.setCorrelatedPath((RootImpl<Y>)root);
        _delegate.addRoot(corrRoot);
        return corrRoot;
    }

    @Override
    public Set<Join<?,?>> getCorrelatedJoins() {
        return _corrJoins == null ? Collections.emptySet() : new CopyOnWriteArraySet(_corrJoins);
    }

    /**
     * Correlate this subquery with the given join.
     */
    @Override
    public <X,Y> Join<X,Y> correlate(Join<X,Y> parentJoin) {
        Join<?,?> corrJoin = Joins.clone(parentJoin);
        ((PathImpl<?,?>)corrJoin).setCorrelatedPath((PathImpl<?,?>)parentJoin);
        if (_corrJoins == null)
            _corrJoins = new ArrayList<>();
        _corrJoins.add(corrJoin);
        return (Join<X,Y>)corrJoin;
    }

    /**
     * Affirms if this is a correlated subquery.
     */
    public boolean isCorrelated() {
        return _corrJoins != null;
    }

    @Override
    public <X,Y> CollectionJoin<X,Y> correlate(CollectionJoin<X,Y> parentJoin) {
        Join corrJoin = Joins.clone((Joins.Collection)parentJoin);
        ((PathImpl<?,?>)corrJoin).setCorrelatedPath((PathImpl<?,?>)parentJoin);
        if (_corrJoins == null)
            _corrJoins = new ArrayList<>();
        _corrJoins.add(corrJoin);
        return (CollectionJoin<X,Y>)corrJoin;
    }

    @Override
    public <X,Y> SetJoin<X,Y> correlate(SetJoin<X,Y> parentJoin) {
        Join corrJoin = Joins.clone((Joins.Set)parentJoin);
        ((PathImpl<?,?>)corrJoin).setCorrelatedPath((PathImpl<?,?>)parentJoin);
        if (_corrJoins == null)
            _corrJoins = new ArrayList<>();
        _corrJoins.add(corrJoin);
        return (SetJoin<X,Y>)corrJoin;
    }

    @Override
    public <X,Y> ListJoin<X,Y> correlate(ListJoin<X,Y> parentJoin) {
        Join corrJoin = Joins.clone((Joins.List)parentJoin);
        ((PathImpl<?,?>)corrJoin).setCorrelatedPath((PathImpl<?,?>)parentJoin);
        if (_corrJoins == null)
            _corrJoins = new ArrayList<>();
        _corrJoins.add(corrJoin);
        return (ListJoin<X,Y>)corrJoin;
    }

    @Override
    public <X,K,V> MapJoin<X,K,V> correlate(MapJoin<X,K,V> parentJoin) {
        Join corrJoin = Joins.clone((Joins.Map)parentJoin);
        ((PathImpl<?,?>)corrJoin).setCorrelatedPath((PathImpl<?,?>)parentJoin);
        if (_corrJoins == null)
            _corrJoins = new ArrayList<>();
        _corrJoins.add(corrJoin);
        return (MapJoin<X,K,V>)corrJoin;
    }

    org.apache.openjpa.kernel.exps.Subquery getSubQ() {
        return _subq;
    }

    /**
     * Convert this path to a kernel path value.
     */
    @Override
    public Value toValue(ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        final boolean subclasses = true;
        CriteriaExpressionBuilder exprBuilder = new CriteriaExpressionBuilder();
        String alias = q.getAlias(this);
        ClassMetaData candidate = getCandidate();
        _subq = factory.newSubquery(candidate, subclasses, alias);
        _subq.setMetaData(candidate);
        Stack<Context> contexts = getContexts();
        Context context = new Context(null, _subq, contexts.peek());
        contexts.push(context);
        _delegate.setContexts(contexts);
        QueryExpressions subexp = exprBuilder.getQueryExpressions(factory, _delegate);
        _subq.setQueryExpressions(subexp);
        if (subexp.projections.length > 0)
            JPQLExpressionBuilder.checkEmbeddable(subexp.projections[0], null);
        contexts.pop();
        return _subq;
    }

    // if we are in a subquery against a collection from a
    // correlated parent, the candidate of the subquery
    // should be the class metadata of the collection element
    private ClassMetaData getCandidate() {
        if (getRoots().isEmpty() && _corrJoins != null) {
            FromImpl<?,?> corrJoin = (FromImpl<?,?>) _corrJoins.get(0);
            if (corrJoin.getJoins() != null) {
                FromImpl<?,?> join = (FromImpl<?,?>)corrJoin.getJoins().iterator().next();
                return getInnermostCandidate(join);
            }
        }

        RootImpl<?> root = (RootImpl<?>)getRoot();
        if (root != null && root.getCorrelatedPath() != null && !root.getJoins().isEmpty()) {
            FromImpl<?,?> join = (FromImpl<?,?>) root.getJoins().iterator().next();
            return getInnermostCandidate(join);
        }

        return ((AbstractManagedType<?>)root.getModel()).meta;
    }

    private ClassMetaData getInnermostCandidate(FromImpl<?,?> from) {
        if (!from.getJoins().isEmpty()) {
            from = (FromImpl<?,?>) from.getJoins().iterator().next();
            return getInnermostCandidate(from);
        }
        return getCandidate(from);
    }


    private ClassMetaData getCandidate(FromImpl<?,?> from) {
        return getFieldType(from._member.fmd);
    }

    private static ClassMetaData getFieldType(FieldMetaData fmd) {
        if (fmd == null)
            return null;

        ClassMetaData cmd = null;
        ValueMetaData vmd;

        if ((vmd = fmd.getElement()) != null)
            cmd = vmd.getDeclaredTypeMetaData();
        else if ((vmd = fmd.getKey()) != null)
            cmd = vmd.getDeclaredTypeMetaData();
        else if ((vmd = fmd.getValue()) != null)
            cmd = vmd.getDeclaredTypeMetaData();

        if (cmd == null || cmd.getDescribedType() == Object.class)
            cmd = fmd.getDeclaredTypeMetaData();
        if (cmd == null && fmd.isElementCollection())
            cmd = fmd.getDefiningMetaData();

        return cmd;
    }


    @Override
    public Class<T> getResultType() {
        return getJavaType();
    }

    @Override
    public StringBuilder asValue(AliasContext q) {
        StringBuilder buffer = new StringBuilder();
        _delegate.render(buffer, _delegate.getRoots(), _corrJoins);
        return buffer;
    }

    @Override
    public StringBuilder asVariable(AliasContext q) {
        return asValue(q);
    }
}
