package org.apache.openjpa.persistence.simple;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.PersistenceTestCase;
import org.apache.openjpa.jdbc.meta.ClassMapping;

public class TestTableNamesDefaultToEntityNames
    extends PersistenceTestCase {

    @Override
    protected Class[] getEntityTypes() {
        return new Class[] { NamedEntity.class };
    }

    public void testEntityNames() {
        ClassMapping cm = (ClassMapping) OpenJPAPersistence.getMetaData(
            getEntityManagerFactory(), NamedEntity.class);
        assertEquals("named", cm.getTable().getName());
    }
}
