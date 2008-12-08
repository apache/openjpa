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

import javax.persistence.CaseExpression;
import javax.persistence.DomainObject;
import javax.persistence.Expression;
import javax.persistence.QueryBuilder;
import javax.persistence.QueryDefinition;
import javax.persistence.SelectItem;

import org.apache.openjpa.persistence.query.AbstractDomainObject;
import org.apache.openjpa.persistence.query.QueryBuilderImpl;
import org.apache.openjpa.persistence.query.QueryDefinitionImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;


/**
 * Tests by stringifying QueryDefinition and comparing the resultant string
 * with an equivalent JPQL.
 * 
 * Does not execute the query.
 * 
 * The examples are taken from Criteria API Section of Java Persistence API 
 * Version 2.0 [1].
 * 
 * [1] <A href="http://jcp.org/aboutJava/communityprocess/pr/jsr317/index.html">
 * JPA API Specification Version 2.0</A>
 * 
 * @author Pinaki Poddar
 *
 */
public class TestCriteria extends SingleEMFTestCase {
	protected QueryBuilderImpl qb; 
	protected StringComparison comparator = new StringComparison();
	
	public void setUp() {
		super.setUp(Contractor.class, Course.class, CreditCard.class, 
			Department.class, Employee.class, Exempt.class, Item.class,
			Manager.class, Person.class, VideoStore.class, Order.class, 
			Customer.class);
		qb = (QueryBuilderImpl)emf.getQueryBuilder();
	}
	
	public void tearDown() {
		// do nothing as you do not have a database connection
	}
	
	void compare(String s, QueryDefinition q) {
		String actual = qb.toJPQL(q);
		if (!comparator.compare(s,actual)) {
			fail("\r\nExpected: [" + s + "]\r\nActual  : [" + actual + "]");
		}
	}
	
	public void testMultipleDomainOfSameClass() {
		DomainObject o1 = qb.createQueryDefinition(Order.class);
		DomainObject o2 = o1.addRoot(Order.class);
		o1.select(o1)
		  .where(o1.get("quantity").greaterThan(o2.get("quantity"))
		    .and(o2.get("customer").get("lastName").equal("Smith"))
		    .and(o2.get("customer").get("firstName").equal("John")));
		
		String jpql = "select o from Order o, Order o2" +
		              " where o.quantity > o2.quantity" +
		              " and o2.customer.lastName = 'Smith'" +
		              " and o2.customer.firstName = 'John'";
		compare(jpql, o1);
	}

	public void testFetchJoin() {
		DomainObject d = qb.createQueryDefinition(Department.class);
		d.leftJoinFetch("employees");
		d.where(d.get("deptNo").equal(1));
		
		
		String jpql = "select d from Department d" +
		              " LEFT JOIN FETCH d.employees" +
		              " where d.deptNo = 1";
		compare(jpql, d);
	}
	
	public void testMultipartNavigation() {
		DomainObject e = qb.createQueryDefinition(Employee.class);
		DomainObject p = e.join("contactInfo").join("phones");
		e.where(e.get("contactInfo").get("address").get("zipCode")
				.equal("95094")).select(p.get("vendor"));
				
		
		String jpql = "select p.vendor from Employee e" +
		              " JOIN e.contactInfo c JOIN c.phones p" +
		              " where e.contactInfo.address.zipCode = '95094'";
		compare(jpql, e);
	}
	
	public void testOperatorPath() {
		QueryDefinition qdef = qb.createQueryDefinition();
		DomainObject item = qdef.addRoot(Item.class);
		DomainObject photo = item.join("photos");
		qdef.select(item.get("name"), photo.value())
		    .where(photo.key().like("egret"));
		
		
		String jpql = "select i.name, VALUE(p) from Item i join i.photos p where KEY(p) like 'egret'";
		compare(jpql, qdef);
	}
	
	public void testLiteral() {
		DomainObject c = qb.createQueryDefinition(Customer.class);
		DomainObject o = c.join("orders");
		DomainObject a = c.join("address");
		o.where(a.get("state").equal("CA").and(a.get("county").equal("Santa Clara")));
		o.select(o.get("quantity"), o.get("cost").times(1.08), a.get("zipCode"));
		
		String jpql = "select o.quantity, o.cost*1.08, a.zipCode" +
		              " from Customer c join c.orders o join c.address a" +
		              " where a.state = 'CA' and a.county = 'Santa Clara'";
		compare(jpql, c);
	}
	
