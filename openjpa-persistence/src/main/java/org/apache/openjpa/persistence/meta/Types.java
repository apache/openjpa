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

import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.MappedSuperclassType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.util.OpenJPAId;

/**
 * Persistent Type according to JPA 2.0.
 * 
 * Implemented as a thin adapter to OpenJPA metadata system. Mostly immutable.
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 * 
 */
public class Types {
    protected static Localizer _loc = Localizer.forPackage(Types.class);

    /**
     * Mirrors a Java class.
     *
     * @param <X> Java class 
     */
    static abstract class BaseType<X> implements Type<X> {
        public final Class<X> cls;

        protected BaseType(Class<X> cls) {
            this.cls = cls;
        }

        public final Class<X> getJavaType() {
            return cls;
        }

        public String toString() {
            return cls.getName();
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

    /**
     *  Instances of the type ManagedType represent entity, mapped 
     *  superclass, and embeddable types.
     *
     *  @param <X> The represented type.
     */
//    public static abstract class Managed<X> extends AbstractManagedType<X> implements
//        ManagedType<X> {
        /**
         * Construct a managed type. The supplied metadata must be resolved i.e.
         * all its fields populated. Because this receiver will populate its
         * attributes corresponding to the available fields of the metadata.
         * 
         */
//        public Managed(ClassMetaData meta, MetamodelImpl model) {
//            super(meta, model);
//        }
        
         /**
         *  Return the bindable type of the represented object.
         *  @return bindable type
         */ 
//        public BindableType getBindableType() {
//            return BindableType.ENTITY_TYPE;
//        }
        
        /**
         * Return the Java type of the represented object.
         * If the bindable type of the object is PLURAL_ATTRIBUTE,
         * the Java element type is returned. If the bindable type is
         * SINGULAR_ATTRIBUTE or ENTITY_TYPE, the Java type of the
         * represented entity or attribute is returned.
         * @return Java type
         */
//        public Class<X> getBindableJavaType() {
//            throw new AbstractMethodError();
//        }
//
//    }

    public static abstract class Identifiable<X> extends AbstractManagedType<X> 
        implements IdentifiableType<X> {

        public Identifiable(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }

        /**
         *  Whether or not the identifiable type has a version attribute.
         *  @return boolean indicating whether or not the identifiable
         *          type has a version attribute
         */
        public boolean hasVersionAttribute() {
            return meta.getVersionField() != null;
        }


        /**
         *  Return the identifiable type that corresponds to the most
         *  specific mapped superclass or entity extended by the entity 
         *  or mapped superclass. 
         *  @return supertype of identifiable type or null if no such supertype
         */
        public IdentifiableType<? super X> getSupertype() {
            return (IdentifiableType<? super X>) model.type(meta
                .getPCSuperclassMetaData().getDescribedType());
        }

        public boolean hasIdAttribute() {
            return meta.getIdentityType() == ClassMetaData.ID_APPLICATION;
        }
        
        /**
         *  Whether or not the identifiable type has an id attribute.
         *  Returns true for a simple id or embedded id; returns false
         *  for an idclass.
         *  @return boolean indicating whether or not the identifiable
         *          type has a single id attribute
         */
        public boolean hasSingleIdAttribute() {
            return meta.getPrimaryKeyFields().length == 1;
        }

        /**
         *  Return the type that represents the type of the id.
         *  @return type of id
         */
        public Type<?> getIdType() {
            Class<?> idType = hasSingleIdAttribute() 
                     ? meta.getPrimaryKeyFields()[0].getDeclaredType() : meta.getObjectIdType();
            return model.getType(idType);
        }
    }

    public static class Embeddable<X> extends AbstractManagedType<X> 
        implements EmbeddableType<X> {
        public Embeddable(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }
        
        public PersistenceType getPersistenceType() {
            return PersistenceType.EMBEDDABLE;
        }
    }

    public static class MappedSuper<X> extends Identifiable<X> implements
        MappedSuperclassType<X> {

        public MappedSuper(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }
        
        public PersistenceType getPersistenceType() {
            return PersistenceType.MAPPED_SUPERCLASS;
        }

    }
    
    public static class Entity<X> extends Identifiable<X> 
        implements EntityType<X> {

        public Entity(ClassMetaData meta, MetamodelImpl model) {
            super(meta, model);
        }
        
        public PersistenceType getPersistenceType() {
            return PersistenceType.ENTITY;
        }
        
        public String getName() {
        	return meta.getTypeAlias();
        }
        /**
         *  Return the bindable type of the represented object.
         *  @return bindable type
         */ 
        public BindableType getBindableType() {
            return BindableType.ENTITY_TYPE;
        }
        
        /**
         * Return the Java type of the represented object.
         * If the bindable type of the object is PLURAL_ATTRIBUTE,
         * the Java element type is returned. If the bindable type is
         * SINGULAR_ATTRIBUTE or ENTITY_TYPE, the Java type of the
         * represented entity or attribute is returned.
         * @return Java type
         */
        public Class<X> getBindableJavaType() {
            return getJavaType();
        }
    }   
    
    /**
     * A pseudo managed type used to represent keys of a java.util.Map as a 
     * pseudo attribute.
    **/ 
    public static class PseudoEntity<X> extends AbstractManagedType<X> {

        protected PseudoEntity(Class<X> cls, MetamodelImpl model) {
            super(cls, model);
        }

        public javax.persistence.metamodel.Type.PersistenceType getPersistenceType() {
            return PersistenceType.ENTITY;
        }       
    }
}
