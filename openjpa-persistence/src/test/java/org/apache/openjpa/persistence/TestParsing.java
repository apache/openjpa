package org.apache.openjpa.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.meta.MetaDataRepository;

public class TestParsing extends TestCase {

    /**
     * Testcase for added OPENJPA-859.
     * 
     * This scenario is testing whether the default annotations are being generated for a class that
     * isn't annotated with a persistence class type (ie: @Entity, @Mapped-Superclass, @Embeddable),
     * but it is in a mapping file.
     * 
     * @throws Exception
     */
    public void testMixedOrmAnno() throws Exception {
        PersistenceProductDerivation pd = new PersistenceProductDerivation();
        Map<String, String> m = new HashMap<String, String>();

        ConfigurationProvider cp = pd.load("", "test_parsing", m);
        OpenJPAConfigurationImpl conf = new OpenJPAConfigurationImpl(true, true);
        cp.setInto(conf);

        MetaDataRepository mdr = conf.getMetaDataRepositoryInstance();
        Set<String> classes = mdr.getPersistentTypeNames(false, null);
        for (String c : classes) {
            Class cls = Class.forName(c);
            mdr.getMetaData(cls, null, true);
        }
    }
}
