package org.apache.openjpa.slice;

import junit.framework.TestCase;

public class DummyTest extends TestCase {
    public void testDeactive() {
        System.err.println("\t\t============================= WARNING ====================================");
        System.err.println("\t\tTest for distributed database is deactivated");
        System.err.println("\t\tTo activate: ");
        System.err.println("\t\t   1. create databases as per META-INF/persistence.xml in openjpa-slice module");
        System.err.println("\t\t   2. uncomment the SureFire plugin in pom.xml");
        System.err.println("\t\t==========================================================================");
    }
}
