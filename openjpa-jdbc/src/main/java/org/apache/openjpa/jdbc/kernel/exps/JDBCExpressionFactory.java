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
package org.apache.openjpa.jdbc.kernel.exps;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.util.Date;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Discriminator;
import org.apache.openjpa.jdbc.meta.strats.NoneDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.VerticalClassStrategy;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.Arguments;
import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.apache.openjpa.kernel.exps.DateTimeExtractPart;
import org.apache.openjpa.kernel.exps.Expression;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.kernel.exps.Parameter;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.Subquery;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.UserException;

/**
 * Expression factory implementation that can be used to execute queries
 * via SQL.
 *
 * @author Abe White
 */
public class JDBCExpressionFactory
    implements ExpressionFactory, Serializable {

    
    private static final long serialVersionUID = 1L;

    private static final Val NULL = new Null();

    private static final Localizer _loc = Localizer.forPackage(JDBCExpressionFactory.class);

    private final ClassMapping _type;
    private final SelectConstructor _cons = new SelectConstructor();
    private int _getMapValueAlias = 0;

    private boolean _isBooleanLiteralAsNumeric = true;

    /**
     * Constructor. Supply the type we're querying against.
     */
    public JDBCExpressionFactory(ClassMapping type) {
        _type = type;
    }

    public void setBooleanLiteralAsNumeric(boolean isBooleanLiteralAsNumeric) {
        _isBooleanLiteralAsNumeric = isBooleanLiteralAsNumeric;
    }

    /**
     * Use to create SQL select.
     */
    public SelectConstructor getSelectConstructor() {
        return _cons;
    }

    @Override
    public Expression emptyExpression() {
        return new EmptyExpression();
    }

    @Override
    public Expression asExpression(Value v) {
        return equal(v, newLiteral(Boolean.TRUE, Literal.TYPE_BOOLEAN));
    }

    @Override
    public Expression equal(Value v1, Value v2) {
        // if we're comparing an unaccessed bound variable, like in:
        // coll.contains (var) && var == x, then translate into:
        // coll.contains (x)
        if (v1 instanceof PCPath && ((PCPath) v1).isUnaccessedVariable())
            return contains(v1, v2);
        if (v2 instanceof PCPath && ((PCPath) v2).isUnaccessedVariable())
            return contains(v2, v1);
        if (v1 instanceof Type || v2 instanceof Type) {
            Value val = v1 instanceof Type ? v1 : v2;
            verifyTypeOperation(val, null, false);
            return new EqualTypeExpression((Val) v1, (Val) v2);
        }
        return new EqualExpression((Val) v1, (Val) v2);
    }

    private void verifyTypeOperation(Value val, Value param, boolean isNotEqual) {
        if (val.getPath() == null)
            return;
        PCPath path = (PCPath) val.getPath();
        Discriminator disc = ((Type) val).getDiscriminator();
        if (disc == null || !(val.getMetaData().getPCSuperclass() != null ||
            val.getMetaData().getPCSubclasses().length > 0))
            throw new UserException(_loc.
                get("invalid-type-argument", path.last() != null ? path.getPCPathString() : path.getSchemaAlias()));

        if (disc.getColumns().length == 0) {
            if (disc.getStrategy() instanceof NoneDiscriminatorStrategy) {
                // limited support for table per class inheritance hierarchy
                if (path.last() != null)
                    throw new UserException(_loc.
                        get("type-argument-unsupported", path.last().getName()));
                if (isNotEqual) {
                    if (param != null && param instanceof Null)
                        throw new UserException(_loc.
                            get("type-in-expression-unsupported", path.getSchemaAlias()));
                    else
                        throw new UserException(_loc.
                            get("type-not-equal-unsupported", path.getSchemaAlias()));
                }
            }
            if (param != null && param instanceof CollectionParam)
                throw new UserException(_loc.
                    get("collection-param-unsupported"));
        }
    }

    @Override
    public Expression notEqual(Value v1, Value v2) {
        if (v1 instanceof Type || v2 instanceof Type) {
            Value val = v1 instanceof Type ? v1 : v2;
            Value param = val == v1 ? (v2 instanceof Null ? v2 : null) : (v1 instanceof Null ? v1 : null);
            verifyTypeOperation(val, param, true);
            return new NotEqualTypeExpression((Val) v1, (Val) v2);
        }
        return new NotEqualExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression lessThan(Value v1, Value v2) {
        return new CompareExpression((Val) v1, (Val) v2,
            CompareExpression.LESS);
    }

    @Override
    public Expression greaterThan(Value v1, Value v2) {
        return new CompareExpression((Val) v1, (Val) v2,
            CompareExpression.GREATER);
    }

    @Override
    public Expression lessThanEqual(Value v1, Value v2) {
        return new CompareExpression((Val) v1, (Val) v2,
            CompareExpression.LESS_EQUAL);
    }

    @Override
    public Expression greaterThanEqual(Value v1, Value v2) {
        return new CompareExpression((Val) v1, (Val) v2,
            CompareExpression.GREATER_EQUAL);
    }

    @Override
    public Expression isEmpty(Value val) {
        return new IsEmptyExpression((Val) val);
    }

    @Override
    public Expression isNotEmpty(Value val) {
        return new IsNotEmptyExpression((Val) val);
    }

    @Override
    public Expression contains(Value map, Value arg) {
        if (map instanceof Const) {
            if (arg instanceof Type) {
                // limited support for table per class inheritance
                verifyTypeOperation(arg, map, false);
                if (((ClassMapping) arg.getMetaData()).getDiscriminator().getColumns().length == 0)
                    return new EqualTypeExpression((Val) arg, (Val) map);
            }

            return new InExpression((Val) arg, (Const) map);
        }
        if (map instanceof SubQ)
            return new InSubQExpression((Val) arg, (SubQ) map);
        return new ContainsExpression((Val) map, (Val) arg);
    }

    @Override
    public Expression containsKey(Value map, Value arg) {
        if (map instanceof Const)
            return new InKeyExpression((Val) arg, (Const) map);
        return new ContainsKeyExpression((Val) map, (Val) arg);
    }

    @Override
    public Expression containsValue(Value map, Value arg) {
        if (map instanceof Const)
            return new InValueExpression((Val) arg, (Const) map);
        return new ContainsExpression((Val) map, (Val) arg);
    }

    @Override
    public Expression isInstance(Value val, Class c) {
        if (val instanceof Const)
            return new ConstInstanceofExpression((Const) val, c);
        return new InstanceofExpression((PCPath) val, c);
    }

    @Override
    public Expression and(Expression exp1, Expression exp2) {
        if (exp1 instanceof BindVariableExpression)
            return new BindVariableAndExpression((BindVariableExpression) exp1,
                (Exp) exp2);
        if (exp2 instanceof BindVariableExpression)
            return new BindVariableAndExpression((BindVariableExpression) exp2,
                (Exp) exp1);
        return new AndExpression((Exp) exp1, (Exp) exp2);
    }

    @Override
    public Expression or(Expression exp1, Expression exp2) {
        return new OrExpression((Exp) exp1, (Exp) exp2);
    }

    @Override
    public Expression not(Expression exp) {
        if (!(exp instanceof IsNotEmptyExpression) &&
            !(exp instanceof InSubQExpression) &&
            HasContainsExpressionVisitor.hasContains(exp))
            return new NotContainsExpression((Exp) exp);
        return new NotExpression((Exp) exp);
    }

    @Override
    public Expression bindVariable(Value var, Value val) {
        // handle the strange case of using a constant path to bind a
        // variable; in these cases the variable acts like an unbound
        // variable that we limit by using an IN clause on the constant
        // value collection
        if (val instanceof Const) {
            PCPath path = new PCPath(_type, (Variable) var);
            path.setMetaData(var.getMetaData());
            return new InExpression(path, (Const) val);
        }
        return new BindVariableExpression((Variable) var, (PCPath) val, false);
    }

    @Override
    public Expression bindKeyVariable(Value var, Value val) {
        // handle the strange case of using a constant path to bind a
        // variable; in these cases the variable acts like an unbound
        // variable that we limit by using an IN clause on the constant
        // value collection
        if (val instanceof Const) {
            PCPath path = new PCPath(_type, (Variable) var);
            path.setMetaData(var.getMetaData());
            return new InKeyExpression(path, (Const) val);
        }
        return new BindVariableExpression((Variable) var, (PCPath) val, true);
    }

    @Override
    public Expression bindValueVariable(Value var, Value val) {
        return bindVariable(var, val);
    }

    @Override
    public Expression startsWith(Value v1, Value v2) {
        return new StartsWithExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression endsWith(Value v1, Value v2) {
        return new EndsWithExpression((Val) v1, (Val) v2);
    }

    @Override
    public Expression notMatches(Value v1, Value v2,
        String single, String multi, String esc) {
        return not(matches(v1, v2, single, multi, esc));
    }

    @Override
    public Expression matches(Value v1, Value v2,
        String single, String multi, String esc) {
        if (!(v2 instanceof Const))
            throw new UserException(_loc.get("const-only", "matches"));
        if (esc == null && _type.getMappingRepository().getDBDictionary().requiresSearchStringEscapeForLike) {
            esc = _type.getMappingRepository().getDBDictionary().searchStringEscape;
        }
        return new MatchesExpression((Val) v1, (Const) v2, single, multi, esc);
    }

    @Override
    public Subquery newSubquery(ClassMetaData candidate, boolean subs,
        String alias) {
        DBDictionary dict = _type.getMappingRepository().getDBDictionary();
        dict.assertSupport(dict.supportsSubselect, "SupportsSubselect");
        return new SubQ((ClassMapping) candidate, subs, alias);
    }

    @Override
    public Path newPath() {
        return new PCPath(_type);
    }

    @Override
    public Path newPath(Value val) {
        if (val instanceof Const)
            return new ConstPath((Const) val);
        if (val instanceof SubQ)
            return new PCPath((SubQ) val);
        return new PCPath(_type, (Variable) val);
    }

    @Override
    public Literal newLiteral(Object val, int ptype) {
        return new Lit(val, ptype);
    }

    @Override
    public Literal newTypeLiteral(Object val, int ptype) {
        return new TypeLit(val, ptype);
    }

    @Override
    public Value getThis() {
        return new PCPath(_type);
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
    public <T extends Date> Value getCurrentTime(Class<T> dateType) {
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
        return new ExtractDateTimeField((Val) value, field);
    }

    @Override
    public Value getDateTimePart(DateTimeExtractPart part, Value value) {
        return new ExtractDateTimePart((Val) value, part);
    }

    @Override
    public Parameter newParameter(Object name, Class type) {
        return new Param(name, type);
    }

    @Override
    public Parameter newCollectionValuedParameter(Object key, Class type) {
        return new CollectionParam(key, type);
    }

    @Override
    public Value newExtension(FilterListener listener, Value target,
        Value arg) {
        return new Extension((JDBCFilterListener) listener,
            (Val) target, (Val) arg, _type);
    }

    @Override
    public Value newAggregate(AggregateListener listener, Value arg) {
        return new Aggregate((JDBCAggregateListener) listener,
            (Val) arg, _type);
    }

    @Override
    public Arguments newArgumentList(Value v1, Value v2) {
        return new Args((Val) v1, (Val) v2);
    }

    @Override
    public Arguments newArgumentList(Value... vs) {
        if (vs == null)
           return new Args(null);
        Val[] vals = new Val[vs.length];
        int i = 0;
        for (Value v : vs) {
            vals[i++] = (Val)v;
        }
        return new Args(vals);
    }

    @Override
    public Value newUnboundVariable(String name, Class type) {
        return new Variable(name, type);
    }

    @Override
    public Value newBoundVariable(String name, Class type) {
        return newUnboundVariable(name, type);
    }

    @Override
    public Value cast(Value val, Class cls) {
        val.setImplicitType(cls);
        return val;
    }

    @Override
    public Value add(Value v1, Value v2) {
        return new Math((Val) v1, (Val) v2, Math.ADD);
    }

    @Override
    public Value subtract(Value v1, Value v2) {
        return new Math((Val) v1, (Val) v2, Math.SUBTRACT);
    }

    @Override
    public Value multiply(Value v1, Value v2) {
        return new Math((Val) v1, (Val) v2, Math.MULTIPLY);
    }

    @Override
    public Value divide(Value v1, Value v2) {
        return new Math((Val) v1, (Val) v2, Math.DIVIDE);
    }

    @Override
    public Value mod(Value v1, Value v2) {
        return new Math((Val) v1, (Val) v2, Math.MOD);
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
        return new NaturalLogarithm((Val) val);
    }

    @Override
    public Value sign(Value val) {
        return new Sign((Val) val);
    }

    @Override
    public Value power(Value base, Value exponent) {
        return new Math((Val) base, (Val) exponent, Math.POWER);
    }

    @Override
    public Value round(Value num, Value precision) {
        return new Math((Val) num, (Val) precision, Math.ROUND);
    }

    @Override
    public Value indexOf(Value v1, Value v2) {
        return new IndexOf((Val) v1, (Val) v2);
    }

    @Override
    public Value concat(Value v1, Value v2) {
        return new Concat((Val) v1, (Val) v2);
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
    public Value substring(Value v1, Value v2) {
        return new Substring((Val) v1, (Val) v2);
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
        ((PCPath) val).verifyIndexedField();
        return new Index((Val) val);
    }

    @Override
    public Value type(Value val) {
        return new Type((Val) val);
    }

    @Override
    public Value mapEntry(Value key, Value val) {
        return new MapEntry((Val) key, (Val) val);
    }

    @Override
    public Value mapKey(Value key, Value val) {
        return new MapKey((Val) key);
    }

    @Override
    public Value getKey(Value val) {
        ((PCPath) val).getKey();
        return val;
    }

    @Override
    public Value getObjectId(Value val) {
        if (val instanceof Const)
            return new ConstGetObjectId((Const) val);
        return new GetObjectId((PCPath) val);
    }

    @Override
    public Value getMapValue(Value map, Value arg) {
        return new GetMapValue((Val) map, (Val) arg,
            "gmv" + _getMapValueAlias++);
    }

    private Value getLiteralRawString(Value val) {
        if (val instanceof Lit) {
            Lit lit = (Lit) val;
            lit.setRaw(true);
        }
        return val;
    }

    @Override
    public Value simpleCaseExpression(Value caseOperand, Expression[] exp,
            Value val1) {
        Exp[] exps = new Exp[exp.length];
        for (int i = 0; i < exp.length; i++)
            exps[i] = (Exp) exp[i];
        val1 = getLiteralRawString(val1);
        return new SimpleCaseExpression((Val) caseOperand, exps,
            (Val) val1);
    }

    @Override
    public Value generalCaseExpression(Expression[] exp,
            Value val) {
        Exp[] exps = new Exp[exp.length];
        for (int i = 0; i < exp.length; i++)
            exps[i] = (Exp) exp[i];
        val = getLiteralRawString(val);
        return new GeneralCaseExpression(exps, (Val) val);
    }

    @Override
    public Expression whenCondition(Expression exp, Value val) {
        val = getLiteralRawString(val);
        return new WhenCondition((Exp) exp, (Val) val);
    }

    @Override
    public Expression whenScalar(Value val1, Value val2) {
        val1 = getLiteralRawString(val1);
        val2 = getLiteralRawString(val2);
        return new WhenScalar((Val) val1, (Val) val2);
    }

    @Override
    public Value coalesceExpression(Value[] vals) {
        Object[] values = new Val[vals.length];
        for (int i = 0; i < vals.length; i++) {
            values[i] = getLiteralRawString(vals[i]);
        }
        return new CoalesceExpression((Val[]) values);
    }

    @Override
    public Value nullIfExpression(Value val1, Value val2) {
        val1 = getLiteralRawString(val1);
        val2 = getLiteralRawString(val2);
        return new NullIfExpression((Val) val1, (Val) val2);
    }

    @Override
    public Value newFunction(String functionName, Class<?> resultType, Value... args) {
        return new DatastoreFunction(functionName, resultType, newArgumentList(args));
    }

    @Override
    public boolean isVerticalType(Value val) {
        if (!(val instanceof Type))
            return false;
        ClassMapping cm = (ClassMapping)((Type)val).getMetaData();
        String strat = cm.getMappingInfo().getHierarchyStrategy();
        if (strat != null && strat.equals(VerticalClassStrategy.ALIAS))
            return true;
        return false;
    }
}
