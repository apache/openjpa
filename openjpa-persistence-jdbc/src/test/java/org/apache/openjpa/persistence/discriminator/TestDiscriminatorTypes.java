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
package org.apache.openjpa.persistence.discriminator;

import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Discriminator;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestDiscriminatorTypes extends SingleEMFTestCase {

    protected void setUp() {
        super.setUp(CharAbstractEntity.class, CharLeafEntity.class,
                CharRootEntity.class, IntegerAbstractEntity.class,
                IntegerLeafEntity.class, IntegerRootEntity.class,
                StringAbstractEntity.class, StringLeafEntity.class,
                StringRootEntity.class);
    }

    public void testCharDiscriminators() {
        EntityManager em = emf.createEntityManager(); // load types

        Discriminator discrim = getMapping("CharAbstractEntity")
                .getDiscriminator();
        assertEquals(new Character('C'), discrim.getValue()); // Generated
        assertEquals(JavaTypes.CHAR, discrim.getJavaType());

        discrim = getMapping("chrLeaf").getDiscriminator();
        assertEquals(new Character('c'), discrim.getValue());
        assertEquals(JavaTypes.CHAR, discrim.getJavaType());

        discrim = getMapping("CharRootEntity").getDiscriminator();
        assertEquals(new Character('R'), discrim.getValue());
        assertEquals(JavaTypes.CHAR, discrim.getJavaType());

        em.close();
    }

    public void testIntDiscriminators() {
        EntityManager em = emf.createEntityManager(); // load the types

        Discriminator discrim = getMapping("IntegerAbstractEntity")
                .getDiscriminator();
        assertEquals(new Integer("IntegerAbstractEntity".hashCode()), discrim
                .getValue()); // Generated value
        assertEquals(JavaTypes.INT, discrim.getJavaType());

        discrim = getMapping("intLeaf").getDiscriminator();
        assertEquals(new Integer("intLeaf".hashCode()), discrim.getValue());
        assertEquals(JavaTypes.INT, discrim.getJavaType());

        discrim = getMapping("IntegerRootEntity").getDiscriminator();
        assertEquals(new Integer(10101), discrim.getValue());
        assertEquals(JavaTypes.INT, discrim.getJavaType());

        em.close();
    }

    public void testStringDiscriminators() {
        EntityManager em = emf.createEntityManager(); // load the types
        Discriminator discrim = getMapping("StringAbstractEntity")
                .getDiscriminator();
        assertEquals("StringAbstractEntity", discrim.getValue()); // Generated
        assertEquals(JavaTypes.STRING, discrim.getJavaType());

        discrim = getMapping("strLeaf").getDiscriminator();
        assertEquals("strLeaf", discrim.getValue());
        assertEquals(JavaTypes.STRING, discrim.getJavaType());

        discrim = getMapping("StringRootEntity").getDiscriminator();
        assertEquals("StringRoot", discrim.getValue());
        assertEquals(JavaTypes.STRING, discrim.getJavaType());
        em.close();
    }

    private ClassMapping getMapping(String name) {
        return (ClassMapping) emf.getConfiguration()
                .getMetaDataRepositoryInstance().getMetaData(name,
                        getClass().getClassLoader(), true);
    }
}
