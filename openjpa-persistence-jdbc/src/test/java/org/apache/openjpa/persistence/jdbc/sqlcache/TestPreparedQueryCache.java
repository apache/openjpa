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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

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
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Performance oriented test to see comparative difference in response time with
 * or with SQL caching.
 * 
 * @author Pinaki Poddar
 * 
 */
public class TestPreparedQueryCache extends SQLListenerTestCase {
	// Fail if performance degrades with cache compared to without cache
    public static final boolean FAIL_ON_PERF_DEGRADE = 
        Boolean.getBoolean("FailOnPerformanceRegression");
	
	// # observations to compute timing statistics
	public static final int SAMPLE_SIZE = 100;
	
	public static final boolean USE_CACHE                  = true;
	public static final boolean IS_NAMED_QUERY             = true;
	
    public static final String[] COMPANY_NAMES = {"IBM", "BEA", "acme.org" };
    public static final int[] START_YEARS = {1900, 2000, 2010 };
	public static final String[] DEPARTMENT_NAMES = { "Marketing", "Sales",
			"Engineering" };
	public static final String[] EMPLOYEE_NAMES = { "Tom", "Dick", "Harray" };
	public static final String[] CITY_NAMES = {"Tulsa", "Durban", "Harlem"};
	
	public static final String EXCLUDED_QUERY_1 = "select count(p) from Company p";
	public static final String EXCLUDED_QUERY_2 = "select count(p) from Department p";
	public static final String INCLUDED_QUERY   = "select p from Address p";
	private Company IBM;
	
	public void setUp() throws Exception {
		super.setUp(CLEAR_TABLES, Company.class, Department.class, 
		    Employee.class, Address.class, 
				"openjpa.jdbc.QuerySQLCache", 
				"true(excludes='select count(p) from Company p;select count(p) from Department p')");
		createTestData();
	}
	
	void createTestData() {
	    EntityManager em = emf.createEntityManager();
	    em.getTransaction().begin();
	    for (int i = 0; i < COMPANY_NAMES.length; i++) {
	        Company company = new Company();
	        if (i == 0) 
	            IBM = company;
	        company.setName(COMPANY_NAMES[i]);
	        company.setStartYear(START_YEARS[i]);
	        em.persist(company);
	        for (int j = 0; j < DEPARTMENT_NAMES.length; j++) {
	            Department dept = new Department();
	            dept.setName(DEPARTMENT_NAMES[j]);
	            company.addDepartment(dept);
	            em.persist(dept);
	            for (int k = 0; k < EMPLOYEE_NAMES.length; k++) {
	                Employee emp = new Employee();
	                emp.setName(EMPLOYEE_NAMES[k]);
	                Address addr = new Address();
	                addr.setCity(CITY_NAMES[k]);
                    em.persist(emp);
	                em.persist(addr);
	                emp.setAddress(addr);
                    dept.addEmployees(emp);
	            }
	        }
	    }
	    em.getTransaction().commit();
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
		compare(!IS_NAMED_QUERY, jpql, COMPANY_NAMES.length, params);
	}

	public void testQueryWithLiteral() {
		String jpql = "select p from Company p where p.name = " + literal(COMPANY_NAMES[0]);
		Object[] params = null;
		compare(!IS_NAMED_QUERY, jpql, 1, params);
	}

	public void testQueryWithParameter() {
		String jpql = "select p from Company p where p.name = :param";
		Object[] params = {"param", COMPANY_NAMES[0]};
		compare(!IS_NAMED_QUERY, jpql, 1, params);
	}

	public void testQueryWithJoinsAndParameters() {
		String jpql = "select e from Employee e " + "where e.name = :emp"
					+ " and e.department.name = :dept"
					+ " and e.department.company.name LIKE  " + literal(COMPANY_NAMES[0]) 
					+ " and e.address.city = :city";
		Object[] params = { "emp", EMPLOYEE_NAMES[0], 
							"dept",	DEPARTMENT_NAMES[0],
							"city", CITY_NAMES[0]};
		compare(!IS_NAMED_QUERY, jpql, 1, params);
	}

	public void testNamedQueryWithNoParameter() {
		String namedQuery = "Company.PreparedQueryWithNoParameter";
		Object[] params = null;
		compare(IS_NAMED_QUERY, namedQuery, COMPANY_NAMES.length, params);
	}

	public void testNamedQueryWithLiteral() {
		String namedQuery = "Company.PreparedQueryWithLiteral";
		Object[] params = null;
		compare(IS_NAMED_QUERY, namedQuery, 1, params);
	}

	public void testNamedQueryWithPositionalParameter() {
		String namedQuery = "Company.PreparedQueryWithPositionalParameter";
		Object[] params = {1, COMPANY_NAMES[0], 2, START_YEARS[0]};
		compare(IS_NAMED_QUERY, namedQuery, 1, params);
	}
	
