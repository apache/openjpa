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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * @author Pinaki Poddar
 *
 */
public class MetamodelHelper {
    private MetamodelImpl _model;
    private Map<ManagedType<?>, List<Attribute<?, ?>>> _attrs = new HashMap<ManagedType<?>, List<Attribute<?,?>>>();
    
    public MetamodelHelper(MetamodelImpl model) {
        _model = model;
    }
    
    public List<Attribute<?,?>> getAttributesInOrder(Class<?> cls) {
        return getAttributesInOrder(_model.managedType(cls));
    }
    
    public List<Attribute<?,?>> getAttributesInOrder(ClassMetaData meta) {
        return getAttributesInOrder(meta.getDescribedType());
    }
    
    /**
     * Gets the attributes of the given type in defined order.
     * @param type
     * @return
     */
    public List<Attribute<?,?>> getAttributesInOrder(ManagedType<?> type) {
        List<Attribute<?,?>> attrs = _attrs.get(type);
        if (attrs != null)
            return attrs;
        List<Attribute<?,?>> list = new ArrayList<Attribute<?,?>>(type.getAttributes());
        Collections.sort(list, new AttributeComparator());
        _attrs.put(type, list);
        return list;
    }

    public static boolean isId(Attribute<?,?> a) {
        if (a instanceof SingularAttribute)
            return ((SingularAttribute<?,?>)a).isId();
        return false;
    }
    
    public static boolean isVersion(Attribute<?,?> a) {
        if (a instanceof SingularAttribute)
            return ((SingularAttribute<?,?>)a).isVersion();
        return false;
    }

    public static Integer getAttributeTypeCode(Attribute<?,?> attr) {
        if (isId(attr))
            return 0;
        if (isVersion(attr))
            return 1;
        
      switch (attr.getPersistentAttributeType()) {
      case BASIC : 
      case EMBEDDED:
          return 2;
      case ONE_TO_ONE: 
      case MANY_TO_ONE:
          return 3;
      case ONE_TO_MANY:
      case MANY_TO_MANY:
      case ELEMENT_COLLECTION: return 4;
      default: return 5;
      }
    }
    
    /**
     * Compares attribute by their qualification.
     * Identity 
     * Version
     * Basic
     * Singular association
     * Plural association
     *
     */
    public static class AttributeComparator implements Comparator<Attribute<?,?>> {
//        @Override
        public int compare(Attribute<?, ?> a1, Attribute<?, ?> a2) {
            Integer t1 = getAttributeTypeCode(a1);
            Integer t2 = getAttributeTypeCode(a2);
            if (t1.equals(t2)) {
                return a1.getName().compareTo(a2.getName());
            } else {
                return t1.compareTo(t2);
            }
        }
    }
}
