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
import java.util.Map;
import java.util.Set;

import javax.persistence.Parameter;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.QueryBuilder;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

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
 * 
 * @since 2.0.0
 *
 */
@SuppressWarnings("serial")
public class CriteriaBuilder implements QueryBuilder, ExpressionParser {
    public static final String LANG_CRITERIA = "javax.persistence.criteria";

    private MetamodelImpl _model;

    public CriteriaBuilder setMetaModel(MetamodelImpl model) {
        _model = model;
        return this;
    }

    public QueryExpressions eval(Object parsed, ExpressionStoreQuery query,
        ExpressionFactory factory, ClassMetaData candidate) {
        CriteriaQueryImpl c = (CriteriaQueryImpl) parsed;
        return c.getQueryExpressions(factory);
    }
    
    public Value[] eval(String[] vals, ExpressionStoreQuery query,
        ExpressionFactory factory, ClassMetaData candidate) {
        throw new AbstractMethodError();
    }

    public String getLanguage() {
        return LANG_CRITERIA;
    }

    public Object parse(String ql, ExpressionStoreQuery query) {
        throw new AbstractMethodError();
    }

    public void populate(Object parsed, ExpressionStoreQuery query) {
        CriteriaQueryImpl c = (CriteriaQueryImpl) parsed;
        query.getContext().setCandidateType(c.getRoot().getJavaType(), true);
        query.setQuery(parsed);
    }

    public <N extends Number> Expression<N> abs(Expression<N> x) {
        return new Expressions.Abs<N>(x);
    }

    public <Y> Expression<Y> all(Subquery<Y> subquery) {
        throw new AbstractMethodError();
    }

    public Predicate and(Predicate... restrictions) {
    	return new PredicateImpl.And(restrictions);
    }

