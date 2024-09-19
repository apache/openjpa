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
package org.apache.openjpa.persistence.enhance.identity;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@IdClass(DependentId4.class)
public class Dependent4 implements Serializable {

	
    private static final long serialVersionUID = 1L;

    @Id
	@Column(name = "id", unique = false, nullable = false)
	private long id;

	@Id
	@ManyToOne
	@JoinColumn(name = "parent_id", referencedColumnName = "id")
	private Employee4 parent;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Employee4 getParent() {
		return parent;
	}

	public void setParent(Employee4 parent) {
		this.parent = parent;
	}
}
