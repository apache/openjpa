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
package org.apache.openjpa.persistence.callbacks;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
@EntityListeners({CDIListener.class, CDICacheableListener.class, NonCDIListener.class})
public class CDIListenedEntity {
	
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Integer value = null;
    
    private Integer cdiLoadedValue = null;
    
    private Integer cdiRandomInstanceValue = null;
    
    private Integer nonCDIRandomLoadedValue = null;
    
    private Integer nonCDIStaticLoadedValue = null;
    
    private Integer cacheableCDIListenerId = null;
    
    @Version
    private Integer version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
    
	public Integer getCdiLoadedValue() {
		return cdiLoadedValue;
	}

	public void setCdiLoadedValue(Integer cdiLoadedValue) {
		this.cdiLoadedValue = cdiLoadedValue;
	}

	public Integer getCdiRandomInstanceValue() {
		return cdiRandomInstanceValue;
	}

	public void setCdiRandomInstanceValue(Integer cdiRandomInstanceValue) {
		this.cdiRandomInstanceValue = cdiRandomInstanceValue;
	}

	public Integer getNonCDIRandomLoadedValue() {
		return nonCDIRandomLoadedValue;
	}

	public void setNonCDIRandomLoadedValue(Integer nonCDIRandomLoadedValue) {
		this.nonCDIRandomLoadedValue = nonCDIRandomLoadedValue;
	}

	public Integer getNonCDIStaticLoadedValue() {
		return nonCDIStaticLoadedValue;
	}

	public void setNonCDIStaticLoadedValue(Integer nonCDIStaticLoadedValue) {
		this.nonCDIStaticLoadedValue = nonCDIStaticLoadedValue;
	}
    
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public void setCacheableCDIListenerId(int code) {
		this.cacheableCDIListenerId = code;
	}
	
	public Integer getCacheableCDIListenerId() {
		return cacheableCDIListenerId;
	}

}
