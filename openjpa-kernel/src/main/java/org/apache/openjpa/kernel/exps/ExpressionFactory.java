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
package org.apache.openjpa.kernel.exps;

import java.time.temporal.Temporal;
import java.util.Date;

import org.apache.openjpa.meta.ClassMetaData;

/**
 * The ExpressionFactory must be implemented by a particular runtime
 * to form {@link Expression}s in its native query language.
 *
 * @author Abe White
 */
public interface ExpressionFactory {

    /**
     * Return a blank expression; this is used when the filter is empty.
     */
    Expression emptyExpression();

    /**
     * Return the given value as an expression.
     */
    Expression asExpression(Value bool);

    /**
     * Return an expression representing the given condition.
     */
    Expression equal(Value v1, Value v2);

    /**
     * Return an expression representing the given condition.
     */
    Expression notEqual(Value v1, Value v2);

    /**
     * Return an expression representing the given condition.
     */
    Expression lessThan(Value v1, Value v2);

    /**
     * Return an expression representing the given condition.
     */
    Expression greaterThan(Value v1, Value v2);

    /**
     * Return an expression representing the given condition.
     */
    Expression lessThanEqual(Value v1, Value v2);

    /**
     * Return an expression representing the given condition.
     */
    Expression greaterThanEqual(Value v1, Value v2);

    /**
     * Return an expression representing the given condition.
     */
    Expression isEmpty(Value target);

    /**
     * Return an expression representing the given condition.
     */
    Expression isNotEmpty(Value target);

    /**
     * Return an expression representing the given condition.
     */
    Expression contains(Value coll, Value arg);

    /**
     * Return an expression representing the given condition.
     */
    Expression containsKey(Value map, Value arg);

    /**
     * Return an expression representing the given condition.
     */
    Expression containsValue(Value map, Value arg);

    /**
     * Return an expression representing the given condition.
     */
    Value getMapValue(Value map, Value arg);

    /**
     * Return whether the first value is an instance of the given class.
     */
    Expression isInstance(Value obj, Class c);

    /**
     * Return the two expressions AND'd together.
     */
    Expression and(Expression exp1, Expression exp2);

    /**
     * Return the two expressions OR'd together.
     */
    Expression or(Expression exp1, Expression exp2);

    /**
     * Return the inverse of this expression.
     */
    Expression not(Expression exp);

    /**
     * Bind the given variable to the given collection value.
     */
    Expression bindVariable(Value var, Value coll);

    /**
     * Bind the given variable to the key set of the given map value.
     */
    Expression bindKeyVariable(Value var, Value map);

    /**
     * Bind the given variable to the value set of the given map value.
     */
    Expression bindValueVariable(Value var, Value map);

    /**
     * Return whether the first string ends with the second.
     */
    Expression endsWith(Value str1, Value str2);

    /**
     * Return whether the string matches the matching pattern.
     *
     * @param str the value to compare
     * @param regexp the pattern against which to compare
     * @param single the token that signifies a single-character match
     * @param multi the token that signifies a multi-character match
     * @param escape the escape token that escapes the matching tokens
     */
    Expression matches(Value str, Value regexp,
        String single, String multi, String escape);

    /**
     * Return whether the string does not match the given pattern.
     *
     * @param str the value to compare
     * @param regexp the pattern against which to compare
     * @param single the token that signifies a single-character match
     * @param multi the token that signifies a multi-character match
     * @param escape the escape token that escapes the matching tokens
     */
    Expression notMatches(Value str, Value regexp, String single,
        String multi, String escape);

    /**
     * Return whether the first string starts with the second.
     */
    Expression startsWith(Value str1, Value str2);

    /**
     * Returns the number of characters in the String value.
     */
    Value stringLength(Value str);

    /**
     * Trims the specified specified trimChar from the specified value.
     *
     * @param str the value from which to trim
     * @param trimChar the characters to trim off
     * @param where which side of the String to trim: null
     * indicates both sides, true indicates
     * leading, and false indicates trailing
     */
    Value trim(Value str, Value trimChar, Boolean where);

