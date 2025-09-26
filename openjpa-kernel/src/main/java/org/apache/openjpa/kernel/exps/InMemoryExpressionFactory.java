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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;

import org.apache.openjpa.kernel.Extent;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;

/**
 * Expression factory implementation that can be used to execute queries
 * in memory.
 *
 * @author Abe White
 */
public class InMemoryExpressionFactory
    implements ExpressionFactory {

    private static final Value NULL = new Null();
    private static final Object UNIQUE = new Object();

    // list of unbound variables in this query
    private List<UnboundVariable> _unbounds = null;

    /**
     * Tests whether the given candidate matches the given type and this
     * expression.
     */
    public boolean matches(QueryExpressions exps, ClassMetaData type,
        boolean subs, Object candidate, StoreContext ctx, Object[] params) {
        // ignore candidates of the wrong type
        if (candidate == null)
            return false;
        if (!subs && candidate.getClass() != type.getDescribedType())
            return false;
        if (subs && !type.getDescribedType().isAssignableFrom
            (candidate.getClass()))
            return false;

        // evaluate the expression for all possible combinations of values
        // of the unbound variables; the candidate matches if any combination
        // matches
        return matches((Exp) exps.filter, candidate, ctx, params, 0);
    }

    /**
     * Recursive method to evaluate the expression for all possible
     * combinations of unbound variables. This method simulates a sequence
     * of embedded procedural loops over the extents of all variables in the
     * unbounds list.
     */
    protected boolean matches(Exp exp, Object candidate, StoreContext ctx,
        Object[] params, int i) {
        // base case: all variables have been aliased; evaluate for current
        // values
        if (_unbounds == null || i == _unbounds.size())
            return exp.evaluate(candidate, candidate, ctx, params);

        // grab the extent for this variable
        UnboundVariable var = _unbounds.get(i);
        Iterator<Object> itr = ctx.extentIterator(var.getType(), true, null, false);
        try {
            // if the extent was empty, then alias the variable to null
            if (!itr.hasNext()) {
                var.setValue(null);
                return matches(exp, candidate, ctx, params, i + 1);
            }

            // try every value, short-circuiting on match
            while (itr.hasNext()) {
                // set the variable to each extent value and recurse
                var.setValue(itr.next());
                if (matches(exp, candidate, ctx, params, i + 1))
                    return true;
            }

            // no match
            return false;
        } finally {
            ImplHelper.close(itr);
        }
    }

    /**
     * Group the list of matches into a list of lists.
     */
    public List group(QueryExpressions exps, List matches,
        StoreContext ctx, Object[] params) {
        if (matches == null || matches.isEmpty() || exps.grouping.length == 0)
            return matches;

        // to form groups we first order on the grouping criteria
        matches = order(exps, exps.grouping, false, matches, ctx, params);

        // now we combine all results whose values for each grouping clause
        // are the same, relying on the fact that these values will already be
        // together due to the sorting
        Object[] prevs = new Object[exps.grouping.length];
        Arrays.fill(prevs, UNIQUE);
        Object[] curs = new Object[exps.grouping.length];
        List grouped = new ArrayList();
        List group = null;
        Object pc;
        boolean eq;
        for (Object match : matches) {
            pc = match;
            eq = true;
            for (int i = 0; i < exps.grouping.length; i++) {
                curs[i] = ((Val) exps.grouping[i]).evaluate(pc, pc, ctx,
                        params);
                eq = eq && Objects.equals(prevs[i], curs[i]);
            }

            // if this object's grouping values differ from the prev,
            // start a new group
            if (!eq) {
                if (group != null)
                    grouped.add(group);
                group = new ArrayList();
            }
            group.add(pc);
            System.arraycopy(curs, 0, prevs, 0, curs.length);
        }
        // add the last group formed
        if (group != null)
            grouped.add(group);

        return grouped;
    }

    /**
     * Return true if the given group matches the having expression.
     */
    public boolean matches(QueryExpressions exps, Collection group,
        StoreContext ctx, Object[] params) {
        if (group == null || group.isEmpty())
            return false;
        if (exps.having == null)
            return true;

        // evaluate the expression for all possible combinations of values
        // of the unbound variables; the group matches if any combination
        // matches
        return matches((Exp) exps.having, group, ctx, params, 0);
    }

    /**
     * Recursive method to evaluate the expression for all possible
     * combinations of unbound variables. This method simulates a sequence
     * of embedded procedural loops over the extents of all variables in the
     * unbounds list.
     */
    private boolean matches(Exp exp, Collection group, StoreContext ctx,
        Object[] params, int i) {
        // base case: all variables have been aliased; evaluate for current
        // values
        if (_unbounds == null || i == _unbounds.size())
            return exp.evaluate(group, ctx, params);

        // grab the extent for this variable
        UnboundVariable var = _unbounds.get(i);
        Extent extent = ctx.getBroker().newExtent(var.getType(), true);
        Iterator itr = extent.iterator();
        try {
            // if the extent was empty, then alias the variable to null
            if (!itr.hasNext()) {
                var.setValue(null);
                return matches(exp, group, ctx, params, i + 1);
            }

            // try every value, short-circuiting on match
            while (itr.hasNext()) {
                // set the variable to each extent value and recurse
                var.setValue(itr.next());
                if (matches(exp, group, ctx, params, i + 1))
                    return true;
            }

            // no match
            return false;
        } finally {
            ImplHelper.close(itr);
        }
    }

    /**
     * Create the projections for the given results.
     */
    public List project(QueryExpressions exps, List matches,
        StoreContext ctx, Object[] params) {
        if (exps.projections.length == 0)
            return matches;

        // if an ungrouped aggregate, evaluate the whole matches list
        if (exps.grouping.length == 0 && exps.isAggregate()) {
            Object[] projection = project(matches, exps, true, ctx, params);
            return Arrays.asList(new Object[]{ projection });
        }

        // evaluate each candidate
        List projected = new ArrayList(matches.size());
        for (Object match : matches)
            projected.add(project(match, exps, exps.grouping.length > 0,
                    ctx, params));
        return projected;
    }

    /**
     * Generate a projection on the given candidate.
     */
    private Object[] project(Object candidate, QueryExpressions exps,
        boolean agg, StoreContext ctx, Object[] params) {
        Object[] projection = new Object[exps.projections.length
            + exps.ordering.length];

        // calcualte result values
        Object result = null;
        for (int i = 0; i < exps.projections.length; i++) {
            if (agg)
                result = ((Val) exps.projections[i]).evaluate((Collection)
                    candidate, null, ctx, params);
            else
                result = ((Val) exps.projections[i]).evaluate(candidate,
                    candidate, ctx, params);
            projection[i] = result;
        }

        // tack on ordering values
        boolean repeat;
        for (int i = 0; i < exps.ordering.length; i++) {
            // already selected as a result?
            repeat = false;
            for (int j = 0; !repeat && j < exps.projections.length; j++) {
                if (exps.orderingClauses[i].equals(exps.projectionClauses[j])) {
                    result = projection[j];
                    repeat = true;
                }
            }

            // not selected as result; calculate value
            if (!repeat) {
                if (agg)
                    result = ((Val) exps.ordering[i]).evaluate((Collection)
                        candidate, null, ctx, params);
                else
                    result = ((Val) exps.ordering[i]).evaluate(candidate,
                        candidate, ctx, params);
            }

            projection[i + exps.projections.length] = result;
        }
        return projection;
    }

    /**
     * Order the given list of matches on the given value.
     */
    public List order(QueryExpressions exps, List matches,
        StoreContext ctx, Object[] params) {
        return order(exps, exps.ordering, true, matches, ctx, params);
    }

    /**
     * Order the given list of matches on the given value.
     *
     * @param projected whether projections have been applied to the matches yet
     */
    private List order(QueryExpressions exps, Value[] orderValues,
        boolean projected, List matches, StoreContext ctx, Object[] params) {
        if (matches == null || matches.isEmpty()
            || orderValues == null || orderValues.length == 0)
            return matches;

        int results = (projected) ? exps.projections.length : 0;
        boolean[] asc = (projected) ? exps.ascending : null;
        int idx;
        for (int i = orderValues.length - 1; i >= 0; i--) {
            // if this is a projection, then in project() we must have selected
            // the ordering value already after the projection values
            idx = (results > 0) ? results + i : -1;
            Collections.sort(matches,
                new OrderValueComparator((Val) orderValues[i],
                    asc == null || asc[i], idx, ctx, params));
        }
        return matches;
    }

    /**
     * Filter the given list of matches, removing duplicate entries.
     */
    public List distinct(QueryExpressions exps, boolean fromExtent,
        List matches) {
        if (matches == null || matches.isEmpty())
            return matches;

        // no need to do distinct if not instructed to, or if these are
        // candidate objects from an extent
        int len = exps.projections.length;
        if ((exps.distinct & QueryExpressions.DISTINCT_TRUE) == 0
            || (fromExtent && len == 0))
            return matches;

        Set seen = new HashSet(matches.size());
        List distinct = null;
        Object cur;
        Object key;
        for (ListIterator li = matches.listIterator(); li.hasNext();) {
            cur = li.next();
            key = (len > 0 && cur != null) ? new ArrayKey((Object[]) cur) : cur;

            if (seen.add(key)) {
                // key hasn't been seen before; if we've created a distinct
                // list, keep adding to it
                if (distinct != null)
                    distinct.add(cur);
            } else if (distinct == null) {
                // we need to copy the matches list because the distinct list
                // will be different (we've come across a non-unique key); add
                // all the elements we've skipped over so far
                distinct = new ArrayList(matches.size());
                distinct.addAll(matches.subList(0, li.previousIndex()));
            }
        }
        return (distinct == null) ? matches : distinct;
    }

    @Override
    public Expression emptyExpression() {
        return new Exp();
    }

    @Override
    public Expression asExpression(Value v) {
        return new ValExpression((Val) v);
    }

    @Override
    public Expression equal(Value v1, Value v2) {
        return new EqualExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression notEqual(Value v1, Value v2) {
        return new NotEqualExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression lessThan(Value v1, Value v2) {
        return new LessThanExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression greaterThan(Value v1, Value v2) {
        return new GreaterThanExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression lessThanEqual(Value v1, Value v2) {
        return new LessThanEqualExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression greaterThanEqual(Value v1, Value v2) {
        return new GreaterThanEqualExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression isEmpty(Value v1) {
        return new IsEmptyExpression((Val) v1);
    }

    @Override
    public Expression isNotEmpty(Value v1) {
        return not(isEmpty(v1));
    }

    @Override
    public Expression contains(Value v1, Value v2) {
        return new ContainsExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression containsKey(Value v1, Value v2) {
        return new ContainsKeyExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression containsValue(Value v1, Value v2) {
        return new ContainsValueExpression((Val) v1, (Val) v2);
    }

    @Override
    public Value getMapValue(Value map, Value arg) {
        return new GetMapValue((Val) map, (Val) arg);
    }

    @Override
    public Expression isInstance(Value v1, Class c) {
        return new InstanceofExpression((Val) v1, c);
    }

    @Override
    public Expression and(Expression exp1, Expression exp2) {
        if (exp1 instanceof BindVariableExpression)
            return new BindVariableAndExpression((BindVariableExpression) exp1,
                (Exp) exp2);
        return new AndExpression((Exp) exp1, (Exp) exp2);
    }

    @Override
    public Expression or(Expression exp1, Expression exp2) {
        return new OrExpression((Exp) exp1, (Exp) exp2);
    }

    @Override
    public Expression not(Expression exp) {
        return new NotExpression((Exp) exp);
    }

    @Override
    public Expression bindVariable(Value var, Value val) {
        return new BindVariableExpression((BoundVariable) var, (Val) val);
    }

    @Override
    public Expression bindKeyVariable(Value var, Value val) {
        return new BindKeyVariableExpression((BoundVariable) var, (Val) val);
    }

    @Override
    public Expression bindValueVariable(Value var, Value val) {
        return new BindValueVariableExpression((BoundVariable) var, (Val) val);
    }

    @Override
    public Expression endsWith(Value v1, Value v2) {
        return new EndsWithExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression matches(Value v1, Value v2,
        String single, String multi, String esc) {
        return new MatchesExpression((Val) v1, (Val) v2, single, multi, esc,
            true);
    }

    @Override
    public Expression notMatches(Value v1, Value v2,
        String single, String multi, String esc) {
        return new MatchesExpression((Val) v1, (Val) v2, single, multi, esc,
            false);
    }

    @Override
    public Expression startsWith(Value v1, Value v2) {
        return new StartsWithExpression((Val) v1, (Val) v2);
    }

    @Override
    public Subquery newSubquery(ClassMetaData candidate, boolean subs,
        String alias) {
        return new SubQ(alias);
    }

    @Override
    public Path newPath() {
        return new CandidatePath();
    }

    @Override
    public Path newPath(Value val) {
        return new ValuePath((Val) val);
    }

    @Override
    public Literal newLiteral(Object val, int parseType) {
        return new Lit(val, parseType);
    }

    @Override
    public Literal newTypeLiteral(Object val, int parseType) {
        return new TypeLit(val, parseType);
    }

    @Override
    public Value getThis() {
        return new This();
    }

    @Override
    public Value getNull() {
        return NULL;
    }

    @Override
    public <T extends Date> Value getCurrentDate(Class<T> dateType) {
        return new CurrentDate(dateType);
    }

    @Override
    public  <T extends Date> Value getCurrentTime(Class<T> dateType) {
        return new CurrentDate(dateType);
    }

    @Override
    public <T extends Date> Value getCurrentTimestamp(Class<T> dateType) {
        return new CurrentDate(dateType);
    }

    @Override
    public <T extends Temporal> Value getCurrentLocalDateTime(Class<T> temporalType) {
        return new CurrentTemporal(temporalType);
    }

    @Override
    public Value getDateTimeField(DateTimeExtractField field, Value value) {
        return new ExtractDateTimeField(field, (Val) value);
    }

    @Override
    public Value getDateTimePart(DateTimeExtractPart part, Value value) {
        return new ExtractDateTimePart(part, (Val) value);
    }
    
    @Override
    public Value newTypecastAsString(Value value) {
    	return new TypecastAsString((Val) value);
    }
    
    @Override
    public Value newTypecastAsNumber(Value value, Class<? extends Number> numberType) {
    	return new TypecastAsNumber((Val) value, numberType);
    }

    @Override
    public Parameter newParameter(Object name, Class type) {
        return new Param(name, type);
    }

    @Override
    public Parameter newCollectionValuedParameter(Object name, Class type) {
        return new CollectionParam(name, type);
    }

    @Override
    public Value newExtension(FilterListener listener, Value target,
        Value arg) {
        return new Extension(listener, (Val) target, (Val) arg);
    }

    @Override
    public Value newAggregate(AggregateListener listener, Value arg) {
        return new Aggregate(listener, (Val) arg);
    }

    @Override
    public Arguments newArgumentList(Value val1, Value val2) {
        return new Args(val1, val2);
    }

    @Override
    public Arguments newArgumentList(Value... values) {
        return new Args(values);
    }

    @Override
    public Value newUnboundVariable(String name, Class type) {
        UnboundVariable var = new UnboundVariable(type);
        if (_unbounds == null)
            _unbounds = new ArrayList<>(3);
        _unbounds.add(var);
        return var;
    }

    @Override
    public Value newBoundVariable(String name, Class type) {
        return new BoundVariable(type);
    }

    @Override
    public Value cast(Value val, Class cls) {
        if (val instanceof CandidatePath)
            ((CandidatePath) val).castTo(cls);
        else if (val instanceof BoundVariable)
            ((BoundVariable) val).castTo(cls);
        else
            val = new Cast((Val) val, cls);
        return val;
    }

    @Override
    public Value add(Value val1, Value val2) {
        return new Add((Val) val1, (Val) val2);
    }

    @Override
    public Value subtract(Value val1, Value val2) {
        return new Subtract((Val) val1, (Val) val2);
    }

    @Override
    public Value multiply(Value val1, Value val2) {
        return new Multiply((Val) val1, (Val) val2);
    }

    @Override
    public Value divide(Value val1, Value val2) {
        return new Divide((Val) val1, (Val) val2);
    }

    @Override
    public Value mod(Value val1, Value val2) {
        return new Mod((Val) val1, (Val) val2);
    }

    @Override
    public Value abs(Value val) {
        return new Abs((Val) val);
    }
    
    @Override
    public Value ceiling(Value val) {
    	return new Ceiling((Val) val);
    }

    @Override
    public Value exp(Value val) {
        return new Exponential((Val) val);
    }

    @Override
    public Value floor(Value val) {
        return new Floor((Val) val);
    }

    @Override
    public Value ln(Value val) {
        return new NaturalLogarithm(((Val) val));
    }

    @Override
    public Value sign(Value val) {
        return new Sign((Val) val);
    }

    @Override
    public Value power(Value base, Value exponent) {
        return new Power((Val) base, (Val) exponent);
    }

    @Override
    public Value round(Value num, Value precision) {
        return new Round((Val) num, (Val) precision);
    }

    @Override
    public Value indexOf(Value val1, Value val2) {
        return new IndexOf((Val) val1, (Val) val2);
    }

    @Override
    public Value concat(Value val1, Value val2) {
        return new Concat((Val) val1, (Val) val2);
    }

    @Override
    public Value stringLength(Value str) {
        return new StringLength((Val) str);
    }

    @Override
    public Value trim(Value str, Value trimChar, Boolean where) {
        return new Trim((Val) str, (Val) trimChar, where);
    }

    @Override
    public Value sqrt(Value val) {
        return new Sqrt((Val) val);
    }

    @Override
    public Value substring(Value val1, Value val2) {
        return new Substring((Val) val1, (Val) val2);
    }
    
    @Override
    public Value left(Value str, Value length) {
    	return new Left((Val) str, (Val) length);
    }
    
    @Override
    public Value right(Value str, Value length) {
    	return new Right((Val) str, (Val) length);
    }
    
    @Override
    public Value replace(Value orig, Value pattern, Value replacement) {
    	return new Replace((Val) orig, (Val) pattern, (Val) replacement);
    }

    @Override
    public Value toUpperCase(Value val) {
        return new ToUpperCase((Val) val);
    }

    @Override
    public Value toLowerCase(Value val) {
        return new ToLowerCase((Val) val);
    }

    @Override
    public Value avg(Value val) {
        return new Avg((Val) val);
    }

    @Override
    public Value count(Value val) {
        return new Count((Val) val);
    }

    @Override
    public Value distinct(Value val) {
        return new Distinct((Val) val);
    }

    @Override
    public Value max(Value val) {
        return new Max((Val) val);
    }

    @Override
    public Value min(Value val) {
        return new Min((Val) val);
    }

    @Override
    public Value sum(Value val) {
        return new Sum((Val) val);
    }

    @Override
    public Value any(Value val) {
        return new Any((Val) val);
    }

    @Override
    public Value all(Value val) {
        return new All((Val) val);
    }

    @Override
    public Value size(Value val) {
        return new Size((Val) val);
    }

    @Override
    public Value index(Value val) {
        return new Index((Val) val);
    }

    @Override
    public Value type(Value val) {
        return new Type((Val) val);
    }

    @Override
    public Value mapEntry(Value key, Value val) {
        throw new UnsupportedException("not implemented yet");
    }

    @Override
    public Value mapKey(Value key, Value val) {
        throw new UnsupportedException("not implemented yet");
    }

    @Override
    public Value getKey(Value val) {
        throw new UnsupportedException("not implemented yet");
    }

    @Override
    public Value getObjectId(Value val) {
        return new GetObjectId((Val) val);
    }

    /**
     * Key that implements hashCode and equals methods for object arrays.
     */
    private static class ArrayKey {

        private final Object[] _arr;

        public ArrayKey(Object[] arr) {
            _arr = arr;
        }

        @Override
        public int hashCode() {
            int rs = 17;
            for (Object o : _arr) {
                rs = 37 * rs + ((o == null) ? 0 : o.hashCode());
            }
            return rs;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this)
                return true;
            if (other == null)
                return false;

            Object[] arr = ((ArrayKey) other)._arr;
            if (_arr.length != arr.length)
                return false;
            for (int i = 0; i < _arr.length; i++)
                if (!Objects.equals(_arr[i], arr[i]))
                    return false;
            return true;
        }
    }

    /**
     * Comparator that uses the result of eval'ing a Value to sort on. Null
     * values are placed last if sorting in ascending order, first if
     * descending.
     */
    private static class OrderValueComparator
        implements Comparator {

        private final StoreContext _ctx;
        private final Val _val;
        private final boolean _asc;
        private final int _idx;
        private final Object[] _params;

        private OrderValueComparator(Val val, boolean asc, int idx,
            StoreContext ctx, Object[] params) {
            _ctx = ctx;
            _val = val;
            _asc = asc;
            _idx = idx;
            _params = params;
        }

        @Override
        public int compare(Object o1, Object o2) {
            if (_idx != -1) {
                o1 = ((Object[]) o1)[_idx];
                o2 = ((Object[]) o2)[_idx];
            } else {
                o1 = _val.evaluate(o1, o1, _ctx, _params);
                o2 = _val.evaluate(o2, o2, _ctx, _params);
            }

            if (o1 == null && o2 == null)
                return 0;
            if (o1 == null)
                return (_asc) ? 1 : -1;
            if (o2 == null)
                return (_asc) ? -1 : 1;

            if (o1 instanceof Boolean && o2 instanceof Boolean) {
                int i1 = (Boolean) o1 ? 1 : 0;
                int i2 = (Boolean) o2 ? 1 : 0;
                return i1 - i2;
            }

            try {
                if (_asc)
                    return ((Comparable) o1).compareTo(o2);
                return ((Comparable) o2).compareTo(o1);
            } catch (ClassCastException cce) {
                Localizer loc = Localizer.forPackage
                    (InMemoryExpressionFactory.class);
                throw new UserException(loc.get("not-comp", o1, o2));
			}
		}
	}

    @Override
    public Value generalCaseExpression(Expression[] exp, Value val) {
        Exp[] exps = new Exp[exp.length];
        for (int i = 0; i < exp.length; i++)
            exps[i] = (Exp) exp[i];
        return new GeneralCase(exps, (Val) val);
    }

    @Override
    public Value simpleCaseExpression(Value caseOperand, Expression[] exp,
        Value val) {
            Exp[] exps = new Exp[exp.length];
            for (int i = 0; i < exp.length; i++)
                exps[i] = (Exp) exp[i];
            return new SimpleCase((Val) caseOperand, exps, (Val) val);
    }

    @Override
    public Expression whenCondition(Expression exp, Value val) {
        return new WhenCondition((Exp) exp, (Val) val);
    }

    @Override
    public Expression whenScalar(Value val1, Value val2) {
        return new WhenScalar((Val) val1, (Val) val2);
    }

    @Override
    public Value coalesceExpression(Value[] val) {
        Val[] vals = new Val[val.length];
        for (int i = 0; i < val.length; i++)
            vals[i] = (Val) val[i];
        return new Coalesce(vals);
    }

    @Override
    public Value nullIfExpression(Value val1, Value val2) {
        return new NullIf((Val) val1, (Val) val2);
    }

    @Override
    public Value newFunction(String functionName, Class<?> resultType, Value... args) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isVerticalType(Value val) {
        return false;
    }
}
