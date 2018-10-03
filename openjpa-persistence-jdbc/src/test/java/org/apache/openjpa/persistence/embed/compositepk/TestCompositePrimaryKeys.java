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
package org.apache.openjpa.persistence.embed.compositepk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import junit.framework.Assert;

public class TestCompositePrimaryKeys extends SingleEMFTestCase {

    // NOTE: There are 3 aspects to the fix to OPENJPA-2631, each being tested in some manner in the test
    // methods below. The 3 aspects of the fix are:
    //
    // 1) Fix in ClassMapping which resolves the reported ClassCastEx.
    // 2) After #1, things progressed further, but for some CriteriaBuilder tests incorrect SQL was created as follows:
    // 2.1) An equals expression was created for only one of the columns in the composite PK. To
    // resolve this a fix was made to class EqualExpression.
    // 2.2) An extra parameter marker (?) was added to the SQL. To resolve this a fix was made to class Lit.

    protected EntityManager em;
    private EntityTransaction tx;

    @Override
    public void setUp() {
        super.setUp(DROP_TABLES, Subject.class, SubjectKey.class, SubjectWithIdClass.class, Topic.class);

            em = emf.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            createData();
    }

    /*
     * OpenJPA handles this test just fine with or without the fixes of OPENJPA-2631.
     */
    public void testFindUsingFindOnSubjectKey() {

        Subject s = createSubject();

        Subject s2 = em.find(Subject.class, s.getKey());

        verifySubject(s, s2);
    }

    /*
     * OpenJPA handles this test just fine with or without the fixes of OPENJPA-2631.  This works,
     * compared to other tests, because a select is performed on the key class' fields.
     */
    public void testFindUsingEqualsOnObjectJPQL() {
        Subject s = createSubject();

        TypedQuery<Subject> query = em.createQuery("select distinct s from Subject s where " +
        		"s.key.subjectNummer = :subjectNummer AND s.key.subjectTypeCode = " +
        		":subjectTypeCode", Subject.class);
        query.setParameter("subjectNummer", s.getKey().getSubjectNummer());
        query.setParameter("subjectTypeCode", s.getKey().getSubjectTypeCode());

        Subject s2 = query.getSingleResult();

        verifySubject(s, s2);
    }

    /*
     * Just like the previous test, OpenJPA handles this test just fine with or without the
     * fixes of OPENJPA-2631.  This works, compared to other tests, because a select is
     * performed on the key class' fields.  This slight difference in this test compared to the
     * previous test is that it traverses from Topic to the SubjectKey fields.
     */
    public void testFindUsingJPQLEqualsOnSubjectKeyAttributes() {

        Subject s = createSubject();

        TypedQuery<Topic> query = em.createQuery("select distinct t from Topic t where t.subject.key.subjectNummer = " +
                ":subjectNummer AND t.subject.key.subjectTypeCode = :subjectTypeCode", Topic.class);
        query.setParameter("subjectNummer", s.getKey().getSubjectNummer());
        query.setParameter("subjectTypeCode", s.getKey().getSubjectTypeCode());
        Topic topic = query.getSingleResult();

        verifyResults(topic, s);
    }

