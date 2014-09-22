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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

/*
 * An entity which has a sequence where the sequence doesn't defined a schema,
 * as such the persistence-unit-default schema (see pudefaults-orm.xml file)
 * should be used when SQL operations are performed on the sequence. 
 */
@Entity
public class PUDefaultSchemaEntity implements Serializable {

    private static final long serialVersionUID = 2134948659397762341L;

    @Id
    @SequenceGenerator(name = "Seq_4DefaultSchema", sequenceName = "SeqName_4DefaultSchema")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Seq_4DefaultSchema")
    @Column(name = "ID")
    private long id;

    public long getId() { return id; }
}
