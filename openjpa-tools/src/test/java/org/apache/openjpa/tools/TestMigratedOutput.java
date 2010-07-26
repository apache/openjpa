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

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

public class TestMigratedOutput extends TestCase {
    static final String W3C_XML_SCHEMA       = "http://www.w3.org/2001/XMLSchema"; 
    static final String JAXP_SCHEMA_SOURCE   = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String ORM_XSD              = "META-INF/orm_2_0-xsd.rsrc";

    
    SAXParser       builder;
    InputStream     xsd; 
    
    public void setUp() throws Exception {
        xsd = Thread.currentThread().getContextClassLoader().getResourceAsStream(ORM_XSD);
        assertNotNull(ORM_XSD + " not found", xsd);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        
        builder = factory.newSAXParser();
        
        builder.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        builder.setProperty(JAXP_SCHEMA_SOURCE, xsd); 
    }
    
    public void tearDown() throws Exception {
        xsd.close();
    }
    
    
    public void testSampleIsJPACompliant() {
        migrateAndValidate("sample");
    }
    
    void migrateAndValidate(String type) {
        String outDir = "target/test-classes/";
        String input  = type + ".hbm.xml";
        String output = type + ".orm.xml";
        String[] args = {"-i", input, "-o", outDir + output};
        SchemaErrorDetector parserError = new SchemaErrorDetector(output, false);
        try {
            new MigrationTool().run(args);
            InputStream generated = Thread.currentThread().getContextClassLoader()
                                          .getResourceAsStream(output);
            assertNotNull(generated);
            builder.parse(generated, parserError);
            assertTrue(parserError.toString(), !parserError.hasErrors());
        } catch (Exception e) {
            parserError.print();
            e.printStackTrace();
            fail(e.toString());
        } 
    }
    
    
}
