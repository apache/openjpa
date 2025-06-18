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

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestConnectionProperties extends SingleEMFTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp(new Object[] { "openjpa.ConnectionProperties",
            "autoReconnect=true,MaxActive=10,MaxWait=6000,TestOnBorrow=true"
        });
        emf.createEntityManager().close();
    }

    public void testEmWithProps() {
        OpenJPAEntityManager em = emf.createEntityManager();
        em.close();
    }
}
