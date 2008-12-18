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
package org.apache.openjpa.persistence.embed;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;


import org.apache.openjpa.persistence.test.SingleEMFTestCase;


public class TestEmbeddable extends SingleEMFTestCase {
   
    public int numEmbeddables = 1;
    public int numBasicTypes = 1;
    public int ID = 1;
    public int numProgramManagers = 2;
    public int numNickNames = 3;
    
    public int numEmployeesPerPhoneNumber = 1;
    public int numPhoneNumbersPerEmployee = 2;
    public int numEmployeesPerProgramManager = 2;
    public int numEmployees = numProgramManagers * numEmployeesPerProgramManager;
    public int numPhoneNumbers = numEmployees * numPhoneNumbersPerEmployee;
    
    public Map<Integer, PhoneNumber> phones = new HashMap<Integer, PhoneNumber>();
    public Map<Integer, Employee> employees = new HashMap<Integer, Employee>();
    public int empId = 1;
    public int phoneId = 1;
    public int pmId = 1;
    public int parkingSpotId = 1;

    public void setUp() {
        setUp(Embed.class, Embed_Coll_Embed.class, Embed_Coll_Integer.class, 
            Embed_Embed.class, Embed_Embed_ToMany.class, Embed_ToMany.class, 
            Embed_ToOne.class, EntityA_Coll_Embed_ToOne.class, 
            EntityA_Coll_String.class, EntityA_Embed_Coll_Embed.class, 
            EntityA_Embed_Coll_Integer.class, EntityA_Embed_Embed.class, 
            EntityA_Embed_Embed_ToMany.class, EntityA_Embed_ToMany.class, 
            EntityA_Embed_ToOne.class, EntityB1.class, 
            EntityA_Coll_Embed_Embed.class, ContactInfo.class,
            Employee.class, JobInfo.class, LocationDetails.class,
            ParkingSpot.class, PhoneNumber.class, ProgramManager.class,
            CLEAR_TABLES);
    }
    
    public void testEntityA_Coll_String() {
        createEntityA_Coll_String();
        queryEntityA_Coll_String();
        findEntityA_Coll_String();
    }

    public void testEntityA_Embed_ToOne() {
        createEntityA_Embed_ToOne();
        queryEntityA_Embed_ToOne();
        findEntityA_Embed_ToOne();
    }

    public void testEntityA_Coll_Embed_ToOne() {
        createEntityA_Coll_Embed_ToOne();
        queryEntityA_Coll_Embed_ToOne();
        findEntityA_Coll_Embed_ToOne();
    }

    public void testEntityA_Embed_ToMany() {
        createEntityA_Embed_ToMany();
        queryEntityA_Embed_ToMany();
        findEntityA_Embed_ToMany();
    }

    public void testEntityA_Embed_Embed_ToMany() {
        createEntityA_Embed_Embed_ToMany();
        queryEntityA_Embed_Embed_ToMany();
        findEntityA_Embed_Embed_ToMany();
    }

    public void testEntityA_Embed_Coll_Integer() {
        createEntityA_Embed_Coll_Integer();
        queryEntityA_Embed_Coll_Integer();
        findEntityA_Embed_Coll_Integer();
    }

    public void testEntityA_Embed_Embed() {
        createEntityA_Embed_Embed();
        queryEntityA_Embed_Embed();
        findEntityA_Embed_Embed();
    }

    public void testEntityA_Coll_Embed_Embed() {
        createEntityA_Coll_Embed_Embed();
        queryEntityA_Coll_Embed_Embed();
        findEntityA_Coll_Embed_Embed();
    }

    public void testEntityA_Embed_Coll_Embed() {
        createEntityA_Embed_Coll_Embed();
        queryEntityA_Embed_Coll_Embed();
        findEntityA_Embed_Coll_Embed();
    }
    
    public void testEmployee() {
        createEmployeeObj();
        queryEmployeeObj();
        findEmployeeObj();
    }

    /*
     * Create EntityA_Coll_String
     */
    public void createEntityA_Coll_String() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_String(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_String(EntityManager em, int id) {
        EntityA_Coll_String a = new EntityA_Coll_String();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numBasicTypes; i++)
            a.addNickName("nickName_" + id + i);
        em.persist(a);
    }

