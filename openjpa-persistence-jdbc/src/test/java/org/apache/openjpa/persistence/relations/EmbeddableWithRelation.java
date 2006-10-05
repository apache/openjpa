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

import javax.persistence.Embeddable;
import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;

@Embeddable
public class EmbeddableWithRelation {

    private String name;

    @ManyToOne(cascade=CascadeType.ALL)
    private MultipleSameTypedEmbedded rel;

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public MultipleSameTypedEmbedded getRel() { 
        return rel; 
    }

    public void setRel(MultipleSameTypedEmbedded rel) { 
        this.rel = rel; 
    }
}
