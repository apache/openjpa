/*
 * TestComplexQueries.java
 *
 * Created on October 17, 2006, 2:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

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
package org.apache.openjpa.persistence.query;


import java.util.List;

import org.apache.openjpa.persistence.query.common.apps.ComplexA;
import org.apache.openjpa.persistence.query.common.apps.ComplexB;
import org.apache.openjpa.persistence.query.common.apps.ComplexC;
import org.apache.openjpa.persistence.query.common.apps.ComplexD;
import org.apache.openjpa.persistence.query.common.apps.ComplexE;
import org.apache.openjpa.persistence.query.common.apps.ComplexF;
import org.apache.openjpa.persistence.query.common.apps.ComplexG;
import junit.framework.AssertionFailedError;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;

public class TestComplexQueries extends BaseQueryTest {

    /**
     * Creates a new instance of TestComplexQueries
     */

    public TestComplexQueries(String test) {
        super(test);
    }

    public void setUp() {
        clear();
    }

    public void clear() {
        deleteAll(ComplexA.class);
        deleteAll(ComplexB.class);
        deleteAll(ComplexC.class);
        deleteAll(ComplexD.class);
        deleteAll(ComplexE.class);
        deleteAll(ComplexF.class);
        deleteAll(ComplexG.class);
    }

    public void complexQuery(int size, Object ob, String filter, Class c) {

//        PersistenceManager pm = getPM();
        Broker broker = getBrokerFactory().newBroker();
        broker.setIgnoreChanges(false);
        broker.begin();

        broker.persist(ob, null);
        // test in-memory
        // assertSize (size, pm.newQuery (c, filter));
        broker.commit();

        broker.begin();
        // test against database
//        OpenJPAQuery q = pm.createQuery("SELECT o FROM "+c.getSimpleName()+"o WHERE o."+filter);
        String qstrng =
            "SELECT o FROM " + c.getSimpleName() + " o WHERE o." + filter;
        assertSize(size,
            (List) broker.newQuery(JPQLParser.LANG_JPQL, c, qstrng).execute());

        broker.commit();
        broker.close();

        broker = getBrokerFactory().newBroker();
        broker.begin();
        // test again against a new PM, for good measure
        assertSize(size,
            (List) broker.newQuery(JPQLParser.LANG_JPQL, c, qstrng).execute());
        broker.rollback();

        broker.close();
    }

    public void testComplex1() {
        complexQuery(1,
            new ComplexA("test", 0, null, null),
            "stringA = \'test\'",
            ComplexA.class);
    }

    public void testComplex2() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "stringG = \'testg\'",
            ComplexG.class);
    }

    public void testComplex3() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'",
            ComplexG.class);
    }

    public void testComplex4() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'"
                + " AND o.f.e.d.c.b.stringB = \'testb\'",
            ComplexG.class);
    }

    public void testComplex5() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'"
                + " AND o.f.e.d.c.b.stringB = \'testb\'"
                + " AND o.f.e.d.c.stringC = \'testc\'",
            ComplexG.class);
    }

    public void testComplex6() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'"
                + " AND o.f.e.d.c.b.stringB = \'testb\'"
                + " AND o.f.e.d.c.stringC = \'testc\'"
                + " AND o.f.e.d.stringD = \'testd\'",
            ComplexG.class);
    }

    public void testComplex7() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'"
                + " AND o.f.e.d.c.b.stringB = \'testb\'"
                + " AND o.f.e.d.c.stringC = \'testc\'"
                + " AND o.f.e.d.stringD = \'testd\'"
                + " AND o.f.e.stringE = \'teste\'",
            ComplexG.class);
    }

    public void testComplex8() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'"
                + " AND o.f.e.d.c.b.stringB = \'testb\'"
                + " AND o.f.e.d.c.stringC = \'testc\'"
                + " AND o.f.e.d.stringD = \'testd\'"
                + " AND o.f.e.stringE = \'teste\'"
                + " AND o.f.stringF = \'testf\'",
            ComplexG.class);
    }

    public void testComplex9() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.e.d.c.b.a.stringA = \'testa\'"
                + " AND o.f.e.d.c.b.stringB = \'testb\'"
                + " AND o.f.e.d.c.stringC = \'testc\'"
                + " AND o.f.e.d.stringD = \'testd\'"
                + " AND o.f.e.stringE = \'teste\'"
                + " AND o.f.stringF = \'testf\'"
                + " AND o.f.e.d.intD < 1"
                + " AND o.f.e.d.intD > -1",
            ComplexG.class);
    }

    public void testComplex10() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.stringF = \'testf\'",
            ComplexG.class);
    }

    public void testComplex11() {
        complexQuery(1,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.stringF = \'testf\' AND o.stringG = \'testg\'",
            ComplexG.class);
    }

    public void testComplex12() {
        try {
            complexQuery(1,
                new ComplexG("testg", 0, null,
                    new ComplexF("testf", 0, null, null,
                        new ComplexE("teste", 0, null, null,
                            new ComplexD("testd", 0, null, null,
                                new ComplexC("testc", 0, null, null,
                                    new ComplexB("testb", 0, null, null,
                                        new ComplexA("testa", 0, null,
                                            null))))))),
                "f.stringF = \'testf\'OR o.stringG = \'testg\'",
                ComplexG.class);
        } catch (junit.framework.AssertionFailedError afe) {
            bug(449, afe, "ORs and relational queries");
        }
    }

    public void testComplex13() {
        complexQuery(0,
            new ComplexG("testg", 0, null,
                new ComplexF("testf", 0, null, null,
                    new ComplexE("teste", 0, null, null,
                        new ComplexD("testd", 0, null, null,
                            new ComplexC("testc", 0, null, null,
                                new ComplexB("testb", 0, null, null,
                                    new ComplexA("testa", 0, null, null))))))),
            "f.stringF = \'testfXXX\'",
            ComplexG.class);
    }

    public void testRelationsIgnoreCache() {
        try {
            relationsIgnoreCacheTest();
        } catch (AssertionFailedError afe) {
            bug(631, afe, "IgnoreCache=false queries do not detect "
                + "all relationship changed");
        }
    }

    public void relationsIgnoreCacheTest() {
        OpenJPAEntityManager pm;
        ComplexC c = new ComplexC();
        ComplexB b = new ComplexB();
        ComplexA a = new ComplexA();

        c.setB(b);
        b.setA(a);

        a.setStringA("Foo");
        pm = getPM();
        startTx(pm);
        pm.persist(c);
        endTx(pm);

        assertSize(1,
            getPM().createNativeQuery("b.a.stringA = \'Foo\'", ComplexC.class));
        assertSize(0, getPM().createNativeQuery("b.a.stringA = \'FooX\'",
            ComplexC.class));

        pm = getPM();
        startTx(pm);
        ComplexA pca = (ComplexA) pm.createExtent(ComplexA.class, false)
            .iterator().next();
        ComplexB pcb = (ComplexB) pm.createExtent(ComplexB.class, false)
            .iterator().next();
        ComplexC pcc = (ComplexC) pm.createExtent(ComplexC.class, false)
            .iterator().next();

        pcc.getB().getA().setStringA("Foo2"); // change the value for

        final OpenJPAQuery q;

        q = pm.createNativeQuery("b.a.stringA = \'Foo\'", ComplexC.class);
        //FIXME jthomas
        //q.setIgnoreCache(true);
        assertEquals(1, q.getMaxResults());

        //FIXME jthomas
        //q.setIgnoreCache(false);
        assertEquals(0, q.getMaxResults());

        // now see if the ignore cache change picks up the current object
        //FIXME jthomas
        //q.setFilter("b.a.stringA = \'Foo2\'");
        //q.setIgnoreCache(false);
        assertEquals(1, q.getMaxResults());

        /*
         Query query = pm.newQuery (C.class, "b.a = param");
         query.declareParameters ("A param");

         query.setIgnoreCache (true);
         assertSize (1, query.execute (pca));

         query.setIgnoreCache (false);
         assertSize (1, query.execute (pca));

         // now change the instance of b.a and see if we fail (as expected)
         pcb.setA (new A ());

         query.setIgnoreCache (true);
         assertSize (1, query.execute (pca));

         query.setIgnoreCache (false);
         // the bug is that the following line will fail: 1 object is returned
         assertSize (0, query.execute (pca));
        */

        final OpenJPAQuery q2;

        q2 = pm.createNativeQuery("b.a = param", ComplexC.class);
        //FIXME jthomas
        /*
        q2.declareParameters("ComplexA param");
        
        q2.setIgnoreCache(true);
        assertSize(1, q2.execute(pca));
        
        q2.setIgnoreCache(false);
        assertSize(1, q2.execute(pca));
        
        // now change the instance of b.a to null and see if we fail
        pcb.setA(null);
        q2.setIgnoreCache(true);
        assertSize(1, q2.execute(pca));
        q2.setIgnoreCache(false);
        assertSize(0, q2.execute(pca));
        
        int fbq = ((FetchPlan) q2.getFetchPlan()).
                getFlushBeforeQueries();
        boolean isFlushing =
                fbq = FetchPlanImpl.FLUSH_TRUEOR
                (fbq = FetchPlanImpl.FLUSH_WITH_CONNECTION AND
                KodoJDOHelper.toBroker(pm).hasConnection());
        
        // now change the instance of b.a and see if we fail (as expected)
        pcb.setA(new ComplexA());
        q2.setIgnoreCache(true);
        if (isFlushing)
            assertSize(0, q2.execute(pca)); // we've already flushed earlier
        else
            assertSize(1, q2.execute(pca));
        q2.setIgnoreCache(false);
        assertSize(0, q2.execute(pca));
        
        // now change the instance of b.a back to the orig value
        pcb.setA(pca);
        q2.setIgnoreCache(true);
        if (isFlushing)
            assertSize(0, q2.execute(pca)); // we've already flushed earlier
        else
            assertSize(1, q2.execute(pca));
        q2.setIgnoreCache(false);
        assertSize(1, q2.execute(pca));
        
        endTx(pm);
         */
    }

}
