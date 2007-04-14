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
package org.apache.openjpa.persistence.test;

import java.util.Map;
import java.util.HashMap;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

public abstract class SingleEMFTestCase
    extends PersistenceTestCase {

    protected OpenJPAEntityManagerFactory emf;

    /**
     * Initialize entity manager factory.
     *
     * @param props list of persistent types used in testing and/or 
     * configuration values in the form key,value,key,value...
     */
    protected void setUp(Object... props) {
        emf = createEMF(props);
    }

    /**
     * Closes the entity manager factory.
     */
    public void tearDown() {
        if (emf == null)
            return;

        try {
            clear(emf);
        } finally {
            closeEMF(emf);
        }
    }
}
