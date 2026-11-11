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
 package org.apache.openjpa.persistence.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaQuery;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.jdbc.sql.HSQLDictionary;
import org.apache.openjpa.jdbc.sql.MariaDBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.lib.jdbc.ReportingSQLException;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.FilteringJDBCListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

class TestWrapperExtension implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable
    {
        Throwable exception = null;
        try {
            invocation.proceed();
        } catch (Throwable running) {
            exception = running;
        } finally {
            try {
                ((AbstractCriteriaTestCase)extensionContext.getRequiredTestInstance()).releaseResources();
            } catch (Throwable tearingDown) {
                if (exception == null) {
                    exception = tearingDown;
                } else {
                    exception.addSuppressed(tearingDown);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }
}

public abstract class AbstractCriteriaTestCase {
    private static final Logger logger = Logger.getLogger(AbstractCriteriaTestCase.class.getCanonicalName());

    protected abstract SQLAuditor getAuditor();

    protected abstract OpenJPAEntityManagerFactorySPI getEntityManagerFactory();

    protected abstract EntityManager getEntityManager();
    private DBDictionary dict = null;

    public DBDictionary getDictionary() {
        return dict;
    }

    @AfterEach
    public void tearDown() throws Exception {
        releaseResources();
    }

    /**
     * Releases everything {@code setUp()} opened: the EntityManager, the audited SQL and the
     * shared EntityManagerFactory, in that order. Every step runs even when an earlier one
     * failed, so that a failure releasing one resource cannot leave the next one open - an
     * exception out of {@code em.close()} must not skip {@code emf.close()} and leak the whole
     * connection pool. The first failure is rethrown, with any later ones added to it as
     * suppressed. Every step is null-safe, because {@link #runBare()} calls {@code tearDown()}
     * even when {@code setUp()} failed part way through, and the method is idempotent
     * (OPENJPA-3016).
     * <p>
     * All three criteria base classes delegate here, so the guarantee {@link #runBare()}
     * relies on holds for each of them.
     */
    protected final void releaseResources() throws Exception {
        ReleaseStep[] steps = {
            this::closeEntityManager,
            this::clearAuditor,
            this::closeEntityManagerFactory,
            this::clearReferences,
            this::clearDictionary
        };
        Exception failure = null;
        for (ReleaseStep step : steps) {
            try {
                step.release();
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * One step of {@link #releaseResources()}.
     */
    @FunctionalInterface
    private interface ReleaseStep {
        void release() throws Exception;
    }

    private void clearAuditor() {
        SQLAuditor auditor = getAuditor();
        if (auditor != null) {
            auditor.clear();
        }
    }

    private void closeEntityManagerFactory() {
        OpenJPAEntityManagerFactorySPI emf = getEntityManagerFactory();
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    private void clearDictionary() {
        dict = null;
    }

    /**
     * Rolls back any transaction left active and closes the EntityManager {@code setUp()}
     * opened, so the JDBC connection it retained goes back to the pool. Null-safe and
     * idempotent; the caller is responsible for clearing the field afterwards.
     */
    protected final void closeEntityManager() {
        EntityManager em = getEntityManager();
        if (em == null || !em.isOpen()) {
            return;
        }
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    /**
     * Nulls the fields {@code setUp()} assigned, so that the next {@code setUp()} rebuilds
     * them. Called from {@link #releaseResources()} once the resources themselves are closed.
     */
    protected abstract void clearReferences();

    /**
     * Create an entity manager factory for persistence unit <code>pu</code>. Put {@link #CLEAR_TABLES} in this list to
     * tell the test framework to delete all table contents before running the tests.
     * NOTE: Caller must close the returned EMF.
     *
     * @param props
     *            list of persistent types used in testing and/or configuration values in the form
     *            key,value,key,value...
     */
    protected OpenJPAEntityManagerFactorySPI createNamedEMF(Class<?>... types) {
        Map<Object, Object> map = new HashMap<>();
        map.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true," + "SchemaAction='drop,add')");
        map.put("openjpa.jdbc.QuerySQLCache", "false");
        map.put("openjpa.DynamicEnhancementAgent", "false");
        map.put("openjpa.RuntimeUnenhancedClasses", "unsupported");
        map.put("openjpa.Compatibility", "QuotedNumbersInQueries=true");
        map.put("openjpa.jdbc.JDBCListeners", new JDBCListener[] { getAuditor() });

        StringBuilder buf = new StringBuilder();
        for (Class<?> c : types) {
            if (buf.length() > 0)
                buf.append(";");
            buf.append(c.getName());
        }

        map.put("openjpa.MetaDataFactory", "jpa(Types=" + buf.toString() + ")");

        Map<Object, Object> config = new HashMap<>(System.getProperties());
        config.putAll(map);
        return (OpenJPAEntityManagerFactorySPI) Persistence.createEntityManagerFactory("test", config);
    }

    void setDictionary() {
        JDBCConfiguration conf = (JDBCConfiguration) getEntityManagerFactory().getConfiguration();
        dict = conf.getDBDictionaryInstance();
        if (dict instanceof DerbyDictionary || dict instanceof HSQLDictionary) {
            dict.requiresCastForComparisons = false;
            dict.requiresCastForMathFunctions = false;
        } else if (dict instanceof OracleDictionary) {
            dict.setJoinSyntax("sql92");
        }
    }

    /**
     * Executes the given CriteriaQuery and JPQL string and compare their respective SQLs for equality.
     */
    void assertEquivalence(CriteriaQuery<?> c, String jpql) {
        assertEquivalence(null, c, jpql, null);
    }

    /**
     * Executes the given CriteriaQuery and JPQL string and compare their respective SQLs for equality
     * with the expected SQL.
     */
    void assertEquivalence(CriteriaQuery<?> c, String jpql, String expectedSQL) {
        assertEquivalence(null, c, jpql, expectedSQL);
    }

    /**
     * Executes the given CriteriaQuery and JPQL string after decorating with the given decorator,
     * and then compare their respective SQLs for equality.
     */
    void assertEquivalence(QueryDecorator decorator, CriteriaQuery<?> c, String jpql) {
        assertEquivalence(decorator, c, jpql, null);
    }

    /**
     * Executes the given CriteriaQuery and JPQL string and compare their respective SQLs for equality.
     * Decorates the query with the given decorator before execution.
     * supplied parameters, if any.
     */
    void assertEquivalence(QueryDecorator decorator, CriteriaQuery<?> c, String jpql, String expectedSQL) {
        logger.fine("JPQL:[" + jpql + "]");
        logger.fine("CQL :[" + ((OpenJPACriteriaQuery<?>)c).toCQL());
        Query cQ = getEntityManager().createQuery(c);
        Query jQ = getEntityManager().createQuery(jpql);
        if (decorator != null) {
            decorator.decorate(cQ);
            decorator.decorate(jQ);
        }
        executeAndCompareSQL(jpql, cQ, jQ, expectedSQL);
    }

    protected boolean same(String expected, String sql) {
        return sql.equalsIgnoreCase(expected) ||
                sql.replace(dict.getLeadingDelimiter(), "")
                    .replace(dict.getTrailingDelimiter(), "")
                    .equalsIgnoreCase(expected);
    }

    /**
     * Execute the two given queries. The first query originated from a JPQL string must be well-formed. The second
     * query originated from a Criteria is being tested.
     *
     * @param sqls
     *            The target SQL for the queries will be filled-in the given array.
     * @return true if both queries execute successfully.
     */
    void executeAndCompareSQL(String jpql, Query cQ, Query jQ, String expectedSQL) {
        List<String> jSQL = null;
        List<String> cSQL = null;
        try {
            jSQL = executeQueryAndCollectSQL(jQ);
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            fail("JPQL " + jpql + " failed to execute\r\n" + w);
        }
        getEntityManager().clear();
        try {
            cSQL = executeQueryAndCollectSQL(cQ);
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            fail("CriteriaQuery corresponding to " + jpql + " failed to execute\r\n" + w);
        }

        if (jSQL.size() != cSQL.size()) {
            printSQL("Target SQL for JPQL", jSQL);
            printSQL("Target SQL for CriteriaQuery", cSQL);
            assertEquals(jSQL.size(), cSQL.size(),
                "No. of SQL generated for JPQL and CriteriaQuery for " + jpql + " is different");
        }

        if (!(dict instanceof DerbyDictionary || dict instanceof MySQLDictionary || dict instanceof MariaDBDictionary))
            return;

        for (int i = 0; i < jSQL.size(); i++) {
            boolean eq = same(cSQL.get(i), jSQL.get(i));
            if (!eq) {
                printSQL("Target SQL for JPQL", jSQL);
                printSQL("Target SQL for CriteriaQuery", cSQL);
            }
            assertTrue(eq,
                    i + "-th SQL for JPQL and CriteriaQuery for " + jpql + " is different\r\n" +
                    "JPQL = [" + jSQL.get(i) + "]\r\n" +
                    "CSQL = [" + cSQL.get(i) + "]\r\n");
        }

        if (expectedSQL != null) {
            assertTrue(same(expectedSQL, jSQL.get(0)),
                    "SQL for JPQL " + jpql + " is different than expecetd " + expectedSQL);

        }
    }

    void executeAndCompareSQL(String jpql, String expectedSQL) {
        Query jQ = getEntityManager().createQuery(jpql);

        List<String> jSQL = null;
        try {
            jSQL = executeQueryAndCollectSQL(jQ);
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            fail("JPQL " + jpql + " failed to execute\r\n" + w);
        }

        if (!(dict instanceof DerbyDictionary || dict instanceof MySQLDictionary || dict instanceof MariaDBDictionary))
            return;

        for (int i = 0; i < jSQL.size(); i++) {
            boolean eq = same(expectedSQL, jSQL.get(i));
            if (!eq) {
                printSQL("SQL for JPQL", jSQL.get(i));
                printSQL("Expected SQL", expectedSQL);
            }
            assertTrue(eq, i + "-th SQL for JPQL: " + jSQL.get(i) + " are different than Expected SQL " + expectedSQL);
        }
    }

    void executeAndCompareSQL(Query jQ, String expectedSQL) {
        List<String> jSQL = null;
        try {
            jSQL = executeQueryAndCollectSQL(jQ);
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            fail(w.toString());
        }

        if (!(dict instanceof DerbyDictionary || dict instanceof MySQLDictionary || dict instanceof MariaDBDictionary)) {
            return;
        }

        String jSql = jSQL.get(0).trim();
        if (jSql.indexOf("optimize for 1 row") != -1) {
            jSql = jSql.substring(0, jSql.indexOf("optimize for 1 row")).trim();
        }

        boolean eq = same(expectedSQL, jSql);
        if (!eq) {
            printSQL("SQL for JPQL", jSql);
        }
        assertTrue(eq, "SQL for JPQL " + jSql + " is different than expecetd " + expectedSQL);
    }

    void executeExpectFail(CriteriaQuery<?> c, String jpql) {
        try {
            Query cQ = getEntityManager().createQuery(c);
            executeQueryAndCollectSQL(cQ);
            fail("CriteriaQuery corresponding to " + jpql + " is expected to fail\r\n");
        } catch (Exception e) {
            // expected
        }
    }

    void executeExpectFail(CriteriaQuery<?> c, String jpql, String[] paramNames, Object[] params) {
        try {
            Query cQ = getEntityManager().createQuery(c);
            for (int i = 0; i < params.length; i++)
                cQ.setParameter(paramNames[i], params[i]);
            executeQueryAndCollectSQL(cQ);
            fail("CriteriaQuery corresponding to " + jpql + " is expected to fail\r\n");
        } catch (Exception e) {
            // expected
        }
    }

    void executeExpectFail(String jpql) {
        try {
            Query jQ = getEntityManager().createQuery(jpql);
            executeQueryAndCollectSQL(jQ);
            fail("JPQL " + jpql + " is expected to Failed to execute\r\n");
        } catch (Exception e) {
            // expected
        }
    }

    void executeExpectFail(String jpql, String[] paramNames, Object[] params) {
        try {
            Query jQ = getEntityManager().createQuery(jpql);
            for (int i = 0; i < params.length; i++)
                jQ.setParameter(paramNames[i], params[i]);
            executeQueryAndCollectSQL(jQ);
            fail("JPQL " + jpql + " is expected to Failed to execute\r\n");
        } catch (Exception e) {
            // expected
        }
    }

    void printSQL(String header, String sql) {
        logger.fine(header);
        logger.fine(sql);
    }

    void printSQL(String header, List<String> sqls) {
        logger.fine(header);
        for (int i = 0; sqls != null && i < sqls.size(); i++) {
            logger.fine(i + ":" + sqls.get(i));
        }
    }


    /**
     * Execute the given query and return the generated SQL. If the query execution fail because the generated SQL is
     * ill-formed, then raised exception should carry the ill-formed SQL for diagnosis.
     */
    List<String> executeQueryAndCollectSQL(Query q) {
        getAuditor().clear();
        try {
            q.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(extractSQL(e), e);
        }
        assertFalse(getAuditor().getSQLs().isEmpty());
        return getAuditor().getSQLs();
    }

    void executeAndCompareSQL(CriteriaQuery<?> q, String expectedSQL) {
        executeAndCompareSQL(getEntityManager().createQuery(q), expectedSQL);
    }

    String extractSQL(Exception e) {
        Throwable t = e.getCause();
        if (t instanceof ReportingSQLException) {
            return ((ReportingSQLException) t).getSQL();
        }
        return "Can not extract SQL from exception " + e;
    }

    public class SQLAuditor extends FilteringJDBCListener {
        public SQLAuditor() {
            super(new ArrayList<>());
        }

        List<String> getSQLs() {
            return getCopy();
        }
    }

    /**
     * Interface to decorate a query such as set parameter or range before execution.
     *
     */
    public interface QueryDecorator {
        void decorate(Query q);
    }
}
