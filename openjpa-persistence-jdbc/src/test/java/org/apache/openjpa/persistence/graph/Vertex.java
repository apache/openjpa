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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

/**
 * Vertex of a persistent graph.
 * <br>
 * A vertex maintains relationship to other connected vertices. 
 * An abstract vertex provides the functionality for a concrete derivation to be member of a graph. 
 * <br>
 * A generic vertex does <em>not</em> define a persistent identity on the derived types. 
 * <br>
 * The only persistent state maintained by a generic vertex is its ({@linkplain Relation relations}
 * to its neighboring vertices.
 * <br>
 * @author Pinaki Poddar
 *
 * @param <V> the type of this vertex.
 */
@SuppressWarnings("serial")

@MappedSuperclass
public abstract class Vertex<V> implements Serializable {
    /**
     * A set of relations starting from this vertex.
     * A vertex owns its relations in a object modeling sense but not in a JPA sense.
     * In a object modeling sense, a relation can not exist without its source vertex.  
     * In a JPA sense, the relational table for a Relation holds a foreign key to the source vertex, 
     * so Relation is the owner of vertex-Relation relation, confusing, eh?  
     */
    @OneToMany(mappedBy="source", 
            cascade=CascadeType.ALL, 
            orphanRemoval=true, 
            targetEntity=Relation.class)
    private Set<Relation<V,?>> relations;
    
    /**
     * Create a relation to the given vertex from this vertex, if no such relation exists.
     * If a relation exists, returns the existing relation.
     *  
     * @param n a non-null vertex.
     */
    public <V2> Relation<V,V2> link(Vertex<V2> n) {
        if (n == null) {
            throw new NullPointerException(this + " can not link to null target");
        }
        Relation<V,V2> r = getRelationTo(n);
        if (r == null) {
            r = new Relation<V,V2>(this, n);
            if (relations == null) {
                relations = new HashSet<Relation<V,?>>();
            }
            relations.add(r);
        }
        return r;
    }
    
    /**
     * Breaks the relation, if exists, from this vertex to the given vertex.
     * Returns the broken link.
     * 
     * @param n a vertex, possibly null.
     */
    public <V2> Relation<V,V2> delink(Vertex<V2> n) {
        Relation<V,V2> r = getRelationTo(n);
        if (r != null) {
            relations.remove(r);
        }
        return r;
    }
    
    /**
     * Gets the relation from this vertex to the given vertex, if exists. Null otherwise.
     * 
     * @param n a vertex, possibly null.
     */
    public <V2> Relation<V,V2> getRelationTo(Vertex<V2> n) {
        if (n == null || relations == null)
            return null;
        for (Relation<V,?> r : relations) {
            if (r.getTarget().equals(n)) {
                return (Relation<V,V2>)r;
            }
        }
        return null;
    }
    
    /**
     * Affirms if the given vertex is linked from this vertex.
     */
    public boolean isRelatedTo(Vertex<V> n) {
        return getRelationTo(n) != null;
    }
    
    /**
     * Gets all the relations starting from this vertex.
     */
    public Set<Relation<V,?>> getRelations() {
        return relations;
    }
    
    /**
     * Gets all the immediate neighbors.
     */
    public Set<Vertex<?>> getNeighbours() {
        if (relations == null)
            return null;
        Set<Vertex<?>> neighbours = new HashSet<Vertex<?>>();
        for (Relation<V,?> r : relations) {
            neighbours.add(r.getTarget());
        }
        return neighbours;
    }

    /**
     * Gets all the immediate neighbors of the given type.
     */
    public <T> Set<Vertex<T>> getNeighbours(Class<T> type, boolean allowSubclass) {
        if (relations == null)
            return null;
        Set<Vertex<T>> neighbours = new HashSet<Vertex<T>>();
        for (Relation<V,?> r : relations) {
            Vertex<?> target = r.getTarget();
            boolean include = allowSubclass ? type.isAssignableFrom(target.getClass()) : type == target.getClass();
            if (include)
                neighbours.add((Vertex<T>)target);
        }
        return neighbours;
    }

}
