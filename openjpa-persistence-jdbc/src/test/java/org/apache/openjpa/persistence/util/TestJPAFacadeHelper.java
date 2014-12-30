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
package org.apache.openjpa.persistence.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.annotations.common.apps.annotApp.annotype.EmbeddedIdClass;
import org.apache.openjpa.persistence.annotations.common.apps.annotApp.annotype.EmbeddedIdEntity;
import org.apache.openjpa.persistence.derivedid.EBigDecimalID;
import org.apache.openjpa.persistence.derivedid.EBigIntegerID;
import org.apache.openjpa.persistence.derivedid.EDBigDecimalID;
import org.apache.openjpa.persistence.derivedid.EDBigIntegerID;
import org.apache.openjpa.persistence.derivedid.EDDateID;
import org.apache.openjpa.persistence.derivedid.EDSQLDateID;
import org.apache.openjpa.persistence.derivedid.EDateID;
import org.apache.openjpa.persistence.derivedid.ESQLDateID;
import org.apache.openjpa.persistence.enhance.identity.Book;
import org.apache.openjpa.persistence.enhance.identity.BookId;
import org.apache.openjpa.persistence.enhance.identity.Library;
import org.apache.openjpa.persistence.enhance.identity.MedicalHistory4;
import org.apache.openjpa.persistence.enhance.identity.Page;
import org.apache.openjpa.persistence.enhance.identity.Person4;
import org.apache.openjpa.persistence.enhance.identity.PersonId4;
import org.apache.openjpa.persistence.identity.BooleanIdEntity;
import org.apache.openjpa.persistence.identity.DoubleObjIdEntity;
import org.apache.openjpa.persistence.identity.FloatIdEntity;
import org.apache.openjpa.persistence.identity.SQLBigDecimalIdEntity;
import org.apache.openjpa.persistence.identity.SQLBigIntegerIdEntity;
import org.apache.openjpa.persistence.identity.SQLDateIdEntity;
import org.apache.openjpa.persistence.identity.StringIdEntity;
import org.apache.openjpa.persistence.identity.entityasidentity.Person;
import org.apache.openjpa.persistence.jdbc.common.apps.mappingApp.CompositeId;
import org.apache.openjpa.persistence.jdbc.common.apps.mappingApp.EntityWithCompositeId;
import org.apache.openjpa.persistence.relations.BasicEntity;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.Id;
import org.apache.openjpa.util.UserException;

public class TestJPAFacadeHelper extends SingleEMFTestCase {
    MetaDataRepository repo = null;

    public void setUp() {
        setUp(CLEAR_TABLES, EmbeddedIdEntity.class, EmbeddedIdClass.class, EBigDecimalID.class, EDBigDecimalID.class,
            EBigIntegerID.class, EDBigIntegerID.class, EDateID.class, EDDateID.class, ESQLDateID.class,
            EDSQLDateID.class, EntityWithCompositeId.class, AllFieldTypes.class, BasicEntity.class, Book.class,
            Library.class, Page.class, Person.class, DoubleObjIdEntity.class, FloatIdEntity.class,
            BooleanIdEntity.class, StringIdEntity.class, SQLBigIntegerIdEntity.class, SQLDateIdEntity.class,
            SQLBigDecimalIdEntity.class, MedicalHistory4.class, Person4.class, PersonId4.class);

        repo = emf.getConfiguration().getMetaDataRepositoryInstance();
    }

