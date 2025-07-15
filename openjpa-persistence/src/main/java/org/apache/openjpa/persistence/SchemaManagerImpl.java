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
package org.apache.openjpa.persistence;

import org.apache.openjpa.kernel.BrokerFactory;

import jakarta.persistence.SchemaManager;
import jakarta.persistence.SchemaValidationException;

/**
 * Implements a no-op SchemaManager object that will throw 
 * UnsupportedOperationException if not concretelly implemented
 * by the given persistence layer.
 * 
 * @author Paulo Cristovão Filho
 */
public class SchemaManagerImpl implements SchemaManager {
	
	private BrokerFactory _factory;
	
	public SchemaManagerImpl(BrokerFactory factory) {
		_factory = factory;
	}

	@Override
	public void create(boolean createSchemas) {
		_factory.createPersistenceStructure(createSchemas);
	}

	@Override
	public void drop(boolean dropSchemas) {
		_factory.dropPersistenceStrucuture(dropSchemas);
	}

	@Override
	public void validate() throws SchemaValidationException {
		try {
			_factory.validatePersistenceStruture();
		} catch (Exception ex) {
			throw new SchemaValidationException(
					String.format("Schema could not be validated: %s", ex.getLocalizedMessage()), 
					(Exception) ex);
		}
	}

	@Override
	public void truncate() {
		_factory.truncateData();;
	}

}
