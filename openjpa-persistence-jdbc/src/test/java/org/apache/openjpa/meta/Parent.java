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
package org.apache.openjpa.meta;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

@Entity(name="META_PARENT")
@SecondaryTable(name = "ParentSecondaryTable", pkJoinColumns =
    { @PrimaryKeyJoinColumn(name = "idParent", referencedColumnName = "idParent") })
public class Parent {

    @Id
    @GeneratedValue
    int idParent;

    String child_ref;

    @OneToOne
    @JoinColumn(name = "CHILD_REF", table = "ParentSecondaryTable", referencedColumnName = "idChild")
    PChild child;

    @OneToOne
    @JoinColumn(name = "CHILDBI_REF", table = "ParentSecondaryTable", referencedColumnName = "idChild")
    PChildBi childbi;

    @ManyToOne
    @JoinColumn(name = "CHILDREN_REF", table = "ParentSecondaryTable", referencedColumnName = "idChild")
    PChild children;
}
