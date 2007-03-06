package org.apache.openjpa.persistence.test;

import java.util.Map;
import java.util.HashMap;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

public abstract class PersistenceTestCase
    extends TestCase {

    protected OpenJPAEntityManagerFactory emf;

    protected Class[] getEntityTypes() {
        return new Class[0];
    }

    public void setUp() {
        Map props = new HashMap(System.getProperties());
        Class[] types = getEntityTypes();
        if (types != null && types.length > 0) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < types.length; i++) {
                buf.append(types[i].getName());
                if (i != types.length - 1)
                    buf.append(",");
            }
            props.put("openjpa.MetaDataFactory",
                "jpa(Types=" + buf.toString() + ")");
        }
        emf = (OpenJPAEntityManagerFactory)
            Persistence.createEntityManagerFactory("test", props);
    }

    public OpenJPAEntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
