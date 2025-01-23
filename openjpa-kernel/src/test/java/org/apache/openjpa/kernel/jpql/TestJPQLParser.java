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

}
