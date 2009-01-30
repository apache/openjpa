package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestParameterProcessing extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(CLEAR_TABLES, Person.class, "openjpa.LogLevel=SQL=TRACE,Query=TRACE");
        createTestData();
//        super.setUp(Person.class, "openjpa.LogLevel=SQL=TRACE,Query=TRACE");
    }
    
    private void createTestData() {
        if (count(Person.class)>0) return;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Person p1 = new Person("John", "Doe", (short)45, 1964);
        Person p2 = new Person("John", "Doe", (short)42, 1967);
        Person p3 = new Person("Harry", "Doe", (short)12, 1995);
        Person p4 = new Person("Barry", "Doe", (short)22, 1985);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        em.persist(p4);
        em.getTransaction().commit();
    }
    
    public void testPositional() {
        String jpql = "select p from Person p where p.firstName=?1 and p.lastName='Doe' and p.age > ?2";
        EntityManager em = emf.createEntityManager();
        
        Query q1 = em.createQuery(jpql);
        assertEquals(JPQLParser.LANG_JPQL, OpenJPAPersistence.cast(q1).getLanguage());
        
        List result1 = q1.setParameter(1, "John")
                       .setParameter(2, (short)40)
                       .getResultList();
        
        assertEquals(2, result1.size());
        
        Query q2 = em.createQuery(jpql);
        assertEquals(QueryLanguages.LANG_PREPARED_SQL, OpenJPAPersistence.cast(q2).getLanguage());
        List result2 = q2.setParameter(1, "Harry")
                  .setParameter(2, (short)10)
                  .getResultList();
        
        assertEquals(1, result2.size());
    }
    
    public void testNamed() {
        String jpql = "select p from Person p where p.firstName=:first and p.lastName='Doe' and p.age > :age";
        EntityManager em = emf.createEntityManager();
        
        Query q1 = em.createQuery(jpql);
        assertEquals(JPQLParser.LANG_JPQL, OpenJPAPersistence.cast(q1).getLanguage());
        
        List result1 = q1.setParameter("first", "John")
                       .setParameter("age", (short)40)
                       .getResultList();
        
        assertEquals(2, result1.size());
        
        Query q2 = em.createQuery(jpql);
        assertEquals(QueryLanguages.LANG_PREPARED_SQL, OpenJPAPersistence.cast(q2).getLanguage());
        List result2 = q2.setParameter("first", "Barry")
                  .setParameter("age", (short)20)
                  .getResultList();
        
        assertEquals(1, result2.size());
    }

}
