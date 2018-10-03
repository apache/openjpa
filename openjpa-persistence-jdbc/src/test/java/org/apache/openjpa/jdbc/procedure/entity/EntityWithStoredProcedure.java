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
package org.apache.openjpa.jdbc.procedure.entity;

import static javax.persistence.ParameterMode.INOUT;
import static javax.persistence.ParameterMode.OUT;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.StoredProcedureParameter;

@Entity
@NamedStoredProcedureQueries({
        @NamedStoredProcedureQuery(
                name = "EntityWithStoredProcedure.simple",
                procedureName = "TESTSIMPLE"
        ),
        @NamedStoredProcedureQuery(
                name = "EntityWithStoredProcedure.inParams",
                procedureName = "TESTINS",
                parameters = {
                        @StoredProcedureParameter(name = "SOME_NUMBER", type = Integer.class),
                        @StoredProcedureParameter(name = "SOME_STRING", type = String.class)
                }
        ),
        @NamedStoredProcedureQuery(
                name = "EntityWithStoredProcedure.x2",
                procedureName = "XTWO",
                parameters = {
                        @StoredProcedureParameter(name = "SOME_NUMBER", type = Integer.class),
                        @StoredProcedureParameter(name = "RESULT", type = Integer.class, mode = OUT)
                }
        ),
        @NamedStoredProcedureQuery(
                name = "EntityWithStoredProcedure.inout",
                procedureName = "XINOUT",
                parameters = {
                        @StoredProcedureParameter(name = "P", type = Integer.class, mode = INOUT)
                }
        ),
        @NamedStoredProcedureQuery(
                name = "EntityWithStoredProcedure.mapping",
                procedureName = "MAPPING",
                resultSetMappings = {"mapping1", "mapping2"}
        )
})
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "mapping1",
                entities = @EntityResult(entityClass = EntityWithStoredProcedure.class)
        ),
        @SqlResultSetMapping(
                name = "mapping2",
                entities = @EntityResult(entityClass = EntityWithStoredProcedure.Mapping2.class)
        )
})
public class EntityWithStoredProcedure {
    @Id
    private long id;
    private String name;

    public EntityWithStoredProcedure() {
        // no-op
    }

    public EntityWithStoredProcedure(final long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityWithStoredProcedure that = (EntityWithStoredProcedure) o;
        return id == that.id && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + name.hashCode();
        return result;
    }

    @Entity
    public static class Mapping2 {
        @Id
        private long id;
        private String name;

        public long getId() {
            return id;
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
    }
}
