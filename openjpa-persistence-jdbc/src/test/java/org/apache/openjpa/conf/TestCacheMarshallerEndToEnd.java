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
package org.apache.openjpa.conf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.query.NamedQueryEntity;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.PersistenceTestCase;

public class TestCacheMarshallerEndToEnd
    extends PersistenceTestCase {

    private static final Object[] STORE_PROPS = new Object[] {
        "openjpa.CacheMarshallers",
        "default(Id=" + MetaDataCacheMaintenance.class.getName()
            + ", OutputFile=" + MetaDataCacheMaintenance.class.getName() +".ser"
            + ", ConsumeSerializationErrors=false"
            + ", ValidationPolicy="
            + OpenJPAVersionAndConfigurationTypeValidationPolicy.class.getName()
            + ")",
        "openjpa.QueryCompilationCache",
        "java.util.concurrent.ConcurrentHashMap",
        AllFieldTypes.class,
        NamedQueryEntity.class,
        CLEAR_TABLES
    };

    private static final Object[] LOAD_PROPS = new Object[] {
        "openjpa.CacheMarshallers",
        "default(Id=" + MetaDataCacheMaintenance.class.getName()
            + ", InputURL=file:" + MetaDataCacheMaintenance.class.getName()
                + ".ser"
            + ", ConsumeSerializationErrors=false"
            + ", ValidationPolicy="
            + OpenJPAVersionAndConfigurationTypeValidationPolicy.class.getName()
            + ")",
        "openjpa.QueryCompilationCache",
        "java.util.concurrent.ConcurrentHashMap",
        AllFieldTypes.class,
        NamedQueryEntity.class
    };


    public void testCacheMarshallerEndToEnd()
        throws IOException {
        OpenJPAEntityManagerFactorySPI emf = createEMF(STORE_PROPS);
        CacheMarshallerImpl cm = (CacheMarshallerImpl)
            CacheMarshallersValue.getMarshallerById(
            emf.getConfiguration(), MetaDataCacheMaintenance.class.getName());
        cm.getOutputFile().delete();
        MetaDataCacheMaintenance maint = new MetaDataCacheMaintenance(
            JPAFacadeHelper.toBrokerFactory(emf), false, true);
        final List<String> lines = new ArrayList<String>();
        PrintStream out = new PrintStream(new ByteArrayOutputStream()) {
            public void println(String line) {
                lines.add(line);
            }

            public void println(Object line) {
                println(line.toString());
            }
        };
        maint.setOutputStream(out);
        maint.store();
        assertContains(lines, "    " + AllFieldTypes.class.getName());
        assertContains(lines, "    " + NamedQueryEntity.class.getName());
        assertContains(lines, "    NamedQueryEntity.namedQuery");
        emf.close();

        emf = createEMF(LOAD_PROPS);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new NamedQueryEntity("foo"));
        em.flush();
        Query q = em.createNamedQuery("NamedQueryEntity.namedQuery");
        assertEquals(1, q.getResultList().size());
        em.getTransaction().rollback();
        em.close();
        emf.close();
    }

    private void assertContains(List<String> lines, String prefix) {
        for (String line : lines)
            if (line.startsWith(prefix))
                return;
        fail("should contain a line starting with " + prefix
            + ": " + lines);
    }
}
