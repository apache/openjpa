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

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Type.PersistenceType;

import org.apache.openjpa.persistence.meta.AbstractManagedType;
import org.apache.openjpa.persistence.meta.Members;

/**
 * Represents a bound type, usually an entity that appears in the from clause,
 * but may also be an embeddable belonging to an entity in the from clause.
 * Serves as a factory for Joins of associations, embeddables and collections
 * belonging to the type, and for Paths of attributes belonging to the type.
 *
 * @param <Z> the parent type of this receiver
 * @param <X> the type represented by this receiver
 */

class FromImpl<Z,X> extends PathImpl<Z,X> implements From<Z,X> {
    private java.util.Set<Join<X, ?>> _joins;
    private java.util.Set<Fetch<X, ?>> _fetches;
    private Type<X> type;

    /**
     * Supply the non-null managed type.
     */
    protected FromImpl(AbstractManagedType<X> type) {
        super(type.getJavaType());
        this.type = type;
    }

    protected FromImpl(PathImpl<?,Z> parent, Members.Member<? super Z, ?> m, Class<X> x) {
        super(parent, m, x);
        this.type = (Type<X>)m.getType();
    }

    @Override
    public Type<?> getType() {
        return type;
    }

    /**
     *  Return the joins that have been made from this receiver.
     */
    @Override
    public java.util.Set<Join<X, ?>> getJoins() {
        return Expressions.returnCopy(_joins);
    }

