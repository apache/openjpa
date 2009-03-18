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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestEmbeddable extends SingleEMFTestCase {
   
    public int numEmbeddables = 1;
    public int numBasicTypes = 1;
    public int numProgramManagers = 2;
    public int numNickNames = 3;
    public int numEmployeesPerPhoneNumber = 1;
    public int numPhoneNumbersPerEmployee = 2;
    public int numEmployeesPerProgramManager = 2;
    public int numEmployees = numProgramManagers * numEmployeesPerProgramManager;
    public int numPhoneNumbers = numEmployees * numPhoneNumbersPerEmployee;
    public int numDepartments = 2;
    public int numEmployeesPerDept = 2;
    public int numItems = 2;
    public int numImagesPerItem = 3;
    public int numCompany = 2;
    public int numDivisionsPerCo = 2;
    public int ID = 1;
    public int itemId = 1;
    public int compId = 1;
    public int divId = 1;
    public int vpId = 1;
    public int deptId = 1;
    public int empId = 1;
    public int phoneId = 1;
    public int pmId = 1;
    public int parkingSpotId = 1;
    public Map<Integer, PhoneNumber> phones = new HashMap<Integer, PhoneNumber>();
    public Map<Integer, Employee> employees = new HashMap<Integer, Employee>();

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
            Department1.class, Employee1.class, Department2.class,
            Employee2.class, EmployeePK2.class, Department3.class,
            Employee3.class, EmployeeName3.class, Item1.class, Item2.class,
            Item3.class, Company1.class, Company2.class, Division.class, 
            VicePresident.class,
            DROP_TABLES);
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

    public void testMapKey() {
        createObjMapKey();
        queryObjMapKey();
        findObjMapKey();
    }
    
    public void testMapKeyClass() {
        createObjMapKeyClass();
        queryObjMapKeyClass();
        findObjMapKeyClass();
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
        queryEmployeeObj(emf);
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

    public void queryEmployeeObj(EntityManagerFactory emf) {
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
    
    public void createObjMapKey() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        for (int i = 0; i < numDepartments; i++)
            createDepartment1(em, deptId++);
        for (int i = 0; i < numDepartments; i++)
            createDepartment2(em, deptId++);
        for (int i = 0; i < numDepartments; i++)
            createDepartment3(em, deptId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }
    
    public void createObjMapKeyClass() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        for (int i = 0; i < numItems; i++)
            createItem1(em, itemId++);
        for (int i = 0; i < numItems; i++)
            createItem2(em, itemId++);
        for (int i = 0; i < numItems; i++)
            createItem3(em, itemId++);
        for (int i = 0; i < numCompany; i++)
            createCompany1(em, compId++);
        for (int i = 0; i < numCompany; i++)
            createCompany2(em, compId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createItem1(EntityManager em, int id) {
        Item1 item = new Item1();
        item.setId(id);
        for (int i = 0; i < numImagesPerItem; i++) {
            item.addImage("image" + id + i, "file" + id + i);
        }
        em.persist(item);
    }
    
    public void createItem2(EntityManager em, int id) {
        Item2 item = new Item2();
        item.setId(id);
        for (int i = 0; i < numImagesPerItem; i++) {
            item.addImage("image" + id + i, "file" + id + i);
        }
        em.persist(item);
    }

    public void createItem3(EntityManager em, int id) {
        Item3 item = new Item3();
        item.setId(id);
        for (int i = 0; i < numImagesPerItem; i++) {
            item.addImage("image" + id + i, "file" + id + i);
        }
        em.persist(item);
    }

    public void createCompany1(EntityManager em, int id) {
        Company1 c = new Company1();
        c.setId(id);
        for (int i = 0; i < numDivisionsPerCo; i++) {
            Division d = createDivision(em, divId++);
            VicePresident vp = createVicePresident(em, vpId++);
            c.addToOrganization(d, vp);
            em.persist(d);
            em.persist(vp);
        }
        em.persist(c);
    }
    
    public void createCompany2(EntityManager em, int id) {
        Company2 c = new Company2();
        c.setId(id);
        for (int i = 0; i < numDivisionsPerCo; i++) {
            Division d = createDivision(em, divId++);
            VicePresident vp = createVicePresident(em, vpId++);
            c.addToOrganization(d, vp);
            em.persist(d);
            em.persist(vp);
        }
        em.persist(c);
    }

    public Division createDivision(EntityManager em, int id) {
        Division d = new Division();
        d.setId(id);
        d.setName("d" + id);
        return d;
    }
    
    public VicePresident createVicePresident(EntityManager em, int id) {
        VicePresident vp = new VicePresident();
        vp.setId(id);
        vp.setName("vp" + id);
        return vp;
    }    

    public void createDepartment1(EntityManager em, int id) {
        Department1 d = new Department1();
        d.setDeptId(id);
        Map emps = new HashMap();
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee1 e = createEmployee1(em, empId++);
            //d.addEmployee1(e);
            emps.put(e.getEmpId(), e);
            e.setDepartment(d);
            em.persist(e);
        }
        d.setEmpMap(emps);
        em.persist(d);
    }
    
    public Employee1 createEmployee1(EntityManager em, int id) {
        Employee1 e = new Employee1();
        e.setEmpId(id);
        return e;
    }
    
    public void createDepartment2(EntityManager em, int id) {
        Department2 d = new Department2();
        d.setDeptId(id);
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee2 e = createEmployee2(em, empId++);
            d.addEmployee(e);
            e.setDepartment(d);
            em.persist(e);
        }
        em.persist(d);
    }
    
    public Employee2 createEmployee2(EntityManager em, int id) {
        Employee2 e = new Employee2("e" + id, new Date());
        return e;
    }
    
    public void createDepartment3(EntityManager em, int id) {
        Department3 d = new Department3();
        d.setDeptId(id);
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee3 e = createEmployee3(em, empId++);
            d.addEmployee(e);
            e.setDepartment(d);
            em.persist(e);
        }
        em.persist(d);
    }
    
    public Employee3 createEmployee3(EntityManager em, int id) {
        Employee3 e = new Employee3();
        EmployeeName3 name = new EmployeeName3("f" + id, "l" + id);
        e.setEmpId(id);
        e.setName(name);
        return e;
    }

    public void findObjMapKey() {
        EntityManager em = emf.createEntityManager();
        Department1 d1 = em.find(Department1.class, 1);
        assertDepartment1(d1);
        
        Employee1 e1 = em.find(Employee1.class, 1);
        assertEmployee1(e1);
        
        Department2 d2 = em.find(Department2.class, 3);
        assertDepartment2(d2);
        
        Map emps = d2.getEmpMap();
        Set<EmployeePK2> keys = emps.keySet();
        for (EmployeePK2 key : keys) {
            Employee2 e2 = em.find(Employee2.class, key);
            assertEmployee2(e2);
        }
        
        Department3 d3 = em.find(Department3.class, 5);
        assertDepartment3(d3);
        
        Employee3 e3 = em.find(Employee3.class, 9);
        assertEmployee3(e3);
        
        em.close();
    }
    
    public void assertDepartment1(Department1 d) {
        int id = d.getDeptId();
        Map<Integer, Employee1> es = d.getEmpMap();
        Assert.assertEquals(2,es.size());
        Set keys = es.keySet();
        for (Object obj : keys) {
            Integer empId = (Integer) obj;
            Employee1 e = es.get(empId);
            Assert.assertEquals(empId.intValue(), e.getEmpId());
        }
    }
    
    public void assertDepartment2(Department2 d) {
        int id = d.getDeptId();
        Map<EmployeePK2, Employee2> es = d.getEmpMap();
        Assert.assertEquals(2,es.size());
        Set<EmployeePK2> keys = es.keySet();
        for (EmployeePK2 pk : keys) {
            Employee2 e = es.get(pk);
            Assert.assertEquals(pk, e.getEmpPK());
        }
    }   

    public void assertDepartment3(Department3 d) {
        int id = d.getDeptId();
        Map<EmployeeName3, Employee3> es = d.getEmployees();
        Assert.assertEquals(2,es.size());
        Set<EmployeeName3> keys = es.keySet();
        for (EmployeeName3 key : keys) {
            Employee3 e = es.get(key);
            Assert.assertEquals(key, e.getName());
        }
    }
    
    public void assertEmployee1(Employee1 e) {
        int id = e.getEmpId();
        Department1 d = e.getDepartment();
        assertDepartment1(d);
    }
    
    public void assertEmployee2(Employee2 e) {
        EmployeePK2 pk = e.getEmpPK();
        Department2 d = e.getDepartment();
        assertDepartment2(d);
    }
    
    public void assertEmployee3(Employee3 e) {
        int id = e.getEmpId();
        Department3 d = e.getDepartment();
        assertDepartment3(d);
    }

    public void queryObjMapKey() {
        queryDepartment(emf);
        queryEmployee(emf);
    }
    
    public void queryDepartment(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q1 = em.createQuery("select d from Department1 d");
        List<Department1> ds1 = q1.getResultList();
        for (Department1 d : ds1){
            assertDepartment1(d);
        }
        
        Query q2 = em.createQuery("select d from Department2 d");
        List<Department2> ds2 = q2.getResultList();
        for (Department2 d : ds2){
            assertDepartment2(d);
        }

        Query q3 = em.createQuery("select d from Department3 d");
        List<Department3> ds3 = q3.getResultList();
        for (Department3 d : ds3){
            assertDepartment3(d);
        }
        
        tran.commit();
        em.close();
    }

    public void queryEmployee(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q1 = em.createQuery("select e from Employee1 e");
        List<Employee1> es1 = q1.getResultList();
        for (Employee1 e : es1){
            assertEmployee1(e);
        }

        Query q2 = em.createQuery("select e from Employee2 e");
        List<Employee2> es2 = q2.getResultList();
        for (Employee2 e : es2){
            assertEmployee2(e);
        }

        Query q3 = em.createQuery("select e from Employee3 e");
        List<Employee3> es3 = q3.getResultList();
        for (Employee3 e : es3){
            assertEmployee3(e);
        }
        tran.commit();
        em.close();
    }
    
    public void findObjMapKeyClass() {
        EntityManager em = emf.createEntityManager();
        
        Item1 item1 = em.find(Item1.class, 1);
        assertItem1(item1);
        
        Item2 item2 = em.find(Item2.class, 3);
        assertItem2(item2);
        
        Item3 item3 = em.find(Item3.class, 5);
        assertItem3(item3);

        Company1 c1 = em.find(Company1.class, 1);
        assertCompany1(c1);
        
        Company2 c2 = em.find(Company2.class, 3);
        assertCompany2(c2);

        Division d = em.find(Division.class, 1);
        assertDivision(d);
        
        VicePresident vp = em.find(VicePresident.class, 1);
        assertVicePresident(vp);
    }
    
    public void assertItem1(Item1 item) {
        int id = item.getId();
        Map images = item.getImages();
        Assert.assertEquals(numImagesPerItem, images.size());
    }
    
    public void assertItem2(Item2 item) {
        int id = item.getId();
        Map images = item.getImages();
        Assert.assertEquals(numImagesPerItem, images.size());
    }

    public void assertItem3(Item3 item) {
        int id = item.getId();
        Map images = item.getImages();
        Assert.assertEquals(numImagesPerItem, images.size());
    }

    public void assertCompany1(Company1 c) {
        int id = c.getId();
        Map organization = c.getOrganization();
        Assert.assertEquals(2,organization.size());
    }
    
    public void assertCompany2(Company2 c) {
        int id = c.getId();
        Map organization = c.getOrganization();
        Assert.assertEquals(2,organization.size());
    }    
    
    public void assertDivision(Division d) {
        int id = d.getId();
        String name = d.getName();
    }

    public void assertVicePresident(VicePresident vp) {
        int id = vp.getId();
        String name = vp.getName();
    }
    
    public void queryObjMapKeyClass() {
        queryItem(emf);
        queryCompany(emf);
        queryDivision(emf);
        queryVicePresident(emf);
    }
    
    public void queryItem(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q1 = em.createQuery("select i from Item1 i");
        List<Item1> is1 = q1.getResultList();
        for (Item1 item : is1){
            assertItem1(item);
        }
        
        Query q2 = em.createQuery("select i from Item2 i");
        List<Item2> is2 = q2.getResultList();
        for (Item2 item : is2){
            assertItem2(item);
        }

        Query q3 = em.createQuery("select i from Item3 i");
        List<Item3> is3 = q3.getResultList();
        for (Item3 item : is3){
            assertItem3(item);
        }

        tran.commit();
        em.close();
    }

    public void queryCompany(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q1 = em.createQuery("select c from Company1 c");
        List<Company1> cs1 = q1.getResultList();
        for (Company1 c : cs1){
            assertCompany1(c);
        }
        Query q2 = em.createQuery("select c from Company2 c");
        List<Company2> cs2 = q2.getResultList();
        for (Company2 c : cs2){
            assertCompany2(c);
        }
        tran.commit();
        em.close();
    }

    public void queryDivision(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Division d");
        List<Division> ds = q.getResultList();
        for (Division d : ds){
            assertDivision(d);
        }
        tran.commit();
        em.close();
    }

    public void queryVicePresident(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select vp from VicePresident vp");
        List<VicePresident> vps = q.getResultList();
        for (VicePresident vp : vps){
            assertVicePresident(vp);
        }
        tran.commit();
        em.close();
    }        
}
