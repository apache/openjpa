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
package org.apache.openjpa.persistence.flush;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name="FL_ASSIGN")
public class Assignment implements Serializable {

    private static final long serialVersionUID = -7707299604883998179L;

    @Id
    @Column(name="ASSIGN_ID")
    @SequenceGenerator(name="assignIdSeq", sequenceName="FL_ASSIGN_SEQ")
    @GeneratedValue(generator="assignIdSeq", strategy=GenerationType.SEQUENCE)
    protected Long assignId;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch=FetchType.LAZY)
    @JoinColumn(name="TOPIC_ID")
    protected Topic topic;

    @Column(name="ASSIGN_TEXT")
    protected String assignText;

    public Long getAssignmentId() {
        return assignId;
    }

    public void setAssignmentId(Long assignId) {
        this.assignId = assignId;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public String getAssignmentText() {
        return assignText;
    }

    public void setAssignmentText(String assignText) {
        this.assignText = assignText;
    }
}