	public void testNamedQueryWithNamedParameter() {
		String namedQuery = "Company.PreparedQueryWithNamedParameter";
		Object[] params = {"name", COMPANY_NAMES[0], "startYear", START_YEARS[0]};
		compare(IS_NAMED_QUERY, namedQuery, 1, params);
	}
	
	public void testPersistenceCapableParameter() {
	    String jpql = "select e from Employee e where e.department.company=:company";
	    Object[] params = {"company", IBM};
	    compare(!IS_NAMED_QUERY, jpql, EMPLOYEE_NAMES.length*DEPARTMENT_NAMES.length, params);
	}
	
	/**
	 * Project results are returned with different types of ROP.
	 */
	public void testProjectionResult() {
	    String jpql = "select e.name from Employee e where e.address.city=:city";
        Object[] params = {"city", CITY_NAMES[0]};
        compare(!IS_NAMED_QUERY, jpql, COMPANY_NAMES.length*DEPARTMENT_NAMES.length, params);
	}
	
	public void testCollectionValuedParameters() {
	    String jpql = "select e from Employee e where e.name in :names";
	    Object[] params1 = {"names", Arrays.asList(new String[]{EMPLOYEE_NAMES[0], EMPLOYEE_NAMES[1]})};
        Object[] params2 = {"names", Arrays.asList(new String[]{EMPLOYEE_NAMES[2]})};
        Object[] params3 = {"names", Arrays.asList(EMPLOYEE_NAMES)};
        
        boolean checkHits = false;
        
        int expectedCount = 2*COMPANY_NAMES.length*DEPARTMENT_NAMES.length;
        run(jpql, params1, USE_CACHE, 2, !IS_NAMED_QUERY, expectedCount, checkHits);
        assertCached(jpql);
        
        expectedCount = 1*COMPANY_NAMES.length*DEPARTMENT_NAMES.length;
        run(jpql, params2, USE_CACHE, 2, !IS_NAMED_QUERY, expectedCount, checkHits);
        
        expectedCount = EMPLOYEE_NAMES.length*COMPANY_NAMES.length*DEPARTMENT_NAMES.length;
        run(jpql, params3, USE_CACHE, 2, !IS_NAMED_QUERY, expectedCount, checkHits);
	}
	
	/**
	 * Compare the result of execution of the same query with and without
	 * Prepared Query Cache.
	 * 
	 */
	void compare(boolean isNamed, String jpql, int expectedCount, Object... params) {
		String realJPQL = isNamed ? getJPQL(jpql) : jpql;
		// run the query once for warming up 
		run(jpql, params, !USE_CACHE, 1, isNamed, expectedCount);
		
		// run N times without cache
		long without = run(jpql, params, !USE_CACHE, SAMPLE_SIZE, isNamed, expectedCount);
		assertNotCached(realJPQL);
		
		// run N times with cache
		long with = run(jpql, params, USE_CACHE, SAMPLE_SIZE, isNamed, expectedCount);
		assertCached(realJPQL);
		
		long delta = (without == 0) ? 0 : (without - with) * 100 / without;
		
		if (delta < 0) {
			if (FAIL_ON_PERF_DEGRADE)
				assertFalse("change in execution time = " + delta + "%", 
						delta < 0);
		}
	}

    long run(String jpql, Object[] params, boolean useCache, int N, 
        boolean isNamedQuery, int expectedCount) {
        return run(jpql, params, useCache, N, isNamedQuery, expectedCount, true);
    }
	/**
	 * Create and run a query N times with the given parameters. The time for 
	 * each query execution is measured in nanosecond precision and 
	 * median time taken in N observation is returned.  
	 * 
	 * returns median time taken for single execution.
	 */
	long run(String jpql, Object[] params, boolean useCache, int N, 
			boolean isNamedQuery, int expectedCount, boolean checkHits) {
		List<Long> stats = new ArrayList<Long>();
		sql.clear();
		for (int i = 0; i < N; i++) {
	        OpenJPAEntityManager em = emf.createEntityManager();
	        ((OpenJPAEntityManagerSPI)em).setQuerySQLCache(useCache);
	        assertEquals(useCache, ((OpenJPAEntityManagerSPI)em).getQuerySQLCache());
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
					fail("key " + key + " is neither Number nor String");
			}
			List list = q.getResultList();
			walk(list);
			int actual = list.size();
			q.closeAll();
			assertEquals(expectedCount, actual);
			long end = System.nanoTime();
			stats.add(end - start);
	        em.close();
		}
        if (useCache && checkHits) {
            String cacheKey = isNamedQuery ? getJPQL(jpql) : jpql;
            long total = getCache().getStatistics().getExecutionCount(cacheKey);
            long hits = getCache().getStatistics().getHitCount(cacheKey);
            assertEquals(N, total);
            assertEquals(N-1, hits);
        }
        assertEquals(N, sql.size());
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
	
	String literal(String s) {
	    return "'"+s+"'";
	}
	
	void walk(List list) {
	    Iterator i = list.iterator();
	    while (i.hasNext())
	        i.next();
	}
}
