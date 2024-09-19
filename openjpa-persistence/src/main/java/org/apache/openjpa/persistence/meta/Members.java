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

import java.util.Collection;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Persistent attribute according to JPA 2.0 metamodel.
 *
 * Implemented as a thin adapter to OpenJPA FieldMetadata. Mostly immutable.
 *
 * @author Pinaki Poddar
 *
 * @since 2.0.0
 *
 */
public class Members {
    /**
     * An attribute of a Java type. A persistent attribute is realized as a field and getter/setter
     * method of a Java class. This implementation adapts kernel's {@link FieldMetaData} construct
     * to meet the JPA API contract.
	 *
	 *
     * @param <X> The type that contains this attribute
     * @param <Y> The type of this attribute
	 */
    public static abstract class Member<X, Y> implements Attribute<X, Y>, Comparable<Member<X,Y>> {
        public final AbstractManagedType<X> owner;
        public final FieldMetaData fmd;

        /**
         * Supply immutable parts.
         *
         * @param owner the persistent type that contains this attribute
         * @param fmd the kernel's concrete representation of this attribute
         */
        protected Member(AbstractManagedType<X> owner, FieldMetaData fmd) {
            this.owner = owner;
            this.fmd = fmd;
        }

        /**
         *  Returns the managed type which declared this attribute.
         */
        @Override
        @SuppressWarnings("unchecked")
        public final ManagedType<X> getDeclaringType() {
            return (ManagedType<X>)owner.model.managedType(fmd.getDeclaringType());
        }

        /**
         *  Returns the java.lang.reflect.Member for this attribute.
         */
        @Override
        public final java.lang.reflect.Member getJavaMember() {
            return fmd.getBackingMember();
        }

        /**
         *  Gets the Java type of this attribute.
         */
        @Override
        @SuppressWarnings("unchecked")
        public final Class<Y> getJavaType() {
            return (Class<Y>)fmd.getDeclaredType();
        }

        /**
         * Gets the name of this attribute.
         */
        @Override
        public final String getName() {
            return fmd.getName();
        }

        /**
         * Returns the type that represents the type of this attribute.
         */
        @SuppressWarnings("unchecked")
        public final Type<Y> getType() {
            return owner.model.getType(isCollection()
            	 ? fmd.getElement().getDeclaredType()
            	 : fmd.getDeclaredType());
        }

        /**
         * Affirms if this attribute is an association.
         */
        @Override
        public final boolean isAssociation() {
            return fmd.isDeclaredTypePC();
        }

        /**
         * Affirms if this attribute is a collection.
         */
        @Override
        public final boolean isCollection() {
            int typeCode = fmd.getDeclaredTypeCode();
            return  typeCode == JavaTypes.COLLECTION
                 || typeCode == JavaTypes.MAP
                 || typeCode == JavaTypes.ARRAY;
        }

        /**
         *  Returns the persistent category for the attribute.
         */
        @Override
        public PersistentAttributeType getPersistentAttributeType() {
            if (fmd.isEmbeddedPC())
                return PersistentAttributeType.EMBEDDED;
            if (fmd.isElementCollection())
                return PersistentAttributeType.ELEMENT_COLLECTION;
            return PersistentAttributeType.BASIC;
        }

        @Override
        public int compareTo(Member<X, Y> o) {
            return fmd.getName().compareTo(o.fmd.getName());
        }

        @Override
        public String toString() {
        	return fmd.getFullName(true);
        }
    }


    /**
     * Represents single-valued persistent attributes.
     *
     * @param <X> The type containing the represented attribute
     * @param <T> The type of the represented attribute
     */
    public static final class SingularAttributeImpl<X, T> extends Member<X, T>
        implements SingularAttribute<X, T> {

        public SingularAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        /**
         *  Affirms if this attribute is an id attribute.
         */
        @Override
        public boolean isId() {
            return fmd.isPrimaryKey();
        }

        /**
         *  Affirms if this attribute represents a version attribute.
         */
        @Override
        public boolean isVersion() {
            return fmd.isVersion();
        }

        /**
         *  Affirms if this attribute can be null.
         */
        @Override
        public boolean isOptional() {
            return fmd.getNullValue() != FieldMetaData.NULL_EXCEPTION;
        }

        /**
         *  Categorizes bindable type represented by this attribute.
         */
        @Override
        public BindableType getBindableType() {
            return fmd.isDeclaredTypePC()
                ? BindableType.ENTITY_TYPE
                : BindableType.SINGULAR_ATTRIBUTE;
        }

        /**
         * Returns the bindable Java type of this attribute.
         *
         * If the bindable category of this attribute is PLURAL_ATTRIBUTE, the Java element type
         * is returned. If the bindable type is SINGULAR_ATTRIBUTE or ENTITY_TYPE, the Java type
         * of the represented entity or attribute is returned.
         */
        @Override
        @SuppressWarnings("unchecked")
        public Class<T> getBindableJavaType() {
            return fmd.getElement().getDeclaredType();
        }

        /**
         * Categorizes the attribute.
         */
        @Override
        public PersistentAttributeType getPersistentAttributeType() {
            if (!fmd.isDeclaredTypePC())
                return super.getPersistentAttributeType();
            if (fmd.getValue().isEmbedded() && fmd.getAssociationType() == 0) {
                return PersistentAttributeType.EMBEDDED;
            }

            return fmd.getMappedByMetaData() == null || !fmd.getType().isAssignableFrom(Collection.class)
                 ? PersistentAttributeType.ONE_TO_ONE
                 : PersistentAttributeType.ONE_TO_MANY;
        }
    }

