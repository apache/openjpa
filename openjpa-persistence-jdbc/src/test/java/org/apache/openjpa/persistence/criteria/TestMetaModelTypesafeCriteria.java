package org.apache.openjpa.persistence.criteria;
import java.math.BigDecimal;
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
import javax.persistence.metamodel.Embeddable;
import javax.persistence.metamodel.Entity;
import javax.persistence.metamodel.Metamodel;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestMetaModelTypesafeCriteria extends SQLListenerTestCase {
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
    CriteriaBuilder cb;
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
                    Product.class,
                    Order.class, 
                    Phone.class,
                    Photo.class,
                    Semester.class,
                    Student.class, 
                    TransactionHistory.class,
                    VideoStore.class);
            
            setDictionary();
            cb = (CriteriaBuilder)emf.getQueryBuilder();
            em = emf.createEntityManager();
            Metamodel mm = em.getMetamodel();
            account_ = mm.entity(Account.class);
            address_ = mm.embeddable(Address.class);
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
        
        void setDictionary() {
            JDBCConfiguration conf = (JDBCConfiguration)emf.getConfiguration();
            DBDictionary dict = conf.getDBDictionaryInstance();
            dict.requiresCastForComparisons = false;
            dict.requiresCastForMathFunctions = false;
        }
        
    public void createObj() {
    }
    
    @AllowFailure
    public void testCriteria() {
        String jpql = "select c from Customer c where c.name='Autowest Toyota'";
        CriteriaQuery q = cb.create();
        Root<Customer> customer = q.from(Customer.class);
        q = cb.create();
        q.select(customer).where(cb.equal(
            customer.get(customer_.getAttribute("name", String.class)), 
            "Autowest Toyota"));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testJoins() {
        String jpql = "SELECT c.name FROM Customer c JOIN c.orders o " + 
            "JOIN o.lineItems i WHERE i.product.productType = 'printer'";
        CriteriaQuery q = cb.create();
        Root<Customer> cust = q.from(Customer.class);
        Join<Customer, Order> order = cust.join(customer_.getSet("orders", 
            Order.class));
        Join<Order, LineItem> item = order.join(order_.getList("lineItems", 
            LineItem.class));
        q.select(cust.get(Customer_.name))
            .where(cb.equal(
                item.get(lineItem_.getAttribute("product", Product.class)).
                get(product_.getAttribute("productType", String.class)), 
                "printer"));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT c FROM Customer c LEFT JOIN c.orders o WHERE " + 
            "c.status = 1";
        q = cb.create();
        Root<Customer> cust1 = q.from(Customer.class);
        Join<Customer, Order> order1 = cust1.join(customer_.getSet("orders", 
            Order.class), JoinType.LEFT);
        q.where(cb.equal(cust1.get(customer_.getAttribute("status", 
            Integer.class)), 1)).select(cust1);
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testFetchJoins() {
        String jpql = "SELECT d FROM Department LEFT JOIN FETCH d.employees " + 
            "WHERE d.deptNo = 1";
        CriteriaQuery q = cb.create();
        Root<Department> d = q.from(Department.class);
        d.fetch(department_.getSet("employees", Employee.class), JoinType.LEFT);
        q.where(cb.equal(d.get(department_.getAttribute("deptNo", 
            Integer.class)), 1)).select(d);
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testPathNavigation() {
        String jpql = "SELECT p.vendor FROM Employee e " + 
            "JOIN e.contactInfo.phones p  WHERE e.contactInfo.address.zipCode ="
            + " '95054'";
        CriteriaQuery q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        Join<Contact, Phone> phone = emp.join(
            employee_.getAttribute("contactInfo", Contact.class)).
            join(contact_.getList("phones", Phone.class));
        q.where(cb.equal(emp.get(employee_.getAttribute("contactInfo", 
            Contact.class)).
            get(contact_.getAttribute("address", Address.class)).
            get(address_.getAttribute("zipCode", String.class)), "95054"));    
        q.select(phone.get(phone_.getAttribute("vendor", String.class)));        
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT i.name, p FROM Item i JOIN i.photos p WHERE KEY(p) " + 
            "LIKE '%egret%'";
        
        q = cb.create();
        Root<Item> item = q.from(Item.class);
        MapJoin<Item, String, Photo> photo = item.join(item_.getMap("photos", 
            String.class, Photo.class));
        q.select(item.get(item_.getAttribute("name", String.class)), photo).
            where(cb.like(photo.key(), "%egret%"));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testRestrictQueryResult() {
        String jpql = "SELECT t FROM CreditCard c JOIN c.transactionHistory t " 
            + "WHERE c.customer.accountNum = 321987 AND INDEX(t) BETWEEN 0 " 
            + "AND 9";
        CriteriaQuery q = cb.create();
        Root<CreditCard> c = q.from(CreditCard.class);
        ListJoin<CreditCard, TransactionHistory> t = 
            c.join(creditCard_.getList("transactionHistory", 
            TransactionHistory.class));
        q.select(t).where(cb.equal(
            c.get(creditCard_.getAttribute("customer", Customer.class)).
            get(customer_.getAttribute("accountNum", Long.class)), 321987),
            cb.between(t.index(), 0, 9));
        
        assertEquivalence(q, jpql);

/*
         
        jpql = "SELECT o FROM Order o WHERE o.lineItems IS EMPTY";
        q = cb.create();
        Root<Order> order = q.from(Order.class);
        Join<Order,LineItem> lineItems = order.join(order_.getList("lineItems", 
            LineItem.class));
        q.where(cb.isEmpty(lineItems));
        q.select(order);
        
        assertEquivalence(q, jpql);
*/        
    }

    @AllowFailure
    public void testExpressions() {
        String jpql = "SELECT o.quantity, o.totalCost*1.08 AS taxedCost, "  
            + "a.zipCode FROM Customer c JOIN c.orders o JOIN c.address a " 
            + "WHERE a.state = 'CA' AND a.county = 'Santa Clara";
        CriteriaQuery q = cb.create();
        Root<Customer> cust = q.from(Customer.class);
        Join<Customer, Order> order = cust.join(customer_.getSet("orders", 
            Order.class));
        Join<Customer, Address> address = cust.join(customer_.getAttribute(
            "address", Address.class));
        q.where(cb.equal(address.get(address_.getAttribute("state", 
            String.class)), "CA"),
            cb.equal(address.get(address_.getAttribute("county", 
            String.class)), "Santa Clara"));
        q.select(order.get(order_.getAttribute("quantity", Integer.class)), 
            cb.prod(order.get(order_.getAttribute("totalCost", Double.class)),
            1.08),
            address.get(address_.getAttribute("zipCode", String.class)));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT TYPE(e) FROM Employee e WHERE TYPE(e) <> Exempt";
        q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        q.select(emp.type()).where(cb.notEqual(emp.type(), Exempt.class));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT w.name FROM Course c JOIN c.studentWaitList w " + 
            "WHERE c.name = 'Calculus' AND INDEX(w) = 0";
        q = cb.create();
        Root<Course> course = q.from(Course.class);
        ListJoin<Course, Student> w = course.join(course_.getList(
            "studentWaitList", Student.class));
        q.where(cb.equal(course.get(course_.getAttribute("name", 
            String.class)), "Calculus"), 
            cb.equal(w.index(), 0)).select(w.get(student_.getAttribute("name", 
            String.class)));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT SUM(i.price) FROM Order o JOIN o.lineItems i JOIN " +
            "o.customer c WHERE c.lastName = 'Smith' AND c.firstName = 'John'";
        q = cb.create();
        Root<Order> o = q.from(Order.class);
        Join<Order, LineItem> i = o.join(order_.getList("lineItems", 
            LineItem.class));
        Join<Order, Customer> c = o.join(order_.getAttribute("customer", 
            Customer.class));
        q.where(cb.equal(c.get(customer_.getAttribute("lastName", 
            String.class)), "Smith"),
            cb.equal(c.get(customer_.getAttribute("firstName", 
            String.class)), "John"));
        q.select(cb.sum(i.get(lineItem_.getAttribute("price", 
            Double.class))));
        
        assertEquivalence(q, jpql);
/*        
        jpql = "SELECT SIZE(d.employees) FROM Department d " + 
            "WHERE d.name = 'Sales'";
        q = cb.create();
        Root<Department> d = q.from(Department.class);
        q.where(cb.equal(d.get(department_.getAttribute("name", 
            String.class)), "Sales"));
        q.select(cb.size(d.get(department_.getSet("employees", 
            Employee.class))));
        
        assertEquivalence(q, jpql);
*/        
        jpql = "SELECT e.name, CASE WHEN e.rating = 1 THEN e.salary * 1.1 " +
            "WHEN e.rating = 2 THEN e.salary * 1.2 ELSE e.salary * 1.01 END " +
            "FROM Employee e WHERE e.department.name = 'Engineering'";
        q = cb.create();
        Root<Employee> e = q.from(Employee.class);
        q.where(cb.equal(e.get(employee_.getAttribute("department", 
            Department.class)).
            get(department_.getAttribute("name", String.class)), 
            "Engineering"));
        q.select(e.get(employee_.getAttribute("name", String.class)), 
            cb.selectCase()
                .when(cb.equal(e.get(employee_.getAttribute("rating", 
                    Integer.class)), 1),
                    cb.prod(e.get(employee_.getAttribute("salary", 
                    Long.class)), 1.1))
                .when(cb.equal(e.get(employee_.getAttribute("rating", 
                    Integer.class)), 2), 
                    cb.prod(e.get(employee_.getAttribute("salary", 
                    Long.class)), 1.2))
                .otherwise(cb.prod(e.get(employee_.getAttribute("salary", 
                    Long.class)), 1.01)));    
 
        assertEquivalence(q, jpql);
    }    
/*    
    @AllowFailure
    public void testLiterals() {
        String jpql = "SELECT p FROM Person p where 'Joe' MEMBER OF " + 
            "p.nickNames";
        CriteriaQuery q = cb.create();
        Root<Person> p = q.from(Person.class);
        q.select(p).where(cb.isMember(cb.literal("Joe"), p.get(person_.
            getSet("nickNames", String.class))));
        
        assertEquivalence(q, jpql);
    }
*/
    
    @AllowFailure
    public void testParameters() {
        String jpql = "SELECT c FROM Customer c Where c.status = :stat";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Parameter<Integer> param = cb.parameter(Integer.class);
        q.select(c).where(cb.equal(c.get(customer_.getAttribute("status", 
            Integer.class)), param));
        
        assertEquivalence(q, jpql, new String[]{"stat"}, new Object[] {1});
    }
    
    @AllowFailure
    public void testSelectList() {
        String jpql = "SELECT v.location.street, KEY(i).title, VALUE(i) FROM " + 
            "VideoStore v JOIN v.videoInventory i WHERE v.location.zipCode = " + 
            "'94301' AND VALUE(i) > 0";
        CriteriaQuery q = cb.create();
        Root<VideoStore> v = q.from(VideoStore.class);
        MapJoin<VideoStore, Movie, Integer> inv = v.join(videoStore_.getMap(
            "videoInventory", Movie.class, Integer.class));
        q.where(cb.equal(v.get(videoStore_.getAttribute("location", 
            Address.class)).
            get(address_.getAttribute("zipCode", String.class)), "94301"),
            cb.gt(inv.value(), 0));
        q.select(v.get(videoStore_.getAttribute("location", Address.class)).
            get(address_.getAttribute("street", String.class)), 
            inv.key().get(movie_.getAttribute("title", String.class)), 
                inv.value());
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT NEW CustomerDetails(c.id, c.status, o.quantity) FROM " + 
            "Customer c JOIN c.orders o WHERE o.quantity > 100";
        q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer, Order> o = c.join(customer_.getSet("orders", Order.class));
        q.where(cb.gt(o.get(order_.getAttribute("quantity", Integer.class)), 
            100));
        q.select(cb.select(CustomerDetails.class, 
            c.get(customer_.getAttribute("id", Integer.class)),
            c.get(customer_.getAttribute("status", Integer.class)),
            o.get(order_.getAttribute("quantity", Integer.class))));
        
        assertEquivalence(q, jpql);
    }

    @AllowFailure
    public void testSubqueries() {
        String jpql = "SELECT goodCustomer FROM Customer goodCustomer WHERE " + 
            "goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) FROM " + 
            "Customer c)";
        CriteriaQuery q = cb.create();
        Root<Customer> goodCustomer = q.from(Customer.class);
        Subquery<Double> sq = q.subquery(Double.class);
        Root<Customer> c = sq.from(Customer.class);
        q.where(cb.lt(goodCustomer.get(customer_.getAttribute("balanceOwed", 
            Integer.class)), 
            sq.select(cb.avg(c.get(customer_.getAttribute("balanceOwed", 
            Integer.class))))));
        q.select(goodCustomer);
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS (" + 
            "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp = " + 
            "emp.spouse)";
        q = cb.create();
        Root<Employee> emp = q.from(Employee.class);
        Subquery<Employee> sq1 = q.subquery(Employee.class);
        Root<Employee> spouseEmp = sq1.from(Employee.class);
        sq1.select(spouseEmp);
        sq1.where(cb.equal(spouseEmp, emp.get(employee_.getAttribute("spouse", 
            Employee.class))));
        q.where(cb.exists(sq));
        q.select(emp).distinct(true);
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL (" + 
            "SELECT m.salary FROM Manager m WHERE m.department = emp.department)";
        q = cb.create();
        Root<Employee> emp1 = q.from(Employee.class);
        q.select(emp1);
        Subquery<BigDecimal> sq2 = q.subquery(BigDecimal.class);
        Root<Manager> m = sq2.from(Manager.class);
        sq2.select(m.get(manager_.getAttribute("salary", BigDecimal.class)));
        sq2.where(cb.equal(m.get(manager_.getAttribute("department", 
            Department.class)), 
            emp1.get(employee_.getAttribute("department", 
            Department.class))));
        q.where(cb.gt(emp.get(employee_.getAttribute("salary", Long.class)), 
            cb.all(sq)));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT c FROM Customer c WHERE " + 
            "(SELECT COUNT(o) FROM c.orders o) > 10";
        q = cb.create();
        Root<Customer> c1 = q.from(Customer.class);
        q.select(c1);
        Subquery<Long> sq3 = q.subquery(Long.class);
        Root<Customer> c2 = sq3.correlate(c1); 
        Join<Customer,Order> o = c2.join(customer_.getSet("orders", 
            Order.class));
        q.where(cb.gt(sq3.select(cb.count(o)), 10));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT o FROM Order o WHERE 10000 < ALL (" + 
            "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        q = cb.create();
        Root<Order> o1 = q.from(Order.class);
        q.select(o1);
        Subquery<Integer> sq4 = q.subquery(Integer.class);
        Root<Order> o2 = sq4.correlate(o1);
        Join<Order,Customer> c3 = o2.join(order_.getAttribute("customer", 
            Customer.class));
        Join<Customer,Account> a = c3.join(customer_.getList("accounts", 
            Account.class));
        sq4.select(a.get(account_.getAttribute("balance", Integer.class)));
        q.where(cb.lt(cb.literal(10000), cb.all(sq4)));
        
        assertEquivalence(q, jpql);

        jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < " +
            "ALL (SELECT a.balance FROM c.accounts a)";
        q = cb.create();
        Root<Order> o3 = q.from(Order.class);
        q.select(o3);
        Join<Order,Customer> c4 = o3.join(Order_.customer);
        Subquery<Integer> sq5 = q.subquery(Integer.class);
        Join<Order,Customer> c5 = sq5.correlate(c4);
        Join<Customer,Account> a2 = c5.join(customer_.getList("accounts", 
            Account.class));
        sq5.select(a.get(account_.getAttribute("balance", Integer.class)));
        q.where(cb.lt(cb.literal(10000), cb.all(sq5)));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testGroupByAndHaving() {
        String jpql = "SELECT c.status, AVG(c.filledOrderCount), COUNT(c) FROM "
            + "Customer c GROUP BY c.status HAVING c.status IN (1, 2)";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.groupBy(c.get(customer_.getAttribute("status", Integer.class)));
        q.having(cb.in(c.get(customer_.getAttribute("status", Integer.class))).
            value(1).value(2));
        q.select(c.get(customer_.getAttribute("status", Integer.class)), 
            cb.avg(c.get(customer_.getAttribute("filledOrderCount", 
                Integer.class))),
            cb.count(c));
        
        assertEquivalence(q, jpql);
    }
    
    @AllowFailure
    public void testOrdering() {
        String jpql = "SELECT o FROM Customer c JOIN c.orders o "  
            + "JOIN c.address a WHERE a.state = 'CA' ORDER BY o.quantity DESC, " 
            + "o.totalCost";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        Join<Customer,Order> o = c.join(customer_.getSet("orders", 
            Order.class));
        Join<Customer,Address> a = c.join(customer_.getAttribute("address", 
            Address.class));
        q.where(cb.equal(a.get(address_.getAttribute("state", String.class)), 
            "CA")); 
        q.orderBy(cb.desc(o.get(order_.getAttribute("quantity", 
            Integer.class))), 
            cb.asc(o.get(order_.getAttribute("totalCost", Double.class))));
        q.select(o);
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT o.quantity, a.zipCode FROM Customer c JOIN c.orders " + 
            "JOIN c.address a WHERE a.state = 'CA' ORDER BY o.quantity, " + 
            "a.zipCode";
        q = cb.create();
        Root<Customer> c1 = q.from(Customer.class); 
        Join<Customer,Order> o1 = c1.join(customer_.getSet("orders", 
            Order.class));
        Join<Customer,Address> a1 = c1.join(customer_.getAttribute("address", 
            Address.class));
        q.where(cb.equal(a1.get(address_.getAttribute("state", String.class)), 
            "CA"));
        q.orderBy(cb.asc(o1.get(order_.getAttribute("quantity", 
            Integer.class))), 
            cb.asc(a1.get(address_.getAttribute("zipCode", 
            String.class))));
        q.select(o1.get(order_.getAttribute("quantity", 
            Integer.class)), 
            a1.get(address_.getAttribute("zipCode", String.class)));
        
        assertEquivalence(q, jpql);
        
        jpql = "SELECT o.quantity, o.cost * 1.08 AS taxedCost, a.zipCode " +
            "FROM Customer c JOIN c.orders o JOIN c.address a " + 
            "WHERE a.state = 'CA' AND a.county = 'Santa Clara' " + 
            "ORDER BY o.quantity, taxedCost, a.zipCode";
        q = cb.create();
        Root<Customer> c2 = q.from(Customer.class);
        Join<Customer,Order> o2 = c2.join(customer_.getSet("orders", 
            Order.class));
        Join<Customer,Address> a2 = c2.join(customer_.getAttribute("address", 
            Address.class));
        q.where(cb.equal(a.get(address_.getAttribute("state", String.class)), 
             "CA"),
            cb.equal(a.get(address_.getAttribute("county", String.class)), 
             "Santa Clara"));
        q.orderBy(cb.asc(o.get(order_.getAttribute("quantity", 
            Integer.class))),
            cb.asc(cb.prod(o.get(order_.getAttribute("totalCost", 
            Double.class)), 1.08)),
            cb.asc(a.get(address_.getAttribute("zipCode", String.class))));
        q.select(o.get(order_.getAttribute("quantity", Integer.class)), 
            cb.prod(o.get(order_.getAttribute("totalCost", Double.class)), 
             1.08),
            a.get(address_.getAttribute("zipCode", String.class)));
        
        assertEquivalence(q, jpql);
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
    
    void assertEquivalence(CriteriaQuery c, String jpql, String[] paramNames, 
        Object[] params) {
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

}
