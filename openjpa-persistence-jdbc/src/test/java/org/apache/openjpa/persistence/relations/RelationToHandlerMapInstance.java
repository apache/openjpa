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
package org.apache.openjpa.persistence.relations;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.apache.openjpa.persistence.PersistentMap;
import org.apache.openjpa.persistence.simple.AllFieldTypes;

@Entity
public class RelationToHandlerMapInstance {
    @Id
    private int id;

    @PersistentMap(keyCascade = CascadeType.PERSIST)

    private Map<AllFieldTypes,String> aftMap =
        new HashMap<>();

    public Map<AllFieldTypes,String> getMap() {
        return aftMap;
    }
}
