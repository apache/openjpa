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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;

/**
 * <p>Persistent type with a compound primary key and LRS fields.</p>
 *
 * @author Abe White
 */
@Entity
public class LRSCompoundPC
    implements LRSPCIntf {

    private static int idGen = 0;

    private int id1;
    private int id2;

    private String stringField;
    private Set stringSet = new HashSet();
    private Set relSet = new HashSet();
    private Collection stringCollection = new ArrayList();
    private Collection relCollection = new ArrayList();
    private Map stringMap = new HashMap();
    private Map relMap = new HashMap();

    private LRSCompoundPC() {
    }

    public LRSCompoundPC(String str) {
        id1 = idGen++;
        id2 = idGen++;
        stringField = str;
    }

    @Override
    public LRSPCIntf newInstance(String stringField) {
        return new LRSCompoundPC(stringField);
    }

    @Override
    public Set getStringSet() {
        return this.stringSet;
    }

    @Override
    public void setStringSet(Set stringSet) {
        this.stringSet = stringSet;
    }

    @Override
    public Set getRelSet() {
        return this.relSet;
    }

    @Override
    public void setRelSet(Set relSet) {
        this.relSet = relSet;
    }

    @Override
    public Collection getStringCollection() {
        return this.stringCollection;
    }

    @Override
    public void setStringCollection(Collection stringCollection) {
        this.stringCollection = stringCollection;
    }

    @Override
    public Collection getRelCollection() {
        return this.relCollection;
    }

    @Override
    public void setRelCollection(Collection relCollection) {
        this.relCollection = relCollection;
    }

    @Override
    public Map getStringMap() {
        return this.stringMap;
    }

    @Override
    public void setStringMap(Map stringMap) {
        this.stringMap = stringMap;
    }

    @Override
    public Map getRelMap() {
        return this.relMap;
    }

    @Override
    public void setRelMap(Map relMap) {
        this.relMap = relMap;
    }

    @Override
    public String getStringField() {
        return this.stringField;
    }

    @Override
    public int compareTo(Object other) {
        return stringField.compareTo(((LRSCompoundPC) other).stringField);
    }
}
