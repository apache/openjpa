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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Tuple;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.ResultShape;
import org.apache.openjpa.kernel.exps.AbstractExpressionBuilder;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.TupleImpl;
import org.apache.openjpa.persistence.meta.AbstractManagedType;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Converts expressions of a CriteriaQuery to kernel Expression.
 * 
 * @author Fay Wang
 * 
 */
public class CriteriaExpressionBuilder {
    
    public QueryExpressions getQueryExpressions(ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        QueryExpressions exps = new QueryExpressions();
        exps.setContexts(q.getContexts());

        evalAccessPaths(exps, factory, q);
        //exps.alias = null;      // String   
        evalDistinct(exps, factory, q);
        evalFetchJoin(exps, factory, q);
        evalCrossJoinRoots(exps, factory, q);
        evalFilter(exps, factory, q);

        evalGrouping(exps, factory, q);
        
        evalOrderingAndProjection(exps, factory, q);

        exps.operation = QueryOperations.OP_SELECT;
        //exps.range = null; // Value[]
        exps.resultClass = q.getResultType();
        exps.parameterTypes = q.getParameterTypes();
        return exps;
    }

    protected void evalAccessPaths(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        Set<ClassMetaData> metas = new HashSet<ClassMetaData>();
        Set<Root<?>> roots = q.getRoots();
        if (roots != null) {
            MetamodelImpl metamodel = q.getMetamodel();    
            for (Root<?> root : roots) {
                metas.add(((AbstractManagedType<?>)root.getModel()).meta);
                if (root.getJoins() != null) {
                    for (Join<?,?> join : root.getJoins()) {
                        Class<?> cls = join.getAttribute().getJavaType();
                        if (join.getAttribute().isAssociation()) {
                            ClassMetaData meta = metamodel.repos.getMetaData(cls, null, true);
                            PersistenceType type = MetamodelImpl.getPersistenceType(meta);
                            if (type == PersistenceType.ENTITY || type == PersistenceType.EMBEDDABLE) 
                                metas.add(meta);
                        }
                    }
                    if (root.getFetches() != null) {
                        for (Fetch<?,?> fetch : root.getFetches()) {
                            metas.add(metamodel.repos.getMetaData(fetch.getAttribute().getJavaType(), null, false));
                        }
                    }
                }
            }
        }
        exps.accessPath = metas.toArray(new ClassMetaData[metas.size()]);
    }

    protected void evalOrderingAndProjection(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        Map<ExpressionImpl<?>, Value> exp2Vals = evalOrdering(exps, factory, q);
        evalProjections(exps, factory, q, exp2Vals, q.getResultShape());
    }
    
    /**
     * Evaluates the ordering expressions by converting them to kernel values.
     * Sets the ordering fields of kernel QueryExpressions.
     *   
     * @param exps kernel QueryExpressions
     * @param factory for kernel expressions
     * @param q a criteria query
     * 
     * @return map of kernel values indexed by criteria query expressions that created it.
     * These kernel values are required to be held in a map to avoid recomputing for the
     * same CriteriaQuery Expressions appearing in ordering terms as well as projection
     * term. 
     * 
     */
    protected Map<ExpressionImpl<?>, Value> evalOrdering(QueryExpressions exps, ExpressionFactory factory, 
        CriteriaQueryImpl<?> q) {
        List<Order> orders = q.getOrderList();
        MetamodelImpl model = q.getMetamodel(); 
        int ordercount = (orders == null) ? 0 : orders.size();
        Map<ExpressionImpl<?>, Value> exp2Vals = new HashMap<ExpressionImpl<?>, Value>();
        exps.ordering = new Value[ordercount];
        exps.orderingClauses = new String[ordercount];
        exps.orderingAliases = new String[ordercount];
        exps.ascending = new boolean[ordercount];
        for (int i = 0; i < ordercount; i++) {
            OrderImpl order = (OrderImpl)orders.get(i);
            ExpressionImpl<?> expr = order.getExpression();
            Value val = Expressions.toValue(expr, factory, model, q);
            exps.ordering[i] = val;
            String alias = expr.getAlias();
            exps.orderingAliases[i] = alias;
            exps.orderingClauses[i] = "";
            val.setAlias(alias);
            exps.ascending[i] = order.isAscending();
            exp2Vals.put(expr, val);
        }
        return exp2Vals;
    }

