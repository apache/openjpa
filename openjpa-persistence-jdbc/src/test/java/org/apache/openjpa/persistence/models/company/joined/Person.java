/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.models.company.joined;

import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="JI_Person")
@Table(name="JI_Person") // OPENJPA-121
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class Person implements IPerson {
    private static long idCounter = System.currentTimeMillis();

    @Id
    private long id = idCounter++;

    @Basic
    private String firstName;

    @Basic
    private String lastName;

    @OneToOne
    private Address homeAddress;

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return this.firstName;
    }


    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLastName() {
        return this.lastName;
    }


    public void setHomeAddress(IAddress homeAddress) {
        this.homeAddress = (Address) homeAddress;
    }

    public IAddress getHomeAddress() {
        return this.homeAddress;
    }


    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

}
