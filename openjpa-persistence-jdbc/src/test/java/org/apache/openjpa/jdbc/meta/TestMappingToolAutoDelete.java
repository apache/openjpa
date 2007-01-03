package org.apache.openjpa.jdbc.meta;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.simple.AllFieldTypes;

import junit.framework.TestCase;


public class TestMappingToolAutoDelete
    extends TestCase {

    private JDBCConfiguration _conf;
    private OpenJPAEntityManagerFactory emf;

    public void setUp() {
        Map props = new HashMap();
        props.put("openjpa.MetaDataFactory",
            "jpa(Types=" + AllFieldTypes.class.getName() + ")");
        emf = OpenJPAPersistence.cast( 
            Persistence.createEntityManagerFactory("test", props));
        _conf = (JDBCConfiguration) emf.getConfiguration();
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new AllFieldTypes());
        em.getTransaction().commit();
        em.close();
    }
    
    public void tearDown() {
        emf.close();
    }

    public void testMappingToolAutoDelete() 
        throws IOException, SQLException {
        MappingTool.Flags flags = new MappingTool.Flags();
        
        // indirect validation that comma-separated schema actions work
        flags.schemaAction = SchemaTool.ACTION_ADD + "," 
            + SchemaTool.ACTION_DELETE_TABLE_CONTENTS;
        
        MappingTool.run(_conf, new String[0], flags, null);

        EntityManager em = emf.createEntityManager();
        assertEquals(Long.valueOf(0), 
            em.createQuery("select count(o) from AllFieldTypes o")
                .getSingleResult());
        em.close();
    }
}
