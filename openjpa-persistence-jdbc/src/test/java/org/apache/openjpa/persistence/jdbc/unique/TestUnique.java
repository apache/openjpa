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
package org.apache.openjpa.persistence.jdbc.unique;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestUnique extends SQLListenerTestCase {
    @Override
    public void setUp(Object... props) {
    	super.setUp(UniqueA.class, UniqueB.class);    			    
    }
    
	public void testMapping() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.getTransaction().commit();
		em.close();
		// The above should trigger schema definition
		
		List<String> sqls = super.sql;
		
		assertSQLFragnment(sqls, "CREATE TABLE UNIQUE_A", 
				"UNIQUE (a1, a2)", 
				"UNIQUE (a3, a4)");
		assertSQLFragnment(sqls, "CREATE TABLE UNIQUE_B", 
				"UNIQUE (b1, b2)");
		assertSQLFragnment(sqls, "CREATE TABLE UNIQUE_SECONDARY", 
				"UNIQUE (sa1)");
		assertSQLFragnment(sqls, "CREATE TABLE UNIQUE_GENERATOR", 
				"UNIQUE (GEN1, GEN2)");
		assertSQLFragnment(sqls, "CREATE TABLE UNIQUE_JOINTABLE", 
				"UNIQUE (UNIQUEA_AID, BS_BID)");
		
	}
	
	void assertSQLFragnment(List<String> list, String...keys) {
		for (String sql : list) {
			String SQL = sql.toUpperCase();
			boolean matched = true;
			for (String key : keys) {
				String KEY = key.toUpperCase();
				if (SQL.indexOf(KEY) == -1) {
					matched = false;
					break;
				}
			}
			if (matched)
				return;
		}
		int i = 0;
		for (String sql : list) {
			i++;
			System.out.println(""+i+":"+sql);
		}
		fail("None of the above SQL contains all keys " + Arrays.toString(keys));
	}
}
