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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Hand-written canonical metamodel matching expected JPA 3.2 output
 * from AnnotationProcessor6, including EntityType and query constants.
 */
@StaticMetamodel(MetamodelConstantsEntity.class)
public class MetamodelConstantsEntity_ {
    public static volatile SingularAttribute<MetamodelConstantsEntity, Long> id;
    public static volatile SingularAttribute<MetamodelConstantsEntity, String> name;

    // JPA 3.2: EntityType constant
    public static volatile EntityType<MetamodelConstantsEntity> class_;

    // JPA 3.2: Named query constants
    public static final String QUERY_METAMODELCONSTANTSENTITY_FINDALL =
        "MetamodelConstantsEntity.findAll";
    public static final String QUERY_METAMODELCONSTANTSENTITY_FINDBYNAME =
        "MetamodelConstantsEntity.findByName";
    public static final String QUERY_METAMODELCONSTANTSENTITY_FINDNATIVE =
        "MetamodelConstantsEntity.findNative";

    // JPA 3.2: Result set mapping constants
    public static final String MAPPING_METAMODELCONSTANTSENTITY_RESULTMAPPING =
        "MetamodelConstantsEntity.resultMapping";
}
