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

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.PreparedQueryCache;
import org.apache.openjpa.kernel.QueryStatistics;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Performance oriented test to see comparative difference in response time with
 * or with SQL caching.
 * 
 * @author Pinaki Poddar
 * 
 */
public class TestPreparedQueryCache extends SingleEMFTestCase {
	// Fail if performance degrades with cache compared to without cache
	public static final boolean FAIL_ON_PERF_DEGRADE = 
		Boolean.getBoolean("FailOnPerformanceRegression");
	
	// # observations to compute timing statistics
	public static final int SAMPLE_SIZE = 100;
	
	public static final boolean USE_CACHE                  = true;
	public static final boolean BIND_DIFFERENT_PARM_VALUES = true;
	public static final boolean IS_NAMED_QUERY             = true;
	
	public static final String[] COMPANY_NAMES = { "acme.org" };
	public static final String[] DEPARTMENT_NAMES = { "Marketing", "Sales",
			"Engineering" };
	public static final String[] EMPLOYEE_NAMES = { "Tom", "Dick", "Harray" };
	
	public static final String EXCLUDED_QUERY_1 = "select count(p) from Company p";
	public static final String EXCLUDED_QUERY_2 = "select count(p) from Department p";
	public static final String INCLUDED_QUERY   = "select p from Address p";
	
	public void setUp() throws Exception {
		super.setUp(Company.class, Department.class, Employee.class,
				Address.class, 
				"openjpa.Log", "SQL=WARN",
				"openjpa.QuerySQLCache", 
				"true(excludes='select count(p) from Company p;select count(p) from Department p')");
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	PreparedQueryCache getCache() {
		return emf.getConfiguration().getQuerySQLCacheInstance();
	}

	public void testPreparedQueryCacheIsActiveByDefault() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertTrue(conf.getQuerySQLCache().startsWith("true"));
		assertNotNull(getCache());
	}
	
	public void testPreparedQueryCacheCanBeDeactivatedDynamically() {
		OpenJPAConfiguration conf = emf.getConfiguration();
		assertNotNull(getCache());
		conf.setQuerySQLCache("false");
		assertNull(getCache());
	}
	
	public void testPreparedQueryCacheIsPerUnitSingleton() {
		PreparedQueryCache c1 = getCache();
		PreparedQueryCache c2 = getCache();
		assertSame(c1, c2);
	}
	
	public void testPreparedQueryIdentifierIsOriginalJPQLQuery() {
        String jpql = "select p from Company p";
        OpenJPAEntityManager em = emf.createEntityManager();
        OpenJPAQuery q1 = em.createQuery(jpql);
        q1.getResultList();
        PreparedQuery pq = getCache().get(jpql);
        assertNotNull(pq);
        assertEquals(jpql, pq.getIdentifier());
        assertEquals(jpql, pq.getOriginalQuery());
	}
	
	public void testExclusionPattern() {
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
		
		q1.getResultList();
		assertCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		PreparedQuery cached = getCache().get(jpql);
		assertEquals(jpql, cached.getIdentifier());
		assertNotEquals(jpql, cached.getTargetQuery());
	}

