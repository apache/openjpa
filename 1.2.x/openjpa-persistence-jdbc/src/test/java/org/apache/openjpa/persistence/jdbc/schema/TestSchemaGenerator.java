/*
 * TestSchemaGenerator.java
 *
 * Created on October 6, 2006, 2:57 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.persistence.jdbc.schema;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;
import javax.sql.DataSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;

import org.apache.openjpa.persistence.jdbc.common.apps.*;


import java.lang.annotation.Annotation;
import junit.framework.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;


public class TestSchemaGenerator extends org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest{
        
    /** Creates a new instance of TestSchemaGenerator */
    public TestSchemaGenerator(String name) 
    {
    	super(name);
    }
    
    public void DBMetadataTest()
    throws Exception {
        OpenJPAEntityManagerFactory pmf = (OpenJPAEntityManagerFactory)
        getEmf();
        //FIXME jthomas
        
        //ClassMapping cm = (ClassMapping) KodoJDOHelper.getMetaData
        //    (pmf, RuntimeTest1.class);
        ClassMapping cm =null;
        JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
        
        DataSource ds = (DataSource) conf.getDataSource2(null);
        Connection c = ds.getConnection();
        DatabaseMetaData meta = c.getMetaData();
        DBDictionary dict = conf.getDBDictionaryInstance();
        
        String schema = cm.getTable().getSchema().getName();
        Table[] tables = dict.getTables(meta, c.getCatalog(), schema,
                cm.getTable().getName(), c);
        assertEquals(1, tables.length);
        
        Column[] columns = dict.getColumns(meta, c.getCatalog(), schema,
                cm.getTable().getName(), null, c);
        for (int i = 0; i < columns.length; i++)
            System.out.println("### " + columns[i].getName());
    }
    
    public void testSchemaGen()
    throws Exception {
        OpenJPAEntityManagerFactory pmf = (OpenJPAEntityManagerFactory)
        getEmf();
        OpenJPAEntityManager pm = pmf.createEntityManager();
        JDBCConfiguration con = (JDBCConfiguration) ((OpenJPAEntityManagerSPI) pm).getConfiguration();
        DBDictionary dict = con.getDBDictionaryInstance();
        MappingRepository repos = con.getMappingRepositoryInstance();
        ClassMapping cm = repos.getMapping(RuntimeTest1.class,
                pm.getClassLoader(), true);
        String schemas = cm.getTable().getSchema().getName();
        if (schemas == null)
            schemas = "";
        schemas += "." + cm.getTable().getName();
        
        Map props=new HashMap();
        props.put("openjpa.jdbc.Schemas", schemas);
        
        OpenJPAEntityManagerFactory kpmf =(OpenJPAEntityManagerFactory)
                getEmf(props);
        JDBCConfiguration conf = (JDBCConfiguration) ((OpenJPAEntityManagerFactorySPI) kpmf).getConfiguration();
        
        StringWriter sw = new StringWriter();
        
        SchemaTool.Flags flags = new SchemaTool.Flags();
        flags.writer = sw;
        flags.primaryKeys = true;
        flags.foreignKeys = true;
        flags.indexes = true;
        flags.openjpaTables = true;
        flags.action = SchemaTool.ACTION_REFLECT;
        
        SchemaTool.run(conf, new String[0], flags,
                getClass().getClassLoader());
        
        sw.flush();
        String data = sw.toString();
        assertTrue(data.length() > 0);
    }
}
