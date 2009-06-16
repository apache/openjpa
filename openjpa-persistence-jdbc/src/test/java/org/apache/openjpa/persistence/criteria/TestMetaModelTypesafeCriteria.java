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
import java.util.List;

import javax.persistence.Parameter;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.Embeddable;
import javax.persistence.metamodel.Entity;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Set;

import org.apache.openjpa.persistence.test.AllowFailure;

public class TestMetaModelTypesafeCriteria extends CriteriaTest {
    protected Entity<Account> account_ = null;
    protected Embeddable<Address> address_ = null;
    protected Embeddable<Contact> contact_ = null;
    protected Entity<Course> course_ = null;
    protected Entity<CreditCard> creditCard_ = null;
    protected Entity<Customer> customer_ = null;
    protected Entity<Department> department_ = null;
    protected Entity<Employee> employee_ = null;
    protected Entity<Exempt> exempt_ = null;
    protected Entity<Item> item_ = null;
    protected Entity<LineItem> lineItem_ = null;
    protected Entity<Manager> manager_ = null;
    protected Entity<Movie> movie_ = null;
    protected Entity<Order> order_ = null;
    protected Entity<Person> person_ = null;
    protected Entity<Phone> phone_ = null;
    protected Entity<Photo> photo_ = null;
    protected Entity<Product> product_ = null;
    protected Entity<Semester> semester_ = null;
    protected Entity<Student> student_ = null;
    protected Entity<TransactionHistory> transactionHistory_ = null;
    protected Entity<VideoStore> videoStore_ = null;

    public void setUp() {
        super.setUp();

        Metamodel mm = em.getMetamodel();
        account_ = mm.entity(Account.class);
        address_ = mm.embeddable(Address.class);
        assertNotNull(address_);
        contact_ = mm.embeddable(Contact.class);
        course_ = mm.entity(Course.class);
        creditCard_ = mm.entity(CreditCard.class);
        customer_ = mm.entity(Customer.class);
        department_ = mm.entity(Department.class);
        employee_ = mm.entity(Employee.class);
        exempt_ = mm.entity(Exempt.class);
        item_ = mm.entity(Item.class);
        lineItem_ = mm.entity(LineItem.class);
        manager_ = mm.entity(Manager.class);
        movie_ = mm.entity(Movie.class);
        order_ = mm.entity(Order.class);
        person_ = mm.entity(Person.class);
        phone_ = mm.entity(Phone.class);
        photo_ = mm.entity(Photo.class);
        product_ = mm.entity(Product.class);
        semester_ = mm.entity(Semester.class);
        student_ = mm.entity(Student.class);
        transactionHistory_ = mm.entity(TransactionHistory.class);
        videoStore_ = mm.entity(VideoStore.class);
    }

    public void testStringEqualExpression() {
        String jpql = "select c from Customer c " 
                    + "where c.name='Autowest Toyota'";
        
        CriteriaQuery q = cb.create();
        Root<Customer> customer = q.from(Customer.class);
        q.select(customer)
         .where(cb.equal(
                customer.get(customer_.getAttribute("name", String.class)), 
                "Autowest Toyota"));

        assertEquivalence(q, jpql);
    }

    public void testSetAndListJoins() {
        String jpql = "SELECT c.name FROM Customer c " 
                    + "JOIN c.orders o JOIN o.lineItems i " 
                    + "WHERE i.product.productType = 'printer'";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.join(customer_.getSet("orders",
                Order.class));
        ListJoin<Order, LineItem> i = o.join(order_.getList("lineItems",
                LineItem.class));
        q.select(c.get(Customer_.name)).where(
                cb.equal(i.get(lineItem_.getAttribute("product", Product.class))
                    .get(product_.getAttribute("productType", String.class)),
                    "printer"));

        assertEquivalence(q, jpql);
    }
    
    public void testLeftSetJoin() {
        String jpql = "SELECT c FROM Customer c "
                    + "LEFT JOIN c.orders o "
                    + "WHERE c.status = 1";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.join(customer_.getSet("orders",
                Order.class), JoinType.LEFT);
        q.where(cb.equal(
                c.get(customer_.getAttribute("status", Integer.class)), 
                1));

        assertEquivalence(q, jpql);
    }

