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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.kernel.jpql.JPQLExpressionBuilder;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.meta.AbstractManagedType;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Subquery is an expression which itself is a query and always appears in the
 * context of a parent query.
 * 
 * @author Pinaki Poddar
 * @author Fay Wang
 * 
 * @param <T> the type selected by this subquery.
 */
public class SubqueryImpl<T> extends ExpressionImpl<T> implements Subquery<T> {
    private final AbstractQuery<?> _parent;
    private final CriteriaQueryImpl<T> _delegate;
    private final MetamodelImpl  _model;
    private java.util.Set<Join<?,?>> _joins;
    private Expression<T> _select;
    private org.apache.openjpa.kernel.exps.Subquery _subq;
    
    public SubqueryImpl(Class<T> cls, AbstractQuery<?> parent) {
        super(cls);
        _parent = parent;
        if (parent instanceof CriteriaQueryImpl) {
            _model = ((CriteriaQueryImpl<?>)parent).getMetamodel();
        } else if (parent instanceof SubqueryImpl) {
            _model = ((SubqueryImpl<?>)parent).getMetamodel();
        } else {
            _model = null;
        }
        _delegate = new CriteriaQueryImpl<T>(_model, this);
    }
    
    public AbstractQuery<?> getParent() {
        return _parent;
    }
    
    public CriteriaQueryImpl<T> getDelegate() {
        return _delegate;
    }
    
    public MetamodelImpl getMetamodel() {
        return _model;
    }
    
    //public Stack<Context> getContexts() {
    //    return getInnermostParent().getContexts();
    //}
    
    public CriteriaQueryImpl<?> getInnermostParent() {
        return (CriteriaQueryImpl<?>)(((_parent instanceof CriteriaQueryImpl)) ? 
            _parent : ((SubqueryImpl<?>)_parent).getInnermostParent());
    }

    public Subquery<T> select(Expression<T> expression) {
        _select = expression;
        _delegate.select(expression);
        return this;
    }
    
    public Expression<T> getSelection() {
        return _select;
    }
    
    public <X> Root<X> from(EntityType<X> entity) {
        return _delegate.from(entity);
    }

    public <X> Root<X> from(Class<X> entityClass) {
        return _delegate.from(entityClass);
    }

    public Set<Root<?>> getRoots() {
        return _delegate.getRoots();
    }
    
    public Root<?> getRoot() {
        return _delegate.getRoot();
    }    

    public Subquery<T> where(Expression<Boolean> restriction) {
        _delegate.where(restriction);
        return this;
    }

    public Subquery<T> where(Predicate... restrictions) {
        _delegate.where(restrictions);
        return this;
    }

    public Subquery<T> groupBy(Expression<?>... grouping) {
        _delegate.groupBy(grouping);
        return this;
    }

    public Subquery<T> having(Expression<Boolean> restriction) {
        _delegate.having(restriction);
        return this;
    }

    public Subquery<T> having(Predicate... restrictions) {
        _delegate.having(restrictions);
        return this;
    }

    public Subquery<T> distinct(boolean distinct) {
        _delegate.distinct(distinct);
        return this;
    }

    public List<Expression<?>> getGroupList() {
        return _delegate.getGroupList();
    }

    public Predicate getRestriction() {
        return _delegate.getRestriction();
    }

    public Predicate getGroupRestriction() {
        return _delegate.getGroupRestriction();
    }

    public boolean isDistinct() {
        return _delegate.isDistinct();
    }

    public <U> Subquery<U> subquery(Class<U> type) {
        return new SubqueryImpl<U>(type, _delegate);
    }
    
    public <Y> Root<Y> correlate(Root<Y> root) {
        Types.Entity<Y> entity = (Types.Entity<Y>)root.getModel();
        RootImpl<Y> corrRoot = new RootImpl<Y>(entity);
        corrRoot.setCorrelatedParent((RootImpl<Y>)root);
        Set<Root<?>> roots = getRoots();
        if (roots == null) {
            roots = new LinkedHashSet<Root<?>>();
            _delegate.setRoots(roots);
        }
        roots.add(corrRoot);
        return corrRoot;
    }
    
    public <X,Y> Join<X,Y> correlate(Join<X,Y> join) {
        _delegate.from(join.getModel().getBindableJavaType());
        return join;
    }
    public <X,Y> CollectionJoin<X,Y> correlate(CollectionJoin<X,Y> join) {
        _delegate.from(join.getModel().getBindableJavaType());
        return join;
    }
    
    public <X,Y> SetJoin<X,Y> correlate(SetJoin<X,Y> join) {
        _delegate.from(join.getModel().getBindableJavaType());
        return join;
    }
    
    public <X,Y> ListJoin<X,Y> correlate(ListJoin<X,Y> join) {
        _delegate.from(join.getModel().getBindableJavaType());
        return join;
    }
    
    public <X,K,V> MapJoin<X,K,V> correlate(MapJoin<X,K,V> join) {
        _delegate.from(join.getModel().getBindableJavaType());
        return join;
    }
    
    public java.util.Set<Join<?, ?>> getJoins() {
        return _joins;
    }
    
    public org.apache.openjpa.kernel.exps.Subquery getSubQ() {
        return _subq;
    }

    /**
     * Convert this path to a kernel path value.
     */
    @Override
    public Value toValue(ExpressionFactory factory, MetamodelImpl model,
        CriteriaQueryImpl<?> q) {
        final boolean subclasses = true;
        CriteriaExpressionBuilder queryEval = new CriteriaExpressionBuilder();
        String alias = q.getAlias(this);
        ClassMetaData candidate = getCandidate(); 
        _subq = factory.newSubquery(candidate, subclasses, alias);
        _subq.setMetaData(candidate);
        //TODO:
        //Stack<Context> contexts = getContexts();
        //Context context = new Context(null, _subq, contexts.peek());
        //contexts.push(context);
        //_delegate.setContexts(contexts);
        QueryExpressions subexp = queryEval.getQueryExpressions(factory, 
                _delegate);
        _subq.setQueryExpressions(subexp);
        if (subexp.projections.length > 0)
            JPQLExpressionBuilder.checkEmbeddable(subexp.projections[0], null);
        //contexts.pop();
        return _subq;
    }
    
    // if we are in a subquery against a collection from a 
    // correlated parent, the candidate of the subquery
    // should be the class metadata of the collection element 
    private ClassMetaData getCandidate() {
        RootImpl<?> root = (RootImpl<?>)getRoot();
        RootImpl<?> correlatedRoot = (RootImpl<?>)root.getCorrelatedParent();
        if (correlatedRoot != null && root.getJoins() != null) {
           Join<?,?> join = root.getJoins().iterator().next();
           FieldMetaData fmd = ((Members.Member<?, ?>)join.getAttribute()).fmd;
           if (join.getAttribute().isCollection()) {
               return fmd.isElementCollection() ? fmd.getEmbeddedMetaData(): fmd.getElement().getDeclaredTypeMetaData();
           } else {
               return fmd.getDeclaredTypeMetaData();
           }
        }
        return ((AbstractManagedType<?>)root.getModel()).meta;
    }
    
}
