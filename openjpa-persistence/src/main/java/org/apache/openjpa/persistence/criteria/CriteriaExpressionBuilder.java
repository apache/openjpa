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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Converts expressions of a CriteriaQuery to kernel Expression.
 * 
 * @author Fay Wang
 * 
 */
public class CriteriaExpressionBuilder {
    
    public QueryExpressions getQueryExpressions(ExpressionFactory factory, 
        CriteriaQueryImpl q) {
        QueryExpressions exps = new QueryExpressions();

        evalAccessPaths(exps, factory, q);
        //exps.alias = null;      // String   
        exps.ascending = new boolean[]{false};
        evalDistinct(exps, factory, q);
        evalFetchJoin(exps, factory, q);

        evalFilter(exps, factory, q);

        evalGrouping(exps, factory, q);

        evalOrdering(exps, factory, q);
        //exps.operation = QueryOperations.OP_SELECT;

        evalProjections(exps, factory, q);


        //exps.range = null; // Value[]
        //exps.resultClass = null; // Class
        exps.parameterTypes = q.getParameterTypes();
        return exps;
    }

    protected void evalAccessPaths(QueryExpressions exps, 
        ExpressionFactory factory, CriteriaQueryImpl q) {
        Set<ClassMetaData> metas = new HashSet<ClassMetaData>();
        Set<Root<?>> roots = q.getRoots();
        if (roots != null) {
            MetamodelImpl metamodel = q.getMetamodel();    
            for (Root<?> root : roots) {
                metas.add(((Types.Managed<?>)root.getModel()).meta);
                if (root.getJoins() != null) {
                    for (Join<?,?> join : root.getJoins()) {
                        Class<?> cls = join.getMember().getMemberJavaType();
                        if (join.getMember().isAssociation()) {
                            ClassMetaData meta = metamodel.repos.
                                getMetaData(cls, null, true);
                            PersistenceType type = metamodel.
                                getPersistenceType(meta);
                            if (type == PersistenceType.ENTITY ||
                                type == PersistenceType.EMBEDDABLE) 
                                metas.add(meta);
                        }
                    }
                }
            }
        }
        exps.accessPath = metas.toArray(new ClassMetaData[metas.size()]);
    }

    protected void evalOrdering(QueryExpressions exps, 
        ExpressionFactory factory, CriteriaQueryImpl q) {
        List<Order> orders = q.getOrderList();
        MetamodelImpl model = q.getMetamodel(); 
        if (orders == null) 
            return;
        int ordercount = orders.size();
        exps.ordering = new Value[ordercount];
        exps.orderingClauses = new String[ordercount];
        exps.orderingAliases = new String[ordercount];
        exps.ascending = new boolean[ordercount];
        for (int i = 0; i < ordercount; i++) {
            OrderImpl order = (OrderImpl)orders.get(i);
            //Expression<? extends Comparable> expr = order.getExpression();
            Expression expr = order.getExpression5();
            exps.ordering[i] = Expressions.toValue(
                    (ExpressionImpl<?>)expr, factory, model, q);

            //exps.orderingClauses[i] = assemble(firstChild);
            //exps.orderingAliases[i] = firstChild.text;
            exps.ascending[i] = order.isAscending();
        }
    }

    protected void evalGrouping(QueryExpressions exps, 
        ExpressionFactory factory, CriteriaQueryImpl q) {
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
            exps.grouping[i] = Expressions.toValue(
                    (ExpressionImpl<?>)groupBy, factory, model, q);;
        }

        exps.having = having == null ? factory.emptyExpression() 
                : having.toKernelExpression(factory, model, q);
    }

    protected void evalDistinct(QueryExpressions exps, 
        ExpressionFactory factory, CriteriaQueryImpl q) {
        Boolean distinct = q.getDistinct();
        if (distinct == null) {
            exps.distinct = QueryExpressions.DISTINCT_FALSE;
        } else if (distinct) {
            exps.distinct = QueryExpressions.DISTINCT_TRUE 
            | QueryExpressions.DISTINCT_AUTO;
        }
        //exps.distinct &= ~QueryExpressions.DISTINCT_AUTO;
    }

    protected void evalFilter(QueryExpressions exps, ExpressionFactory factory,
        CriteriaQueryImpl q) {
        Set<Root<?>> roots = q.getRoots();
        MetamodelImpl model = q.getMetamodel();
        PredicateImpl where = q.getRestriction();
        q.assertRoot();
        org.apache.openjpa.kernel.exps.Expression filter = null;
        for (Root<?> root : roots) {
            if (root.getJoins() != null) {
                for (Join<?, ?> join : root.getJoins()) {
                    filter = and(factory, ((ExpressionImpl<?>)join)
                        .toKernelExpression(factory, model, q), filter);
                }
            }
            if (((RootImpl<?>)root).getCorrelatedParent() != null) {
                filter = and(factory, ((RootImpl<?>)root)
                    .toKernelExpression(factory, model, q), filter);
            }
        }
        if (where != null) {
            filter = and(factory, where.toKernelExpression
                    (factory, model, q), filter);
        }
        if (filter == null) 
            filter = factory.emptyExpression();
        exps.filter = filter;
    }

    protected void evalProjections(QueryExpressions exps, 
        ExpressionFactory factory, CriteriaQueryImpl q) {
        List<Selection<?>> selections = q.getSelectionList();
        MetamodelImpl model = q.getMetamodel();
        // TODO: fill in projection clauses
        //    exps.projectionClauses = null; // String[]
        if (isDefaultProjection(selections, q)) {
            exps.projections = new Value[0];
            return ;
        }
        exps.projections = new Value[selections.size()];
        List<Value> projections = new ArrayList<Value>();
        List<String> aliases = new ArrayList<String>();
        getProjections(exps, selections, projections, aliases, factory, q, 
            model);
        exps.projections = projections.toArray(new Value[0]);
        exps.projectionAliases = aliases.toArray(new String[0]);
    }

    private void getProjections(QueryExpressions exps, 
        List<Selection<?>> selections, List projections, List aliases, 
        ExpressionFactory factory, CriteriaQueryImpl q, MetamodelImpl model) {
        for (Selection<?> s : selections) {
            List<Selection<?>> sels = ((SelectionImpl)s).getSelections();
            if (sels == null) {
                projections.add(((ExpressionImpl<?>)s).
                    toValue(factory, model, q));
                aliases.add(q.getAlias(s));
            } else {
                // this is for constructor expression in the selection
                exps.resultClass = s.getJavaType();
                getProjections(exps, sels, projections, aliases, factory, q, 
                    model);
            }            
        }
    }

    protected boolean isDefaultProjection(List<Selection<?>> selections, 
        CriteriaQueryImpl q) {
        return selections == null 
        || (selections.size() == 1 && selections.get(0) == q.getRoot());
    }

    protected void evalFetchJoin(QueryExpressions exps, 
        ExpressionFactory factory, CriteriaQueryImpl q) {
        //exps.fetchInnerPaths = null; // String[]
        //exps.fetchPaths = null;      // String[]
    }

    protected static org.apache.openjpa.kernel.exps.Expression and (
        ExpressionFactory factory,
        org.apache.openjpa.kernel.exps.Expression e1, 
        org.apache.openjpa.kernel.exps.Expression e2) {
        return e1 == null ? e2 : e2 == null ? e1 : factory.and(e1, e2);
    }
}
