package org.apache.openjpa.persistence.delimited.identifiers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestManualDelimId extends SQLListenerTestCase {
    OpenJPAEntityManager em;
    int id = 0;
    EntityF entityF;
    JDBCConfiguration conf;
    DBDictionary dict;
    
    public void setUp() throws Exception {
        // TODO: retest with DROP to figure out problem
//        super.setUp(EntityF2.class,DROP_TABLES);
        super.setUp(
            org.apache.openjpa.persistence.delimited.identifiers.
            EntityF.class);
        assertNotNull(emf);
        
        em = emf.createEntityManager();
        assertNotNull(em);
        
        conf = (JDBCConfiguration) emf.getConfiguration();
        dict = conf.getDBDictionaryInstance();
    }

    public void createEntityF(int id) {
        entityF = new EntityF(id, "fName");
        entityF.setNonDelimName("fNonDelimName");
        entityF.setSecName("sec name");
        entityF.addCollectionSet("xxx");
        entityF.addCollectionSet("yyy");
        entityF.addCollectionDelimSet("aaa");
        entityF.addCollectionDelimSet("bbb");
        entityF.addCollectionMap("aaa", "xxx");
        entityF.addCollectionMap("bbb", "yyy");
        entityF.addDelimCollectionMap("www", "xxx");
        entityF.addDelimCollectionMap("yyy", "zzz");
    }
    

    
    public void testCreateF() {
        id++;
        createEntityF(id);
        
        em.getTransaction().begin();
        em.persist(entityF);
        em.getTransaction().commit();
        
        System.out.println(super.toString(sql));
        
    }

    // TODO: change to boolean return and remove assert
}
