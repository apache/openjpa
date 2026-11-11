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
package org.apache.openjpa.persistence.jdbc.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FetchGroup;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.UpdateStrategies;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>Test the parsing of Kodo metadata extension annotations.</p>
 *
 * @author Abe White
 */
class TestExtensionAnnotations
    extends SingleEMFTestCase {

    private ClassMapping _mapping;

    @BeforeEach
    public void setUp() {
        setUp(ExtensionsEntity.class);
        _mapping = ((JDBCConfiguration) emf.getConfiguration()).
            getMappingRepositoryInstance().getMapping(ExtensionsEntity.class,
            null, true);
    }

    @Test
    void dataStoreId() {
        assertEquals(ClassMetaData.ID_DATASTORE, _mapping.getIdentityType());
        assertEquals(ValueStrategies.SEQUENCE, _mapping.getIdentityStrategy());
        assertEquals("system", _mapping.getIdentitySequenceName());
    }

    @Test
    void dataCache() {
        assertNull(_mapping.getDataCacheName());
    }

    @Test
    void fetchGroups() {
        FetchGroup fg = _mapping.getFetchGroup("detail");
        assertNotNull(fg);
        assertFalse(fg.isPostLoad());
        FieldMapping fm = _mapping.getFieldMapping("rel");
        assertEquals(1, fm.getCustomFetchGroups().length);
        assertEquals("detail", fm.getCustomFetchGroups()[0]);
        assertEquals(-1, fg.getRecursionDepth(fm));
        fm = _mapping.getFieldMapping("seq");
        assertEquals(1, fm.getCustomFetchGroups().length);
        assertEquals("detail", fm.getCustomFetchGroups()[0]);
        assertEquals(1, fg.getRecursionDepth(fm));
    }

    @Test
    void valueStrategy() {
        assertEquals(ValueStrategies.UUID_HEX,
            _mapping.getField("uuid").getValueStrategy());
        assertEquals(ValueStrategies.UUID_STRING,
            _mapping.getField("uuidString").getValueStrategy());
        assertEquals(ValueStrategies.UUID_TYPE4_HEX,
            _mapping.getField("uuidT4Hex").getValueStrategy());
        assertEquals(ValueStrategies.UUID_TYPE4_STRING,
            _mapping.getField("uuidT4String").getValueStrategy());
        FieldMapping seq = _mapping.getFieldMapping("seq");
        assertEquals(ValueStrategies.SEQUENCE, seq.getValueStrategy());
        assertEquals("system", seq.getValueSequenceName());
    }

    @Test
    void readOnly() {
        assertEquals(UpdateStrategies.RESTRICT,
            _mapping.getField("seq").getUpdateStrategy());
    }

    @Test
    void inverseLogical() {
        assertEquals("owner", _mapping.getField("rel").getInverse());
        assertNull(_mapping.getField("owner").getInverse());
    }

    @Test
    void dependent() {
        assertEquals(ValueMetaData.CASCADE_AUTO,
            _mapping.getField("rel").getCascadeDelete());
        assertEquals(ValueMetaData.CASCADE_NONE,
            _mapping.getField("eager").getCascadeDelete());
        assertEquals(ValueMetaData.CASCADE_AUTO,
            _mapping.getField("eager").getElement().getCascadeDelete());
    }

    @Test
    void lrs() {
        assertTrue(_mapping.getField("lrs").isLRS());
        assertFalse(_mapping.getField("eager").isLRS());
    }

    @Test
    void classCriteria() {
        assertTrue(_mapping.getFieldMapping("eager").getElementMapping().
            getUseClassCriteria());
        assertFalse(_mapping.getFieldMapping("eager").getUseClassCriteria());
        assertFalse(_mapping.getFieldMapping("lrs").getElementMapping().
            getUseClassCriteria());
    }

    @Test
    void externalValues() {
        FieldMapping externalValues = _mapping.getFieldMapping
            ("externalValues");
        assertEquals(JavaTypes.CHAR, externalValues.getDeclaredTypeCode());
        assertEquals(JavaTypes.INT, externalValues.getTypeCode());
        assertEquals(1, externalValues.getExternalValueMap().
            get('M'));
        assertEquals(2, externalValues.getExternalValueMap().
            get('F'));
    }

    @Test
    void externalizer() {
        FieldMapping externalizer = _mapping.getFieldMapping("externalizer");
        assertEquals(JavaTypes.OBJECT, externalizer.getDeclaredTypeCode());
        assertEquals(JavaTypes.STRING, externalizer.getTypeCode());
        assertEquals("java.lang.String", externalizer.getExternalValue
            (String.class, null));
        assertEquals(String.class, externalizer.getFieldValue
            (String.class.getName(), null));
    }
}
