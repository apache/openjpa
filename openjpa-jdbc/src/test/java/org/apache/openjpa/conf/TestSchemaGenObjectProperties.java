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
package org.apache.openjpa.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Schema generation script targets and sources may be given as {@link Writer}
 * and {@link Reader} objects. Those are picked out of the properties before the
 * generic string handling sees them, which must not disturb the caller's map.
 */
public class TestSchemaGenObjectProperties {

    private static final String CREATE_TARGET =
        "jakarta.persistence.schema-generation.scripts.create-target";
    private static final String DROP_TARGET =
        "jakarta.persistence.schema-generation.scripts.drop-target";
    private static final String CREATE_SOURCE =
        "jakarta.persistence.schema-generation.create-script-source";
    private static final String DROP_SOURCE =
        "jakarta.persistence.schema-generation.drop-script-source";

    private Map<String, Object> allFour() {
        Map<String, Object> props = new HashMap<>();
        props.put(CREATE_TARGET, new StringWriter());
        props.put(DROP_TARGET, new StringWriter());
        props.put(CREATE_SOURCE, new StringReader(""));
        props.put(DROP_SOURCE, new StringReader(""));
        return props;
    }

    @Test
    public void testObjectsAreCaptured() {
        Map<String, Object> props = allFour();
        OpenJPAConfigurationImpl conf = new OpenJPAConfigurationImpl();
        conf.fromProperties(props);

        assertSame(props.get(CREATE_TARGET), conf.getCreateScriptTargetWriter());
        assertSame(props.get(DROP_TARGET), conf.getDropScriptTargetWriter());
        assertSame(props.get(CREATE_SOURCE), conf.getCreateScriptSourceReader());
        assertSame(props.get(DROP_SOURCE), conf.getDropScriptSourceReader());
        assertTrue(conf.isSchemaGenerationExplicit());
    }

    @Test
    public void testCallersMapIsNotModified() {
        Map<String, Object> props = allFour();
        Map<String, Object> before = new HashMap<>(props);

        new OpenJPAConfigurationImpl().fromProperties(props);

        assertEquals(before, props);
    }

    @Test
    public void testUnmodifiableMapIsAccepted() {
        Map<String, Object> props = Map.copyOf(allFour());
        OpenJPAConfigurationImpl conf = new OpenJPAConfigurationImpl();

        conf.fromProperties(props);

        assertSame(props.get(CREATE_TARGET), conf.getCreateScriptTargetWriter());
        assertSame(props.get(DROP_SOURCE), conf.getDropScriptSourceReader());
    }

    @Test
    public void testStringValuesStillWork() {
        Map<String, Object> props = new HashMap<>();
        props.put(CREATE_TARGET, "create.sql");
        OpenJPAConfigurationImpl conf = new OpenJPAConfigurationImpl();

        conf.fromProperties(props);

        assertEquals("create.sql", conf.getCreateScriptTarget());
        assertEquals(null, conf.getCreateScriptTargetWriter());
        assertTrue(props.containsKey(CREATE_TARGET));
    }
}
