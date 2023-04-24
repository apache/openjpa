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
package org.apache.openjpa.persistence.pudefaults;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/*
 * An entity which has a @Table annotation which contains a schema name,
 * as such schema in the annotation should take precedence over the
 * persistence-unit-default schema (see pudefaults-orm.xml file).  However,
 * the schema has been overridden in the mapping file as such the
 * schema in the mapping file trumps all.
 */
@Entity
@Table(name = "PUSchemaInTableMapping", schema = "schemaInTableAnnotation")
public class PUSchemaInTableMappingEntity implements Serializable {

    private static final long serialVersionUID = -566154189043208199L;

    @Id
    @SequenceGenerator(name = "Seq_4TableMappingSchema", sequenceName = "SeqName_4TableMappingSchema")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Seq_4TableMappingSchema")
    @Column(name = "ID")
    private long id;

    public long getId() { return id; }
}
