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
package org.apache.openjpa.persistence.merge.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

@Entity
@Table(name = "MRG_PACKAGE2")
@TableGenerator(name = "Pkg2Gen", allocationSize = 10, pkColumnValue = "MRG_PACKAGE2")
public class ShipPackage2 {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Pkg2Gen")
    @Column(name = "PKG_ID")
    private long id;

    @OneToOne(mappedBy = "pkg", fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    private Label2 label;

    @SuppressWarnings("unused")
    @Version
    private int version;

    public void setId(long pid) {
        id = pid;
    }

    public long getId() {
        return id;
    }

    public void setLabel2(Label2 l) {
        label = l;
    }

    public Label2 getLabel2() {
        return label;
    }

}
