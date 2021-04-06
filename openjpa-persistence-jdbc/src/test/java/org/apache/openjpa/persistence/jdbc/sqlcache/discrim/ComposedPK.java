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
package org.apache.openjpa.persistence.jdbc.sqlcache.discrim;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class ComposedPK implements Serializable {

    private static final long serialVersionUID = -7415701271873221026L;

    private Short field1;

    private Integer field2;

    public ComposedPK(){}

    public Short getField1() {
        return field1;
    }

    public void setField1(Short field1) {
        this.field1 = field1;
    }

    public Integer getField2() {
        return field2;
    }

    public void setField2(Integer field2) {
        this.field2 = field2;
    }

    public ComposedPK(Short field1, Integer field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field1 == null) ? 0 : field1.hashCode());
        result = prime * result + ((field2 == null) ? 0 : field2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ComposedPK other = (ComposedPK) obj;
        if (field1 == null) {
            if (other.field1 != null)
                return false;
        } else if (!field1.equals(other.field1))
            return false;
        if (field2 == null) {
            if (other.field2 != null)
                return false;
        } else if (!field2.equals(other.field2))
            return false;
        return true;
    }

}
