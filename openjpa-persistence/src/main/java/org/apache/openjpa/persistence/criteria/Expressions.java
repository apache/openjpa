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
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.QueryBuilder;
import javax.persistence.criteria.Subquery;
import javax.persistence.criteria.QueryBuilder.Trimspec;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.kernel.jpql.JPQLExpressionBuilder;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Expressions according to JPA 2.0.
 * 
 * A facade to OpenJPA kernel expressions to enforce stronger typing.
 * 
 * @author Pinaki Poddar
 * @author Fay Wang
 * 
 * @since 2.0.0
 *
 */
public class Expressions {
	
    /**
     * Convert the given Criteria expression to a corresponding kernel value 
     * using the given ExpressionFactory.
     * Handles null expression.
     */
     static Value toValue(ExpressionImpl<?> e, ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
        return (e == null) ? factory.getNull() : e.toValue(factory, model, q);
    }
     
     static void setImplicitTypes(Value v1, Value v2, Class<?> expected, CriteriaQueryImpl<?> q) {
         JPQLExpressionBuilder.setImplicitTypes(v1, v2, expected, q.getMetamodel(), 
             q.getParameterTypes(), q.toString());
     }
    
    /**
     * Unary Functional Expression applies a unary function on a input operand Expression.
     *
     * @param <X> the type of the resultant expression
     */
    public abstract static class UnaryFunctionalExpression<X> extends ExpressionImpl<X> {
        protected final ExpressionImpl<?> e;
        /**
         * Supply the resultant type and input operand expression.
         */
        public UnaryFunctionalExpression(Class<X> t, Expression<?> e) {
            super(t);
            this.e  = (ExpressionImpl<?>)e;
        }
        
        public UnaryFunctionalExpression(Expression<X> e) {
            this(e.getJavaType(), e);
        }
    }
    
    /**
     * Binary Functional Expression applies a binary function on a pair of input Expression.
     * 
     * @param <X> the type of the resultant expression
     */
    public abstract static class BinarayFunctionalExpression<X> extends ExpressionImpl<X>{
        protected final ExpressionImpl<?> e1;
        protected final ExpressionImpl<?> e2;
        
        /**
         * Supply the resultant type and pair of input operand expressions.
         */
        public BinarayFunctionalExpression(Class<X> t, Expression<?> x, Expression<?> y) {
            super(t);
            e1 = (ExpressionImpl<?>)x;
            e2 = (ExpressionImpl<?>)y;
        }
    }
    
    /**
     * Functional Expression applies a function on a list of input Expressions.
     * 
     * @param <X> the type of the resultant expression
     */
    public abstract static class FunctionalExpression<X> extends ExpressionImpl<X> {
        protected final ExpressionImpl<?>[] args;
        
        /**
         * Supply the resultant type and list of input operand expressions.
         */
        public FunctionalExpression(Class<X> t, Expression<?>... args) {
            super(t);
            int len = args == null ? 0 : args.length;
            this.args = new ExpressionImpl<?>[len];
            for (int i = 0; args != null && i < args.length; i++) {
                this.args[i] = (ExpressionImpl<?>)args[i];
            }
        }
    }
   
    /**
     * Binary Logical Expression applies a function on a pair of input Expression to generate a Predicate
     * i.e. an expression whose resultant type is Boolean.
     *
     */
   public static class BinaryLogicalExpression extends PredicateImpl {
        protected final ExpressionImpl<?> e1;
        protected final ExpressionImpl<?> e2;
        
        public BinaryLogicalExpression(Expression<?> x, Expression<?> y) {
            super();
            e1 = (ExpressionImpl<?>)x;
            e2 = (ExpressionImpl<?>)y;
        }
        
        @Override
        public PredicateImpl clone() {
            return new BinaryLogicalExpression(e1, e2);
        }
    }
    
    
    public static class Abs<X> extends UnaryFunctionalExpression<X> {
        public  Abs(Expression<X> x) {
            super(x);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.abs(Expressions.toValue(e, factory, model, q));
        }
    }
    
    public static class Count extends UnaryFunctionalExpression<Long> {
        private boolean _distinct; 
        public  Count(Expression<?> x) {
            this(x, false);
        }
        
        public  Count(Expression<?> x, boolean distinct) {
            super(Long.class, x);
            _distinct = distinct;
            
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Value v = factory.count(Expressions.toValue(e, factory, model, q));
            return _distinct ? factory.distinct(v) : v;
        }
    }

    public static class Avg extends UnaryFunctionalExpression<Double> {
        public  Avg(Expression<?> x) {
            super(Double.class, x);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.avg(Expressions.toValue(e, factory, model, q));
        }
    }
    
