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

import java.util.StringTokenizer;

public interface ManagedInterfaceSupAppId {

    int getId1();

    void setId1(int i);

    int getId2();

    void setId2(int i);

    int getIntFieldSup();

    void setIntFieldSup(int i);

    public static class Id implements java.io.Serializable {

        
        private static final long serialVersionUID = 1L;
        public int id1;
        public int id2;

        public Id() {
        }

        public Id(String str) {
            StringTokenizer tok = new StringTokenizer(str, ",");
            id1 = Integer.parseInt(tok.nextToken());
            id2 = Integer.parseInt(tok.nextToken());
        }

        @Override
        public String toString() {
            return id1 + "," + id2;
        }

        @Override
        public int hashCode() {
            return id1 + id2;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Id))
                return false;
            Id other = (Id) o;
            return id1 == other.id1 && id2 == other.id2;
        }
    }
}
