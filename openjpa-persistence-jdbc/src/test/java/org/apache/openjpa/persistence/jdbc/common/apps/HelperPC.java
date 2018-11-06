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

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 *	<p>Persistent type used in testing.</p>
 *
 *	@author		Abe White
 */
@Entity
@Table(name="HELPERPC")
public class HelperPC implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Column(length=50, name="strngfld")
	private String 		stringField;

	private HelperPC 	helper;

	@OneToOne(cascade={CascadeType.PERSIST, CascadeType.REMOVE})
	private EagerPC		eager;

	@Id
	private int id;

	public HelperPC()
	{}

	public HelperPC(int id)
	{
		this.id = id;
	}

	public String getStringField ()
	{
		return this.stringField;
	}

	public void setStringField (String stringField)
	{
		this.stringField = stringField;
	}

	public HelperPC getHelper ()
	{
		return this.helper;
	}

	public void setHelper (HelperPC helper)
	{
		this.helper = helper;
	}

	public EagerPC getEager ()
	{
		return this.eager;
	}

	public void setEager (EagerPC eager)
	{
		this.eager = eager;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
