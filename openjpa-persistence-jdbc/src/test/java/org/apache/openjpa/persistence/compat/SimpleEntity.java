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
package org.apache.openjpa.persistence.compat;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

@NamedQuery(name="FindXTwo", query="select s from simple s where s.name = :fname")

@NamedQueries( {
    @NamedQuery(name="FindOne", query="select s from simple s where s.name = :fname"),
    @NamedQuery(name="FindOne", query="select s from simple s where s.name = :fname"),
    @NamedQuery(name="FindAll", query="select s from simple s")
})

@NamedNativeQueries( {
    @NamedNativeQuery(name = "findSimpleEntitites",
        query = "SELECT ID, NAME, VALUE FROM SIMPLE_ENTITY",
        resultSetMapping = "simpleEntitiesResult") })

@SqlResultSetMapping(name = "simpleEntitiesResult",
    entities = @EntityResult(
    entityClass = org.apache.openjpa.persistence.query.SimpleEntity.class,
    fields = {@FieldResult(name = "id", column = "ID"),
        @FieldResult(name = "name", column = "NAME"),
        @FieldResult(name = "value", column = "VALUE") }))
@Entity(name = "simple")
@Table(name = "SIMPLE_ENTITY")
public class SimpleEntity implements Serializable {

    
    private static final long serialVersionUID = 1L;
    public static final String NAMED_QUERY_WITH_POSITIONAL_PARAMS = "SelectWithPositionalParameter";
    public static final String NAMED_QUERY_WITH_NAMED_PARAMS = "SelectWithNamedParameter";

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private long id;

    @Basic
    @Column(name = "NAME")
    private String name;

    @Basic
    @Column(name = "VALUE")
    private String value;

    public SimpleEntity() {
    }

    public SimpleEntity(String name, String value) {
        this();
        this.name = name;
        this.value = value;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

