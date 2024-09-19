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
package org.apache.openjpa.integration.persistence.provider;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

public class DummyProvider1 implements PersistenceProvider {

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(
        PersistenceUnitInfo persistenceunitinfo, Map map) {
        return null;
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        // no-op
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return false;
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String s, Map map) {
        return null;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return null;
    }

}
