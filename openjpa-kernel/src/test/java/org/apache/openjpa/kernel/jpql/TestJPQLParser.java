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

}
