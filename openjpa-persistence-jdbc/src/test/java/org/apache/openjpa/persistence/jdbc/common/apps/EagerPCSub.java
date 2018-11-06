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

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *	<p>Persistent type used in testing.</p>
 *
 *	@author		Abe White
 */
@Entity
@Table(name="EAGERPCSUB")
public class EagerPCSub	extends EagerPC
{
	private int 	intField;

	@Column(name="eagercoll2")
	@OneToMany
	@Transient private List eagerCollection2 = new LinkedList ();

	public EagerPCSub()
	{
	}

	public EagerPCSub(int id)
	{
		super(id);
	}

	public int getIntField ()
	{
		return this.intField;
	}

	public void setIntField (int intField)
	{
		this.intField = intField;
	}

	public List getEagerCollection2 ()
	{
		return this.eagerCollection2;
	}

	public void setEagerCollection2 (List eagerCollection2)
	{
		this.eagerCollection2 = eagerCollection2;
	}
}
