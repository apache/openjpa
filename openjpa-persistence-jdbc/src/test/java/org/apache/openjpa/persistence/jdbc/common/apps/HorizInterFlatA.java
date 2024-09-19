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
package org.apache.openjpa.persistence.jdbc.common.apps;

import jakarta.persistence.Entity;

/**
 * Interface for intermediate horizontal mappings with flat mappings.
 *
 * @author <a href="mailto:marc@solarmetric.com">Marc Prud'hommeaux</a>
 */
@Entity
public class HorizInterFlatA
    implements HorizInterA {

    private String stringA;
    private int intA;

    @Override
    public void setStringA(String stringA) {
        this.stringA = stringA;
    }

    @Override
    public String getStringA() {
        return this.stringA;
    }

    @Override
    public void setIntA(int intA) {
        this.intA = intA;
    }

    @Override
    public int getIntA() {
        return this.intA;
    }
}
