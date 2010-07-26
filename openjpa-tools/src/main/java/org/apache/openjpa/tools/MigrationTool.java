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
package org.apache.openjpa.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.openjpa.tools.action.Actions;
import org.apache.openjpa.tools.util.CommandProcessor;
import org.apache.openjpa.tools.util.LogRecordFormatter;
import org.apache.openjpa.tools.util.Option;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Converts a XML document to another XML document based on a set of
 * actions that are themselves specified through a XML descriptor constrained by a XML Schema.
 * <br>
 * The concrete use case for such extreme XMLmania is to convert a set of Hibernate Mapping or 
 * Configuration files to JPA standard mapping/configuration files.
 * <p>
 * Usage:<br>
 * <code>$ java org.apache.openjpa.tools.MigrationTool -i input [-o output] [-err error output] 
 * [-rule rule xml] [-verbose]</code>
 * <br>
 * Simply typing<br>
 * <code>$ java org.apache.openjpa.tools.MigrationTool</code>
 * <br>
 * prints the usage of each options.  
 * 
 * <p>
 * <B>Design/Implementation Note</B>:<br>
 * The Hibernate mapping descriptor is <em>not</em> isomorphic to JPA mapping descriptor.
 * Besides isomorphic changes such as an attribute <code>not-null="true"</code> 
 * becoming <code>optional="false"</code>, more complex transformations can occur, 
 * such as <br>
 * <LI>some attributes can become elements (e.g. <code>&lt;column&gt;</code>)
 * <LI>some elements can become attributes (e.g. <code>&lt;cache&gt;</code>
 * <LI>some elements can split into two elements that are added at different hierarchy in
 * the XML document (e.g. <code>&lt;complex-id&gt;</code>
 * <LI>new elements that have no counterpart in Hibernate may be inserted 
 * (e.g. <code>&lt;attributes&gt;</code> below <code>&lt;entity&gt;</code>)
 * <LI>some element can be fully replaced by its own child element (e.g. <code>&lt;bag&gt;</code>)
 * <br>
 * These actions, wherever possible, are parameterized and defined via an 
 * <A HREF="../../../../resources/META-INF/migration-actions.xsd">XML Schema definition</A>.
 * The recipe to transform individual Hibernate elements are described in a 
 *  <A HREF="../../../../resources/META-INF/migration-actions.xml">XML specification</A>
 *  complaint to this schema.
 * 
 *  
 * @author Pinaki Poddar
 *
 */
public class MigrationTool  {
    static final String W3C_XML_SCHEMA       = "http://www.w3.org/2001/XMLSchema"; 
    static final String JAXP_SCHEMA_SOURCE   = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String LOAD_EXTERNAL_DTD    = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    
    static final String ACTION_SCHMA_XSD     = "META-INF/migration-actions.xsd";
    static final String DEFAULT_ACTIONS      = "META-INF/migration-actions.xml";
    
    private InputStream    _input;
    private OutputStream   _ouput;
    private InputStream    _rulebase;
    
    private Document       _target;
    
    /**
     * Maps each node name of the input document to a list of actions.
     */
    private Map<String, List<Actions>> actionMap = new HashMap<String, List<Actions>>();
    
    
    /**
     * Standard JDK logger with a specialized formatter to augment location information
     * in the input document from the SAX parser.
     */
    private Logger             _logger;
    private LogRecordFormatter _formatter;

    public static void main(String[] args) throws Exception {
    }
    
    public void run(String[] args) throws Exception {
        // sets up command processing
        CommandProcessor cp = new CommandProcessor();
        Option<String> inputOption    = cp.register(String.class, true,  true, "-input","-i");
        Option<String> outputOption   = cp.register(String.class, false, true, "-output","-o");
        Option<String> errorOption    = cp.register(String.class, false, true, "-error","-err");
        Option<String> rulebase = cp.register(String.class, false, true, "-rules");
        rulebase.setDefault(DEFAULT_ACTIONS);
        Option<Boolean> verbose = cp.register(Boolean.class, false, false, "-verbose","-v");
        
        inputOption.setDescription("Hibernate XML file.");
        outputOption.setDescription("Output file name. Defaults to standard console.");
        errorOption.setDescription("Error output file name. Add a + sign at the end " +
            "to append to an existing file. Defaults to standard error console.");
        rulebase.setDescription("Rules specification XML file. Defaults to " + DEFAULT_ACTIONS);
        verbose.setDescription("Prints detailed trace. Defaults to false.");
        
        cp.setAllowsUnregisteredOption(false);
        if (!cp.validate(args)) {
            System.err.println(cp.usage(MigrationTool.class));
            System.exit(1);
        } else {
            cp.setFrom(args);
        }
        _input = getInputStream(cp.getValue(inputOption));
        _ouput = getOutputStream(cp.getValue(outputOption));
        _rulebase  = getInputStream(cp.getValue(rulebase));
        
        _logger = Logger.getLogger(getClass().getPackage().getName());
        Handler handler = null;
        _formatter = new LogRecordFormatter();
        _formatter.setSource(cp.getValue(inputOption));
        
        if (!cp.isSet(errorOption)) {
            handler = new ConsoleHandler();
        } else {
            String errorOutput = cp.getValue(errorOption);
            if (errorOutput.endsWith("+")) {
                handler = new FileHandler(errorOutput.substring(errorOutput.length()-1), true);
            } else {
                handler = new FileHandler(errorOutput, false);
            }
        }
        _logger.setUseParentHandlers(false);
        handler.setFormatter(_formatter);
        _logger.addHandler(handler);
       _logger.setLevel(cp.isSet(verbose) ? Level.INFO : Level.WARNING);
        
       
       buildRepository(_rulebase);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature(LOAD_EXTERNAL_DTD, Boolean.FALSE);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document source = parser.parse(_input);
        _target = parser.newDocument();
        
        // Do some work
        Element newRoot = executeActions(_target, source.getDocumentElement(), null);
        _target.appendChild(newRoot);
        addSchemaToRoot();
        addComments(newRoot);
        
        
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer serializer = tfactory.newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT,     "yes");
        serializer.setOutputProperty(OutputKeys.STANDALONE, "no");
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        serializer.transform(new DOMSource(_target), new StreamResult(_ouput));
        _ouput.close();
    }
    
    /**
     * Builds a repository of actions by parsing the rule base - a XML specification of rules.
     * 
     * @param is an input stream content of a mapping action specification.
     * @throws Exception
     */
    private void buildRepository(InputStream is) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        InputStream xsd = Thread.currentThread().getContextClassLoader().getResourceAsStream(ACTION_SCHMA_XSD);
        if (xsd != null) {
            factory.setAttribute(JAXP_SCHEMA_SOURCE, xsd); 
        }
        factory.setIgnoringElementContentWhitespace(true);
        
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.parse(is);
        
        Element root = doc.getDocumentElement();
        NodeList actions = root.getElementsByTagName("actions");
        int M = actions.getLength();
        for (int i = 0; i < M; i++) {
            Element element = (Element)actions.item(i);
            String sourceTag = element.getAttribute("for");
            _logger.info("Building action list for [" + sourceTag + "]");
            actionMap.put(sourceTag, createAction(element));
        }
        is.close();
    }
    
    private List<Actions> createAction(Element e) {
        List<Actions> actions = new ArrayList<Actions>();
        NodeList actionNodes = e.getChildNodes();
        int N = actionNodes.getLength();
        for (int i = 0; i < N; i++) {
            Node node = actionNodes.item(i);;
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element)node;
            Actions a = getActionFor(element);
            actions.add(a);
        }
        return actions;
    }
    
    Actions getActionFor(Element e) {
        String name = e.getNodeName();
        for (Actions.Code c : Actions.Code.values()) {
            if (c.toString().equals(name)) {
                return c.getTemplate(e);
            }
        }
        throw new RuntimeException("No action for " + e.getNodeName());
    }
    
    void addSchemaToRoot() {
        Element root = _target.getDocumentElement();
        String[] nvpairs = new String[] {
                "xmlns",              "http://java.sun.com/xml/ns/persistence/orm",
                "xmlns:xsi",          "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation", "http://java.sun.com/xml/ns/persistence/orm orm_2_0.xsd",
                "version",            "2.0",
        };
        for (int i = 0; i < nvpairs.length; i += 2) {
            root.setAttribute(nvpairs[i], nvpairs[i+1]);
        }
    }
    
    void addComments(Element e) {
        String[] comments = {
                "Generated by OpenJPA Migration Tool",
                "Generated on  : " + new Date()
        };
        Node refChild = e.getFirstChild();
        for (String c : comments) {
            Comment comment = _target.createComment(c);
            e.insertBefore(comment, refChild);
        }
    }
    
    /**
     * Actions in order of their operation.
     * @param target
     * @param source
     * @param current TODO
     * @param actions
     * @return
     */
    protected Element executeActions(Document target, Element source, Element current) {
        List<Actions> actions = actionMap.get(source.getNodeName());
        if (actions == null) {
            _logger.severe("Element [" +  source.getNodeName() + "] is unknown");
            throw new RuntimeException("No action for element [" +  source.getNodeName() + "]");
        }
        if (actions.isEmpty()) {
            return null;
        }
        _logger.info("Processing source [" + source.getNodeName() + "] with " + actions.size() + " actions");
        List<String> consumedAttrs = new ArrayList<String>();
        List<String> consumedElements = new ArrayList<String>();
        // first action must create a target element
        Actions action = actions.get(0);
        Element newElement = (Element)action.run(target, source, current, consumedAttrs, consumedElements);
        Element root = newElement;
        
        for (int i = 1; i < actions.size(); i++) {
            action = actions.get(i);
            _logger.info("Processing source [" + source.getNodeName() + "] "
                + i + "-th Action " + action.getClass().getSimpleName()); 
            Node newNode = action.run(target, source, root, consumedAttrs, consumedElements);
            
            if (newNode != null) {
                switch (action.getOrder()) {
                case RENAME_ATTR:
                    root.setAttributeNode((Attr)newNode);
                    break;
                case INSERT_NODE:
                    root.appendChild(newNode);
                    root = (Element)newNode;
                    break;
                case PROMOTE_ATTR:
                case RENAME_CHILD_NODE:
                case SPLIT_NODE:
                    root.appendChild(newNode);
                    break;
                case IGNORE_ATTR:
                    break;
                case RENAME_NODE:
                    break;
                    default:
                        throw new RuntimeException("Result of " + action + " not handled");
                }
            }
        }
        
        List<Element> subelements = getDirectChildren(source);
        for (Element sub : subelements) {
            if (consumedElements.contains(sub.getNodeName())) {
                continue;
            }
            Element newNode = executeActions(target, sub, root);
            if (newNode != null) {
                root.appendChild(newNode);
            } 
        }
        
        Set<String> leftoverAttrNames = getAttributeNames(source);
        leftoverAttrNames.removeAll(consumedAttrs);
        if (!leftoverAttrNames.isEmpty()) {
            for (String a : leftoverAttrNames) {
                _logger.warning("Attribute [" + a + "] is not processed");
            }
        }
        return newElement;
    }
    
    protected List<Element> getDirectChildren(Node node) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = node.getChildNodes();
        
        int N = children.getLength();
        for (int i = 0; i < N; i++) {
            Node child = children.item(i);
            if (child instanceof Element && child.getParentNode() == node) {
                result.add((Element)child);
            }
        }
        return result;
    }
    
    protected Set<String> getAttributeNames(Element source) {
        Set<String> names = new HashSet<String>();
        NamedNodeMap attrs = source.getAttributes();
        int M = attrs.getLength();
        for (int i = 0; i < M; i++) {
            names.add(attrs.item(i).getNodeName());
        }
        return names;
    }
    
    private InputStream getInputStream(String v) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(v);
        if (is != null)
            return is;
        try {
            return new FileInputStream(new File(v));
        } catch (Exception e) {
            throw new RuntimeException(this + " can not convert [" + v + "] into an input stream", e);
        }
    }
    
    private OutputStream getOutputStream(String v) {
        try {
             return new PrintStream(new File(v));
         } catch (Exception e) {
             throw new RuntimeException(this + " can not convert " + v + " into an output file stream", e);
         }
     }

}
