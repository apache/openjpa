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

import javax.persistence.metamodel.AbstractCollection;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type;

import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Member according to JPA 2.0 metamodel.
 * 
 * Implemented as a thin adapter to OpenJPA FieldMetadata.
 * Mostly immutable.
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 *
 */
public class Members {
	/**
	 * Root of implementation hierarchy.
	 *
	 * @param <X> the class that owns this member
	 * @param <Y> the class of the value held by this member
	 */
    public static abstract class Member<X, Y> 
       implements javax.persistence.metamodel.Member<X, Y> {

        public final Types.Managed<X> owner;
        public final FieldMetaData fmd;

        protected Member(Types.Managed<X> owner, FieldMetaData fmd) {
            this.owner = owner;
            this.fmd = fmd;
        }

        public final ManagedType<X> getDeclaringType() {
            return (ManagedType<X>)owner.model.getType(fmd.getDeclaringType());
        }
        
        public Type<?> getType() {
            return owner.model.getType(fmd.getDeclaredType());
        }

        public final java.lang.reflect.Member getJavaMember() {
            return fmd.getBackingMember();
        }

        public Class<Y> getMemberJavaType() {
            return (Class<Y>) fmd.getDeclaredType();
        }

        public final String getName() {
            return fmd.getName();
        }

        public final boolean isAssociation() {
            return fmd.isDeclaredTypePC();
        }

        public final boolean isCollection() {
            return fmd.getDeclaredTypeCode() == JavaTypes.COLLECTION
                || fmd.getDeclaredTypeCode() == JavaTypes.MAP;
        }
        
        public String toString() {
            return fmd.getName() + ":" + getType();
        }
    }

    /**
     * Attributes are non-collection members.
     *
	 * @param <X> the class that owns this member
	 * @param <T> the class of the value held by this member
     */
    public static final class Attribute<X, T> extends Member<X, T> implements
        javax.persistence.metamodel.Attribute<X, T> {

        public Attribute(Types.Managed<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        public Multiplicity getMultiplicity() {
            throw new AbstractMethodError();
        }

        public boolean isId() {
            return fmd.isPrimaryKey();
        }

        public boolean isVersion() {
            return fmd.isVersion();
        }

        public boolean isOptional() {
            return fmd.getNullValue() != FieldMetaData.NULL_EXCEPTION;
        }
        
        public boolean isEmbedded() {
            return fmd.isEmbedded();
        }

        public Type<T> getAttributeType() {
            return owner.model.type(fmd.getDeclaredType());
        }

        public BindableType getBindableType() {
            return fmd.isDeclaredTypePC() ? BindableType.MANAGED_TYPE
                : BindableType.ATTRIBUTE;
        }

        public Class<T> getJavaType() {
            return super.getMemberJavaType();
        }
        
        public void validateMeta(Field f) {
            
        }
    }

    /**
     * Root of collection members.
     *
	 * @param <X> the class that owns this member
	 * @param <C> the container class that holds this member 
	 *            (e.g. java.util.Set)
     * @param <E> the class of the element held by this member 
     */
    public static abstract class BaseCollection<X, C, E> extends Member<X, C>
        implements AbstractCollection<X, C, E> {

        public BaseCollection(Types.Managed<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        public final Type<E> getElementType() {
            return owner.model.getType(fmd.getElement().getDeclaredType());
        }

        public final BindableType getBindableType() {
            return BindableType.COLLECTION;
        }

        public Class<E> getJavaType() {
            return fmd.getDeclaredType();
        }
        
        public Class getMemberJavaType() {
            return fmd.getElement().getDeclaredType();
        }
    }

    /**
     * Members declared as java.util.Collection<E>.
     */
    public static class Collection<X, E> extends
        BaseCollection<X, java.util.Collection<E>, E> implements
        javax.persistence.metamodel.Collection<X, E> {

        public Collection(Types.Managed<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        public Multiplicity getMultiplicity() {
            return Multiplicity.ONE_TO_MANY;
        }

        public CollectionType getCollectionType() {
            return CollectionType.COLLECTION;
        }
    }

    /**
     * Members declared as java.util.List<E>.
     */
    public static class List<X, E> extends
        BaseCollection<X, java.util.List<E>, E> implements
        javax.persistence.metamodel.List<X, E> {

        public List(Types.Managed<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        public Multiplicity getMultiplicity() {
            return Multiplicity.ONE_TO_MANY;
        }

        public CollectionType getCollectionType() {
            return CollectionType.LIST;
        }
    }

    /**
     * Members declared as java.util.Set<E>.
     */
    public static class Set<X, E> extends
        BaseCollection<X, java.util.Set<E>, E> implements
        javax.persistence.metamodel.Set<X, E> {

        public Set(Types.Managed<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        public Multiplicity getMultiplicity() {
            return Multiplicity.ONE_TO_MANY;
        }

        public CollectionType getCollectionType() {
            return CollectionType.SET;
        }
    }

    /**
     * Members declared as java.util.Map<K,V>.
     */
    public static class Map<X, K, V> extends
        BaseCollection<X, java.util.Map<K, V>, V> implements
        javax.persistence.metamodel.Map<X, K, V> {

        public Map(Types.Managed<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        public CollectionType getCollectionType() {
            return CollectionType.MAP;
        }

        public Multiplicity getMultiplicity() {
            return Multiplicity.MANY_TO_MANY;
        }

        public Class<K> getKeyJavaType() {
            return (Class<K>) fmd.getKey().getDeclaredType();
        }

        public Type<K> getKeyType() {
            return owner.model.getType(getKeyJavaType());
        }
    }
}
