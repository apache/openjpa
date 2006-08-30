/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.persistence.AnnotationPersistenceMetaDataParser;
import org.apache.openjpa.persistence.PersistenceMetaDataFactory;
import org.apache.openjpa.persistence.XMLPersistenceMetaDataParser;

/**
 * {@link MetaDataFactory} for JPA mapping information.
 *
 * @author Abe White
 * @since 0.4.0
 */
public class PersistenceMappingFactory
    extends PersistenceMetaDataFactory {

    @Override
    protected AnnotationPersistenceMetaDataParser newAnnotationParser() {
        AnnotationPersistenceMappingParser parser =
            new AnnotationPersistenceMappingParser((JDBCConfiguration)
                repos.getConfiguration());
        // strict mode means we're using a separate mapping parser, so if
        // we're adapting parse metadata hints
        if (strict)
            parser.setMappingOverride(((MappingRepository) repos).
                getStrategyInstaller().isAdapting());
        return parser;
    }

    @Override
    protected XMLPersistenceMetaDataParser newXMLParser(boolean loading) {
        XMLPersistenceMappingParser parser = new XMLPersistenceMappingParser
            ((JDBCConfiguration) repos.getConfiguration());
        // strict mode means we're using a separate mapping parser, so if
        // we're adapting parse metadata hints
        if (strict && loading)
            parser.setMappingOverride(((MappingRepository) repos).
                getStrategyInstaller().isAdapting());
        return parser;
    }
}
