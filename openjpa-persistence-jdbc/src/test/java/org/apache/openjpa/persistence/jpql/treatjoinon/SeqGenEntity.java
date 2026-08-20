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
package org.apache.openjpa.persistence.jpql.treatjoinon;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.Table;

/**
 * Entity using @SequenceGenerators container annotation.
 * Tests that the parser correctly handles this JPA 3.1+ feature.
 * Note: @GeneratedValue is NOT used here because Derby doesn't
 * support native sequences - we only test metadata parsing.
 */
@Entity
@Table(name = "SEQGEN_ENTITY")
@SequenceGenerators({
    @SequenceGenerator(name = "TEST_SEQ_GEN", allocationSize = 1, initialValue = 10)
})
public class SeqGenEntity {
    @Id
    private int id;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
