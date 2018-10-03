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
package org.apache.openjpa.meta;

import java.io.Serializable;

/**
 * Describe metadata about an xml type.
 *
 * @author Catalina Wei
 * @since 1.0.0
 */
public interface XMLMetaData extends Serializable {
    /**
     * JAXB XML binding default name
     */
    String defaultName = "##default";
    int XMLTYPE = 0;
    int ELEMENT = 1;
    int ATTRIBUTE = 2;

    /**
     * Return true if mapping on an XmlRootElement.
     */
    boolean isXmlRootElement();

    /**
     * Return true if mapping on an XmlElement.
     */
    boolean isXmlElement();

    /**
     * Return true if mapping on an XmlAttribute.
     */
    boolean isXmlAttribute();

    /**
     * Return XMLMapping for a given field.
     * @param name the field name.
     * @return XMLMapping.
     */
    XMLMetaData getFieldMapping(String name);

    /**
     * Set type.
     */
    void setType(Class type);

    /**
     * Return type.
     */
    Class getType();

    /**
     * Return type code.
     */
    int getTypeCode();

    /**
     * Return the mapping name.
     */
    String getName();

    /**
     * Return xml element tag name or xml attribute name.
     */
    String getXmlname();

    /**
     * Return xml namespace.
     */
    String getXmlnamespace();

    /**
     * Set field name.
     * @param name the field name.
     */
    void setName(String name);

    /**
     * Set xml element or attribute name.
     * @param name the element name or attribute name
     */
    void setXmlname(String name);

    /**
     * Set namespace.
     */
    void setXmlnamespace(String namespace);

    /**
     * Set xmltype
     * @param type XMLTYPE, ELEMENT, or ATTRIBUTE
     */
    void setXmltype(int type);

    /**
     * Return xmltype
     * @return xmltype
     */
    int getXmltype();

    void setXmlRootElement(boolean isXmlRootElement);

    void addField(String name, XMLMetaData field);
}