	public void testTypeExpression() {
		DomainObject e = qb.createQueryDefinition(Employee.class);
		e.select(e.type())
		 .where(e.type().equal(Exempt.class).not());
		
		String jpql = "select TYPE(e)" +
		              " from Employee e" +
		              " where TYPE(e) <> Exempt";
		compare(jpql, e);
	}

	public void testIndex() {
		DomainObject c = qb.createQueryDefinition(Course.class);
		DomainObject w = c.join("studentWaitList");
		c.where(c.get("name").equal("Calculus").and(w.index().equal(0)))
		 .select(w.get("name"));
		
		String jpql = "select s.name" +
		              " from Course c join c.studentWaitList s" +
		              " where c.name = 'Calculus' and INDEX(s) = 0";
		compare(jpql, c);
	}
	
	public void testSum() {
		DomainObject o = qb.createQueryDefinition(Order.class);
		DomainObject l = o.join("lineItems");
		DomainObject c = o.join("customer");
		c.where(c.get("lastName").equal("Smith").and(c.get("firstName").equal("John")))
		 .select(l.get("price").sum());
		
		String jpql = "select SUM(l.price)" +
		              " from Order o join o.lineItems l JOIN o.customer c" +
		              " where c.lastName = 'Smith' and c.firstName = 'John'";
		compare(jpql, c);
	}
	
	public void testSize() {
		DomainObject d = qb.createQueryDefinition(Department.class);
		d.where(d.get("name").equal("Sales"))
		 .select(d.get("employees").size());
		
		String jpql = "select SIZE(d.employees)" +
		              " from Department d " +
		              " where d.name = 'Sales'";
		compare(jpql, d);
	}
	
	public void testGeneralCase() {
		DomainObject e = qb.createQueryDefinition(Employee.class);
		e.where(e.get("department").get("name").equal("Engineering"));
		e.select(e.get("name"),
		e.generalCase()
		.when(e.get("rating").equal(1))
		.then(e.get("salary").times(1.1))
		.when(e.get("rating").equal(2))
		.then(e.get("salary").times(1.2))
		.elseCase(e.get("salary").times(1.01)));
		
		String jpql = "SELECT e.name,"
					+ " CASE WHEN e.rating = 1 THEN e.salary * 1.1"
					+ " WHEN e.rating = 2 THEN e.salary * 1.2"
					+ " ELSE e.salary * 1.01"
					+ " END"
					+ " FROM Employee e"
					+ " WHERE e.department.name = 'Engineering'";
		
		compare(jpql, e);
	}
	
	public void testMemberOf() {
		DomainObject p = qb.createQueryDefinition(Person.class);
		p.where(p.literal("Joe").member(p.get("nicknames")));
		
		String jpql = "select p from Person p " +
		              " where 'Joe' MEMBER OF p.nicknames";
		compare(jpql, p);
	}
	
	public void testParamater() {
		QueryDefinition qdef = qb.createQueryDefinition();
		DomainObject customer = qdef.addRoot(Customer.class);
		qdef.where(customer.get("status").equal(qdef.param("status")));
		
		String jpql = "select c from Customer c " +
		              " where c.status = :status";
		compare(jpql, qdef);
	}
	
	public void testBetween() {
		DomainObject c = qb.createQueryDefinition(CreditCard.class);
		DomainObject t = c.join("transactionHistory");
		c.select(t).where(c.get("holder").get("name").equal("John Doe")
				.and(t.index().between(0, 9)));
		
		
		String jpql = "select t from CreditCard c JOIN c.transactionHistory t" +
		              " where c.holder.name = 'John Doe' AND INDEX(t) " +
		              " BETWEEN 0 AND 9";
		compare(jpql, c);
	}
	
	public void testIsEmpty() {
		DomainObject o = qb.createQueryDefinition(Order.class);
		o.where(o.get("lineItems").isEmpty());
		
		
		String jpql = "select o from Order o " +
		              " where o.lineItems IS EMPTY";
		compare(jpql, o);
	}
	