    public static class Sqrt extends UnaryFunctionalExpression<Double> {
        public  Sqrt(Expression<? extends Number> x) {
            super(Double.class, x);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.sqrt(Expressions.toValue(e, factory, model, q));
        }
    }
    
    public static class Max<X> extends UnaryFunctionalExpression<X> {
        public  Max(Expression<X> x) {
            super(x);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.max(Expressions.toValue(e, factory, model, q));
        }
    }

    public static class Min<X> extends UnaryFunctionalExpression<X> {
        public  Min(Expression<X> x) {
            super(x);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.min(Expressions.toValue(e, factory, model, q));
        }
    }
    
    public static class Size extends UnaryFunctionalExpression<Integer> {
        public  Size(Expression<? extends Collection<?>> x) {
            super(Integer.class, x);
        }
        
        public  Size(Collection<?> x) {
            this(new Constant<Collection<?>>(x));
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Value val = Expressions.toValue(e, factory, model, q);
            if (val instanceof Literal && ((Literal)val).getParseType() == Literal.TYPE_COLLECTION)
                return factory.newLiteral(((Collection)((Literal)val).getValue()).size(), 
                    Literal.TYPE_NUMBER);
                
            return factory.size(val);
        }
    }
    
    public static class DatabaseFunction<T> extends FunctionalExpression<T> {
        private final String functionName;
        private final Class<T> resultType;
        public  DatabaseFunction(String name, Class<T> resultType, Expression<?>... exps) {
            super(resultType, exps);
            functionName = name;
            this.resultType = resultType;
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.newFunction(functionName, getJavaType(), 
                new Expressions.ListArgument(resultType, args).toValue(factory, model, q));
        }
    }

    
    public static class Type<X extends Class> extends UnaryFunctionalExpression<X> {
        public Type(PathImpl<?, ?> path) {
            super((Class)Class.class, path);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.type(Expressions.toValue(e, factory, model, q));
        }
    }

    public static class Cast<B> extends UnaryFunctionalExpression<B> {
        public Cast(Expression<?> x, Class<B> b) {
            super(b, x);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.cast(Expressions.toValue(e, factory, model, q), getJavaType());
        }
    }
    
    public static class Concat extends BinarayFunctionalExpression<String> {
        public Concat(Expression<String> x, Expression<String> y) {
            super(String.class, x, y);
        }
        
        public Concat(Expression<String> x, String y) {
            this(x, new Constant<String>(y));
        }
        
        public Concat(String x, Expression<String> y) {
            this(new Constant<String>(x), y);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.concat(
                Expressions.toValue(e1, factory, model, q), 
                Expressions.toValue(e2, factory, model, q));
        }
    }
    
    public static class Substring extends UnaryFunctionalExpression<String> {
        private ExpressionImpl<Integer> from;
        private ExpressionImpl<Integer> len;
        
        public Substring(Expression<String> s, Expression<Integer> from, Expression<Integer> len) {
            super(s);
            this.from = (ExpressionImpl<Integer>)from;
            this.len  = (ExpressionImpl<Integer>)len;
        }
        
        public Substring(Expression<String> s, Expression<Integer> from) {
            this(s, (ExpressionImpl<Integer>)from, null);
        }

        public Substring(Expression<String> s) {
            this(s, (Expression<Integer>)null, (Expression<Integer>)null);
        }
        
        public Substring(Expression<String> s, Integer from) {
            this(s, new Constant<Integer>(from), null);
        }
        
        public Substring(Expression<String> s, Integer from, Integer len) {
            this(s, new Constant<Integer>(from), new Constant<Integer>(len));
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return JPQLExpressionBuilder.convertSubstringArguments(factory, 
                Expressions.toValue(e, factory, model, q), 
                from == null ? null : from.toValue(factory, model, q), 
                len == null ? null : len.toValue(factory, model, q));
        }
    }

    public static class Locate extends ExpressionImpl<Integer> {
        private ExpressionImpl<String> pattern;
        private ExpressionImpl<Integer> from;
        private ExpressionImpl<String> path;
        
        public Locate(Expression<String> x, Expression<String> y, Expression<Integer> from) {
            super(Integer.class);
            path = (ExpressionImpl<String>)x;
            pattern = (ExpressionImpl<String>)y;
            this.from = (ExpressionImpl<Integer>)from;
        }

        public Locate(Expression<String> x, Expression<String> y) {
            this(x, y, null);
         }
        
        public Locate(Expression<String> x, String y) {
            this(x, new Constant<String>(y), null);
        }
        