    /**
     *  Join to the given attribute using an inner join.
     */
    @Override
    public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> attribute) {
        return join(attribute, JoinType.INNER);
    }

    /**
     *  Join to the given attribute using the given join type.
     */
    @Override
    public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> attribute, JoinType jt) {
        Join<X, Y> join = new Joins.SingularJoin<>(this,
                (Members.SingularAttributeImpl<? super X, Y>) attribute, jt);
        addJoin(join);

        return join;
    }

    /**
     *  Join to the given Collection-valued attribute using an inner join.
     */
    @Override
    public <Y> CollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection) {
        return join(collection, JoinType.INNER);
    }

    /**
     *  Join to the given Collection-valued attribute using the given
     *  join type.
     */
    @Override
    public <Y> CollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection,
        JoinType jt) {
        CollectionJoin<X, Y> join = new Joins.Collection<>(this,
             (Members.CollectionAttributeImpl<? super X, Y>)collection, jt);
        addJoin(join);

         return join;
    }

    /**
     *  Join to the given Set-valued attribute using an inner join.
     */
    @Override
    public <Y> SetJoin<X,Y> join(SetAttribute<? super X, Y> set) {
        return join(set, JoinType.INNER);
    }

    /**
     *  Join to the given Set-valued attribute using the given join type.
     */
    @Override
    public <Y> SetJoin<X,Y> join(SetAttribute<? super X, Y> set, JoinType jt) {
        SetJoin<X, Y> join = new Joins.Set<>(this, (Members.SetAttributeImpl<? super X, Y>)set, jt);
        addJoin(join);
        return join;
    }

    /**
     *  Join to the given List-valued attribute using an inner join.
     */
    @Override
    public <Y> ListJoin<X,Y> join(ListAttribute<? super X, Y> list) {
        return join(list, JoinType.INNER);
    }

    /**
     *  Join to the given List-valued attribute using the given join type.
     */
    @Override
    public <Y> ListJoin<X,Y> join(ListAttribute<? super X, Y> list, JoinType jt) {
        ListJoin<X, Y> join = new Joins.List<>(this, (Members.ListAttributeImpl<? super X, Y>)list, jt);
        addJoin(join);
        return join;
    }

    /**
     *  Join to the given Map-valued attribute using an inner join.
     */
    @Override
    public <K,V> MapJoin<X,K,V> join(MapAttribute<? super X,K,V> map) {
        return join(map, JoinType.INNER);
    }

    /**
     *  Join to the given Map-valued attribute using the given join type.
     */
    @Override
    public <K,V> MapJoin<X,K,V> join(MapAttribute<? super X,K,V> map, JoinType jt) {
        MapJoin<X,K,V> join = new Joins.Map<>(this, (Members.MapAttributeImpl<? super X,K,V>)map, jt);
        addJoin(join);
        return join;
    }

    // String based counterparts

    @Override
    public <W,Y> Join<W,Y> join(String attr) {
        return join(attr, JoinType.INNER);
    }

    @Override
    public <W,Y> Join<W,Y> join(String name, JoinType jt) {
        assertJoinable(type);
        ManagedType<X> mType = (ManagedType<X>)type;
        Attribute<?, ?> attr = mType.getAttribute(name);
        assertJoinable(attr.getDeclaringType());
        if (attr instanceof SingularAttribute) {
            return join((SingularAttribute)attr, jt);
        } else if (attr instanceof ListAttribute) {
            return join((ListAttribute)attr, jt);
        } else if (attr instanceof SetAttribute) {
            return join((SetAttribute)attr, jt);
        } else if (attr instanceof CollectionAttribute) {
            return join((CollectionAttribute)attr, jt);
        } else if (attr instanceof MapAttribute) {
            return join((MapAttribute)attr, jt);
        } else {
            throw new IllegalArgumentException(name);
        }
    }


    @Override
    public <W,Y> CollectionJoin<W, Y> joinCollection(String attr) {
        assertJoinable(type);
        return (CollectionJoin<W,Y>)join(((ManagedType<X>)type).getCollection(attr), JoinType.INNER);
    }

    @Override
    public <W,Y> CollectionJoin<W, Y> joinCollection(String attr, JoinType jt) {
        assertJoinable(type);
        return (CollectionJoin<W,Y>)join(((ManagedType<X>)type).getCollection(attr), jt);
    }

    @Override
    public <W,Y> ListJoin<W, Y> joinList(String attr) {
        assertJoinable(type);
        return (ListJoin<W,Y>)join(((ManagedType<X>)type).getList(attr), JoinType.INNER);
    }

    @Override
    public <W,Y> ListJoin<W,Y> joinList(String attr, JoinType jt) {
        assertJoinable(type);
        return (ListJoin<W,Y>)join(((ManagedType<X>)type).getList(attr), jt);
    }

    @Override
    public <W,K,V> MapJoin<W,K,V> joinMap(String attr) {
        assertJoinable(type);
        return (MapJoin<W,K,V>)join(((ManagedType<X>)type).getMap(attr));
    }

    @Override
    public <W,K,V> MapJoin<W,K,V>  joinMap(String attr, JoinType jt) {
        assertJoinable(type);
        return (MapJoin<W,K,V>)join(((ManagedType<X>)type).getMap(attr));
    }

    @Override
    public <W,Y> SetJoin<W, Y>  joinSet(String attr) {
        assertJoinable(type);
        return (SetJoin<W, Y>)join(((ManagedType<X>)type).getSet(attr));
    }

    @Override
    public <W,Y> SetJoin<W, Y>  joinSet(String attr, JoinType jt) {
        assertJoinable(type);
        return (SetJoin<W, Y>)join(((ManagedType<X>)type).getSet(attr), jt);
    }

    void assertJoinable(Type<?> type) {
        if (type.getPersistenceType() == PersistenceType.BASIC) {
            throw new IllegalArgumentException(this + " is a basic path and can not be navigated to ");
        }
    }

    private void addJoin(Join<X,?> join) {
        if (_joins == null)
            _joins = new HashSet<>();
         _joins.add(join);
    }


    @Override
    public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> assoc, JoinType jt) {
        return addFetch((Members.Member<? super X, Y>)assoc, jt);
    }

    @Override
    public <Y> Fetch<X,Y> fetch(SingularAttribute<? super X, Y> assoc) {
        return fetch(assoc, JoinType.INNER);
    }

    @Override
    public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> assoc,
        JoinType jt) {
        return addFetch((Members.Member<? super X, Y>)assoc, jt);
    }

    @Override
    public <Y> Fetch<X,Y> fetch(PluralAttribute<? super X, ?, Y> assoc) {
        return fetch(assoc, JoinType.INNER);
    }

    //String-based:

    @Override
    public <X,Y> Fetch<X, Y> fetch(String assocName) {
        return fetch(assocName, JoinType.INNER);
    }

    @Override
    public <X,Y> Fetch<X, Y> fetch(String name, JoinType jt) {
        assertJoinable(type);
        Attribute<? super X,?> attr = ((ManagedType<X>)type).getAttribute(name);
        if (attr.isCollection()) {
            return fetch((PluralAttribute)attr, jt);
        } else {
            return fetch(((SingularAttribute)attr), jt);
        }
    }

    @Override
    public java.util.Set<Fetch<X, ?>> getFetches() {
        return Expressions.returnCopy(_fetches);
    }

    private <Y> Fetch<X,Y> addFetch(Members.Member<? super X, Y> member,
            JoinType jt) {
        Fetch<X,Y> fetch = new FetchPathImpl(this, member, jt);
        if (_fetches == null)
            _fetches = new HashSet<>();
        _fetches.add(fetch);
        return fetch;
    }

    @Override
    public void acceptVisit(CriteriaExpressionVisitor visitor) {
        Expressions.acceptVisit(visitor, this,
            _joins == null ? null : _joins.toArray(new ExpressionImpl<?>[_joins.size()]));
    }

    @Override
    public From<Z,X> getCorrelationParent() {
        return (From<Z,X>)getCorrelatedPath();
    }

	@Override
	public Predicate equalTo(Expression<?> value) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public Predicate equalTo(Object value) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public Predicate notEqualTo(Expression<?> value) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public Predicate notEqualTo(Object value) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public <X> Expression<X> cast(Class<X> type) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public <Y> Join<X, Y> join(Class<Y> entityClass) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public <Y> Join<X, Y> join(Class<Y> entityClass, JoinType joinType) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public <Y> Join<X, Y> join(EntityType<Y> entity) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}

	@Override
	public <Y> Join<X, Y> join(EntityType<Y> entity, JoinType joinType) {
		// TODO Auto-generated method stub
    	throw new UnsupportedOperationException("Not yet implemented (JPA 3.2)");
	}
	
}
