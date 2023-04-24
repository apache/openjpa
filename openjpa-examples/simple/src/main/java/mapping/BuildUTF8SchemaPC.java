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
package mapping;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * <p>Persistent type used in testing the mappingtool's buildSchema action
 * with files that use UTF-8 column names.</p>
 *
 */
@Entity
public class BuildUTF8SchemaPC {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Basic
    @Column(name = "cha\u00EEne", length = 50)
    private String stringField = null;


    public BuildUTF8SchemaPC() {}

    public BuildUTF8SchemaPC(String chain)
    {
        stringField = chain;
    }

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String chain) {
        stringField = chain;
    }

    public int getId() {
        return id;
    }

}