    /**
     * Root of multi-cardinality attribute.
     *
	 * @param <X> the type that owns this member
	 * @param <C> the container type that holds this member (e.g. java.util.Set&lt;Employee&gt;)
     * @param <E> the type of the element held by this member (e.g. Employee).
     */
    public static abstract class PluralAttributeImpl<X, C, E> extends Member<X, C>
        implements PluralAttribute<X, C, E> {

        public PluralAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        /**
         * Returns the type representing the element type of the collection.
         */
        @Override
        public final Type<E> getElementType() {
            return owner.model.getType(getBindableJavaType());
        }

        /**
         *  Returns the bindable category of this attribute.
         */
        @Override
        public final BindableType getBindableType() {
            return BindableType.PLURAL_ATTRIBUTE;
        }

        /**
         * Returns the bindable Java type of this attribute.
         *
         * For PLURAL_ATTRIBUTE, the Java element type is returned.
         */
        @Override
        @SuppressWarnings("unchecked")
        public Class<E> getBindableJavaType() {
            return fmd.getElement().getDeclaredType();
        }


        @Override
        public PersistentAttributeType getPersistentAttributeType() {
            return PersistentAttributeType.ONE_TO_MANY;
        }
    }

    /**
     * Represents attributes declared as java.util.Collection&lt;E&gt;.
     */
    public static class CollectionAttributeImpl<X, E>
        extends PluralAttributeImpl<X, java.util.Collection<E>, E>
        implements CollectionAttribute<X, E> {

        public CollectionAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        @Override
        public CollectionType getCollectionType() {
            return CollectionType.COLLECTION;
        }
    }

    /**
     * Represents attributes declared as java.util.List&lt;E&gt;.
     */
    public static class ListAttributeImpl<X, E>
        extends PluralAttributeImpl<X, java.util.List<E>, E>
        implements ListAttribute<X, E> {

        public ListAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        @Override
        public CollectionType getCollectionType() {
            return CollectionType.LIST;
        }
    }

    /**
     * Represents attributes declared as java.util.Set&lt;E&gt;.
     */
    public static class SetAttributeImpl<X, E>
        extends PluralAttributeImpl<X, java.util.Set<E>, E>
        implements SetAttribute<X, E> {

        public SetAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        @Override
        public CollectionType getCollectionType() {
            return CollectionType.SET;
        }
    }

    /**
     * Represents the keys of java.util.Map&lt;K,V&gt; in managed type &lt;X&gt; as a pseudo-attribute of type
     * java.util.Set&lt;K&gt;.
     *
     * @param <X> the declaring type of the original java.util.Map&lt;K,V&gt; attribute
     * @param <K> the type of the key of the original java.util.Map&lt;K,V&gt; attribute
     */
    public static class KeyAttributeImpl<X,K> extends SetAttributeImpl<X, K> {
        public KeyAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd){
            super(owner, fmd);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<K> getBindableJavaType() {
            return (Class<K>)fmd.getKey().getDeclaredType();
        }
    }

    /**
     * Represents attributes declared as java.util.Map&lt;K,V&gt; in managed type &lt;X&gt;.
     */
    public static class MapAttributeImpl<X, K, V>
        extends PluralAttributeImpl<X, java.util.Map<K, V>, V>
        implements MapAttribute<X, K, V> {

        public MapAttributeImpl(AbstractManagedType<X> owner, FieldMetaData fmd) {
            super(owner, fmd);
        }

        @Override
        public CollectionType getCollectionType() {
            return CollectionType.MAP;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<K> getKeyJavaType() {
            return fmd.getKey().getDeclaredType();
        }

        @Override
        public Type<K> getKeyType() {
            return owner.model.getType(getKeyJavaType());
        }

        @Override
        public PersistentAttributeType getPersistentAttributeType() {
            return PersistentAttributeType.MANY_TO_MANY;
        }
    }
}
