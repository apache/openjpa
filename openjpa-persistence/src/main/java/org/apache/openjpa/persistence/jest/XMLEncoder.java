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

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Marshals a root instance and its persistent closure as an XML element.
 * The closure is resolved against the persistence context that contains the root instance.
 * The XML document adheres to the <code>jest-instance.xsd</code> schema. 
 * 
 * @author Pinaki Poddar
 *
 */
public class XMLEncoder {
    /**
     * The element/attribute tags declared in <code>jest-instance.xsd</code> XML schema.
     */
    public static final String ELEMENT_NULL_REF    = "null";
    public static final String ELEMENT_INSTANCE    = "instance";
    public static final String ELEMENT_REF         = "ref";
    public static final String ELEMENT_BASIC       = "basic";
    public static final String ELEMENT_LOB         = "lob";
    public static final String ELEMENT_SINGULAR    = "singular";
    public static final String ELEMENT_COLLECTION  = "collection";
    public static final String ELEMENT_MAP         = "map";
    public static final String ELEMENT_MEMBER      = "member";
    public static final String ELEMENT_ENTRY       = "entry";
    public static final String ELEMENT_ENTRY_KEY   = "key";
    public static final String ELEMENT_ENTRY_VALUE = "value";
        
    public static final String ATTR_TYPE           = "type";
    public static final String ATTR_ID             = "id";
    public static final String ATTR_NAME           = "name";
    public static final String ATTR_NULL           = "null";
    public static final String ATTR_MEMBER_TYPE    = "member-type";
    public static final String ATTR_KEY_TYPE       = "key-type";
    public static final String ATTR_VALUE_TYPE     = "value-type";

    public static final  InputStream _xsd;
    static final String W3C_XML_SCHEMA          = "http://www.w3.org/2001/XMLSchema";
    static final String W3C_XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
    static final String JAXP_SCHEMA_SOURCE      = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String JAXP_SCHEMA_LANGUAGE    = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String JEST_INSTANCE_XSD       = "jest-instance.xsd";
    static final String JEST_INSTANCE_XSD_PATH  = "META-INF/" + JEST_INSTANCE_XSD;
    
    private MetamodelHelper _model;
    private static final DocumentBuilder _builder;
    private static final Transformer   _transformer;
    
