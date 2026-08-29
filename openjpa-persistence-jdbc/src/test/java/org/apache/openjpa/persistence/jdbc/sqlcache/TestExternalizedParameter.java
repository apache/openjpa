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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.QueryImpl;

import junit.framework.TestCase;

/**
 * Tests that we can detect if a query is using query parameters for fields whose values are externalized.
 * <p>
 * Such queries can not be reparameterized and are therefore excluded from the prepared query cache. The
 * detection operates on the {@link QueryExpressions} that are attached as a user object to the
 * {@link ResultList} produced by the kernel query. Since JPA 3.2 the facade
 * {@link jakarta.persistence.Query#getResultList()} returns a mutable copy of that result rather than the
 * {@code ResultList} itself, so the tests execute the kernel query directly to get hold of the expressions.
 *
 * @author Pinaki Poddar
 *
 */
public class TestExternalizedParameter extends TestCase {
    private static String RESOURCE = "META-INF/persistence.xml";
    private static String UNIT_NAME = "PreparedQuery";
    private static EntityManagerFactory emf;

    @Override
    public void setUp() throws Exception {
        if (emf == null) {
            Properties config = new Properties();
            config.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true,SchemaAction='drop,add')");
            config.put("openjpa.jdbc.QuerySQLCache", "true");
            config.put("openjpa.RuntimeUnenhancedClasses", "unsupported");
            config.put("openjpa.DynamicEnhancementAgent", "false");
            config.put("openjpa.Log", "SQL=WARN");
            emf = OpenJPAPersistence.createEntityManagerFactory(UNIT_NAME, RESOURCE, config);
        }
    }

    public void testNoFalseAlarmOnExternalizedParameterDetection() {
        String jpql = "select b from Book b where b.title=:title";
        EntityManager em = emf.createEntityManager();
        Map<String, Object> params = new HashMap<>();
        params.put("title", "XYZ");

        QueryExpressions[] exps = getExpressions(execute(em, jpql, params));
        assertNotNull(exps);

        assertFalse(isUsingExternalizedParameter(exps[0]));
    }

    public void testCanDetectExternalizedSingleParameterValue() {
        String jpql = "select b from Book b where b.token=:token";
        EntityManager em = emf.createEntityManager();
        Map<String, Object> params = new HashMap<>();
        params.put("token", "MEDIUM");

        QueryExpressions[] exps = getExpressions(execute(em, jpql, params));
        assertNotNull(exps);

        assertTrue(isUsingExternalizedParameter(exps[0]));
    }

    public void testCanDetectExternalizedMixedParameterValue() {
        String jpql = "select b from Book b where b.token=:token and b.title = :title";
        EntityManager em = emf.createEntityManager();
        Map<String, Object> params = new HashMap<>();
        params.put("token", "MEDIUM");
        params.put("title", "LARGE");

        QueryExpressions[] exps = getExpressions(execute(em, jpql, params));
        assertNotNull(exps);

        assertTrue(isUsingExternalizedParameter(exps[0]));
    }

    /**
     * Executes the given JPQL on the kernel query to get hold of the raw {@link ResultList}.
     */
    Object execute(EntityManager em, String jpql, Map<String, Object> params) {
        QueryImpl<?> query = (QueryImpl<?>) em.createQuery(jpql);
        return query.getDelegate().execute(params);
    }

    public QueryExpressions[] getExpressions(Object result) {
        if (!(result instanceof ResultList))
            return null;
        Object userObject = ((ResultList<?>)result).getUserObject();
        if (userObject == null || !userObject.getClass().isArray() || ((Object[])userObject).length != 2)
            return null;
        Object executor = ((Object[])userObject)[1];
        if (!(executor instanceof StoreQuery.Executor))
            return null;
        return ((StoreQuery.Executor)executor).getQueryExpressions();
    }

    boolean isUsingExternalizedParameter(QueryExpressions exp) {
        List<FieldMetaData> fmds = exp.getParameterizedFields();
        for (FieldMetaData fmd : fmds) {
            if (fmd.isExternalized())
                return true;
        }
        return false;
    }

}
