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
package org.apache.openjpa.persistence.graph;

import java.io.Serializable;
import java.util.Properties;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

/**
 * Generic, directed, attributed Relation as a first-class entity.
 * <br>
 * A relation is 
 * <ol>
 * <LI>generic because the vertices it links are generically typed.
 * <LI>directed because it distinguishes the two end points as source and target.
 * <LI>attributed because any arbitrary name-value pair can be associated with a relation.
 * </ol>
 * A relation is immutable in terms of its two vertices. The properties
 * attached to a relation can change.
 *
 * @param <V1> the type of <em>source</em> vertex linked by this relation.
 * @param <V2> the type of <em>target</em> vertex linked by this relation.
 *  
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
@Entity
public class Relation<V1,V2> implements Serializable {
    /**
     * Relation is a first class object with its own identifier.
     */
    @Id
    @GeneratedValue
    private long id;
    
    /**
     * A Relation must have a non-null vertex as source.
     */
    @OneToOne(optional=false)
    private Vertex<V1> source;
    
    /**
     * A Relation must have a non-null vertex as source.
     */
    @OneToOne(optional=false)
    private Vertex<V2> target;
    
    /**
     * The properties of a Relation is a set of key-value pairs and is declared as 
     * <code>java.util.Properties</code>.
     * <br>
     * Declaring the key-value pairs as <code>java.util.Properties</code> makes OpenJPA
     * assume that both key and value will be stored in database as String.
     * This is not <em>strictly</em> correct because <code>java.util.Properties</code>
     * declares its key and value as <code>java.lang.Object</code>. Hence it is possible for an application
     * to insert key and/or value that are not a String but that type information will not be preserved in
     * the database. Subsequently, when loaded from database the key and value
     * both will appear as String and hence it becomes the application's responsibility to decode the
     * Strings back to the actual type. While this provision loses type information, it allows the
     * database record to be readable and more importantly supports query that are predicated on 
     * (equality only) key-value pairs.
     * <br>
     * Another possibility to express key-value pair as
     * <br>
     * <code>Map<String,Serializable> attrs;</code>
     * <br> 
     * This will serialize the values but preserve their types. The down-side is neither a query can be 
     * predicated on value nor are the database records readable.  
     * <br>
     * The third alternative is a Map where keys are String and values are Object 
     * <br>
     * <code>Map<String,Object> attrs;</code>
     * This leads to the whole map being serialized as a single blob of data.
     */
    @ManyToMany(cascade={CascadeType.ALL},fetch=FetchType.LAZY)
    private Properties attrs;
    
    /**
     * Special constructor for byte code enhancement.
     */
    protected Relation() {
    }
    
    /**
     * A relation is immutable in terms of two vertices it connects.
     * Either vertex must not be null.
     */
    public Relation(Vertex<V1> s, Vertex<V2> t) {
        if (s == null)
            throw new NullPointerException("Can not create relation from a null source vertex");
        if (t == null)
            throw new NullPointerException("Can not create relation to a null target vertex");
        source = s;
        target = t;
        attrs = new Properties();
    }
    
    /**
     * Gets generated persistent identity.
     */
    public long getId() {
        return id;
    }
    
    /**
     * Gets the immutable source vertex.
    */
    public Vertex<V1> getSource() {
        return source;
    }
    
    /**
     * Gets the immutable target vertex.
    */
    public Vertex<V2> getTarget() {
        return target;
    }
    
    /**
     * Affirms if the given attribute is associated with this relation.
     */
    public boolean hasAttribute(String attr) {
        return attrs.containsKey(attr);
    }
    
    /**
     * Gets the value of the given attribute.
     * 
     * @return value of the given attribute. A null value does not distinguish whether
     * the attribute was set to a null value or the attribute was absent. 
     */
    public Object getAttribute(String attr) {
        return attrs.get(attr);
    }
    
    public Properties getAttributes() {
        return attrs;
    }

    /**
     * Adds the given key-value pair, overwriting any prior association to the same attribute.
     * 
     * @return the same relation for fluent method-chaining
     */
    public Relation<V1,V2> addAttribute(String attr, Object v) {
        attrs.put(attr, v);
        return this;
    }
    
    /**
     * Removes the given attribute.
     * 
     * @return value of the given attribute that just has been removed. A null value does not 
     * distinguish whether the attribute was set to a null value or the attribute was absent. 
     */
    public Relation<V1,V2> removeAttribute(String attr) {
        attrs.remove(attr);
        return this;
    }
}
