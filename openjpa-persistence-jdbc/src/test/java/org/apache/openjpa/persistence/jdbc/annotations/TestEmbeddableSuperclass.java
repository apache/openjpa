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
package org.apache.openjpa.persistence.jdbc.annotations;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.strats.ClobValueHandler;
import org.apache.openjpa.jdbc.meta.strats.FullClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.MaxEmbeddedClobFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.StringFieldStrategy;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>Test embeddable superclasses.</p>
 *
 * @author Abe White
 */
class TestEmbeddableSuperclass
    extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(EmbeddableSuper.class, EmbeddableSuperSub.class, CLEAR_TABLES);
    }

    @Test
    void superclassEmbeddedOnly() {
        ClassMapping cls = ((JDBCConfiguration) emf.getConfiguration()).
            getMappingRepositoryInstance().getMapping(EmbeddableSuper.class,
            null, true);
        assertTrue(cls.isEmbeddedOnly());
        assertEquals(NoneClassStrategy.getInstance(), cls.getStrategy());
    }

    @Test
    void subclassMappingDefaultsAndOverrides() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance().
            getMapping(EmbeddableSuperSub.class, null, true);
        assertFalse(cls.isEmbeddedOnly());
        assertInstanceOf(FullClassStrategy.class, cls.getStrategy());
        assertEquals(ClassMetaData.ID_APPLICATION, cls.getIdentityType());
        assertTrue(cls.isOpenJPAIdentity());

        FieldMapping fm = cls.getFieldMapping("pk");
        assertTrue(fm.isPrimaryKey());
        assertEquals(ValueStrategies.SEQUENCE, fm.getValueStrategy());
        assertEquals("ID", fm.getColumns()[0].getName());

        assertNull(cls.getField("trans"));

        fm = cls.getFieldMapping("clob");
        assertEquals("CC", fm.getColumns()[0].getName());
        DBDictionary dict = conf.getDBDictionaryInstance();
        if (dict.getPreferredType(Types.CLOB) == Types.CLOB) {
            if (dict.maxEmbeddedClobSize > 0)
                assertInstanceOf(MaxEmbeddedClobFieldStrategy.class, fm.getStrategy());
            else
                assertInstanceOf(ClobValueHandler.class, fm.getHandler());
        } else
            assertInstanceOf(StringFieldStrategy.class, fm.getStrategy());
    }

    @Test
    void subclassDiscriminatorMapping() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance().
            getMapping(EmbeddableSuperSub.class, null, true);
        assertEquals("DISC", cls.getDiscriminator().getColumns()[0].
            getName());
    }

    @Test
    void versionOverrideMapping() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance().
            getMapping(EmbeddableSuperSub.class, null, true);
        assertEquals("VERSVAL", cls.getVersion().getColumns()[0].getName());
    }

    @Test
    void relationMappings() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance().
            getMapping(EmbeddableSuperSub.class, null, true);
        FieldMapping fm = cls.getFieldMapping("sub");
        assertInstanceOf(RelationFieldStrategy.class, fm.getStrategy());

        fm = cls.getFieldMapping("sup");
        assertEquals(RelationFieldStrategy.class, fm.getStrategy().getClass());
    }

    @Test
    void persistAndFind() {
        EmbeddableSuperSub parent = new EmbeddableSuperSub();
        parent.setClob("parent");
        EmbeddableSuperSub sub = new EmbeddableSuperSub();
        sub.setClob("sub");
        EmbeddableSuperSub sup = new EmbeddableSuperSub();
        sup.setClob("sup");
        parent.setSub(sub);
        parent.setSup(sup);

        OpenJPAEntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persistAll(parent, sub, sup);
        em.getTransaction().commit();
        long pk = parent.getPK();
        em.close();

        em = emf.createEntityManager();
        parent = em.find(EmbeddableSuperSub.class, pk);
        assertEquals("parent", parent.getClob());
        assertEquals("sub", parent.getSub().getClob());
        assertEquals("sup", parent.getSup().getClob());
        em.close();
    }
}
