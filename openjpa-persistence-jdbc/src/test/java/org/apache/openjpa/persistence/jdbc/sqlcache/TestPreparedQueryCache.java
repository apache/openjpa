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
package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.PreparedQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Performance oriented test to see comparative difference in response time with
 * or with SQL caching.
 * 
 * @author Pinaki Poddar
 * 
 */
public class TestPreparedQueryCache extends SingleEMFTestCase {
	public static final int SAMPLE_SIZE = 100;
	public static final long NANOS_TO_MILLS = 1000*1000;
	public static final boolean QUERY_CACHE = true;
	public static final String[] COMPANY_NAMES = { "acme.org" };
	public static final String[] DEPARTMENT_NAMES = { "Marketing", "Sales",
			"Engineering" };
	public static final String[] EMPLOYEE_NAMES = { "Tom", "Dick", "Harray" };

	public void setUp() throws Exception {
		super.setUp(Company.class, Department.class, Employee.class,
				Address.class, "openjpa.Log", "SQL=WARN");
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testPreparedQueryCacheIsActiveByDefault() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertEquals("true", conf.getPreparedQueryCache());
		assertNotNull(conf.getPreparedQueryCacheInstance());
	}
	
	public void testPreparedQueryCacheCanBeDeactivatedDynamically() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertNotNull(conf.getPreparedQueryCacheInstance());
		conf.setPreparedQueryCache("false");
		assertNull(conf.getPreparedQueryCacheInstance());
	}

	public void testSimpleQueryCache() {
		compare("select p from Company p");
	}

	public void testQueryWithLiteral() {
		compare("select p from Company p where p.name = 'PObject'");
	}

	public void testQueryWithParameter() {
		compare("select p from Company p where p.name = :param", 
				"param", "x");
	}

	public void testJoins() {
		compare("select e from Employee e " + "where e.name = :emp "
				+ "and e.department.name = :dept " +
				"and e.department.company.name = :company " +
				"and e.address.zip = :zip", 
				
				"emp", "John", 
				"dept",	"Engineering",
				"company", "acme.org",
				"zip", 12345);
	}

	/**
	 * Compare the result of execution of the same query with and without
	 * Prepared Query Cache.
	 * 
	 */
	void compare(String jpql, Object... params) {
		// run the query once for warming up 
		run(jpql, params, !QUERY_CACHE, 1);
		
		// run N times without cache
		long without = run(jpql, params, !QUERY_CACHE, SAMPLE_SIZE);
		
		// run N times with cache
		long with = run(jpql, params, QUERY_CACHE, SAMPLE_SIZE);
		
		long delta = (without == 0) ? 0 : (without - with) * 100 / without;
		
		String sql = getSQL(jpql);
		System.err.println("Execution time in millis for " + SAMPLE_SIZE
				+ " query execution with and without SQL cache:" + with + " "
				+ without + " (" + delta + "%)");
		System.err.println("JPQL: " + jpql);
		System.err.println("SQL : " + sql);
		assertFalse("change in execution time = " + delta + "%", delta < 0);
	}

	/**
	 * Create and run a query N times with the given parameters. The time for 
	 * each query execution is measured in nanosecond precision) and finally
	 * median time taken in N observation is returned in nanosecond.  
	 * 
	 * returns median time taken for single execution.
	 */
	long run(String jpql, Object[] params, boolean cache, int N) {
		OpenJPAEntityManager em = emf.createEntityManager();
		em.getConfiguration().setPreparedQueryCache("" + cache);
		
		List<Long> stats = new ArrayList<Long>();
		for (int i = 0; i < N; i++) {
			long start = System.nanoTime();
			OpenJPAQuery q = em.createQuery(jpql);
			for (int j = 0; params != null && j < params.length - 1; j += 2) {
				String key = params[j].toString();
				Object val = params[j + 1];
				if (val instanceof String)
					q.setParameter(key, val + "-" + i);
				else if (val instanceof Integer)
					q.setParameter(key, ((Integer) val).intValue() + i);
				else
					q.setParameter(key, val);
			}
			q.getResultList();
			q.closeAll();
			long end = System.nanoTime();
			stats.add(end - start);
		}
		em.close();
		Collections.sort(stats);
		return stats.get(N/2);
	}	
	
	public boolean isCached(String jpql) {
		Map cache = emf.getConfiguration().getPreparedQueryCacheInstance();
		return cache != null && cache.get(jpql) != null
				&& cache.get(jpql) != PreparedQuery.NOT_CACHABLE;
	}
	
	String getSQL(String jpql) {
		Map cache = emf.getConfiguration().getPreparedQueryCacheInstance();
		return cache == null ? null : ((PreparedQuery)cache.get(jpql)).getSQL();
	}



	public static void main(String[] args) throws Exception {
		TestPreparedQueryCache _this = new TestPreparedQueryCache();
		_this.setUp();
		String jpql = "select e from Employee e where e.name = :emp and "
				+ "e.department.name = :dept and "
				+ "e.department.company.name = :company and e.address.zip = :zip";
		Object[] params = { "emp", "John", "dept", "Engineering", "company",
				"acme.org", "zip", 12345 };
		_this.run(jpql, params, true, 100);
	}

}
