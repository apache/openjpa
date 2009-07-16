package org.apache.openjpa.persistence.criteria;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.Bindable;

import org.apache.openjpa.persistence.test.AllowFailure;

/**
 * Tests Criteria Queries that use Join.
 *  
 * @author Pinaki Poddar
 *
 */
public class TestJoinCondition extends CriteriaTest {
    protected Class[] getDomainClasses() {
        return new Class[]{A.class,B.class,C.class,D.class};
    }

    public void testSingleAttributeJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b);
        assertTrue(b.getModel() instanceof Bindable);
        assertSame(B.class, b.getJavaType());
    }
    
    public void testCollectionJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        CollectionJoin<C,D> d = c.join(C_.coll);
        assertSame(D.class, d.getJavaType());
    }
    
    public void testSetJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        SetJoin<C,D> d = c.join(C_.set);
        assertSame(D.class, d.getJavaType());
    }
    
    public void testListJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        ListJoin<C,D> d = c.join(C_.list);
        assertSame(D.class, d.getJavaType());
    }
    
    public void testInnerJoinSingleAttributeWithoutCondition() {
        String jpql = "select a from A a INNER JOIN a.b b";
        CriteriaQuery<?> c = cb.createQuery();
        c.from(A.class).join(A_.b, JoinType.INNER);
        
        assertEquivalence(c, jpql);
    }  
    
    public void testCrossJoinWithoutCondition() {
        String jpql = "select a from A a, C c";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        
        assertEquivalence(cq, jpql);
    }
    
    @AllowFailure
    public void testCrossJoin() {
        String jpql = "select a from A a, C c where a.name=c.name";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        cq.where(cb.equal(a.get(A_.name), c.get(C_.name)));
        
        assertEquivalence(cq, jpql);
    }

    public void testInnerJoinSingleAttribute() {
        String jpql = "select a from A a INNER JOIN a.b b WHERE a.id=b.age";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b);
        cq.where(cb.equal(a.get(A_.id), b.get(B_.age)));
        
        assertEquivalence(cq, jpql);
    }
    
    public void testOuterJoinSingleAttributeWithoutCondition() {
        String jpql = "select a from A a LEFT JOIN a.b b";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b, JoinType.LEFT);
        
        assertEquivalence(cq, jpql);
    }
    
    public void testOuterJoinSingleAttribute() {
        String jpql = "select a from A a LEFT JOIN a.b b where a.id=b.age";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b, JoinType.LEFT);
        cq.where(cb.equal(a.get(A_.id), b.get(B_.age)));
        
        assertEquivalence(cq, jpql);
    }

    public void testSetJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.set d";
        CriteriaQuery<?> c = cb.createQuery();
        c.from(C.class).join(C_.set);
        
        assertEquivalence(c, jpql);
    }
    
    public void testListJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.list d";
        CriteriaQuery<?> c = cb.createQuery();
        c.from(C.class).join(C_.list);
        
        assertEquivalence(c, jpql);
    }
    
    public void testCollectionJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.coll d";
        CriteriaQuery<?> c = cb.createQuery();
        c.from(C.class).join(C_.coll);
        
        assertEquivalence(c, jpql);
    }
    
    public void testMapJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.map d";
        CriteriaQuery<?> c = cb.createQuery();
        c.from(C.class).join(C_.map);
        
        assertEquivalence(c, jpql);
    }
 
    public void testKeyExpression() {
        String jpql = "select c from C c JOIN c.map d where KEY(d)=33";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        MapJoin<C,Integer,D> d = c.join(C_.map);
        cq.where(cb.equal(d.key(),33));
        
        assertEquivalence(cq, jpql);
    }
    
    public void testValueExpression() {
        String jpql = "select c from C c JOIN c.map d where VALUE(d).name='xy'";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        MapJoin<C,Integer,D> d = c.join(C_.map);
        cq.where(cb.equal(d.value().get(D_.name),"xy"));
        
        assertEquivalence(cq, jpql);
    }
    
    public void testFetchJoin() {
        String jpql = "select a from A a JOIN FETCH a.b";
        
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        a.fetch(A_.b);
        
        assertEquivalence(cq, jpql);
    }
    
    public void testOuterFetchJoin() {
        String jpql = "select a from A a LEFT JOIN FETCH a.b";
        
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        a.fetch(A_.b, JoinType.LEFT);
        
        assertEquivalence(cq, jpql);
    }
}
