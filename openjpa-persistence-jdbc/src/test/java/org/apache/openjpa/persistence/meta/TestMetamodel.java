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
import java.lang.reflect.Modifier;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Collection;
import javax.persistence.metamodel.Entity;
import javax.persistence.metamodel.List;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Set;
import javax.persistence.metamodel.AbstractCollection.CollectionType;
import javax.persistence.metamodel.AbstractCollection.Multiplicity;
import javax.persistence.metamodel.Bindable.BindableType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests JPA 2.0 Metamodel.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestMetamodel extends SingleEMFTestCase {
    private static MetamodelImpl model;
    
    public void setUp() {
        if (model == null) {
    	super.setUp(
    			ImplicitFieldAccessMappedSuperclass.class,
    	        ImplicitFieldAccessBase.class, 
    	        ImplicitFieldAccessSubclass.class,
    	        ExplicitFieldAccess.class, 
    	        ExplicitPropertyAccess.class,
    	        Embed0.class, 
    	        Embed1.class);
    	emf.createEntityManager();
        model = (MetamodelImpl)emf.getMetamodel();
        }
    }
    
    public void testMetaModelForDomainClassesExist() {
        assertFalse(model.getEntities().isEmpty());
        assertFalse(model.getEmbeddables().isEmpty());
        assertFalse(model.getManagedTypes().isEmpty());
    }
    
    public void testMetaClassFieldsArePopulated() {
        Entity<ImplicitFieldAccessSubclass> m = 
            model.entity(ImplicitFieldAccessSubclass.class);
        Class<?> mCls = m.getJavaType();
        assertNotNull(m);
        assertSame(ImplicitFieldAccessSubclass.class, mCls);
        
        Class<?> m2Cls = model.repos.getMetaModel(mCls, true);
        assertNotNull(m2Cls);
        try {
            Field f2 = getStaticField(m2Cls, "base");
            assertNotNull(f2);
            Object value = f2.get(null);
            assertNotNull(value);
            assertTrue(Attribute.class.isAssignableFrom(value.getClass()));
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        }
    }
    
    public void testDomainClassAreCategorizedInPersistentCategory() {
    	assertCategory(PersistenceType.MAPPED_SUPERCLASS, 
    			ImplicitFieldAccessMappedSuperclass.class);
    	assertCategory(PersistenceType.ENTITY, ImplicitFieldAccessBase.class);
    	assertCategory(PersistenceType.ENTITY, ImplicitFieldAccessSubclass.class);
    	assertCategory(PersistenceType.EMBEDDABLE, Embed0.class);
    	assertCategory(PersistenceType.EMBEDDABLE, Embed1.class);
    	
        assertNotNull(model.entity(ImplicitFieldAccessBase.class));
        assertNotNull(model.entity(ImplicitFieldAccessSubclass.class));      
        assertNotNull(model.embeddable(Embed0.class));
        assertNotNull(model.embeddable(Embed1.class));
        
        java.util.Set<ManagedType<?>> managedTypes = model.getManagedTypes();
        managedTypes.removeAll(model.getEmbeddables());
        managedTypes.removeAll(model.getEntities());
        assertNotNull(model.type(ImplicitFieldAccessMappedSuperclass.class));
        assertTrue(managedTypes.contains(
        		model.type(ImplicitFieldAccessMappedSuperclass.class)));
    }
    
    public void testGetAttributeByNameAndTypeFromMetaClass() {
        ManagedType<ImplicitFieldAccessBase> e0 = model.entity(
        		ImplicitFieldAccessBase.class);
        assertNotNull(e0.getAttribute("f0"));
        assertNotNull(e0.getAttribute("f0", String.class));
        assertSame(e0.getAttribute("f0"), e0.getAttribute("f0", String.class));
        try {
            e0.getAttribute("f0", ExplicitFieldAccess.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            
        }
        ManagedType<ImplicitFieldAccessSubclass> e1 = model.entity(
        		ImplicitFieldAccessSubclass.class);
        assertNotNull(e1.getAttribute("f0"));
        
    }
    
    public void testAttributeByDeclaration() {
        ManagedType<ImplicitFieldAccessBase> e0 = 
        	model.entity(ImplicitFieldAccessBase.class);
        ManagedType<ImplicitFieldAccessSubclass> e1 = 
        	model.entity(ImplicitFieldAccessSubclass.class);
        assertNotNull(e0.getAttribute("f0"));
        assertNotNull(e1.getAttribute("f0"));
        assertNotNull(e0.getAttribute("f0", String.class));
        assertNotNull(e1.getAttribute("f0", String.class));
        assertSame(e0.getAttribute("f0"), e0.getAttribute("f0", String.class));
        assertSame(e1.getAttribute("f0"), e1.getAttribute("f0", String.class));
        assertNotSame(e0.getAttribute("f0"), e1.getAttribute("f0"));
        assertNotSame(e0.getAttribute("f0", String.class), e1.getAttribute("f0", String.class));
        assertNotNull(e0.getDeclaredAttribute("f0"));
        try {
            e1.getDeclaredAttribute("f0");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            
        }
    }
    
    public void testPCCollection() {
        ManagedType<ImplicitFieldAccessBase> e0 = model.entity(ImplicitFieldAccessBase.class);
        ManagedType<ExplicitFieldAccess> r1 = model.entity(ExplicitFieldAccess.class);
        Collection relColl 
          = e0.getCollection("collectionRelation", ExplicitFieldAccess.class);
        assertEquals(CollectionType.COLLECTION, relColl.getCollectionType());
        assertEquals(e0, relColl.getDeclaringType());
        assertEquals(r1, relColl.getElementType());
        assertEquals(java.util.Collection.class, relColl.getMemberJavaType());
        assertEquals(BindableType.COLLECTION, relColl.getBindableType());
        assertEquals(Multiplicity.ONE_TO_MANY, relColl.getMultiplicity());
    }
    
    public void testPCList() {
        ManagedType<ImplicitFieldAccessBase> e0 = model.entity(ImplicitFieldAccessBase.class);
        ManagedType<ExplicitFieldAccess> r1 = model.entity(ExplicitFieldAccess.class);
        List relList = e0.getList("listRelation", ExplicitFieldAccess.class);
        assertEquals(CollectionType.LIST, relList.getCollectionType());
        assertEquals(e0, relList.getDeclaringType());
        assertEquals(r1, relList.getElementType());
        assertEquals(java.util.List.class, relList.getMemberJavaType());
        assertEquals(BindableType.COLLECTION, relList.getBindableType());
        assertEquals(Multiplicity.ONE_TO_MANY, relList.getMultiplicity());
    }
    
    public void testPCSet() {
        ManagedType<ImplicitFieldAccessBase> e0 = 
        	model.entity(ImplicitFieldAccessBase.class);
        ManagedType<ExplicitFieldAccess> r1 = 
        	model.entity(ExplicitFieldAccess.class);
        Set relSet = e0.getSet("setRelation", ExplicitFieldAccess.class);
        assertEquals(CollectionType.SET, relSet.getCollectionType());
        assertEquals(e0, relSet.getDeclaringType());
        assertEquals(r1, relSet.getElementType());
        assertEquals(java.util.Set.class, relSet.getMemberJavaType());
        assertEquals(BindableType.COLLECTION, relSet.getBindableType());
        assertEquals(Multiplicity.ONE_TO_MANY, relSet.getMultiplicity());
    }
    
    public void testDeclaredFields() {
        ManagedType<ImplicitFieldAccessSubclass> e1 = 
        	model.entity(ImplicitFieldAccessSubclass.class);
        java.util.Set<?> all = e1.getAttributes();
        java.util.Set<?> decl = e1.getDeclaredAttributes();
        assertTrue("All fields " + all + "\r\nDeclared fields " + decl + "\r\n"+
         "expecetd not all fields as declared", all.size() > decl.size());
    }
    
    public void testNonExistentField() {
        ManagedType<ImplicitFieldAccessBase> e0 = 
        	model.entity(ImplicitFieldAccessBase.class);
        ManagedType<ImplicitFieldAccessSubclass> e1 = 
        	model.entity(ImplicitFieldAccessSubclass.class);
        assertFails(e0, "xyz", false);
        assertFails(e1, "f0", true);
        
    }
    
    void assertFails(ManagedType<?> type, String name, boolean dec) {
        try {
            Attribute<?,?> a = dec ? type.getDeclaredAttribute(name) 
                : type.getAttribute(name);
            fail("Expected to fail " + name + " on " + type);
        } catch (IllegalArgumentException e) {
            System.err.println("Expeceted:" + e);
        }
    }
    
    
    PersistenceType categorize(Class<?> c) {
        Types.Managed<?> type = (Types.Managed<?>)model.getType(c);
        ClassMetaData meta = type.meta;
        return MetamodelImpl.getPersistenceType(meta);
    }
    
    void assertCategory(PersistenceType category, Class<?> cls) {
    	assertEquals(cls.toString(), category, categorize(cls));
    }
    
    Field getStaticField(Class<?> cls, String name) {
        try {
            Field[] fds = cls.getDeclaredFields();
            for (Field f : fds) {
                int mods = f.getModifiers();
                if (f.getName().equals(name) && Modifier.isStatic(mods))
                    return f;
            }
        } catch (Exception e) {
        }
        return null;
    }
}
