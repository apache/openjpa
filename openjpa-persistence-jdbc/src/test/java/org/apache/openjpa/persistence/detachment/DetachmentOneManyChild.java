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
package org.apache.openjpa.persistence.detachment;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.apache.openjpa.persistence.jdbc.ForeignKey;

@Entity
public class DetachmentOneManyChild {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @ManyToOne(optional=false)
    @JoinColumn(name="PARENT_ID", nullable=false)
    @ForeignKey
    private DetachmentOneManyParent parent;

    @Version
    private Integer optLock;

    public long getId() { 
        return id; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public DetachmentOneManyParent getParent() { 
        return parent; 
    }

    public void setParent(DetachmentOneManyParent parent) { 
        this.parent = parent; 
    }
}
