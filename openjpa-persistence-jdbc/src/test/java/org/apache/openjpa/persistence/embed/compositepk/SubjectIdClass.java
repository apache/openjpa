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
package org.apache.openjpa.persistence.embed.compositepk;

import java.io.Serializable;

public class SubjectIdClass implements Serializable {

    
    private static final long serialVersionUID = 1L;

    private Integer subjectNummer;

    private String subjectTypeCode;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((subjectNummer == null) ? 0 : subjectNummer.hashCode());
        result = prime * result + ((subjectTypeCode == null) ? 0 : subjectTypeCode.hashCode());
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
        SubjectIdClass other = (SubjectIdClass) obj;
        if (subjectNummer == null) {
            if (other.subjectNummer != null)
                return false;
        } else if (!subjectNummer.equals(other.subjectNummer))
            return false;
        if (subjectTypeCode == null) {
            if (other.subjectTypeCode != null)
                return false;
        } else if (!subjectTypeCode.equals(other.subjectTypeCode))
            return false;
        return true;
    }
}
