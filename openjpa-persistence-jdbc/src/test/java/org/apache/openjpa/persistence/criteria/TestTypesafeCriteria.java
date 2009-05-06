package org.apache.openjpa.persistence.criteria;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests type-strict version of Criteria API.
 * 
 * Most of the tests build Criteria Query and then execute the query as well
 * as a reference JPQL query supplied as a string. The test is validated by
 * asserting that the resultant SQL queries for these two alternative form
 * of executing a query are the same.
 * 
 * 
 *  
 * @author Pinaki Poddar
 *
 */
public class TestTypesafeCriteria extends SQLListenerTestCase {
    CriteriaBuilder cb;
    CriteriaQuery c;
    EntityManager em;
    
    public void setUp() {
        super.setUp(Account.class);
        setDictionary();
        cb = (CriteriaBuilder)emf.getQueryBuilder();
        em = emf.createEntityManager();
        
        c = cb.create();
    }
    
    void setDictionary() {
    	JDBCConfiguration conf = (JDBCConfiguration)emf.getConfiguration();
    	DBDictionary dict = conf.getDBDictionaryInstance();
    	dict.requiresCastForComparisons = false;
    	dict.requiresCastForMathFunctions = false;
    }
    
    public void testRoot() {
    	String jpql = "select a from Account a";
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account);
        
        assertEquivalence(c, jpql);
    }
    
    public void testImplicitRoot() {
    	String jpql = "select a from Account a";
        CriteriaQuery c = cb.create();
        c.from(Account.class);
        
        assertEquivalence(c, jpql);
    }
    
    public void testEqual() {
    	String jpql = "select a from Account a where a.balance=100";
    	
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account).where(cb.equal(account.get(Account_.balance), 100));
        
        assertEquivalence(c, jpql);
    }
    
    public void testEqual2() {
    	String jpql = "select a from Account a where a.balance=a.loan";
    	
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account).where(cb.equal(account.get(Account_.balance), account.get(Account_.loan)));
        
        assertEquivalence(c, jpql);
    }
    
    public void testProj() {
    	String jpql = "select a.balance,a.loan from Account a";
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account.get(Account_.balance), account.get(Account_.loan));
        assertEquivalence(c, jpql);
        
    }

    
    public void testAbs() {
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        
    	String jpql = "select a from Account a where abs(a.balance)=100";
        c.select(account).where(cb.equal(cb.abs(account.get(Account_.balance)), 100));
        assertEquivalence(c, jpql);
    }
    
    public void testAvg() {
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        
    	String jpql = "select avg(a.balance) from Account a";
        c.select(cb.avg(account.get(Account_.balance)));
        assertEquivalence(c, jpql);
    }

    public void testInPredicate() {
    	String jpql = "select a from Account a where a.owner in ('X','Y','Z')";
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.where(cb.in(account.get(Account_.owner)).value("X").value("Y").value("Z"));
        assertEquivalence(c, jpql);
    }
    
    public void testBinaryPredicate() {
    	String jpql = "select a from Account a where a.balance>100 and a.balance<200";
    	
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account).where(cb.and(
          cb.greaterThan(account.get(Account_.balance), 100),
          cb.lessThan(account.get(Account_.balance), 200)));
        
        assertEquivalence(c, jpql);
    }
    
    public void testUnaryExpression() {
    	String jpql = "select a from Account a where a.balance=abs(a.balance)";
    	
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account).where(cb.equal(account.get(Account_.balance), cb.abs(account.get(Account_.balance))));
        
        assertEquivalence(c, jpql);
    }
    
    public void testBetweenExpression() {
    	String jpql = "select a from Account a where a.balance between 100 and 200";
    	
        CriteriaQuery c = cb.create();
        Root<Account> account = c.from(Account.class);
        c.select(account).where(cb.between(account.get(Account_.balance), 100, 200));
        
        assertEquivalence(c, jpql);
    }

    
    
    void assertEquivalence(CriteriaQuery c, String jpql) {
    	sql.clear();
    	List cList = em.createQuery(c).getResultList();
    	assertEquals(1, sql.size());
    	String cSQL = sql.get(0);
    	
    	sql.clear();
    	List jList = em.createQuery(jpql).getResultList();
    	assertEquals(1, sql.size());
    	String jSQL = sql.get(0);
    	
    	assertEquals(jSQL, cSQL);
    }
}