    public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
    	return new PredicateImpl.And(x,y);
    }

    public <Y> Expression<Y> any(Subquery<Y> subquery) {
        throw new AbstractMethodError();
    }

    public Order asc(Expression<?> x) {
        return new OrderImpl(x, true);
    }

    public <N extends Number> Expression<Double> avg(Expression<N> x) {
        return new Expressions.Avg(x);
    }

    public <Y extends Comparable<Y>> Predicate between(
        Expression<? extends Y> v, Expression<? extends Y> x,
        Expression<? extends Y> y) {
        return new Expressions.Between<Y>(v, x, y);
    }

    public <Y extends Comparable<Y>> Predicate between(
        Expression<? extends Y> v, Y x, Y y) {
        return new Expressions.Between<Y>(v,x,y);
    }

    public <T> Coalesce<T> coalesce() {
        return new Expressions.Coalesce();
    }

    public <Y> Expression<Y> coalesce(Expression<? extends Y> x,
        Expression<? extends Y> y) {
    	return new Expressions.Coalesce<Y>().value(x).value(y);
    }

    public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Y y) {
    	return new Expressions.Coalesce<Y>().value(x).value(y);
   }

    public Expression<String> concat(Expression<String> x, 
        Expression<String> y) {
    	return new Expressions.Concat(x, y);
    }

    public Expression<String> concat(Expression<String> x, String y) {
    	return new Expressions.Concat(x, y);
    }

    public Expression<String> concat(String x, Expression<String> y) {
    	return new Expressions.Concat(x, y);
    }

    public Predicate conjunction() {
        return new PredicateImpl.And();
    }

    public Expression<Long> count(Expression<?> x) {
        return new Expressions.Count(x);
    }

    public Expression<Long> countDistinct(Expression<?> x) {
        return new Expressions.Count(x, true);
    }

    public CriteriaQuery create() {
        return new CriteriaQueryImpl(_model);
    }

    public Expression<Date> currentDate() {
    	return new Expressions.CurrentDate();
    }

    public Expression<Time> currentTime() {
    	return new Expressions.CurrentTime();
    }

    public Expression<Timestamp> currentTimestamp() {
    	return new Expressions.CurrentTimestamp();
    }

    public Order desc(Expression<?> x) {
    	return new OrderImpl(x, false);
    }

    public <N extends Number> Expression<N> diff(Expression<? extends N> x,
        Expression<? extends N> y) {
        return new Expressions.Diff<N>(x, y);
    }

    public <N extends Number> Expression<N> diff(
        Expression<? extends N> x, N y) {
        return new Expressions.Diff<N>(x, y);
    }

    public <N extends Number> Expression<N> diff(N x, 
        Expression<? extends N> y) {
        return new Expressions.Diff<N>(x, y);
    }

    public Predicate disjunction() {
        return new PredicateImpl.Or();
    }

    public Predicate equal(Expression<?> x, Expression<?> y) {
        return new Expressions.Equal(x, y);
    }

    public Predicate equal(Expression<?> x, Object y) {
        return new Expressions.Equal(x, y);
    }

    public Predicate exists(Subquery<?> subquery) {
        throw new AbstractMethodError();
    }

    public <T> Expression<T> function(String name, Class<T> type,
        Expression<?>... args) {
        throw new AbstractMethodError();
    }

    public Predicate ge(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    public Predicate ge(Expression<? extends Number> x, Number y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    public <Y extends Comparable<Y>> Predicate greaterThan(
        Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.GreaterThan(x,y);
    }

    public <Y extends Comparable<Y>> Predicate greaterThan(
        Expression<? extends Y> x, Y y) {
        return new Expressions.GreaterThan(x, y);
    }

    public <Y extends Comparable<Y>> Predicate greaterThanOrEqualTo(
        Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    public <Y extends Comparable<Y>> Predicate greaterThanOrEqualTo(
        Expression<? extends Y> x, Y y) {
        return new Expressions.GreaterThanEqual(x,y);
    }

    public <X extends Comparable<X>> Expression<X> greatest(Expression<X> x) {
    	return new Expressions.Max<X>(x);
    }

    public Predicate gt(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.GreaterThan(x,y);
    }

    public Predicate gt(Expression<? extends Number> x, Number y) {
        return new Expressions.GreaterThan(x,y);
    }

    public <T> In<T> in(Expression<? extends T> expression) {
        return new Expressions.In<T>(expression);
    }

    public <C extends Collection<?>> Predicate isEmpty(
        Expression<C> collection) {
        return new Expressions.IsEmpty(collection);
    }

    public Predicate isFalse(Expression<Boolean> x) {
        return new Expressions.Equal(x, false);
    }

    public <E, C extends Collection<E>> Predicate isMember(E e,
        Expression<C> c) {
        return new Expressions.IsMember<E>(e, c);
    }

    public <E, C extends Collection<E>> Predicate isMember(Expression<E> e,
        Expression<C> c) {
        return new Expressions.IsMember<E>(e.getJavaType(), e, c);
    }

    public <C extends Collection<?>> Predicate isNotEmpty(
        Expression<C> collection) {
        return isEmpty(collection).negate();
    }

    public <E, C extends Collection<E>> Predicate isNotMember(E e,
        Expression<C> c) {
        return isMember(e, c).negate();
    }

    public <E, C extends Collection<E>> Predicate isNotMember(
        Expression<E> e, Expression<C> c) {
        return isMember(e, c).negate();
    }

    public Predicate isTrue(Expression<Boolean> x) {
        return new Expressions.Equal(x, false);
    }

    public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map) {
        throw new AbstractMethodError();
    }

    public Predicate le(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.LessThanEqual(x,y);
    }

    public Predicate le(Expression<? extends Number> x, Number y) {
        return new Expressions.LessThanEqual(x,y);
    }

    public <X extends Comparable<X>> Expression<X> least(Expression<X> x) {
        return new Expressions.Min<X>(x);
    }

    public Expression<Integer> length(Expression<String> x) {
        throw new AbstractMethodError();

    }

    public <Y extends Comparable<Y>> Predicate lessThan(
        Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.LessThan(x,y);
    }

    public <Y extends Comparable<Y>> Predicate lessThan(
        Expression<? extends Y> x, Y y) {
        return new Expressions.LessThan(x,y);

    }

    public <Y extends Comparable<Y>> Predicate lessThanOrEqualTo(
        Expression<? extends Y> x, Expression<? extends Y> y) {
        return new Expressions.LessThanEqual(x,y);
    }

    public <Y extends Comparable<Y>> Predicate lessThanOrEqualTo(
        Expression<? extends Y> x, Y y) {
        return new Expressions.LessThanEqual(x,y);
    }

    public Predicate like(Expression<String> x, Expression<String> pattern) {
        return new Expressions.Like(x,pattern);
    }

    public Predicate like(Expression<String> x, String pattern) {
        return new Expressions.Like(x,pattern);
    }

    public Predicate like(Expression<String> x, Expression<String> pattern,
        Expression<Character> escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    public Predicate like(Expression<String> x, Expression<String> pattern,
        char escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    public Predicate like(Expression<String> x, String pattern,
        Expression<Character> escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    public Predicate like(Expression<String> x, String pattern, 
        char escapeChar) {
        return new Expressions.Like(x,pattern,escapeChar);
    }

    public <T> Expression<T> literal(T value) {
        return new Expressions.Constant<T>(value);
    }

    public Expression<Integer> locate(Expression<String> x,
        Expression<String> pattern) {
        throw new AbstractMethodError();

    }

    public Expression<Integer> locate(Expression<String> x, String pattern) {
        throw new AbstractMethodError();

    }

    public Expression<Integer> locate(Expression<String> x,
        Expression<String> pattern, Expression<Integer> from) {
        throw new AbstractMethodError();

    }

    public Expression<Integer> locate(Expression<String> x, String pattern,
        int from) {
        throw new AbstractMethodError();

    }

    public Expression<String> lower(Expression<String> x) {
        throw new AbstractMethodError();

    }

    public Predicate lt(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.LessThan(x,y);
    }

    public Predicate lt(Expression<? extends Number> x, Number y) {
        return new Expressions.LessThan(x,y);
    }

    public <N extends Number> Expression<N> max(Expression<N> x) {
        return new Expressions.Max<N>(x);
    }

    public <N extends Number> Expression<N> min(Expression<N> x) {
        return new Expressions.Min<N>(x);
    }

    public Expression<Integer> mod(Expression<Integer> x, 
        Expression<Integer> y) {
        return new Expressions.Mod(x,y);
    }

    public Expression<Integer> mod(Expression<Integer> x, Integer y) {
        return new Expressions.Mod(x,y);
    }

    public Expression<Integer> mod(Integer x, Expression<Integer> y) {
        return new Expressions.Mod(x,y);
    }

    public <N extends Number> Expression<N> neg(Expression<N> x) {
        throw new AbstractMethodError();
    }

    public Predicate not(Expression<Boolean> restriction) {
        throw new AbstractMethodError();
    }

    public Predicate notEqual(Expression<?> x, Expression<?> y) {
        return equal(x, y).negate();
    }

    public Predicate notEqual(Expression<?> x, Object y) {
        return equal(x, y).negate();
    }

    public Predicate notLike(Expression<String> x, Expression<String> pattern) {
        return like(x, pattern).negate();
    }

    public Predicate notLike(Expression<String> x, String pattern) {
        return like(x, pattern).negate();
    }

    public Predicate notLike(Expression<String> x, Expression<String> pattern,
        Expression<Character> escapeChar) {
        return like(x, pattern, escapeChar).negate();
    }

    public Predicate notLike(Expression<String> x, Expression<String> pattern,
        char escapeChar) {
        return like(x, pattern, escapeChar).negate();
    }

    public Predicate notLike(Expression<String> x, String pattern,
        Expression<Character> escapeChar) {
        return like(x, pattern, escapeChar).negate();
    }

    public Predicate notLike(Expression<String> x, String pattern,
        char escapeChar) {
        return like(x, pattern, escapeChar).negate();
    }

    public <Y> Expression<Y> nullif(Expression<Y> x, Expression<?> y) {
        throw new AbstractMethodError();

    }

    public <Y> Expression<Y> nullif(Expression<Y> x, Y y) {
        throw new AbstractMethodError();

    }

    public Predicate or(Predicate... restrictions) {
        return new PredicateImpl.Or();
    }

    public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
    	return new PredicateImpl.Or(x,y);
    }

    public <T> Parameter<T> parameter(Class<T> paramClass) {
        return new ParameterImpl<T>(paramClass);
    }

    public <T> Parameter<T> parameter(Class<T> paramClass, String name) {
        return new ParameterImpl<T>(paramClass).setName(name);
    }

    public <N extends Number> Expression<N> prod(Expression<? extends N> x,
        Expression<? extends N> y) {
        return new Expressions.Product<N>(x,y);
    }

    public <N extends Number> Expression<N> prod(Expression<? extends N> x, 
    	N y) {
        return new Expressions.Product<N>(x,y);
    }

    public <N extends Number> Expression<N> prod(N x, 
        Expression<? extends N> y) {
        return new Expressions.Product<N>(x,y);
    }

    public Expression<Number> quot(Expression<? extends Number> x,
        Expression<? extends Number> y) {
        return new Expressions.Quotient<Number>(x,y);
    }

    public Expression<Number> quot(Expression<? extends Number> x, Number y) {
        return new Expressions.Quotient<Number>(x,y);
    }

    public Expression<Number> quot(Number x, Expression<? extends Number> y) {
        return new Expressions.Quotient<Number>(x,y);
    }

    public <Y> Selection<Y> select(Class<Y> result, 
        Selection<?>... selections) {
        throw new AbstractMethodError();
    }

    public <R> Case<R> selectCase() {
        return new Expressions.Case();
    }

    public <C, R> SimpleCase<C, R> selectCase(
        Expression<? extends C> expression) {
        return new Expressions.SimpleCase(expression);
    }

    public <C extends Collection<?>> Expression<Integer> size(C collection) {
        return new Expressions.Size(collection);
    }

    public <C extends Collection<?>> Expression<Integer> size(
        Expression<C> collection) {
        return new Expressions.Size(collection);
    }

    public <Y> Expression<Y> some(Subquery<Y> subquery) {
        throw new AbstractMethodError();
    }

    public Expression<Double> sqrt(Expression<? extends Number> x) {
        return new Expressions.Sqrt(x);
    }

    public Expression<String> substring(Expression<String> x,
        Expression<Integer> from) {
    	return new Expressions.Substring(x);
    }

    public Expression<String> substring(Expression<String> x, int from) {
        return new Expressions.Substring(x, from);
    }

    public Expression<String> substring(Expression<String> x,
        Expression<Integer> from, Expression<Integer> len) {
        return new Expressions.Substring(x, from, len);
    }

    public Expression<String> substring(Expression<String> x, int from, 
        int len) {
        return new Expressions.Substring(x, from, len);
    }

    public <N extends Number> Expression<N> sum(Expression<N> x) {
        return new Expressions.Sum<N>(x);
    }

    public <N extends Number> Expression<N> sum(Expression<? extends N> x,
        Expression<? extends N> y) {
        return new Expressions.Sum<N>(x,y);
    }

    public <N extends Number> Expression<N> sum(Expression<? extends N> x, 
        N y) {
        return new Expressions.Sum<N>(x,y);
    }

    public <N extends Number> Expression<N> sum(N x, 
        Expression<? extends N> y) {
        return new Expressions.Sum<N>(x,y);
    }

    public Expression<BigDecimal> toBigDecimal(
        Expression<? extends Number> number) {
        return new Expressions.Cast<BigDecimal>(number, BigDecimal.class);
    }

    public Expression<BigInteger> toBigInteger(
        Expression<? extends Number> number) {
        return new Expressions.Cast<BigInteger>(number, BigInteger.class);
    }

    public Expression<Double> toDouble(Expression<? extends Number> number) {
        return new Expressions.Cast<Double>(number, Double.class);
    }

    public Expression<Float> toFloat(Expression<? extends Number> number) {
        return new Expressions.Cast<Float>(number, Float.class);
    }

    public Expression<Integer> toInteger(Expression<? extends Number> number) {
        return new Expressions.Cast<Integer>(number, Integer.class);
    }

    public Expression<Long> toLong(Expression<? extends Number> number) {
        return new Expressions.Cast<Long>(number, Long.class);
    }

    public Expression<String> toString(Expression<Character> character) {
        return new Expressions.Cast<String>(character, String.class);
    }

    public Expression<String> trim(Expression<String> x) {
        return new Expressions.Trim(x);
    }

    public Expression<String> trim(Trimspec ts, Expression<String> x) {
        return new Expressions.Trim(x, ts);
    }

    public Expression<String> trim(Expression<Character> t, 
        Expression<String> x) {
        return new Expressions.Trim(x, t);
    }

    public Expression<String> trim(char t, Expression<String> x) {
        return new Expressions.Trim(x, t);
    }

    public Expression<String> trim(Trimspec ts, Expression<Character> t,
        Expression<String> x) {
        return new Expressions.Trim(x, t, ts);
    }

    public Expression<String> trim(Trimspec ts, char t, Expression<String> x) {
        return new Expressions.Trim(x, t, ts);
    }

    public Expression<String> upper(Expression<String> x) {
        throw new AbstractMethodError();

    }

    public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
        throw new AbstractMethodError();
    }
}
