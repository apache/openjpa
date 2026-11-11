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
package org.apache.openjpa.persistence.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Criteria API validation fixes.
 */
class TestCriteriaAPIValidation extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(AllFieldTypes.class, CLEAR_TABLES);
    }

    /**
     * ParameterExpression.getPosition() should return null for named parameters.
     */
    @Test
    void parameterExpressionPositionIsNull() {
        CriteriaBuilder cb = emf.getCriteriaBuilder();
        ParameterExpression<String> param = cb.parameter(String.class, "myParam");
        assertNull(param.getPosition());
        assertEquals("myParam", param.getName());
    }

    /**
     * CriteriaBuilder.literal(null) should throw IllegalArgumentException.
     */
    @Test
    void literalNullThrows() {
        CriteriaBuilder cb = emf.getCriteriaBuilder();
        try {
            cb.literal(null);
            fail("Expected IllegalArgumentException for literal(null)");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /**
     * CriteriaBuilder.tuple(tuple(...)) should throw IllegalArgumentException
     * because compound selections cannot be nested.
     */
    @Test
    void nestedTupleThrows() {
        CriteriaBuilder cb = emf.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<AllFieldTypes> root = cq.from(AllFieldTypes.class);

        Selection<?>[] selections = { root.get("intField"), root.get("stringField") };
        CompoundSelection<Tuple> inner = cb.tuple(selections);
        try {
            cb.tuple(inner);
            fail("Expected IllegalArgumentException for nested tuple");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /**
     * CriteriaBuilder.array(array(...)) should throw IllegalArgumentException
     * because compound selections cannot be nested.
     */
    @Test
    void nestedArrayThrows() {
        CriteriaBuilder cb = emf.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<AllFieldTypes> root = cq.from(AllFieldTypes.class);

        Selection<?>[] selections = { root.get("intField"), root.get("stringField") };
        CompoundSelection<Object[]> inner = cb.array(selections);
        try {
            cb.array(inner);
            fail("Expected IllegalArgumentException for nested array");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /**
     * CriteriaBuilder.tuple(array(...)) should throw IllegalArgumentException.
     */
    @Test
    void tupleContainingArrayThrows() {
        CriteriaBuilder cb = emf.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<AllFieldTypes> root = cq.from(AllFieldTypes.class);

        Selection<?>[] selections = { root.get("intField"), root.get("stringField") };
        CompoundSelection<Object[]> inner = cb.array(selections);
        try {
            cb.tuple(inner);
            fail("Expected IllegalArgumentException for tuple containing array");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }
}
