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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
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
 * Marshals a root instance and its persistent closure as JSON object.
 * The closure is resolved against the persistence context that contains the root instance.
 * The JSON format introduces a $id and $ref to address reference that pure JSON does not. 
 * 
 * @author Pinaki Poddar
 *
 */
public class JSONEncoder {
    /**
     * The element/attribute tags declared in <code>jest-instance.xsd</code> XML schema.
     */
    public static final String ELEMENT_NULL_REF    = "null";
    public static final String ELEMENT_INSTANCE    = "instance";
    public static final String ELEMENT_REF         = "ref";
        
    
    private MetamodelHelper _model;
    
    public JSONEncoder(MetamodelImpl model) {
        _model = new MetamodelHelper(model);
    }
    
    /**
     * Encodes the given managed instance into a new XML element as a child of the given parent node.
     * 
     * @param sm a managed instance, can be null.
     * @param parent the parent node to which the new node be attached.
     */
    public StringBuilder encode(final OpenJPAStateManager sm) {
        return encode(sm, new HashSet<OpenJPAStateManager>(), 0, false);
    }
    StringBuilder indent(StringBuilder buf, int indent) {
        if (indent <= 0)
            return buf;
        char[] spaces = new char[indent*4];
        Arrays.fill(spaces, ' ');
        buf.insert(0, spaces);
        return buf;
    }
    StringBuilder end(StringBuilder buf, char ch, int indent) {
        char[] spaces = new char[indent*4];
        Arrays.fill(spaces, ' ');
        return buf.append("\r\n").append(spaces).append(ch);
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
    private StringBuilder encode(final OpenJPAStateManager sm, final Set<OpenJPAStateManager> visited, 
        int indent, boolean indentPara) {
        if (visited == null) {
            throw new IllegalArgumentException("null closure for encoder");
        }
        StringBuilder root =  indent(new StringBuilder("{"), indentPara ? indent : 0);
        if (sm == null) {
            return root.append("null}");
        }
        boolean ref = !visited.add(sm);
        if (ref) {
            return indent(root.append(quoted("$ref")).append(": ").append(ior(sm)).append('}'), 
                indentPara ? indent : 0);
        } else {
            indent(root.append(quoted("$id")).append(": ").append(ior(sm)), indentPara ? indent : 0);
        }
        
        StringBuilder child = new StringBuilder();
        BitSet loaded = sm.getLoaded();
        StoreContext ctx = (StoreContext)sm.getGenericContext();
        List<Attribute<?, ?>> attrs = _model.getAttributesInOrder(sm.getMetaData());
        for (int i = 0; i < attrs.size(); child = new StringBuilder(), i++) {
            FieldMetaData fmd = ((Members.Member<?, ?>) attrs.get(i)).fmd;
            if (!loaded.get(fmd.getIndex())) 
                continue;
            Object value = sm.fetch(fmd.getIndex());
            child.append(quoted(fmd.getName())).append(": ");
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
                         child.append(quoted(value));
                break;
                
                case JavaTypes.PC:
                    if (value == null) {
                        child.append("null");
                    } else {
                        child.append(encode(ctx.getStateManager(value), visited, indent+1, false));
                    }
                    break;
                    
                case JavaTypes.ARRAY:
                    Object[] values = (Object[])value;
                    value = Arrays.asList(values);
                // no break;
                case JavaTypes.COLLECTION:
                    if (value == null) {
                        child.append("null");
                        break;
                    }
                    child.append("[");
                    Collection<?> members = (Collection<?>)value;
                    boolean basic = fmd.getElement().getTypeMetaData() == null;
                    int k = 0;
                    for (Object o : members) {
                        child.append("\r\n");
                        if (o == null) {
                            child.append(indent(new StringBuilder("null"), indent+1)); 
                        } else {
                            if (basic) {
                                child.append(indent(new StringBuilder(quoted(o)), indent+1));
                            } else {
                                child.append(encode(ctx.getStateManager(o), visited, indent+1, true));
                            }
                        }
                    }
                    end(child, ']', indent+1);
                    break;
                case JavaTypes.MAP:
                    if (value == null) {
                        child.append("null");
                        break;
                    }
                    child.append("[");
                    Set<Map.Entry> entries = ((Map)value).entrySet();
                    boolean basicKey   = fmd.getElement().getTypeMetaData() == null;
                    boolean basicValue = fmd.getValue().getTypeMetaData() == null;
                    for (Map.Entry<?,?> e : entries) {
                        if (e.getKey() == null) {
                            child.append("null:");
                        } else {
                            if (basicKey) {
                                child.append(quoted(e.getKey())).append(":");
                            } else {
                                child.append(encode(ctx.getStateManager(e.getKey()), visited, indent+1, true));
                            }
                        }
                        if (e.getValue() == null) {
                            child.append("null");
                        } else {
                            if (basicValue) {
                                child.append(quoted(e.getValue()));
                            } else {
                                child.append(encode(ctx.getStateManager(e.getValue()), visited, indent+1, false));
                            }
                        }
                    }
                    break;
                    
                case JavaTypes.INPUT_STREAM:
                case JavaTypes.INPUT_READER:
                    child = new StringBuilder(fmd.getName());
                    if (value == null) {
                        child.append("null");
                    } else { 
                        child.append(streamToString(value));
                    }
                    break;
                    
                case JavaTypes.PC_UNTYPED:
                case JavaTypes.OBJECT:
                case JavaTypes.OID:
                    System.err.println("Not handled " + fmd.getName() + " of type " + fmd.getDeclaredType());
            }
            
            if (child != null) {
                root.append("\r\n");
                root.append(indent(child, indent+1));
                if (loaded.length()-1 != i)
                    root.append(",");
           }
        }
        return end(root, '}', indent);
    }
    
    
    String ior(OpenJPAStateManager sm) {
        return quoted(typeOf(sm)+"-"+sm.getObjectId().toString());
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
    
    String quoted(Object o) {
        if (o == null) return "null";
        if (o instanceof Number)
            return o.toString();
        return "\"" + o.toString() + "\"";
    }
}
