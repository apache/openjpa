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
package org.apache.openjpa.persistence.embed.compositepk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "CPK_SUBJECT_GROUP")
public class SubjectGroup {
    @Id
    @Column(name = "ID")
    private Integer id;

    @ManyToMany
    @JoinTable(name = "CPK_GROUP_SUBJECTS")
    private List<Subject> subjects = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "CPK_GROUP_TOPICS")
    private Map<Subject, Topic> topics = new HashMap<>();

    public SubjectGroup() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public Map<Subject, Topic> getTopics() {
        return topics;
    }
}
