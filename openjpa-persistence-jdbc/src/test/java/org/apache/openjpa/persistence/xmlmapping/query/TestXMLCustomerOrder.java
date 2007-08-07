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
package org.apache.openjpa.persistence.xmlmapping.query;

import java.io.FileWriter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.textui.TestRunner;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DB2Dictionary;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.jdbc.sql.SQLServerDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.persistence.xmlmapping.xmlbindings.myaddress.*;
import org.apache.openjpa.persistence.xmlmapping.entities.*;
import org.apache.openjpa.persistence.xmlmapping.entities.Customer.CreditRating;

/**
 * Test query with predicates on persistent field mapped to XML column.
 * 
 * @author Catalina Wei
 * @since 1.0.0
 */
public class TestXMLCustomerOrder
    extends SQLListenerTestCase {

    public void setUp() {
        OpenJPAEntityManagerFactory emf = createEMF();
        DBDictionary dict = ((JDBCConfiguration) emf.getConfiguration())
            .getDBDictionaryInstance();

        // skip if dictionary has no support for XML column type
        if (!dict.supportsXMLColumn)
            return;

        setUp(org.apache.openjpa.persistence.xmlmapping.entities.Customer.class
            , org.apache.openjpa.persistence.xmlmapping.entities.Customer
                .CustomerKey.class
            , org.apache.openjpa.persistence.xmlmapping.entities.Order.class
            , org.apache.openjpa.persistence.xmlmapping.entities.EAddress.class
            );
    }

    public static void main(String[] args) {
        TestRunner.run(TestXMLCustomerOrder.class);
    }

    public void testXMLCustomerOrder() {	
        OpenJPAEntityManager em =
            OpenJPAPersistence.cast(emf.createEntityManager());
        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration())
            .getDBDictionaryInstance();

        // skip if dictionary has no support for XML column type 
        if (!dict.supportsXMLColumn)
            return;

        String sqllog = TestXMLCustomerOrder.class.getName();
        sqllog = sqllog.replace('.', '/');
        sqllog = "./" + sqllog;
        if (dict instanceof DB2Dictionary)
            sqllog += ".db2";
        else if (dict instanceof OracleDictionary)
            sqllog += ".oracle";
        else if (dict instanceof SQLServerDictionary)
            sqllog += ".sqlserver";

        // For platform specific expected sqls are under resources.
        // The generated sql of the test is captured and written to file:
        //     ./TestXMLCustomerOrder.log
        // This output file contents should match with the platform specfic 
        // sqls.        
        System.out.println("Expected pushdown SQL log file is in: " + sqllog);
        
        sql.clear();

        try {
            em.getTransaction().begin();
            deleteAllData(em );
            em.getTransaction().commit();

            em.getTransaction().begin();
            loadData(em);
            em.getTransaction().commit();

            em.close();

            // By closing and recreating the EntityManager, 
            // this guarantees that data will be retrieved from 
            // the database rather than just reused from the 
            // persistence context created by the load methods above.

            em = emf.createEntityManager();

            System.err.println("Main started.");
            int test=1;
            List<Address> addrs = em.createQuery(
                "select o.shipAddress from Order o")
                .getResultList();
            for (Address addr : addrs) {
                System.out.println("addr= " + addr.toString());
            }
            String qstrings[] = {
                "select o from Order o",
                "select o from Order o, Order o2 where o.shipAddress.city " +
                    "= o2.shipAddress.city",
                "select o from Order o, Customer c where o.shipAddress.city " +
                    "= c.address.city",
                "select o from Order o where o.shipAddress.city = 'San Jose'"
            };
            String qstring = null;
            for (int i = 0;i < qstrings.length; i++) {
                qstring = qstrings[i];
                List orders = em.createQuery(qstring).getResultList();
                printOrders(orders, test++);
            }

            // query passing parameters
            qstring = "select o from Order o where o.shipAddress.city = ?1";
            Query q5 = em.createQuery(qstring);
            q5.setParameter(1, "San Jose");
            List orders =q5.getResultList();
            printOrders(orders, test++);

            qstring = "select o from Order o where ?1 = o.shipAddress.city";
            Query q6 = em.createQuery(qstring);
            q6.setParameter(1, "San Jose");
            orders = q6.getResultList();
            printOrders(orders, test++);

            em.close();

            // test updates
            em = emf.createEntityManager();
            testUpdateShipaddress(em, test++);

            em.close();
            em = emf.createEntityManager();

            // query after updates 
            orders = em.createQuery("select o from Order o").getResultList();
            System.out.println("After Update:");
            printOrders(orders, test++);

            // queries expecting exceptions
            String[] badqstrings = {
                "select o from Order o where o.shipAddress.city = 95141",
                "select o from Order o where o.shipAddress.street " +
                    "= '555 Bailey'",
                "select o from Order o where o.shipAddress.zip = 95141"
            };
            for (int i = 0; i < badqstrings.length; i++) {
                qstring = badqstrings[i];
                try {
                    System.out.println("\n>> Query "+test+": "+qstring);
                    test++;
                    orders = em.createQuery(qstring).getResultList();
                }
                catch (Exception e){
                    System.out.println("Exception: "+e);
                }  
            }

            dumpSql();
            em.close();
            emf.close();
            System.out.println("Main ended normally.");
        } catch (Exception e){
            System.out.println("Exception: "+e);
            e.printStackTrace();
        }       
    }

    private void dumpSql() {
        String out = "./TestXMLCustomerOrder.log";
        try {
            FileWriter fw = new FileWriter(out);
            for (int i = 0; i < sql.size(); i++) {
                System.out.println(sql.get(i));
                fw.write(sql.get(i)+"\n");
            }
            fw.close();
        } catch (Exception e) {            
        }
    }

    private void printOrders(List orders, int test) {
        System.out.println("\n>> Query "+test);
        System.out.println("result size = "+orders.size());
        for (int i = 0; i < orders.size(); i++) {
            printOrder((Order) orders.get(i));
        }
    }

    private void loadData(EntityManager em) {

        ObjectFactory addressFactory = new ObjectFactory();

        Customer c2 = new Customer();
        c2.setCid( new Customer.CustomerKey("USA", 2) );
        c2.setName("A&J Auto");
        c2.setRating( CreditRating.GOOD );
        c2.setAddress(new EAddress("2480 Campbell Ave", "Campbell", "CA"
                , "95123"));
        em.persist(c2);

        Customer c1 = new Customer();
        c1.setCid( new Customer.CustomerKey("USA", 1) );
        c1.setName("Harry's Auto");
        c1.setRating( CreditRating.GOOD );
        c1.setAddress( new EAddress("12500 Monterey", "San Jose", "CA"
                , "95141"));
        em.persist(c1);

        Order o1 = new Order(10, 850, false, c1);
        USAAddress addr1 = addressFactory.createUSAAddress();
        addr1.setCity("San Jose");
        addr1.setState("CA");
        addr1.setZIP(new Integer("95141"));
        addr1.getStreet().add("12500 Monterey");
        addr1.setName( c1.getName());
        o1.setShipAddress(addr1);
        em.persist(o1);

        Order o2 = new Order(20, 1000, false, c1);
        CANAddress addr2 = addressFactory.createCANAddress();
        addr2.setName(c2.getName());
        addr2.getStreet().add("123 Warden Road");
        addr2.setCity("Markham");
        addr2.setPostalCode("L6G 1C7");
        addr2.setProvince("ON");
        o2.setShipAddress(addr2);
        em.persist(o2);
    }

    private void testUpdateShipaddress(EntityManager em, int test)
        throws Exception {
        em.getTransaction().begin();
        String query = "select o from Order o where o.shipAddress.city " +
        "= 'San Jose'";
        List orders = em.createQuery(query).getResultList(); 
        System.out.println("Before Update: ");
        printOrders(orders, test);
        em.getTransaction().commit();

        // update in separate transaction                    
        Order o = (Order) orders.get(0);
        EntityTransaction et = em.getTransaction();
        et.begin();
        Address addr = o.getShipAddress();
        addr.setCity("Cupertino");
        if (addr instanceof USAAddress)
            ((USAAddress) addr).setZIP(95014);

        // update shipAddress
        o.setShipAddress(addr);
        et.commit();
    }

    private void deleteAllData(EntityManager em) {
        em.createQuery("delete from Order o").executeUpdate();
        em.createQuery("delete from Customer c").executeUpdate();
    }

    private void printOrder(Order o){
        System.out.println(" Customer ID:"+o.getCustomer().getCid());
        System.out.println(" Order Number:"+o.getOid());
        System.out.println("Ship to: "+o.getShipAddress().toString());
        System.out.println();		
    }
}
