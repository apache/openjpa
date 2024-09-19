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
package org.apache.openjpa.persistence.jdbc.unique;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="UNIQUE_B",
	   uniqueConstraints={@UniqueConstraint(columnNames={"f1","f2"})})
public class UniqueB {
	@Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="testGenerator")
	@TableGenerator(name="testGenerator", table="UNIQUE_GENERATOR",
			pkColumnName="GEN1", valueColumnName="GEN2",
            uniqueConstraints={@UniqueConstraint(columnNames={"GEN1","GEN2"})})
	private int bid;

	// Same named field in UniqueA also is defined as unique
	@Column(unique=true, nullable=false)
	private int f1;

	@Column(nullable=false)
	private int f2;
}
