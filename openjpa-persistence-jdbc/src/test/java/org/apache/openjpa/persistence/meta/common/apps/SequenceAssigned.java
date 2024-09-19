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
package org.apache.openjpa.persistence.meta.common.apps;

import jakarta.persistence.Entity;

@Entity
public class SequenceAssigned {

    private long pk;
    private SequenceAssigned other;

    public void setPK(long l) {
        pk = l;
    }

    public long getPK() {
        return pk;
    }

    public void setOther(SequenceAssigned other) {
        this.other = other;
    }

    public SequenceAssigned getOther() {
        return other;
    }

    public static class Id implements java.io.Serializable {

        
        private static final long serialVersionUID = 1L;
        public long pk;

        public Id() {
        }

        public Id(String str) {
            pk = Long.parseLong(str);
        }

        @Override
        public int hashCode() {
            return (int) (pk % (long) Integer.MAX_VALUE);
        }

        @Override
        public String toString() {
            return pk + "";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Id))
                return false;
            return pk == ((Id) o).pk;
        }
    }
}
