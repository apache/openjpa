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
package org.apache.openjpa.enhance;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.apache.openjpa.persistence.Type;

@Entity
public class UnenhancedPropertyAccessSubclass
    extends UnenhancedPropertyAccess
    implements UnenhancedSubtype {

    
    private static final long serialVersionUID = 1L;
    private UnenhancedPropertyAccess related;
    private int intField;

    @Override
    @OneToOne(cascade = CascadeType.ALL)
    @Type(UnenhancedPropertyAccess.class)
    public UnenhancedType getRelated() {
        return related;
    }

    @Override
    public void setRelated(UnenhancedType related) {
        this.related = (UnenhancedPropertyAccess) related;
    }

    @Override
    public void setIntField(int i) {
        intField = i;
    }

    @Override
    public int getIntField() {
        return intField;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        UnenhancedPropertyAccessSubclass un =
            (UnenhancedPropertyAccessSubclass) super.clone();
        un.setRelated((UnenhancedType) getRelated().clone());
        return un;
    }
}
