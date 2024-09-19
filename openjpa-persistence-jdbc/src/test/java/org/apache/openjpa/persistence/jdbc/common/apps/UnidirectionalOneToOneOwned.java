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
package org.apache.openjpa.persistence.jdbc.common.apps;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Entity which is traget of a unidirectional one-to-one relation.
 *
 * Used in
 * @see
 * org.apache.openjpa.persistence.query.TestProjectionQueryWithIdenticalResult
 *
 * @author Pinaki Poddar
 *
 */
@Entity
public class UnidirectionalOneToOneOwned {
	@Id
	@GeneratedValue
	private long id;

	private String marker;

	@Version
	private int version;

	public String getMarker() {
		return marker;
	}

	public void setMarker(String marker) {
		this.marker = marker;
	}

	public long getId() {
		return id;
	}

	public int getVersion() {
		return version;
	}
}
