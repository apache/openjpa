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
package org.apache.openjpa.persistence.query;

import java.util.Map;
import java.util.ArrayList;

import org.apache.openjpa.persistence.test.SingleEMTestCase;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.QueryImpl;
import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;

public class TestMethodQLQuery
    extends SingleEMTestCase {

    @Override
    public void setUp() {
        setUp(SimpleEntity.class);
    }

    public void testMethodQLWithoutParametersDeclared() {
        OpenJPAQuery q = em.createQuery(QueryLanguages.LANG_METHODQL,
            getClass().getName() + ".echo");
        ((QueryImpl) q).getDelegate().setCandidateType(
            SimpleEntity.class, true);
        q.setParameter("param", 5);
        ((QueryImpl) q).getDelegate().declareParameters("Integer param");
        assertEquals(5, q.getResultList().get(0));
    }

    public void testInvalidMethodReturnType() {
        OpenJPAQuery q = em.createQuery(QueryLanguages.LANG_METHODQL,
            getClass().getName() + ".invalidReturnMeth");
        ((QueryImpl) q).getDelegate().setCandidateType(
            SimpleEntity.class, true);
        try {
            q.getResultList().get(0);
            fail("should have gotten an exception since method is invalid");
        } catch (ArgumentException ae) {
            // expected
        }
    }

    public void testVoidMethodReturnType() {
        OpenJPAQuery q = em.createQuery(QueryLanguages.LANG_METHODQL,
            getClass().getName() + ".voidMeth");
        ((QueryImpl) q).getDelegate().setCandidateType(
            SimpleEntity.class, true);
        try {
            q.getResultList().get(0);
            fail("should have gotten an exception since method is invalid");
        } catch (ArgumentException ae) {
            // expected
        }
    }

    public static ResultObjectProvider echo(StoreContext ctx,
        ClassMetaData meta, boolean subs, Map params, FetchConfiguration conf) {
        return new ListResultObjectProvider(new ArrayList(params.values()));
    }

    public static void voidMeth(StoreContext ctx,
        ClassMetaData meta, boolean subs, Map params, FetchConfiguration conf) {
    }

    public static Object invalidReturnMeth(StoreContext ctx,
        ClassMetaData meta, boolean subs, Map params, FetchConfiguration conf) {
        return null;
    }
}
