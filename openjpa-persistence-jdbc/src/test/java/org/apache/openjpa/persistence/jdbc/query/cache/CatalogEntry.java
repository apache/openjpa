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

package org.apache.openjpa.persistence.jdbc.query.cache;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class CatalogEntry {
	@Id
	private long catalogEntryId;
	
	private String partNumber;
	
	private int quantity = 50;
	
	@OneToMany(targetEntity=CatalogEntryDescription.class, 
			mappedBy="CatalogEntryForCatalogEntryDescription", cascade=CascadeType.MERGE)
	private List CatalogEntryDescription = new ArrayList();

	public long getCatalogEntryId() {
		return catalogEntryId;
	}

	public void setCatalogEntryId(long catalogEntryId) {
		this.catalogEntryId = catalogEntryId;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	
	public List getCatalogEntryDescription() {
		return CatalogEntryDescription;
	}

	public void setCatalogEntryDescription(List catalogEntryDescription) {
		CatalogEntryDescription = catalogEntryDescription;
	}

	public CatalogEntry() {
	}

	/**
	 * @param catalogEntryId
	 * @param partNumber
	 */
	public CatalogEntry(long catalogEntryId, String partNumber) {
		super();
		this.catalogEntryId = catalogEntryId;
		this.partNumber = partNumber;
	}
}
