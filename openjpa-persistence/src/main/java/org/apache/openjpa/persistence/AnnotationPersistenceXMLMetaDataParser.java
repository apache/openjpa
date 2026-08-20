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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.DelegatingMetaDataFactory;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.XMLMetaData;

/**
 * JAXB xml annotation metadata parser.
 *
 * @author Catalina Wei
 * @since 1.0.0
 */
public class AnnotationPersistenceXMLMetaDataParser <T extends Annotation> {

    private static final Localizer _loc = Localizer.forPackage
        (AnnotationPersistenceXMLMetaDataParser.class);

    private final OpenJPAConfiguration _conf;
    private final Log _log;
    private MetaDataRepository _repos = null;

    // cache the JAXB Xml... classes if they are present so we do not
    // have a hard-wired dependency on JAXB here
    private Class<T> xmlTypeClass = null;
    private Class<T> xmlRootElementClass = null;
    private Class<T> xmlAccessorTypeClass = null;
    private Class<T> xmlAttributeClass = null;
    private Class<T> xmlElementClass = null;
    private Method xmlTypeName = null;
    private Method xmlTypeNamespace = null;
    private Method xmlRootName = null;
    private Method xmlRootNamespace = null;
    private Method xmlAttributeName = null;
    private Method xmlAttributeNamespace = null;
    private Method xmlElementName = null;
    private Method xmlElementNamespace = null;
    private Method xmlAccessorValue = null;

    /**
     * Constructor; supply configuration.
     */
    @SuppressWarnings("unchecked")
	public AnnotationPersistenceXMLMetaDataParser(OpenJPAConfiguration conf) {
        _conf = conf;
        _log = conf.getLog(OpenJPAConfiguration.LOG_METADATA);
        try {
            xmlTypeClass = (Class<T>) Class.forName("jakarta.xml.bind.annotation.XmlType");
            xmlTypeName = xmlTypeClass.getMethod("name");
            xmlTypeNamespace = xmlTypeClass.getMethod("namespace");
            xmlRootElementClass = (Class<T>) Class.forName("jakarta.xml.bind.annotation.XmlRootElement");
            xmlRootName = xmlRootElementClass.getMethod("name");
            xmlRootNamespace = xmlRootElementClass.getMethod("namespace");
            xmlAccessorTypeClass = (Class<T>) Class.forName("jakarta.xml.bind.annotation.XmlAccessorType");
            xmlAccessorValue = xmlAccessorTypeClass.getMethod("value");
            xmlAttributeClass = (Class<T>) Class.forName("jakarta.xml.bind.annotation.XmlAttribute");
            xmlAttributeName = xmlAttributeClass.getMethod("name");
            xmlAttributeNamespace = xmlAttributeClass.getMethod("namespace");
            xmlElementClass = (Class<T>) Class.forName("jakarta.xml.bind.annotation.XmlElement");
            xmlElementName = xmlElementClass.getMethod("name");
            xmlElementNamespace = xmlElementClass.getMethod("namespace");
        } catch (Exception e) {
        }
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
    }

