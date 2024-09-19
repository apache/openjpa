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

import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;

public class StringValidator
{
	@PrePersist
	public void prePersist(FlightSchedule sched)
	{
		if(sched.getName().length() == 0 || sched.getName().equals(""))
            throw new IllegalArgumentException("Needs a valid name");
	}

	@PostPersist
	public void postPersist(FlightSchedule sched)
	{
        System.out.println("Schedule " + sched
                + " is successfully persisted: StringValidator.class");
	}
}
