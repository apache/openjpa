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
package org.apache.openjpa.persistence.lockmgr;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

@Entity
public class LockTask {
    @Id
    private int id;

    @Version
    private int version;

    @OneToMany(mappedBy = "task", cascade = { CascadeType.ALL })
    private Collection<LockStory> stories;

    @ManyToOne(cascade = { CascadeType.ALL })
    private LockEmployee employee;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Collection<LockStory> getStories() {
        return stories;
    }

    public void setStories(Collection<LockStory> stories) {
        this.stories = stories;
    }

    public LockEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(LockEmployee employee) {
        this.employee = employee;
    }

    public int getVersion() {
        return version;
    }
    
    public String toString() {
        return this.getClass().getName() + "[id=" + getId() + ", ver="
            + getVersion() + "] stories={" + getStories() + "}";
    }
}
