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
package org.apache.openjpa.persistence.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Has optional relations into two joined hierarchies.
 */
@Entity
@Table(name = "TPA_ITEM")
public class TypePathItem {

    @Id
    @Column(name = "ID")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VEHICLE_ID")
    private TypePathVehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TOOL_ID")
    private TypePathTool tool;

    public TypePathItem() {
    }

    public TypePathItem(long id, TypePathVehicle vehicle, TypePathTool tool) {
        this.id = id;
        this.vehicle = vehicle;
        this.tool = tool;
    }

    public long getId() {
        return id;
    }

    public TypePathVehicle getVehicle() {
        return vehicle;
    }

    public TypePathTool getTool() {
        return tool;
    }
}
