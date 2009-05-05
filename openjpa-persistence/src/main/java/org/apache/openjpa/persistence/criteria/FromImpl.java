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
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Map;
import javax.persistence.metamodel.Set;

import org.apache.openjpa.persistence.criteria.PathImpl;

/**
 * Represents a bound type, usually an entity that appears in the from clause, 
 * but may also be an embeddable belonging to an entity in the from clause. 
 * Serves as a factory for Joins of associations, embeddables and collections 
 * belonging to the type, and for Paths of attributes belonging to the type.
 * 
 * @param <Z> the parent type of this receiver
 * @param <X> the type represented by this receiver 
 */

public class FromImpl<Z,X> extends PathImpl<X> implements From<Z,X> {
    private java.util.Set<Join<X, ?>> _joins;
    private java.util.Set<Fetch<X, ?>> _fetches;
    
    /**
     * Supply the non-null managed type.
     */
    public FromImpl(ManagedType<X> type) {
        super(type.getJavaType());
    }
    
    /**
     *  Return the joins that have been made from this type.
     *  @return joins made from this type
     */
    public java.util.Set<Join<X, ?>> getJoins() {
        throw new AbstractMethodError();
    }

    /**
     *  Join to the specified attribute using an inner join.
     *  @param attribute  target of the join
     *  @return the resulting join
     */
    public <Y> Join<X, Y> join(Attribute<? super X, Y> attribute) {
        return join(attribute, JoinType.INNER);
    }

    /**
     *  Join to the specified attribute using the given join type.
     *  @param attribute  target of the join
     *  @param jt  join type 
     *  @return the resulting join
     */
    public <Y> Join<X, Y> join(Attribute<? super X, Y> attribute, JoinType jt) {
        throw new AbstractMethodError();
    }

    /**
     *  Join to the specified Collection-valued attribute using an 
     *  inner join.
     *  @param collection  target of the join
     *  @return the resulting join
     */
    public <Y> CollectionJoin<X, Y> join(Collection<? super X, Y> collection) {
        return join(collection, JoinType.INNER);
    }

    /**
     *  Join to the specified Collection-valued attribute using the given 
     *  join type.
     *  @param collection  target of the join
     *  @return the resulting join
     */
    public <Y> CollectionJoin<X, Y> join(Collection<? super X, Y> collection, 
        JoinType jt) {
        throw new AbstractMethodError();
    }

    /**
     *  Join to the specified Set-valued attribute using an inner join.
     *  @param set  target of the join
     *  @return the resulting join
     */
    public <Y> SetJoin<X,Y> join(Set<? super X, Y> set) {
        return join(set, JoinType.INNER);
    }
    
    /**
     *  Join to the specified Set-valued attribute using the given join type.
     *  @param set  target of the join
     *  @return the resulting join
     */
    public <Y> SetJoin<X,Y> join(Set<? super X, Y> set, JoinType jt) {
        throw new AbstractMethodError();
    }

    /**
     *  Join to the specified List-valued attribute using an inner join.
     *  @param set  target of the join
     *  @return the resulting join
     */
    public <Y> ListJoin<X,Y> join(List<? super X, Y> list) {
        return join(list, JoinType.INNER);
    }

    /**
     *  Join to the specified List-valued attribute using the given join type.
     *  @param set  target of the join
     *  @return the resulting join
     */
    public <Y> ListJoin<X,Y> join(List<? super X, Y> list, JoinType jt) {
        throw new AbstractMethodError();
    }
    
    /**
     *  Join to the specified Map-valued attribute using an inner join.
     *  @param set  target of the join
     *  @return the resulting join
     */
    public <K,V> MapJoin<X,K,V> join(Map<? super X,K,V> map) {
        return join(map, JoinType.INNER);
    }

    /**
     *  Join to the specified Map-valued attribute using the given join type.
     *  @param set  target of the join
     *  @return the resulting join
     */
    public <K,V> MapJoin<X,K,V> join(Map<? super X,K,V> map, JoinType jt) {
        throw new AbstractMethodError();
    }
    
    // String based counterparts

    public Join join(String attributeName) {
        throw new AbstractMethodError();
    }

    public Join join(String attributeName, JoinType jt) {
        throw new AbstractMethodError();
    }


    public CollectionJoin joinCollection(String attributeName) {
        throw new AbstractMethodError();
    }

    public CollectionJoin joinCollection(String attributeName, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public ListJoin joinList(String attributeName) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public ListJoin joinList(String attributeName, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public MapJoin joinMap(String attributeName) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public MapJoin joinMap(String attributeName, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public SetJoin joinSet(String attributeName) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public SetJoin joinSet(String attributeName, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Fetch fetch(Attribute assoc) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Fetch fetch(AbstractCollection assoc) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Fetch fetch(String assocName) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Fetch fetch(Attribute assoc, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Fetch fetch(AbstractCollection assoc, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Fetch fetch(String assocName, JoinType jt) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public java.util.Set<Fetch<X, ?>> getFetches() {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

}
