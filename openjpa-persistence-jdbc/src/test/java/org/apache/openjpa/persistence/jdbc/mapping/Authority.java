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
package org.apache.openjpa.persistence.jdbc.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.openjpa.persistence.Persistent;
import org.apache.openjpa.persistence.jdbc.Strategy;

@Entity
@Table(name="authority")
@NamedQueries( {
       @NamedQuery(name = "AllIonAuthorities", query = "SELECT x FROM IonAuthority x")
})
public class Authority {
@Id
       @GeneratedValue(strategy = GenerationType.AUTO)
       @Column(name = "ID")
       private Integer id;

       @Enumerated( EnumType.STRING )
       @Column(nullable=false, length=128, updatable=true, insertable=true)
       @Persistent
       @Strategy("org.apache.openjpa.jdbc.meta.strats.EnumValueHandler")
       private AuthorityValues authorityName;


       @XmlType(name = "IonAuthorityValues")
       @XmlEnum
       public enum AuthorityValues {

          AUTH1,
          AUTH2,
       }

       public Authority() {}
       public Authority(AuthorityValues auth) {
           authorityName = auth;
       }

       public Integer getId() {
           return id;
       }

       public void setAuthorityName(AuthorityValues auth) {
           authorityName = auth;
       }

       public AuthorityValues getAuthorityName() {
           return authorityName;
       }
}
