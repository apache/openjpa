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
package org.apache.openjpa.persistence.criteria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.AllowFailure;
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
 */
public class TestTypesafeCriteria extends SQLListenerTestCase {
    CriteriaBuilder cb;
    CriteriaQuery c;
    EntityManager em;
    
    public void setUp() {
         super.setUp(DROP_TABLES,
                Account.class,
                Address.class, 
                Contact.class,
                Contractor.class, 
                Course.class, 
                CreditCard.class, 
                Customer.class, 
                Department.class, 
                Employee.class, 
                Exempt.class,
                FrequentFlierPlan.class,
                Item.class,
                LineItem.class,
                Manager.class, 
                Movie.class,
                Person.class, 
                Order.class, 
                Phone.class,
                Photo.class,
                Product.class,
                Semester.class,
                Student.class, 
                TransactionHistory.class,
                VideoStore.class);
        
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

    public void testSimpleJoin() {
        CriteriaQuery c = cb.create();
//        c.from(Account.class).join(Account_.history)
    }
    
    @AllowFailure
    public void testJoins1() {
        String jpql = "SELECT c.name FROM Customer c JOIN c.orders o " + 
            "JOIN o.lineItems i WHERE i.product.productType = 'printer'";
        CriteriaQuery c = cb.create();
        Root<Customer> cust = c.from(Customer.class);
        Join<Customer, Order> order = cust.join(Customer_.orders);
        Join<Order, LineItem> item = order.join(Order_.lineItems);
        c.select(cust.get(Customer_.name))
            .where(cb.equal(item.get(LineItem_.product).
            get(Product_.productType), "printer"));
        
        assertEquivalence(c, jpql);
    }
    
    @AllowFailure
    public void testJoins2() {
        String jpql = "SELECT c FROM Customer c LEFT JOIN c.orders o " + 
            "WHERE c.status = 1";
        c = cb.create();
        Root<Customer> cust = c.from(Customer.class);
        Join<Customer, Order> order = cust.join(Customer_.orders, 
            JoinType.LEFT);
        c.where(cb.equal(cust.get(Customer_.status), 1)).select(cust);
        
        assertEquivalence(c, jpql);
    }
    
    @AllowFailure
    public void testFetchJoins() {
        String jpql = "SELECT d FROM Department LEFT JOIN FETCH d.employees " + 
            "WHERE d.deptNo = 1";
        CriteriaQuery q = cb.create();
        Root<Department> d = q.from(Department.class);
        d.fetch(Department_.employees, JoinType.LEFT);
        q.where(cb.equal(d.get(Department_.deptNo), 1)).select(d);
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testPathNavigation1() {
        String jpql = "SELECT p.vendor FROM Employee e JOIN " + 
            "e.contactInfo.phones p " + 
            "WHERE e.contactInfo.address.zipCode = '95054'";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        Join<Contact, Phone> phone = emp.join(Employee_.contactInfo).
            join(Contact_.phones);
        q.where(cb.equal(emp.get(Employee_.contactInfo).get(Contact_.address).
            get(Address_.zipCode), "95054"));    
        q.select(phone.get(Phone_.vendor));        
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testPathNavigation2() {
        String jpql = "SELECT i.name, p FROM Item i JOIN i.photos p WHERE KEY(p) " + 
            "LIKE '%egret%'";
        CriteriaQuery q = cb.create();
        Root<Item> item = q.from(Item.class);
        MapJoin<Item, String, Photo> photo = item.join(Item_.photos);
        q.select(item.get(Item_.name), photo).
            where(cb.like(photo.key(), "%egret%"));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testRestrictQueryResult1() {
        String jpql = "SELECT t FROM CreditCard c JOIN c.transactionHistory t " 
            + "WHERE c.customer.accountNum = 321987 AND INDEX(t) BETWEEN 0 " 
            + "AND 9";
        CriteriaQuery q = cb.create();
        Root<CreditCard> c = q.from(CreditCard.class);
        ListJoin<CreditCard, TransactionHistory> t = 
            c.join(CreditCard_.transactionHistory);
        q.select(t).where(cb.equal(
            c.get(CreditCard_.customer).get(Customer_.accountNum), 321987),
            cb.between(t.index(), 0, 9));
        
        assertEquivalence(q, jpql);
    }
    
    public void testRestrictQueryResult2() {
        String jpql = "SELECT o FROM Order o WHERE o.lineItems IS EMPTY";
        CriteriaQuery q = cb.create();
        Root<Order> order = q.from(Order.class);
        q.where(cb.isEmpty(order.get(Order_.lineItems))).select(order);
        
        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testExpression1() {
        String jpql = "SELECT o.quantity, o.totalCost*1.08 AS taxedCost, "  
            + "a.zipCode FROM Customer c JOIN c.orders o JOIN c.address a " 
            + "WHERE a.state = 'CA' AND a.county = 'Santa Clara";
        CriteriaQuery q = cb.create();
        Root<Customer> cust = q.from(Customer.class);
        Join<Customer, Order> order = cust.join(Customer_.orders);
        Join<Customer, Address> address = cust.join(Customer_.address);
        q.where(cb.equal(address.get(Address_.state), "CA"),
            cb.equal(address.get(Address_.county), "Santa Clara"));
        q.select(order.get(Order_.quantity), 
            cb.prod(order.get(Order_.totalCost), 1.08),
            address.get(Address_.zipCode));
        
        assertEquivalence(q, jpql);
    }
    
    public void testExpression2() {
        String jpql = "SELECT TYPE(e) FROM Employee e WHERE TYPE(e) <> Exempt";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        q.select(emp.type()).where(cb.notEqual(emp.type(), Exempt.class));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testExpression3() {
        String jpql = "SELECT w.name FROM Course c JOIN c.studentWaitList w " + 
            "WHERE c.name = 'Calculus' AND INDEX(w) = 0";
        CriteriaQuery q = cb.create();
        Root<Course> course = q.from(Course.class);
        ListJoin<Course, Student> w = course.join(Course_.studentWaitList);
        q.where(cb.equal(course.get(Course_.name), "Calculus"), 
                cb.equal(w.index(), 0)).select(w.get(Student_.name));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testExpression4() {
        String jpql = "SELECT SUM(i.price) FROM Order o JOIN o.lineItems i JOIN " +
            "o.customer c WHERE c.lastName = 'Smith' AND c.firstName = 'John'";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        Join<Order, LineItem> i = o.join(Order_.lineItems);
        Join<Order, Customer> c = o.join(Order_.customer);
        q.where(cb.equal(c.get(Customer_.lastName), "Smith"),
                cb.equal(c.get(Customer_.firstName),"John"));
        q.select(cb.sum(i.get(LineItem_.price)));
        
        assertEquivalence(q, jpql);
    }
    
    public void testExpression5() {
        String jpql = "SELECT SIZE(d.employees) FROM Department d " + 
            "WHERE d.name = 'Sales'";
        CriteriaQuery q = cb.create();
        Root<Department> d = q.from(Department.class);
        q.where(cb.equal(d.get(Department_.name), "Sales"));
        q.select(cb.size(d.get(Department_.employees)));
        
        assertEquivalence(q, jpql);
    } 
    
    public void testGeneralCaseExpression() {
        String jpql = "SELECT e.name, CASE " + 
            "WHEN e.rating = 1 THEN e.salary * 1.1 " +
            "WHEN e.rating = 2 THEN e.salary * 1.2 ELSE e.salary * 1.01 END " +
            "FROM Employee e WHERE e.department.name = 'Engineering'";
        CriteriaQuery q = cb.create();
        Root<Employee> e = q.from(Employee.class);
        q.where(cb.equal(e.get(Employee_.department).get(Department_.name), 
            "Engineering"));
        q.select(e.get(Employee_.name), 
            cb.selectCase()
                .when(cb.equal(e.get(Employee_.rating), 1),
                    cb.prod(e.get(Employee_.salary), 1.1))
                .when(cb.equal(e.get(Employee_.rating), 2), 
                    cb.prod(e.get(Employee_.salary), 1.2))
                .otherwise(cb.prod(e.get(Employee_.salary), 1.01)));    

        assertEquivalence(q, jpql);
    }    
    
    public void testSimpleCaseExpression1() {
        String jpql = "SELECT e.name, CASE e.rating " + 
            "WHEN 1 THEN e.salary * 1.1 " +
            "WHEN 2 THEN e.salary * 1.2 ELSE e.salary * 1.01 END " +
            "FROM Employee e WHERE e.department.name = 'Engineering'";
        CriteriaQuery q = cb.create();
        Root<Employee> e = q.from(Employee.class);
        q.where(cb.equal(e.get(Employee_.department).get(Department_.name), 
            "Engineering"));
        q.select(e.get(Employee_.name), 
            cb.selectCase(e.get(Employee_.rating))
                .when(1, cb.prod(e.get(Employee_.salary), 1.1))
                .when(2, cb.prod(e.get(Employee_.salary), 1.2))
                .otherwise(cb.prod(e.get(Employee_.salary), 1.01)));    

        assertEquivalence(q, jpql);
    }
    
    public void testSimpleCaseExpression2() {
        String jpql = "SELECT e.name, CASE e.rating WHEN 1 THEN 10 " +
            "WHEN 2 THEN 20 ELSE 30 END " +
            "FROM Employee e WHERE e.department.name = 'Engineering'";
        CriteriaQuery q = cb.create();
        Root<Employee> e = q.from(Employee.class);
        q.where(cb.equal(e.get(Employee_.department).get(Department_.name), 
            "Engineering"));
        q.select(e.get(Employee_.name), 
            cb.selectCase(e.get(Employee_.rating))
                .when(1, 10)
                .when(2, 20)
                .otherwise(30));    
        assertEquivalence(q, jpql);
    }    
    
    public void testLiterals() {
        String jpql = "SELECT p FROM Person p where 'Joe' MEMBER OF p.nickNames";
        CriteriaQuery q = cb.create();
        Root<Person> p = q.from(Person.class);
        q.select(p).where(cb.isMember(cb.literal("Joe"), p.get(
            Person_.nickNames)));
        
        assertEquivalence(q, jpql);
    }
    
    public void testParameters1() {
        String jpql = "SELECT c FROM Customer c Where c.status = :stat";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param = cb.parameter(Integer.class, "stat");
        q.select(c).where(cb.equal(c.get(Customer_.status), param));
        
        assertEquivalence(q, jpql, new String[]{"stat"}, new Object[] {1});
    }

    public void testParameters2() {
        String jpql = "SELECT c FROM Customer c Where c.status = :stat AND " + 
            "c.name = :name";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param1 = cb.parameter(Integer.class, "stat");
        Parameter<String> param2 = cb.parameter(String.class, "name");
        q.select(c).where(cb.and(cb.equal(c.get(Customer_.status), param1),
            cb.equal(c.get(Customer_.name), param2)));

        assertEquivalence(q, jpql, new String[] { "stat", "name" }, 
            new Object[] { 1, "test" });
    }

    public void testParameters3() {
        String jpql = "SELECT c FROM Customer c Where c.status = ?1";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param = cb.parameter(Integer.class);
        q.select(c).where(cb.equal(c.get(Customer_.status), param));
        assertEquivalence(q, jpql, new Object[] { 1 });
    }

    public void testParameters4() {
        String jpql = "SELECT c FROM Customer c Where c.status = ?1 AND " + 
            "c.name = ?2";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param1 = cb.parameter(Integer.class);
        Parameter<Integer> param2 = cb.parameter(Integer.class);
        q.select(c).where(cb.and(cb.equal(c.get(Customer_.status), param1),
            cb.equal(c.get(Customer_.name), param2)));    
        assertEquivalence(q, jpql, new Object[] { 1, "test" });
    }
    
    // do not support collection-valued input parameter
    @AllowFailure
    public void testParameters5() {
        String jpql = "SELECT c FROM Customer c Where c.status IN (:coll)";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<List> param1 = cb.parameter(List.class);
        //q.select(c).where(cb.in(c.get(Customer_.status)).value(params1));    
        List vals = new ArrayList();
        vals.add(1);
        vals.add(2);
        
        //assertEquivalence(q, jpql,  new String[] {"coll"}, new Object[] {vals});
    }
    
    @AllowFailure
    public void testSelectList1() {
        String jpql = "SELECT v.location.street, KEY(i).title, VALUE(i) FROM " + 
            "VideoStore v JOIN v.videoInventory i WHERE v.location.zipCode = " + 
            "'94301' AND VALUE(i) > 0";
        CriteriaQuery q = cb.create();
        Root<VideoStore> v = q.from(VideoStore.class);
        MapJoin<VideoStore, Movie, Integer> inv = v.join(
            VideoStore_.videoInventory);
        q.where(cb.equal(v.get(VideoStore_.location).get(Address_.zipCode), 
            "94301"),
            cb.gt(inv.value(), 0));
        q.select(v.get(VideoStore_.location).get(Address_.street), 
            inv.key().get(Movie_.title), inv.value());
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testSelectList2() {
        String jpql = "SELECT NEW CustomerDetails(c.id, c.status, o.quantity) FROM " + 
        "Customer c JOIN c.orders o WHERE o.quantity > 100";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer, Order> o = c.join(Customer_.orders);
        q.where(cb.gt(o.get(Order_.quantity), 100));
        q.select(cb.select(CustomerDetails.class, 
                c.get(Customer_.id),
                c.get(Customer_.status),
                o.get(Order_.quantity)));
        
        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSubqueries1() {
        String jpql = "SELECT goodCustomer FROM Customer goodCustomer WHERE " + 
            "goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) FROM " + 
            "Customer c)";
        CriteriaQuery q = cb.create();
        Root<Customer> goodCustomer = q.from(Customer.class);
        Subquery<Double> sq = q.subquery(Double.class);
        Root<Customer> c = sq.from(Customer.class);
        q.where(cb.lt(goodCustomer.get(Customer_.balanceOwed), 
            sq.select(cb.avg(c.get(Customer_.balanceOwed)))));
        q.select(goodCustomer);
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testSubqueries2() {
        String jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS (" + 
            "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp = " + 
            "emp.spouse)";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        Subquery<Employee> sq = q.subquery(Employee.class);
        Root<Employee> spouseEmp = sq.from(Employee.class);
        sq.select(spouseEmp);
        sq.where(cb.equal(spouseEmp, emp.get(Employee_.spouse)));
        q.where(cb.exists(sq));
        q.select(emp).distinct(true);
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testSubqueries3() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL (" + 
            "SELECT m.salary FROM Manager m WHERE m.department = " + 
            "emp.department)";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        q.select(emp);
        Subquery<BigDecimal> sq = q.subquery(BigDecimal.class);
        Root<Manager> m = sq.from(Manager.class);
        sq.select(m.get(Manager_.salary));
        sq.where(cb.equal(m.get(Manager_.department), emp.get(
            Employee_.department)));
        q.where(cb.gt(emp.get(Employee_.salary), cb.all(sq)));
    
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testSubqueries4() {
        String jpql = "SELECT c FROM Customer c WHERE " + 
            "(SELECT COUNT(o) FROM c.orders o) > 10";
        CriteriaQuery q = cb.create();
        Root<Customer> c1 = q.from(Customer.class);
        q.select(c1);
        Subquery<Long> sq3 = q.subquery(Long.class);
        Root<Customer> c2 = sq3.correlate(c1); 
        Join<Customer,Order> o = c2.join(Customer_.orders);
        q.where(cb.gt(sq3.select(cb.count(o)), 10));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testSubqueries5() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL (" + 
            "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o);
        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Order> osq = sq.correlate(o);
        Join<Order,Customer> c = osq.join(Order_.customer);
        Join<Customer,Account> a = c.join(Customer_.accounts);
        sq.select(a.get(Account_.balance));
        q.where(cb.lt(cb.literal(10000), cb.all(sq)));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testSubqueries6() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < " +
            "ALL (SELECT a.balance FROM c.accounts a)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o);
        Join<Order,Customer> c = o.join(Order_.customer);
        Subquery<Integer> sq = q.subquery(Integer.class);
        Join<Order,Customer> csq = sq.correlate(c);
        Join<Customer,Account> a = csq.join(Customer_.accounts);
        sq.select(a.get(Account_.balance));
        q.where(cb.lt(cb.literal(10000), cb.all(sq)));
        
        assertEquivalence(q, jpql);
    }
    
    public void testGroupByAndHaving() {
        String jpql = "SELECT c.status, AVG(c.filledOrderCount), COUNT(c) FROM "
            + "Customer c GROUP BY c.status HAVING c.status IN (1, 2)";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.groupBy(c.get(Customer_.status));
        q.having(cb.in(c.get(Customer_.status)).value(1).value(2));
        q.select(c.get(Customer_.status), 
            cb.avg(c.get(Customer_.filledOrderCount)),
            cb.count(c));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testOrdering1() {
        String jpql = "SELECT o FROM Customer c JOIN c.orders o " + 
            "JOIN c.address a WHERE a.state = 'CA' ORDER BY o.quantity DESC, " +
            "o.totalCost";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer,Order> o = c.join(Customer_.orders);
        Join<Customer,Address> a = c.join(Customer_.address);
        q.where(cb.equal(a.get(Address_.state), "CA")); 
        q.orderBy(cb.desc(o.get(Order_.quantity)), 
            cb.asc(o.get(Order_.totalCost)));
        q.select(o);
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testOrdering2() {
        String jpql = "SELECT o.quantity, a.zipCode FROM Customer c " + 
            "JOIN c.orders JOIN c.address a WHERE a.state = 'CA' " + 
            "ORDER BY o.quantity, a.zipCode";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class); 
        Join<Customer,Order> o = c.join(Customer_.orders);
        Join<Customer,Address> a = c.join(Customer_.address);
        q.where(cb.equal(a.get(Address_.state), "CA"));
        q.orderBy(cb.asc(o.get(Order_.quantity)), cb.asc(a.get(
            Address_.zipCode)));
        q.select(o.get(Order_.quantity), a.get(Address_.zipCode));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testOrdering3() {
        String jpql = "SELECT o.quantity, o.cost * 1.08 AS taxedCost, " + 
            "a.zipCode FROM Customer c JOIN c.orders o JOIN c.address a " + 
            "WHERE a.state = 'CA' AND a.county = 'Santa Clara' " + 
            "ORDER BY o.quantity, taxedCost, a.zipCode";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer,Order> o = c.join(Customer_.orders);
        Join<Customer,Address> a = c.join(Customer_.address);
        q.where(cb.equal(a.get(Address_.state), "CA"),
                cb.equal(a.get(Address_.county), "Santa Clara"));
        q.orderBy(cb.asc(o.get(Order_.quantity)),
                cb.asc(cb.prod(o.get(Order_.totalCost), 1.08)),
                cb.asc(a.get(Address_.zipCode)));
        q.select(o.get(Order_.quantity), 
                cb.prod(o.get(Order_.totalCost), 1.08),
                a.get(Address_.zipCode));

        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testOrdering4() {
        String jpql = "SELECT c FROM Customer c "
                + "ORDER BY c.name DESC, c.status";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.orderBy(cb.desc(c.get(Customer_.name)), cb.asc(c
                .get(Customer_.status)));
        q.select(c);

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testOrdering5() {
        String jpql = "SELECT c.firstName, c.lastName, c.balanceOwed " + 
            "FROM Customer c ORDER BY c.name DESC, c.status";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.orderBy(cb.desc(c.get(Customer_.name)), cb.asc(c
            .get(Customer_.status)));
        q.select(c.get(Customer_.firstName), c.get(Customer_.lastName),
            c.get(Customer_.balanceOwed));

        assertEquivalence(q, jpql);
    }

    void assertEquivalence(CriteriaQuery c, String jpql, 
        String[] paramNames, Object[] params) {
        sql.clear();
        Query q = em.createQuery(c);
        for (int i = 0; i < paramNames.length; i++) {
            q.setParameter(paramNames[i], params[i]);
        }
        List cList = q.getResultList();
        assertEquals(1, sql.size());
        String cSQL = sql.get(0);
        
        sql.clear();
        q = em.createQuery(jpql);
        for (int i = 0; i < paramNames.length; i++) {
            q.setParameter(paramNames[i], params[i]);
        }
        List jList = q.getResultList();
        assertEquals(1, sql.size());
        String jSQL = sql.get(0);
        
        assertEquals(jSQL, cSQL);
    }

    void assertEquivalence(CriteriaQuery c, String jpql, 
        Object[] params) {
        sql.clear();
        Query q = em.createQuery(c);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i+1, params[i]);
        }
        List cList = q.getResultList();
        assertEquals(1, sql.size());
        String cSQL = sql.get(0);

        sql.clear();
        q = em.createQuery(jpql);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i+1, params[i]);
        }
        List jList = q.getResultList();
        assertEquals(1, sql.size());
        String jSQL = sql.get(0);

        assertEquals(jSQL, cSQL);
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
