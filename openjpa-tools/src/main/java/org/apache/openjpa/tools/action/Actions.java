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
package org.apache.openjpa.tools.action;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Defines an action that operates on an input XML node in a source document to create another
 * XML node in a target document. The output of an action may not be of the same type of
 * its input node -- for example -- an attribute of the source can be turned into a element
 * in the target.
 * <br>
 * Typical example of such action is to change the attribute values of attribute names or
 * insert an extra node in the tree hierarchy.
 * <br>
 * The parameters of an action (e.g. its source element/attribute, how the values will map
 * etc.) are supplied by a different XML element. This every concrete action has a 
 * constructor with an XML Element as argument. 
 * <br>
 * The actions are enumerated and supports a comparison order. The purpose is to determine
 * suitable order of execution for a list of actions in future.  
 * 
 *  
 * @author Pinaki Poddar
 *
 */
public interface Actions {//extends Comparable<Actions> {
    /**
     * Get the enumeration code of this action.
     * Actions are orderable based on the ordinal values of this enumeration.
     */
    public Code getOrder();
    
    /**
     * Runs this action.
     * 
     * @param targetDoc the target document.
     * @param source the element in the source document to be processed.
     * @param current the target element from the top of the stack. This is the
     * element most likely to add the result of this method.
     * @return the output of this action. Null value is permissible to signal that
     * this action have skipped the given source.  
     */
    public Node run(Document targetDoc, Element source, Element current,
            Collection<String> consumedAttrs, Collection<String> consumedElements);
    
    
    /**
     * An enumeration of different allowed actions.
     * <br>
     * <B>Important</B>: The string value of each enumeration must match the XML node
     * name used for the <A HREF="../../../../../../resources/META-INF/actionrules.xml">
     * defined rules</A>. 
     * <br>
     * <B>Important</B>: The enumeration order is significant. This order will determine
     * the order of execution of a list of actions.
     * 
     *
     */
    public static enum Code {
        RENAME_NODE      ("rename-node",       RenameNode.class),
        RENAME_CHILD_NODE("rename-child-node", RenameChildNode.class),
        RENAME_ATTR      ("rename-attr",       RenameAttr.class),
        PROMOTE_ATTR     ("promote-attr",      PromoteAttr.class),
        INSERT_NODE      ("insert-node",       InsertNode.class),
        IGNORE_ATTR      ("ignore-attr",       IgnoreAttr.class),
        IGNORE_NODE      ("ignore-node",       IgnoreNode.class),
        SPLIT_NODE       ("split-node",        SplitNode.class),
        CUSTOM_NODE      ("custom-node",       CustomNode.class);
        
        private final String xml;
        private final Class<? extends Actions> template;
        
        private Code(String xml, Class<? extends Actions> t) {
            this.xml = xml;
            this.template = t;
        }
        
        /**
         * Gets the name of this code which must match the XML tags used
         * to refer to this action.
         */
        public String toString() {
            return xml;
        }
        
