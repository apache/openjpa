package org.apache.openjpa.persistence.simple;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestTableNamesDefaultToEntityNames
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(NamedEntity.class);
    }

    public void testEntityNames() {
        ClassMapping cm = (ClassMapping) OpenJPAPersistence.getMetaData(
            emf, NamedEntity.class);
        assertEquals("named", cm.getTable().getName());
    }
}
