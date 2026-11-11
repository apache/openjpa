/*
 * TestMetaDataValueIndicator.java
 *
 * Created on October 4, 2006, 1:35 PM
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
package org.apache.openjpa.persistence.jdbc.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.strats.SuperclassDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.ValueMapDiscriminatorStrategy;
import org.apache.openjpa.persistence.Extent;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.jdbc.common.apps.MultiE;
import org.apache.openjpa.persistence.jdbc.common.apps.MultiF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class TestMetaDataValueIndicator extends org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest {
    private OpenJPAEntityManager pm;

    private ClassMapping eMapping;
    private ClassMapping fMapping;

    @BeforeEach
    void setUp() {
        // ### I hate that we have to do this
        Class c = MultiE.class;
        c = MultiF.class;

        pm = (OpenJPAEntityManager)currentEntityManager();

        eMapping =
            (ClassMapping) ((OpenJPAEntityManagerSPI) pm).getConfiguration().
            getMetaDataRepositoryInstance().getMetaData(MultiE.class,
            pm.getClassLoader(), true);
        fMapping =
            (ClassMapping) ((OpenJPAEntityManagerSPI) pm).getConfiguration().
            getMetaDataRepositoryInstance().getMetaData(MultiF.class,
            pm.getClassLoader(), true);
    }

    @AfterEach
    public void tearDown()
        throws Exception {
        if (pm.getTransaction().isActive())
            pm.getTransaction().rollback();
        pm.close();
        super.tearDown();
    }

    @Test
    void metaData() {
        assertEquals(ValueMapDiscriminatorStrategy.class,
                eMapping.getDiscriminator().getStrategy().getClass());
        assertEquals(SuperclassDiscriminatorStrategy.class,
                fMapping.getDiscriminator().getStrategy().getClass());
    }

    @Test
    void operations() {
       deleteAll(MultiE.class);

        pm = (OpenJPAEntityManager)currentEntityManager();
        pm.getTransaction().begin();

        MultiE e = new MultiE();
        e.setString0("foo");
        pm.persist(e);

        MultiF f = new MultiF();
        f.setString0("bar");
        pm.persist(f);

        pm.getTransaction().commit();

        pm = (OpenJPAEntityManager)currentEntityManager();
        //FIXME jthomas
        /* Cant find equivalent of getExtent()
        assertEquals(2, countExtent(pm.getExtent(MultiE.class, true)));
        assertEquals(1, countExtent(pm.getExtent(MultiE.class, false)));
        assertEquals(1, countExtent(pm.getExtent(MultiF.class, true)));
        assertEquals(1, countExtent(pm.getExtent(MultiF.class, false)));

        Object oid = pm.getObjectId
            (pm.getExtent(MultiE.class, true).iterator().next());
         */
        //FIXME remove the next line once the above block is fixed
        Object oid =null;
        pm = (OpenJPAEntityManager)currentEntityManager();
        assertNotNull(pm.getObjectId(oid));
        pm.close();

        pm = (OpenJPAEntityManager)currentEntityManager();
        assertNotNull(pm.getObjectId(oid));
    }

    private int countExtent(Extent e) {
        int count = 0;
        for (Object o : e) {
            count++;
        }
        return count;
    }


}
