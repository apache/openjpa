package org.apache.openjpa.persistence.test;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.EntityManager;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;

public abstract class SingleEMFTest extends TestCase {

    protected EntityManagerFactory emf;
    protected Class[] classes;

    public SingleEMFTest(Class... classes) {
        this.classes = classes;
    }

    /**
     * Can be overridden to return a list of classes that will be used
     * for this test.
     */
    protected Class[] getClasses() {
        return classes;
    }

    /**
     * Modify the properties that are used to create the EntityManagerFactory.
     * By default, this will set up the MetaDataFactory with the
     * persistent classes for this test case. This method can be overridden
     * to add more properties to the map.
     */
    protected void setEMFProps(Map props) {
        // if we have specified a list of persistent classes to examine,
        // then set it in the MetaDataFactory so that our automatic
        // schema generation will work.
        Class[] pclasses = getClasses();
        if (pclasses != null) {
            StringBuilder str = new StringBuilder();
            for (Class c : pclasses)
                str.append(str.length() > 0 ? ";" : "").append(c.getName());

            props.put("openjpa.MetaDataFactory", "jpa(Types=" + str + ")");
        }

        if (clearDatabaseInSetUp()) {
            props.put("openjpa.jdbc.SynchronizeMappings",
                "buildSchema(ForeignKeys=true," +
                    "SchemaAction='add,deleteTableContents')");
        }
    }

    protected boolean clearDatabaseInSetUp() {
        return false;
    }

    public EntityManagerFactory emf() {
        return emf;
    }

    public boolean closeEMF() {
        if (emf == null)
            return false;

        if (!emf.isOpen())
            return false;

        for (Iterator iter = ((AbstractBrokerFactory) OpenJPAPersistence
            .toBrokerFactory(emf)).getOpenBrokers().iterator();
            iter.hasNext(); ) {
            Broker b = (Broker) iter.next();
            if (b != null && !b.isClosed()) {
                EntityManager em = OpenJPAPersistence.toEntityManager(b);
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }

        emf.close();
        return !emf.isOpen();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Map props = new HashMap(System.getProperties());
        setEMFProps(props);
        emf = Persistence.createEntityManagerFactory("test", props);

        if (clearDatabaseInSetUp()) // get an EM to trigger schema manipulations
            emf.createEntityManager().close();
    }

    @Override
    public void tearDown() throws Exception {
        closeEMF();
        super.tearDown();
    }
}