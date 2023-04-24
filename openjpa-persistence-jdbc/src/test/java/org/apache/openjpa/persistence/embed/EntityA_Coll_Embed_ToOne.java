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
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="TBL2A")
public class EntityA_Coll_Embed_ToOne implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // contains a collection of Embed1ToOne
    // Embed1ToOne does not have an element collection or to-Many relationships
    @Id
    Integer id;

    @Column(length=30)
    String name;

    @Basic(fetch=FetchType.LAZY)
    int age;

    //@PersistentCollection(elementEmbedded=true)
    //@ContainerTable

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="EMBED1ToOneS2") // use default join column name
    @AttributeOverrides({
        @AttributeOverride(name="name1", column=@Column(name="EMB_NAME1")),
        @AttributeOverride(name="name2", column=@Column(name="EMB_NAME2")),
        @AttributeOverride(name="name3", column=@Column(name="EMB_NAME3"))
    })

    protected Set<Embed_ToOne> embed1s = new HashSet<>();

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Embed_ToOne> getEmbed1ToOnes() {
        return embed1s;
    }

    public void addEmbed1ToOne(Embed_ToOne embed1) {
        embed1s.add(embed1);
    }
}

