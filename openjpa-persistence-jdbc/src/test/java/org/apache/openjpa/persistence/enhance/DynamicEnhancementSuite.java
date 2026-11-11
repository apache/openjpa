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

import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import jakarta.persistence.Persistence;

@Suite
@SuiteDisplayName("JUnit Platform Suite Demo")
@IncludeClassNamePatterns({
    "org.apache.openjpa.persistence.annotations.TestAnnotationBasics",
    "org.apache.openjpa.persistence.annotations.TestEmbeddedId",
    "org.apache.openjpa.persistence.annotations.TestEnumerated",
    "org.apache.openjpa.persistence.annotations.TestFlatInheritance",
    "org.apache.openjpa.persistence.annotations.TestGenerators",
    "org.apache.openjpa.persistence.annotations.TestJoinedInheritance",
    "org.apache.openjpa.persistence.annotations.TestManyToMany",
    "org.apache.openjpa.persistence.annotations.TestMapKey",
    "org.apache.openjpa.persistence.annotations.TestMappedSuperClass",
    "org.apache.openjpa.persistence.annotations.TestOneToOne",
    "org.apache.openjpa.persistence.annotations.TestPersistentCollection",
    "org.apache.openjpa.persistence.annotations.TestSerializedLobs",
    "org.apache.openjpa.persistence.annotations.TestTablePerClassInheritance",
    "org.apache.openjpa.persistence.datacache.TestDataCacheBehavesIdentical",
    "org.apache.openjpa.persistence.datacache.TestQueryResultSize",
    "org.apache.openjpa.persistence.detach.TestDetachNoCascade",
    "org.apache.openjpa.persistence.detachment.TestGetReferenceAndImplicitDetachment",
    "org.apache.openjpa.persistence.enhance.identity.TestMultipleLevelDerivedIdentity",
    "org.apache.openjpa.persistence.enhance.identity.TestMultipleLevelDerivedIdentity1",
    "org.apache.openjpa.persistence.identity.TestFloatingPointIds",
    "org.apache.openjpa.persistence.identity.TestGenerationType",
    "org.apache.openjpa.persistence.identity.TestSQLBigDecimalId",
    "org.apache.openjpa.persistence.identity.TestSQLBigIntegerId",
    "org.apache.openjpa.persistence.identity.TestSQLDateId",
    "org.apache.openjpa.persistence.jdbc.annotations.TestEJBEmbedded",
    "org.apache.openjpa.persistence.jdbc.annotations.TestEmbeddableSuperclass",
    "org.apache.openjpa.persistence.jdbc.annotations.TestOneToMany",
    "org.apache.openjpa.persistence.jdbc.annotations.TestVersion",
    "org.apache.openjpa.persistence.jdbc.mapping.TestPrecisionMapping",
    "org.apache.openjpa.persistence.jdbc.maps.m2mmapex2.TestMany2ManyMapEx2",
    "org.apache.openjpa.persistence.jdbc.maps.m2mmapex6.TestMany2ManyMapEx6",
    "org.apache.openjpa.persistence.jpql.clauses.TestEJBClauses",
    "org.apache.openjpa.persistence.jpql.clauses.TestEJBDeleteUpdateImpl",
    "org.apache.openjpa.persistence.jpql.expressions.TestEntityTypeExpression",
    "org.apache.openjpa.persistence.kernel.TestExtents",
    "org.apache.openjpa.persistence.kernel.TestProxies2",
    "org.apache.openjpa.persistence.kernel.TestSavepoints",
    "org.apache.openjpa.persistence.kernel.TestStateManagerImplData",
    "org.apache.openjpa.persistence.kernel.TestStoreBlob",
    "org.apache.openjpa.persistence.meta.TestMetamodel",
    "org.apache.openjpa.persistence.query.TestComplexQueries",
    "org.apache.openjpa.persistence.query.TestNamedQueries",
    "org.apache.openjpa.persistence.query.TestQueryResults",
    "org.apache.openjpa.persistence.relations.TestBulkUpdatesAndEmbeddedFields",
    "org.apache.openjpa.persistence.relations.TestCascadingOneManyWithForeignKey",
    "org.apache.openjpa.persistence.relations.TestChainEntities",
    "org.apache.openjpa.persistence.relations.TestEagerBidiSQL",
    "org.apache.openjpa.persistence.relations.TestHandlerCollections",
    "org.apache.openjpa.persistence.relations.TestHandlerToHandlerMaps",
    "org.apache.openjpa.persistence.relations.TestHandlerToRelationMaps",
    "org.apache.openjpa.persistence.relations.TestIdOrderedOneMany",
    "org.apache.openjpa.persistence.relations.TestInverseEagerSQL",
    "org.apache.openjpa.persistence.relations.TestLRS",
    "org.apache.openjpa.persistence.relations.TestLazyManyToOne",
    "org.apache.openjpa.persistence.relations.TestManyEagerSQL",
    "org.apache.openjpa.persistence.relations.TestManyOneAsId",
    "org.apache.openjpa.persistence.relations.TestMapCollectionToBlob",
    "org.apache.openjpa.persistence.relations.TestMultipleSameTypedEmbeddedWithEagerRelations",
    "org.apache.openjpa.persistence.relations.TestOneOneNulls",
    "org.apache.openjpa.persistence.relations.TestRelationFieldAsPrimaryKeyAndForeignKey",
    "org.apache.openjpa.persistence.relations.TestRelationToHandlerMaps",
    "org.apache.openjpa.persistence.relations.TestRelationToRelationMaps",
    "org.apache.openjpa.persistence.relations.TestTargetedIFaceRelations",
    "org.apache.openjpa.persistence.simple.TestBasicAnnotation",
    "org.apache.openjpa.persistence.simple.TestCaseInsensitiveKeywordsInJPQL",
    "org.apache.openjpa.persistence.simple.TestEntityManagerClear",
    "org.apache.openjpa.persistence.simple.TestEntityManagerFactory",
    "org.apache.openjpa.persistence.simple.TestEntityManagerMerge",
    "org.apache.openjpa.persistence.simple.TestEntityManagerMethodsThrowAfterClose",
    "org.apache.openjpa.persistence.simple.TestFlushBeforeDetach",
    "org.apache.openjpa.persistence.simple.TestJoin",
    "org.apache.openjpa.persistence.simple.TestPersistence",
    "org.apache.openjpa.persistence.simple.TestPropertiesMethods",
    "org.apache.openjpa.persistence.simple.TestRefresh",
    "org.apache.openjpa.persistence.simple.TestSerializedFactory",
    "org.apache.openjpa.persistence.simple.TestTableNamesDefaultToEntityNames",
    "org.apache.openjpa.persistence.spring.TestLibService",
    "org.apache.openjpa.persistence.xml.TestSimpleXmlEntity",
    "org.apache.openjpa.persistence.xml.TestXmlOverrideEntity"
})
public class DynamicEnhancementSuite {
    @BeforeSuite
    static void beforeSuite() {
       Persistence.createEntityManagerFactory("test", System.getProperties());
    }
}