	public void testNonCorrelatedSubQuery() {
		QueryDefinition q1 = qb.createQueryDefinition();
		DomainObject goodCustomer = q1.addRoot(Customer.class);
		
		QueryDefinition q2 = qb.createQueryDefinition();
		DomainObject customer = q2.addRoot(Customer.class);
		
		q1.where(goodCustomer.get("balanceOwned")
				.lessThan(q2.select(customer.get("balanceOwned").avg())));
		
		String jpql = "select c from Customer c "
					+ " where c.balanceOwned < " 
					+ "(select AVG(c2.balanceOwned) from Customer c2)";
		compare(jpql, q1);
	}

	public void testNew() {
		QueryDefinition q = qb.createQueryDefinition();
		DomainObject customer = q.addRoot(Customer.class);
		DomainObject order = customer.join("orders");
		q.where(order.get("count").greaterThan(100))
		.select(q.newInstance(Customer.class,
		customer.get("id"),
		customer.get("status"),
		order.get("count")));
		
		
		String jpql = "SELECT NEW org.apache.openjpa.persistence.criteria.Customer(c.id, c.status, o.count)"
					+ " FROM Customer c JOIN c.orders o" 
					+ " WHERE o.count > 100";
		compare(jpql, q);
	}
	
	public void testKeyValueOperatorPath() {
	QueryDefinition q = qb.createQueryDefinition();
	DomainObject v = q.addRoot(VideoStore.class);
	DomainObject i = v.join("videoInventory");
	q.where(v.get("location").get("zipcode").equal("94301")
	.and(i.value().greaterThan(0)));
	q.select(v.get("location").get("street"),
	i.key().get("title"),
	i.value());
	
	String jpql = "SELECT v.location.street, KEY(v2).title, VALUE(v2)" 
				+ " FROM VideoStore v JOIN v.videoInventory v2"
				+ " WHERE v.location.zipcode = '94301' AND VALUE(v2) > 0";
	compare(jpql, q);
	}
	
	public void testGroupByHaving() {
		QueryDefinition q = qb.createQueryDefinition();
		DomainObject customer = q.addRoot(Customer.class);
		q.select(customer.get("status"),
		customer.get("filledOrderCount").avg(),
		customer.count())
		.groupBy(customer.get("status"))
		.having(customer.get("status").in(1, 2));
		
		String jpql = "SELECT c.status, AVG(c.filledOrderCount), COUNT(c)"
					+ " FROM Customer c"
					+ " GROUP BY c.status"
					+ " HAVING c.status IN (1, 2)";
		
		compare(jpql, q);
	}
	
	public void testGroupByHaving2() {
		QueryDefinition q = qb.createQueryDefinition();
		DomainObject customer = q.addRoot(Customer.class);
		q.select(customer.get("country"),
		customer.count())
		.groupBy(customer.get("country"))
		.having(customer.count().greaterThan(30));
		
		String jpql = "SELECT c.country, COUNT(c)" 
					+ " FROM Customer c"
					+ " GROUP BY c.country"
					+ " HAVING COUNT(c) > 30";
		compare(jpql, q);
	}
	
	public void testOrderBy() {
		QueryDefinition q = qb.createQueryDefinition();
		DomainObject customer = q.addRoot(Customer.class);
		DomainObject order = customer.join("orders");
		DomainObject address = customer.join("address");
		q.where(address.get("state").equal("CA"))
		.select(order)
		.orderBy(order.get("quantity").desc(), order.get("totalcost"));
		String jpql = "SELECT o"
					+ " FROM Customer c JOIN c.orders o JOIN c.address a"
					+ " WHERE a.state = 'CA'"
					+ " ORDER BY o.quantity DESC, o.totalcost";		
		compare(jpql, q);
	}
	
	public void testOrderBy2() {
	QueryDefinition q = qb.createQueryDefinition();
	DomainObject customer = q.addRoot(Customer.class);
	DomainObject order = customer.join("orders");
	DomainObject address = customer.join("address");
	q.where(address.get("state").equal("CA"))
	.select(order.get("quantity"), address.get("zipcode"))
	.orderBy(order.get("quantity").desc(), address.get("zipcode"));
	String jpql = "SELECT o.quantity, a.zipcode"
				+ " FROM Customer c JOIN c.orders o JOIN c.address a"
				+ " WHERE a.state = 'CA'"
				+ " ORDER BY o.quantity DESC, a.zipcode";
	compare(jpql, q);
	}
	
