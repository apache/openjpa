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

import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

public class NameValidator
{
	@PrePersist
	public void validateName(NamedEntity obj)
	{
		System.out.println("NameValidator is running on "+obj);
		if(obj.getName().equals("") || obj.getName() == null)
			throw new NullPointerException();

		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("namevalidator");
	}

	@PostUpdate
	public void testpost(NamedEntity obj)
	{
		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("namevalidatorpou");
	}

	@PostRemove
	public void validateNothing(NamedEntity obj)
	{
		System.out.println("NameValidator is running on ");

		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("namevalidatorpor");
	}

	@PreRemove
	public void validateNothing1(NamedEntity obj)
	{
		System.out.println("NameValidator is running on ");

		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("namevalidatorprr");
	}
}
