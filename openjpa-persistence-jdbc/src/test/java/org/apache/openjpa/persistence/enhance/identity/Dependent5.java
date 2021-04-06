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
package org.apache.openjpa.persistence.enhance.identity;
import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

@Entity
@IdClass(DependentId5.class)
public class Dependent5 implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    String name;

    @Id
    @JoinColumns({
       @JoinColumn(name="FIRSTNAME", referencedColumnName="FIRSTNAME"),
       @JoinColumn(name="LASTNAME", referencedColumnName="LASTNAME")
    })
    @ManyToOne
    Employee5 emp;

    public Dependent5(String name, Employee5 emp) {
        this.name = name;
        this.emp = emp;
    }

    public Dependent5(DependentId5 dId, Employee5 emp){
        this.name = dId.getName();
        this.emp = emp;
    }
}
