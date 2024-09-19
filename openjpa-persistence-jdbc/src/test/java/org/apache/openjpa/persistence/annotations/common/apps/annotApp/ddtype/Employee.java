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
package org.apache.openjpa.persistence.annotations.common.apps.annotApp.ddtype;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Transient;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@EntityListeners({NameValidator.class, LongNameValidator.class})
public class Employee implements NamedEntity
{
	@Id
	private int id;

	@Basic
	protected String name;

	@GeneratedValue(strategy=GenerationType.AUTO)
	protected int lifecheck;

	@Transient
	protected long syncTime;

	public Employee()
	{}

	public Employee(int id, String name)
	{
		this.id = id;
		this.name = name;
	}

	@Override
    public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return id;
	}

	@PostPersist
	private void resetSyncTime()
	{
        System.out.println("resetSyncTime is running on " + this + "GEN #: "
		        + lifecheck);
		syncTime = System.currentTimeMillis();

		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("employeepop");
		store.getClist().add("gen#"+lifecheck);
	}

	@PostLoad
	public void pload()
	{
		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("employeepol");
	}

	@PostUpdate
	public void pupdate()
	{
		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("employeepou");
	}

	@Override
    public String toString()
	{
        return "Name: " + name + " of " + this.getClass().getName()
            + " Id: " + id + " Synctime: " + syncTime;
	}

	public int getCheck() {
		return this.lifecheck;
	}

	public void setCheck(int check) {
		this.lifecheck = check;
	}
}
