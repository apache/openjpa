package org.apache.openjpa.persistence.delimited.identifiers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

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
    
    @Override
    public void setUp() throws Exception {
        super.setUp(
            org.apache.openjpa.persistence.delimited.identifiers.EntityF.class,
            DROP_TABLES);
        assertNotNull(emf);
        
        em = emf.createEntityManager();
        assertNotNull(em);
        
        conf = (JDBCConfiguration) emf.getConfiguration();
        dict = conf.getDBDictionaryInstance();
    }

    // TODO: remove parameter
    public void createEntityF(int id) {
//        entityF = new EntityF(id, "fName");
        entityF = new EntityF("fName");
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
    
//     TODO: temp - test on multiple DBs
//    public void testDBCapability() {
//        Connection conn = (Connection)em.getConnection();
//        try {
//            DatabaseMetaData meta = conn.getMetaData();
//            System.out.println("LC - " + 
//                meta.storesLowerCaseIdentifiers());
//            System.out.println("LCQ - " + 
//                meta.storesLowerCaseQuotedIdentifiers());
//            System.out.println("MC - " + 
//                meta.storesMixedCaseIdentifiers());
//            System.out.println("MCQ - " + 
//                meta.storesMixedCaseQuotedIdentifiers());
//            System.out.println("UC - " + 
//                meta.storesUpperCaseIdentifiers());
//            System.out.println("UCQ - " + 
//                meta.storesUpperCaseQuotedIdentifiers());
//            System.out.println("");
//            System.out.println("db product name - " + 
//                meta.getDatabaseProductName());
//            System.out.println("db product version - " + 
//                meta.getDatabaseProductVersion());
//            System.out.println("driver name - " + 
//                meta.getDriverName());
//            System.out.println("driver version - " + 
//                meta.getDriverVersion());
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    } 
    
    public void testCreateF() {
        id++;
        createEntityF(id);
        
        em.getTransaction().begin();
        em.persist(entityF);
        em.getTransaction().commit();
        
        runQueries();
        
    }

    
    private void runQueries() {
        em.clear();
        queryOnEntityOnly();
        em.clear();
        queryOnColumnValue();
        em.clear();
        queryCollection();
    }
    
    private void queryOnEntityOnly() {
        String query =
            "SELECT DISTINCT f " +
            "FROM EntityF f";
        Query q = em.createQuery(query);
        List<EntityF> results = (List<EntityF>)q.getResultList();
        assertEquals(1,results.size());
    }
    
    private void queryOnColumnValue() {
        String query =
            "SELECT DISTINCT f " +
            "FROM EntityF f " +
            "WHERE f.name = 'fName'";
        Query q = em.createQuery(query);
        List<EntityF> results = (List<EntityF>)q.getResultList();
        assertEquals(1,results.size());
    }
    
    private void queryCollection() {
        String query =
            "SELECT DISTINCT f " +
            "FROM EntityF f, IN(f.collectionDelimSet) s " +
            "WHERE s = 'aaa'";
        Query q = em.createQuery(query);
        List<EntityF> results = (List<EntityF>)q.getResultList();
        assertEquals(1,results.size());
    }
}
