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
package org.apache.openjpa.persistence.managedinterface;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class ManagedInterfaceOwner {

    @Id
    private int id;

    private int intField;

    @OneToOne(cascade=CascadeType.PERSIST)
    private ManagedInterfaceSup iface;

    @Embedded
    private ManagedInterfaceEmbed embed;

    public int getIntField() {
        return intField;
    }

    public void setIntField(int i) {
        intField = i;
    }

    public ManagedInterfaceSup getIFace() {
        return iface;
    }

    public void setIFace(ManagedInterfaceSup iface) {
        this.iface = iface;
    }

    public ManagedInterfaceEmbed getEmbed() {
        return embed;
    }

    public void setEmbed(ManagedInterfaceEmbed embed) {
        this.embed = embed;
    }
}
