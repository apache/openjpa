package org.apache.openjpa.persistence.jdbc;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.persistence.simple.Person;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestOptimisticDisable extends SQLListenerTestCase{
	
	public void setUp() {
        setUp(Person.class, CLEAR_TABLES,
        		"openjpa.jdbc.TransactionIsolation", "repeatable-read",
        		"openjpa.Optimistic", "false");
    }
	
	public void testQuery(){
		Person p1 = new Person();
        p1.setId(102);
        p1.setSurname("TestName");
        Person p2 = new Person();
        p2.setId(103);
        p2.setSurname("TestName");

        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        em.persist(p1);
        em.persist(p2);
        em.getTransaction().commit();
        em.close();
		
    	em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();       
        final Query q = em.createQuery("SELECT person FROM Person person WHERE person.surname = :surname");
        q.setParameter("surname", "TestName");
        final List<Person> persons = q.getResultList();
        tx.commit();
        em.close();
        assertEquals(2, persons.size());
	}
	
	

}
