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
package org.apache.openjpa.persistence.embed;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

/**
 * EmbeddedId for DID2cDependent. Contains a non-@Embeddable IdClass field.
 */
@Embeddable
public class DID2cDependentId implements Serializable {
    private static final long serialVersionUID = 1L;

    String name;
    DID2cEmployeeId empPK;

    public DID2cDependentId() {}
    public DID2cDependentId(String n, DID2cEmployeeId e) { name = n; empPK = e; }
    public String getName() { return name; }
    public void setName(String n) { name = n; }
    public DID2cEmployeeId getEmpPK() { return empPK; }
    public void setEmpPK(DID2cEmployeeId e) { empPK = e; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DID2cDependentId)) return false;
        DID2cDependentId x = (DID2cDependentId) o;
        return eq(name, x.name) && eq(empPK, x.empPK);
    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
