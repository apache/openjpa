/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.relations;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

@Entity
@IdClass(ManyOneCompoundIdOwnerId.class)
public class ManyOneCompoundIdOwner {

    @Id
    @GeneratedValue
    private long longId;

    @Id
    @ManyToOne
    private BasicEntity entityId;

    private String name;

    @ManyToOne
    private ManyOneCompoundIdOwner selfRel;

    @Version
    private Integer optLock;

    public long getLongId() {
        return longId;
    }

    public BasicEntity getEntityId() { 
        return entityId; 
    }

    public void setEntityId(BasicEntity entityId) { 
        this.entityId = entityId; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public ManyOneCompoundIdOwner getSelfRel() { 
        return selfRel; 
    }

    public void setSelfRel(ManyOneCompoundIdOwner selfRel) { 
        this.selfRel = selfRel; 
    }
}
