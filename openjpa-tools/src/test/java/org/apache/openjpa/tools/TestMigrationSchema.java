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
package org.apache.openjpa.tools;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import junit.framework.TestCase;

/**
 * Verifies the XML schema for the migration actions can distinguish between
 * valid and invalid document.
 * 
 * @author Pinaki Poddar
 * 
 */
public class TestMigrationSchema extends TestCase {
    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String ACTIONS_XSD = "META-INF/migration-actions.xsd";

    SAXParser parser;
    InputStream xsd;

    public void setUp() throws Exception {
        if (parser == null) {
            xsd = Thread.currentThread().getContextClassLoader().getResourceAsStream(ACTIONS_XSD);
            assertNotNull(ACTIONS_XSD + " not found", xsd);
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);

            parser = factory.newSAXParser();

            parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            parser.setProperty(JAXP_SCHEMA_SOURCE, xsd);
        }
        
    }

    public void testValidDocument() {
        String input = "META-INF/migration-actions.xml";
        InputStream is = Thread.currentThread().getContextClassLoader()
                               .getResourceAsStream(input);
        assertNotNull(input + " not found in classpath", is);
        try {
            SchemaErrorDetector detector = new SchemaErrorDetector(input, false); 
            parser.parse(is, detector);
            assertTrue(detector.toString(), !detector.hasErrors());
        } catch (SAXException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testInvalidDocument() {
        String input = "invalid-migration-actions.xml";
        InputStream is = Thread.currentThread().getContextClassLoader()
                               .getResourceAsStream(input);
        assertNotNull(input + " not found in classpath", is);
        try {
            SchemaErrorDetector detector = new SchemaErrorDetector(input, true); 
            parser.parse(is, detector);
            assertTrue(detector.hasErrors());
        } catch (SAXException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
