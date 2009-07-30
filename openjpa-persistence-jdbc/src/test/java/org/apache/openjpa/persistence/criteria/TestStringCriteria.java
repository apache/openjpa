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
import java.util.Set;

import javax.persistence.Parameter;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.test.AllowFailure;

public class TestStringCriteria extends CriteriaTest {

    @AllowFailure
    public void testCriteria() {
        String jpql = "select c from Customer c where c.name='Autowest Toyota'";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> customer = q.from(Customer.class);
        q = cb.createQuery();
        q.select(customer).where(
                cb.equal(customer.get("name"), "Autowest Toyota"));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testJoins1() {
        String jpql = "SELECT c.name FROM Customer c JOIN c.orders o "
                + "JOIN o.lineItems i WHERE i.product.productType = 'printer'";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> cust = q.from(Customer.class);
        SetJoin<Customer, Order> order = cust.joinSet("orders");
        ListJoin<Order, LineItem> item = order.joinList("lineItems");
        q.select(cust.get(Customer_.name)).where(
                cb.equal(item.get("product").get("productType"), "printer"));

        assertEquivalence(q, jpql);
    }

    public void testJoins2() {
        String jpql = "SELECT c FROM Customer c LEFT JOIN c.orders o WHERE "
                + "c.status = 1";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> cust1 = q.from(Customer.class);
        SetJoin<Customer, Order> order1 = cust1
                .joinSet("orders", JoinType.LEFT);
        q.where(cb.equal(cust1.get("status"), 1)).select(cust1);

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testFetchJoins() {
        String jpql = "SELECT d FROM Department LEFT JOIN FETCH d.employees "
                + "WHERE d.deptNo = 1";
        CriteriaQuery q = cb.createQuery();
        Root<Department> d = q.from(Department.class);
        d.fetch("employees", JoinType.LEFT);
        q.where(cb.equal(d.get("deptNo"), 1)).select(d);

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testPathNavigation() {
        String jpql = "SELECT p.vendor FROM Employee e "
                + "JOIN e.contactInfo.phones p  WHERE e.contactInfo.address.zipCode ="
                + " '95054'";
        CriteriaQuery q = cb.createQuery();
        Root<Employee> emp = q.from(Employee.class);
        ListJoin<Contact, Phone> phone = emp.join("contactInfo").joinList(
                "phones");
        q.where(cb.equal(emp.get("contactInfo").get("address").get("zipCode"),
                "95054"));
        q.select(phone.get("vendor"));

        assertEquivalence(q, jpql);
    }

    public void testKey() {
        String jpql = "SELECT i.name, p FROM Item i JOIN i.photos p "
                + "WHERE KEY(p) LIKE '%egret%'";

        CriteriaQuery q = cb.createQuery();
        Root<Item> item = q.from(Item.class);
        MapJoin<Item, String, Photo> photo = item.joinMap("photos");
        q.multiselect(item.get("name"), photo)
                .where(cb.like(photo.key(), "%egret%"));

        assertEquivalence(q, jpql);
    }

    public void testRestrictQueryResult() {
        String jpql = "SELECT t FROM CreditCard c JOIN c.transactionHistory t "
                + "WHERE c.customer.accountNum = 321987 AND INDEX(t) BETWEEN 0 "
                + "AND 9";
        CriteriaQuery q = cb.createQuery();
        Root<CreditCard> c = q.from(CreditCard.class);
        ListJoin<CreditCard, TransactionHistory> t = c
                .joinList("transactionHistory");
        q.select(t).where(
                cb.equal(c.get("customer").get("accountNum"), 321987),
                cb.between(t.index(), 0, 9));

        assertEquivalence(q, jpql);

        /*
         * 
         * jpql = "SELECT o FROM Order o WHERE o.lineItems IS EMPTY"; q =
         * cb.createQuery(); Root<Order> order = q.from(Order.class);
         * ListJoin<Order,LineItem> lineItems = order.joinList("lineItems");
         * q.where(cb.isEmpty(lineItems)); q.select(order);
         * 
         * assertEquivalence(q, jpql);
         */
    }

    @AllowFailure
    public void testExpressions() {
        String jpql = "SELECT o.quantity, o.totalCost*1.08 AS taxedCost, "
                + "a.zipCode FROM Customer c JOIN c.orders o JOIN c.address a "
                + "WHERE a.state = 'CA' AND a.county = 'Santa Clara";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> cust = q.from(Customer.class);
        SetJoin<Customer, Order> order = cust.joinSet("orders");
        Join<Customer, Address> address = cust.join("address");
        q.where(cb.equal(address.get("state"), "CA"), cb.equal(address
                .get("county"), "Santa Clara"));
        q.multiselect(order.get("quantity"),
        // cb.prod(order.get("totalCost"), cb.literal(1.08)), //compiler err
                address.get("zipCode"));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testIndex() {
        String jpql = "SELECT w.name FROM Course c JOIN c.studentWaitList w "
                + "WHERE c.name = 'Calculus' AND INDEX(w) = 0";
        CriteriaQuery q = cb.createQuery();
        Root<Course> course = q.from(Course.class);
        ListJoin<Course, Student> w = course.joinList("studentWaitList");
        q.where(cb.equal(course.get("name"), "Calculus"),
                cb.equal(w.index(), 0)).select(w.get("name"));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSum() {
        String jpql = "SELECT SUM(i.price) FROM Order o JOIN o.lineItems i JOIN "
                + "o.customer c WHERE c.lastName = 'Smith' AND c.firstName = 'John'";
        CriteriaQuery q = cb.createQuery();
        Root<Order> o = q.from(Order.class);
        ListJoin<Order, LineItem> i = o.joinList("lineItems");
        Join<Order, Customer> c = o.join("customer");
        q.where(cb.equal(c.get("lastName"), "Smith"), cb.equal(c
                .get("firstName"), "John"));
        // q.select(cb.sum(i.get("price"))); //compiler err

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSize() {
        String jpql = "SELECT SIZE(d.employees) FROM Department d "
                + "WHERE d.name = 'Sales'";
        CriteriaQuery q = cb.createQuery();
        Root<Department> d = q.from(Department.class);
        q.where(cb.equal(d.get("name"), "Sales"));
        // q.select(cb.size((d.get("employees")))); // compiler err

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testCase() {
        String jpql = "SELECT e.name, CASE WHEN e.rating = 1 THEN e.salary * 1.1 "
                + "WHEN e.rating = 2 THEN e.salary * 1.2 ELSE e.salary * 1.01 END "
                + "FROM Employee e WHERE e.department.name = 'Engineering'";
        CriteriaQuery<Employee> q = cb.createQuery(Employee.class);
        Root<Employee> e = q.from(Employee.class);
        q.where(cb.equal(e.get("department").get("name"), "Engineering"));
        q.multiselect(e.get("name"), cb.selectCase().when(
                cb.equal(e.get("rating"), 1), 1.1)       // cb.prod(e.get("salary"), 1.1))
                .when(cb.equal(e.get("rating"), 2), 1.2) // cb.prod(e.get("salary"), 1.2))
                .otherwise(1.01));                       // cb.prod(e.get("salary"), 1.01)));
        assertEquivalence(q, jpql);
    }
//    
//    public void testABCDEFG() {
//        String jpql = "SELECT e.name, CASE WHEN e.rating = 1 THEN e.salary * 1.1 "
//                + "WHEN e.rating = 2 THEN e.salary * 1.2 ELSE e.salary * 1.01 END "
//                + "FROM Employee e WHERE e.department.name = 'Engineering'";
//        CriteriaQuery<Employee> q = cb.createQuery(Employee.class);
//        Root<Employee> e = q.from(Employee.class);
//        q.where(cb.equal(e.get(Employee_.department).get(Department_.name), "Engineering"));
//        q.multiselect(e.get(Employee_.name), cb.selectCase().when(
//                cb.equal(e.get(Employee_.rating), 1), 1.1)       // cb.prod(e.get("salary"), 1.1))
//                .when(cb.equal(e.get(Employee_.rating), 2), 1.2) // cb.prod(e.get("salary"), 1.2))
//                .otherwise(1.01));                       // cb.prod(e.get("salary"), 1.01)));
//        assertEquivalence(q, jpql);
//    }
    
    /*
     * @AllowFailure public void testLiterals() { String jpql =
     * "SELECT p FROM Person p where 'Joe' MEMBER OF " + "p.nickNames";
     * CriteriaQuery q = cb.createQuery(); Root<Person> p = q.from(Person.class);
     * q.select(p).where(cb.isMember(cb.literal("Joe"), p.get(person_.
     * getSet("nickNames", String.class))));
     * 
     * assertEquivalence(q, jpql); }
     */

    @AllowFailure
    public void testParameters() {
        String jpql = "SELECT c FROM Customer c Where c.status = :stat";
        CriteriaQuery<Customer> q = cb.createQuery(Customer.class);
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param = cb.parameter(Integer.class);
        q.select(c).where(cb.equal(c.get("status"), param));

        assertEquivalence(q, jpql, new String[] { "stat" }, new Object[] { 1 });
    }

    @AllowFailure
    public void testSelectList() {
        String jpql = "SELECT v.location.street, KEY(i).title, VALUE(i) FROM "
                + "VideoStore v JOIN v.videoInventory i "
                + "WHERE v.location.zipCode = " + "'94301' AND VALUE(i) > 0";
        CriteriaQuery q = cb.createQuery();
        Root<VideoStore> v = q.from(VideoStore.class);
        MapJoin<VideoStore, Movie, Integer> inv = v.joinMap("videoInventory");
        q.where(cb.equal(v.get("location").get("zipCode"), "94301"), cb.gt(inv
                .value(), 0));
        q.multiselect(v.get("location").get("street"), inv.key().get("title"), inv
                .value());

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void TestContructor() {
        String jpql = "SELECT NEW CustomerDetails(c.id, c.status, o.quantity) FROM "
                + "Customer c JOIN c.orders o WHERE o.quantity > 100";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.joinSet("orders");
        // q.where(cb.gt(o.get("quantity"), 100)); //compiler err
        //q.select(cb.select(CustomerDetails.class, c.get("id"), c.get("status"),
        //        o.get("quantity")));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSubquery1() {
        String jpql = "SELECT goodCustomer FROM Customer goodCustomer WHERE "
                + "goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) FROM "
                + "Customer c)";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> goodCustomer = q.from(Customer.class);
        Subquery<Double> sq = q.subquery(Double.class);
        Root<Customer> c = sq.from(Customer.class);
        // q.where(cb.lt(goodCustomer.get("balanceOwed"), // compiler err
        // sq.select(cb.avg(c.get("balanceOwed")))));
        q.select(goodCustomer);

        assertEquivalence(q, jpql);
    }

    public void testSubquery2() {
        String jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS ("
                + "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp = "
                + "emp.spouse)";
        CriteriaQuery q = cb.createQuery();
        Root<Employee> emp = q.from(Employee.class);
        Subquery<Employee> sq1 = q.subquery(Employee.class);
        Root<Employee> spouseEmp = sq1.from(Employee.class);
        sq1.select(spouseEmp);
        sq1.where(cb.equal(spouseEmp, emp.get("spouse")));
        q.where(cb.exists(sq1));
        q.select(emp).distinct(true);

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSubquery3() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL ("
                + "SELECT m.salary FROM Manager m WHERE m.department ="
                + " emp.department)";
        CriteriaQuery q = cb.createQuery();
        Root<Employee> emp1 = q.from(Employee.class);
        q.select(emp1);
        Subquery<BigDecimal> sq2 = q.subquery(BigDecimal.class);
        Root<Manager> m = sq2.from(Manager.class);
        // sq2.select(m.get("salary")); // compiler err
        sq2.where(cb.equal(m.get("department"), emp1.get("department")));
        // q.where(cb.gt(emp1.get("salary"), // compiler err
        // cb.all(sq2)));

        assertEquivalence(q, jpql);
    }

    public void testSubquery4() {
        String jpql = "SELECT c FROM Customer c WHERE "
                + "(SELECT COUNT(o) FROM c.orders o) > 10";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> c1 = q.from(Customer.class);
        q.select(c1);
        Subquery<Long> sq3 = q.subquery(Long.class);
        Root<Customer> c2 = sq3.correlate(c1);
        SetJoin<Customer, Order> o = c2.joinSet("orders");
        q.where(cb.gt(sq3.select(cb.count(o)), 10));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSubquery5() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL ("
                + "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        CriteriaQuery q = cb.createQuery();
        Root<Order> o1 = q.from(Order.class);
        q.select(o1);
        Subquery<Integer> sq4 = q.subquery(Integer.class);
        Root<Order> o2 = sq4.correlate(o1);
        Join<Order, Customer> c3 = o2.join("customer");
        ListJoin<Customer, Account> a = c3.joinList(("accounts"));
        // sq4.select(a.get("balance")); // compiler err
        q.where(cb.lt(cb.literal(10000), cb.all(sq4)));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSubquery6() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
                + "ALL (SELECT a.balance FROM c.accounts a)";
        CriteriaQuery q = cb.createQuery();
        Root<Order> o3 = q.from(Order.class);
        q.select(o3);
        Join<Order, Customer> c4 = o3.join(Order_.customer);
        Subquery<Integer> sq5 = q.subquery(Integer.class);
        Join<Order, Customer> c5 = sq5.correlate(c4);
        ListJoin<Customer, Account> a = c5.joinList("accounts");
        // sq5.select(a.get("balance")); // compiler err
        q.where(cb.lt(cb.literal(10000), cb.all(sq5)));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testGroupByAndHaving() {
        String jpql = "SELECT c.status, AVG(c.filledOrderCount), COUNT(c) FROM "
                + "Customer c GROUP BY c.status HAVING c.status IN (1, 2)";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> c = q.from(Customer.class);
        q.groupBy(c.get("status"));
        q.having(cb.in(c.get("status")).value(1).value(2));
        q.multiselect(c.get("status"),
        //cb.avg(c.get("filledOrderCount")), // compiler err
                cb.count(c));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testOrdering1() {
        String jpql = "SELECT o FROM Customer c JOIN c.orders o "
                + "JOIN c.address a WHERE a.state = 'CA' ORDER BY o.quantity DESC, "
                + "o.totalCost";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.joinSet("orders");
        Join<Customer, Address> a = c.join("address");
        q.where(cb.equal(a.get("state"), "CA"));
        q.orderBy(cb.desc(o.get("quantity")), cb.asc(o.get("totalCost")));
        q.select(o);

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testOrdering2() {
        String jpql = "SELECT o.quantity, a.zipCode FROM Customer c JOIN c.orders "
                + "JOIN c.address a WHERE a.state = 'CA' ORDER BY o.quantity, "
                + "a.zipCode";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> c1 = q.from(Customer.class);
        SetJoin<Customer, Order> o1 = c1.joinSet("orders");
        Join<Customer, Address> a1 = c1.join("address");
        q.where(cb.equal(a1.get("state"), "CA"));
        q.orderBy(cb.asc(o1.get("quantity")), cb.asc(a1.get("zipCode")));
        q.multiselect(o1.get("quantity"), a1.get("zipCode"));

        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testOrdering3() {
        String jpql = "SELECT o.quantity, o.cost * 1.08 AS taxedCost, a.zipCode "
                + "FROM Customer c JOIN c.orders o JOIN c.address a "
                + "WHERE a.state = 'CA' AND a.county = 'Santa Clara' "
                + "ORDER BY o.quantity, taxedCost, a.zipCode";
        CriteriaQuery q = cb.createQuery();
        Root<Customer> c2 = q.from(Customer.class);
        SetJoin<Customer, Order> o = c2.joinSet("orders");
        Join<Customer, Address> a = c2.join("address");
        q.where(cb.equal(a.get("state"), "CA"), cb.equal(a.get("county"),
                "Santa Clara"));
        q.orderBy(cb.asc(o.get("quantity")),
        // cb.asc(cb.prod(o.get("totalCost"), 1.08)), //compiler err
                cb.asc(a.get("zipCode")));
        q.multiselect(o.get("quantity"),
         //cb.prod(o.get("totalCost"), 1.08), //compiler err
                a.get("zipCode"));

        assertEquivalence(q, jpql);
    }

}
