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
package org.apache.openjpa.persistence.identity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity with a compound Id of two fields, one simple and one an Entity.
 *
 * @author Michael Vorburger
 */
@Entity
@Table(name = "test_complex")
@IdClass(ComplexCompoundIdTestEntityId.class)
public class ComplexCompoundIdTestEntity {

	@Id
	@Column(nullable = false)
	private Long id;

    @Id
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH },
            fetch = FetchType.LAZY, optional = true)
    @JoinColumn(nullable = true, name = "type_id")
	private TypeEntity type;

	public Long getId() {
		return id;
	}

	public TypeEntity getType() {
		return type;
	}
}
