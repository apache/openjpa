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
package org.apache.openjpa.persistence.common.apps;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PartPK implements Serializable {
    /*Textile Id*/

    
    private static final long serialVersionUID = 1L;

    @Column(name="ID_TXE")
    private Integer textileId;

    /*Part Number*/
    @Column(name="NU_PT")
    private Integer partNumber;

    public PartPK() {
    }

    public Integer getTextileId() {
        return textileId;
    }

    public void setTextileId(Integer aTextileId) {
        textileId = aTextileId;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer aPartNumber) {
        partNumber = aPartNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PartPK other = (PartPK) obj;
        if (partNumber == null) {
            if (other.partNumber != null) {
                return false;
            }
        } else if (!partNumber.equals(other.partNumber)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((partNumber == null) ? 0 : partNumber.hashCode());
        return result;
    }
}
