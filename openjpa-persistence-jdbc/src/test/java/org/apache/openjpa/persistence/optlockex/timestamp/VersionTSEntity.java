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
package org.apache.openjpa.persistence.optlockex.timestamp;

import java.io.Serializable;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class VersionTSEntity implements Serializable {

    private static final long serialVersionUID = 2948711625184868242L;

    @Id
    private Long id;

    @Version
    @Column(columnDefinition="TIMESTAMP(3)")
    private Timestamp updateTimestamp;

    private Integer someInt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public void setSomeInt(Integer someInt) {
        this.someInt = someInt;

    }

    public Integer getSomeInt() {
        return someInt;
    }
}
