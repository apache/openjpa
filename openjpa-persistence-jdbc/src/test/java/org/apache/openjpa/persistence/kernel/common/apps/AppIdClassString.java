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

//import java.lang.annotation.Annotation;

@Entity
@Table(name = "APP_IDCS")
@IdClass(AppIdClassString.Idkey.class)
public class AppIdClassString {

    @Id
    private int pk;

    protected AppIdClassString() {
        this(1);
    }

    public AppIdClassString(int pk) {
        this.pk = pk;
    }

    public int getPk() {
        return pk;
    }

    public static class Idkey implements java.io.Serializable {

        
        private static final long serialVersionUID = 1L;
        public int pk;

        public Idkey() {
        }

        public Idkey(String pk) {
            if (pk != null)
                this.pk = Integer.parseInt(pk);
        }

        @Override
        public int hashCode() {
            return pk;
        }

        @Override
        public String toString() {
            return pk + "";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Id))
                return false;
            return pk == ((Idkey) o).pk;
        }
    }
}