        public Locate(String x, Expression<String> y) {
            this(new Constant<String>(x), y, null);
        }
        
        public Locate(Expression<String> x, String y, int from) {
            this(x, new Constant<String>(y), new Constant<Integer>(from));
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Value locateSearch = path.toValue(factory, model, q);
            Value locateFromIndex = (from == null ? null : Expressions.toValue(from, factory, model, q));
            Value locatePath = Expressions.toValue(pattern, factory, model, q);
            
            return factory.add(factory.indexOf(locateSearch,
                    locateFromIndex == null ? locatePath
                        : factory.newArgumentList(locatePath,
                            factory.subtract(locateFromIndex, 
                                             factory.newLiteral(Integer.valueOf(1), Literal.TYPE_NUMBER)))),
                                             factory.newLiteral(Integer.valueOf(1), Literal.TYPE_NUMBER));
        }
    }
    
    public static class Trim extends BinarayFunctionalExpression<String> {
        static Expression<Character> defaultTrim = new Constant<Character>(Character.class, new Character(' '));
        static Trimspec defaultSpec = Trimspec.BOTH;
        private Trimspec ts;
        
        public Trim(Expression<String> x, Expression<Character> y, 
            Trimspec ts) {
            super(String.class, x, y);
            this.ts = ts;
        }
        
        public Trim(Expression<String> x, Expression<Character> y) {
            this(x, y, defaultSpec);
        }
        
        public Trim(Expression<String> x) {
            this(x, defaultTrim, defaultSpec);
        }
        
        public Trim(Expression<String> x, Character t) {
            this(x, new Constant<Character>(Character.class, t), defaultSpec);
        }
        
        public Trim(Expression<String> x, Character t, Trimspec ts) {
            this(x, new Constant<Character>(Character.class, t), ts);
        }
        
        public Trim(Expression<String> x, Trimspec ts) {
            this(x, defaultTrim, ts);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Boolean spec = null;
            if (ts != null) {
                switch (ts) {
                case LEADING  : spec = true;  break;
                case TRAILING : spec = false; break;
                case BOTH     : spec = null;  break;
                }
            }
            return factory.trim(
                Expressions.toValue(e1, factory, model, q), 
                Expressions.toValue(e2, factory, model, q), spec);
        }
    }
    
    public static class Sum<N extends Number> extends BinarayFunctionalExpression<N> {
        public Sum(Expression<? extends Number> x, Expression<? extends Number> y) {
            super((Class<N>)x.getJavaType(), x, y);
        }
        
        public Sum(Expression<? extends Number> x) {
            this(x, (Expression<? extends Number>)null);
        }

        public Sum(Expression<? extends Number> x, Number y) {
            this(x, new Constant<Number>(Number.class, y));
        }

        public Sum(Number x, Expression<? extends Number> y) {
            this(new Constant<Number>(Number.class, x), y);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return (e2 == null) 
            ?   factory.sum(Expressions.toValue(e1, factory, model, q))
            :   factory.add(
                   Expressions.toValue(e1, factory, model, q), 
                   Expressions.toValue(e2, factory, model, q));
        }
    }
    
    public static class Product<N extends Number> extends BinarayFunctionalExpression<N> {
        public Product(Expression<? extends Number> x, Expression<? extends Number> y) {
            super((Class<N>)x.getJavaType(), x, y);
        }

        public Product(Expression<? extends Number> x, Number y) {
            this(x, new Constant<Number>(Number.class, y));
        }

        public Product(Number x, Expression<? extends Number> y) {
            this(new Constant<Number>(Number.class, x), y);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.multiply(
                Expressions.toValue(e1, factory, model, q), 
                Expressions.toValue(e2, factory, model, q));
        }
    }
    
    public static class Diff<N extends Number> extends BinarayFunctionalExpression<N> {
        public Diff(Expression<? extends Number> x, Expression<? extends Number> y) {
            super((Class<N>)x.getJavaType(), x, y);
        }

        public Diff(Expression<? extends Number> x, Number y) {
            this(x, new Constant<Number>(Number.class, y));
        }

        public Diff(Number x, Expression<? extends Number> y) {
            this(new Constant<Number>(Number.class, x), y);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.subtract(
                Expressions.toValue(e1, factory, model, q), 
                Expressions.toValue(e2, factory, model, q));
        }
    }

    
    public static class Quotient<N extends Number> extends BinarayFunctionalExpression<N> {
        public Quotient(Expression<? extends Number> x, Expression<? extends Number> y) {
            super((Class<N>)x.getJavaType(), x, y);
        }