    /**
     * Return a subquery. Paths for the candidates of the subquery are
     * created with {@link #newPath(Value)}, passing in the subquery as the
     * value.
     *
     * @param candidate the candidate class of the subquery
     * @param subs whether the query includes subclasses
     * @param alias the alias given to the query candidate class
     */
    Subquery newSubquery(ClassMetaData candidate, boolean subs,
        String alias);

    /**
     * Return a value representing a path which will consist
     * of a chain of 1 or more field names starting in the namespace of the
     * candidate class.<br /> Example: <code>parent.address.city</code>
     */
    Path newPath();

    /**
     * Return a value representing a path which will consist
     * of a chain of 1 or more field names starting in the namespace of the
     * given value.<br /> Example: <code>var.address.city</code>
     */
    Path newPath(Value val);

    /**
     * Return a value representing the given constant, which will be
     * a {@link Number}, {@link String}, or {@link Boolean} instance.
     */
    Literal newLiteral(Object val, int parseType);

    /**
     * Return a value representing <code>this</code>.
     */
    Value getThis();

    /**
     * Return a value representing null.
     */
    Value getNull();

    /**
     * Return a value representing the current date.
     */
    <T extends Date> Value getCurrentDate(Class<T> dateType);

    /**
     * Return a value representing the current time.
     */
    <T extends Date> Value getCurrentTime(Class<T> timeType);

    /**
     * Return a value representing the current timestamp.
     */
    <T extends Date> Value getCurrentTimestamp(Class<T> timestampType);

    /**
     * Return a value representing the current local temporal.
     */
    <T extends Temporal> Value getCurrentLocalDateTime(Class<T> temporalType);

    /**
     * Returns the integer or double value of the required ChronoField from the temporal value
     */
    Value getDateTimeField(DateTimeExtractField field, Value value);

    /**
     * Return the Date or time part of the given temporal value
     */
    Value getDateTimePart(DateTimeExtractPart part, Value value);
    
    /**
     * Returns the value typecasted as string
     * 
     */
    Value newTypecastAsString(Value value);

    /**
     * Return a value representing a parameter for the given value. The
     * type may be <code>Object</code> if the parameter is not declared.
     */
    Parameter newParameter(Object name, Class type);

    /**
     * Return a value representing a collection-valued parameter. The
     * type may be <code>Object</code> if the parameter is not declared.
     */
    Parameter newCollectionValuedParameter(Object name, Class type);

    /**
     * Return the value of the given extension.
     */
    Value newExtension(FilterListener listener, Value target,
        Value args);

    /**
     * Return the value of the given function.
     */
    Value newAggregate(AggregateListener listener, Value args);

    /**
     * Return a function argument list consisting of the given values, either
     * of which may itself be an argument list.
     */
    Arguments newArgumentList(Value arg1, Value arg2);

    /**
     * Return a function argument list consisting of the given values, any
     * of which may itself be an argument list.
     */
    Arguments newArgumentList(Value... values);

    /**
     * Return an unbound variable. This method will only be called once for
     * a given named unbound variable. The type may be <code>Object</code>
     * if the variable is not declared.
     */
    Value newUnboundVariable(String name, Class type);

    /**
     * This method will be called only once per variable bound in a
     * <code>contains</code> clause, and the returned value will be reused
     * for any further instances of the variable in subexpression of the
     * filter string. The type may be <code>Object</code> if the variable is
     * not declared.
     */
    Value newBoundVariable(String name, Class type);

    /**
     * Cast the value to the given class.
     */
    Value cast(Value obj, Class cls);

    /**
     * Return a value representing the two values added together.
     */
    Value add(Value num1, Value num2);

    /**
     * Return a value representing the second value subtracted from the first.
     */
    Value subtract(Value num1, Value num2);

    /**
     * Return a value representing the two values multiplied together.
     */
    Value multiply(Value num1, Value num2);

    /**
     * Return a value representing the first value divided by the first one.
     */
    Value divide(Value num1, Value num2);

    /**
     * Return a value representing the first value mod'd by the second one.
     */
    Value mod(Value num1, Value num2);

    /**
     * Return a value representing the absolute value of the given one.
     */
    Value abs(Value num);
    
    /**
     * Returns a value representing the ceiling value of the given one.
     */
    Value ceiling(Value num);

