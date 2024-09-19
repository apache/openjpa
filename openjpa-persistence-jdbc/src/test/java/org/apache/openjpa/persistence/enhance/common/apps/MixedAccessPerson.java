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
package org.apache.openjpa.persistence.enhance.common.apps;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Entity to test subclass enhancing with mixed access
 * Note that this class will not be enhanced during the build!
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
@Entity
@Access(AccessType.FIELD)
public class MixedAccessPerson {

    @Id
    @GeneratedValue
    private int id;

    private String firstName;

    private String internalLastName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Access(AccessType.PROPERTY)
    public String getLastName() {
        return internalLastName;
    }

    public void setLastName(String lastName) {
        this.internalLastName = lastName != null ? "changed_" + lastName : null;
    }
}