        public Quotient(Expression<? extends Number> x, Number y) {
            this(x, new Constant<Number>(y));
        }

        public Quotient(Number x, Expression<? extends Number> y) {
            this(new Constant<Number>(x), y);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.divide(
                Expressions.toValue(e1, factory, model, q), 
                Expressions.toValue(e2, factory, model, q));
        }
    }

    public static class Mod extends BinarayFunctionalExpression<Integer> {
        public  Mod(Expression<Integer> x, Expression<Integer> y) {
            super(Integer.class, x,y);
        }
        public  Mod(Expression<Integer> x, Integer y) {
            this(x,new Constant<Integer>(Integer.class, y));
        }
        public  Mod(Integer x, Expression<Integer> y) {
            this(new Constant<Integer>(Integer.class, x),y);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.mod(
                Expressions.toValue(e1, factory, model, q), 
                Expressions.toValue(e2, factory, model, q));
        }
    }

    public static class CurrentDate extends ExpressionImpl<java.sql.Date> {
        public  CurrentDate() {
            super(java.sql.Date.class);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.getCurrentDate();
        }
    }
    
    public static class CurrentTime extends ExpressionImpl<java.sql.Time> {
        public  CurrentTime() {
            super(java.sql.Time.class);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.getCurrentTime();
        }
    }
    
