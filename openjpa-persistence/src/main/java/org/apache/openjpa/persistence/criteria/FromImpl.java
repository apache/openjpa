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

package org.apache.openjpa.persistence.criteria;

import java.util.HashSet;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.AbstractCollection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Collection;
import javax.persistence.metamodel.List;
import javax.persistence.metamodel.Map;
import javax.persistence.metamodel.Set;

import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.Types;

/**
 * Represents a bound type, usually an entity that appears in the from clause, 
 * but may also be an embeddable belonging to an entity in the from clause. 
 * Serves as a factory for Joins of associations, embeddables and collections 
 * belonging to the type, and for Paths of attributes belonging to the type.
 * 
 * @param <Z> the parent type of this receiver
 * @param <X> the type represented by this receiver 
 */

public class FromImpl<Z,X> extends PathImpl<Z,X> implements From<Z,X> {
    private java.util.Set<Join<X, ?>> _joins;
    private java.util.Set<Fetch<X, ?>> _fetches;
    private Types.Managed<X> type;
    
    /**
     * Supply the non-null managed type.
     */
    protected FromImpl(Types.Managed<X> type) {
        super(type.getJavaType());
        this.type = type;
    }
    
    protected FromImpl(PathImpl<?,Z> parent, Members.Member<? super Z, ?> m, 
            Class<X> x) {
        super(parent, m, x);
    }
    
    /**
     *  Return the joins that have been made from this receiver.
     */
    public java.util.Set<Join<X, ?>> getJoins() {
        return _joins;
    }

    /**
     *  Join to the given attribute using an inner join.
     */
    public <Y> Join<X, Y> join(Attribute<? super X, Y> attribute) {
        return join(attribute, JoinType.INNER);
    }

    /**
     *  Join to the given attribute using the given join type.
     */
    public <Y> Join<X, Y> join(Attribute<? super X, Y> attribute, JoinType jt) {
        Join<X, Y> join = new Joins.Attribute<X,Y>(this, 
                (Members.Attribute<? super X, Y>) attribute, jt);
        addJoin(join);
        
        return join;
    }

    /**
     *  Join to the given Collection-valued attribute using an inner join.
     */
    public <Y> CollectionJoin<X, Y> join(Collection<? super X, Y> collection) {
        return join(collection, JoinType.INNER);
    }

    /**
     *  Join to the given Collection-valued attribute using the given 
     *  join type.
     */
    public <Y> CollectionJoin<X, Y> join(Collection<? super X, Y> collection, 
        JoinType jt) {
        CollectionJoin<X, Y> join = new Joins.Collection<X, Y>(this, 
             (Members.Collection<? super X, Y>)collection, jt);
        addJoin(join);
         
         return join;
    }

    /**
     *  Join to the given Set-valued attribute using an inner join.
     */
    public <Y> SetJoin<X,Y> join(Set<? super X, Y> set) {
        return join(set, JoinType.INNER);
    }
    
    /**
     *  Join to the given Set-valued attribute using the given join type.
     */
    public <Y> SetJoin<X,Y> join(Set<? super X, Y> set, JoinType jt) {
        SetJoin<X, Y> join = new Joins.Set<X, Y>(this, 
                (Members.Set<? super X, Y>)set, jt);
        addJoin(join);    
        return join;
    }

    /**
     *  Join to the given List-valued attribute using an inner join.
     */
    public <Y> ListJoin<X,Y> join(List<? super X, Y> list) {
        return join(list, JoinType.INNER);
    }

    /**
     *  Join to the given List-valued attribute using the given join type.
     */
    public <Y> ListJoin<X,Y> join(List<? super X, Y> list, JoinType jt) {
        ListJoin<X, Y> join = new Joins.List<X, Y>(this, 
                (Members.List<? super X, Y>)list, jt);
        addJoin(join);    
        return join;
    }
    
    /**
     *  Join to the given Map-valued attribute using an inner join.
     */
    public <K,V> MapJoin<X,K,V> join(Map<? super X,K,V> map) {
        return join(map, JoinType.INNER);
    }

    /**
     *  Join to the given Map-valued attribute using the given join type.
     */
    public <K,V> MapJoin<X,K,V> join(Map<? super X,K,V> map, JoinType jt) {
        MapJoin<X,K,V> join = new Joins.Map<X,K,V>(this, 
                (Members.Map<? super X,K,V>)map, jt);
        addJoin(join);    
        return join;
    }
    
    // String based counterparts

    public <W,Y> Join<W,Y> join(String attr) {
        return join(attr, JoinType.INNER);
    }

    public <W,Y> Join<W,Y> join(String attr, JoinType jt) {
        return (Join<W,Y>)join(type.getAttribute(attr), jt);
    }


    public <W,Y> CollectionJoin<W, Y> joinCollection(String attr) {
        return (CollectionJoin<W,Y>)join(attr, JoinType.INNER);
    }

    public <W,Y> CollectionJoin<W, Y> joinCollection(String attr, JoinType jt) {
        return (CollectionJoin<W,Y>)join(type.getCollection(attr), jt);
    }

    public <W,Y> ListJoin<W, Y> joinList(String attr) {
        return (ListJoin<W,Y>)join(attr, JoinType.INNER);
    }

    public <W,Y> ListJoin<W,Y> joinList(String attr, JoinType jt) {
        return (ListJoin<W,Y>)join(type.getList(attr), jt);
    }

    public <W,K,V> MapJoin<W,K,V> joinMap(String attr) {
        return (MapJoin<W,K,V>)join(type.getMap(attr));
    }

    public <W,K,V> MapJoin<W,K,V>  joinMap(String attr, JoinType jt) {
        return (MapJoin<W,K,V>)join(type.getMap(attr));
    }

    public <W,Y> SetJoin<W, Y>  joinSet(String attr) {
        return (SetJoin<W, Y>)join(type.getSet(attr));
    }

    public <W,Y> SetJoin<W, Y>  joinSet(String attr, JoinType jt) {
        return (SetJoin<W, Y>)join(type.getSet(attr), jt);
    }

    private void addJoin(Join<X,?> join) {
        if (_joins == null)
            _joins = new HashSet<Join<X,?>>();
         _joins.add(join);
    }
    
    
    public <Y> Fetch<X, Y> fetch(Attribute<? super X, Y> assoc, JoinType jt) {
        throw new AbstractMethodError();
    }

    public <Y> Fetch<X,Y> fetch(Attribute<? super X, Y> assoc) {
        throw new AbstractMethodError();
    }

    public <Y> Fetch<X, Y> fetch(AbstractCollection<? super X, ?, Y> assoc,
        JoinType jt) {
        throw new AbstractMethodError();
    }
    
    public <Y> Fetch<X,Y> fetch(AbstractCollection<? super X, ?, Y> assoc) {
        throw new AbstractMethodError();
    }

    //String-based:

    public <Y> Fetch<X, Y> fetch(String assocName) {
        return (Fetch<X, Y>)fetch(type.getAttribute(assocName));
    }

    public <Y> Fetch<X, Y> fetch(String assocName, JoinType jt) {
        return (Fetch<X, Y>)fetch(type.getAttribute(assocName), JoinType.INNER);
    }

    public java.util.Set<Fetch<X, ?>> getFetches() {
        throw new AbstractMethodError();
    }
}
