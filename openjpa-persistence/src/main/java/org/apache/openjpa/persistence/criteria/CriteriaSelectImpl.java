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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.ParameterExpression;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.lib.util.OrderedMap;

/**
 * Represents a set operation (UNION, INTERSECT, EXCEPT) combining
 * two CriteriaSelect operands.
 *
 * @since 4.0.0
 */
public class CriteriaSelectImpl<T> implements CriteriaSelect<T> {

    private final int setOperationType;
    private final CriteriaSelect<?> left;
    private final CriteriaSelect<?> right;
    private final Class<T> resultClass;

    CriteriaSelectImpl(int setOperationType,
                       CriteriaSelect<?> left,
                       CriteriaSelect<?> right,
                       Class<T> resultClass) {
        this.setOperationType = setOperationType;
        this.left = left;
        this.right = right;
        this.resultClass = resultClass;
    }

    int getSetOperationType() {
        return setOperationType;
    }

    CriteriaSelect<?> getLeft() {
        return left;
    }

    CriteriaSelect<?> getRight() {
        return right;
    }

    Class<T> getResultClass() {
        return resultClass;
    }

    QueryExpressions getQueryExpressions(ExpressionFactory factory) {
        QueryExpressions exps = new QueryExpressions();
        exps.setOperationType = setOperationType;
        exps.setOperands = new QueryExpressions[]{
            evalOperand(left, factory),
            evalOperand(right, factory)
        };
        exps.operation =
            org.apache.openjpa.kernel.QueryOperations.OP_SELECT;

        QueryExpressions leftExps = exps.setOperands[0];
        exps.accessPath = leftExps.accessPath;
        exps.parameterTypes = collectParameters();
        return exps;
    }

    private QueryExpressions evalOperand(CriteriaSelect<?> operand,
                                         ExpressionFactory factory) {
        if (operand instanceof CriteriaSelectImpl) {
            return ((CriteriaSelectImpl<?>) operand)
                .getQueryExpressions(factory);
        }
        return ((CriteriaQueryImpl<?>) operand)
            .getQueryExpressions(factory);
    }

    OrderedMap<Object, Class<?>> collectParameters() {
        OrderedMap<Object, Class<?>> params = new OrderedMap<>();
        collectParameters(left, params);
        collectParameters(right, params);
        return params;
    }

    private void collectParameters(CriteriaSelect<?> operand,
                                   OrderedMap<Object, Class<?>> params) {
        if (operand instanceof CriteriaSelectImpl) {
            OrderedMap<Object, Class<?>> sub =
                ((CriteriaSelectImpl<?>) operand).collectParameters();
            for (Object key : sub.keySet()) {
                params.put(key, sub.get(key));
            }
        } else {
            OrderedMap<Object, Class<?>> sub =
                ((CriteriaQueryImpl<?>) operand).getParameterTypes();
            for (Object key : sub.keySet()) {
                params.put(key, sub.get(key));
            }
        }
    }

    public Set<ParameterExpression<?>> getParameters() {
        Set<ParameterExpression<?>> result = new HashSet<>();
        collectParameterExpressions(left, result);
        collectParameterExpressions(right, result);
        return result;
    }

    private void collectParameterExpressions(
            CriteriaSelect<?> operand,
            Set<ParameterExpression<?>> result) {
        if (operand instanceof CriteriaSelectImpl) {
            result.addAll(
                ((CriteriaSelectImpl<?>) operand).getParameters());
        } else {
            result.addAll(
                ((CriteriaQueryImpl<?>) operand).getParameters());
        }
    }

    public void compile() {
        compileOperand(left);
        compileOperand(right);
    }

    private void compileOperand(CriteriaSelect<?> operand) {
        if (operand instanceof CriteriaSelectImpl) {
            ((CriteriaSelectImpl<?>) operand).compile();
        } else {
            ((OpenJPACriteriaQuery<?>) operand).compile();
        }
    }

    @Override
    public String toString() {
        String keyword;
        switch (setOperationType) {
            case QueryExpressions.SET_OP_UNION:
                keyword = " UNION ";
                break;
            case QueryExpressions.SET_OP_UNION_ALL:
                keyword = " UNION ALL ";
                break;
            case QueryExpressions.SET_OP_INTERSECT:
                keyword = " INTERSECT ";
                break;
            case QueryExpressions.SET_OP_INTERSECT_ALL:
                keyword = " INTERSECT ALL ";
                break;
            case QueryExpressions.SET_OP_EXCEPT:
                keyword = " EXCEPT ";
                break;
            case QueryExpressions.SET_OP_EXCEPT_ALL:
                keyword = " EXCEPT ALL ";
                break;
            default:
                keyword = " ??? ";
                break;
        }
        return "(" + left + ")" + keyword + "(" + right + ")";
    }
}