    /**
     * Returns a value representing the Euler's e constant powered to the given value.
     */
    Value exp(Value num);

    /**
     * Returns a value representing the floor of the given value.
     */
    Value floor(Value num);

    /**
     * Returns the natural logarithm of the given value
     */
    Value ln(Value num);

    /**
     * Returns the sign of the given value as a number (-1 for negative, 0 for zeroes and 1 for positive)
     */
    Value sign(Value num);

    /**
     * Returns a value representing base powered by the exponent
     */
    Value power(Value base, Value exponent);

    /*
     * Returns the number rounded to the given precision.
     */
    Value round(Value num, Value precision);

    /**
     * Return a value representing the indexOf (LOCATE in JPQL) function on
     * the given target with the given args.
     * The optional second argument is one-based.
     */
    Value indexOf(Value str, Value args);

    /**
     * Return a value representing the concatenation of
     * the given target with the given args.
     */
    Value concat(Value str, Value args);

    /**
     * Return the square root of the given value.
     */
    Value sqrt(Value num);

    /**
     * Return a value representing the substring function on
     * the given target with the given args. Unlike as with {@link String#substring},
     * the start index is one-based, and the second argument is the length.
     */
    Value substring(Value str, Value args);

    /**
     * Return the upper case of the given value.
     */
    Value toUpperCase(Value str);

    /**
     * Return the upper case of the given value.
     */
    Value toLowerCase(Value str);

    /**
     * Return the average of the given value for all matches.
     */
    Value avg(Value num);

    /**
     * Return the count of the given value for all matches.
     */
    Value count(Value obj);

    /**
     * Return the max of the given value for all matches.
     */
    Value max(Value num);

    /**
     * Return the max of the given value for all matches.
     */
    Value min(Value num);

    /**
     * Return the max of the given value for all matches.
     */
    Value sum(Value num);

    /**
     * Return an expression representing the given condition.
     *
     * @since 0.4.0.0
     */
    Value any(Value target);

    /**
     * Return an expression representing the given condition.
     *
     * @since 0.4.0.0
     */
    Value all(Value target);

    /**
     * Return an expression representing the given condition.
     *
     * @since 0.4.0.0
     */
    Value size(Value target);

    /**
     * Return an index/position of the given value within a collection/map.
     *
     * @since 2.0.0
     */
    Value index(Value target);

    /**
     * Return the type/class of the given value.
     *
     * @since 2.0.0
     */
    Value type(Value target);

    /**
     * Return the map entry of the given value.
     *
     * @since 2.0.0
     */
    Value mapEntry(Value key, Value val);

    /**
     * Return the map key of the given value
     */
    Value mapKey(Value key, Value val);

    /**
     * Path navigation thru map key
     */
    Value getKey(Value val);

    /**
     * Return distinct values of the given value. This is typically used
     * within aggregates, for example: max(distinct(path))
	 *
	 * @since 0.4.0.0
	 */
	Value distinct (Value obj);

	/**
	 * Return the object id of the given value.
	 */
	Value getObjectId (Value val);

    /**
     * Return a simple case expression
     */
    Value simpleCaseExpression(Value caseOperand,
            Expression[] exp, Value val);

    /**
     * Return a general case expression
     */
    Value generalCaseExpression(Expression[] exp, Value val);

    /**
     * Return a when condidional clause
     */
    Expression whenCondition(Expression exp, Value val);

    /**
     * Return a when scalar_expression clause
     */
    Expression whenScalar(Value val1, Value val2);

    /**
     * Return a coalesce expression
     */
    Value coalesceExpression(Value[] val);

    /**
     * Return a nullif expression
     */
    Value nullIfExpression(Value val1, Value val2);

    /**
     * Return a value representing the given constant, which will be
     * a {@link Number}, {@link String}, or {@link Boolean} instance.
     */
    Literal newTypeLiteral(Object val, int parseType);

    /**
     * Return a value representing the given datastore function with the given arguments.
     */
    Value newFunction(String functionName, Class<?> resultType, Value... args);

    /**
     * Return true if the Value is a Type expression and the Type uses joined table strategy.
     */
    boolean isVerticalType(Value val);
}
