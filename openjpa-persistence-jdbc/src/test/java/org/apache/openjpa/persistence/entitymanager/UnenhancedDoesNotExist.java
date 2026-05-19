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
package org.apache.openjpa.persistence.entitymanager;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity that maps to a table that does not exist in the database.
 * Used to test that operations on non-existent tables throw RuntimeException
 * and mark transaction for rollback.
 * Prefixed with "Unenhanced" to skip build-time enhancement.
 */
@Entity
@Table(name = "DOESNOTEXIST")
public class UnenhancedDoesNotExist implements java.io.Serializable {

    private int id;
    private String firstName;

    public UnenhancedDoesNotExist() {
    }

    public UnenhancedDoesNotExist(int id) {
        this.id = id;
    }

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