    /**
     * Parse persistence metadata for the given field metadata. This parser/class is NOT threadsafe! The caller of
     * this method needs to insure that the MetaData(/Mapping)Repository is locked prior to calling this method.
     */
    public synchronized void parse(Class<?> cls) {
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("parse-class", cls.getName()));
        parseXMLClassAnnotations(cls);
    }

    /**
     * Read annotations for the current type.
     */
    private XMLMetaData parseXMLClassAnnotations(Class<?> cls) {
        // check immediately whether the class has JAXB XML annotations
        if (cls == null || xmlTypeClass == null
            || !(cls.isAnnotationPresent((Class<? extends Annotation>) xmlTypeClass)
                && cls.isAnnotationPresent((Class<? extends Annotation>) xmlRootElementClass)))
            return null;

        // find / create metadata
        XMLMetaData meta = getXMLMetaData(cls);

        return meta;
    }

    /**
     * Find or create xml metadata for the current type.
     */
    private XMLMetaData getXMLMetaData(Class<?> cls) {
        XMLMetaData meta = getRepository().getCachedXMLMetaData(cls);
        if (meta == null) {
            // if not in cache, create metadata
            meta = getRepository().addXMLClassMetaData(cls);
            parseXmlRootElement(cls, meta);
            populateFromReflection(cls, meta);
        }
        return meta;
    }

    private void parseXmlRootElement(Class<?> type, XMLMetaData meta) {
        try {
            if (type.getAnnotation(xmlRootElementClass) != null) {
                meta.setXmlRootElement(true);
                meta.setXmlname((String) xmlRootName.invoke(type.getAnnotation(xmlRootElementClass), new Object[]{}));
                meta.setXmlnamespace((String) xmlRootNamespace.invoke(type.getAnnotation(xmlRootElementClass), new Object[]{}));
            }
            else {
                meta.setXmlname((String) xmlTypeName.invoke(type.getAnnotation(xmlTypeClass), new Object[]{}));
                meta.setXmlnamespace((String) xmlTypeNamespace.invoke(type.getAnnotation(xmlTypeClass), new Object[]{}));
            }
        } catch (Exception e) {
        }
    }

    private void populateFromReflection(Class<?> cls, XMLMetaData meta) {
        Member[] members;

        Class<?> superclass = cls.getSuperclass();

        // handle inheritance at sub-element level
        if (superclass.isAnnotationPresent(xmlTypeClass))
            populateFromReflection(superclass, meta);

        try {
            if (Objects.equals(xmlAccessorValue.invoke(cls.getAnnotation(xmlAccessorTypeClass), new Object[]{}).toString(), "FIELD"))
                members = cls.getDeclaredFields();
            else
                members = cls.getDeclaredMethods();

            for (Member member : members) {
                AnnotatedElement el = (AnnotatedElement) member;
                XMLMetaData field = null;
                if (el.getAnnotation(xmlElementClass) != null) {
                    String xmlname = (String) xmlElementName.invoke(el.getAnnotation(xmlElementClass), new Object[]{});
                    // avoid JAXB XML bind default name
                    if (Objects.equals(XMLMetaData.defaultName, xmlname))
                        xmlname = member.getName();
                    if (((Field) member).getType().isAnnotationPresent((Class<? extends Annotation>) xmlTypeClass)) {
                        field = _repos.addXMLClassMetaData(((Field) member).getType());
                        parseXmlRootElement(((Field) member).getType(), field);
                        populateFromReflection(((Field) member).getType()
                                , field);
                        field.setXmltype(XMLMetaData.XMLTYPE);
                        field.setXmlname(xmlname);
                    }
                    else {
                        field = _repos.newXMLFieldMetaData(((Field) member)
                                .getType(), member.getName());
                        field.setXmltype(XMLMetaData.ELEMENT);
                        field.setXmlname(xmlname);
                        field.setXmlnamespace((String) xmlElementNamespace.invoke(el.getAnnotation(xmlElementClass)
                                        , new Object[]{}));
                    }
                }
                else if (el.getAnnotation(xmlAttributeClass) != null) {
                    field = _repos.newXMLFieldMetaData(((Field) member)
                            .getType(), member.getName());
                    field.setXmltype(XMLMetaData.ATTRIBUTE);
                    String xmlname = (String) xmlAttributeName.invoke(
                            el.getAnnotation(xmlAttributeClass), new Object[]{});
                    // avoid JAXB XML bind default name
                    if (Objects.equals(XMLMetaData.defaultName, xmlname))
                        xmlname = member.getName();
                    field.setXmlname("@" + xmlname);
                    field.setXmlnamespace((String) xmlAttributeNamespace.invoke(
                            el.getAnnotation(xmlAttributeClass), new Object[]{}));
                }
                if (field != null)
                    meta.addField(member.getName(), field);
            }
        } catch(Exception e) {
        }
    }
}