    protected void evalGrouping(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        //    exps.grouping = null; // Value[]
        //    exps.groupingClauses = null; // String[]
        List<Expression<?>> groups = q.getGroupList();
        MetamodelImpl model = q.getMetamodel();
        PredicateImpl having = q.getGroupRestriction();
        if (groups == null) 
            return;
        int groupByCount = groups.size();
        exps.grouping = new Value[groupByCount];
        for (int i = 0; i < groupByCount; i++) {
            Expression<?> groupBy = groups.get(i);    
            exps.grouping[i] = Expressions.toValue((ExpressionImpl<?>)groupBy, factory, model, q);;
        }

        exps.having = having == null ? factory.emptyExpression() : having.toKernelExpression(factory, model, q);
    }

    protected void evalDistinct(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        Boolean distinct = q.getDistinct();
        if (distinct == null) {
            exps.distinct = QueryExpressions.DISTINCT_FALSE;
        } else if (distinct) {
            exps.distinct = QueryExpressions.DISTINCT_TRUE 
            | QueryExpressions.DISTINCT_AUTO;
        }
        //exps.distinct &= ~QueryExpressions.DISTINCT_AUTO;
    }

    protected void evalCrossJoinRoots(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        Set<Root<?>> roots = q.getRoots();
        MetamodelImpl model = q.getMetamodel();
        SubqueryImpl<?> subQuery = q.getDelegator();
        if (subQuery == null || subQuery.getCorrelatedJoins() == null) {
            q.assertRoot();
            if (roots.size() > 1) { // cross join
                for (Root<?> root : roots) {
                    String alias = q.getAlias(root);
                    Value var = factory.newBoundVariable(alias, AbstractExpressionBuilder.TYPE_OBJECT);
                    var.setMetaData(((Types.Entity)root.getModel()).meta);
                    q.registerRoot(root, var);
                }
            }
        }
    }
    
    protected void evalFilter(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        Set<Root<?>> roots = q.getRoots();
        MetamodelImpl model = q.getMetamodel();
        PredicateImpl where = q.getRestriction();
        SubqueryImpl<?> subQuery = q.getDelegator();
        org.apache.openjpa.kernel.exps.Expression filter = null;
        if (subQuery == null || subQuery.getCorrelatedJoins() == null) {
            q.assertRoot();
            for (Root<?> root : roots) {
                if (root.getJoins() != null) {
                    for (Join<?, ?> join : root.getJoins()) {
                        filter = and(factory, ((ExpressionImpl<?>)join).toKernelExpression(factory, model, q), filter);
                    }
                }
                ((RootImpl<?>)root).addToContext(factory, model, q);
            }
        }
        if (subQuery != null) {
            List<Join<?,?>> corrJoins = subQuery.getCorrelatedJoins();
            if (corrJoins != null) {
                for (int i = 0; i < corrJoins.size(); i++) 
                    filter = and(factory, ((ExpressionImpl<?>)corrJoins.get(i)).toKernelExpression(factory, model, q),  
                            filter);
            }
        }
        
        if (where != null) {
            filter = and(factory, where.toKernelExpression(factory, model, q), filter);
        }
        if (filter == null) {
            filter = factory.emptyExpression();
        }
        exps.filter = filter;
    }

