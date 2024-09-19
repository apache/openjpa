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

import java.util.Collection;
import java.util.HashSet;

import jakarta.persistence.Entity;

/**
 * <p>Helper class in eager to-many join testing.</p>
 *
 * @author Abe White
 */
@Entity
public class EagerOuterJoinPC2 {

    private String name = null;
    private EagerOuterJoinPC ref = null;
    private HelperPC helper = null;
    private Collection stringCollection = new HashSet();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EagerOuterJoinPC getRef() {
        return this.ref;
    }

    public void setRef(EagerOuterJoinPC ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + name;
    }

    public HelperPC getHelper() {
        return this.helper;
    }

    public void setHelper(HelperPC helper) {
        this.helper = helper;
    }

    public Collection getStringCollection() {
        return this.stringCollection;
    }

    public void setStringCollection(Collection stringCollection) {
        this.stringCollection = stringCollection;
    }
}
