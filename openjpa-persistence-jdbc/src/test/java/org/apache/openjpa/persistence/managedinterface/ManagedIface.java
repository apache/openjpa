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

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.apache.openjpa.persistence.ManagedInterface;
import org.apache.openjpa.persistence.PersistentCollection;
import org.apache.openjpa.persistence.query.SimpleEntity;

@ManagedInterface
@Entity
public interface ManagedIface extends ManagedInterfaceSup {
    int getIntField();
    void setIntField(int i);

    @Embedded ManagedInterfaceEmbed getEmbed();
    void setEmbed(ManagedInterfaceEmbed embed);

    @OneToOne(cascade=CascadeType.PERSIST) ManagedIface getSelf();
    void setSelf(ManagedIface iface);

    @PersistentCollection Set<Integer> getSetInteger();
    void setSetInteger(Set<Integer> collection);

    @OneToMany(cascade=CascadeType.PERSIST) Set<SimpleEntity> getSetPC();
    void setSetPC(Set<SimpleEntity> collection);

    @OneToMany(cascade=CascadeType.PERSIST) Set<ManagedIface> getSetI();
    void setSetI(Set<ManagedIface> collection);

    @OneToOne(cascade=CascadeType.PERSIST) SimpleEntity getPC();
    void setPC(SimpleEntity pc);

    void unimplemented();
}
