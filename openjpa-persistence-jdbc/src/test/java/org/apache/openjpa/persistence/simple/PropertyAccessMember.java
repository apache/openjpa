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
package org.apache.openjpa.persistence.simple;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Property-access entity with @Version and equals()/hashCode() that
 * uses getClass() comparison and direct field access, mirroring the
 * JPA TCK entityManagerFactory.Member entity.
 */
@Entity
@Table(name = "PA_MEMBER")
public class PropertyAccessMember implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int memberId;
    private Integer version;
    private String memberName;

    public PropertyAccessMember() {
    }

    public PropertyAccessMember(int memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = memberName;
    }

    @Id
    @Column(name = "MEMBER_ID")
    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    @Version
    @Column(name = "VERSION")
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Column(name = "MEMBER_NAME")
    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyAccessMember member = (PropertyAccessMember) o;
        return memberId == member.memberId
            && Objects.equals(version, member.version)
            && Objects.equals(memberName, member.memberName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, version, memberName);
    }
}