    public void testEmbeddedId() throws Exception {
        ClassMetaData cmd = repo.getMetaData(EmbeddedIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new EmbeddedIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        // Initialize and persist entity
        EmbeddedIdClass id = new EmbeddedIdClass();
        id.setPk1(1);
        id.setPk2(2);
        EmbeddedIdEntity entity = new EmbeddedIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        // Find the entity and retrieve the objectId we use internally
        EmbeddedIdEntity persistedEntity = em.find(EmbeddedIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testCompositeId() throws Exception {
        ClassMetaData cmd = repo.getMetaData(EntityWithCompositeId.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new EntityWithCompositeId());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }
        int intId = 1;
        String nameId = "CompositeEntity";
        EntityWithCompositeId entity = new EntityWithCompositeId();
        entity.setId(intId);
        entity.setName(nameId);
        CompositeId id = new CompositeId(intId, nameId);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        EntityWithCompositeId persistedEntity = em.find(EntityWithCompositeId.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testBasic() throws Exception {
        ClassMetaData cmd = repo.getMetaData(BasicEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new BasicEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, "a");
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        BasicEntity entity = new BasicEntity();

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        // Find the entity and retrieve the objectId we use internally
        BasicEntity persistedEntity = em.find(BasicEntity.class, entity.getId());
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, entity.getId()));
        Object o = JPAFacadeHelper.toOpenJPAObjectId(cmd, entity.getId());
        assertEquals(o, JPAFacadeHelper.toOpenJPAObjectId(cmd, o));
    }

    public void testIntegerId() {
        ClassMetaData cmd = repo.getMetaData(Person.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new Person());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        Integer id = Integer.valueOf(1);
        Person entity = new Person();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        Person persistedEntity = em.find(Person.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testDoubleId() {
        ClassMetaData cmd = repo.getMetaData(DoubleObjIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new DoubleObjIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        Double id = Double.valueOf(1);
        DoubleObjIdEntity entity = new DoubleObjIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        DoubleObjIdEntity persistedEntity = em.find(DoubleObjIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testFloatId() {
        ClassMetaData cmd = repo.getMetaData(FloatIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new FloatIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        Float id = Float.valueOf(1);
        FloatIdEntity entity = new FloatIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        FloatIdEntity persistedEntity = em.find(FloatIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testBooleanId() {
        ClassMetaData cmd = repo.getMetaData(BooleanIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new BooleanIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        Boolean id = Boolean.valueOf(true);
        BooleanIdEntity entity = new BooleanIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        BooleanIdEntity persistedEntity = em.find(BooleanIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testStringId() {
        ClassMetaData cmd = repo.getMetaData(StringIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new StringIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        String id = "StringId";
        StringIdEntity entity = new StringIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        StringIdEntity persistedEntity = em.find(StringIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testBigIntegerId() {
        ClassMetaData cmd = repo.getMetaData(SQLBigIntegerIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new SQLBigIntegerIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        BigInteger id = BigInteger.valueOf(1);
        SQLBigIntegerIdEntity entity = new SQLBigIntegerIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        SQLBigIntegerIdEntity persistedEntity = em.find(SQLBigIntegerIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testBigDecimalId() {
        ClassMetaData cmd = repo.getMetaData(SQLBigDecimalIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new SQLBigDecimalIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        BigDecimal id = BigDecimal.valueOf(1);
        SQLBigDecimalIdEntity entity = new SQLBigDecimalIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        SQLBigDecimalIdEntity persistedEntity = em.find(SQLBigDecimalIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testDateId() {
        ClassMetaData cmd = repo.getMetaData(SQLDateIdEntity.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new SQLDateIdEntity());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        long time = ((long) (System.currentTimeMillis() / 1000)) * 1000;
        Date id = new Date(time);
        SQLDateIdEntity entity = new SQLDateIdEntity();
        entity.setId(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        SQLDateIdEntity persistedEntity = em.find(SQLDateIdEntity.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testDerivedId() throws Exception {
        ClassMetaData cmd = repo.getMetaData(EDSQLDateID.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new EDSQLDateID());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        Date d = new Date(2014, 3, 26);
        ESQLDateID id = new ESQLDateID(d);
        EDSQLDateID entity = new EDSQLDateID(id);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(id);
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        EDSQLDateID persistedEntity = em.find(EDSQLDateID.class, d);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, d));
    }

    public void testCompositeDerivedId() throws Exception {
        ClassMetaData cmd = repo.getMetaData(Book.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new Book());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        String bookName = "Harry Potter";
        String libName = "Library Name";
        Library entity = new Library();
        entity.setName(libName);
        Book book = new Book();
        book.setName(bookName);
        entity.addBook(book);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        BookId id = new BookId();
        id.setName(bookName);
        id.setLibrary(libName);

        Book persistedEntity = em.find(Book.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testCompositeDerivedEmbeddedId() {
        ClassMetaData cmd = repo.getMetaData(MedicalHistory4.class, null, true);
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, new MedicalHistory4());
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }

        PersonId4 id = new PersonId4("First", "Last");
        Person4 person = new Person4();
        person.setId(id);
        MedicalHistory4 entity = new MedicalHistory4();
        entity.setPatient(person);
        entity.setName("MedicalHistory");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(person);
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();

        MedicalHistory4 persistedEntity = em.find(MedicalHistory4.class, id);
        StateManagerImpl smi = ((StateManagerImpl) ((PersistenceCapable) persistedEntity).pcGetStateManager());
        Object oid = smi.getObjectId();

        assertEquals(oid, JPAFacadeHelper.toOpenJPAObjectId(cmd, id));
    }

    public void testNoId() throws Exception {
        ClassMetaData cmd = repo.getMetaData(AllFieldTypes.class, null, true);
        try {
            // Don't parameterize this collection to force the JVM to use the
            // ...(ClassMetaData meta, Collection<Object> oids) method sig.
            Collection ids = new ArrayList<AllFieldTypes>();
            ids.add(new AllFieldTypes());
            JPAFacadeHelper.toOpenJPAObjectIds(cmd, ids);
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }
        try {
            JPAFacadeHelper.toOpenJPAObjectId(cmd, "a");
            fail("Didn't fail!");
        } catch (UserException re) {
            // expected
        }
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        em.getTransaction().begin();
        AllFieldTypes type = new AllFieldTypes();
        em.persist(type);
        em.getTransaction().commit();
        Object oid = em.getObjectId(type);
        assertEquals(Id.class, JPAFacadeHelper.toOpenJPAObjectId(cmd, oid).getClass());
    }
}
