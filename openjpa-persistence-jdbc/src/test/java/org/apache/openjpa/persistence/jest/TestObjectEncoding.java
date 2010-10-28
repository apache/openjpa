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

package org.apache.openjpa.persistence.jest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.persistence.EntityManagerFactoryImpl;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

/**
 * @author Pinaki Poddar
 *
 */
public class TestObjectEncoding extends TestCase {
    private static EntityManagerFactory _emf;
    private static DocumentBuilderFactory _factory;
    private DocumentBuilder _builder; 
    
    static final String W3C_XML_SCHEMA          = "http://www.w3.org/2001/XMLSchema";
    static final String W3C_XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
    static final String JAXP_SCHEMA_SOURCE      = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String JAXP_SCHEMA_LANGUAGE    = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String JEST_INSTANCE_XSD       = "jest-instance.xsd";
    static final String JEST_INSTANCE_XSD_PATH  = "META-INF/" + JEST_INSTANCE_XSD;

    static SAXParser validatingParser;
    static InputStream xsd;
    static URI _schemaFile;
    
    protected void setUp() throws Exception {
        super.setUp();
        if (_emf == null) {
            _emf = Persistence.createEntityManagerFactory("jest");
            _factory = DocumentBuilderFactory.newInstance();
            _builder = _factory.newDocumentBuilder();
            File f = Files.getFile(JEST_INSTANCE_XSD_PATH, Thread.currentThread().getContextClassLoader());
            assertNotNull(JEST_INSTANCE_XSD_PATH + " not found", f);
            _schemaFile = f.toURI();
            xsd = new FileInputStream(f);
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);

            validatingParser = factory.newSAXParser();
            validatingParser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            validatingParser.setProperty(JAXP_SCHEMA_SOURCE, xsd);
        }
    }

    /**
     * Test encoding of a persistent closure.
     * Ensure that it is compliant to schema.
     * 
     * @throws Exception
     */
    public void testXMLEncoding() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("jest");
        long ssn = System.currentTimeMillis();
        createObjectGraph(ssn);
        
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        JObject p = em.find(JObject.class, ssn);
        assertNotNull(p.getSpouse());
        assertFalse(p.getFriends().isEmpty());
        
        Document doc = encodeXML(em, p);
        
        File file = new File(getName()+".xml");
        writeDoc(doc, file);
        System.err.println("Encoded document written to " + file.getAbsolutePath());
        validateDoc(file);
    }
    
    /**
     * Encode persistent closure of the given object in the given persistent context.
     * 
     * @return XML document
     */
    Document encodeXML(EntityManager em, Object obj) {
        Document doc = _builder.newDocument();
        OpenJPAStateManager sm = ((StoreContext)JPAFacadeHelper.toBroker(em)).getStateManager(obj);
        Element root = doc.createElement("instances");
        doc.appendChild(root);
        new XMLEncoder((MetamodelImpl)em.getMetamodel()).encode(sm, root);
        addSchemaToRoot(doc);
        return doc;
    }
    
    
    void addSchemaToRoot(Document doc) {
        Element root = doc.getDocumentElement();
        String[] nvpairs = new String[] {
                "xmlns:xsi",                     W3C_XML_SCHEMA_INSTANCE,
                "xsi:noNamespaceSchemaLocation", JEST_INSTANCE_XSD,
                "version",                       "1.0",
        };
        for (int i = 0; i < nvpairs.length; i += 2) {
            root.setAttribute(nvpairs[i], nvpairs[i+1]);
        }
    }
    
    /**
     * Write the given XML document to the given file.
     */
    void writeDoc(Document doc, File file) throws Exception {
        OutputStream out = new FileOutputStream(file);
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer serializer = tfactory.newTransformer();
        serializer.setOutputProperty(OutputKeys.METHOD,     "xml");
        serializer.setOutputProperty(OutputKeys.INDENT,     "yes");
        serializer.setOutputProperty(OutputKeys.STANDALONE, "no");
        serializer.setOutputProperty(OutputKeys.ENCODING,   "UTF-8");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        serializer.transform(new DOMSource(doc), new StreamResult(out));
    }
    
    /**
     * Validate the XML in the given file. 
     */
    public void validateDoc(File file) {
        try {
            SchemaErrorDetector detector = new SchemaErrorDetector(file.getAbsolutePath()); 
            InputStream is = new FileInputStream(file);
            validatingParser.parse(is, detector);
            assertTrue(detector.toString(), !detector.hasErrors());
        } catch (SAXException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testRemoteAccessImplicitInActive() {
         assertFalse(((EntityManagerFactoryImpl)_emf).allowsRemoteAccess());
    }
    
    public void testRemoteAccessActiveByConfiguration() {
        Map<String,Object> config = new HashMap<String, Object>();
        config.put("openjpa.RemoteAccess", "true");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("jest", config);
        assertTrue(((EntityManagerFactoryImpl)emf).allowsRemoteAccess());
    }
    
    

    public void testJSONEncoding() throws Exception {
        long ssn = System.currentTimeMillis();
        createObjectGraph(ssn);
        
        // Encoding happens within a transaction
        EntityManager em = _emf.createEntityManager();
        em.getTransaction().begin();
        JObject p = em.find(JObject.class, ssn);
        assertNotNull(p.getSpouse());
        assertFalse(p.getFriends().isEmpty());
        String json = encodeJSON(em, p);
        em.getTransaction().rollback();
        System.err.println(json);
    }
    
    String encodeJSON(EntityManager em, Object obj) {
        OpenJPAStateManager sm = ((StoreContext)JPAFacadeHelper.toBroker(em)).getStateManager(obj);
        StringBuilder root = new JSONEncoder((MetamodelImpl)em.getMetamodel()).encode(sm);
        return root.toString();
     }
    
    /**
     * Create and persist a connected object graph with circular reference. 
     * @param ssn the id for the root object
     */
    void createObjectGraph(long ssn) {
        EntityManager em = _emf.createEntityManager();
        em.getTransaction().begin();
        JObject p1 = new JObject(); p1.setAge(20); p1.setName("P1");  p1.setSsn(ssn);
        JObject p2 = new JObject(); p2.setAge(20); p2.setName("P2");  p2.setSsn(ssn+1);
        JObject p3 = new JObject(); p3.setAge(20); p3.setName("P3");  p3.setSsn(ssn+2);
        em.persist(p1); em.persist(p2); em.persist(p3);
        p1.setSpouse(p2); p2.setSpouse(p1);
        p1.addFriend(p2); p1.addFriend(p3);
        p2.addFriend(p1); 
        p3.addFriend(p3);
        em.getTransaction().commit();
    }


}