    protected void evalProjections(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q,
        Map<ExpressionImpl<?>, Value> exp2Vals, ResultShape<?> shape) {
        List<Selection<?>> selections = q.getSelectionList();
        MetamodelImpl model = q.getMetamodel();
        if (isDefaultProjection(selections, q)) {
            exps.projections = new Value[0];
            return ;
        }
        exps.projections = new Value[selections.size()];
        List<Value> projections = new ArrayList<Value>();
        List<String> aliases = new ArrayList<String>();
        List<String> clauses = new ArrayList<String>();
        getProjections(exps, selections, projections, aliases, clauses, factory, q, model, exp2Vals, shape);
        exps.projections = projections.toArray(new Value[projections.size()]);
        exps.projectionAliases = aliases.toArray(new String[aliases.size()]);
        exps.projectionClauses = clauses.toArray(new String[clauses.size()]);
        exps.shape = shape;
    }

    /**
     * Scans the projection terms to populate the kernel QueryExpressions projection clauses
     * and aliases.
     *   
     * @param exps 
     * @param selections
     * @param projections list of kernel values for projections 
     * @param aliases list of kernel projection aliases 
     * @param clauses list of kernel projection clauses
     * @param factory for kernel expressions
     * @param q a Criteria Query
     * @param model of domain entities
     * @param exp2Vals the evaluated kernel values indexed by the Criteria Expressions
     */
    private void getProjections(QueryExpressions exps, List<Selection<?>> selections, 
        List<Value> projections, List<String> aliases, List<String> clauses, 
        ExpressionFactory factory, CriteriaQueryImpl<?> q, MetamodelImpl model, 
        Map<ExpressionImpl<?>, Value> exp2Vals, ResultShape<?> shape) {
        for (Selection<?> s : selections) {
            if (s.isCompoundSelection()) {
                getProjections(exps, s.getCompoundSelectionItems(), projections, aliases, 
                    clauses, factory, q, model, exp2Vals, q.getNestedShape(s));
            } else {
                Value val = (exp2Vals != null && exp2Vals.containsKey(s) 
                        ? exp2Vals.get(s) : ((ExpressionImpl<?>)s).toValue(factory, model, q));
                String alias = s.getAlias();
                val.setAlias(alias);
                projections.add(val);
                aliases.add(alias);
                clauses.add(alias);
            }         
        }
    }
    

    protected boolean isDefaultProjection(List<Selection<?>> selections, CriteriaQueryImpl<?> q) {
        if (selections == null)
            return true;
        if (selections.size() != 1)
            return false;
        Selection<?> sel = selections.get(0);
        if (q.getRoots() != null && sel == q.getRoot())
            return true;
        if ((sel instanceof PathImpl<?,?>) && ((PathImpl<?,?>)sel)._correlatedPath != null)
            return true;
        return false;
    }

    protected void evalFetchJoin(QueryExpressions exps, ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        List<String> iPaths = new ArrayList<String>();
        List<String> oPaths = new ArrayList<String>();
        Set<Root<?>> roots = q.getRoots();
        if (roots == null)
            return;
        for (Root root : roots) {
            Set<Fetch> fetches = root.getFetches();
            if (fetches == null)
                continue;
            for (Fetch<?,?> fetch : fetches) {
                String fPath = ((Members.Member<?, ?>)fetch.getAttribute())
                   .fmd.getFullName(false);
                oPaths.add(fPath);
                if (fetch.getJoinType() == JoinType.INNER) {
                   iPaths.add(fPath);
                } 
            }
        }
        if (!iPaths.isEmpty())
            exps.fetchInnerPaths = iPaths.toArray(new String[iPaths.size()]);
        if (!oPaths.isEmpty())
            exps.fetchPaths = oPaths.toArray(new String[oPaths.size()]);
    }

    protected static org.apache.openjpa.kernel.exps.Expression and (ExpressionFactory factory,
        org.apache.openjpa.kernel.exps.Expression e1, org.apache.openjpa.kernel.exps.Expression e2) {
        return e1 == null ? e2 : e2 == null ? e1 : factory.and(e1, e2);
    }
}
