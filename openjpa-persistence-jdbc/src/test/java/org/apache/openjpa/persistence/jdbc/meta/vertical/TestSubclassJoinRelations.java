/*
 * TestSubclassJoinRelations.java
 *
 * Created on October 5, 2006, 4:28 PM
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
package org.apache.openjpa.persistence.jdbc.meta.vertical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.jdbc.common.apps.Base;
import org.apache.openjpa.persistence.jdbc.common.apps.BaseSub1;
import org.apache.openjpa.persistence.jdbc.common.apps.BaseSub1Sub1;
import org.apache.openjpa.persistence.jdbc.common.apps.BaseSub1Sub2;
import org.apache.openjpa.persistence.jdbc.common.apps.BaseSub2;
import org.apache.openjpa.persistence.jdbc.common.apps.Relations;
import org.apache.openjpa.persistence.jdbc.kernel.TestSQLListenerTestCase;
import org.junit.jupiter.api.Test;


public class TestSubclassJoinRelations extends TestSQLListenerTestCase {
    private String _outer = "OUTER";
    private Object _oid = null;

    @Override
    public void setUpTestCase() {
        // make sure all classes are registered
        Class[] reg = new Class[]{
            Base.class, BaseSub1.class, BaseSub2.class,
            BaseSub1Sub1.class, BaseSub1Sub2.class,
        };

        if (((JDBCConfiguration) getConfiguration()).getDBDictionaryInstance().
            joinSyntax == JoinSyntaxes.SYNTAX_DATABASE)
            _outer = "(+)";

       deleteAll(Relations.class);
       deleteAll(Base.class);

        Relations rel = new Relations();
        BaseSub2 sub2 = new BaseSub2();
        sub2.setBaseField(3);
        sub2.setBaseSub2Field(4);
        rel.setBase(sub2);

        BaseSub1 sub1 = new BaseSub1();
        sub1.setBaseField(2);
        sub1.setBaseSub1Field(3);
        rel.setBaseSub1(sub1);

        BaseSub1Sub2 sub1sub2 = new BaseSub1Sub2();
        sub1sub2.setBaseField(4);
        sub1sub2.setBaseSub1Field(5);
        sub1sub2.setBaseSub1Sub2Field(6);
        rel.setBaseSub1Sub2(sub1sub2);

        Base base = new Base();
        base.setBaseField(1);
        rel.getBaseList().add(base);
        rel.getBaseList().add(sub2);

        rel.getBaseSub1List().add(sub1);
        rel.getBaseSub1List().add(sub1sub2);

        rel.getBaseSub1Sub2List().add(sub1sub2);

        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        startTx(pm);
        pm.persist(rel);
        endTx(pm);
        _oid = pm.getObjectId(rel);
        pm.close();
    }

    @Test
    void nullOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        assertNull(rel.getNullBase());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void eagerNullOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("nullBase");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        assertNull(rel.getNullBase());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void emptyBaseList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        assertEquals(0, rel.getEmptyBaseList().size());
        pm.close();

        assertEquals(1, sql.size());
        assertSQL(_outer);
    }

    @Test
    void eagerEmptyBaseList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("emptyBaseList");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        assertEquals(0, rel.getEmptyBaseList().size());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void baseOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        BaseSub2 sub2 = (BaseSub2) rel.getBase();
        assertEquals(3, sub2.getBaseField());
        assertEquals(4, sub2.getBaseSub2Field());
        assertEquals(BaseSub2.class, sub2.getClass());
        pm.close();

        assertEquals(1, sql.size(), sql.toString());
        assertSQL(_outer);
    }

    @Test
    void eagerBaseOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("base");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        BaseSub2 sub2 = (BaseSub2) rel.getBase();
        assertEquals(3, sub2.getBaseField());
        assertEquals(4, sub2.getBaseSub2Field());
        assertEquals(BaseSub2.class, sub2.getClass());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void leafOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) rel.getBaseSub1Sub2();
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
    }

    @Test
    void eagerLeafOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("baseSub1Sub2");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) rel.getBaseSub1Sub2();
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void midOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        BaseSub1 sub1 = (BaseSub1) rel.getBaseSub1();
        assertEquals(2, sub1.getBaseField());
        assertEquals(3, sub1.getBaseSub1Field());
        assertEquals(BaseSub1.class, sub1.getClass());
        pm.close();

        assertEquals(1, sql.size());
        assertSQL(_outer);
    }

    @Test
    void eagerMidOneOne()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("baseSub1");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        BaseSub1 sub1 = (BaseSub1) rel.getBaseSub1();
        assertEquals(2, sub1.getBaseField());
        assertEquals(3, sub1.getBaseSub1Field());
        assertEquals(BaseSub1.class, sub1.getClass());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void baseList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        List list = rel.getBaseList();
        assertEquals(2, list.size());
        Base base = (Base) list.get(0);
        assertEquals(1, base.getBaseField());
        assertEquals(Base.class, base.getClass());
        BaseSub2 sub2 = (BaseSub2) list.get(1);
        assertEquals(3, sub2.getBaseField());
        assertEquals(4, sub2.getBaseSub2Field());
        assertEquals(BaseSub2.class, sub2.getClass());
        pm.close();

        assertEquals(1, sql.size());
        assertSQL(_outer);
    }

    @Test
    void eagerBaseList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("baseList");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        List list = rel.getBaseList();
        assertEquals(2, list.size());
        Base base = (Base) list.get(0);
        assertEquals(1, base.getBaseField());
        assertEquals(Base.class, base.getClass());
        BaseSub2 sub2 = (BaseSub2) list.get(1);
        assertEquals(3, sub2.getBaseField());
        assertEquals(4, sub2.getBaseSub2Field());
        assertEquals(BaseSub2.class, sub2.getClass());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void leafList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        List list = rel.getBaseSub1Sub2List();
        assertEquals(1, list.size());
        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) list.get(0);
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
    }

    @Test
    void eagerLeafList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("baseSub1Sub2List");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(2, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        List list = rel.getBaseSub1Sub2List();
        assertEquals(1, list.size());
        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) list.get(0);
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void midList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertNotSQL(_outer);
        sql.clear();

        List list = rel.getBaseSub1List();
        assertEquals(2, list.size());
        BaseSub1 sub1 = (BaseSub1) list.get(0);
        assertEquals(2, sub1.getBaseField());
        assertEquals(3, sub1.getBaseSub1Field());
        assertEquals(BaseSub1.class, sub1.getClass());
        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) list.get(1);
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(1, sql.size());
        assertSQL(_outer);
    }

    @Test
    void eagerMidList()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        pm.getFetchPlan().addFetchGroup("baseSub1List");
        Relations rel = (Relations) pm.getObjectId(_oid);

        assertEquals(1, sql.size());
        assertSQL(_outer);
        sql.clear();

        List list = rel.getBaseSub1List();
        assertEquals(2, list.size());
        BaseSub1 sub1 = (BaseSub1) list.get(0);
        assertEquals(2, sub1.getBaseField());
        assertEquals(3, sub1.getBaseSub1Field());
        assertEquals(BaseSub1.class, sub1.getClass());
        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) list.get(1);
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(0, sql.size());
    }

    @Test
    void projections()
        throws Exception {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        OpenJPAQuery q = (OpenJPAQuery) pm.createNativeQuery("",Relations.class);
        //FIXME jthomas
        /*
        q.setResult("base, baseSub1, baseSub1Sub2");
        q.setUnique(true);
        Object[] res = (Object[]) q.execute();
         */
        Object[] res=null;
        assertNotNull(res);
        assertEquals(3, res.length);

        BaseSub2 sub2 = (BaseSub2) res[0];
        assertEquals(3, sub2.getBaseField());
        assertEquals(4, sub2.getBaseSub2Field());
        assertEquals(BaseSub2.class, sub2.getClass());

        BaseSub1 sub1 = (BaseSub1) res[1];
        assertEquals(2, sub1.getBaseField());
        assertEquals(3, sub1.getBaseSub1Field());
        assertEquals(BaseSub1.class, sub1.getClass());

        BaseSub1Sub2 sub1sub2 = (BaseSub1Sub2) res[2];
        assertEquals(4, sub1sub2.getBaseField());
        assertEquals(5, sub1sub2.getBaseSub1Field());
        assertEquals(6, sub1sub2.getBaseSub1Sub2Field());
        pm.close();

        assertEquals(1, sql.size());
        assertSQL(_outer);
    }
}
