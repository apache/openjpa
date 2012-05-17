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
package org.apache.openjpa.kernel.jpql;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.ExpressionStoreQuery;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.exps.AbstractExpressionBuilder;
import org.apache.openjpa.kernel.exps.Expression;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Literal;
import org.apache.openjpa.kernel.exps.Parameter;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Subquery;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.UserException;
import serp.util.Numbers;

/**
 * Builder for JPQL expressions. This class takes the query parsed
 * in {@link JPQL} and converts it to an expression tree using
 * an {@link ExpressionFactory}. Public for unit testing purposes.
 *
 * @author Marc Prud'hommeaux
 * @author Patrick Linskey
 * @nojavadoc
 */
public class JPQLExpressionBuilder
    extends AbstractExpressionBuilder
    implements JPQLTreeConstants {

    private static final int VAR_PATH = 1;
    private static final int VAR_ERROR = 2;

    private static Localizer _loc = Localizer.forPackage
        (JPQLExpressionBuilder.class);

    private final Stack contexts = new Stack();
    private LinkedMap parameterTypes;
    private int aliasCount = 0;

    /**
     * Constructor.
     *
     * @param factory the expression factory to use
     * @param query used to resolve variables, parameters,
     * and class names used in the query
     * @param parsedQuery the parsed query
     */
    public JPQLExpressionBuilder(ExpressionFactory factory,
        ExpressionStoreQuery query, Object parsedQuery) {
        super(factory, query.getResolver());

        contexts.push(new Context(parsedQuery instanceof ParsedJPQL
            ? (ParsedJPQL) parsedQuery
            : parsedQuery instanceof String
            ? getParsedQuery((String) parsedQuery)
            : null, null));

        if (ctx().parsed == null)
            throw new InternalException(parsedQuery + "");
    }

    protected Localizer getLocalizer() {
        return _loc;
    }

    protected ClassLoader getClassLoader() {
        // we don't resolve in the context of anything but ourselves
        return getClass().getClassLoader();
    }

    protected ParsedJPQL getParsedQuery() {
        return ctx().parsed;
    }

    protected ParsedJPQL getParsedQuery(String jpql) {
        return new ParsedJPQL(jpql);
    }

    private void setCandidate(ClassMetaData cmd, String schemaAlias) {
        addAccessPath(cmd);

        if (cmd != null)
            ctx().meta = cmd;

        if (schemaAlias != null)
            ctx().schemaAlias = schemaAlias;
    }

    private String nextAlias() {
        return "jpqlalias" + (++aliasCount);
    }

    protected ClassMetaData resolveClassMetaData(JPQLNode node) {
        // handle looking up alias names
        String schemaName = assertSchemaName(node);
        ClassMetaData cmd = getClassMetaData(schemaName, false);
        if (cmd != null)
            return cmd;

        // we might be referencing a collection field of a subquery's parent
        if (isPath(node)) {
            Path path = getPath(node);
            return getFieldType(path.last());
        }

        // now run again to throw the correct exception
        return getClassMetaData(schemaName, true);
    }

    private ClassMetaData getClassMetaData(String alias, boolean assertValid) {
        ClassLoader loader = getClassLoader();
        MetaDataRepository repos = resolver.getConfiguration().
            getMetaDataRepositoryInstance();

        // first check for the alias
        ClassMetaData cmd = repos.getMetaData(alias, loader, false);

        if (cmd != null)
            return cmd;

        // now check for the class name; this is not technically permitted
        // by the JPA spec, but is required in order to be able to execute
        // JPQL queries from other facades (like JDO) that do not have
        // the concept of entity names or aliases
        Class c = resolver.classForName(alias, null);
        if (c != null)
            cmd = repos.getMetaData(c, loader, assertValid);
        else if (assertValid)
            cmd = repos.getMetaData(alias, loader, true);

        if (cmd == null && assertValid)
            throw parseException(EX_USER, "not-schema-name",
                new Object[]{ alias }, null);

        return cmd;
    }

    private Class getCandidateType() {
        return getCandidateMetaData().getDescribedType();
    }

    private ClassMetaData getCandidateMetaData() {
        if (ctx().meta != null)
            return ctx().meta;

        ClassMetaData cls = getCandidateMetaData(root());
        if (cls == null)
            throw parseException(EX_USER, "not-schema-name",
                new Object[]{ root() }, null);

        setCandidate(cls, null);
        return cls;
    }

    protected ClassMetaData getCandidateMetaData(JPQLNode node) {
        // examing the node to find the candidate query
        // ### this should actually be the primary SELECT instance
        // resolved against the from variable declarations
        JPQLNode from = node.findChildByID(JJTFROMITEM, true);
        if (from == null) {
            // OPENJPA-15 allow subquery without a FROMITEM
            if (node.id == JJTSUBSELECT) { 
                from = node.findChildByID(JJTFROM, true);
            }
            else {
                throw parseException(EX_USER, "no-from-clause", null, null);
            }
        }

        for (int i = 0; i < from.children.length; i++) {
            JPQLNode n = from.children[i];

            if (n.id == JJTABSTRACTSCHEMANAME) {
                // we simply return the first abstract schema child
                // as resolved into a class
                ClassMetaData cmd = resolveClassMetaData(n);

                if (cmd != null)
                    return cmd;

                // not a schema: treat it as a class
                String cls = assertSchemaName(n);
                if (cls == null)
                    throw parseException(EX_USER, "not-schema-name",
                        new Object[]{ root() }, null);

                return getClassMetaData(cls, true);
            }
            // OPENJPA-15 support subquery's from clause do not start with 
            // identification_variable_declaration()
            if (node.id == JJTSUBSELECT) {
                if (n.id == JJTINNERJOIN) {
                    n = n.getChild(0);
                }
                if (n.id == JJTPATH) {
                    Path path = getPath(n);
                    ClassMetaData cmd = getFieldType(path.last());
                    if (cmd != null) {
                        return cmd;
                    }
                    else {
                        throw parseException(EX_USER, "no-alias", 
                                new Object[]{ n }, null);
                    }
                }
            }           
        }

        return null;
    }

    protected String currentQuery() {
        return ctx().parsed == null || root().parser == null ? null
            : root().parser.jpql;
    }

    QueryExpressions getQueryExpressions() {
        QueryExpressions exps = new QueryExpressions();

        evalQueryOperation(exps);

        Expression filter = null;
        filter = and(evalFromClause(root().id == JJTSELECT), filter);
        filter = and(evalWhereClause(exps), filter);
        filter = and(evalSelectClause(exps), filter);

        exps.filter = filter == null ? factory.emptyExpression() : filter;

        evalGroupingClause(exps);
        evalHavingClause(exps);
        evalFetchJoins(exps);
        evalSetClause(exps);
        evalOrderingClauses(exps);

        if (parameterTypes != null)
            exps.parameterTypes = parameterTypes;

        exps.accessPath = getAccessPath();

        return exps;
    }

    private Expression and(Expression e1, Expression e2) {
        return e1 == null ? e2 : e2 == null ? e1 : factory.and(e1, e2);
    }

    private static String assemble(JPQLNode node) {
        return assemble(node, ".", 0);
    }

    /**
     * Assemble the children of the specific node by appending each
     * child, separated by the delimiter.
     */
    private static String assemble(JPQLNode node, String delimiter, int last) {
        StringBuffer result = new StringBuffer();
        JPQLNode[] parts = node.children;
        for (int i = 0; parts != null && i < parts.length - last; i++)
            result.append(result.length() > 0 ? delimiter : "").
                append(parts[i].text);

        return result.toString();
    }

    private Expression assignProjections(JPQLNode parametersNode,
        QueryExpressions exps) {
        int count = parametersNode.getChildCount();
        exps.projections = new Value[count];
        exps.projectionClauses = new String[count];
        exps.projectionAliases = new String[count];

        Expression exp = null;
        for (int i = 0; i < count; i++) {
            JPQLNode parent = parametersNode.getChild(i);
            JPQLNode node = onlyChild(parent);
            Value proj = getValue(node);
            exps.projections[i] = proj;
            exps.projectionAliases[i] = nextAlias();
        }
        return exp;
    }

    private void evalQueryOperation(QueryExpressions exps) {
        // determine whether we want to select, delete, or update
        if (root().id == JJTSELECT || root().id == JJTSUBSELECT)
            exps.operation = QueryOperations.OP_SELECT;
        else if (root().id == JJTDELETE)
            exps.operation = QueryOperations.OP_DELETE;
        else if (root().id == JJTUPDATE)
            exps.operation = QueryOperations.OP_UPDATE;
        else
            throw parseException(EX_UNSUPPORTED, "unrecognized-operation",
                new Object[]{ root() }, null);
    }

    private void evalGroupingClause(QueryExpressions exps) {
        // handle GROUP BY clauses
        JPQLNode groupByNode = root().findChildByID(JJTGROUPBY, true);

        if (groupByNode == null)
            return;

        int groupByCount = groupByNode.getChildCount();

        exps.grouping = new Value[groupByCount];

        for (int i = 0; i < groupByCount; i++) {
            JPQLNode node = groupByNode.getChild(i);
            exps.grouping[i] = getValue(node);
        }
    }

    private void evalHavingClause(QueryExpressions exps) {
        // handle HAVING clauses
        JPQLNode havingNode = root().findChildByID(JJTHAVING, true);

        if (havingNode == null)
            return;

        exps.having = getExpression(onlyChild(havingNode));
    }

    private void evalOrderingClauses(QueryExpressions exps) {
        // handle ORDER BY clauses
        JPQLNode orderby = root().findChildByID(JJTORDERBY, false);
        if (orderby != null) {
            int ordercount = orderby.getChildCount();
            exps.ordering = new Value[ordercount];
            exps.ascending = new boolean[ordercount];
            for (int i = 0; i < ordercount; i++) {
                JPQLNode node = orderby.getChild(i);
                exps.ordering[i] = getValue(firstChild(node));
                // ommission of ASC/DESC token implies ascending
                exps.ascending[i] = node.getChildCount() <= 1 ||
                    lastChild(node).id == JJTASCENDING ? true : false;
            }
        }
    }

    private Expression evalSelectClause(QueryExpressions exps) {
        if (exps.operation != QueryOperations.OP_SELECT)
            return null;

        JPQLNode selectNode = root();

        JPQLNode selectClause = selectNode.
            findChildByID(JJTSELECTCLAUSE, false);
        if (selectClause != null && selectClause.hasChildID(JJTDISTINCT))
            exps.distinct = exps.DISTINCT_TRUE | exps.DISTINCT_AUTO;
        else
            exps.distinct = exps.DISTINCT_FALSE;

        JPQLNode constructor = selectNode.findChildByID(JJTCONSTRUCTOR, true);
        if (constructor != null) {
            // build up the fully-qualified result class name by
            // appending together the components of the children
            String resultClassName = assemble(left(constructor));
            exps.resultClass = resolver.classForName(resultClassName, null);

            // now assign the arguments to the select clause as the projections
            return assignProjections(right(constructor), exps);
        } else {
            // handle SELECT clauses
            JPQLNode expNode = selectNode.
                findChildByID(JJTSELECTEXPRESSIONS, true);
            if (expNode == null)
                return null;

            int selectCount = expNode.getChildCount();
            JPQLNode selectChild = firstChild(expNode);

            // if we are selecting just one thing and that thing is the
            // schema's alias, then do not treat it as a projection
            if (selectCount == 1 && selectChild != null &&
                selectChild.getChildCount() == 1 &&
                onlyChild(selectChild) != null &&
                assertSchemaAlias().
                    equalsIgnoreCase(onlyChild(selectChild).text)) {
                return null;
            } else {
                // JPQL does not filter relational joins for projections
                exps.distinct &= ~exps.DISTINCT_AUTO;
                return assignProjections(expNode, exps);
            }
        }
    }

    private String assertSchemaAlias() {
        String alias = ctx().schemaAlias;

        if (alias == null)
            throw parseException(EX_USER, "alias-required",
                new Object[]{ ctx().meta }, null);

        return alias;
    }

    protected Expression evalFetchJoins(QueryExpressions exps) {
        Expression filter = null;

        // handle JOIN FETCH
        Set joins = null;
        Set innerJoins = null;

        JPQLNode[] outers = root().findChildrenByID(JJTOUTERFETCHJOIN);
        for (int i = 0; outers != null && i < outers.length; i++)
            (joins == null ? joins = new TreeSet() : joins).
                add(getPath(onlyChild(outers[i])).last().getFullName(false));

        JPQLNode[] inners = root().findChildrenByID(JJTINNERFETCHJOIN);
        for (int i = 0; inners != null && i < inners.length; i++) {
            String path = getPath(onlyChild(inners[i])).last()
                .getFullName(false);
            (joins == null ? joins = new TreeSet() : joins).add(path);
            (innerJoins == null ? innerJoins = new TreeSet() : innerJoins).
                add(path);
        }

        if (joins != null)
            exps.fetchPaths = (String[]) joins.
                toArray(new String[joins.size()]);
        if (innerJoins != null)
            exps.fetchInnerPaths = (String[]) innerJoins.
                toArray(new String[innerJoins.size()]);

        return filter;
    }

    protected void evalSetClause(QueryExpressions exps) {
        // handle SET field = value
        JPQLNode[] nodes = root().findChildrenByID(JJTUPDATEITEM);
        for (int i = 0; nodes != null && i < nodes.length; i++) {
            FieldMetaData field = getPath(firstChild(nodes[i])).last();
            Value val = getValue(onlyChild(lastChild(nodes[i])));
            exps.putUpdate(field, val);
        }
    }

    private Expression evalWhereClause(QueryExpressions exps) {
        // evaluate the WHERE clause
        JPQLNode whereNode = root().findChildByID(JJTWHERE, false);
        if (whereNode == null)
            return null;
        return (Expression) eval(whereNode);
    }

    private Expression evalFromClause(boolean needsAlias) {
        Expression exp = null;

        // build up the alias map in the FROM clause
        JPQLNode from = root().findChildByID(JJTFROM, false);
        if (from == null)
            throw parseException(EX_USER, "no-from-clause", null, null);

        for (int i = 0; i < from.children.length; i++) {
            JPQLNode node = from.children[i];

            if (node.id == JJTFROMITEM)
                exp = evalFromItem(exp, node, needsAlias);
            else if (node.id == JJTOUTERJOIN)
                exp = addJoin(node, false, exp);
            else if (node.id == JJTINNERJOIN)
                exp = addJoin(node, true, exp);
            else if (node.id == JJTINNERFETCHJOIN)
                ; // we handle inner fetch joins in the evalFetchJoins() method
            else if (node.id == JJTOUTERFETCHJOIN)
                ; // we handle outer fetch joins in the evalFetchJoins() method
            else
                throw parseException(EX_USER, "not-schema-name",
                    new Object[]{ node }, null);
        }

        return exp;
    }

    /**
     * Adds a join condition to the given expression.
     *
     * @param node the node to check
     * @param inner whether or not the join should be an inner join
     * @param exp an existing expression to AND, or null if none
     * @return the Expression with the join condition added
     */
    private Expression addJoin(JPQLNode node, boolean inner, Expression exp) {
        // the type will be the declared type for the field
        Path path = getPath(firstChild(node), false, inner);

        JPQLNode alias = node.getChildCount() >= 2 ? right(node) : null;
        // OPENJPA-15 support subquery's from clause do not start with 
        // identification_variable_declaration()
        if (inner && ctx().subquery != null && ctx().schemaAlias == null) {
            setCandidate(getFieldType(path.last()), alias.text);

            Path subpath = factory.newPath(ctx().subquery);
            subpath.setMetaData(ctx().subquery.getMetaData());
            exp =  and(exp, factory.equal(path, subpath));
        }

        return addJoin(path, alias, inner, exp);
    }

    private Expression addJoin(Path path, JPQLNode aliasNode, boolean inner,
        Expression exp) {
        FieldMetaData fmd = path.last();

        if (fmd == null)
            throw parseException(EX_USER, "path-no-meta",
                new Object[]{ path, null }, null);

        String alias = aliasNode != null ? aliasNode.text : nextAlias();

        Value var = getVariable(alias, true);
        var.setMetaData(getFieldType(fmd));

        Expression join = null;

        // if the variable is already bound, get the var's value and
        // do a regular contains with that
        boolean bound = isBound(var);
        if (bound) {
            var = getValue(aliasNode, VAR_PATH);
        } else {
            bind(var);
            join = and(join, factory.bindVariable(var, path));
        }

        if (!fmd.isTypePC()) // multi-valued relation
        {
            if (bound)
                join = and(join, factory.contains(path, var));

            setImplicitContainsTypes(path, var, CONTAINS_TYPE_ELEMENT);
        }

        return and(exp, join);
    }

    private Expression evalFromItem(Expression exp, JPQLNode node,
        boolean needsAlias) {
        ClassMetaData cmd = resolveClassMetaData(firstChild(node));

        String alias = null;

        if (node.getChildCount() < 2) {
            if (needsAlias)
                throw parseException(EX_USER, "alias-required",
                    new Object[]{ cmd }, null);
        } else {
            alias = right(node).text;
            JPQLNode left = left(node);

            // check to see if the we are referring to a path in the from
            // clause, since we might be in a subquery against a collection
            if (isPath(left)) {
                Path path = getPath(left);
                setCandidate(getFieldType(path.last()), alias);

                Path subpath = factory.newPath(ctx().subquery);
                subpath.setMetaData(ctx().subquery.getMetaData());
                return and(exp, factory.equal(path, subpath));
            } else {
                // we have an alias: bind it as a variable
                Value var = getVariable(alias, true);
                var.setMetaData(cmd);
                bind(var);
            }
        }

        // ### we assign the first FROMITEM instance we see as
        // the global candidate, which is incorrect: we should
        // instead be mapping this to the SELECTITEM to see
        // which is the desired candidate
        if (ctx().schemaAlias == null)
            setCandidate(cmd, alias);

        return exp;
    }

    protected boolean isDeclaredVariable(String name) {
        // JPQL doesn't support declaring variables
        return false;
    }

    /**
     * Check to see if the specific node is a path (vs. a schema name)
     */
    boolean isPath(JPQLNode node) {
        if (node.getChildCount() < 2)
            return false;

        final String name = firstChild(node).text;
        if (name == null)
            return false;

        // handle the case where the class name is the alias
        // for the candidate (we don't use variables for this)
        if (getMetaDataForAlias(name) != null)
            return true;

        if (!isSeenVariable(name))
            return false;

        final Value var = getVariable(name, false);

        if (var != null)
            return isBound(var);

        return false;
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

        return cmd;
    }

    /**
     * Identification variables in JPQL are case insensitive, so lower-case
     * all variables we are going to bind.
     */
    protected Value getVariable(String id, boolean bind) {
        if (id == null)
            return null;

        return super.getVariable(id.toLowerCase(), bind);
    }

    protected boolean isSeendVariable(String id) {
        return id != null && super.isSeenVariable(id.toLowerCase());
    }

    /**
     * Returns the class name using the children of the JPQLNode.
     */
    private String assertSchemaName(JPQLNode node) {
        if (node.id != JJTABSTRACTSCHEMANAME)
            throw parseException(EX_USER, "not-identifer",
                new Object[]{ node }, null);

        return assemble(node);
    }

    /**
     * Recursive helper method to evaluate the given node.
     */
    private Object eval(JPQLNode node) {
        Value val1 = null;
        Value val2 = null;
        Value val3 = null;

        boolean not = node.not;

        switch (node.id) {
            case JJTWHERE: // top-level WHERE clause
                return getExpression(onlyChild(node));

            case JJTBOOLEANLITERAL:
                return factory.newLiteral("true".equalsIgnoreCase
                    (node.text) ? Boolean.TRUE : Boolean.FALSE,
                    Literal.TYPE_BOOLEAN);

            case JJTINTEGERLITERAL:
                // use BigDecimal because it can handle parsing exponents
                BigDecimal intlit = new BigDecimal
                    (node.text.endsWith("l") || node.text.endsWith("L")
                        ? node.text.substring(0, node.text.length() - 1)
                        : node.text).
                    multiply(new BigDecimal(negative(node)));
                return factory.newLiteral(new Long(intlit.longValue()),
                    Literal.TYPE_NUMBER);

            case JJTDECIMALLITERAL:
                BigDecimal declit = new BigDecimal
                    (node.text.endsWith("d") || node.text.endsWith("D") ||
                        node.text.endsWith("f") || node.text.endsWith("F")
                        ? node.text.substring(0, node.text.length() - 1)
                        : node.text).
                    multiply(new BigDecimal(negative(node)));
                return factory.newLiteral(declit, Literal.TYPE_NUMBER);

            case JJTSTRINGLITERAL:
            case JJTTRIMCHARACTER:
            case JJTESCAPECHARACTER:
                return factory.newLiteral(trimQuotes(node.text),
                    Literal.TYPE_SQ_STRING);

            case JJTPATTERNVALUE:
                return eval(firstChild(node));

            case JJTNAMEDINPUTPARAMETER:
                return getParameter(node.text, false);

            case JJTPOSITIONALINPUTPARAMETER:
                return getParameter(node.text, true);

            case JJTOR: // x OR y
                return factory.or(getExpression(left(node)),
                    getExpression(right(node)));

            case JJTAND: // x AND y
                return and(getExpression(left(node)),
                    getExpression(right(node)));

            case JJTEQUALS: // x = y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, null);
                return factory.equal(val1, val2);

            case JJTNOTEQUALS: // x <> y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, null);
                return factory.notEqual(val1, val2);

            case JJTLESSTHAN: // x < y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, null);
                return factory.lessThan(val1, val2);

            case JJTLESSOREQUAL: // x <= y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, null);
                return factory.lessThanEqual(val1, val2);

            case JJTGREATERTHAN: // x > y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, null);
                return factory.greaterThan(val1, val2);

            case JJTGREATEROREQUAL: // x >= y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, null);
                return factory.greaterThanEqual(val1, val2);

            case JJTADD: // x + y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, TYPE_NUMBER);
                return factory.add(val1, val2);

            case JJTSUBTRACT: // x - y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, TYPE_NUMBER);
                return factory.subtract(val1, val2);

            case JJTMULTIPLY: // x * y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, TYPE_NUMBER);
                return factory.multiply(val1, val2);

            case JJTDIVIDE: // x / y
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, TYPE_NUMBER);
                return factory.divide(val1, val2);

            case JJTBETWEEN: // x.field [NOT] BETWEEN 5 AND 10
                val1 = getValue(child(node, 0, 3));
                val2 = getValue(child(node, 1, 3));
                val3 = getValue(child(node, 2, 3));
                setImplicitTypes(val1, val2, null);
                setImplicitTypes(val1, val3, null);
                return evalNot(not, and(factory.greaterThanEqual(val1, val2),
                    factory.lessThanEqual(val1, val3)));

            case JJTIN: // x.field [NOT] IN ('a', 'b', 'c')

                Expression inExp = null;
                Iterator inIterator = node.iterator();
                // the first child is the path
                val1 = getValue((JPQLNode) inIterator.next());

                while (inIterator.hasNext()) {
                    val2 = getValue((JPQLNode) inIterator.next());

                    // special case for <value> IN (<subquery>) or
                    // <value> IN (<single value>)
                    if (!(val2 instanceof Literal) && node.getChildCount() == 2)
                        return evalNot(not, factory.contains(val2, val1)); 

                    // this is currently a sequence of OR expressions, since we
                    // do not have support for IN expressions
                    setImplicitTypes(val1, val2, null);
                    if (inExp == null)
                        inExp = factory.equal(val1, val2);
                    else
                        inExp = factory.or(inExp, factory.equal(val1, val2));
                }

                // we additionally need to add in a "NOT NULL" clause, since
                // the IN behavior that is expected by the CTS also expects
                // to filter our NULLs
                return and(evalNot(not, inExp),
                    factory.notEqual(val1, factory.getNull()));

            case JJTISNULL: // x.field IS [NOT] NULL
                if (not)
                    return factory.notEqual
                        (getValue(onlyChild(node)), factory.getNull());
                else
                    return factory.equal
                        (getValue(onlyChild(node)), factory.getNull());

            case JJTPATH:
                return getPathOrConstant(node);

            case JJTIDENTIFIER:
            case JJTIDENTIFICATIONVARIABLE:
                return getIdentifier(node);

            case JJTNOT:
                return factory.not(getExpression(onlyChild(node)));

            case JJTLIKE: // field LIKE '%someval%'
                val1 = getValue(left(node));
                val2 = getValue(right(node));

                setImplicitType(val1, TYPE_STRING);
                setImplicitType(val2, TYPE_STRING);

                // look for an escape character beneath the node
                String escape = null;
                JPQLNode escapeNode = right(node).
                    findChildByID(JJTESCAPECHARACTER, true);
                if (escapeNode != null)
                    escape = trimQuotes(onlyChild(escapeNode).text);

                if (not)
                    return factory.notMatches(val1, val2, "_", "%", escape);
                else
                    return factory.matches(val1, val2, "_", "%", escape);

            case JJTISEMPTY:
                return evalNot(not,
                    factory.isEmpty(getValue(onlyChild(node))));

            case JJTSIZE:
                return factory.size(getValue(onlyChild(node)));

            case JJTUPPER:
                val1 = getValue(onlyChild(node));
                setImplicitType(val1, TYPE_STRING);
                return factory.toUpperCase(val1);

            case JJTLOWER:
                return factory.toLowerCase(getStringValue(onlyChild(node)));

            case JJTLENGTH:
                return factory.stringLength(getStringValue(onlyChild(node)));

            case JJTABS:
                return factory.abs(getNumberValue(onlyChild(node)));

            case JJTSQRT:
                return factory.sqrt(getNumberValue(onlyChild(node)));

            case JJTMOD:
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitTypes(val1, val2, TYPE_NUMBER);
                return factory.mod(val1, val2);

            case JJTTRIM: // TRIM([[where] [char] FROM] field)
                val1 = getValue(lastChild(node));
                setImplicitType(val1, TYPE_STRING);

                Boolean trimWhere = null;

                JPQLNode firstTrimChild = firstChild(node);

                if (node.getChildCount() > 1) {
                    trimWhere =
                        firstTrimChild.id == JJTTRIMLEADING ? Boolean.TRUE
                            :
                            firstTrimChild.id == JJTTRIMTRAILING ? Boolean.FALSE
                                : null;
                }

                Value trimChar;

                // if there are 3 children, then we know the trim
                // char is the second node
                if (node.getChildCount() == 3)
                    trimChar = getValue(secondChild(node));
                    // if there are two children, then we need to check to see
                    // if the first child is a leading/trailing/both node,
                    // or the trim character node
                else if (node.getChildCount() == 2
                    && firstTrimChild.id != JJTTRIMLEADING
                    && firstTrimChild.id != JJTTRIMTRAILING
                    && firstTrimChild.id != JJTTRIMBOTH)
                    trimChar = getValue(firstChild(node));
                    // othwerwise, we default to trimming the space character
                else
                    trimChar = factory.newLiteral(" ", Literal.TYPE_STRING);

                return factory.trim(val1, trimChar, trimWhere);

            case JJTCONCAT:
                val1 = getValue(left(node));
                val2 = getValue(right(node));
                setImplicitType(val1, TYPE_STRING);
                setImplicitType(val2, TYPE_STRING);
                return factory.concat(val1, val2);

            case JJTSUBSTRING:
                val1 = getValue(child(node, 0, 3));
                val2 = getValue(child(node, 1, 3));
                val3 = getValue(child(node, 2, 3));
                setImplicitType(val1, TYPE_STRING);
                setImplicitType(val2, Integer.TYPE);
                setImplicitType(val3, Integer.TYPE);

                // the semantics of the JPQL substring() function
                // are that arg2 is the 1-based start index, and arg3 is
                // the length of the string to be return; this is different
                // than the semantics of the ExpressionFactory's substring,
                // which matches the Java language (0-based start index,
                // arg2 is the end index): we perform the translation by
                // adding one to the first argument, and then adding the
                // first argument to the second argument to get the endIndex
                //
                // ### we could get rid of some messy expressions by checking for
                // the common case where the arguments are specified as
                // a literal, in which case we could just do the calculations
                // in memory; otherwise we wind up with ugly looking SQL like:
                // SELECT ... FROM ... t1
                // (SUBSTRING(t1.ASTR, (? - ?) + 1, (? + (? - ?)) - ((? - ?))) = ?)
                // [params=(long) 2, (int) 1, (long) 2, (long) 2, (int) 1,
                // (long) 2, (int) 1, (String) oo
                return factory.substring(val1, factory.newArgumentList
                    (factory.subtract(val2, factory.newLiteral
                        (Numbers.valueOf(1), Literal.TYPE_NUMBER)),
                        (factory.add(val3,
                            (factory.subtract(val2, factory.newLiteral
                                (Numbers.valueOf(1), Literal.TYPE_NUMBER)))))));

            case JJTLOCATE:
                // as with SUBSTRING (above), the semantics for LOCATE differ
                // from ExpressionFactory.indexOf in that LOCATE uses a
                // 0-based index, and indexOf uses a 1-based index
                Value locatePath = getValue(firstChild(node));
                Value locateSearch = getValue(secondChild(node));
                Value locateFromIndex = null;
                if (node.getChildCount() > 2) // optional start index arg
                    locateFromIndex = getValue(thirdChild(node));

                setImplicitType(locatePath, TYPE_STRING);
                setImplicitType(locateSearch, TYPE_STRING);

                if (locateFromIndex != null)
                    setImplicitType(locateFromIndex, TYPE_STRING);

                return factory.add(factory.indexOf(locateSearch,
                    locateFromIndex == null ? locatePath
                        : factory.newArgumentList(locatePath,
                        factory.subtract(locateFromIndex,
                            factory.newLiteral(Numbers.valueOf(1),
                                Literal.TYPE_NUMBER)))),
                    factory.newLiteral(Numbers.valueOf(1),
                        Literal.TYPE_NUMBER));

            case JJTAGGREGATE:
                // simply pass-through while asserting a single child
                return eval(onlyChild(node));

            case JJTCOUNT:
                return factory.count(getValue(lastChild(node)));

            case JJTMAX:
                return factory.max(getNumberValue(onlyChild(node)));

            case JJTMIN:
                return factory.min(getNumberValue(onlyChild(node)));

            case JJTSUM:
                return factory.sum(getNumberValue(onlyChild(node)));

            case JJTAVERAGE:
                return factory.avg(getNumberValue(onlyChild(node)));

            case JJTDISTINCTPATH:
                return factory.distinct(getValue(onlyChild(node)));

            case JJTEXISTS:
                return factory.isNotEmpty((Value) eval(onlyChild(node)));

            case JJTANY:
                return factory.any((Value) eval(onlyChild(node)));

            case JJTALL:
                return factory.all((Value) eval(onlyChild(node)));

            case JJTSUBSELECT:
                return getSubquery(node);

            case JJTMEMBEROF:
                val1 = getValue(left(node), VAR_PATH);
                val2 = getValue(right(node), VAR_PATH);
                setImplicitContainsTypes(val2, val1, CONTAINS_TYPE_ELEMENT);
                return evalNot(not, factory.contains(val2, val1));

            case JJTCURRENTDATE:
                return factory.getCurrentDate();

            case JJTCURRENTTIME:
                return factory.getCurrentTime();

            case JJTCURRENTTIMESTAMP:
                return factory.getCurrentTimestamp();

            default:
                throw parseException(EX_FATAL, "bad-tree",
                    new Object[]{ node }, null);
        }
    }

    protected void setImplicitTypes(Value val1, Value val2, Class expected) {
        super.setImplicitTypes(val1, val2, expected);

        // as well as setting the types for conversions, we also need to
        // ensure that any parameters are declared with the correct type,
        // since the JPA spec expects that these will be validated
        Parameter param = val1 instanceof Parameter ? (Parameter) val1
            : val2 instanceof Parameter ? (Parameter) val2 : null;
        Path path = val1 instanceof Path ? (Path) val1
            : val2 instanceof Path ? (Path) val2 : null;

        // we only check for parameter-to-path comparisons
        if (param == null || path == null || parameterTypes == null)
            return;

        FieldMetaData fmd = path.last();
        if (fmd == null)
            return;

        Class type = path.isXPath() ? path.getType() : fmd.getDeclaredType();
        if (type == null)
            return;

        String paramName = param.getParameterName();
        if (paramName == null)
            return;

        // make sure we have already declared the parameter
        if (parameterTypes.containsKey(paramName))
            parameterTypes.put(paramName, type);
    }

    private Value getStringValue(JPQLNode node) {
        return getTypeValue(node, TYPE_STRING);
    }

    private Value getNumberValue(JPQLNode node) {
        return getTypeValue(node, TYPE_NUMBER);
    }

    private Value getTypeValue(JPQLNode node, Class implicitType) {
        Value val = getValue(node);
        setImplicitType(val, implicitType);
        return val;
    }

    private Value getSubquery(JPQLNode node) {
        final boolean subclasses = true;
        String alias = nextAlias();

        // parse the subquery
        ParsedJPQL parsed = new ParsedJPQL(node.parser.jpql, node);

        ClassMetaData candidate = getCandidateMetaData(node);
        Subquery subq = factory.newSubquery(candidate, subclasses, alias);
        subq.setMetaData(candidate);

        contexts.push(new Context(parsed, subq));

        try {
            QueryExpressions subexp = getQueryExpressions();
            subq.setQueryExpressions(subexp);
            return subq;
        } finally {
            // remove the subquery parse context
            contexts.pop();
        }
    }

    /**
     * Record the names and order of implicit parameters.
     */
    private Parameter getParameter(String id, boolean positional) {
        if (parameterTypes == null)
            parameterTypes = new LinkedMap(6);
        if (!parameterTypes.containsKey(id))
            parameterTypes.put(id, TYPE_OBJECT);

        Class type = Object.class;
        ClassMetaData meta = null;
        int index;

        if (positional) {
            try {
                // indexes in JPQL are 1-based, as opposed to 0-based in
                // the core ExpressionFactory
                index = Integer.parseInt(id) - 1;
            } catch (NumberFormatException e) {
                throw parseException(EX_USER, "bad-positional-parameter",
                    new Object[]{ id }, e);
            }

            if (index < 0)
                throw parseException(EX_USER, "bad-positional-parameter",
                    new Object[]{ id }, null);
        } else {
            // otherwise the index is just the current size of the params
            index = parameterTypes.indexOf(id);
        }

        Parameter param = factory.newParameter(id, type);
        param.setMetaData(meta);
        param.setIndex(index);

        return param;
    }

    /**
     * Checks to see if we should evaluate for a NOT expression.
     */
    private Expression evalNot(boolean not, Expression exp) {
        return not ? factory.not(exp) : exp;
    }

    /**
     * Trim off leading and trailing single-quotes, and then
     * replace any internal '' instances with ' (since repeating the
     * quote is the JPQL mechanism of escaping a single quote).
     */
    private String trimQuotes(String str) {
        if (str == null || str.length() <= 1)
            return str;

        if (str.startsWith("'") && str.endsWith("'"))
            str = str.substring(1, str.length() - 1);

        int index = -1;

        while ((index = str.indexOf("''", index + 1)) != -1)
            str = str.substring(0, index + 1) + str.substring(index + 2);

        return str;
    }

    /**
     * An IntegerLiteral and DecimalLiteral node will
     * have a child node of Negative if it is negative:
     * if so, this method returns -1, else it returns 1.
     */
    private short negative(JPQLNode node) {
        if (node.children != null && node.children.length == 1
            && firstChild(node).id == JJTNEGATIVE)
            return -1;
        else
            return 1;
    }

    private Value getIdentifier(JPQLNode node) {
        final String name = node.text;
        final Value val = getVariable(name, false);

        ClassMetaData cmd = getMetaDataForAlias(name);

        if (cmd != null) {
            // handle the case where the class name is the alias
            // for the candidate (we don't use variables for this)
            Value thiz = factory.getThis();
            thiz.setMetaData(cmd);
            return thiz;
        } else if (val instanceof Path) {
            return (Path) val;
        } else if (val instanceof Value) {
            return (Value) val;
        }

        throw parseException(EX_USER, "unknown-identifier",
            new Object[]{ name }, null);
    }

    private Value getPathOrConstant(JPQLNode node) {
        // first check to see if the path is an enum or static field, and
        // if so, load it
        String className = assemble(node, ".", 1);
        Class c = resolver.classForName(className, null);
        if (c != null) {
            String fieldName = lastChild(node).text;

            try {
                Field field = c.getField(fieldName);
                Object value = field.get(null);
                return factory.newLiteral(value, Literal.TYPE_UNKNOWN);
            } catch (NoSuchFieldException nsfe) {
                throw parseException(EX_USER, "no-field",
                    new Object[]{ className, fieldName }, nsfe);
            } catch (Exception e) {
                throw parseException(EX_USER, "unaccessible-field",
                    new Object[]{ className, fieldName }, e);
            }
        } else {
            return getPath(node, false, true);
        }
    }

    private Path getPath(JPQLNode node) {
        return getPath(node, false, true);
    }

    private Path getPath(JPQLNode node, boolean pcOnly, boolean inner) {
        // resolve the first element against the aliases map ...
        // i.e., the path "SELECT x.id FROM SomeClass x where x.id > 10"
        // will need to have "x" in the alias map in order to resolve
        Path path;

        final String name = firstChild(node).text;
        final Value val = getVariable(name, false);

        // handle the case where the class name is the alias
        // for the candidate (we don't use variables for this)
        if (name.equalsIgnoreCase(ctx().schemaAlias)) {
            if (ctx().subquery != null) {
                path = factory.newPath(ctx().subquery);
                path.setMetaData(ctx().subquery.getMetaData());
            } else {
                path = factory.newPath();
                path.setMetaData(ctx().meta);
            }
        } else if (getMetaDataForAlias(name) != null)
            path = newPath(null, getMetaDataForAlias(name));
        else if (val instanceof Path)
            path = (Path) val;
        else if (val.getMetaData() != null)
            path = newPath(val, val.getMetaData());
        else
            throw parseException(EX_USER, "path-no-meta",
                new Object[]{ assemble(node), null }, null);

        // walk through the children and assemble the path
        boolean allowNull = !inner;
        for (int i = 1; i < node.children.length; i++) {
            if (path.isXPath()) {
                for (int j = i; j <node.children.length; j++)
                    path = (Path) traverseXPath(path, node.children[j].text);
                return path;
            }
            path = (Path) traversePath(path, node.children[i].text, pcOnly,
                allowNull);

            // all traversals but the first one will always be inner joins
            allowNull = false;
        }

        return path;
    }

    protected Class getDeclaredVariableType(String name) {
        ClassMetaData cmd = getMetaDataForAlias(name);
        if (cmd != null)
            return cmd.getDescribedType();

        if (name != null && name.equals(ctx().schemaAlias))
            return getCandidateType();

        // JPQL has no declared variables
        return null;
    }

    /**
     * Returns an Expression for the given node by eval'ing it.
     */
    private Expression getExpression(JPQLNode node) {
        Object exp = eval(node);

        // check for boolean values used as expressions
        if (!(exp instanceof Expression))
            return factory.asExpression((Value) exp);
        return (Expression) exp;
    }

    private Value getValue(JPQLNode node) {
        return getValue(node, VAR_PATH);
    }

    private Path newPath(Value val, ClassMetaData meta) {
        Path path = val == null ? factory.newPath() : factory.newPath(val);
        if (meta != null)
            path.setMetaData(meta);
        return path;
    }

    /**
     * Returns a Value for the given node by eval'ing it.
     */
    private Value getValue(JPQLNode node, int handleVar) {
        Value val = (Value) eval(node);

        // determind how to evauate a variabe
        if (!val.isVariable())
            return val;
        else if (handleVar == VAR_PATH && !(val instanceof Path))
            return newPath(val, val.getMetaData());
        else if (handleVar == VAR_ERROR)
            throw parseException(EX_USER, "unexpected-var",
                new Object[]{ node.text }, null);
        else
            return val;
    }

    ////////////////////////////
    // Parse Context Management
    ////////////////////////////

    private Context ctx() {
        return (Context) contexts.peek();
    }

    private JPQLNode root() {
        return ctx().parsed.root;
    }

    private ClassMetaData getMetaDataForAlias(String alias) {
        for (int i = contexts.size() - 1; i >= 0; i--) {
            Context context = (Context) contexts.get(i);
            if (alias.equalsIgnoreCase(context.schemaAlias))
                return context.meta;
        }

        return null;
    }

    private class Context {

        private final ParsedJPQL parsed;
        private ClassMetaData meta;
        private String schemaAlias;
        private Subquery subquery;

        Context(ParsedJPQL parsed, Subquery subquery) {
            this.parsed = parsed;
            this.subquery = subquery;
        }
    }

    ////////////////////////////
    // Node traversal utilities
    ////////////////////////////

    private JPQLNode onlyChild(JPQLNode node)
        throws UserException {
        JPQLNode child = firstChild(node);

        if (node.children.length > 1)
            throw parseException(EX_USER, "multi-children",
                new Object[]{ node, Arrays.asList(node.children) }, null);

        return child;
    }

    /**
     * Returns the left node (the first of the children), and asserts
     * that there are exactly two children.
     */
    private JPQLNode left(JPQLNode node) {
        return child(node, 0, 2);
    }

    /**
     * Returns the right node (the second of the children), and asserts
     * that there are exactly two children.
     */
    private JPQLNode right(JPQLNode node) {
        return child(node, 1, 2);
    }

    private JPQLNode child(JPQLNode node, int childNum, int assertCount) {
        if (node.children.length != assertCount)
            throw parseException(EX_USER, "wrong-child-count",
                new Object[]{ new Integer(assertCount), node,
                    Arrays.asList(node.children) }, null);

        return node.children[childNum];
    }

    private JPQLNode firstChild(JPQLNode node) {
        if (node.children == null || node.children.length == 0)
            throw parseException(EX_USER, "no-children",
                new Object[]{ node }, null);
        return node.children[0];
    }

    private static JPQLNode secondChild(JPQLNode node) {
        return node.children[1];
    }

    private static JPQLNode thirdChild(JPQLNode node) {
        return node.children[2];
    }

    private static JPQLNode lastChild(JPQLNode node) {
        return lastChild(node, 0);
    }

    /**
     * The Nth from the last child. E.g.,
     * lastChild(1) will return the second-to-the-last child.
     */
    private static JPQLNode lastChild(JPQLNode node, int fromLast) {
        return node.children[node.children.length - (1 + fromLast)];
    }

    /**
     * Base node that will be generated by the JPQLExpressionBuilder; base
     * class of the {@link SimpleNode} that is used by {@link JPQL}.
     *
     * @author Marc Prud'hommeaux
     * @see Node
     * @see SimpleNode
     */
    protected abstract static class JPQLNode
        implements Node, Serializable {

        final int id;
        final JPQL parser;
        JPQLNode parent;
        JPQLNode[] children;
        String text;
        boolean not = false;

        public JPQLNode(JPQL parser, int id) {
            this.id = id;
            this.parser = parser;
        }

        public void jjtOpen() {
        }

        public void jjtClose() {
        }

        JPQLNode[] findChildrenByID(int id) {
            Collection set = new HashSet();
            findChildrenByID(id, set);
            return (JPQLNode[]) set.toArray(new JPQLNode[set.size()]);
        }

        private void findChildrenByID(int id, Collection set) {
            for (int i = 0; children != null && i < children.length; i++) {
                if (children[i].id == id)
                    set.add(children[i]);

                children[i].findChildrenByID(id, set);
            }
        }

        boolean hasChildID(int id) {
            return findChildByID(id, false) != null;
        }

        JPQLNode findChildByID(int id, boolean recurse) {
            for (int i = 0; children != null && i < children.length; i++) {
                JPQLNode child = children[i];

                if (child.id == id)
                    return children[i];

                if (recurse) {
                    JPQLNode found = child.findChildByID(id, recurse);
                    if (found != null)
                        return found;
                }
            }

            // not found
            return null;
        }

        public void jjtSetParent(Node parent) {
            this.parent = (JPQLNode) parent;
        }

        public Node jjtGetParent() {
            return this.parent;
        }

        public void jjtAddChild(Node n, int i) {
            if (children == null) {
                children = new JPQLNode[i + 1];
            } else if (i >= children.length) {
                JPQLNode c[] = new JPQLNode[i + 1];
                System.arraycopy(children, 0, c, 0, children.length);
                children = c;
            }

            children[i] = (JPQLNode) n;
        }

        public Node jjtGetChild(int i) {
            return children[i];
        }

        public int getChildCount() {
            return jjtGetNumChildren();
        }

        public JPQLNode getChild(int index) {
            return (JPQLNode) jjtGetChild(index);
        }

        public Iterator iterator() {
            return Arrays.asList(children).iterator();
        }

        public int jjtGetNumChildren() {
            return (children == null) ? 0 : children.length;
        }

        void setText(String text) {
            this.text = text;
        }

        void setToken(Token t) {
            setText(t.image);
        }

        public String toString() {
            return JPQLTreeConstants.jjtNodeName[this.id];
        }

        public String toString(String prefix) {
            return prefix + toString();
        }

        /**
         * Debugging method.
         *
         * @see #dump(java.io.PrintStream,String)
         */
        public void dump(String prefix) {
            dump(System.out, prefix);
        }

        public void dump() {
            dump(" ");
        }

        /**
         * Debugging method to output a parse tree.
         *
         * @param out the stream to which to write the debugging info
         * @param prefix the prefix to write out before lines
         */
        public void dump(PrintStream out, String prefix) {
            dump(out, prefix, false);
        }

        public void dump(PrintStream out, String prefix, boolean text) {
            out.println(toString(prefix)
                + (text && this.text != null ? " [" + this.text + "]" : ""));
            if (children != null) {
                for (int i = 0; i < children.length; ++i) {
                    JPQLNode n = (JPQLNode) children[i];
                    if (n != null) {
                        n.dump(out, prefix + " ", text);
                    }
                }
            }
        }
    }

    /**
     * Public for unit testing purposes.
     * @nojavadoc
     */
    public static class ParsedJPQL
        implements Serializable {

        protected final JPQLNode root;
        protected final String query;
        
        // cache of candidate type data. This is stored here in case this  
        // parse tree is reused in a context that does not know what the 
        // candidate type is already. 
        private Class _candidateType;

        ParsedJPQL(String jpql) {
            this(jpql, parse(jpql));
        }

        ParsedJPQL(String query, JPQLNode root) {
            this.root = root;
            this.query = query;
        }

        private static final JPQLNode parse(String jpql) {
            if (jpql == null)
                jpql = "";

            try {
                return (JPQLNode) new JPQL(jpql).parseQuery();
            } catch (Error e) {
                // special handling for Error subclasses, which the
                // parser may sometimes (unfortunately) throw
                throw new UserException(_loc.get("parse-error",
                    new Object[]{ e.toString(), jpql }));
            } catch (ParseException e) {
                throw new UserException(_loc.get("parse-error",
                    new Object[]{ e.toString(), jpql }), e);
            }
        }

        void populate(ExpressionStoreQuery query) {
            QueryContext ctx = query.getContext();

            // if the owning query's context does not have
            // any candidate class, then set it here
            if (ctx.getCandidateType() == null) {
                if (_candidateType == null)
                    _candidateType = new JPQLExpressionBuilder
                        (null, query, this).getCandidateType();
                ctx.setCandidateType(_candidateType, true);
            }
        }
        
        /**
         * Public for unit testing purposes.
         */
        public Class getCandidateType() {
            return _candidateType;
        }

        public String toString ()
		{
			return this.query;
		}
	}
}