	public void testPreparedQueryIsCachedAcrossExecution() {
		String jpql = "select p from Company p";
		OpenJPAEntityManager em = emf.createEntityManager();
		OpenJPAQuery q1 = em.createQuery(jpql);
		assertNotCached(jpql);
		assertLanguage(q1, JPQLParser.LANG_JPQL);
		
		
		q1.getResultList();
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
		
		q1.getResultList();
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
		
		q1.getResultList();
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

	public void testQueryStatistics() {
		String jpql1 = "select c from Company c";
		String jpql2 = "select c from Company c where c.name = 'PObject'";
		OpenJPAEntityManager em = emf.createEntityManager();
		int N1 = 5;
		int N2 = 8;
		for (int i = 0; i < N1; i++) {
	        OpenJPAQuery q1 = em.createQuery(jpql1);
			q1.getResultList();
		}
		for (int i = 0; i < N2; i++) {
	        OpenJPAQuery q2 = em.createQuery(jpql2);
			q2.getResultList();
		}
		
		QueryStatistics stats = getCache().getStatistics();
		stats.dump(System.out);

		assertEquals(N1,      stats.getExecutionCount(jpql1));
		assertEquals(N2,      stats.getExecutionCount(jpql2));
		assertEquals(N1+N2,   stats.getExecutionCount());
		assertEquals(N1-1,    stats.getHitCount(jpql1));
		assertEquals(N2-1,    stats.getHitCount(jpql2));
		assertEquals(N1+N2-2, stats.getHitCount());
		
	}

	public void testResetQueryStatistics() {
		String jpql1 = "select c from Company c";
		String jpql2 = "select c from Company c where c.name = 'PObject'";
		OpenJPAEntityManager em = emf.createEntityManager();
		int N10 = 4;
		int N20 = 7;
		for (int i = 0; i < N10; i++) {
	        OpenJPAQuery q1 = em.createQuery(jpql1);
			q1.getResultList();
		}
		for (int i = 0; i < N20; i++) {
	        OpenJPAQuery q2 = em.createQuery(jpql2);
			q2.getResultList();
		}
		
		QueryStatistics stats = getCache().getStatistics();
		assertEquals(N10,       stats.getExecutionCount(jpql1));
		assertEquals(N20,       stats.getExecutionCount(jpql2));
		assertEquals(N10+N20,   stats.getExecutionCount());
		assertEquals(N10-1,     stats.getHitCount(jpql1));
		assertEquals(N20-1,     stats.getHitCount(jpql2));
		assertEquals(N10+N20-2, stats.getHitCount());
		
		stats.reset();
		
		int N11 = 7;
		int N21 = 4;
		for (int i = 0; i < N11; i++) {
            OpenJPAQuery q1 = em.createQuery(jpql1);
			q1.getResultList();
		}
		for (int i = 0; i < N21; i++) {
            OpenJPAQuery q2 = em.createQuery(jpql2);
			q2.getResultList();
		}

		stats.dump(System.out);
		
		assertEquals(N11,     stats.getExecutionCount(jpql1));
		assertEquals(N21,     stats.getExecutionCount(jpql2));
		assertEquals(N11+N21, stats.getExecutionCount());
		assertEquals(N11,     stats.getHitCount(jpql1));
		assertEquals(N21,     stats.getHitCount(jpql2));
		assertEquals(N11+N21, stats.getHitCount());
		
		assertEquals(N10+N11,     stats.getTotalExecutionCount(jpql1));
		assertEquals(N20+N21,     stats.getTotalExecutionCount(jpql2));
		assertEquals(N10+N11+N20+N21, stats.getTotalExecutionCount());
		assertEquals(N10+N11-1,     stats.getTotalHitCount(jpql1));
		assertEquals(N20+N21-1,     stats.getTotalHitCount(jpql2));
		assertEquals(N10+N11+N20+N21-2, stats.getTotalHitCount());
		
	}

	public void testQueryWithNoParameter() {
		String jpql = "select p from Company p";
		Object[] params = null;
		compare(!IS_NAMED_QUERY, jpql, BIND_DIFFERENT_PARM_VALUES, params);
	}

	public void testQueryWithLiteral() {
		String jpql = "select p from Company p where p.name = 'PObject'";
		Object[] params = null;
		compare(!IS_NAMED_QUERY, jpql, BIND_DIFFERENT_PARM_VALUES, params);
	}

	public void testQueryWithParameter() {
		String jpql = "select p from Company p where p.name = :param";
		Object[] params = {"param", "x"};
		compare(!IS_NAMED_QUERY, jpql, BIND_DIFFERENT_PARM_VALUES, params);
	}

	public void testQueryWithJoinsAndParameters() {
		String jpql = "select e from Employee e " + "where e.name = :emp "
					+ "and e.department.name = :dept "
					+ "and e.department.company.name LIKE 'IBM' " 
					+ "and e.department.company.name = :company " 
					+ "and e.address.zip = :zip";
		Object[] params = { "emp", "John", 
							"dept",	"Engineering",
							"company", "acme.org",
							"zip", 12345};
		compare(!IS_NAMED_QUERY, jpql, BIND_DIFFERENT_PARM_VALUES, params);
	}

	public void testNamedQueryWithNoParameter() {
		String namedQuery = "Company.PreparedQueryWithNoParameter";
		Object[] params = null;
		compare(IS_NAMED_QUERY, namedQuery, BIND_DIFFERENT_PARM_VALUES, params);
	}

	public void testNamedQueryWithLiteral() {
		String namedQuery = "Company.PreparedQueryWithLiteral";
		Object[] params = null;
		compare(IS_NAMED_QUERY, namedQuery, BIND_DIFFERENT_PARM_VALUES, params);
	}

	public void testNamedQueryWithPositionalParameter() {
		String namedQuery = "Company.PreparedQueryWithPositionalParameter";
		Object[] params = {1, "x", 2, 1960};
		compare(IS_NAMED_QUERY, namedQuery, BIND_DIFFERENT_PARM_VALUES, params);
	}
	
	public void testNamedQueryWithNamedParameter() {
		String namedQuery = "Company.PreparedQueryWithNamedParameter";
		Object[] params = {"name", "x", "startYear", 1960};
		compare(IS_NAMED_QUERY, namedQuery, BIND_DIFFERENT_PARM_VALUES, params);
	}
	
	/**
	 * Compare the result of execution of the same query with and without
	 * Prepared Query Cache.
	 * 
	 */
	void compare(boolean isNamed, String jpql, boolean append, Object... params) {
		String realJPQL = isNamed ? getJPQL(jpql) : jpql;
		// run the query once for warming up 
		run(jpql, params, !USE_CACHE, 1, isNamed, append);
		
		// run N times without cache
		long without = run(jpql, params, !USE_CACHE, SAMPLE_SIZE, isNamed, append);
		assertNotCached(realJPQL);
		
		// run N times with cache
		long with = run(jpql, params, USE_CACHE, SAMPLE_SIZE, isNamed, append);
		assertCached(realJPQL);
		
		long delta = (without == 0) ? 0 : (without - with) * 100 / without;
		
		String sql = getSQL(realJPQL);
		System.err.println("Execution time in nanos for " + SAMPLE_SIZE
				+ " query execution with and without SQL cache:" + with + " "
				+ without + " (" + delta + "%)");
		System.err.println("JPQL: " + realJPQL);
		System.err.println("SQL : " + sql);
		if (delta < 0) {
			if (FAIL_ON_PERF_DEGRADE)
				assertFalse("change in execution time = " + delta + "%", 
						delta < 0);
			else 
				System.err.println("*** WARN: Perforamce regression with cache." + 
					" Execution time degrades by " + delta + "%");
		} else {
		    System.err.println("change in execution time = +" + delta + "%");
		}
	}

	/**
	 * Create and run a query N times with the given parameters. The time for 
	 * each query execution is measured in nanosecond precision and 
	 * median time taken in N observation is returned.  
	 * 
	 * returns median time taken for single execution.
	 */
	long run(String jpql, Object[] params, boolean useCache, int N, 
			boolean isNamedQuery, boolean appendIndexValuetoParameters) {
		OpenJPAEntityManager em = emf.createEntityManager();
		((OpenJPAEntityManagerSPI)em).setQuerySQLCache(useCache);
		assertEquals(useCache, ((OpenJPAEntityManagerSPI)em).getQuerySQLCache());
		List<Long> stats = new ArrayList<Long>();
		for (int i = 0; i < N; i++) {
			long start = System.nanoTime();
			OpenJPAQuery q = isNamedQuery 
				? em.createNamedQuery(jpql) : em.createQuery(jpql);
			for (int j = 0; params != null && j < params.length - 1; j += 2) {
				Object key = params[j];
				Object val = params[j + 1];
				if (key instanceof Integer)
					q.setParameter(((Number)key).intValue(), val); 
				else if (key instanceof String)
					q.setParameter(key.toString(), val); 
				else
					throw new RuntimeException("key " + key + " is neither Number nor String");
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
	
	/**
	 * Get the SQL corresponding to the given query key.
	 * @param jpql
	 * @return
	 */
	String getSQL(String queryKey) {
		PreparedQueryCache cache = emf.getConfiguration().getQuerySQLCacheInstance();
		if (cache == null)
			return "null";
		PreparedQuery query = cache.get(queryKey);
		return (query != null) ? query.getTargetQuery() : "null";
	}
	
	String getJPQL(String namedQuery) {
		return emf.getConfiguration().getMetaDataRepositoryInstance()
				  .getQueryMetaData(null, namedQuery, null, true)
				  .getQueryString();
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
		_this.run(jpql, params, 
				USE_CACHE, 
				SAMPLE_SIZE, 
				!IS_NAMED_QUERY, 
				BIND_DIFFERENT_PARM_VALUES);
		long end = System.nanoTime();
		System.err.println("Time taken " + (end-start) + "ns");
	}

}
