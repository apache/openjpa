import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;

public class BrokerImplInitialization extends TestCase {

    public void testInitialization() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(null);
        emf.createEntityManager().close(); // initialization
        long start = System.currentTimeMillis();
        int count = 100000;
        int hash = 0;
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        for(int i = 0; i < count; i++) {
            if (i % 10000 == 0)
                System.out.printf("starting iteration %d after %d millis.\n",
                    i, System.currentTimeMillis() - start);
            EntityManager em = emf.createEntityManager();
            hash |= em.hashCode();
            em.close();
        }
        System.out.printf("took %d millis to complete %d runs.\n", 
            System.currentTimeMillis() - start, count);
        System.out.println("hash: " + hash);
        emf.close();
    }
}
