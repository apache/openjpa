/*
 * Copyright 2013 Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc.strategy;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;

import org.apache.openjpa.persistence.jdbc.strategy.MappedEntity.Key;

/**
 * Defines the aggregator side of the entity relation
 */
@Entity
public class MapperEntity {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @MapKeyColumn(name = "CODE")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Key, MappedEntity> values = new HashMap<>();

    public MappedEntity get(Key key) {
        if (!values.containsKey(key)) {
            values.put(key, new MappedEntity());
        }
        return values.get(key);
    }

    public void remove(Key key) {
        values.remove(key);
    }

}
