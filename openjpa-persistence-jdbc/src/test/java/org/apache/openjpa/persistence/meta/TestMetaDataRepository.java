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
package org.apache.openjpa.persistence.meta;

import java.security.AccessController;
import java.util.Collection;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

public class TestMetaDataRepository extends AbstractPersistenceTestCase {
    private final String PU_NAME = "mdr-pu";

    @Override
    public void setUp() throws Exception {
        super.setUp();

    }

    /**
     * This method ensures that some of the basic MetaData is properly registered after creating an
     * EMF.
     */
    public void testPreloadBasic() {
        OpenJPAEntityManagerFactorySPI emf = null;
        try {
            emf = createNamedEMF(PU_NAME, "openjpa.MetaDataRepository", "Preload=true,NoLock=true");
            MetaDataRepository mdr = emf.getConfiguration().getMetaDataRepositoryInstance();

            // Check that there is cached metadata in the repo
            ClassMetaData metadata = mdr.getCachedMetaData(MdrTestEntity.class);
            assertNotNull(metadata);

            // Make sure that there is an alias registered
            int numEntities =
                mdr.getPersistentTypeNames(false,
                    AccessController.doPrivileged(J2DoPrivHelper.getContextClassLoaderAction())).size();
            Collection<String> aliases = mdr.getAliasNames();
            assertTrue(aliases.size() == numEntities);

            QueryMetaData query = mdr.getCachedQueryMetaData(MdrTestEntity.class, "query");
            assertNotNull(query);
        } finally {
            if (emf != null) {
                emf.close();
            }
        }
    }
}
