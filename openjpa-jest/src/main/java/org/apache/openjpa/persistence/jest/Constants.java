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

package org.apache.openjpa.persistence.jest;


/**
 * Static String constants
 *
 * @author Pinaki Poddar
 *
 */
public interface Constants {
    /**
     * Common Command Qualifiers
     */
    String QUALIFIER_FORMAT      = "format";
    String QUALIFIER_PLAN        = "plan";

    /**
     * Mime Types
     */
    String MIME_TYPE_PLAIN = "text/plain";
    String MIME_TYPE_JS    = "text/javascript";
    String MIME_TYPE_CSS   = "text/css";
    String MIME_TYPE_XML   = "text/xml";
    String MIME_TYPE_JSON  = "application/json";


    /**
     * Dojo Toolkit URL and Themes
     */
    String DOJO_BASE_URL = "http://ajax.googleapis.com/ajax/libs/dojo/1.5";
    String DOJO_THEME    = "claro";



    /**
     * Root element of XML instances. Must match the name defined in <A href="jest-instance.xsd>jest-instance.xsd</A>.
     */
    String ROOT_ELEMENT_INSTANCE   = "instances";
    String ATTR_ID      = "id";
    String ATTR_REL     = "rel";
    String ATTR_SRC     = "src";
    String ATTR_TYPE    = "type";
    String ATTR_NAME    = "name";
    String ATTR_VERSION = "version";
    String ATTR_CLASS   = "class";
    String ATTR_HREF    = "href";
    String ATTR_STYLE   = "style";
    String ATTR_NULL           = "null";
    String ATTR_MEMBER_TYPE    = "member-type";
    String ATTR_KEY_TYPE       = "key-type";
    String ATTR_VALUE_TYPE     = "value-type";

    /**
     * Elements and attributes in properties XML.
     */
    String ROOT_ELEMENT_PROPERTIES = "properties";
    String ELEMENT_PROPERTY     = "property";
    String ATTR_PROPERTY_KEY     = "name";
    String ATTR_PROPERTY_VALUE     = "value";


    String ROOT_ELEMENT_ERROR      = "error";
    String ELEMENT_ERROR_HEADER    = "error-header";
    String ELEMENT_ERROR_MESSAGE   = "error-message";
    String ELEMENT_ERROR_TRACE     = "stacktrace";

    /**
     * Root element of XML meta-model. Must match the name defined in <A href="jest-model.xsd>jest-model.xsd</A>.
     */
    String ROOT_ELEMENT_MODEL      = "metamodel";

    String ELEMENT_INSTANCE      = "instance";
    String ELEMENT_URI           = "uri";
    String ELEMENT_DESCRIPTION   = "description";
    String ELEMENT_REF           = "ref";
    String ELEMENT_NULL_REF      = "null";
    String ELEMENT_MEMBER        = "member";
    String ELEMENT_ENTRY         = "entry";
    String ELEMENT_ENTRY_KEY     = "key";
    String ELEMENT_ENTRY_VALUE   = "value";



    /**
     * JEST resources
     */
    String JEST_INSTANCE_XSD       = "jest-instance.xsd";

    String JAXP_SCHEMA_SOURCE      = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    String JAXP_SCHEMA_LANGUAGE    = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    String NULL_VALUE          = "null";


}