	public void testOrderByExpression() {
	DomainObject o = qb.createQueryDefinition(Order.class);
	DomainObject a = o.join("customer").join("address");
	SelectItem taxedCost = o.get("cost").times(1.08);
	o.select(o.get("quantity"), taxedCost, a.get("zipcode"))
	.where(a.get("state").equal("CA")
	.and(a.get("county").equal("Santa Clara")))
	.orderBy(o.get("quantity"), taxedCost, a.get("zipcode"));
	
	String jpql = "SELECT o.quantity, o.cost*1.08 as o2, a.zipcode" 
				+ " FROM Order o JOIN o.customer c JOIN c.address a"
				+ " WHERE a.state = 'CA' AND a.county = 'Santa Clara'"
				+ " ORDER BY o.quantity, o2, a.zipcode";
	
	compare(jpql, o);
	}
	
	public void testCorrelatedSubquery() {
		QueryDefinition q1 = qb.createQueryDefinition();
		DomainObject emp = q1.addRoot(Employee.class);
		
		QueryDefinition q2 = qb.createQueryDefinition();
		DomainObject spouseEmp = q2.addRoot(Employee.class);
		
		q2.where(spouseEmp.equal(emp.get("spouse"))).select(spouseEmp);
		q1.selectDistinct(emp).where(q2.exists());
		
		String jpql = "SELECT DISTINCT e "
					+ " FROM Employee e"
					+ " WHERE EXISTS ("
					        + " SELECT e2 " 
					        + " FROM Employee e2"
					        + " WHERE e2 = e.spouse)";
		
		compare(jpql, q1);
	}
	
	public void testCreateSubquery() {
		DomainObject customer = qb.createQueryDefinition(Customer.class);
		DomainObject order = qb.createSubqueryDefinition(customer.get("orders"));
		customer.where(order.select(order.get("price").avg()).greaterThan(100));
		
		String jpql = "SELECT c "
					+ " FROM Customer c"
					+ " WHERE (SELECT AVG(o.price) FROM c.orders o) > 100";
		
		compare(jpql, customer);
	}
	
	public void testTypeList() {
		DomainObject q = qb.createQueryDefinition(Employee.class);
		q.where(q.type().in(Exempt.class, Contractor.class));
		
		String jpql = "SELECT e "
			+ " FROM Employee e"
			+ " WHERE TYPE(e) IN (Exempt, Contractor)";
		compare(jpql, q);
	}
	
	public void testStringList() {
		DomainObject q = qb.createQueryDefinition(Employee.class);
		q.where(q.get("country").in("USA", "UK", "France"));
		
		String jpql = "SELECT e "
			+ " FROM Employee e"
			+ " WHERE e.country IN ('USA', 'UK', 'France')";
		compare(jpql, q);
	}
	
	public void testConcat() {
		DomainObject e = qb.createQueryDefinition(Employee.class);
		DomainObject f = e.join("frequentFlierPlan");
		Expression c = 
		e.generalCase().when(f.get("annualMiles").greaterThan(50000)).then("Platinum")
		               .when(f.get("annualMiles").greaterThan(25000)).then("Gold")
		               .elseCase("");
		e.select(e.get("name"), f.get("name"), e.concat(c,e.literal("Frequent Flyer")));
		
		String jpql = "SELECT e.name, f.name, CONCAT(" 
			+ " CASE WHEN f.annualMiles > 50000 THEN 'Platinum'" 
			+ "      WHEN f.annualMiles > 25000 THEN 'Gold'" 
			+ " ELSE '' END, 'Frequent Flyer')" 
			+ "FROM Employee e JOIN e.frequentFlierPlan f";
			
		compare(jpql, e);
	}
	
	public void testRecursiveDefinitionIsNotAllowed() {
		DomainObject q = qb.createQueryDefinition(Customer.class);
		q.where(q.exists().and(q.get("name").equal("wrong")));
		
		try {
			compare("?", q);
			fail();
		} catch (RuntimeException e) {
			// good
		}
	}
	
}
