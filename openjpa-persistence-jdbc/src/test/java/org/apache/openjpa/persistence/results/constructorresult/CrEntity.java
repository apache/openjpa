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
package org.apache.openjpa.persistence.results.constructorresult;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

@Entity
@Table(name = "CR_ENTITY")
@SqlResultSetMapping(
    name = "CrDtoMapping",
    classes = @ConstructorResult(
        targetClass = CrDto.class,
        columns = {
            @ColumnResult(name = "ID", type = Long.class),
            @ColumnResult(name = "NAME", type = String.class),
            @ColumnResult(name = "PRICE", type = Double.class)
        }
    )
)
@NamedNativeQuery(
    name = "CrEntity.findAllDto",
    query = "SELECT e.ID, e.NAME, e.PRICE FROM CR_ENTITY e",
    resultSetMapping = "CrDtoMapping"
)
public class CrEntity {

    @Id
    private long id;

    private String name;

    private double price;

    public CrEntity() {
    }

    public CrEntity(long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
