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

import static org.junit.Assert.*;

import org.apache.openjpa.kernel.jpql.JPQLExpressionBuilder.JPQLNode;
import org.junit.Test;

public class TestJPQLParser {

    @Test
    public void testSimpleJPQLExtractFieldFromPath() {
        try {
            String query = "SELECT a FROM Usuario AS a where (extract(year from a.dateOfBirth) - 2000) < 25";
            JPQLNode node = (JPQLNode) new JPQL(query).parseQuery();
            assertNotNull(node);
        } catch (ParseException ex) {
            fail();
        }
    }
    
    @Test
    public void testSimpleJPQLExtractFieldFromDate() {
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
    public void testJPQL() {
        try {
            String query = "SELECT c FROM CompUser AS u WHERE EXTRACT (YEAR FROM {d '2006-03-21'}) > 2005";
            assertNotNull(new JPQL(query).parseQuery());
        } catch (ParseException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void testSimpleJPQLExtractPart() {
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
    public void testSimpleCastToString() {
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
    public void testSimpleCastToStringOnSelect() {
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
    public void testSimpleCastToInteger() {
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
    public void testSimpleCastToIntegerOnSelect() {
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
    public void testSimpleCastToLong() {
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
    public void testSimpleCastToLongOnSelect() {
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
    public void testSimpleCastToFloat() {
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
    public void testSimpleCastToFloatOnSelect() {
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
    public void testSimpleCastToDouble() {
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
    public void testSimpleCastToDoubleOnSelect() {
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
    public void testStringLeftFunction() {
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
    public void testStringRightFunction() {
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
    public void testReplaceStringFunction() {
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
    public void testIdFunctionSimple() {
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
    public void testIdFunctionOnSelect() {
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
    public void testVersionFunctionEquals() {
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
    public void testVersionFunctionSimple() {
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
    public void testStringConcatOperator() {
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
    public void testStringConcatOperatorInWhere() {
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
    public void testOrderByNullsFirst() {
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
    public void testOrderByNullsLast() {
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
    public void testOrderByNullsWithoutAscDesc() {
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
    public void testOrderByScalarExpression() {
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
    public void testDeleteUsingIdAndVersion() {
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
    public void testUnion() {
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
    public void testUnionAll() {
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
    public void testIntersect() {
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
    public void testIntersectAll() {
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
    public void testExcept() {
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
    public void testExceptAll() {
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
    public void testUnionWithIntersect() {
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
    public void testParenthesizedUnion() {
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
    public void testOptionalSelectClause() {
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
    public void testOptionalIdentificationVariable() {
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
    public void testOptionalSelectAndIdentificationVariable() {
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
    public void testOptionalSelectNoWhere() {
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
}
