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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Predicate.BooleanOperator;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.apache.openjpa.kernel.ExpressionStoreQuery;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.ExpressionParser;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Factory for Criteria query expressions.
 *
 * Acts as an adapter to OpenJPA ExpressionFactory.
 *
 * @author Pinaki Poddar
 * @author Fay Wang
 *
 * @since 2.0.0
 *
 */
public class CriteriaBuilderImpl implements OpenJPACriteriaBuilder, ExpressionParser {
    private static final long serialVersionUID = 1L;
    private MetamodelImpl _model;

    public OpenJPACriteriaBuilder setMetaModel(MetamodelImpl model) {
        _model = model;
        return this;
    }

    @Override
    public Metamodel getMetamodel() {
        return _model;
    }

    @Override
    public QueryExpressions eval(Object parsed, ExpressionStoreQuery query,
        ExpressionFactory factory, ClassMetaData candidate) {
        CriteriaQueryImpl<?> c = (CriteriaQueryImpl<?>) parsed;
        return c.getQueryExpressions(factory);
    }

    @Override
    public Value[] eval(String[] vals, ExpressionStoreQuery query,
        ExpressionFactory factory, ClassMetaData candidate) {
        return null;
    }

    @Override
    public String getLanguage() {
        return LANG_CRITERIA;
    }

    /**
     *  Create a Criteria query object with the specified result type.
     *  @param resultClass  type of the query result
     *  @return query object
     */
    @Override
    public <T> OpenJPACriteriaQuery<T> createQuery(Class<T> resultClass) {
        return new CriteriaQueryImpl<>(_model, resultClass);
    }

    /**
     *  Create a Criteria query object that returns a tuple of
     *  objects as its result.
     *  @return query object
     */
    @Override
    public OpenJPACriteriaQuery<Tuple> createTupleQuery() {
        return new CriteriaQueryImpl<>(_model, Tuple.class);
    }

    @Override
    public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public Object parse(String ql, ExpressionStoreQuery query) {
        throw new AbstractMethodError();
    }

    @Override
    public void populate(Object parsed, ExpressionStoreQuery query) {
        CriteriaQueryImpl<?> c = (CriteriaQueryImpl<?>) parsed;
        query.invalidateCompilation();
        query.getContext().setCandidateType(c.getRoot().getJavaType(), true);
        query.setQuery(parsed);
    }

    @Override
    public <N extends Number> Expression<N> abs(Expression<N> x) {
        return new Expressions.Abs<>(x);
    }

    @Override
    public <Y> Expression<Y> all(Subquery<Y> subquery) {
        return new Expressions.All<>(subquery);
    }

    @Override
    public Predicate and(Predicate... restrictions) {
    	return new PredicateImpl.And(restrictions);
    }