        /**
         * Creates a prototype action instance populated from the given element. 
         *  
         * @param e the element carrying the details to parameterize an action
         * instance of this type.
         * 
         * @return
         */
        public Actions getTemplate(Element e) {
            try {
                return template.getConstructor(Element.class).newInstance(e);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1.getTargetException());
            } catch (Exception e2) {
               throw new RuntimeException(e2);
            }
        }
        
    }
    
    /**
     * An abstract action to ease derivations.
     *
     */
    public abstract static class Abstract implements Actions {
        
        protected final Element original;
        protected final Code order;
        
        protected Abstract(Code o, Element s) {
            order    = o;
            original = s;
        }
        
        
        /**
         * Gets the enumeration code of this action.
         */
        public final Code getOrder() {
            return order;
        }
        
        /**
         * Gets the attribute value as the parameter of the action.
         * This accessor validates that the attribute indeed exists.
         */
        protected String getAttribute(String attrName) {
            if (!original.hasAttribute(attrName)) {
                throw new IllegalArgumentException(this + " requires the input element must " +
                        " have an attribute [" + attrName + "]");
            }
            return original.getAttribute(attrName);
        }
        
        /**
         * Gets the attribute value as the parameter of the action.
         * This accessor validates that either the attribute or the default
         * attribute indeed exists.
         */
        protected String getAttribute(String attrName, String defValue) {
            if (!original.hasAttribute(attrName)) {
                if (defValue == null || defValue.isEmpty()) {
                    throw new IllegalArgumentException(this + " requires the input element must " +
                        " have an attribute [" + attrName + "] or defaulted by the value of [" +
                        defValue + "]");
                } else {
                    return defValue;
                }
            }
            return original.getAttribute(attrName);
        }
        
        protected Element getElementByName(Element base, String name, boolean mustExist) {
            NodeList nodes = base.getElementsByTagName(name);
            int N = nodes.getLength();
            if (N == 0) {
                if (mustExist)
                    throw new IllegalArgumentException(base.getNodeName() + " must have a [" + name 
                            + "] sub-element");
                else
                    return null;
            } else if (N > 1) {
                if (mustExist)
                    throw new IllegalArgumentException(base.getNodeName() + " must have a single " +
                            "[" + name + "] sub-element but has " + N + " elements");
                else {
                    return (Element)nodes.item(0);
                }
            }
            return (Element)nodes.item(0);
        }
        
    }
    
    /**
     * Renames a node.
     * This is often the first action in a series of actions. So that the subsequent
     * actions' output can be appended in a proper tree hierarchy.
     *
     */
    public static class RenameNode extends Abstract {
        private final String sourcetName;
        private final String targetName;
        
        public RenameNode(Element node) {
            super(Code.RENAME_NODE, node);
            targetName  = getAttribute("to", node.getParentNode().getNodeName());
            sourcetName = getAttribute("from", targetName);
        }
        
        /**
         * Creates an element without any attribute or sub-element. 
         */
        public Element run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            consumedElements.add(sourcetName);
            Element newElement = targetDoc.createElement(targetName);
            return newElement;
        }
    }
    
    public static class RenameChildNode  extends Abstract {
        private final String sourcetName;
        private final String targetName;

        public RenameChildNode(Element s) {
            super(Code.RENAME_CHILD_NODE, s);
            sourcetName = getAttribute("from");
            targetName  = getAttribute("to", "from");
        }

        @Override
        public Node run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            Element sourceNode = getElementByName(source, sourcetName, false);
            if (sourceNode == null)
                return null;
            consumedElements.add(sourcetName);
            Element newNode = targetDoc.createElement(targetName);
            NamedNodeMap attrs = sourceNode.getAttributes();
            int M = attrs.getLength();
            for (int i = 0; i< M ; i++) {
                Attr sourceAttr = (Attr)attrs.item(i);
                newNode.setAttribute(sourceAttr.getNodeName(), sourceAttr.getValue());
            }
            return newNode;
        }
    }
    
    public static class RenameAttr  extends Abstract{
        String sourceName;
        String targetName;
        private Map<String, String> _valueMap = new HashMap<String, String>();
        
        public RenameAttr(Element e) {
            super(Code.RENAME_ATTR, e);
            sourceName = e.getAttribute("from");
            targetName = e.getAttribute("to");
            
            NodeList values = e.getElementsByTagName("map-value");
            int M = values.getLength();
            for (int i = 0; i < M; i++) {
                Element item = (Element)values.item(i);
                _valueMap.put(item.getAttribute("from"), item.getAttribute("to"));
            }
        }

        public Attr run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            if (source.hasAttribute(sourceName)) {
                consumedAttrs.add(sourceName);
                Attr newAttr = targetDoc.createAttribute(targetName);
                String sourceAttrValue = source.getAttribute(sourceName);
                String newValue = _valueMap.isEmpty() ? sourceAttrValue :
                    _valueMap.get(sourceAttrValue);
                newAttr.setValue(newValue);
                return newAttr;
            } else {
                return null;
            }
        }
    }
    
    public static class InsertNode  extends Abstract{
        String dummyName;
        
        public InsertNode(Element e) {
            super(Code.INSERT_NODE, e);
            dummyName = e.getAttribute("name");
        }
        
        public Element run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            Element dummy = targetDoc.createElement(dummyName);
            return dummy;
        }

    }
    
    /**
     * An attribute in the source element becomes a sub-element in the target document.
     * 
     * @author Pinaki Poddar
     *
     */
    public static class PromoteAttr  extends Abstract{
        String sourceName;
        String targetName;
        String targetAttrName;
        Map<String,String> borrow = new HashMap<String, String>();
        private Map<String, String> _valueMap = new HashMap<String, String>();
        
        public PromoteAttr(Element e) {
            super(Code.PROMOTE_ATTR, e);
            sourceName = e.getAttribute("from");
            targetName = e.getAttribute("to");
            targetAttrName = e.getAttribute("as");
            
            NodeList values = e.getElementsByTagName("consume-attr");
            int M = values.getLength();
            for (int i = 0; i < M; i++) {
                Element item = (Element)values.item(i);
                borrow.put(item.getAttribute("from"), item.getAttribute("to"));
            }
            values = e.getElementsByTagName("map-value");
            M = values.getLength();
            for (int i = 0; i < M; i++) {
                Element item = (Element)values.item(i);
                _valueMap.put(item.getAttribute("from"), item.getAttribute("to"));
            }
        }
        
        public Element run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            if (!source.hasAttribute(sourceName))
                return null;
            consumedAttrs.add(sourceName);
            Element newElement = targetDoc.createElement(targetName);
            String sourceAttrValue = source.getAttribute(sourceName);
            if (targetAttrName.isEmpty()) {
                String targetAttrValue = _valueMap.containsKey(sourceAttrValue)
                    ? _valueMap.get(sourceAttrValue) : sourceAttrValue;
                newElement.setTextContent(targetAttrValue);
            } else {
                newElement.setAttribute(targetAttrName, sourceAttrValue);
            
                for (Map.Entry<String, String> entry : borrow.entrySet()) {
                    if (source.hasAttribute(entry.getKey())) {
                        consumedAttrs.add(entry.getKey());
                        newElement.setAttribute(entry.getValue(), source.getAttribute(entry.getKey()));
                    }
                }
            }
            return newElement;
        }
    }
    
    
    public static class IgnoreAttr extends Abstract {
        public IgnoreAttr(Element e) {
            super(Code.IGNORE_ATTR, e);
        }

        @Override
        public Node run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            consumedAttrs.add(original.getAttribute("name"));
            return null;
        }
    }
    
    public static class IgnoreNode extends Abstract {
        public IgnoreNode(Element e) {
            super(Code.IGNORE_NODE, e);
        }

        @Override
        public Node run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            consumedElements.add(original.getAttribute("name"));
            return null;
        }
    }
    
    /**
     * Splits the node into two nodes. The one node is returned to the caller.
     * Other node is added to the current nodes parent.
     * 
     */
    public static class SplitNode extends Abstract {
        String sourceName;
        String targetName;
        String sourceAttrName;
        public SplitNode(Element e) {
            super(Code.SPLIT_NODE, e);
            sourceName = e.getAttribute("from");
            targetName = e.getAttribute("to");
            sourceAttrName = e.getAttribute("on");
        }

        @Override
        public Node run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            Element forParent  = targetDoc.createElement(targetName);
            Element sourceChild = getElementByName(source, sourceName, false);
            if (sourceChild == null)
                return null;
            forParent.setAttribute(sourceAttrName, sourceChild.getAttribute(sourceAttrName));
            current.getParentNode().insertBefore(forParent, current); 
            
            return null;
        }
    }
    
    /**
     * This action is for collection field mapping such as &lt;bag&gt; or &lt;set&gt;
     * and currently completely hard-coded.
     * 
     *
     */
    public static class CustomNode extends Abstract {
        public CustomNode(Element e) {
            super(Code.CUSTOM_NODE, e);
        }

        @Override
        public Node run(Document targetDoc, Element source, Element current,
                Collection<String> consumedAttrs, Collection<String> consumedElements) {
            consumedAttrs.add("embed-xml");
            consumedAttrs.add("mutable");
            consumedAttrs.add("outer-join");
            consumedAttrs.add("optimistic-lock");
            consumedAttrs.add("sort");
            
            String realTag = "one-to-many";
            Element realElement = getElementByName(source, realTag, false);
            if (realElement == null) {
                realTag = "many-to-many";
                realElement =  getElementByName(source, realTag, true);
            }
            consumedElements.add(realTag);
            Element newElement = targetDoc.createElement(realTag);
            newElement.setAttribute("name", source.getAttribute("name"));
            consumedAttrs.add("name");
            if (realElement.hasAttribute("class")) {
                newElement.setAttribute("target-entity", realElement.getAttribute("class"));
            }
            if (source.hasAttribute("lazy")) {
                consumedAttrs.add("lazy");
                newElement.setAttribute("fetch", 
                        "true".equals(source.getAttribute("lazy")) ? "LAZY" : "EAGER");
            }
            if (source.hasAttribute("order-by")) {
                consumedAttrs.add("order-by");
                Element orderColumn = targetDoc.createElement("order-column");
                orderColumn.setAttribute("name", source.getAttribute("order-by"));
                newElement.appendChild(orderColumn);
            }
            if (source.hasAttribute("inverse") && "true".equals(source.getAttribute("inverse"))) {
                consumedAttrs.add("inverse");
                newElement.setAttribute("mapped-by","?");
            }
            
            // TODO: Handle original "key" attribute 
            consumedElements.add("key");
//
//            Element keyElement = getElementByName(source, "key", false);
//            if (keyElement != null) {
//                consumedElements.add("key");
//                Element joinColumn = targetDoc.createElement("join-column");
//                joinColumn.setAttribute("name", keyElement.getAttribute("column"));
//                newElement.appendChild(joinColumn);
//            }
            if (source.hasAttribute("cascade")) {
                consumedAttrs.add("cascade");
                Element cascadeElement = targetDoc.createElement("cascade");
                String[] cascades = source.getAttribute("cascade").split("\\,");
                for (String c : cascades) {
                    if (c.indexOf("delete-orphan") != -1) {
                        newElement.setAttribute("orphan-removal", "true");
                    } else {
                        Element cascadeSubElement = targetDoc.createElement("cascade-" + 
                                ("delete".equals(c) ? "remove" : c));
                        cascadeElement.appendChild(cascadeSubElement);
                    }
                }
                newElement.appendChild(cascadeElement);
            }
            
            
            return newElement;
        }
    }
}
