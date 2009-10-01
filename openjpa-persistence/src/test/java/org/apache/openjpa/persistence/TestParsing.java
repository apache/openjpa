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
package org.apache.openjpa.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.meta.MetaDataRepository;

public class TestParsing extends TestCase {

    /**
     * Testcase for added OPENJPA-859.
     * 
     * This scenario is testing whether the default annotations are being generated for a class that
     * isn't annotated with a persistence class type (ie: @Entity, @Mapped-Superclass, @Embeddable),
     * but it is in a mapping file.
     * 
     * @throws Exception
     */
    public void testMixedOrmAnno() throws Exception {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        Map<String, String> m = new HashMap<String, String>();

        ConfigurationProvider cp = pd.load("", "test_parsing", m);
        OpenJPAConfigurationImpl conf = new OpenJPAConfigurationImpl(true, true);
        cp.setInto(conf);

        MetaDataRepository mdr = conf.getMetaDataRepositoryInstance();
        Set<String> classes = mdr.getPersistentTypeNames(false, null);
        for (String c : classes) {
            Class cls = Class.forName(c);
            mdr.getMetaData(cls, null, true);
        }
    }
}

