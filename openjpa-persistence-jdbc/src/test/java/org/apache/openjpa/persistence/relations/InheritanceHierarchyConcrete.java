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
package org.apache.openjpa.persistence.relations;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.apache.openjpa.persistence.FetchAttribute;
import org.apache.openjpa.persistence.FetchGroup;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorValue("concrete")
@FetchGroup(name="value", attributes={@FetchAttribute(name="value")})
public class InheritanceHierarchyConcrete extends InheritanceHierarchyAbstract implements Serializable {

	
    private static final long serialVersionUID = 1L;
    @Basic
	private int value;

	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}

}

