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
package org.apache.openjpa.persistence.relations;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

@Entity
public class OneManyEagerParent {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @OneToMany(mappedBy="parent", fetch=FetchType.EAGER)
    @OrderBy("name ASC")
    private List<OneManyLazyChild> lazychildren =
        new ArrayList<>();

    @OneToMany(mappedBy="parent", fetch=FetchType.EAGER)
    @OrderBy("name ASC")
    private List<OneManyEagerChild> eagerchildren =
        new ArrayList<>();

    @Version
    private Integer optLock;

    public long getId() {
        return id;
    }

    public List<OneManyLazyChild> getLazyChildren() {
        return lazychildren;
    }

    public void addLazyChild(OneManyLazyChild child) {
        child.setParent(this);
        lazychildren.add(child);
    }

    public List<OneManyEagerChild> getEagerChildren() {
        return eagerchildren;
    }

    public void addEagerChild(OneManyEagerChild child) {
        child.setParent(this);
        eagerchildren.add(child);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
