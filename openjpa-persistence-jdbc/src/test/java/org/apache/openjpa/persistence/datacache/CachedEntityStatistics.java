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
package org.apache.openjpa.persistence.datacache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

@Entity
public class CachedEntityStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;

    @Version
    protected int version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Set<CachedEntityStatistics> eagerList = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Set<CachedEntityStatistics> lazyList = new HashSet<>();

    String firstName, lastName;

    public CachedEntityStatistics() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setVersion(int v) {
        version = v;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getVersion() {
        return version;
    }

    public Collection<CachedEntityStatistics> getEagerList() {
        return eagerList;
    }

    public void addEager(CachedEntityStatistics p) {
        eagerList.add(p);
    }

    public Set<CachedEntityStatistics> getLazyList() {
        return lazyList;
    }

    public void setLazyList(Set<CachedEntityStatistics> p) {
        lazyList = p;
    }

    public void addLazy(CachedEntityStatistics p) {
        lazyList.add(p);
    }

    @Override
    public int hashCode() {
        int res = id * 31;
        for (CachedEntityStatistics m : eagerList) {
            res = res * 31 + m.hashCode();
        }

        return res;

    }

    @Override
    public String toString() {
        return id + "->" + getEagerList();
    }

}
