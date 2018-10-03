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
package org.apache.openjpa.persistence.kernel.common.apps;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * <p>Persistent type used in testing</p>
 *
 * @author Abe White
 */

@Entity
@Table(name = "impl_1")
public class InterfaceTestImpl1 implements InterfaceTest, Serializable {
    private static final long serialVersionUID = 1L;
    private String stringField;

    protected InterfaceTestImpl1() {
    }

    public InterfaceTestImpl1(String str) {
        this.stringField = str;
    }

    @Override
    public String getStringField() {
        return this.stringField;
    }

    @Override
    public void setStringField(String str) {
        this.stringField = str;
    }
}
