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

package org.apache.openjpa.persistence.meta;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import javax.persistence.metamodel.AbstractCollection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Collection;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.List;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Map;
import javax.persistence.metamodel.MappedSuperclass;
import javax.persistence.metamodel.Member;
import javax.persistence.metamodel.Set;
import javax.persistence.metamodel.Type;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Type according to JPA 2.0.
 * 
 * Implemented as a thin adapter to OpenJPA metadata system.
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 * 
 */
public class Types {
    protected static Localizer _loc = Localizer.forPackage(Types.class);

    /**
     * Mirrors a concrete Java type X.
     *
     * @param <X> Java class 
     */
    private static abstract class BaseType<X> implements Type<X> {
        public final Class<X> cls;

        protected BaseType(Class<X> cls) {
            this.cls = cls;
        }

        public final Class<X> getJavaType() {
            return cls;
        }

        public String toString() {
            return "" + cls;
        }
    }

    public static class Basic<X> extends BaseType<X> implements Type<X> {
        public Basic(Class<X> cls) {
            super(cls);
        }

        public PersistenceType getPersistenceType() {
            return PersistenceType.BASIC;
        }
    }

    public static abstract class Managed<X> extends BaseType<X> implements
        ManagedType<X> {

        public final MetamodelImpl model;
        public final ClassMetaData meta;
        
        private java.util.Map<FieldMetaData, Attribute<? super X, ?>> attrs =
            new HashMap<FieldMetaData, Attribute<? super X, ?>>();

        private java.util.Map<FieldMetaData, AbstractCollection<? super X,?,?>> 
        colls = new HashMap<FieldMetaData, AbstractCollection<? super X,?,?>>();

        public Managed(ClassMetaData meta, MetamodelImpl model) {
            super((Class<X>)meta.getDescribedType());
            this.model = model;
            this.meta = meta;
            FieldMetaData[] fmds = meta.getFields();
            for (FieldMetaData f : fmds) {
                int decCode = f.getDeclaredTypeCode();
                switch (decCode) {
                case JavaTypes.BOOLEAN:
                case JavaTypes.BOOLEAN_OBJ:
                    attrs.put(f, new Members.Attribute<X, Boolean>(this, f));
                    break;
                case JavaTypes.BYTE:
                case JavaTypes.BYTE_OBJ:
                    attrs.put(f, new Members.Attribute<X, Byte>(this, f));
                    break;
                case JavaTypes.CHAR:
                case JavaTypes.CHAR_OBJ:
                    attrs.put(f, new Members.Attribute<X, Character>(this, f));
                    break;
                case JavaTypes.DOUBLE:
                case JavaTypes.DOUBLE_OBJ:
                    attrs.put(f, new Members.Attribute<X, Double>(this, f));
                    break;
                case JavaTypes.FLOAT:
                case JavaTypes.FLOAT_OBJ:
                    attrs.put(f, new Members.Attribute<X, Float>(this, f));
                    break;
                case JavaTypes.INT:
                case JavaTypes.INT_OBJ:
                    attrs.put(f, new Members.Attribute<X, Integer>(this, f));
                    break;
                case JavaTypes.LONG:
                case JavaTypes.LONG_OBJ:
                    attrs.put(f, new Members.Attribute<X, Long>(this, f));
                    break;
                case JavaTypes.SHORT:
                case JavaTypes.SHORT_OBJ:
                    attrs.put(f, new Members.Attribute<X, Short>(this, f));
                    break;
                case JavaTypes.STRING:
                    attrs.put(f, new Members.Attribute<X, String>(this, f));
                    break;
                case JavaTypes.NUMBER:
                    attrs.put(f, new Members.Attribute<X, Number>(this, f));
                    break;
                case JavaTypes.DATE:
                    attrs.put(f, new Members.Attribute<X, Date>(this, f));
                    break;
                case JavaTypes.CALENDAR:
                    attrs.put(f, new Members.Attribute<X, Calendar>(this, f));
                    break;
                case JavaTypes.BIGDECIMAL:
                    attrs.put(f, new Members.Attribute<X, BigDecimal>(this, f));
                    break;
                case JavaTypes.BIGINTEGER:
                    attrs.put(f, new Members.Attribute<X, BigInteger>(this, f));
                    break;
                case JavaTypes.LOCALE:
                    attrs.put(f, new Members.Attribute<X, Locale>(this, f));
                    break;
                case JavaTypes.PC:
                    attrs.put(f, new Members.Attribute(this, f));
                    break;
                case JavaTypes.OBJECT:
                    attrs.put(f, new Members.Attribute(this, f));
                    break;
                case JavaTypes.ARRAY:
                case JavaTypes.COLLECTION:
                    switch (model.getCollectionType(f.getDeclaredType())) {
                    case COLLECTION:
                        colls.put(f, new Members.Collection(this, f));
                        break;
                    case LIST:
                        colls.put(f, new Members.List(this, f));
                        break;
                    case SET:
                        colls.put(f, new Members.Set(this, f));
                        break;
                    }
                    break;
                 case JavaTypes.MAP:
                     colls.put(f, new Members.Map(this, f));
                     break;
                 default:
                     throw new IllegalStateException(_loc.get(
                     "field-unrecognized", f.getFullName(false), decCode)
                     .getMessage());
                }
                // TODO: Account for the following codes
                // case ARRAY = 11;
                // case PC_UNTYPED = 27;
                // case OID = 29;
                // case INPUT_STREAM = 30;
                // case INPUT_READER = 31;
            }
        }
        
        public Member<? super X,?> getMember(Field field) {
            return getMember(field.getName());
        }
        
        public Member<? super X,?> getMember(String name) {
            FieldMetaData fmd = meta.getField(name);
            if (fmd == null) {
                throw new IllegalArgumentException(_loc.get("field-missing", 
                    name, meta.getDescribedType(), 
                    Arrays.toString(meta.getFieldNames())).getMessage());
            }
            if (attrs.containsKey(fmd))
                return attrs.get(fmd);
            if (colls.containsKey(fmd))
                return colls.get(fmd);
            throw new IllegalArgumentException(_loc.get("field-missing", 
            name, meta.getDescribedType(), 
            Arrays.toString(meta.getFieldNames())).getMessage());        
        }

        public <Y> Attribute<? super X, Y> getAttribute(String name,
            Class<Y> type) {
            return (Attribute<? super X, Y>) attrs.get(getField(name, type));
        }

        public <Y> Attribute<X, Y> getDeclaredAttribute(String name,
            Class<Y> type) {
            return (Attribute<X, Y>) attrs.get(getField(name, type, true));
        }

        public <E> Collection<? super X, E> getCollection(String name,
            Class<E> elementType) {
            return getCollectionMember(name, java.util.Collection.class, 
                elementType, false);
        }

        public <E> Set<? super X, E> getSet(String name, Class<E> elementType) {
            return getCollectionMember(name, java.util.Set.class, elementType, 
            		false);
        }

        public <E> List<? super X, E> getList(String name, 
            Class<E> elementType) {
            return getCollectionMember(name, java.util.List.class, elementType, 
            		false);
        }

        public <K, V> Map<? super X, K, V> getMap(String name,
            Class<K> keyType, Class<V> valueType) {
            return getMapMember(name, valueType, keyType, false);
        }

        public <E> Collection<X, E> getDeclaredCollection(String name,
            Class<E> elementType) {
            return getCollectionMember(name, java.util.Collection.class, 
            		elementType, true);
        }

        public <E> Set<X, E> getDeclaredSet(String name, Class<E> elementType) {
            return getCollectionMember(name, java.util.Set.class, elementType, 
            		true);
        }

        public <E> List<X, E> getDeclaredList(String name, 
            Class<E> elementType) {
            return getCollectionMember(name, java.util.List.class, elementType, 
            		true);
        }

        public <K, V> Map<X, K, V> getDeclaredMap(String name,
            Class<K> keyType, Class<V> valueType) {
            return getMapMember(name, valueType, keyType, true);
        }

        public java.util.Set<Attribute<? super X, ?>> getAttributes() {
            return collect(attrs.values());
        }

        public java.util.Set<Attribute<X, ?>> getDeclaredAttributes() {
            return filter(collect(attrs.values()));
        }

        public java.util.Set<AbstractCollection<? super X, ?, ?>> 
            getCollections() {
            return collect(colls.values());
        }

        public java.util.Set<AbstractCollection<X, ?, ?>> 
            getDeclaredCollections() {
            return filter(collect(colls.values()));
        }

        <T> java.util.Set<T> collect(java.util.Collection<T> values) {
            java.util.Set<T> result = new HashSet<T>();
            result.addAll(values);
            return result;
        }

        <T extends Member<X, ?>> java.util.Set<T> filter(
            java.util.Set<? extends Member<? super X, ?>> values) {
            java.util.Set<T> result = new HashSet<T>();
            for (Member<? super X, ?> m : values) {
                if (isDeclared(m))
                    result.add((T) m);
            }
            return result;
        }

        // relaxed-type: gets the model elements by String arguments. 

        public Attribute<? super X, ?> getAttribute(String name) {
            return getAttribute(name, null);
        }

        public Attribute<X, ?> getDeclaredAttribute(String name) {
            return getDeclaredAttribute(name, null);
        }

        public Collection<? super X, ?> getCollection(String name) {
            return getCollectionMember(name, java.util.Collection.class, null, 
            	false);
        }

        public Set<? super X, ?> getSet(String name) {
            return getCollectionMember(name, java.util.Set.class, null, false);
        }

        public List<? super X, ?> getList(String name) {
            return getCollectionMember(name, java.util.List.class, null, false);
        }

        public Map<? super X, ?, ?> getMap(String name) {
            return getMapMember(name, null, null, false);
        }

        public Collection<X, ?> getDeclaredCollection(String name) {
            return getCollectionMember(name, java.util.Collection.class, null, 
            	true);
        }

        public Set<X, ?> getDeclaredSet(String name) {
            return getCollectionMember(name, java.util.Set.class, null, true);
        }

        public List<X, ?> getDeclaredList(String name) {
            return getCollectionMember(name, java.util.List.class, null, true);
        }

        public Map<X, ?, ?> getDeclaredMap(String name) {
            return getMapMember(name, null, null, true);
        }

        public BindableType getBindableType() {
            return BindableType.MANAGED_TYPE;
        }

        // =====================================================================
        // Support functions
        // =====================================================================
        
        FieldMetaData getField(String name) {
            return getField(name, null, null, null, false);
        }

        FieldMetaData getField(String name, Class type) {
            return getField(name, type, null, null, false);
        }

        FieldMetaData getField(String name, Class type, boolean declaredOnly) {
            return getField(name, type, null, null, declaredOnly);
        }

        /**
         * Get the field of the given name after validating the conditions. null
         * value on any condition implies not to validate.
         * 
         * @param name simple name i.e. without the class name
         * @param type the expected type of the field.
         * @param element the expected element type of the field.
         * @param key the expected key type of the field.
         * @param declared is this field declared in this receiver
         * 
         * @exception IllegalArgumentException if any of the validation fails.
         * 
         */
        FieldMetaData getField(String name, Class<?> type, Class<?> elementType,
            Class<?> keyType, boolean decl) {
            FieldMetaData fmd =
                decl ? meta.getDeclaredField(name) : meta.getField(name);

            if (fmd == null) {
                if (decl && meta.getField(name) != null) {
                    throw new IllegalArgumentException(_loc.get(
                        "field-not-decl", name, cls,
                        meta.getField(name).getDeclaringType()).getMessage());
                } else {
                    throw new IllegalArgumentException(_loc.get(
                        "field-missing", name, meta.getDescribedType(),
                        Arrays.toString(meta.getFieldNames())).getMessage());
                }
            }
            assertType("field-type-mismatch", fmd, fmd.getDeclaredType(), type);
            assertType("field-element-type-mismatch", fmd, fmd.getElement()
                .getDeclaredType(), elementType);
            assertType("field-key-type-mismatch", fmd, fmd.getKey()
                .getDeclaredType(), keyType);
            return fmd;
        }

        void assertType(String msg, FieldMetaData fmd, Class<?> actual,
            Class<?> expected) {
            if (expected != null && !expected.isAssignableFrom(actual)) {
                throw new IllegalArgumentException(_loc.get(msg, fmd.getName(), 
                	actual, expected).getMessage());
            }
        }

        boolean isDeclared(Member<?,?> member) {
            return member.getDeclaringType() == this;
        }

        <T extends AbstractCollection, C,E> T getCollectionMember(
            String name, Class<C> target, Class<E> eType, boolean dec) {
            FieldMetaData fmd = getField(name, target, eType, null, dec);
            return (T) colls.get(fmd);
        }

        <T extends Map, K, V> T getMapMember(String name,
            Class<V> vType, Class<K> kType, boolean dec) {
            FieldMetaData fmd = getField(name, java.util.Map.class, vType, 
                kType, dec);
            return (T) colls.get(fmd);
        }

    }

