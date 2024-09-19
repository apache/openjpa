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
package org.apache.openjpa.persistence.lock.extended;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashSet;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Version;

@NamedQueries ( value={
        @NamedQuery(
            name="findLSE1xmLfJTEgrNormal"
            , query="SELECT c FROM LSE1xmLfJTEgr c WHERE c.firstName LIKE :firstName"
            , lockMode=LockModeType.PESSIMISTIC_WRITE
            ),
        @NamedQuery(
            name="findLSE1xmLfJTEgrExtended"
            , query="SELECT c FROM LSE1xmLfJTEgr c WHERE c.firstName LIKE :firstName"
            , lockMode=LockModeType.PESSIMISTIC_WRITE
            , hints={@QueryHint(name="jakarta.persistence.lock.scope",value="EXTENDED")}
            )
        }
    )

@Entity
public class LSE1xmLfJTEgr implements Externalizable {

    @Id
    private int id;

    @Version
    private int version;

    private String firstName;

    @OneToMany(fetch=FetchType.EAGER)//(mappedBy="ownerOne")
    private Collection<LSE1xmRt> uniRight = new HashSet<>();

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

    public Collection<LSE1xmRt> getUniRight() {
        return uniRight;
    }

    public void setUnitRight(Collection<LSE1xmRt> uniRight) {
        this.uniRight = uniRight;
    }

    public void addUnitRight(LSE1xmRt uniRight) {
        this.uniRight.add(uniRight);
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + '@'
            + Integer.toHexString(System.identityHashCode(this)) + "[id="
            + getId() + ", ver=" + getVersion() + ", firstName=" + firstName + "] ownedMany=" + getUniRight();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException {
        id = in.readInt();
        version = in.readInt();
        firstName = in.readUTF();
        uniRight = (Collection<LSE1xmRt>) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);
        out.writeInt(version);
        out.writeUTF(firstName);
        out.writeObject(uniRight);
    }
}
