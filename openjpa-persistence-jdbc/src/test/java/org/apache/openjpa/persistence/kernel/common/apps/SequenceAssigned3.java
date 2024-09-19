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
package org.apache.openjpa.persistence.kernel.common.apps;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "seqAss3")
@IdClass(SequenceAssigned3.seq3Id.class)
public class SequenceAssigned3 {

    @Id
    private Long pk;

    public SequenceAssigned3() {
    }

    public SequenceAssigned3(Long pk) {
        this.pk = pk;
    }

    public void setPK(Long l) {
        pk = l;
    }

    public Long getPK() {
        return pk;
    }

    public static class seq3Id implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public Long pk;

        public seq3Id() {
        }

        public seq3Id(String str) {
            pk = Long.valueOf(str);
        }

        @Override
        public int hashCode() {
            return (int) (pk == null ? 0 : pk
                % Integer.MAX_VALUE);
        }

        @Override
        public String toString() {
            return pk + "";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof seq3Id))
                return false;
            return pk == ((seq3Id) o).pk;
        }
    }
}
