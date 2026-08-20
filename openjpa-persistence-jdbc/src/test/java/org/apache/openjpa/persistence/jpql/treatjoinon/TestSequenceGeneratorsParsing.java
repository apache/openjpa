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

import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test that @SequenceGenerators container annotation is properly parsed.
 * Corresponds to TCK sequenceGeneratorTest.
 */
public class TestSequenceGeneratorsParsing extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(SeqGenEntity.class, CLEAR_TABLES);
    }

    /**
     * Test that a sequence generator defined via @SequenceGenerators
     * is properly recognized by the metadata repository.
     */
    public void testSequenceGeneratorsAnnotation() {
        MetaDataRepository repos = emf.getConfiguration()
            .getMetaDataRepositoryInstance();
        // Ensure the class metadata is resolved (which triggers annotation parsing)
        assertNotNull(repos.getMetaData(SeqGenEntity.class, null, true));
        // Check that the sequence metadata was registered during parsing.
        // Use getCachedSequenceMetaData to avoid instantiation (Derby
        // doesn't support native sequences).
        SequenceMetaData seqMeta = repos.getCachedSequenceMetaData(
            "TEST_SEQ_GEN");
        assertNotNull("@SequenceGenerators should be parsed and "
            + "sequence metadata should be available", seqMeta);
    }
}