    public static abstract class Identifiable<X> extends Managed<X> implements
        IdentifiableType<X> {

        public Identifiable(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }

        public <Y> Attribute<? super X, Y> getId(Class<Y> type) {
            FieldMetaData[] pks = meta.getPrimaryKeyFields();
            Class<?> idType = meta.getObjectIdType();
            return (Attribute<? super X, Y>) getAttribute(pks[0].getName(),
                idType);
        }

        public <Y> Attribute<? super X, Y> getVersion(Class<Y> type) {
            FieldMetaData vfmd = meta.getVersionField();
            return (Attribute<? super X, Y>) getAttribute(vfmd.getName());
        }

        public <Y> Attribute<X, Y> getDeclaredId(Class<Y> type) {
            FieldMetaData[] pks = meta.getPrimaryKeyFields();
            Class<?> idType = meta.getObjectIdType();
            return (Attribute<X, Y>) getDeclaredAttribute(pks[0].getName(),
                idType);
        }

        public <Y> Attribute<X, Y> getDeclaredVersion(Class<Y> type) {
            FieldMetaData vfmd = meta.getVersionField();
            return (Attribute<X, Y>) getDeclaredAttribute(vfmd.getName());
        }

        public IdentifiableType<? super X> getSupertype() {
            return (IdentifiableType<? super X>) model.type(meta
                .getPCSuperclassMetaData().getDescribedType());
        }

        public boolean hasIdAttribute() {
            return meta.getIdentityType() == ClassMetaData.ID_APPLICATION;
        }

        public Type<?> getIdType() {
            Class<?> idType = meta.getObjectIdType();
            return model.type(idType);
        }
    }

    public static class Embeddable<X> extends Managed<X> 
        implements javax.persistence.metamodel.Embeddable<X> {
        public Embeddable(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }
        
        public PersistenceType getPersistenceType() {
            return PersistenceType.EMBEDDABLE;
        }
    }

    public static class MappedSuper<X> extends Identifiable<X> implements
        MappedSuperclass<X> {

        public MappedSuper(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }
        
        public PersistenceType getPersistenceType() {
            return PersistenceType.MAPPED_SUPERCLASS;
        }

    }
    
    public static class Entity<X> extends Identifiable<X> 
        implements javax.persistence.metamodel.Entity<X> {

        public Entity(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }
        
        public PersistenceType getPersistenceType() {
            return PersistenceType.ENTITY;
        }
        
        public String getName() {
        	return meta.getTypeAlias();
        }
    }
}