    @AllowFailure(message="FetchJoin not implemented")
    public void testFetchJoins() {
        String jpql = "SELECT d FROM Department LEFT JOIN FETCH d.employees "
                + "WHERE d.deptNo = 1";
        CriteriaQuery q = cb.create();
        Root<Department> d = q.from(Department.class);
        d.fetch(department_.getSet("employees", Employee.class), JoinType.LEFT);
        q.where(
                cb.equal(d.get(department_
                        .getAttribute("deptNo", Integer.class)), 1)).select(d);

        assertEquivalence(q, jpql);
    }

    public void testPathNavigation() {
        String jpql = "SELECT p.vendor FROM Employee e "
                    + "JOIN e.contactInfo.phones p  "
                    + "WHERE e.contactInfo.address.zipCode = '95054'";
        
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        Join<Contact, Phone> phone = emp.join(
                employee_.getAttribute("contactInfo", Contact.class)).join(
                contact_.getList("phones", Phone.class));
        q.where(cb.equal(emp.get(
                employee_.getAttribute("contactInfo", Contact.class)).get(
                contact_.getAttribute("address", Address.class)).get(
                address_.getAttribute("zipCode", String.class)), "95054"));
        q.select(phone.get(phone_.getAttribute("vendor", String.class)));

        assertEquivalence(q, jpql);
    }
    
    public void testKeyPathNavigation() {
        String jpql = "SELECT i.name, p FROM Item i JOIN i.photos p " 
                    + "WHERE KEY(p) LIKE '%egret%'";

        CriteriaQuery q = cb.create();
        Root<Item> item = q.from(Item.class);
        MapJoin<Item, String, Photo> photo = item.join(
                item_.getMap("photos", String.class, Photo.class));
        q.select(item.get(item_.getAttribute("name", String.class)), photo)
                .where(cb.like(photo.key(), "%egret%"));

        assertEquivalence(q, jpql);
    }

    public void testIndexExpression() {
        String jpql = "SELECT t FROM CreditCard c JOIN c.transactionHistory t "
                + "WHERE c.customer.accountNum = 321987 AND INDEX(t) BETWEEN 0 "
                + "AND 9";
        
        CriteriaQuery cq = cb.create();
        Root<CreditCard> c = cq.from(CreditCard.class);
        ListJoin<CreditCard, TransactionHistory> t = c.join(creditCard_
                .getList("transactionHistory", TransactionHistory.class));
        cq.select(t).where(
                cb.equal(c.get(
                        creditCard_.getAttribute("customer", Customer.class))
                        .get(customer_.getAttribute("accountNum", Long.class)),
                        321987), cb.between(t.index(), 0, 9));

        assertEquivalence(cq, jpql);
    }
    
    @AllowFailure(message="as() not implemented")
    public void testIsEmptyExpressionOnJoin() {
        String jpql = "SELECT o FROM Order o WHERE o.lineItems IS EMPTY"; 
        CriteriaQuery q = cb.create(); 
        Root<Order> o = q.from(Order.class);
        ListJoin<Order,LineItem> lineItems =
        o.join(order_.getList("lineItems", LineItem.class));
        q.where(cb.isEmpty(lineItems.as(List.class))); 
        q.select(o);
        assertEquivalence(q, jpql);
    }

    public void testFunctionalExpressionInProjection() {
        String jpql = "SELECT o.quantity, o.totalCost*1.08 AS taxedCost, "
                + "a.zipCode FROM Customer c JOIN c.orders o JOIN c.address a "
                + "WHERE a.state = 'CA' AND a.county = 'Santa Clara'";
        
        CriteriaQuery q = cb.create();
        Root<Customer> cust = q.from(Customer.class);
        Join<Customer, Order> order = cust.join(customer_.getSet("orders",
                Order.class));
        Join<Customer, Address> address = cust.join(customer_.getAttribute(
                "address", Address.class));
        q.where(cb.equal(address.get(address_.getAttribute("state",
                String.class)), "CA"), cb.equal(address.get(address_
                .getAttribute("county", String.class)), "Santa Clara"));
        q.select(order.get(order_.getAttribute("quantity", Integer.class)),
                cb.prod(order.get(order_
                        .getAttribute("totalCost", Double.class)), 1.08),
                address.get(address_.getAttribute("zipCode", String.class)));

        assertEquivalence(q, jpql);
    }
    
    public void testTypeExpression() {
        String jpql = "SELECT TYPE(e) FROM Employee e WHERE TYPE(e) <> Exempt";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        q.select(emp.type()).where(cb.notEqual(emp.type(), Exempt.class));

        assertEquivalence(q, jpql);
    }
    
