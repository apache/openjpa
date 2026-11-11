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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.openjpa.kernel.jpql.JPQLExpressionBuilder.JPQLNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestJPQLParser {

    @Test
    void simpleJPQLExtractFieldFromPath() {
        Assertions.assertDoesNotThrow(() -> {
            String query = "SELECT a FROM Usuario AS a where (extract(year from a.dateOfBirth) - 2000) < 25";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        });
    }

    @Test
    void simpleJPQLExtractFieldFromDate() {
        try {
            String query = "SELECT a FROM Usuario AS a where extract (DAY from {d '2005-04-13'}) = 10";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void jpql() {
        try {
            String query = "SELECT c FROM CompUser AS u WHERE EXTRACT (YEAR FROM {d '2006-03-21'}) > 2005";
            assertNotNull(new JPQL(query).parseQuery());
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleJPQLExtractPart() {
        try {
            String query = "SELECT a FROM Usuario AS a where extract(date from a.dateOfBirth) = {d '2025-07-12'}";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToString() {
        try {
            String query = "SELECT u FROM User AS u WHERE CAST((EXTRACT(year FROM u.dateOfBirth)) AS STRING) = '1983'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToStringOnSelect() {
        try {
            String query = "SELECT CAST(u.birthYear AS string) FROM User AS u WHERE EXTRACT(year FROM u.dateOfBirth) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToInteger() {
        try {
            String query = "SELECT u FROM User AS u WHERE CAST('1983' AS INTEGER) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToIntegerOnSelect() {
        try {
            String query = "SELECT CAST(u.birthYear as Integer) FROM User AS u WHERE extract(year from u.dateOfBirth) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToLong() {
        try {
            String query = "SELECT u FROM User AS u WHERE CAST('1983' AS LONG) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToLongOnSelect() {
        try {
            String query = "SELECT CAST(u.birthYear as long) FROM User AS u WHERE extract(year from u.dateOfBirth) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToFloat() {
        try {
            String query = "SELECT u FROM User AS u WHERE CAST('1983' AS FLOAT) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToFloatOnSelect() {
        try {
            String query = "SELECT CAST(u.birthYear as FLOAT) FROM User AS u WHERE extract(year from u.dateOfBirth) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToDouble() {
        try {
            String query = "SELECT u FROM User AS u WHERE CAST(u.cast AS INTEGER) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void simpleCastToDoubleOnSelect() {
        try {
            String query = "SELECT CAST(u.birthYear as double) FROM User AS u WHERE extract(year from u.dateOfBirth) = 1983";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void stringLeftFunction() {
        try {
            String query = "SELECT LEFT(u.name, 3) FROM User AS u WHERE LEFT(u.lastName, 1) = 'D'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();

    }

    @Test
    void stringRightFunction() {
        try {
            String query = "SELECT RIGHT(u.name, 3) FROM User AS u WHERE right(u.lastName, 1) = 'D'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();

    }

    @Test
    void replaceStringFunction() {
        try {
            String query = "SELECT REPLACE(u.name, 'John', u.lastName) FROM User u WHERE REPLACE(u.name, 'ohn', 'ack') = 'Jack'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void replaceAsIdentifierFunction() {
        try {
            String query = "SELECT REPLACE(u.name, 'John', u.lastName) "
                    + "FROM User u WHERE REPLACE(u.name, 'ohn', 'ack') = 'Jack' AND u.replace = true";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void idFunctionSimple() {
        try {
            String query = "SELECT u FROM User AS u WHERE ID(u) = :id";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void idFunctionOnSelect() {
        try {
            String query = "SELECT ID(u) FROM User AS u WHERE u.name = :name";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void versionFunctionEquals() {
        try {
            String query = "SELECT u FROM User AS u WHERE VERSION(u) = :version";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void versionFunctionSimple() {
        try {
            String query = "SELECT u FROM User AS u WHERE VERSION(u) <> :version";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void stringConcatOperator() {
        try {
            String query = "SELECT u.firstName || ' ' || u.lastName FROM User AS u";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void stringConcatOperatorInWhere() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.firstName || u.lastName = :fullName";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void orderByNullsFirst() {
        try {
            String query = "SELECT u FROM User AS u ORDER BY u.name ASC NULLS FIRST";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void orderByNullsLast() {
        try {
            String query = "SELECT u FROM User AS u ORDER BY u.name DESC NULLS LAST, u.age ASC NULLS FIRST";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void orderByNullsWithoutAscDesc() {
        try {
            String query = "SELECT u FROM User AS u ORDER BY u.name NULLS LAST";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void orderByScalarExpression() {
        try {
            String query = "SELECT u FROM User AS u ORDER BY UPPER(u.name) ASC";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void deleteUsingIdAndVersion() {
        try {
            String query = "DELETE from Employee WHERE id(this) = :id AND version(this) = :version";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void union() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 UNION SELECT u FROM User AS u WHERE u.name = 'Admin'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void unionAll() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 UNION ALL SELECT u FROM User AS u WHERE u.name = 'Admin'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void intersect() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 INTERSECT SELECT u FROM User AS u WHERE u.active = true";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void intersectAll() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 INTERSECT ALL SELECT u FROM User AS u WHERE u.active = true";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void except() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 EXCEPT SELECT u FROM User AS u WHERE u.name = 'Admin'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void exceptAll() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 EXCEPT ALL SELECT u FROM User AS u WHERE u.name = 'Admin'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void unionWithIntersect() {
        try {
            String query = "SELECT u FROM User AS u WHERE u.age > 30 "
                + "UNION SELECT u FROM User AS u WHERE u.active = true "
                + "INTERSECT SELECT u FROM User AS u WHERE u.name = 'Admin'";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void parenthesizedUnion() {
        try {
            String query = "(SELECT u FROM User AS u WHERE u.age > 30 "
                + "UNION SELECT u FROM User AS u WHERE u.name = 'Admin') "
                + "INTERSECT SELECT u FROM User AS u WHERE u.active = true";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void optionalSelectClause() {
        try {
            String query = "FROM User u WHERE u.age > 30";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void optionalIdentificationVariable() {
        try {
            String query = "SELECT this FROM User"
                + " WHERE this.age > 30";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void optionalSelectAndIdentificationVariable() {
        try {
            String query = "FROM User WHERE this.age > 30";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void optionalSelectNoWhere() {
        try {
            String query = "FROM User";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
            return;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        fail();
    }

    @Test
    void chainedAdditiveOperatorsAreLeftAssociative() throws Exception {
        // 10 - 3 - 2 must parse as (10 - 3) - 2, i.e. the left operand of the
        // outermost subtraction is itself a subtraction.
        String query = "SELECT u FROM User AS u WHERE u.age = 10 - 3 - 2";
        JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
        JPQLNode subtract = node.findChildByID(JPQLTreeConstants.JJTSUBTRACT, true);
        assertNotNull(subtract);
        assertEquals(JPQLTreeConstants.JJTSUBTRACT, subtract.children[0].id);
        assertEquals("2", subtract.children[1].text);
    }

    @Test
    void chainedMultiplicativeOperatorsAreLeftAssociative() throws Exception {
        // 100 / 5 / 2 must parse as (100 / 5) / 2.
        String query = "SELECT u FROM User AS u WHERE u.age = 100 / 5 / 2";
        JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
        JPQLNode divide = node.findChildByID(JPQLTreeConstants.JJTDIVIDE, true);
        assertNotNull(divide);
        assertEquals(JPQLTreeConstants.JJTDIVIDE, divide.children[0].id);
        assertEquals("2", divide.children[1].text);
    }

    @Test
    void mixedAdditiveOperatorsAreLeftAssociative() throws Exception {
        // 10 - 3 + 2 must parse as (10 - 3) + 2, not as 10 - (3 + 2).
        String query = "SELECT u FROM User AS u WHERE u.age = 10 - 3 + 2";
        JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
        JPQLNode add = node.findChildByID(JPQLTreeConstants.JJTADD, true);
        assertNotNull(add);
        assertEquals(JPQLTreeConstants.JJTSUBTRACT, add.children[0].id);
        assertEquals("2", add.children[1].text);
    }

    @Test
    void mixedMultiplicativeOperatorsAreLeftAssociative() throws Exception {
        // 100 / 5 * 2 must parse as (100 / 5) * 2, not as 100 / (5 * 2).
        String query = "SELECT u FROM User AS u WHERE u.age = 100 / 5 * 2";
        JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
        JPQLNode multiply = node.findChildByID(JPQLTreeConstants.JJTMULTIPLY, true);
        assertNotNull(multiply);
        assertEquals(JPQLTreeConstants.JJTDIVIDE, multiply.children[0].id);
        assertEquals("2", multiply.children[1].text);
    }

    @Test
    void multiplicationBindsTighterThanAddition() throws Exception {
        // 1 + 2 * 3 must parse as 1 + (2 * 3).
        String query = "SELECT u FROM User AS u WHERE u.age = 1 + 2 * 3";
        JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
        JPQLNode add = node.findChildByID(JPQLTreeConstants.JJTADD, true);
        assertNotNull(add);
        assertEquals("1", add.children[0].text);
        assertEquals(JPQLTreeConstants.JJTMULTIPLY, add.children[1].id);
    }

    @Test
    void unaryMinusOnPath() throws Exception {
        JPQLNode node = (JPQLNode) new JPQL("SELECT -u.age FROM User u").parseQuery();
        JPQLNode negate = node.findChildByID(JPQLTreeConstants.JJTNEGATE, true);
        assertNotNull(negate);
        assertEquals(JPQLTreeConstants.JJTPATH, negate.children[0].id);
    }

    @Test
    void unaryPlusOnPath() throws Exception {
        JPQLNode node = (JPQLNode) new JPQL("SELECT +u.age FROM User u").parseQuery();
        assertNull(node.findChildByID(JPQLTreeConstants.JJTNEGATE, true));
        assertNotNull(node.findChildByID(JPQLTreeConstants.JJTPATH, true));
    }

    @Test
    void unaryMinusBindsTighterThanMultiplication() throws Exception {
        // -u.age * 2 must parse as (-u.age) * 2
        String query = "SELECT u FROM User u WHERE -u.age * 2 < 0";
        JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
        JPQLNode multiply = node.findChildByID(JPQLTreeConstants.JJTMULTIPLY, true);
        assertNotNull(multiply);
        assertEquals(JPQLTreeConstants.JJTNEGATE, multiply.children[0].id);
    }

    @Test
    void unaryMinusOnArithmeticPrimaries() throws Exception {
        String[] queries = {
            "SELECT -(u.age + u.height) FROM User u",
            "SELECT u FROM User u WHERE -u.age < 0",
            "SELECT u FROM User u WHERE u.age = -:age",
            "SELECT u FROM User u WHERE u.age = -?1",
            "SELECT u FROM User u ORDER BY -u.age",
            "SELECT u.name, -SUM(u.age) FROM User u GROUP BY u.name HAVING -SUM(u.age) < 0",
            "SELECT -CASE WHEN u.age > 10 THEN u.age ELSE 0 END FROM User u",
            "SELECT -ABS(u.age) FROM User u",
            "SELECT u.age - -u.height FROM User u",
            "SELECT u FROM User u WHERE u.age BETWEEN -u.height AND +u.height",
        };
        for (String query : queries) {
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node.findChildByID(JPQLTreeConstants.JJTNEGATE, true), query);
        }
    }

    @Test
    void signedNumericLiteralsStayLiterals() throws Exception {
        String[] queries = {
            "SELECT u FROM User u WHERE u.age = -1",
            "SELECT u FROM User u WHERE u.age = - 1",
            "SELECT u FROM User u WHERE u.age = +1",
            "SELECT u FROM User u WHERE u.height = -1.5",
            "SELECT -1 FROM User u",
        };
        for (String query : queries) {
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNull(node.findChildByID(JPQLTreeConstants.JJTNEGATE, true), query);
        }
    }

    @Test
    void subtractionOfSignedLiteral() throws Exception {
        // 1 - -1 is a subtraction whose right operand is the literal -1
        JPQLNode node = (JPQLNode) new JPQL("SELECT u FROM User u WHERE u.age = 1 - -1").parseQuery();
        assertNull(node.findChildByID(JPQLTreeConstants.JJTNEGATE, true));
        JPQLNode subtract = node.findChildByID(JPQLTreeConstants.JJTSUBTRACT, true);
        assertNotNull(subtract);
        assertEquals(JPQLTreeConstants.JJTINTEGERLITERAL, subtract.children[1].id);
        assertEquals(JPQLTreeConstants.JJTNEGATIVE, subtract.children[1].children[0].id);
    }

    @Test
    void subtractionWithoutWhitespace() throws Exception {
        // u.age -1 is a subtraction, not a path followed by the literal -1
        JPQLNode node = (JPQLNode) new JPQL("SELECT u FROM User u WHERE u.age -1 = 0").parseQuery();
        assertNull(node.findChildByID(JPQLTreeConstants.JJTNEGATE, true));
        JPQLNode subtract = node.findChildByID(JPQLTreeConstants.JJTSUBTRACT, true);
        assertNotNull(subtract);
        assertEquals(JPQLTreeConstants.JJTPATH, subtract.children[0].id);
        assertEquals(JPQLTreeConstants.JJTINTEGERLITERAL, subtract.children[1].id);
        assertNull(subtract.children[1].children);
    }
}
