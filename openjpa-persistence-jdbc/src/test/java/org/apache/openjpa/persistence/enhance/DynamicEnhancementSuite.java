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
package org.apache.openjpa.persistence.enhance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.persistence.Persistence;

import org.apache.openjpa.enhance.PCEnhancerAgent;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.persistence.annotations.TestAnnotationBasics;
import org.apache.openjpa.persistence.annotations.TestEmbeddedId;
import org.apache.openjpa.persistence.annotations.TestEnumerated;
import org.apache.openjpa.persistence.annotations.TestFlatInheritance;
import org.apache.openjpa.persistence.annotations.TestGenerators;
import org.apache.openjpa.persistence.annotations.TestJoinedInheritance;
import org.apache.openjpa.persistence.annotations.TestManyToMany;
import org.apache.openjpa.persistence.annotations.TestMapKey;
import org.apache.openjpa.persistence.annotations.TestMappedSuperClass;
import org.apache.openjpa.persistence.annotations.TestOneToOne;
import org.apache.openjpa.persistence.annotations.TestPersistentCollection;
import org.apache.openjpa.persistence.annotations.TestSerializedLobs;
import org.apache.openjpa.persistence.annotations.TestTablePerClassInheritance;
import org.apache.openjpa.persistence.datacache.TestQueryResultSize;
import org.apache.openjpa.persistence.detach.TestDetachNoCascade;
import org.apache.openjpa.persistence.detachment.
    TestGetReferenceAndImplicitDetachment;
import org.apache.openjpa.persistence.enhance.identity.
    TestMultipleLevelDerivedIdentity;
import org.apache.openjpa.persistence.enhance.identity.
    TestMultipleLevelDerivedIdentity1;
import org.apache.openjpa.persistence.identity.TestFloatingPointIds;
import org.apache.openjpa.persistence.identity.TestGenerationType;
import org.apache.openjpa.persistence.identity.TestSQLBigDecimalId;
import org.apache.openjpa.persistence.identity.TestSQLBigIntegerId;
import org.apache.openjpa.persistence.identity.TestSQLDateId;
import org.apache.openjpa.persistence.jdbc.annotations.TestEJBEmbedded;
import org.apache.openjpa.persistence.jdbc.annotations.TestEmbeddableSuperclass;
import org.apache.openjpa.persistence.jdbc.annotations.TestOneToMany;
import org.apache.openjpa.persistence.jdbc.annotations.TestVersion;
import org.apache.openjpa.persistence.jdbc.mapping.TestPrecisionMapping;
import org.apache.openjpa.persistence.jdbc.maps.m2mmapex2.TestMany2ManyMapEx2;
import org.apache.openjpa.persistence.jdbc.maps.m2mmapex6.TestMany2ManyMapEx6;
import org.apache.openjpa.persistence.jpql.clauses.TestEJBClauses;
import org.apache.openjpa.persistence.jpql.clauses.TestEJBDeleteUpdateImpl;
import org.apache.openjpa.persistence.jpql.expressions.TestEntityTypeExpression;
import org.apache.openjpa.persistence.kernel.TestExtents;
import org.apache.openjpa.persistence.kernel.TestProxies2;
import org.apache.openjpa.persistence.kernel.TestSavepoints;
import org.apache.openjpa.persistence.kernel.TestStateManagerImplData;
import org.apache.openjpa.persistence.kernel.TestStoreBlob;
import org.apache.openjpa.persistence.meta.TestMetamodel;
import org.apache.openjpa.persistence.query.TestComplexQueries;
import org.apache.openjpa.persistence.query.TestNamedQueries;
import org.apache.openjpa.persistence.query.TestQueryResults;
import org.apache.openjpa.persistence.relations.
    TestBulkUpdatesAndEmbeddedFields;
import org.apache.openjpa.persistence.relations.
    TestCascadingOneManyWithForeignKey;
import org.apache.openjpa.persistence.relations.TestChainEntities;
import org.apache.openjpa.persistence.relations.TestEagerBidiSQL;
import org.apache.openjpa.persistence.relations.TestHandlerCollections;
import org.apache.openjpa.persistence.relations.TestHandlerToHandlerMaps;
import org.apache.openjpa.persistence.relations.TestHandlerToRelationMaps;
import org.apache.openjpa.persistence.relations.TestIdOrderedOneMany;
import org.apache.openjpa.persistence.relations.TestInverseEagerSQL;
import org.apache.openjpa.persistence.relations.TestLRS;
import org.apache.openjpa.persistence.relations.TestLazyManyToOne;
import org.apache.openjpa.persistence.relations.TestManyEagerSQL;
import org.apache.openjpa.persistence.relations.TestManyOneAsId;
import org.apache.openjpa.persistence.relations.TestMapCollectionToBlob;
import org.apache.openjpa.persistence.relations.
    TestMultipleSameTypedEmbeddedWithEagerRelations;
import org.apache.openjpa.persistence.relations.TestOneOneNulls;
import org.apache.openjpa.persistence.relations.
    TestRelationFieldAsPrimaryKeyAndForeignKey;
import org.apache.openjpa.persistence.relations.TestRelationToHandlerMaps;
import org.apache.openjpa.persistence.relations.TestRelationToRelationMaps;
import org.apache.openjpa.persistence.relations.TestTargetedIFaceRelations;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.simple.TestBasicAnnotation;
import org.apache.openjpa.persistence.simple.TestCaseInsensitiveKeywordsInJPQL;
import org.apache.openjpa.persistence.simple.TestEntityManagerClear;
import org.apache.openjpa.persistence.simple.TestEntityManagerFactory;
import org.apache.openjpa.persistence.simple.TestEntityManagerMerge;
import org.apache.openjpa.persistence.simple.
    TestEntityManagerMethodsThrowAfterClose;
import org.apache.openjpa.persistence.simple.TestFlushBeforeDetach;
import org.apache.openjpa.persistence.simple.TestJoin;
import org.apache.openjpa.persistence.simple.TestPersistence;
import org.apache.openjpa.persistence.simple.TestPropertiesMethods;
import org.apache.openjpa.persistence.simple.TestRefresh;
import org.apache.openjpa.persistence.simple.TestSerializedFactory;
import org.apache.openjpa.persistence.simple.
    TestTableNamesDefaultToEntityNames;
import org.apache.openjpa.persistence.spring.TestLibService;
import org.apache.openjpa.persistence.xml.TestSimpleXmlEntity;
import org.apache.openjpa.persistence.xml.TestXmlOverrideEntity;

/**
 * TODO: this should be refactored to a @RunWith or similar...
 */
public class DynamicEnhancementSuite {
    static {
        Persistence.createEntityManagerFactory("test", System.getProperties());
    }
}
