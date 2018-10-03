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

import java.util.HashMap;
import java.util.Objects;

public class XMLClassMetaData implements XMLMetaData
{
    
    private static final long serialVersionUID = 1L;
    private Class _type;
    private int _code = JavaTypes.OBJECT;
    private int _xmltype = XMLTYPE;
    private String _name = null;
    private String _xmlname = null;
    private String _xmlnamespace = null;
    private boolean _isXMLRootElement = false;
    private HashMap _fieldMap = new HashMap();

    /**
     * Constructor.
     *
     * @param type the class that contains XmlType annotation.
     * @param name  the persistent field name that maps to xml column
     */
    public XMLClassMetaData(Class type, String name) {
        _type = type;
        _name = name;
    }

    /**
     * Constructor.
     *
     * @param type the class that contains XmlType annotation.
     */
    public XMLClassMetaData(Class type) {
        _type = type;
    }

    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void setXmlname(String name) {
        _xmlname = name;
    }

    @Override
    public String getXmlname() {
        return _isXMLRootElement ? null : _xmlname;
    }

    @Override
    public void setXmlnamespace(String name) {
        // avoid JAXB XML bind default name
        if (!Objects.equals(defaultName, name))
            _xmlnamespace = name;
    }

    @Override
    public String getXmlnamespace() {
        return _xmlnamespace;
    }

    @Override
    public void setXmlRootElement(boolean isXMLRootElement) {
        _isXMLRootElement = isXMLRootElement;
    }

    @Override
    public boolean isXmlRootElement() {
        return _isXMLRootElement;
    }

    @Override
    public boolean isXmlElement() {
        return false;
    }

    @Override
    public boolean isXmlAttribute() {
        return false;
    }

    @Override
    public XMLMetaData getFieldMapping(String name) {
        return (XMLMetaData) _fieldMap.get(name);
    }

    @Override
    public void setType(Class type) {
        _type = type;
    }

    @Override
    public Class getType() {
        return _type;
    }

    @Override
    public int getTypeCode() {
        return _code;
    }

    @Override
    public void setXmltype(int type) {
        _xmltype = type;
    }

    @Override
    public int getXmltype() {
        return _xmltype;
    }

    @Override
    public void addField(String name, XMLMetaData field) {
        _fieldMap.put(name, field);
    }
}