    public void testJoinAndIndexExpression() {
       String jpql = "SELECT w.name FROM Course c JOIN c.studentWaitList w "
                + "WHERE c.name = 'Calculus' AND INDEX(w) = 0";
        
       CriteriaQuery q = cb.create();
        Root<Course> course = q.from(Course.class);
        ListJoin<Course, Student> w = course.join(course_.getList(
                "studentWaitList", Student.class));
        q.where(
                cb.equal(
                        course.get(course_.getAttribute("name", String.class)),
                        "Calculus"), cb.equal(w.index(), 0)).select(
                w.get(student_.getAttribute("name", String.class)));

        assertEquivalence(q, jpql);
    }
    
    public void testAggregateExpressionInProjection() {
        String jpql = "SELECT SUM(i.price) " 
             + "FROM Order o JOIN o.lineItems i JOIN o.customer c "
             + "WHERE c.lastName = 'Smith' AND c.firstName = 'John'";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        Join<Order, LineItem> i = o.join(order_.getList("lineItems",
                LineItem.class));
        Join<Order, Customer> c = o.join(order_.getAttribute("customer",
                Customer.class));
        q.where(cb
                .equal(c.get(customer_.getAttribute("lastName", String.class)),
                        "Smith"), cb.equal(c.get(customer_.getAttribute(
                "firstName", String.class)), "John"));
        q.select(cb.sum(i.get(lineItem_.getAttribute("price", Double.class))));

        assertEquivalence(q, jpql);
    }
    
    public void testSizeExpressionInProjection() {
        String jpql = "SELECT SIZE(d.employees) FROM Department d " 
         + "WHERE d.name = 'Sales'"; 
        
        CriteriaQuery q = cb.create(); 
        Root<Department> d = q.from(Department.class);
        q.where(cb.equal(
                d.get(department_.getAttribute("name", String.class)), 
                "Sales"));
        Set<Department, Employee> employees = 
            department_.getDeclaredSet("employees", Employee.class);
        q.select(cb.size(d.get(employees)));
        
        assertEquivalence(q, jpql);
        
    }
    
    public void testCaseExpression() {
        String jpql = "SELECT e.name, "
             + "CASE WHEN e.rating = 1 THEN e.salary * 1.1 "
             + "WHEN e.rating = 2 THEN e.salary * 1.2 ELSE e.salary * 1.01 END "
             + "FROM Employee e WHERE e.department.name = 'Engineering'";
        
        CriteriaQuery q = cb.create();
        Root<Employee> e = q.from(Employee.class);
        q.where(cb.equal(e.get(
                        employee_.getAttribute("department", Department.class))
                        .get(department_.getAttribute("name", String.class)),
                        "Engineering"));
        q.select(e.get(employee_.getAttribute("name", String.class)), cb
                .selectCase().when(
                        cb.equal(e.get(employee_.getAttribute("rating",
                                Integer.class)), 1),
                        cb.prod(e.get(employee_.getAttribute("salary",
                                Long.class)), 1.1)).when(
                        cb.equal(e.get(employee_.getAttribute("rating",
                                Integer.class)), 2),
                        cb.prod(e.get(employee_.getAttribute("salary",
                                Long.class)), 1.2)).otherwise(
                        cb.prod(e.get(employee_.getAttribute("salary",
                                Long.class)), 1.01)));

        assertEquivalence(q, jpql);
    }

    public void testMemberOfExpression() {
      String jpql = "SELECT p FROM Person p where 'Joe' MEMBER OF p.nickNames";
     
      CriteriaQuery q = cb.create(); 
      Root<Person> p = q.from(Person.class);
      q.select(p).where(cb.isMember(cb.literal("Joe"), 
             p.get(person_.getDeclaredSet("nickNames", String.class))));
     
       assertEquivalence(q, jpql); 
     }

    public void testParameters() {
        String jpql = "SELECT c FROM Customer c Where c.status = :stat";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param = cb.parameter(Integer.class, "stat");
        q.select(c).where(cb.equal(
            c.get(customer_.getAttribute("status", Integer.class)), param));

        assertEquivalence(q, jpql, new String[] { "stat" }, new Object[] { 1 });
    }

