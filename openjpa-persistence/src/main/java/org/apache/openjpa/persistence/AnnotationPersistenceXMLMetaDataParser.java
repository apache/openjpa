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
package org.apache.openjpa.persistence;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.DelegatingMetaDataFactory;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.XMLFieldMetaData;
import org.apache.openjpa.meta.XMLMetaData;

/**
 * JAXB xml annotation metadata parser.
 *
 * @author Catalina Wei
 * @nojavadoc
 */
public class AnnotationPersistenceXMLMetaDataParser {

    private static final Localizer _loc = Localizer.forPackage
        (AnnotationPersistenceXMLMetaDataParser.class);

    private final OpenJPAConfiguration _conf;
    private final Log _log;
    private MetaDataRepository _repos = null;

    // the class we were invoked to parse
    private Class _cls = null;
    private FieldMetaData _fmd = null;

    /**
     * Constructor; supply configuration.
     */
    public AnnotationPersistenceXMLMetaDataParser(OpenJPAConfiguration conf) {
        _conf = conf;
        _log = conf.getLog(OpenJPAConfiguration.LOG_METADATA);
    }

    /**
     * Configuration supplied on construction.
     */
    public OpenJPAConfiguration getConfiguration() {
        return _conf;
    }

    /**
     * Metadata log.
     */
    public Log getLog() {
        return _log;
    }

    /**
     * Returns the repository for this parser. If none has been set,
     * create a new repository and sets it.
     */
    public MetaDataRepository getRepository() {
        if (_repos == null) {
            MetaDataRepository repos = _conf.newMetaDataRepositoryInstance();
            MetaDataFactory mdf = repos.getMetaDataFactory();
            if (mdf instanceof DelegatingMetaDataFactory)
                mdf = ((DelegatingMetaDataFactory) mdf).getInnermostDelegate();
            if (mdf instanceof PersistenceMetaDataFactory)
                ((PersistenceMetaDataFactory) mdf).setXMLAnnotationParser(this);
            _repos = repos;
        }
        return _repos;
    }

    /**
     * Set the metadata repository for this parser.
     */
    public void setRepository(MetaDataRepository repos) {
        _repos = repos;
    }

    /**
     * Clear caches.
     */
    public void clear() {
        _cls = null;
        _fmd = null;
    }

    /**
     * Parse persistence metadata for the given field metadata.
     */
    public void parse(FieldMetaData fmd) {
        _fmd = fmd;
        _cls = fmd.getDeclaredType();
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("parse-class", _cls.getName()));

        try {
            parseXMLClassAnnotations();
        } finally {
            _cls = null;
            _fmd = null;
        }
    }

    /**
     * Read annotations for the current type.
     */
    private XMLMetaData parseXMLClassAnnotations() {
        // check immediately whether the class has JAXB XML annotations
        if (!_cls.isAnnotationPresent(XmlType.class))
            return null;

        // find / create metadata
        XMLMetaData meta = getXMLMetaData();
        
        return meta;
    }

    /**
     * Find or create xml metadata for the current type. 
     */
    private synchronized XMLMetaData getXMLMetaData() {
        XMLMetaData meta = getRepository().getCachedXMLMetaData(_cls);
        if (meta == null) {
            // if not in cache, create metadata
            meta = getRepository().addXMLMetaData(_cls, _fmd.getName());
            parseXmlRootElement(_cls, meta);
            populateFromReflection(_cls, meta);
        }
        return meta;
    }
    
    private void parseXmlRootElement(Class type, XMLMetaData meta) {
        if (type.getAnnotation(XmlRootElement.class) != null) {
            meta.setXmlRootElement(true);
            meta.setXmlname(((XmlRootElement) type.getAnnotation
                (XmlRootElement.class)).name());
            meta.setXmlnamespace(((XmlRootElement) type.getAnnotation
                (XmlRootElement.class)).namespace());
        }
        else {
            meta.setXmlname(((XmlType) type.getAnnotation
                (XmlType.class)).name());
            meta.setXmlnamespace(((XmlType) type.getAnnotation
                (XmlType.class)).namespace());           
        }        
    }

    private void populateFromReflection(Class cls, XMLMetaData meta) {
        Member[] members;
        
        Class superclass = cls.getSuperclass();

        // handle inheritance at sub-element level
        if (superclass.isAnnotationPresent(XmlType.class))
            populateFromReflection(superclass, meta);

        if (((XmlAccessorType) cls.getAnnotation(XmlAccessorType.class)).value()
            == XmlAccessType.FIELD)
            members = cls.getDeclaredFields();
        else
            members = cls.getDeclaredMethods();

        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            AnnotatedElement el = (AnnotatedElement) member;
            XMLMetaData field = null;
            if (el.getAnnotation(XmlElement.class) != null) {
                String xmlname = el.getAnnotation(XmlElement.class).name();
                // avoid JAXB XML bind default name
                if (StringUtils.equals(XMLMetaData.defaultName, xmlname))
                    xmlname = member.getName();
                if (((Field) member).getType()
                    .isAnnotationPresent(XmlType.class)) {
                    field = _repos.addXMLMetaData(((Field) member).getType()
                        , member.getName());
                    parseXmlRootElement(((Field) member).getType(), field);
                    populateFromReflection(((Field) member).getType(), field);
                    field.setXmltype(XMLMetaData.XMLTYPE);
                    field.setXmlname(xmlname);
                }
                else {
                    field = _repos.newXMLFieldMetaData(((Field) member)
                        .getType(), member.getName());
                    field.setXmltype(XMLMetaData.ELEMENT);
                    field.setXmlname(xmlname);
                    field.setXmlnamespace(el.getAnnotation(XmlElement.class)
                        .namespace());                    
                }
            }
            else if (el.getAnnotation(XmlAttribute.class) != null) {
                field = _repos.newXMLFieldMetaData(((Field) member).getType()
                    , member.getName());
                field.setXmltype(XMLFieldMetaData.ATTRIBUTE);
                String xmlname = el.getAnnotation(XmlAttribute.class).name();
                // avoid JAXB XML bind default name
                if (StringUtils.equals(XMLMetaData.defaultName, xmlname))
                    xmlname = member.getName();
                field.setXmlname("@"+xmlname);
                field.setXmlnamespace(el.getAnnotation(XmlAttribute.class)
                    .namespace());                
            }
            meta.addField(member.getName(), field);
        }        
    }
}
