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

import javax.persistence.EntityManager;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.PreparedQuery;
import org.apache.openjpa.persistence.PreparedQueryCache;
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
	public static final String EXCLUDED_QUERY_1 = "select count(p) from Company p";
	public static final String EXCLUDED_QUERY_2 = "select count(p) from Department p";
	public static final String INCLUDED_QUERY   = "select count(p) from Address p";
	
	public void setUp() throws Exception {
		super.setUp(Company.class, Department.class, Employee.class,
				Address.class, 
				"openjpa.Log", "SQL=WARN",
				"openjpa.PreparedQueryCache", 
				"true(excludes='select count(p) from Company p;select count(p) from Department p')");
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	PreparedQueryCache getCache() {
		return emf.getConfiguration().getPreparedQueryCacheInstance();
	}

	public void testPreparedQueryCacheIsActiveByDefault() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertEquals(true, conf.getPreparedQueryCache());
		assertNotNull(getCache());
	}
	
	public void testPreparedQueryCacheCanBeDeactivatedDynamically() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertNotNull(getCache());
		conf.setPreparedQueryCache(false);
		assertNull(getCache());
	}
	
	public void testPreparedQueryCacheIsPerUnitSingleton() {
		PreparedQueryCache c1 = getCache();
		PreparedQueryCache c2 = getCache();
		assertSame(c1, c2);
	}
	
	public void testExclusionPattern() {
		List<String> patterns = getCache().getExcludes(); 
		OpenJPAEntityManager em = emf.createEntityManager();
		OpenJPAQuery q1 = em.createQuery(EXCLUDED_QUERY_1);
		q1.getResultList();
		assertNotCached(EXCLUDED_QUERY_1);
		
		OpenJPAQuery q2 = em.createQuery(EXCLUDED_QUERY_2);
		q2.getResultList();
		assertNotCached(EXCLUDED_QUERY_2);
		
		OpenJPAQuery q3 = em.createQuery(INCLUDED_QUERY);
		q3.getResultList();
		assertCached(INCLUDED_QUERY);
	}
	
	void assertLanguage(OpenJPAQuery q, String lang) {
		assertEquals(lang, q.getLanguage());
	}
	
	void assertCached(String id) {
		PreparedQuery cached = getCache().get(id);
		assertNotNull(getCache() + ": " + getCache().getMapView() + 
				" does not contain " + id, cached);
	}
	
	void assertNotCached(String id) {
		PreparedQueryCache cache = getCache();
		if (cache != null) {
			assertNull(cache.get(id));
		}
	}
	
	public void testPreparedQueryIsCachedOnExecution() {
		String jpql = "select p from Company p";
		OpenJPAEntityManager em = emf.createEntityManager();
		OpenJPAQuery q1 = em.createQuery(jpql);
		assertNotCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		List result = q1.getResultList();
		assertCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		PreparedQuery cached = getCache().get(jpql);
		assertEquals(jpql, cached.getIdentifier());
		assertNotEquals(jpql, cached.getSQL());
	}

	public void testPreparedQueryIsCachedAcrossExecution() {
		String jpql = "select p from Company p";
		OpenJPAEntityManager em = emf.createEntityManager();
		OpenJPAQuery q1 = em.createQuery(jpql);
		assertNotCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		
		List result = q1.getResultList();
		assertCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		// Create a new query with the same JPQL
		// This is not only cached, its language is different too
		OpenJPAQuery q2 = em.createQuery(jpql);
		assertCached(jpql);
		assertLanguage(q2, QueryLanguages.LANG_PREPARED_SQL);
	}
	
	public void testInvalidatePreparedQueryWithHint() {
		String jpql = "select p from Company p";
		OpenJPAEntityManager em = emf.createEntityManager();
		OpenJPAQuery q1 = em.createQuery(jpql);
		assertNotCached(jpql);
		List result = q1.getResultList();
		assertCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		// Create a new query with the same JPQL
		// This is cached on creation, its language is Prepared SQL
		OpenJPAQuery q2 = em.createQuery(jpql);
		assertCached(jpql);
		assertLanguage(q2, QueryLanguages.LANG_PREPARED_SQL);
		q2.getResultList();
		
		// Now execute with hints to invalidate. 
		q2.setHint(QueryHints.HINT_INVALIDATE_PREPARED_QUERY, true);
		// Immediately it should be removed from the cache
		assertNotCached(jpql);
		assertEquals(JPQLParser.LANG_JPQL, q2.getLanguage());
		q2.getResultList();
		
		// Create a new query with the same JPQL
		// This is not cached on creation, its language is JPQL
		OpenJPAQuery q3 = em.createQuery(jpql);
		assertNotCached(jpql);
		assertLanguage(q3, JPQLParser.LANG_JPQL);
	}
	
	public void testIgnorePreparedQueryWithHint() {
		String jpql = "select p from Company p";
		OpenJPAEntityManager em = emf.createEntityManager();
		OpenJPAQuery q1 = em.createQuery(jpql);
		assertNotCached(jpql);
		List result = q1.getResultList();
		assertCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		// Create a new query with the same JPQL
		// This is cached on creation, its language is PREPARED SQL
		OpenJPAQuery q2 = em.createQuery(jpql);
		assertCached(jpql);
		assertLanguage(q2, QueryLanguages.LANG_PREPARED_SQL);
		q2.getResultList();
		
		// Now execute with hints to ignore. 
		q2.setHint(QueryHints.HINT_IGNORE_PREPARED_QUERY, true);
		// It should remain in the cache
		assertCached(jpql);
		// But its language should be JPQL and not PREPARED SQL
		assertEquals(JPQLParser.LANG_JPQL, q2.getLanguage());
		q2.getResultList();
		
		// Create a new query with the same JPQL
		// This is cached on creation, its language is PREPARED SQL
		OpenJPAQuery q3 = em.createQuery(jpql);
		assertCached(jpql);
		assertLanguage(q3, QueryLanguages.LANG_PREPARED_SQL);
	}


	public void testQueryWithNoParameter() {
		compare("select p from Company p");
	}

	public void testQueryWithLiteral() {
		compare("select p from Company p where p.name = 'PObject'");
	}

	public void testQueryWithParameter() {
		compare("select p from Company p where p.name = :param", 
				"param", "x");
	}

	public void testQueryWithJoinsAndParameters() {
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
//		if (true) return;
		// run the query once for warming up 
		run(jpql, params, !QUERY_CACHE, 1);
		
		// run N times without cache
		long without = run(jpql, params, !QUERY_CACHE, SAMPLE_SIZE);
		assertNotCached(jpql);
		
		// run N times with cache
		long with = run(jpql, params, QUERY_CACHE, SAMPLE_SIZE);
		assertCached(jpql);
		
		long delta = (without == 0) ? 0 : (without - with) * 100 / without;
		
		String sql = getSQL(jpql);
		System.err.println("Execution time in nanos for " + SAMPLE_SIZE
				+ " query execution with and without SQL cache:" + with + " "
				+ without + " (" + delta + "%)");
		System.err.println("JPQL: " + jpql);
		System.err.println("SQL : " + sql);
		assertFalse("change in execution time = " + delta + "%", delta < 0);
	}

	/**
	 * Create and run a query N times with the given parameters. The time for 
	 * each query execution is measured in nanosecond precision and 
	 * median time taken in N observation is returned.  
	 * 
	 * returns median time taken for single execution.
	 */
	long run(String jpql, Object[] params, boolean cache, int N) {
		OpenJPAEntityManager em = emf.createEntityManager();
		em.getConfiguration().setPreparedQueryCache(cache);
		
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
	
	String getSQL(String jpql) {
		PreparedQueryCache cache = emf.getConfiguration().getPreparedQueryCacheInstance();
		if (cache == null)
			return "null";
		if (cache.get(jpql) != null)
			return cache.get(jpql).getSQL();
		return "null";
	}



	public static void main(String[] args) throws Exception {
		TestPreparedQueryCache _this = new TestPreparedQueryCache();
		_this.setUp();
		String jpql = "select e from Employee e where e.name = :emp and "
				+ "e.department.name = :dept and "
				+ "e.department.company.name = :company and e.address.zip = :zip";
		Object[] params = { "emp", "John", "dept", "Engineering", "company",
				"acme.org", "zip", 12345 };
		System.err.println("Executing  100 times [" + jpql + "]");
		System.err.println("Press return to continue...");
//		System.in.read();
		long start = System.nanoTime();
		_this.run(jpql, params, true, 100);
		long end = System.nanoTime();
		System.err.println("Time taken " + (end-start) + "ns");
	}

}