    /*
     * This test results in an EXPECTED exception:
     *
     * ArgumentException: An error occurred while parsing the query filter 'select distinct g from Topic g where
     * t.subject.key = :subjectKey'. Error message: JPQL query does not support conditional expression over embeddable
     * class. JPQL string: "key". See section 4.6.3 of the JPA 2.0 specification.
     *
     * The message in the exception tells it all. Per the spec, you can not do a compare on embeddables.
     */
    public void testFindUsingJPQLEqualsOnSubjectKey() {
        try {
            em.createQuery("select distinct t from Topic t where t.subject.key = :subjectKey");
        } catch (Throwable t) {
            // An exception is EXPECTED!
            Assert.assertTrue(t.getMessage().contains("does not support conditional expression"));
        }
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey cannot be cast to
     * [Ljava.lang.Object;]
     * at org.apache.openjpa.jdbc.kernel.exps.Param.appendTo(Param.java:149)
     *
     * With fix #1, this test works fine.
     */
    public void testFindSubjectUsingJPQLEqualsOnSubject() {

        Subject s = createSubject();

        TypedQuery<Subject> query = em.createQuery("select s from Subject s where s = :subject", Subject.class);
        query.setParameter("subject", s);
        Subject s2 = query.getSingleResult();

        verifySubject(s, s2);
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey cannot be cast to
     * [Ljava.lang.Object;]
     * at org.apache.openjpa.jdbc.kernel.exps.Param.appendTo(Param.java:149)
     *
     * With fix #1, this test works fine.
     */
    public void testFindUsingNamedQuery() {

        Subject s = createSubject();

        TypedQuery<Topic> q = em.createNamedQuery("bySubject", Topic.class);

        q.setParameter("subject", s);

        Topic topic = q.getSingleResult();

        verifyResults(topic, s);
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey cannot be cast to
     * [Ljava.lang.Object;]
     * at org.apache.openjpa.jdbc.kernel.exps.Param.appendTo(Param.java:149)
     *
     * With fix #1, this test works fine.
     */
    public void testFindUsingJPQLEqualsOnSubject() {

        Subject s = createSubject();

        TypedQuery<Topic> query =
            em.createQuery("select distinct t from Topic t where t.subject = :subject", Topic.class);
        query.setParameter("subject", s);
        Topic topic = query.getSingleResult();

        verifyResults(topic, s);
    }

    /*
     * Due to the fix #1 (see notes above), this fails on OJ with:
     *
     * java.lang.ArrayIndexOutOfBoundsException: Array index out of range: 0
     * at org.apache.openjpa.jdbc.meta.ClassMapping.toDataStoreValue(ClassMapping.java:272)
     *
     */
    public void testFindUsingJPQLSubjectKeyIn() {
        Query query = em.createQuery("select distinct s from Subject s where s.key in :subjectKeyList");
        query.setParameter("subjectKeyList",
                Arrays.asList(
                    new SubjectKey(1, "Type"),
                    new SubjectKey(2, "Type2"),
                    new SubjectKey(3, "Type3")));
        query.getResultList();
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey cannot be cast to
     * [Ljava.lang.Object;]
     * at org.apache.openjpa.jdbc.kernel.exps.Param.appendTo(Param.java:149)
     *
     * With fix #1, the CCEx is avoided/resolved. However, we then got an incorrectly generated SQL as follows:
     *
     * SELECT t0.SUBJECTNUMMER, t0.CODE_SUBJECTTYPE FROM SUBJECT t0 WHERE (t0.SUBJECTNUMMER = ?)
     *   optimize for 1 row [params=(int) 1]
     *
     * Notice that 't0.CODE_SUBJECTTYPE' is missing.  With fix #2.1 this issue is resolved.
     *
     * The thing to note (which is different than the test 'findSubjectUsingCriteriaBuilderEquals' below) is that
     * the Subject is treated as an OpenJPA 'Parameter' (see changes in EqualExpression). The test
     * 'findSubjectUsingCriteriaBuilderEquals' below causes the Subject to be treated as a Lit. There is
     * a bug in both cases, with an additional bug for the 'Lit' case.
     */
    public void testFindSubjectUsingCriteriaBuilderEqualsAndParameter() {

        Subject s = createSubject();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Subject> cq = builder.createQuery(Subject.class);

        Root<Subject> subjectRoot = cq.from(Subject.class);
        cq.select(subjectRoot);

        ParameterExpression<Subject> param1 = builder.parameter(Subject.class, "subject");
        Predicate subjectPredicate = builder.equal(subjectRoot, param1);

        cq.where(subjectPredicate);

        TypedQuery<Subject> query = em.createQuery(cq);
        query.setParameter("subject", s);

        Subject s2 = query.getSingleResult();

        verifySubject(s, s2);
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * Caused by: java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey
     * cannot be cast to [Ljava.lang.Object;
     *   at org.apache.openjpa.jdbc.kernel.exps.Lit.appendTo(Lit.java:120)
     *
     * Notice the exception this time is in 'Lit'.  Previous CCEx for the other tests have been in Param.
     * With fix #1, the CCEx is avoided/resolved. However, we then got an incorrectly generated SQL as follows:
     *
     * SELECT t0.SUBJECTNUMMER, t0.CODE_SUBJECTTYPE FROM SUBJECT t0 WHERE (t0.SUBJECTNUMMER = ??)
     * optimize for 1 row [params=(int) 1, (String) Type]
     *
     * Notice that 't0.CODE_SUBJECTTYPE' is missing, and there are two parameter markers.  With fix #2.1 and
     * #2.2, this issue is resolved.
     *
     * The other thing to note (which is different than the test 'findSubjectUsingCriteriaBuilderEqualsAndParameter'
     * above) is that the Subject is treated as an OpenJPA 'Lit' (see changes in EqualExpression). The test
     * 'findSubjectUsingCriteriaBuilderEqualsAndParameter' above treats the Subject as a Parameter. There is a bug in
     * both cases, with an additional bug for the 'Lit' case.
     */
    public void testFindSubjectUsingCriteriaBuilderEquals() {

        Subject s = createSubject();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Subject> cq = builder.createQuery(Subject.class);

        Root<Subject> subjectRoot = cq.from(Subject.class);
        cq.select(subjectRoot);

        Predicate subjectPredicate = builder.equal(subjectRoot, s);

        // Before the fix of JIRA OPENJPA-2631, the following was a way to fix/work around the issue, in
        // other words, selecting the individual fields of the PK worked fine....I'll leave this here but
        // commented out for history sake:
        // Predicate subjectPredicate1 = builder.equal(subjectRoot.get(Subject_.key).get(SubjectKey_.subjectNummer),
        // subject.getKey().getSubjectNummer());
        // Predicate subjectPredicate2 = builder.equal(subjectRoot.get(Subject_.key).get(SubjectKey_.subjectTypeCode),
        // subject.getKey().getSubjectTypeCode());
        // Predicate subjectPredicate = builder.and(subjectPredicate1,subjectPredicate2);

        cq.where(subjectPredicate);

        TypedQuery<Subject> query = em.createQuery(cq);

        Subject s2 = query.getSingleResult();

        verifySubject(s, s2);
    }

    /*
     * For comparison, this test does the same CriteriaBuilder code on Topic (an entity
     * with a single PK) as was done in the previous test to make sure it works.
     */
    public void testFindTopicUsingCriteriaBuilderEquals() {

        Topic t = new Topic();
        t.setId(5);

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Topic> cq = builder.createQuery(Topic.class);

        Root<Topic> topicRoot = cq.from(Topic.class);
        cq.select(topicRoot);

        Predicate topicPredicate = builder.equal(topicRoot, t);
        cq.where(topicPredicate);

        TypedQuery<Topic> query = em.createQuery(cq);

        Topic topic = query.getSingleResult();

        verifyResults(topic, createSubject());
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * Caused by: java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey
     * cannot be cast to [Ljava.lang.Object;
     *   at org.apache.openjpa.jdbc.kernel.exps.Lit.appendTo(Lit.java:120)
     *
     * Notice the exception this time is in 'Lit'.  Previous CCEx for the other tests have been in Param.
     * With fix #1, the CCEx is avoided/resolved. However, we then got an incorrectly generated SQL as follows:
     *
     * SELECT t0.ID, t1.SUBJECTNUMMER, t1.CODE_SUBJECTTYPE FROM TOPIC t0 LEFT OUTER JOIN SUBJECT t1 ON
     * t0.SUBJECT_SUBJECTNUMMER =
     * t1.SUBJECTNUMMER AND t0.SUBJECT_CODE_SUBJECTTYPE = t1.CODE_SUBJECTTYPE WHERE (t0.SUBJECT_SUBJECTNUMMER = ??)
     * optimize for 1 row [params=(int) 1, (String) Type]
     *
     * Notice that 't0.CODE_SUBJECTTYPE' is missing, and there are two parameter markers.  With fix #2.1 and
     * #2.2, this issue is resolved.
     */
    public void testFindUsingCriteriaBuilderEquals() {

        Subject s = createSubject();
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Topic> cq = builder.createQuery(Topic.class);

        Root<Topic> topic = cq.from(Topic.class);
        cq.select(topic).distinct(true);

        Predicate topicPredicate = builder.equal(topic.get("subject"), s);
        cq.where(topicPredicate);

        TypedQuery<Topic> query = em.createQuery(cq);
        Topic t = query.getSingleResult();

        verifyResults(t, s);
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * Caused by: java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey
     * cannot be cast to [Ljava.lang.Object;
     *   at org.apache.openjpa.jdbc.kernel.exps.InExpression.orContains(InExpression.java:178)
     *
     * Notice this time the CCEx occurs in InExpression.  With fix #1 the issue is resolved.
     */
    public void testFindUsingJPQLInClauseOnSubject() {
        Subject s = createSubject();
        SubjectKey key = new SubjectKey(999, "Bla");
        Subject s2 = new Subject();
        s2.setKey(key);

        List<Subject> subjectList = new ArrayList<>();
        subjectList.add(s);
        subjectList.add(s2);

        TypedQuery<Topic> query = em.createQuery(
                "select distinct t from Topic t where t.subject in :subjectList", Topic.class);
        query.setParameter("subjectList", subjectList);
        Topic t = query.getSingleResult();

        verifyResults(t, s);
    }

    /*
     * Prior to the fix #1 (see notes above), this fails on OJ with:
     *
     * Caused by: java.lang.ClassCastException: org.apache.openjpa.persistence.embed.compositepk.SubjectKey
     * cannot be cast to [Ljava.lang.Object;
     *   at org.apache.openjpa.jdbc.kernel.exps.Lit.appendTo(Lit.java:120)
     *
     * Notice the exception this time is in 'Lit'.  Previous CCEx for the other tests have been in Param.
     *
     * With fix #1, the CCEx is avoided/resolved. However, we then got an incorrectly generated SQL as follows:
     *
     * SELECT t0.ID, t1.SUBJECTNUMMER, t1.CODE_SUBJECTTYPE FROM TOPIC t0 LEFT OUTER JOIN SUBJECT t1 ON
     * t0.SUBJECT_SUBJECTNUMMER =
     * t1.SUBJECTNUMMER AND t0.SUBJECT_CODE_SUBJECTTYPE = t1.CODE_SUBJECTTYPE WHERE (t0.SUBJECT_SUBJECTNUMMER = ??)
     * optimize for 1 row [params=(int) 1, (String) Type]
     *
     * Notice that 't0.CODE_SUBJECTTYPE' is missing, and there are two parameter markers.  With fix #2.1 and
     * #2.2, this issue is resolved.
     */
    public void testFindUsingCriteriaBuilderInClauseOnSubject() {

        Subject s = createSubject();
        SubjectKey key = new SubjectKey(999, "Bla");
        Subject s2 = new Subject();
        s2.setKey(key);

        List<Subject> subjectList = new ArrayList<>();
        subjectList.add(s);
        subjectList.add(s2);

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Topic> cq = builder.createQuery(Topic.class);

        Root<Topic> topic = cq.from(Topic.class);
        cq.select(topic).distinct(true);

        Predicate subjectInSubjectList = topic.get(Topic_.subject).in(subjectList);
        cq.where(subjectInSubjectList);

        TypedQuery<Topic> query = em.createQuery(cq);
        Topic t = query.getSingleResult();

        verifyResults(t, s);
    }

    /*
     * This test works fine with or without the fixes.  This was added as a comparison to the case
     * where an @EmbeddedId is used.  In other words, this query selects a Subject which uses
     * a @IdClass (still considered an embeddable in OpenJPA).
     */
    public void testFindUsingJPQLEqualsOnSubjectWithIdClass() {
        SubjectWithIdClass s = new SubjectWithIdClass();
        s.setSubjectNummer(1);
        s.setSubjectTypeCode("Type");

            TypedQuery<SubjectWithIdClass> query =
                em.createQuery("select s from SubjectWithIdClass s where s = :subject", SubjectWithIdClass.class);

            query.setParameter("subject", s);
            SubjectWithIdClass s2 = query.getSingleResult();


        Assert.assertNotNull(s2);
        Assert.assertEquals(s.getSubjectNummer(), s2.getSubjectNummer());
        Assert.assertEquals(s.getSubjectTypeCode(), s2.getSubjectTypeCode());
    }

    /*
     * For this test, the CCEx is actually never hit with or without the fixes.  However, incorrect
     * SQL was generated as follows:
     *
     * SELECT t0.SUBJECTNUMMER, t0.CODE_SUBJECTTYPE FROM SUBJECT2 t0 WHERE
     * (t0.SUBJECTNUMMER = ??)  optimize for 1 row [params=(int) 1, (String) Type]}
     *
     * Notice that 't0.CODE_SUBJECTTYPE' is missing, and there is an extra parameter marker.  With
     * fix #2.1 and #2.2 this issue is resolved.
     */
    public void testFindUsingCriteriaBuilderOnSubjectWithIdClass() {
        SubjectWithIdClass s = new SubjectWithIdClass();
        s.setSubjectNummer(1);
        s.setSubjectTypeCode("Type");

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<SubjectWithIdClass> cq = builder.createQuery(SubjectWithIdClass.class);

        Root<SubjectWithIdClass> subjectRoot = cq.from(SubjectWithIdClass.class);
        cq.select(subjectRoot);

        Predicate subjectPredicate = builder.equal(subjectRoot, s);

        cq.where(subjectPredicate);

        TypedQuery<SubjectWithIdClass> query = em.createQuery(cq);

        SubjectWithIdClass s2 = query.getSingleResult();

        Assert.assertNotNull(s2);
        Assert.assertEquals(s.getSubjectNummer(), s2.getSubjectNummer());
        Assert.assertEquals(s.getSubjectTypeCode(), s2.getSubjectTypeCode());
    }


    private void createData(){
        Subject s = new Subject();
        SubjectKey sk = new SubjectKey();
        sk.setSubjectNummer(1);
        sk.setSubjectType("Type2");
        s.setKey(sk);
        em.persist(s);

        s = new Subject();
        sk = new SubjectKey();
        sk.setSubjectNummer(1);
        sk.setSubjectType("Type");
        s.setKey(sk);
        em.persist(s);

        Topic t = new Topic();
        t.setId(5);
        t.setSubject(s);
        em.persist(t);

        SubjectWithIdClass swic = new SubjectWithIdClass();
        swic.setSubjectNummer(1);
        swic.setSubjectTypeCode("Type");
        em.persist(swic);

        swic = new SubjectWithIdClass();
        swic.setSubjectNummer(1);
        swic.setSubjectTypeCode("Type2");
        em.persist(swic);

        em.flush();
    }

    private Subject createSubject() {
        SubjectKey key = new SubjectKey(1, "Type");
        Subject result = new Subject();
        result.setKey(key);

        return result;
    }

    public void verifyResults(Topic topic, Subject s) {
        Assert.assertNotNull(topic);
        Assert.assertEquals(new Integer(5), topic.getId());
        Subject s2 = topic.getSubject();
        verifySubject(s, s2);
    }

    public void verifySubject(Subject expected, Subject actual) {
        Assert.assertNotNull(expected);
        Assert.assertEquals(expected.getKey().getSubjectNummer(), actual.getKey().getSubjectNummer());
        Assert.assertEquals(expected.getKey().getSubjectTypeCode(), actual.getKey().getSubjectTypeCode());
    }

    @Override
    public void tearDown() {
        if (tx != null && tx.isActive()) {
            tx.rollback();
            tx = null;
        }

        if (em != null && em.isOpen()) {
            em.close();
            em = null;
        }
    }
}
