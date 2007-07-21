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
package org.apache.openjpa.jdbc.meta;

import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.XMLClassMetaData;
import org.apache.openjpa.meta.XMLMapping;

/**
 * Repository of object/relational mapping information.
 *  (extended to include XML mapping metadata for XML columns)
 *  
 * @author Catalina Wei
 * @since 1.0.0
 */
public class XMLMappingRepository extends MappingRepository {
    // xml mapping
    protected final XMLMapping[] EMPTY_XMLMETAS;
    private final Map _xmlmetas = new HashMap();

    public XMLMappingRepository() {
        super();
        EMPTY_XMLMETAS = newXMLClassMetaDataArray(0);
    }
    
    public synchronized XMLClassMetaData addXMLClassMetaData(FieldMetaData fmd, 
        String name) {        
        XMLClassMetaData meta = newXMLClassMetaData(fmd, name);
        addXMLClassMetaData(fmd.getDeclaredType(), meta);
        return meta;
    }
    
    public XMLMapping getXMLClassMetaData(Class cls) {
        synchronized(_xmlmetas) {
            if (_xmlmetas.isEmpty())
                return null;
            else
                return (XMLClassMetaData) _xmlmetas.get(cls);
        }
    }
    
    public XMLMapping getXMLMetaData(FieldMetaData fmd) {
        XMLMapping xmlmeta = null;
        if (XMLClassMetaData.isXMLMapping(fmd.getDeclaredType())) {
            xmlmeta = getXMLClassMetaData(fmd.getDeclaredType());
            if (xmlmeta == null)
                xmlmeta = addXMLClassMetaData(fmd, fmd.getName());
        }
        return xmlmeta;
    }
    
    public synchronized void addXMLClassMetaData(Class cls, XMLMapping meta) {
        _xmlmetas.put(cls, meta);
    }    
    
    protected XMLClassMetaData newXMLClassMetaData(FieldMetaData fmd, String name) {
        return new XMLClassMetaData(fmd.getDeclaredType(), name, this);
    }
        
    protected XMLMapping[] newXMLClassMetaDataArray(int length) {
        return new XMLClassMetaData[length];
    }
}
