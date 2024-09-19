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
package org.apache.openjpa.persistence.fields;

import static jakarta.persistence.GenerationType.IDENTITY;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class ByteArrayHolder implements Serializable {
    @Id
    @Column(name="TASK_ID")
    @GeneratedValue(strategy=IDENTITY)
    private int taskId;

    @Column(columnDefinition="CHAR(16) FOR BIT DATA NOT NULL")  // type 1004 size -1 should be size 0
    //@Lob //type 1004 size -1 should be 1003
    private byte[] tkiid;

    private static final long serialVersionUID = 1L;

    public ByteArrayHolder() {
        super();
    }

    public int getTaskId() {
        return this.taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public byte[] getTkiid() {
        return this.tkiid;
    }

    public void setTkiid(byte[] tkiid) {
        this.tkiid = tkiid;
    }
}
