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
package
    org.apache.openjpa.persistence.annotations.common.apps.annotApp.annotype;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import jakarta.persistence.Version;


@Entity
@Table(name="annotypeentity_1")
@Inheritance(strategy=InheritanceType.JOINED)
@SqlResultSetMapping(name="NativeTestResult",
        entities=@EntityResult(entityClass=Entity1.class))
public class Entity1 implements Serializable
{
	private static final long serialVersionUID = 2882935803066041165L;

	@Id
	protected long pk;

	@Basic
	@Column(length=35)
	protected String stringField;

	@Basic
	protected int intField;

	@OneToOne(cascade={CascadeType.REMOVE, CascadeType.PERSIST})
	protected Entity2 entity2Field;

	@Version
	protected int versionField;

	public Entity1()
	{}

	public Entity1 (long pk, String stringField, int intField)
	{
		this.pk = pk;
		this.stringField = stringField;
		this.intField = intField;
	}


	public long getPk ()
	{
		return pk;
	}


	public void setStringField (String val)
	{
		stringField = val;
	}


	public String getStringField ()
	{
		return stringField;
	}


	public void setIntField (int val)
	{
		intField = val;
	}


	public int getIntField ()
	{
		return intField;
	}


	public void setEntity2Field (Entity2 val)
	{
		entity2Field = val;
	}


	public Entity2 getEntity2Field ()
	{
		return entity2Field;
	}

	@Override
    public String toString()
	{
        return("PK: " + pk + " StringField: " + stringField
                + " IntField: " + intField);
	}
}
