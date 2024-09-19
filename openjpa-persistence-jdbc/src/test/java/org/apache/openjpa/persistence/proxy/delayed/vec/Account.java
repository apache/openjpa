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
package org.apache.openjpa.persistence.proxy.delayed.vec;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.proxy.delayed.IAccount;
import org.apache.openjpa.persistence.proxy.delayed.IUserIdentity;

@Entity
@Table(name="DC_ACCOUNT")
public class Account implements IAccount {

    public Account() {
    }

    public Account(String name, IUserIdentity uid) {
        setName(name);
        setUserIdent(uid);
    }

    @Id
    @GeneratedValue
    @Column(name="ACCT_ID")
    private int id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="UID_ID")
    private UserIdentity userIdent;

    private String name;

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setUserIdent(IUserIdentity userIdent) {
        this.userIdent = (UserIdentity)userIdent;
    }

    @Override
    public IUserIdentity getUserIdent() {
        return userIdent;
    }
}
