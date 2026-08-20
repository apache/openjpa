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
 * EmbeddedId for DID2bDependent. Contains a non-@Embeddable IdClass field.
 * (JPA 2.4.1.3 ex2b).
 */
@Embeddable
public class DID2bDependentId implements Serializable {
    String name;
    DID2bEmployeeId empPK;

    public DID2bDependentId() {}
    public DID2bDependentId(String n, DID2bEmployeeId e) { name = n; empPK = e; }
    public String getName() { return name; }
    public void setName(String n) { name = n; }
    public DID2bEmployeeId getEmpPK() { return empPK; }
    public void setEmpPK(DID2bEmployeeId e) { empPK = e; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DID2bDependentId)) return false;
        DID2bDependentId x = (DID2bDependentId) o;
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