    @AllowFailure(message="Generates invalid SQL")
    public void testKeyExpressionInSelectList() {
        String jpql = "SELECT v.location.street, KEY(i).title, VALUE(i) FROM "
                + "VideoStore v JOIN v.videoInventory i "
                + "WHERE v.location.zipCode = " + "'94301' AND VALUE(i) > 0";
        CriteriaQuery q = cb.create();
        Root<VideoStore> v = q.from(VideoStore.class);
        MapJoin<VideoStore, Movie, Integer> inv = v.join(videoStore_.getMap(
                "videoInventory", Movie.class, Integer.class));
        q.where(cb.equal(v.get(
                videoStore_.getAttribute("location", Address.class)).get(
                address_.getAttribute("zipCode", String.class)), "94301"), cb
                .gt(inv.value(), 0));
        q.select(v.get(videoStore_.getAttribute("location", Address.class))
                .get(address_.getAttribute("street", String.class)), inv.key()
                .get(movie_.getAttribute("title", String.class)), inv.value());

        assertEquivalence(q, jpql);
    }
    
    public void testConstructorInSelectList() {
        String jpql = "SELECT NEW CustomerDetails(c.id, c.status, o.quantity) "
                    + "FROM Customer c JOIN c.orders o WHERE o.quantity > 100";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.join(
                customer_.getSet("orders", Order.class));
        q.where(cb.gt(o.get(order_.getAttribute("quantity", Integer.class)),
                100));
        q.select(cb.select(CustomerDetails.class, 
                c.get(customer_.getAttribute("id", Long.class)), 
                c.get(customer_.getAttribute("status", Integer.class)), 
                o.get(order_.getAttribute("quantity",  Integer.class))));

        assertEquivalence(q, jpql);
    }

    public void testUncorrelatedSubqueryWithAggregateProjection() {
        String jpql = "SELECT goodCustomer FROM Customer goodCustomer WHERE "
                + "goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) FROM "
                + "Customer c)";
        CriteriaQuery q = cb.create();
        Root<Customer> goodCustomer = q.from(Customer.class);
        Subquery<Double> sq = q.subquery(Double.class);
        Root<Customer> c = sq.from(Customer.class);
        q.where(cb.lt(goodCustomer.get(customer_.getAttribute("balanceOwed",
                Integer.class)), sq.select(cb.avg(c.get(customer_.getAttribute(
                "balanceOwed", Integer.class))))));
        q.select(goodCustomer);

        assertEquivalence(q, jpql);
    }
    
    public void testSubqueryWithExistsClause() {
        String jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS ("
                + "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp = "
                + "emp.spouse)";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        Subquery<Employee> sq = q.subquery(Employee.class);
        Root<Employee> spouseEmp = sq.from(Employee.class);
        sq.select(spouseEmp);
        sq.where(cb.equal(spouseEmp, emp.get(employee_.getAttribute("spouse",
                Employee.class))));
        q.where(cb.exists(sq));
        q.select(emp).distinct(true);

        assertEquivalence(q, jpql);
    }

    public void testSubqueryWithAllClause() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL ("
                + "SELECT m.salary FROM Manager m WHERE m.department ="
                + " emp.department)";
        
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        q.select(emp);
        Subquery<BigDecimal> sq = q.subquery(BigDecimal.class);
        Root<Manager> m = sq.from(Manager.class);
        sq.select(m.get(manager_.getAttribute("salary", BigDecimal.class)));
        sq.where(cb.equal(m.get(manager_.getAttribute("department",
                Department.class)), emp.get(employee_.getAttribute(
                "department", Department.class))));
        q.where(cb.gt(emp.get(employee_.getAttribute("salary", Long.class)), cb
                .all(sq)));

        assertEquivalence(q, jpql);
    }
    
    public void testCorrelatedSubqueryWithCount() {
        String jpql = "SELECT c FROM Customer c WHERE "
                + "(SELECT COUNT(o) FROM c.orders o) > 10";
        CriteriaQuery q = cb.create();
        Root<Customer> c1 = q.from(Customer.class);
        q.select(c1);
        Subquery<Long> sq3 = q.subquery(Long.class);
        Root<Customer> c2 = sq3.correlate(c1);
        Join<Customer, Order> o = c2.join(customer_.getSet("orders",
                Order.class));
        q.where(cb.gt(sq3.select(cb.count(o)), 10));

        assertEquivalence(q, jpql);
    }
    
    public void testCorrelatedSubqueryWithJoin() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL ("
                + "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o);
        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Order> o2 = sq.correlate(o);
        Join<Order, Customer> c = o2.join(order_.getAttribute("customer",
                Customer.class));
        Join<Customer, Account> a = c.join(customer_.getList("accounts",
                Account.class));
        sq.select(a.get(account_.getAttribute("balance", Integer.class)));
        q.where(cb.lt(cb.literal(10000), cb.all(sq)));

        assertEquivalence(q, jpql);
    }
    
    @AllowFailure(message="Root of the subquery._delegate not set")
    public void testCorrelatedSubqueryWithAllClause() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c "
                    + "WHERE 10000 < ALL (SELECT a.balance FROM c.accounts a)";
        
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o);
        Join<Order, Customer> c = o.join(Order_.customer);
        Subquery<Integer> sq = q.subquery(Integer.class);
        Join<Order, Customer> csq = sq.correlate(c);
        Join<Customer, Account> a = csq.join(customer_.getList("accounts",
                Account.class));
        sq.select(a.get(account_.getAttribute("balance", Integer.class)));
        q.where(cb.lt(cb.literal(10000), cb.all(sq)));

