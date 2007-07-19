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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.openjpa.jdbc.meta.XMLMappingRepository;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.XMLMapping;
import org.apache.openjpa.meta.XMLMetaData;
import org.apache.commons.lang.StringUtils;

/**
 * Contains metadata about a persistent field that maps to an xml column.
 * This metadata is loaded at runtime when query involves predicates
 * that navigate through xpath.
 *
 * @author Catalina Wei
 * @since 1.0.0
 */
public class XMLClassMetaData implements XMLMapping     
{
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
     * @name  the persistent field name that maps to xml column
     * @param repo the meta repository.
     */
    public XMLClassMetaData(Class type, String name, XMLMappingRepository repos) {
       _type = type;
       _isXMLRootElement = _type.getAnnotation(XmlRootElement.class) != null;
       if (_isXMLRootElement) {
           _xmlname = ((XmlRootElement) _type.getAnnotation
                   (XmlRootElement.class)).name();
           _xmlnamespace = ((XmlRootElement) _type.getAnnotation
                   (XmlRootElement.class)).namespace();
       }
       else {
           _xmlname = ((XmlType) _type.getAnnotation
                   (XmlType.class)).name();
           _xmlnamespace = ((XmlType) _type.getAnnotation
                   (XmlType.class)).namespace();
           _name = name;
       }
       populateFromReflection(_type, repos);
    }
    
    /**
     * Constructor. Supply described type and repository.
     * 
     * @param type the class that contains XmlType annotation.
     * @param repo the meta repository.
     */
    protected XMLClassMetaData(Class type, XMLMappingRepository repos) {
        _type = type;
        _isXMLRootElement = _type.getAnnotation(XmlRootElement.class) != null;
        if (_isXMLRootElement) {
            _xmlname = ((XmlRootElement) _type.getAnnotation
                    (XmlRootElement.class)).name();
            _xmlnamespace = ((XmlRootElement) _type.getAnnotation
                    (XmlRootElement.class)).namespace();
        }
        else {
            _xmlname = ((XmlType) _type.getAnnotation
                    (XmlType.class)).name();
            _xmlnamespace = ((XmlType) _type.getAnnotation
                    (XmlType.class)).namespace();           
        }
        populateFromReflection(_type, repos);
        repos.addXMLClassMetaData(type, this);
    }

    /**
     * Given a class type return true if XmlType annotation exists
     * @param type
     * @return true if XmlType annotation is present else false.
     */
    public static boolean isXMLMapping(Class type) {
        return type.isAnnotationPresent(XmlType.class);
    }
    
    public void setName(String name) {
        _name = name;
    }
    
    public String getName() {
        return _name;
    }    
    
    public void setXmlname(String name) {
        _xmlname = name;
    }
    
    public String getXmlname() {
        return _isXMLRootElement ? null : _xmlname;
    }

    public void setXmlnamespace(String name) {
        // avoid JAXB XML bind default name
        if (!StringUtils.equals(defaultName, name))
            _xmlnamespace = name;
    }
    
    public String getXmlnamespace() {
        return _xmlnamespace;
    }

    public boolean isXmlRootElement() {
        return _isXMLRootElement;
    }
    
    public boolean isXmlElement() {
        return false;
    }
    
    public boolean isXmlAttribute() {
        return false;
    }
    
    public XMLMapping getFieldMapping(String name) {
        return (XMLMapping) _fieldMap.get(name);
    }
    
    public void setType(Class type) {
        _type = type;
    }
    
    public Class getType() {
        return _type;
    }
    
    public int getTypeCode() {
        return _code;
    }

    public void setXmltype(int type) {
        _xmltype = type;
    }
    public int getXmltype() {
        return _xmltype;
    }

    private synchronized void populateFromReflection(Class cls, 
        XMLMappingRepository repos) {
        Member[] members;
        if (((XmlAccessorType)cls.getAnnotation(XmlAccessorType.class)).value()
                == XmlAccessType.FIELD)
            members = cls.getDeclaredFields();
        else
            members = cls.getDeclaredMethods();
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            AnnotatedElement el = (AnnotatedElement) member;
            XMLMapping field = null;
            if (el.getAnnotation(XmlElement.class) != null) {
                String xmlname = el.getAnnotation(XmlElement.class).name();
                // avoid JAXB XML bind default name
                if (StringUtils.equals(defaultName, xmlname))
                    xmlname = member.getName();
                if (((Field) member).getType().
                        isAnnotationPresent(XmlType.class)) {
                    field = new XMLClassMetaData(((Field) member).getType(),
                            repos);
                    field.setXmltype(XMLTYPE);
                    field.setXmlname(xmlname);
                }
                else {
                    field = new XMLMetaData();
                    field.setXmltype(ELEMENT);
                    field.setXmlname(xmlname);
                    field.setXmlnamespace(el.getAnnotation(XmlElement.class)
                            .namespace());                    
                }
            }
            else if (el.getAnnotation(XmlAttribute.class) != null) {
                field = new XMLMetaData();
                field.setXmltype(XMLMetaData.ATTRIBUTE);
                String xmlname = el.getAnnotation(XmlAttribute.class).name();
                // avoid JAXB XML bind default name
                if (StringUtils.equals(defaultName, xmlname))
                    xmlname = member.getName();
                field.setXmlname("@"+xmlname);
                field.setXmlnamespace(el.getAnnotation(XmlAttribute.class)
                        .namespace());                
            }
            field.setName(member.getName());
            field.setType(((Field) member).getType());                
            _fieldMap.put(member.getName(), field);
        }        
    }
}
