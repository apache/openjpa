/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.kernel;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "nholder2")
public class TestEJBNoPersistentFieldsNholderEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private int idkey;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private EJBNoPersistentFieldsNoPersistentFieldsPCEntity npf;

    public TestEJBNoPersistentFieldsNholderEntity() {
    }

    public TestEJBNoPersistentFieldsNholderEntity(EJBNoPersistentFieldsNoPersistentFieldsPCEntity npf, int idkey) {
        this.npf = npf;
        this.idkey = idkey;
    }

    public void setNpf(EJBNoPersistentFieldsNoPersistentFieldsPCEntity npf) {
        this.npf = npf;
    }

    public EJBNoPersistentFieldsNoPersistentFieldsPCEntity getNpf() {
        return this.npf;
    }

    public int getIdKey() {
        return idkey;
    }

    public void setIdKey(int idkey) {
        this.idkey = idkey;
    }
}