    /*
     * Create EntityA_Embed_ToOne
     */
    public void createEntityA_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_ToOne(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_ToOne(EntityManager em, int id) {
        EntityA_Embed_ToOne a = new EntityA_Embed_ToOne();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_ToOne embed = createEmbed_ToOne(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_ToOne createEmbed_ToOne(EntityManager em, int id) {
        Embed_ToOne embed = new Embed_ToOne();
        embed.setName1("name1");
        embed.setName2("name2");
        embed.setName3("name3");
        EntityB1 b = new EntityB1();
        b.setId(id);
        b.setName("b" + id);
        embed.setEntityB(b);
        em.persist(b);
        return embed;
    }

    /*
     * Create EntityA_Coll_Embed_ToOne
     */
    public void createEntityA_Coll_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_Embed_ToOne(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_Embed_ToOne(EntityManager em, int id) {
        EntityA_Coll_Embed_ToOne a = new EntityA_Coll_Embed_ToOne();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed_ToOne embed = createEmbed_ToOne(em, i+id);
            EntityB1 b = new EntityB1();
            b.setId(id + i);
            b.setName("b" + id + i);
            a.addEmbed1ToOne(embed);
        }
        em.persist(a);
    }

    /*
     * Create EntityA_Embed_ToMany
     */
    public void createEntityA_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_ToMany(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_ToMany(EntityManager em, int id) {
        EntityA_Embed_ToMany a = new EntityA_Embed_ToMany();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_ToMany embed = createEmbed_ToMany(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_ToMany createEmbed_ToMany(EntityManager em, int id) {
        Embed_ToMany embed = new Embed_ToMany();
        embed.setName1("name1");
        embed.setName2("name2");
        embed.setName3("name3");
        for (int i = 0; i < numEmbeddables; i++) {
            EntityB1 b = new EntityB1();
            b.setId(id + i);
            b.setName("b" + id + i);
            embed.addEntityB(b);
            em.persist(b);
        }
        return embed;
    }

   /*
     * Create EntityA_Embed_Embed_ToMany
     */
    public void createEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Embed_ToMany(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Embed_ToMany(EntityManager em, int id) {
        EntityA_Embed_Embed_ToMany a = new EntityA_Embed_Embed_ToMany();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Embed_ToMany embed = createEmbed_Embed_ToMany(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Embed_ToMany createEmbed_Embed_ToMany(EntityManager em, int id) {
        Embed_Embed_ToMany embed = new Embed_Embed_ToMany();
        embed.setIntVal1(1);
        embed.setIntVal2(2);
        embed.setIntVal3(3);
        Embed_ToMany embed_ToMany = createEmbed_ToMany(em, id);
        embed.setEmbed(embed_ToMany);
        return embed;
    }
    
    /*
     * Create EntityA_Embed_Coll_Integer
     */
    public void createEntityA_Embed_Coll_Integer() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Coll_Integer(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Coll_Integer(EntityManager em, int id) {
        EntityA_Embed_Coll_Integer a = new EntityA_Embed_Coll_Integer();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Coll_Integer embed = createEmbed_Coll_Integer(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Coll_Integer createEmbed_Coll_Integer(EntityManager em, int id) {
        Embed_Coll_Integer embed = new Embed_Coll_Integer();
        embed.setIntVal1(id*10 + 1);
        embed.setIntVal2(id*10 + 2);
        embed.setIntVal3(id*10 + 3);
        for (int i = 0; i < numBasicTypes; i++) {
            embed.addOtherIntVal(id * 100 + i);
        }
        return embed;
    }

    /*
     * Create EntityA_Embed_Embed
     */
    public void createEntityA_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Embed(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Embed(EntityManager em, int id) {
        EntityA_Embed_Embed a = new EntityA_Embed_Embed();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Embed embed = createEmbed_Embed(em, id, 0);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Embed createEmbed_Embed(EntityManager em, int id, int idx) {
        Embed_Embed embed = new Embed_Embed();
        embed.setIntVal1(id * 100 + idx * 10 + 1);
        embed.setIntVal2(id * 100 + idx * 10 + 2);
        embed.setIntVal3(id * 100 + idx * 10 + 3);
        Embed embed1 = createEmbed(id, idx);
        embed.setEmbed(embed1);
        return embed;
    }

    public Embed createEmbed(int id, int idx) {
        Embed embed = new Embed();
        embed.setIntVal1(id * 100 + idx * 10 + 4);
        embed.setIntVal2(id * 100 + idx * 10 + 5);
        embed.setIntVal3(id * 100 + idx * 10 + 6);
        return embed;
    }

    /*
     * Create EntityA_Coll_Embed_Embed
     */
    public void createEntityA_Coll_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Coll_Embed_Embed(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Coll_Embed_Embed(EntityManager em, int id) {
        EntityA_Coll_Embed_Embed a = new EntityA_Coll_Embed_Embed();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed_Embed embed = createEmbed_Embed(em, id, i);
            a.addEmbed(embed);
        }
        em.persist(a);
    }

    /*
     * Create EntityA_Embed_Coll_Embed
     */
    public void createEntityA_Embed_Coll_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createEntityA_Embed_Coll_Embed(em, ID);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createEntityA_Embed_Coll_Embed(EntityManager em, int id) {
        EntityA_Embed_Coll_Embed a = new EntityA_Embed_Coll_Embed();
        a.setId(id);
        a.setName("a" + id);
        a.setAge(id);
        Embed_Coll_Embed embed = createEmbed_Coll_Embed(em, id);
        a.setEmbed(embed);
        em.persist(a);
    }

    public Embed_Coll_Embed createEmbed_Coll_Embed(EntityManager em, int id) {
        Embed_Coll_Embed embed = new Embed_Coll_Embed();
        embed.setIntVal1(id * 10 + 1);
        embed.setIntVal2(id * 10 + 2);
        embed.setIntVal3(id * 10 + 3);
        for (int i = 0; i < numEmbeddables; i++) {
            Embed embed1 = createEmbed(id, i);
            embed.addEmbed(embed1);
        }
        return embed;
    }

    /*
     * Create Employee
     */
    public void createEmployeeObj() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        createPhoneNumbers(em);
        createEmployees(em);
        createProgramManagers(em);
        
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }
    
    public void createProgramManagers(EntityManager em) {
        empId = 1;
        for (int i = 0; i < numProgramManagers; i++)
            createProgramManager(em, pmId++);
    }
    
    public void createProgramManager(EntityManager em, int id) {
        ProgramManager pm = new ProgramManager();
        pm.setId(id);
        for (int i = 0; i < numEmployeesPerProgramManager; i++) {
            Employee e = employees.get(empId++);
            pm.addManage(e);
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobDescription("jobDescription" + e.getEmpId());
            jobInfo.setProgramManager(pm);
            e.setJobInfo(jobInfo);
        }
        em.persist(pm);
    }
    
    public void createEmployees(EntityManager em) {
        phoneId = 1;
        for (int i = 0; i < numEmployees; i++) {
            Employee e = createEmployee(em, empId++);
            employees.put(e.getEmpId(), e);
        }
    }

    public Employee createEmployee(EntityManager em, int id) {
        Employee e = new Employee();
        e.setEmpId(id);
        ContactInfo contactInfo = new ContactInfo();
        for (int i = 0; i < numPhoneNumbersPerEmployee; i++) { 
            PhoneNumber phoneNumber = phones.get(phoneId++);
            contactInfo.addPhoneNumber(phoneNumber);
            e.setContactInfo(contactInfo);
            phoneNumber.addEmployees(e);
            em.persist(phoneNumber);
        }
        ParkingSpot parkingSpot = createParkingSpot(em, parkingSpotId++);
        LocationDetails location = new LocationDetails();
        location.setOfficeNumber(id);
        location.setParkingSpot(parkingSpot);
        e.setLocationDetails(location);
        parkingSpot.setAssignedTo(e);
        for (int i = 0; i < numNickNames; i++)
            e.addNickName("nickName" + id + i);
        em.persist(parkingSpot);
        em.persist(e);
        return e;
    }
    
    public void createPhoneNumbers(EntityManager em) {
        for (int i = 0; i < numPhoneNumbers; i++) {
            PhoneNumber p = new PhoneNumber();
            p.setNumber(phoneId++);
            phones.put(p.getNumber(), p);
            em.persist(p);
        }
    }    
    
    public ParkingSpot createParkingSpot(EntityManager em, int id) {
        ParkingSpot p = new ParkingSpot();
        p.setId(id);
        p.setGarage("garage" + id);
        em.persist(p);
        return p;
    }    

    public void findEmployeeObj() {
        EntityManager em = emf.createEntityManager();
        ProgramManager pm = em.find(ProgramManager.class, 1);
        assertProgramManager(pm);

        pm = em.find(ProgramManager.class, 2);
        assertProgramManager(pm);

        Employee e = em.find(Employee.class, 1);
        assertEmployee(e);
        
        PhoneNumber p = em.find(PhoneNumber.class, 1);
        assertPhoneNumber(p);
        
        ParkingSpot ps = em.find(ParkingSpot.class, 1);
        assertParkingSpot(ps);
       
        em.close();
    }
    
    public void queryEmployeeObj() {
        queryProgramManager(emf);
        queryEmployee(emf);
        queryPhoneNumber(emf);
        queryParkingSpot(emf);
    }
    
    public void queryParkingSpot(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select p from ParkingSpot p");
        List<ParkingSpot> ps = q.getResultList();
        for (ParkingSpot p : ps){
            assertParkingSpot(p);
        }
        tran.commit();
        em.close();
    }
    
    public void queryProgramManager(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select pm from ProgramManager pm");
        List<ProgramManager> pms = q.getResultList();
        for (ProgramManager pm : pms){
            assertProgramManager(pm);
        }
        tran.commit();
        em.close();
    }

    public void queryPhoneNumber(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select p from PhoneNumber p");
        List<PhoneNumber> ps = q.getResultList();
        for (PhoneNumber p : ps){
            assertPhoneNumber(p);
        }
        tran.commit();
        em.close();
    }

    public void queryEmployee(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee e");
        List<Employee> es = q.getResultList();
        for (Employee e : es){
            assertEmployee(e);
        }
        tran.commit();
        em.close();
    }

    public void assertProgramManager(ProgramManager pm) {
        int id = pm.getId();
        Collection<Employee> es = pm.getManages();
        assertEquals(numEmployeesPerProgramManager, es.size());
        for (Employee e : es) {
            assertEmployee(e);
        }
    }
    
    public void assertEmployee(Employee e) {
        int id = e.getEmpId();
        ContactInfo c = e.getContactInfo();
        List<PhoneNumber> phones = c.getPhoneNumbers();
        assertEquals(numPhoneNumbersPerEmployee, phones.size());
        for (PhoneNumber p : phones) {
            assertPhoneNumber(p);
        }
        
        LocationDetails loc = e.getLocationDetails();
        int officeNumber = loc.getOfficeNumber();
        ParkingSpot p = loc.getParkingSpot();
        assertParkingSpot(p);
        ProgramManager pm = e.getJobInfo().getProgramManager();
        Set<String> nickNames = e.getNickNames();
        assertEquals(numNickNames, nickNames.size());
        
    }
    
    public void assertPhoneNumber(PhoneNumber p) {
        int number = p.getNumber();
        Collection<Employee> es = p.getEmployees();
        assertEquals(numEmployeesPerPhoneNumber, es.size());
    }
    
    public void assertParkingSpot(ParkingSpot p) {
        String garage = p.getGarage();
        Employee e = p.getAssignedTo();
    }
    
    /*
     * Find EntityA_Coll_String
     */
    public void findEntityA_Coll_String() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_String a = em.find(EntityA_Coll_String.class, ID);
        checkEntityA_Coll_String(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_ToOne
     */
    public void findEntityA_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_ToOne a = em.find(EntityA_Embed_ToOne.class, ID);
        checkEntityA_Embed_ToOne(a);
        em.close();
    }

    /*
     * Find EntityA_Coll_Embed_ToOne
     */
    public void findEntityA_Coll_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_Embed_ToOne a = em.find(EntityA_Coll_Embed_ToOne.class, ID);
        checkEntityA_Coll_Embed_ToOne(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_ToMany
     */
    public void findEntityA_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_ToMany a = em.find(EntityA_Embed_ToMany.class, ID);
        checkEntityA_Embed_ToMany(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Embed_ToMany
     */
    public void findEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Embed_ToMany a = em.find(EntityA_Embed_Embed_ToMany.class, ID);
        checkEntityA_Embed_Embed_ToMany(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Coll_Integer
     */
    public void findEntityA_Embed_Coll_Integer() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Coll_Integer a = em.find(EntityA_Embed_Coll_Integer.class, ID);
        checkEntityA_Embed_Coll_Integer(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Embed
     */
    public void findEntityA_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Embed a = em.find(EntityA_Embed_Embed.class, ID);
        checkEntityA_Embed_Embed(a);
        em.close();
    }

    /*
     * Find EntityA_Coll_Embed_Embed
     */
    public void findEntityA_Coll_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityA_Coll_Embed_Embed a = em.find(EntityA_Coll_Embed_Embed.class, ID);
        checkEntityA_Coll_Embed_Embed(a);
        em.close();
    }

    /*
     * Find EntityA_Embed_Coll_Embed
     */
    public void findEntityA_Embed_Coll_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityA_Embed_Coll_Embed a = em.find(EntityA_Embed_Coll_Embed.class, ID);
        checkEntityA_Embed_Coll_Embed(a);
        em.close();
    }

    /*
     * check EntityA_Coll_String
     */
    public void checkEntityA_Coll_String(EntityA_Coll_String a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Set<String> nickNames = a.getNickNames();
        for (String nickName : nickNames)
            assertEquals("nickName_" + id + "0", nickName);
    }

    /*
     * check EntityA_Embed_ToOne
     */
    public void checkEntityA_Embed_ToOne(EntityA_Embed_ToOne a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_ToOne embed = a.getEmbed();
        checkEmbed_ToOne(embed);
    }

    /*
     * check EntityA_Coll_Embed_ToOne
     */
    public void checkEntityA_Coll_Embed_ToOne(EntityA_Coll_Embed_ToOne a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Set<Embed_ToOne> embeds = a.getEmbed1ToOnes();
        for (Embed_ToOne embed : embeds)
            checkEmbed_ToOne(embed);
    }

    public void checkEmbed_ToOne(Embed_ToOne embed) {
        String name1 = embed.getName1();
        String name2 = embed.getName2();
        String name3 = embed.getName3();
        assertEquals("name1", name1);
        assertEquals("name2", name2);
        assertEquals("name3", name3);
        EntityB1 b = embed.getEntityB();
        assertEquals(1, b.getId());
        assertEquals("b" + b.getId(), b.getName());
    }

    /*
     * check EntityA_Embed_ToMany
     */
    public void checkEntityA_Embed_ToMany(EntityA_Embed_ToMany a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_ToMany embed = a.getEmbed();
        checkEmbed_ToMany(embed);
    }

    public void checkEmbed_ToMany(Embed_ToMany embed) {
        String name1 = embed.getName1();
        String name2 = embed.getName2();
        String name3 = embed.getName3();
        assertEquals("name1", name1);
        assertEquals("name2", name2);
        assertEquals("name3", name3);
        List<EntityB1> bs = embed.getEntityBs();
        for (EntityB1 b : bs) {
            assertEquals(1, b.getId());
            assertEquals("b" + b.getId() + "0", b.getName());
        }
    }

    /*
     * check EntityA_Embed_Embed_ToMany
     */
    public void checkEntityA_Embed_Embed_ToMany(EntityA_Embed_Embed_ToMany a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Embed_ToMany embed = a.getEmbed();
        checkEmbed_Embed_ToMany(embed);
    }
    
    public void checkEmbed_Embed_ToMany(Embed_Embed_ToMany embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(1, intVal1);
        assertEquals(2, intVal2);
        assertEquals(3, intVal3);
        Embed_ToMany embed1 = embed.getEmbed();
        checkEmbed_ToMany(embed1);
    }
    
    /*
     * check EntityA_Embed_Coll_Integer
     */
    public void checkEntityA_Embed_Coll_Integer(EntityA_Embed_Coll_Integer a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Coll_Integer embed = a.getEmbed();
        checkEmbed_Integers(embed);
    }

    public void checkEmbed_Integers(Embed_Coll_Integer embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(11, intVal1);
        assertEquals(12, intVal2);
        assertEquals(13, intVal3);
        Set<Integer> intVals = embed.getOtherIntVals();
        for (Integer intVal : intVals) {
            assertEquals(100, intVal.intValue());
        }
    }

    /*
     * check EntityA_Embed_Embed
     */
    public void checkEntityA_Embed_Embed(EntityA_Embed_Embed a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Embed embed = a.getEmbed();
        checkEmbed_Embed(embed);
    }

    public void checkEmbed_Embed(Embed_Embed embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(101, intVal1);
        assertEquals(102, intVal2);
        assertEquals(103, intVal3);
        Embed embed1 = embed.getEmbed();
        checkEmbed(embed1);
    }

    public void checkEmbed(Embed embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(104, intVal1);
        assertEquals(105, intVal2);
        assertEquals(106, intVal3);
    }

    /*
     * check EntityA_Coll_Embed_Embed
     */
    public void checkEntityA_Coll_Embed_Embed(EntityA_Coll_Embed_Embed a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        List<Embed_Embed> embeds = a.getEmbeds();
        for (Embed_Embed embed : embeds)
            checkEmbed_Embed(embed);
    }

    /*
     * check EntityA_Embed_Coll_Embed
     */
    public void checkEntityA_Embed_Coll_Embed(EntityA_Embed_Coll_Embed a) {
        int id = a.getId();
        String name = a.getName();
        int age = a.getAge();
        assertEquals(1, id);
        assertEquals("a" + id ,name);
        assertEquals(1, age);
        Embed_Coll_Embed embed = a.getEmbed();
        checkEmbed_Coll_Embed(embed);
    }

    public void checkEmbed_Coll_Embed(Embed_Coll_Embed embed) {
        int intVal1 = embed.getIntVal1();
        int intVal2 = embed.getIntVal2();
        int intVal3 = embed.getIntVal3();
        assertEquals(11, intVal1);
        assertEquals(12, intVal2);
        assertEquals(13, intVal3);
        List<Embed> embeds = embed.getEmbeds();
        for (Embed embed1 : embeds)
            checkEmbed(embed1);
    }

    /*
     * Query EntityA_Coll_String
     */
    public void queryEntityA_Coll_String() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_String a");
        List<EntityA_Coll_String> as = q.getResultList();
        for (EntityA_Coll_String a : as) {
            checkEntityA_Coll_String(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_ToOne
     */
    public void queryEntityA_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_ToOne a");
        List<EntityA_Embed_ToOne> as = q.getResultList();
        for (EntityA_Embed_ToOne a : as) {
            checkEntityA_Embed_ToOne(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Coll_Embed_ToOne
     */
    public void queryEntityA_Coll_Embed_ToOne() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_Embed_ToOne a");
        List<EntityA_Coll_Embed_ToOne> as = q.getResultList();
        for (EntityA_Coll_Embed_ToOne a : as) {
            checkEntityA_Coll_Embed_ToOne(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_ToMany
     */
    public void queryEntityA_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_ToMany a");
        List<EntityA_Embed_ToMany> as = q.getResultList();
        for (EntityA_Embed_ToMany a : as) {
            checkEntityA_Embed_ToMany(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Embed_ToMany
     */
    public void queryEntityA_Embed_Embed_ToMany() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Embed_ToMany a");
        List<EntityA_Embed_Embed_ToMany> as = q.getResultList();
        for (EntityA_Embed_Embed_ToMany a : as) {
            checkEntityA_Embed_Embed_ToMany(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Coll_Integer
     */
    public void queryEntityA_Embed_Coll_Integer() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Coll_Integer a");
        List<EntityA_Embed_Coll_Integer> as = q.getResultList();
        for (EntityA_Embed_Coll_Integer a : as) {
            checkEntityA_Embed_Coll_Integer(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Embed
     */
    public void queryEntityA_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Embed a");
        List<EntityA_Embed_Embed> as = q.getResultList();
        for (EntityA_Embed_Embed a : as) {
            checkEntityA_Embed_Embed(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Coll_Embed_Embed
     */
    public void queryEntityA_Coll_Embed_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Coll_Embed_Embed a");
        List<EntityA_Coll_Embed_Embed> as = q.getResultList();
        for (EntityA_Coll_Embed_Embed a : as) {
            checkEntityA_Coll_Embed_Embed(a);
        }
        tran.commit();
        em.close();
    }

    /*
     * Query EntityA_Embed_Coll_Embed
     */
    public void queryEntityA_Embed_Coll_Embed() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select a from EntityA_Embed_Coll_Embed a");
        List<EntityA_Embed_Coll_Embed> as = q.getResultList();
        for (EntityA_Embed_Coll_Embed a : as) {
            checkEntityA_Embed_Coll_Embed(a);
        }
        tran.commit();
        em.close();
    }
}
