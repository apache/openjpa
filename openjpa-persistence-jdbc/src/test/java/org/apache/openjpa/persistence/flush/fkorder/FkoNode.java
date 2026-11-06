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
package org.apache.openjpa.persistence.flush.fkorder;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "OJ3034_NODE")
public class FkoNode {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "NEXT_ID", nullable = false)
    private FkoNode next;

    public FkoNode() {
    }

    public FkoNode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public FkoNode getNext() {
        return next;
    }

    public void setNext(FkoNode next) {
        this.next = next;
    }
}