        assertEquivalence(q, jpql);
    }

    public void testGroupByAndHaving() {
        String jpql = "SELECT c.status, AVG(c.filledOrderCount), COUNT(c) FROM "
                + "Customer c GROUP BY c.status HAVING c.status IN (1, 2)";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.groupBy(c.get(customer_.getAttribute("status", Integer.class)));
        q.having(cb.in(c.get(customer_.getAttribute("status", Integer.class)))
                .value(1).value(2));
        q.select(c.get(customer_.getAttribute("status", Integer.class)), cb
                .avg(c.get(customer_.getAttribute("filledOrderCount",
                        Integer.class))), cb.count(c));

        assertEquivalence(q, jpql);
    }

    public void testOrderingByExpressionNotIncludedInSelection() {
        String jpql = "SELECT o FROM Customer c " 
                    + "JOIN c.orders o JOIN c.address a "
                    + "WHERE a.state = 'CA' "
                    + "ORDER BY o.quantity DESC, o.totalCost";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.join(customer_.getSet("orders", 
                Order.class));
        Join<Customer, Address> a = c.join(customer_.getAttribute("address",
                Address.class));
        q.where(cb.equal(
                a.get(address_.getAttribute("state", String.class)),
                "CA"));
        q.orderBy(
          cb.desc(o.get(order_.getAttribute("quantity", Integer.class))),
          cb.asc(o.get(order_.getAttribute("totalCost", Double.class))));
        q.select(o);

        assertEquivalence(q, jpql);
    }
    
    public void testOrderingByExpressionIncludedInSelection() {
        String jpql = "SELECT o.quantity, a.zipCode FROM Customer c "
                    + "JOIN c.orders o JOIN c.address a " 
                    + "WHERE a.state = 'CA' "
                    + "ORDER BY o.quantity, a.zipCode";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer, Order> o = c.join(customer_.getSet("orders",
                Order.class));
        Join<Customer, Address> a = c.join(customer_.getAttribute("address",
                Address.class));
        q.where(cb.equal(
                a.get(address_.getAttribute("state", String.class)),
                "CA"));
        q.orderBy(cb
                .asc(o.get(order_.getAttribute("quantity", Integer.class))),
                cb.asc(a.get(address_.getAttribute("zipCode", String.class))));
        q.select(o.get(order_.getAttribute("quantity", Integer.class)), 
                a.get(address_.getAttribute("zipCode", String.class)));

        assertEquivalence(q, jpql);
    }
    
    public void testOrderingWithNumericalExpressionInSelection() {
        String jpql = "SELECT o.quantity, o.totalCost * 1.08 AS taxedCost, "
                + "a.zipCode "
                + "FROM Customer c JOIN c.orders o JOIN c.address a "
                + "WHERE a.state = 'CA' AND a.county = 'Santa Clara' "
                + "ORDER BY o.quantity, taxedCost, a.zipCode";
        
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer, Order> o = c.join(customer_.getSet("orders",
                Order.class));
        Join<Customer, Address> a = c.join(customer_.getAttribute("address",
                Address.class));
        q.where(cb.equal(a.get(address_.getAttribute("state", String.class)),
                "CA"), cb.equal(a.get(address_.getAttribute("county",
                String.class)), "Santa Clara"));
        q.orderBy(
                cb.asc(o.get(order_.getAttribute("quantity", Integer.class))),
                cb.asc(cb.prod(o.get(order_.getAttribute("totalCost",
                        Double.class)), 1.08)), cb.asc(a.get(address_
                        .getAttribute("zipCode", String.class))));
        q.select(o.get(order_.getAttribute("quantity", Integer.class)), cb
                .prod(o.get(order_.getAttribute("totalCost", Double.class)),
                        1.08), a.get(address_.getAttribute("zipCode",
                String.class)));
        assertEquivalence(q, jpql);
    }
}