    @Override
    public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
    	return new PredicateImpl.And(x,y);
    }

    @Override
    public <Y> Expression<Y> any(Subquery<Y> subquery) {
        return new Expressions.Any<>(subquery);
    }

    @Override
    public Order asc(Expression<?> x) {
        return new OrderImpl(x, true);
    }

    @Override
    public <N extends Number> Expression<Double> avg(Expression<N> x) {
        return new Expressions.Avg(x);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> v, Expression<? extends Y> x,
        Expression<? extends Y> y) {
        return new Expressions.Between(v,x,y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> v, Y x, Y y) {
        return new Expressions.Between(v,x,y);
    }

    @Override
    public <T> Coalesce<T> coalesce() {
        return new Expressions.Coalesce(Object.class);
    }

    @Override
    public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
    	return new Expressions.Coalesce(x.getJavaType()).value(x).value(y);
    }

    @Override
    public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Y y) {
    	return new Expressions.Coalesce(x.getJavaType()).value(x).value(y);
   }

    @Override
    public Expression<String> concat(Expression<String> x, Expression<String> y) {
    	return new Expressions.Concat(x, y);
    }

    @Override
    public Expression<String> concat(Expression<String> x, String y) {
    	return new Expressions.Concat(x, y);
    }

    @Override
    public Expression<String> concat(String x, Expression<String> y) {
    	return new Expressions.Concat(x, y);
    }

    @Override
    public Predicate conjunction() {
        return new PredicateImpl.And();
    }

    @Override
    public Expression<Long> count(Expression<?> x) {
        return new Expressions.Count(x);
    }

    @Override
    public Expression<Long> countDistinct(Expression<?> x) {
        return new Expressions.Count(x, true);
    }

    @Override
    public OpenJPACriteriaQuery<Object> createQuery() {
        return new CriteriaQueryImpl<>(_model, Object.class);
    }

    @Override
    public Expression<Date> currentDate() {
    	return new Expressions.CurrentDate();
    }

    @Override
    public Expression<Time> currentTime() {
    	return new Expressions.CurrentTime();
    }

    @Override
    public Expression<Timestamp> currentTimestamp() {
    	return new Expressions.CurrentTimestamp();
    }

    @Override
    public Order desc(Expression<?> x) {
    	return new OrderImpl(x, false);
    }

    @Override
    public <N extends Number> Expression<N> diff(Expression<? extends N> x,
        Expression<? extends N> y) {
        return new Expressions.Diff<>(replaceExpressionForBinaryOperator(x), replaceExpressionForBinaryOperator(y));
    }

    @Override
    public <N extends Number> Expression<N> diff(
        Expression<? extends N> x, N y) {
        return new Expressions.Diff<>(replaceExpressionForBinaryOperator(x), y);
    }

    @Override
    public <N extends Number> Expression<N> diff(N x,
        Expression<? extends N> y) {
        return new Expressions.Diff<>(x, replaceExpressionForBinaryOperator(y));
    }

    @Override
    public Predicate disjunction() {
        return new PredicateImpl.Or();
    }

    @Override
    public Predicate equal(Expression<?> x, Expression<?> y) {
        if (y == null)
            return new Expressions.IsNull((ExpressionImpl<?> )x);
        return new Expressions.Equal(replaceExpressionForBinaryOperator(x), replaceExpressionForBinaryOperator(y));
    }

    private <T> Expression<T> replaceExpressionForBinaryOperator(final Expression<T> expression) {
        if (expression == PredicateImpl.TRUE()) {
            return (Expression<T>) PredicateImpl.TRUE_CONSTANT;
        }
        if (expression == PredicateImpl.FALSE()) {
            return (Expression<T>) PredicateImpl.FALSE_CONSTANT;
        }
        return expression;
    }

    @Override
    public Predicate equal(Expression<?> x, Object y) {
        if (y == null)
            return new Expressions.IsNull((ExpressionImpl<?> )x);
        return new Expressions.Equal(x, y);
    }

    @Override
    public Predicate exists(Subquery<?> subquery) {
        return new Expressions.Exists(subquery);
    }

    @Override
    public <T> Expression<T> function(String name, Class<T> type,
        Expression<?>... args) {
        return new Expressions.DatabaseFunction(name, type, args);
    }

    @Override
    public <X, T, V extends T> Join<X, V> treat(Join<X, T> join, Class<V> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <X, T, E extends T> CollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <X, T, E extends T> SetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <X, T, E extends T> ListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <X, K, T, V extends T> MapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <X, T extends X> Path<T> treat(Path<X> path, Class<T> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public <X, T extends X> Root<T> treat(Root<X> root, Class<T> type) {
        throw new UnsupportedOperationException("JPA 2.1");
    }

    @Override
    public Predicate ge(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    @Override
    public Predicate ge(Expression<? extends Number> x, Number y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThan(
        Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.GreaterThan(x,y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThan(
        Expression<? extends Y> x, Y y) {
        return new Expressions.GreaterThan(x, y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(
        Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(
        Expression<? extends Y> x, Y y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    @Override
    public <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> x) {
    	return new Expressions.Max<>(x);
    }

    @Override
    public Predicate gt(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.GreaterThan(x,y);
    }

    @Override
    public Predicate gt(Expression<? extends Number> x, Number y) {
        return new Expressions.GreaterThan(x,y);
    }

    @Override
    public <T> In<T> in(Expression<? extends T> expression) {
        return new Expressions.In<>(expression);
    }

    @Override
    public <C extends Collection<?>> Predicate isEmpty(Expression<C> collection) {
        return new Expressions.IsEmpty(collection);
    }

    @Override
    public Predicate isFalse(Expression<Boolean> x) {
        return new Expressions.Equal(x, false);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isMember(E e, Expression<C> c) {
        return new Expressions.IsMember<>(e, c);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isMember(Expression<E> e, Expression<C> c) {
        return new Expressions.IsMember<>(e, c);
    }

    @Override
    public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> collection) {
        return new Expressions.IsNotEmpty(collection);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isNotMember(E e, Expression<C> c) {
        return isMember(e, c).not();
    }

    @Override
    public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> e, Expression<C> c) {
        return isMember(e, c).not();
    }

    @Override
    public Predicate isTrue(Expression<Boolean> x) {
        if (x instanceof PredicateImpl) {
            PredicateImpl predicate = (PredicateImpl)x;
            if (predicate.isEmpty()) {
                return predicate.getOperator() == BooleanOperator.AND ? PredicateImpl.TRUE() : PredicateImpl.FALSE();
            }
        }
        return new Expressions.Equal(x, true);
    }

    @Override
    public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map) {
        return new Expressions.Constant<>(map == null ? Collections.EMPTY_SET : map.keySet());
    }

    @Override
    public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new Expressions.LessThanEqual(x,y);
    }

    @Override
    public Predicate le(Expression<? extends Number> x, Number y) {
        return new Expressions.LessThanEqual(x,y);
    }

    @Override
    public <X extends Comparable<? super X>> Expression<X> least(Expression<X> x) {
        return new Expressions.Min<>(x);
    }

    @Override
    public Expression<Integer> length(Expression<String> x) {
        return new Expressions.Length(x);

    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.LessThan(x,y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Y y) {
        return new Expressions.LessThan(x,y);

    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x,
        Expression<? extends Y> y) {
        return new Expressions.LessThanEqual(x,y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
        return new Expressions.LessThanEqual(x,y);
    }

    @Override
    public Predicate like(Expression<String> x, Expression<String> pattern) {
        return new Expressions.Like(x,pattern);
    }

    @Override
    public Predicate like(Expression<String> x, String pattern) {
        return new Expressions.Like(x,pattern);
    }

    @Override
    public Predicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    @Override
    public Predicate like(Expression<String> x, Expression<String> pattern, char escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    @Override
    public Predicate like(Expression<String> x, String pattern, Expression<Character> escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    @Override
    public Predicate like(Expression<String> x, String pattern, char escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    @Override
    public <T> Expression<T> literal(T value) {
        if (Boolean.TRUE.equals(value))
            return (Expression<T>)PredicateImpl.TRUE();
        if (Boolean.FALSE.equals(value))
            return (Expression<T>)PredicateImpl.FALSE();
        return new Expressions.Constant<>(value);
    }

    @Override
    public Expression<Integer> locate(Expression<String> x, Expression<String> pattern) {
        return new Expressions.Locate(x, pattern);
    }

    @Override
    public Expression<Integer> locate(Expression<String> x, String pattern) {
        return new Expressions.Locate(x, pattern);

    }

    @Override
    public Expression<Integer> locate(Expression<String> x, Expression<String> pattern, Expression<Integer> from) {
        return new Expressions.Locate(x, pattern, from);

    }

    @Override
    public Expression<Integer> locate(Expression<String> x, String pattern, int from) {
        return new Expressions.Locate(x, pattern, from);

    }

    @Override
    public Expression<String> lower(Expression<String> x) {
        return new Expressions.Lower(x);

    }

    @Override
    public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new Expressions.LessThan(x,y);
    }

    @Override
    public Predicate lt(Expression<? extends Number> x, Number y) {
        return new Expressions.LessThan(x,y);
    }

    @Override
    public <N extends Number> Expression<N> max(Expression<N> x) {
        return new Expressions.Max<>(x);
    }

    @Override
    public <N extends Number> Expression<N> min(Expression<N> x) {
        return new Expressions.Min<>(x);
    }

    @Override
    public Expression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
        return new Expressions.Mod(x,y);
    }

    @Override
    public Expression<Integer> mod(Expression<Integer> x, Integer y) {
        return new Expressions.Mod(x,y);
    }

    @Override
    public Expression<Integer> mod(Integer x, Expression<Integer> y) {
        return new Expressions.Mod(x,y);
    }

    @Override
    public <N extends Number> Expression<N> neg(Expression<N> x) {
        return new Expressions.Diff<>(0, x);
    }

    @Override
    public Predicate not(Expression<Boolean> restriction) {
        return ((Predicate)restriction).not();
    }

    @Override
    public Predicate notEqual(Expression<?> x, Expression<?> y) {
        return new Expressions.NotEqual(x, y);
    }

    @Override
    public Predicate notEqual(Expression<?> x, Object y) {
        return new Expressions.NotEqual(x, y);
    }

    @Override
    public Predicate notLike(Expression<String> x, Expression<String> pattern) {
        return like(x, pattern).not();
    }

    @Override
    public Predicate notLike(Expression<String> x, String pattern) {
        return like(x, pattern).not();
    }

    @Override
    public Predicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
        return like(x, pattern, escapeChar).not();
    }

    @Override
    public Predicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar) {
        return like(x, pattern, escapeChar).not();
    }

    @Override
    public Predicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
        return like(x, pattern, escapeChar).not();
    }

    @Override
    public Predicate notLike(Expression<String> x, String pattern, char escapeChar) {
        return like(x, pattern, escapeChar).not();
    }

    @Override
    public <Y> Expression<Y> nullif(Expression<Y> x, Expression<?> y) {
        return new Expressions.Nullif(x, y);
    }

    @Override
    public <Y> Expression<Y> nullif(Expression<Y> x, Y y) {
        return new Expressions.Nullif(x, y);
    }

    @Override
    public Predicate or(Predicate... restrictions) {
        return new PredicateImpl.Or(restrictions);
    }

    @Override
    public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
        return new PredicateImpl.Or(x,y);
    }

    /**
     * Construct a ParameterExpression with a null name as key.
     * The name of this parameter will be assigned automatically
     * when this parameter expression is
     * {@linkplain CriteriaQueryImpl#registerParameter(ParameterExpressionImpl)
     * registered} in a Criteriaquery during tree traversal.
     *
     * @see ParameterExpressionImpl#assignAutoName(String)
     */
    @Override
    public <T> ParameterExpression<T> parameter(Class<T> paramClass) {
        return new ParameterExpressionImpl<>(paramClass, null);
    }

    @Override
    public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name) {
        return new ParameterExpressionImpl<>(paramClass, name);
    }

    @Override
    public <N extends Number> Expression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
        return new Expressions.Product<>(x,y);
    }

    @Override
    public <N extends Number> Expression<N> prod(Expression<? extends N> x, N y) {
        return new Expressions.Product<>(x,y);
    }

    @Override
    public <N extends Number> Expression<N> prod(N x, Expression<? extends N> y) {
        return new Expressions.Product<>(x,y);
    }

    @Override
    public Expression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new Expressions.Quotient<>(x,y);
    }

    @Override
    public Expression<Number> quot(Expression<? extends Number> x, Number y) {
        return new Expressions.Quotient<>(x,y);
    }

    @Override
    public Expression<Number> quot(Number x, Expression<? extends Number> y) {
        return new Expressions.Quotient<>(x,y);
    }

    /**
     * Define a select list item corresponding to a constructor.
     * @param result  class whose instance is to be constructed
     * @param selections  arguments to the constructor
     * @return selection item
     */
    @Override
    public <Y> CompoundSelection<Y> construct(Class<Y> result, Selection<?>... selections) {
        return new CompoundSelections.NewInstance<>(result, selections);
    }

    @Override
    public <R> Case<R> selectCase() {
        return new Expressions.Case(Object.class);
    }

    @Override
    public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
        return new Expressions.SimpleCase(expression);
    }

    @Override
    public <C extends Collection<?>> Expression<Integer> size(C collection) {
        return new Expressions.Size(collection);
    }

    @Override
    public <C extends Collection<?>> Expression<Integer> size(Expression<C> collection) {
        return new Expressions.Size(collection);
    }

    @Override
    public <Y> Expression<Y> some(Subquery<Y> subquery) {
        //some and any are synonymous
        return new Expressions.Any<>(subquery);
    }

    @Override
    public Expression<Double> sqrt(Expression<? extends Number> x) {
        return new Expressions.Sqrt(x);
    }

    @Override
    public Expression<String> substring(Expression<String> x, Expression<Integer> from) {
    	return new Expressions.Substring(x, from);
    }

    @Override
    public Expression<String> substring(Expression<String> x, int from) {
        return new Expressions.Substring(x, from);
    }

    @Override
    public Expression<String> substring(Expression<String> x, Expression<Integer> from, Expression<Integer> len) {
        return new Expressions.Substring(x, from, len);
    }

    @Override
    public Expression<String> substring(Expression<String> x, int from, int len) {
        return new Expressions.Substring(x, from, len);
    }

    @Override
    public <N extends Number> Expression<N> sum(Expression<N> x) {
        return new Expressions.Sum<>(x);
    }

    @Override
    public <N extends Number> Expression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
        return new Expressions.Sum<>(x,y);
    }

    @Override
    public <N extends Number> Expression<N> sum(Expression<? extends N> x, N y) {
        return new Expressions.Sum<>(x,y);
    }

    @Override
    public <N extends Number> Expression<N> sum(N x, Expression<? extends N> y) {
        return new Expressions.Sum<>(x,y);
    }

    @Override
    public Expression<Long> sumAsLong(Expression<Integer> x) {
        return sum(x).as(Long.class);
    }

    @Override
    public Expression<Double> sumAsDouble(Expression<Float> x) {
        return sum(x).as(Double.class);
    }

    @Override
    public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> number) {
        return new Expressions.Cast<>(number, BigDecimal.class);
    }

    @Override
    public Expression<BigInteger> toBigInteger(Expression<? extends Number> number) {
        return new Expressions.Cast<>(number, BigInteger.class);
    }

    @Override
    public Expression<Double> toDouble(Expression<? extends Number> number) {
        return new Expressions.Cast<>(number, Double.class);
    }

    @Override
    public Expression<Float> toFloat(Expression<? extends Number> number) {
        return new Expressions.Cast<>(number, Float.class);
    }

    @Override
    public Expression<Integer> toInteger(Expression<? extends Number> number) {
        return new Expressions.Cast<>(number, Integer.class);
    }

    @Override
    public Expression<Long> toLong(Expression<? extends Number> number) {
        return new Expressions.Cast<>(number, Long.class);
    }

    @Override
    public Expression<String> toString(Expression<Character> character) {
        return new Expressions.Cast<>(character, String.class);
    }

    @Override
    public Expression<String> trim(Expression<String> x) {
        return new Expressions.Trim(x);
    }

    @Override
    public Expression<String> trim(Trimspec ts, Expression<String> x) {
        return new Expressions.Trim(x, ts);
    }

    @Override
    public Expression<String> trim(Expression<Character> t, Expression<String> x) {
        return new Expressions.Trim(x, t);
    }

    @Override
    public Expression<String> trim(char t, Expression<String> x) {
        return new Expressions.Trim(x, t);
    }

    @Override
    public Expression<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x) {
        return new Expressions.Trim(x, t, ts);
    }

    @Override
    public Expression<String> trim(Trimspec ts, char t, Expression<String> x) {
        return new Expressions.Trim(x, t, ts);
    }

    @Override
    public Expression<String> upper(Expression<String> x) {
        return new Expressions.Upper(x);

    }

    @Override
    public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
        return new Expressions.Constant<>(map == null ? Collections.EMPTY_LIST : map.values());
    }

    @Override
    public CompoundSelection<Object[]> array(Selection<?>... terms) {
        return new CompoundSelections.Array<>(Object[].class, terms);
    }

    @Override
    public Predicate isNotNull(Expression<?> x) {
        return new Expressions.IsNotNull((ExpressionImpl<?>)x);
    }

    @Override
    public Predicate isNull(Expression<?> x) {
        return new Expressions.IsNull((ExpressionImpl<?> )x);
    }

    @Override
    public <T> Expression<T> nullLiteral(Class<T> t) {
        return new Expressions.Constant<>(t, (T)null);
    }


    /**
     * Define a tuple-valued selection item
     * @param selections  selection items
     * @return tuple-valued compound selection
     * @throws IllegalArgumentException if an argument is a tuple- or
     *          array-valued selection item
     */
    @Override
    public CompoundSelection<Tuple> tuple(Selection<?>... selections) {
        return new CompoundSelections.Tuple(selections);
    }

    /**
     * Create a predicate based upon the attribute values of a given
     * "example" entity instance. The predicate is the conjunction
     * or disjunction of predicates for subset of attribute of the entity.
     * <br>
     * By default, all the singular entity attributes (the basic, embedded
     * and uni-cardinality relations) that have a non-null or non-default
     * value for the example instance and are not an identity or version
     * attribute are included. The comparable attributes can be further
     * pruned by specifying variable list of attributes as the final argument.
     *
     * @param example an instance of an entity class
     *
     * @param style specifies various aspects of comparison such as whether
     * non-null attribute values be included, how string-valued attribute be
     * compared, whether the individual attribute based predicates are ANDed
     * or ORed etc.
     *
     * @param excludes list of attributes that are excluded from comparison.
     *
     * @return a predicate
     */
    @Override
    public <T> Predicate qbe(From<?, T> from, T example, ComparisonStyle style, Attribute<?,?>... excludes) {
        if (from == null)
            throw new NullPointerException();
        if (example == null) {
            return from.isNull();
        }
        ManagedType<T> type = (ManagedType<T>)_model.managedType(example.getClass());
        return new CompareByExample<>(this, type, from, example,
            style == null ? qbeStyle() : style, excludes);
    }

    @Override
    public <T> Predicate qbe(From<?, T> from, T example, ComparisonStyle style) {
        return qbe(from, example, style, null);
    }

    @Override
    public <T> Predicate qbe(From<?, T> from, T example, Attribute<?,?>... excludes) {
        return qbe(from, example, qbeStyle(), excludes);
    }

    @Override
    public <T> Predicate qbe(From<?, T> from, T example) {
        return qbe(from, example, qbeStyle(), null);
    }

    /**
     * Create a style to tune different aspects of comparison by example.
     */
    @Override
    public ComparisonStyle qbeStyle() {
        return new ComparisonStyle.Default();
    }
}
