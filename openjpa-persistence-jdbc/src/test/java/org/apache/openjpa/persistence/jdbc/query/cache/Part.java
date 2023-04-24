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
package org.apache.openjpa.persistence.jdbc.query.cache;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.apache.openjpa.persistence.DataCache;

@Entity
//@MappedSuperclass
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="PARTTYPE")

@DataCache
abstract public class Part {

    @Id  int partno;
    @Column(length=20)
    String name;
    int inventory;

    @OneToMany(mappedBy="child",cascade=CascadeType.PERSIST)
    protected Collection<Usage> usedIn = new ArrayList<>();

    @Version
    long version;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getPartno() {
        return partno;
    }
    public void setPartno(int partno) {
        this.partno = partno;
    }
    public Collection<Usage> getUsedIn() {
        return usedIn;
    }
    public void setUsedIn(Collection<Usage> usedIn) {
        this.usedIn = usedIn;
    }
    public int getInventory() {
        return inventory;
    }
    public void setInventory(int inventory) {
        this.inventory = inventory;
    }
}