    static {
        try {
            _builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            _transformer = TransformerFactory.newInstance().newTransformer();
            _xsd = Thread.currentThread().getContextClassLoader().getResourceAsStream(JEST_INSTANCE_XSD_PATH);
            _transformer.setOutputProperty(OutputKeys.METHOD,     "xml");
            _transformer.setOutputProperty(OutputKeys.INDENT,     "yes");
            _transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            _transformer.setOutputProperty(OutputKeys.ENCODING,   "UTF-8");
            _transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public XMLEncoder(MetamodelImpl model) {
        _model = new MetamodelHelper(model);
    }
    
    /**
     * Write the given XML document to the given file.
     */
    void writeDoc(Document doc, OutputStream out) throws Exception {
        _transformer.transform(new DOMSource(doc), new StreamResult(out));
        out.flush();
    }
    
    /**
     * Encodes the given managed instance into a new XML element as a child of the given parent node.
     * 
     * @param sm a managed instance, can be null.
     * @param parent the parent node to which the new node be attached.
     */
    public Document encode(final OpenJPAStateManager sm) {
        Document doc = _builder.newDocument();
        Element root = doc.createElement("instances");
        doc.appendChild(root);
        encode(sm, root, new HashSet<OpenJPAStateManager>());
        return doc;
    }
    
    /**
     * Encodes the given managed instance into a new XML element as a child of the given parent node.
     * 
     * @param sm a managed instance, can be null.
     * @param parent the parent node to which the new node be attached.
     */
    public Element encode(final OpenJPAStateManager sm, Element parent) {
        return encode(sm, parent, new HashSet<OpenJPAStateManager>());
    }
    
    /**
     * Encodes the closure of a persistent instance into a XML element.
     * 
     * @param sm the managed instance to be encoded. Can be null.
     * @param parent the parent XML element to which the new XML element be added. Must not be null. Must be
     * owned by a document. 
     * @param visited the persistent instances that had been encoded already. Must not be null or immutable.
     * 
     * @return the new element. The element has been appended as a child to the given parent in this method.  
     */
    private Element encode(final OpenJPAStateManager sm, final Element parent, final Set<OpenJPAStateManager> visited) {
        if (parent == null) {
            throw new IllegalArgumentException("null parent element for encoder");
        }
        Document doc = parent.getOwnerDocument();
        if (doc == null) {
            throw new IllegalStateException("Given parent element is not part of XML document");
        }
        if (visited == null) {
            throw new IllegalArgumentException("null closure for encoder");
        }
        if (sm == null) {
            Element nullRef = doc.createElement(ELEMENT_NULL_REF);
            parent.appendChild(nullRef);
            return nullRef;
        }
        boolean ref = !visited.add(sm);
        Element root = doc.createElement(ref ? ELEMENT_REF : ELEMENT_INSTANCE);
        parent.appendChild(root);
        root.setAttribute(ATTR_ID, ior(sm));
        if (ref) 
            return root;
        
        Element child = null;
        BitSet loaded = sm.getLoaded();
        StoreContext ctx = (StoreContext)sm.getGenericContext();
        List<Attribute<?, ?>> attrs = _model.getAttributesInOrder(sm.getMetaData());
        for (int i = 0; i < attrs.size(); child = null, i++) {
            FieldMetaData fmd = ((Members.Member<?, ?>) attrs.get(i)).fmd;
            if (!loaded.get(fmd.getIndex())) 
                continue;
            Object value = sm.fetch(fmd.getIndex());
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.BOOLEAN:
                case JavaTypes.BYTE:
                case JavaTypes.CHAR:
                case JavaTypes.DOUBLE:
                case JavaTypes.FLOAT:
                case JavaTypes.INT:
                case JavaTypes.LONG:
                case JavaTypes.SHORT:

                case JavaTypes.BOOLEAN_OBJ:
                case JavaTypes.BYTE_OBJ:
                case JavaTypes.CHAR_OBJ:
                case JavaTypes.DOUBLE_OBJ:
                case JavaTypes.FLOAT_OBJ:
                case JavaTypes.INT_OBJ:
                case JavaTypes.LONG_OBJ:
                case JavaTypes.SHORT_OBJ:

                case JavaTypes.BIGDECIMAL:
                case JavaTypes.BIGINTEGER:
                case JavaTypes.DATE:
                case JavaTypes.NUMBER:
                case JavaTypes.CALENDAR:
                case JavaTypes.LOCALE:
                case JavaTypes.STRING:
                case JavaTypes.ENUM:
                child = doc.createElement(ELEMENT_BASIC);
                child.setAttribute(ATTR_NAME, fmd.getName());
                if (value == null) {
                    encodeNull(child);
                } else { 
                    encodeBasic(child, value, fmd.getDeclaredType());
                }
                break;
                
                case JavaTypes.OID:
                    child = doc.createElement(ELEMENT_REF);
                    child.setAttribute(ATTR_NAME, fmd.getName());
                    if (value == null) {
                        encodeNull(child);
                    } else { 
                        encodeBasic(child, value, fmd.getDeclaredType());
                    }
                    break;
                    
                case JavaTypes.PC:
                    child = doc.createElement(ELEMENT_SINGULAR);
                    child.setAttribute(ATTR_NAME, fmd.getName());
                    child.setAttribute(ATTR_TYPE, typeOf(fmd));
                    OpenJPAStateManager other = ctx.getStateManager(value);
                    encode(other, child, visited);
                    break;
                    
                case JavaTypes.ARRAY:
                    Object[] values = (Object[])value;
                    value = Arrays.asList(values);
                // no break;
                case JavaTypes.COLLECTION:
                    child = doc.createElement(ELEMENT_COLLECTION);
                    child.setAttribute(ATTR_NAME, fmd.getName());
                    child.setAttribute(ATTR_TYPE, typeOf(fmd));
                    child.setAttribute(ATTR_MEMBER_TYPE, typeOf(fmd.getElement().getDeclaredType()));
                    if (value == null) {
                        encodeNull(child);
                        break;
                    }
                    Collection<?> members = (Collection<?>)value;
                    boolean basic = fmd.getElement().getTypeMetaData() == null;
                    for (Object o : members) {
                        Element member = doc.createElement(ELEMENT_MEMBER);
                        child.appendChild(member);
                        if (o == null) {
                            encodeNull(member); 
                        } else {
                            if (basic) {
                                encodeBasic(member, o, o.getClass());
                            } else {
                                encode(ctx.getStateManager(o), member, visited);
                            }
                        }
                    }
                    break;
                case JavaTypes.MAP:
                    child = doc.createElement(ELEMENT_MAP);
                    child.setAttribute(ATTR_NAME, fmd.getName());
                    child.setAttribute(ATTR_TYPE, typeOf(fmd));
                    child.setAttribute(ATTR_KEY_TYPE, typeOf(fmd.getElement().getDeclaredType()));
                    child.setAttribute(ATTR_VALUE_TYPE, typeOf(fmd.getValue().getDeclaredType()));
                    if (value == null) {
                        encodeNull(child);
                        break;
                    }
                    Set<Map.Entry> entries = ((Map)value).entrySet();
                    boolean basicKey   = fmd.getElement().getTypeMetaData() == null;
                    boolean basicValue = fmd.getValue().getTypeMetaData() == null;
                    for (Map.Entry<?,?> e : entries) {
                        Element entry = doc.createElement(ELEMENT_ENTRY);
                        Element entryKey = doc.createElement(ELEMENT_ENTRY_KEY);
                        Element entryValue = doc.createElement(ELEMENT_ENTRY_VALUE);
                        entry.appendChild(entryKey);
                        entry.appendChild(entryValue);
                        child.appendChild(entry);
                        if (e.getKey() == null) {
                            encodeNull(entryKey);
                        } else {
                            if (basicKey) {
                                encodeBasic(entryKey, e.getKey(), e.getKey().getClass());
                            } else {
                                encode(ctx.getStateManager(e.getKey()), entryKey, visited);
                            }
                        }
                        if (e.getValue() == null) {
                            encodeNull(entryValue);
                        } else {
                            if (basicValue) {
                                encodeBasic(entryValue, e.getValue(), e.getValue().getClass());
                            } else {
                                encode(ctx.getStateManager(e.getValue()), entryValue, visited);
                            }
                        }
                    }
                    break;
                    
                case JavaTypes.INPUT_STREAM:
                case JavaTypes.INPUT_READER:
                    child = doc.createElement(ELEMENT_LOB);
                    child.setAttribute(ATTR_NAME, fmd.getName());
                    child.setAttribute(ATTR_TYPE, typeOf(fmd));
                    if (value == null) {
                        encodeNull(child);
                    } else { 
                        CDATASection data = doc.createCDATASection(streamToString(value));
                        child.appendChild(data);
                    }
                    break;
                    
                case JavaTypes.PC_UNTYPED:
                case JavaTypes.OBJECT:
                    System.err.println("Not handled " + fmd.getName() + " of type " + fmd.getDeclaredType());
            }
            
            if (child != null) {
                root.appendChild(child);
            }
        }
        return root;
    }
    
    /**
     * Sets the given value element as null. The <code>null</code> attribute is set to true.
     * 
     * @param element the XML element to be set
     */
    void encodeNull(Element element) {
        element.setAttribute(ATTR_NULL, "true");
    }
    
    
    /**
     * Sets the given value element. The <code>type</code> is set to the given runtime type.
     * String form of the given object is set as the text content. 
     * 
     * @param element the XML element to be set
     * @param obj value of the element. Never null.
     */
    void encodeBasic(Element element, Object obj, Class<?> runtimeType) {
        element.setAttribute(ATTR_TYPE, typeOf(runtimeType));
        element.setTextContent(obj.toString());
    }
    
    String ior(OpenJPAStateManager sm) {
        return typeOf(sm)+"-"+sm.getObjectId().toString();
    }
    
    String typeOf(OpenJPAStateManager sm) {
        return sm.getMetaData().getDescribedType().getSimpleName();
    }
    
    String typeOf(Class<?> cls) {
        return cls.getSimpleName();
    }
    
    String typeOf(ClassMetaData meta) {
        return meta.getDescribedType().getSimpleName();
    }
    
    String typeOf(ValueMetaData vm) {
        if (vm.getTypeMetaData() == null)
            return typeOf(vm.getType()); 
        return typeOf(vm.getTypeMetaData());
    }
    
    String typeOf(FieldMetaData fmd) {
        return fmd.getType().getSimpleName();
    }
    
    
    /**
     * Convert the given stream (either an InutStream or a Reader) to a String
     * to be included in CDATA section of a XML document.
     * 
     * @param value the field value to be converted. Can not be null 
     * @return
     */
    String streamToString(Object value) {
        Reader reader = null;
        if (value instanceof InputStream) {
            reader = new BufferedReader(new InputStreamReader((InputStream)value));
        } else if (value instanceof Reader) {
            reader = (Reader)value;
        } else {
            throw new RuntimeException();
        }
        CharArrayWriter writer = new CharArrayWriter();
        try {
            for (int c; (c = reader.read()) != -1;) {
                writer.write(c);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return writer.toString();
    }
    
    void addSchemaToRoot(Document doc) {
        Element root = doc.getDocumentElement();
        String[] nvpairs = new String[] {
                "xmlns:xsi",                     W3C_XML_SCHEMA_INSTANCE,
                "xsi:noNamespaceSchemaLocation", JEST_INSTANCE_XSD,
                "version",                       "1.0",
        };
        for (int i = 0; i < nvpairs.length; i += 2) {
            root.setAttribute(nvpairs[i], nvpairs[i+1]);
        }
    }

}
