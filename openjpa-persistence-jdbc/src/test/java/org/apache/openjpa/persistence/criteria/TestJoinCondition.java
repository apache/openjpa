package org.apache.openjpa.persistence.criteria;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.Bindable;

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
        CriteriaQuery cq = cb.create();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b);
        assertTrue(b.getModel() instanceof Bindable);
        assertSame(B.class, b.getJavaType());
        assertSame(B.class, b.getMember().getMemberJavaType());
        assertEquals(Bindable.BindableType.MANAGED_TYPE,
           b.getModel().getBindableType());
    }
    
    public void testCollectionJoinModel() {
        CriteriaQuery cq = cb.create();
        Root<C> c = cq.from(C.class);
        CollectionJoin<C,D> d = c.join(C_.coll);
        assertSame(Collection.class, d.getJavaType());
        assertSame(D.class, d.getMember().getMemberJavaType());
    }
    
    public void testSetJoinModel() {
        CriteriaQuery cq = cb.create();
        Root<C> c = cq.from(C.class);
        SetJoin<C,D> d = c.join(C_.set);
        assertSame(Set.class, d.getJavaType());
        assertSame(D.class, d.getMember().getMemberJavaType());
    }
    
    public void testListJoinModel() {
        CriteriaQuery cq = cb.create();
        Root<C> c = cq.from(C.class);
        ListJoin<C,D> d = c.join(C_.list);
        assertSame(List.class, d.getJavaType());
        assertSame(D.class, d.getMember().getMemberJavaType());
    }
    
    public void testInnerJoinSingleAttributeWithoutCondition() {
        String jpql = "select a from A a INNER JOIN a.b b";
        CriteriaQuery c = cb.create();
        c.from(A.class).join(A_.b, JoinType.INNER);
        
        assertEquivalence(c, jpql);
    }  
    
    public void testCrossJoinWithoutCondition() {
        String jpql = "select a from A a, C c";
        CriteriaQuery cq = cb.create();
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        
        assertEquivalence(cq, jpql);
    }
    
    public void testCrossJoin() {
        String jpql = "select a from A a, C c where a.name=c.name";
        CriteriaQuery cq = cb.create();
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        cq.where(cb.equal(a.get(A_.name), c.get(C_.name)));
        
        assertEquivalence(cq, jpql);
    }

    public void testInnerJoinSingleAttribute() {
        String jpql = "select a from A a INNER JOIN a.b b WHERE a.id=b.age";
        CriteriaQuery cq = cb.create();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b);
        cq.where(cb.equal(a.get(A_.id), b.get(B_.age)));
        
        assertEquivalence(cq, jpql);
    }
    
    public void testOuterJoinSingleAttributeWithoutCondition() {
        String jpql = "select a from A a LEFT JOIN a.b b";
        CriteriaQuery cq = cb.create();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b, JoinType.LEFT);
        
        assertEquivalence(cq, jpql);
    }
    
    public void testOuterJoinSingleAttribute() {
        String jpql = "select a from A a LEFT JOIN a.b b where a.id=b.age";
        CriteriaQuery cq = cb.create();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b, JoinType.LEFT);
        cq.where(cb.equal(a.get(A_.id), b.get(B_.age)));
        
        assertEquivalence(cq, jpql);
    }

    public void testSetJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.set d";
        CriteriaQuery c = cb.create();
        c.from(C.class).join(C_.set);
        
        assertEquivalence(c, jpql);
    }
    
    public void testListJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.list d";
        CriteriaQuery c = cb.create();
        c.from(C.class).join(C_.list);
        
        assertEquivalence(c, jpql);
    }
    
    public void testCollectionJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.coll d";
        CriteriaQuery c = cb.create();
        c.from(C.class).join(C_.coll);
        
        assertEquivalence(c, jpql);
    }
    
    public void testMapJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.map d";
        CriteriaQuery c = cb.create();
        c.from(C.class).join(C_.map);
        
        assertEquivalence(c, jpql);
    }
 
    public void testKeyExpression() {
        String jpql = "select c from C c JOIN c.map d where KEY(d)=33";
        CriteriaQuery cq = cb.create();
        Root<C> c = cq.from(C.class);
        MapJoin<C,Integer,D> d = c.join(C_.map);
        cq.where(cb.equal(d.key(),33));
        
        assertEquivalence(cq, jpql);
    }
    
    public void testValueExpression() {
        String jpql = "select c from C c JOIN c.map d where VALUE(d).name='xy'";
        CriteriaQuery cq = cb.create();
        Root<C> c = cq.from(C.class);
        MapJoin<C,Integer,D> d = c.join(C_.map);
        cq.where(cb.equal(d.value().get(D_.name),"xy"));
        
        assertEquivalence(cq, jpql);
    }
}
