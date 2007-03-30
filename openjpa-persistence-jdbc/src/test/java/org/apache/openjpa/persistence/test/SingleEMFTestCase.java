package org.apache.openjpa.persistence.test;

import java.util.Map;
import java.util.HashMap;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;

public abstract class SingleEMFTestCase
    extends PersistenceTestCase {

    protected OpenJPAEntityManagerFactory emf;

    /**
     * Initialize entity manager factory.
     *
     * @param props list of persistent types used in testing and/or 
     * configuration values in the form key,value,key,value...
     */
    protected void setUp(Object... props) {
        emf = createEMF(props);
    }

    /**
     * Closes the entity manager factory.
     */
    public void tearDown() {
        if (emf == null)
            return;

        try {
            clear(emf);
        } finally {
            closeEMF(emf);
        }
    }
}