    public static class CurrentTimestamp extends ExpressionImpl<java.sql.Timestamp> {
        public  CurrentTimestamp() {
            super(java.sql.Timestamp.class);
        }

        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.getCurrentTimestamp();
        }
    }

    public static class Equal extends BinaryLogicalExpression {
        public <X,Y> Equal(Expression<X> x, Expression<Y> y) {
            super(x,y);
        }
        
        public <X> Equal(Expression<X> x, Object y) {
            this(x, new Constant<Object>(Object.class, y));
        }
        
        @Override
        public PredicateImpl clone() {
            return new Equal(e1, e2);
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val1 = Expressions.toValue(e1, factory, model, q);
            Value val2 = Expressions.toValue(e2, factory, model, q);
            Expressions.setImplicitTypes(val1, val2, e1.getJavaType(), q);
            return isNegated() ? factory.notEqual(val1, val2) : factory.equal(val1, val2);
        }
    }
    
    public static class GreaterThan extends BinaryLogicalExpression {
        public <X,Y> GreaterThan(Expression<X> x, Expression<Y> y) {
            super(x,y);
        }
        
        public <X> GreaterThan(Expression<X> x, Object y) {
            this(x, new Constant<Object>(Object.class, y));
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val1 = Expressions.toValue(e1, factory, model, q);
            Value val2 = Expressions.toValue(e2, factory, model, q); 
            Expressions.setImplicitTypes(val1, val2, e1.getJavaType(), q); 
            return factory.greaterThan(val1, val2);
        }
    }
    
    public static class GreaterThanEqual extends BinaryLogicalExpression {
        public <X,Y> GreaterThanEqual(Expression<X> x, Expression<Y> y) {
            super(x,y);
        }
        
        public <X> GreaterThanEqual(Expression<X> x, Object y) {
            this(x, new Constant<Object>(Object.class, y));
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val1 = Expressions.toValue(e1, factory, model, q);
            Value val2 = Expressions.toValue(e2, factory, model, q); 
            Expressions.setImplicitTypes(val1, val2, e1.getJavaType(), q); 
            return factory.greaterThanEqual(val1, val2);
        }
    }
   
    public static class LessThan extends BinaryLogicalExpression {
        public <X,Y> LessThan(Expression<X> x, Expression<Y> y) {
            super(x,y);
        }
        
        public <X> LessThan(Expression<X> x, Object y) {
            this(x, new Constant<Object>(Object.class, y));
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val1 = Expressions.toValue(e1, factory, model, q);
            Value val2 = Expressions.toValue(e2, factory, model, q); 
            Expressions.setImplicitTypes(val1, val2, e1.getJavaType(), q); 
            return factory.lessThan(val1, val2);
        }
    }
    
    public static class LessThanEqual extends BinaryLogicalExpression {
        public <X,Y> LessThanEqual(Expression<X> x, Expression<Y> y) {
            super(x,y);
        }
        
        public <X> LessThanEqual(Expression<X> x, Object y) {
            this(x, new Constant<Object>(Object.class, y));
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val1 = Expressions.toValue(e1, factory, model, q);
            Value val2 = Expressions.toValue(e2, factory, model, q); 
            Expressions.setImplicitTypes(val1, val2, e1.getJavaType(), q); 
            return factory.lessThanEqual(val1, val2);
        }
    }

    public static class Between<Y extends Comparable<Y>> extends PredicateImpl.And {
        public Between(Expression<? extends Y> v, Expression<? extends Y> x, Expression<? extends Y> y) {
            super(new GreaterThanEqual(v,x), new LessThanEqual(v,y));
        }
        
        public Between(Expression<? extends Y> v, Y x, Y y) {
            this(v, new Constant<Y>(x), new Constant<Y>(y));
        }
    }
    
    public static class Constant<X> extends ExpressionImpl<X> {
        public final Object arg;
        public Constant(Class<X> t, X x) {
            super(t);
            this.arg = x;
        }
        
        public Constant(X x) {
            this((Class<X>)x.getClass(), x);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Object value = arg;
            if (arg instanceof ParameterExpressionImpl) {
                return ((ParameterExpressionImpl)arg).toValue(factory, model, q);
            }
            int literalType = Literal.TYPE_UNKNOWN;
            if (arg != null) {
                Class<?> literalClass = value.getClass();
                if (Number.class.isAssignableFrom(literalClass)) {
                    literalType = Literal.TYPE_NUMBER;
                } else if (Boolean.class.isAssignableFrom(literalClass)) {
                    literalType = Literal.TYPE_BOOLEAN;
                } else if (String.class.isAssignableFrom(literalClass)) {
                    literalType = Literal.TYPE_STRING;
                } else if (Enum.class.isAssignableFrom(literalClass)) {
                    literalType = Literal.TYPE_ENUM;
                } else if (Class.class.isAssignableFrom(literalClass)) {
                    literalType = Literal.TYPE_CLASS;
                    Literal lit = factory.newTypeLiteral(value, Literal.TYPE_CLASS);
                    ClassMetaData can = ((Types.Entity<X>)q.getRoot().getModel()).meta;
                    Class<?> candidate = can.getDescribedType();
                    if (candidate.isAssignableFrom((Class)value)) {
                       lit.setMetaData(model.repos.getMetaData((Class<?>)value, null, true));
                    } else {
                        lit.setMetaData(can);
                    }
                    return lit;
                } else if (Collection.class.isAssignableFrom(literalClass)) {
                    literalType = Literal.TYPE_COLLECTION;
                }
            }
            return factory.newLiteral(value, literalType);
        }
    }
    
    public static class TypeConstant<X> extends Constant<X> {
        public TypeConstant(X x) {
            super((Class<X>)x.getClass(),x);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.newTypeLiteral(arg, Literal.TYPE_CLASS);
        }
    }
    
    public static class IsEmpty extends PredicateImpl {
        final ExpressionImpl<?> collection;
        public IsEmpty(Expression<?> collection) {
            super();
            this.collection = (ExpressionImpl<?>)collection;
        }
        
        @Override
        public PredicateImpl clone() {
            return new IsEmpty(collection);
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val = Expressions.toValue(collection, factory, model, q);
            return (isNegated()) ? factory.not(factory.isEmpty(val)) : factory.isEmpty(val);
        }
    }
    
    public static class IsNotEmpty extends PredicateImpl {
        final ExpressionImpl<?> collection;
        public IsNotEmpty(Expression<?> collection) {
            super();
            this.collection = (ExpressionImpl<?>)collection;
        }
        
        @Override
        public PredicateImpl clone() {
            return new IsNotEmpty(collection);
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value val = Expressions.toValue(collection, factory, model, q);
            return (isNegated()) ? factory.isEmpty(val) : factory.isNotEmpty(val);
        }
    }

    
    public static class Index extends UnaryFunctionalExpression<Integer> {
        public Index(Joins.List<?,?> e) {
            super(Integer.class, e);
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Value toValue(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl<?> q) {
            Value v = Expressions.toValue(e, factory, model, q);
            ClassMetaData meta = ((PathImpl<?,?>)e)._member.fmd.getElement().getTypeMetaData();
            v.setMetaData(meta);
            return factory.index(v);
        }
    }
    
    public static class IsMember<E> extends PredicateImpl {
        final ExpressionImpl<E> element;
        final ExpressionImpl<?> collection;
        
        public IsMember(Class<E> t, Expression<E> element, Expression<?> collection) {
            this.element = (ExpressionImpl<E>)element;
            this.collection = (ExpressionImpl<?>)collection;
        }
        
        public IsMember(Class<E> t, E element, Expression<?> collection) {
            this(t, new Constant<E>(element), collection);
        }
        
        public IsMember(E element, Expression<?> collection) {
            this((Class<E>)element.getClass(), element, collection);
        }
        
        @Override
        public PredicateImpl clone() {
            return new IsMember<E>(element.getJavaType(), element, collection);
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            org.apache.openjpa.kernel.exps.Expression contains = factory.contains(
                Expressions.toValue(collection, factory, model, q), 
                Expressions.toValue(element, factory, model, q));
            return _negated ? factory.not(contains) : contains;
        }
    }
    
    public static class Like extends PredicateImpl {
        public static final String MATCH_MULTICHAR  = "%";
        public static final String MATCH_SINGLECHAR = "_";
        
        final ExpressionImpl<String> str;
        final ExpressionImpl<String> pattern;
        final ExpressionImpl<Character> escapeChar;
        
        public Like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
            super();
            this.str = (ExpressionImpl<String>)x;
            this.pattern = (ExpressionImpl<String>)pattern;
            this.escapeChar = (ExpressionImpl<Character>)escapeChar;
        }
        
        public Like(Expression<String> x, Expression<String> pat, char esc) {
            this(x, pat, new Constant<Character>(Character.class, esc));
        }
        
        public Like(Expression<String> x, Expression<String> pattern) {
            this(x, pattern, null);
        }
        
        public Like(Expression<String> x, String pattern) {
            this(x, new Constant<String>(pattern), null);
        }
        
        public Like(Expression<String> x, String pat,  
            Expression<Character> esc) {
            this(x, new Constant<String>(pat), esc);
        }
        
        public Like(Expression<String> x, String pat,  Character esc) {
            this(x, new Constant<String>(pat), new Constant<Character>(esc));
        }

        @Override
        public PredicateImpl clone() {
            return new Like(str, pattern, escapeChar);
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            String escapeStr = escapeChar == null ? null :
                ((Character)((Literal)Expressions.toValue(
                    escapeChar, factory, model, q)).getValue()).toString();
            
            return factory.matches(
                Expressions.toValue(str, factory, model, q), 
                Expressions.toValue(pattern, factory, model, q), 
                MATCH_SINGLECHAR, MATCH_MULTICHAR, escapeStr);
        }
    }
    
    public static class Coalesce<T> extends ExpressionImpl<T> implements QueryBuilder.Coalesce<T> {
        private final List<Expression<? extends T>> values = new ArrayList<Expression<? extends T>>();
        
        public Coalesce(Class<T> cls) {
            super(cls);
        }
        
        public Coalesce<T> value(T value) {
            values.add(new Constant<T>(value));
            return this;
        }
        
        public Coalesce<T> value(Expression<? extends T> value) {
            values.add(value); 
            return this;
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Value toValue(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Value[] vs = new Value[values.size()];
            int i = 0;
            for (Expression<?> e : values)
                vs[i++] = Expressions.toValue((ExpressionImpl<?>)e, factory, model, q);
            return factory.coalesceExpression(vs);
        }
    }
    
    public static class Nullif<T> extends ExpressionImpl<T> {
        private Expression<T> val1;
        private Expression<?> val2;

        public Nullif(Expression<T> x, Expression<?> y) {
            super(x.getJavaType());
            val1 = x;
            val2 = y;
        }

        public Nullif(Expression<T> x, T y) {
            super(x.getJavaType());
            val1 = x;
            val2 = new Constant<T>(y);
        }

        @Override
        public org.apache.openjpa.kernel.exps.Value toValue(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Value value1 = Expressions.toValue((ExpressionImpl<?>)val1, factory, model, q); 
            Value value2 = Expressions.toValue((ExpressionImpl<?>)val2, factory, model, q); 
            return factory.nullIfExpression(value1, value2);
        }
    }

    public static class IsNull extends PredicateImpl {
        final ExpressionImpl<?> e;
        public IsNull(ExpressionImpl<?> e) {
            super();
            this.e = e;
        }
        
        @Override
        public PredicateImpl negate() {
            return new Expressions.IsNotNull(e);
        }        
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.equal(
                Expressions.toValue(e, factory, model, q), 
                factory.getNull());
        }
    }
    
    public static class IsNotNull extends PredicateImpl {
        final ExpressionImpl<?> e;
        public IsNotNull(ExpressionImpl<?> e) {
            super();
            this.e = e;
        }
        
        @Override
        public PredicateImpl negate() {
            return new Expressions.IsNull(e);
        }       

        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.notEqual(
                Expressions.toValue(e, factory, model, q), 
                factory.getNull());
        }
    }
    
    
    public static class In<T> extends PredicateImpl.Or implements QueryBuilder.In<T> {
        final ExpressionImpl<T> e;
        private boolean negate;
        public In(Expression<?> e) {
            super((Predicate[])null);
            this.e = (ExpressionImpl<T>)e;
        }
        
        public Expression<T> getExpression() {
            return e;
        }

        public In<T> value(T value) {
            add(new Expressions.Equal(e,value));
            return this;
        }

        public In<T> value(Expression<? extends T> value) {
            add(new Expressions.Equal(e,value));
            return this;
        }
        
        public In<T> negate() {
            this.negate = !negate;
            return this;
        }
    
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            org.apache.openjpa.kernel.exps.Expression inExpr = null;
            if (_exps.size() == 1) {
                Expressions.Equal e = (Expressions.Equal)_exps.get(0);
                ExpressionImpl<?> e2 = e.e2;
                ExpressionImpl<?> e1 = e.e1;
               Value val2 = Expressions.toValue(e2, factory, model, q);
                if (!(val2 instanceof Literal)) {
                     Value val1 = Expressions.toValue(e1, factory, model, q);
                    Expressions.setImplicitTypes(val1, val2, e1.getJavaType(), q);
                    inExpr = factory.contains(val2, val1);
                    return negate ? factory.not(inExpr) : inExpr;
                } else if (((Literal)val2).getParseType() == Literal.TYPE_COLLECTION) {
                    List<Expression<Boolean>> exps = new ArrayList<Expression<Boolean>>();
                    Collection coll = (Collection)((Literal)val2).getValue();
                    for (Object v : coll) {
                        exps.add(new Expressions.Equal(e1,v));
                    }
                    _exps = exps;
                }
            } 
            inExpr = super.toKernelExpression(factory, model, q); 
            IsNotNull notNull = new Expressions.IsNotNull(e);
            if (negate) 
                inExpr = factory.not(inExpr);
            
            return factory.and(inExpr, notNull.toKernelExpression(factory, model, q));
        }
    }
    
    public static class Case<T> extends ExpressionImpl<T> implements QueryBuilder.Case<T> {
        private final List<Expression<? extends T>> thens = new ArrayList<Expression<? extends T>>();
        private final List<Expression<Boolean>> whens     = new ArrayList<Expression<Boolean>>();
        private Expression<? extends T> otherwise;

        public Case(Class<T> cls) {
            super(cls);
        }

        public Case<T> when(Expression<Boolean> when, Expression<? extends T> then) {
            whens.add(when);
            thens.add(then);
            return this;
        }

        public Case<T> when(Expression<Boolean> when, T then) {
            return when(when, new Expressions.Constant<T>(then));
        }

        public Case<T> otherwise(Expression<? extends T> otherwise) {
            this.otherwise = otherwise;
            return this;
        }

        public Case<T> otherwise(T otherwise) {
            return otherwise(new Expressions.Constant<T>(otherwise));
        }

        @Override
        public org.apache.openjpa.kernel.exps.Value toValue(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            int size = whens.size();
            org.apache.openjpa.kernel.exps.Expression[] exps = new org.apache.openjpa.kernel.exps.Expression[size];
            for (int i = 0; i < size; i++) {
                org.apache.openjpa.kernel.exps.Expression expr = ((Expressions.BinaryLogicalExpression)whens.get(i)).
                    toKernelExpression(factory, model, q);
                Value action = Expressions.toValue((ExpressionImpl<?>)thens.get(i), factory, model, q);
                exps[i] = factory.whenCondition(expr, action);
            }

            Value other = Expressions.toValue((ExpressionImpl<?>)otherwise, factory, model, q);
            return factory.generalCaseExpression(exps, other);
        }
    }

    public static class SimpleCase<C,R> extends ExpressionImpl<R> implements QueryBuilder.SimpleCase<C,R> {
        private final List<Expression<? extends R>> thens = new ArrayList<Expression<? extends R>>();
        private final List<Expression<C>> whens = new ArrayList<Expression<C>>();
        private Expression<? extends R> otherwise;
        private Expression<C> caseOperand;

        public SimpleCase(Class<R> cls) {
            super(cls);
        }
        
        public SimpleCase(Expression<C> expr) {
            super(null);
            this.caseOperand = expr;
        }
        
        public Expression<C> getExpression() {
            return caseOperand;
        }

        public SimpleCase<C,R> when(C when, Expression<? extends R> then) {
            whens.add(new Constant<C>(when));
            thens.add(then);
            return this;
        }

        public SimpleCase<C,R> when(C when, R then) {
            return when(when, new Expressions.Constant<R>(then));
        }

        public SimpleCase<C,R> otherwise(Expression<? extends R> otherwise) {
            this.otherwise = otherwise;
            return this;
        }

        public SimpleCase<C,R> otherwise(R otherwise) {
            return otherwise(new Expressions.Constant<R>(otherwise));
        }

        @Override
        public org.apache.openjpa.kernel.exps.Value toValue(
                ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            Value caseOperandExpr = Expressions.toValue((ExpressionImpl<?>)caseOperand, factory, model, q);
            int size = whens.size();
            org.apache.openjpa.kernel.exps.Expression[] exps = new org.apache.openjpa.kernel.exps.Expression[size];
            for (int i = 0; i < size; i++) {
                Value when = Expressions.toValue((ExpressionImpl<C>)whens.get(i), factory, model, q);
                Value action = Expressions.toValue((ExpressionImpl<?>)thens.get(i), factory, model, q);
                exps[i] = factory.whenScalar(when, action);
            }

            Value other = Expressions.toValue((ExpressionImpl<?>)otherwise, factory, model, q);
            return factory.simpleCaseExpression(caseOperandExpr, exps, other);
        }
    }

    public static class Lower extends UnaryFunctionalExpression<String> {
        public Lower(Expression<String> x) {
            super(String.class, x);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.toLowerCase(
                Expressions.toValue(e, factory, model, q));
        }
    }

    public static class Upper extends UnaryFunctionalExpression<String> {
        public Upper(Expression<String> x) {
            super(String.class, x);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model,
            CriteriaQueryImpl<?> q) {
            return factory.toUpperCase(
                Expressions.toValue(e, factory, model, q));
        }
    }

    public static class Length extends UnaryFunctionalExpression<Integer> {
        public Length(Expression<String> x) {
            super(Integer.class, x);
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.stringLength(Expressions.toValue(e, factory, model, q));
        }
    }
     
    public static class Exists<X> extends PredicateImpl {
        final SubqueryImpl<X> e;
        public Exists(Subquery<X> x) {
            super();
            e = (SubqueryImpl<X>)x;
        }

        @Override
        public PredicateImpl clone() {
            return new Exists<X>(e);
        }
        
        @Override
        org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            org.apache.openjpa.kernel.exps.Expression exists = 
                factory.isNotEmpty(Expressions.toValue(e, factory, model, q));
            return isNegated() ? factory.not(exists) : exists;
        }        
    }
    
    public static class All<X> extends ExpressionImpl<X> {
        final SubqueryImpl<X> e;
        public All(Subquery<X> x) {
            super(x.getJavaType());
            e = (SubqueryImpl<X>)x;
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model,
            CriteriaQueryImpl<?> q) {
            return factory.all(Expressions.toValue(e, factory, model, q));
        }
    }

    public static class Any<X> extends ExpressionImpl<X> {
        final SubqueryImpl<X> e;
        public Any(Subquery<X> x) {
            super(x.getJavaType());
            e = (SubqueryImpl<X>)x;
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.any(Expressions.toValue(e, factory, model, q));
        }
    }

    public static class Not<X> extends PredicateImpl {
        protected final ExpressionImpl<Boolean> e;
        public Not(Expression<Boolean> ne) {
            super();
            e = (ExpressionImpl<Boolean>)ne;
        }
        
        @Override
        public PredicateImpl clone() {
            return new Not<X>(e);
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
          ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            return factory.not(super.toKernelExpression(factory, model, q));
        }        
    }
    
    public static class CastAs<Y> extends ExpressionImpl<Y> {
        protected final ExpressionImpl<?> actual;
        public CastAs(Class<Y> cast, ExpressionImpl<?> actual) {
            super(cast);
            this.actual = actual;
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Value toValue(
          ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            org.apache.openjpa.kernel.exps.Value e = actual.toValue(factory, model, q);
            e.setImplicitType(getJavaType());
            return e;
        }
    }
    
    /**
     * An expression that is composed of one or more expressions.
     *
     * @param <T>
     */
    public static class ListArgument<T> extends ExpressionImpl<T> {
        private final ExpressionImpl<?>[] _args;
        public ListArgument(Class<T> cls, ExpressionImpl<?>... args) {
            super(cls);
            _args = args;
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Arguments toValue(
          ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            org.apache.openjpa.kernel.exps.Value[] kvs = new org.apache.openjpa.kernel.exps.Value[_args.length];
            int i = 0;
            for (ExpressionImpl<?> arg : _args) {
                kvs[i++] = arg.toValue(factory, model, q);
            }
            org.apache.openjpa.kernel.exps.Arguments e = factory.newArgumentList(kvs);
            e.setImplicitType(getJavaType());
            return e;
        }
    }

}
