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
package org.apache.openjpa.persistence.query.common.apps;

import jakarta.persistence.Entity;

/**
 * <p>Used in testing; should be enhanced.</p>
 *
 * @author Abe White
 */
@Entity
public class QueryTest3 extends QueryTest2 {

    private int num2 = 0;

    public QueryTest3() {
    }

    public QueryTest3(int id) {
        super(id);
    }

    public void setNum2(int val) {
        num2 = val;
    }
}
