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
            org.apache.openjpa.persistence.delimited.identifiers.EntityF.class);
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
    
    // TODO: temp - test on multiple DBs
//    public void testDBCapability() {
//        Connection conn = (Connection)em.getConnection();
//        try {
//            DatabaseMetaData meta = conn.getMetaData();
//            System.out.println("LC - " + meta.storesLowerCaseIdentifiers());
//            System.out.println("LCQ - " + meta.storesLowerCaseQuotedIdentifiers());
//            System.out.println("MC - " + meta.storesMixedCaseIdentifiers());
//            System.out.println("MCQ - " + meta.storesMixedCaseQuotedIdentifiers());
//            System.out.println("UC - " + meta.storesUpperCaseIdentifiers());
//            System.out.println("UCQ - " + meta.storesUpperCaseQuotedIdentifiers());
//            System.out.println("");
//            System.out.println("db product name - " + meta.getDatabaseProductName());
//            System.out.println("db product version - " + meta.getDatabaseProductVersion());
//            System.out.println("driver name - " + meta.getDriverName());
//            System.out.println("driver version - " + meta.getDriverVersion());
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
        
        System.out.println(super.toString(sql));
        
//        getColumnInfo("\"primary_entityF\"", "\"f_name\"", "\"delim_id\"");
//        getColumnInfo("\"primary entityF\"", null, "\"delim id\"");
//        getColumnInfo("\"secondary entityF\"", null, "\"delim id\"");
    }

    // TODO: change to boolean return and remove assert
//        private void getColumnInfo(String tableName, String columnName, String schemaName) {
//            Connection conn = (Connection)em.getConnection();
//            try {
//                DatabaseMetaData meta = conn.getMetaData();
//    //            tableName = "\"" + tableName + "\"";
//                Column[] columns = dict.getColumns(meta, conn.getCatalog(), schemaName, tableName, columnName, conn);
//                System.out.println("columns.length - " + columns.length); 
//                
////                assertEquals(1, columns.length);
//                
//                for (Column column : columns) {
//                    System.out.println("column name - " + column.getName());
//                    System.out.println("column fullName - " + column.getFullName());
//                    System.out.println("column schemaName - " + column.getSchemaName());
//                    System.out.println("column tableName - " + column.getTableName());
//                    System.out.println("column description - " + column.getDescription());
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            finally {
//                try {
//                    conn.commit();
//                    conn.close();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                    fail("problem closing connection");
//                }
//            }
//        }
}
