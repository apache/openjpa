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
package org.apache.openjpa.persistence.optlockex.timestamp;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/*
 * Test create for JIRA OPENJPA-2476, see it for a very detailed
 * description of the issue.
 */
public class TestTimestampOptLockEx extends SingleEMFTestCase {

    @Override
    public void setUp() {
        // By default we'd round a Timestamp to the nearest millisecond on Oracle (see DBDictionary.datePrecision
        // and DBDictionary.setTimestamp) and nearest microsecond on DB2 (see DB2Dictionary.datePrecision and
        // DBDictionary.setTimestamp) when sending the value to the db...if we change datePrecision to 1, we round to
        // the nearest nanosecond.  On DB2 and Oracle, it appears the default precision is microseconds but it seems
        // DB2 truncates (no rounding) to microsecond for anything it is given with greater precision, whereas Oracle
        // rounds.  So in the case of DB2, this test will pass if datePrecision=1, but still fails on Oracle.
        // On the other hand, if we set the datePrecision to 1000000 and run against DB2, the test will fail.

        // This test requires datePrecision to be set to the same precision as the Timestamp column.
        // I've only been testing on Oracle and DB2 and not sure how other DBs treat a Timestamps precision
        // by default.  In VersionTSEntity I use a Timestamp(3) but this is not supported on, at least, Derby
        // and older versions of DB2...at this time I'll enable only on Oracle.
        setSupportedDatabases(org.apache.openjpa.jdbc.sql.OracleDictionary.class);
        if (isTestsDisabled()) {
            return;
        }

        // Set datePrecision=1000000 for Oracle since we are using Timestamp(3)....on Oracle
        // the default is 1000000 so we shouldn't need to set it, but lets set it to future
        // proof the test.
        super.setUp(DROP_TABLES, "openjpa.jdbc.DBDictionary", "datePrecision=1000000", VersionTSEntity.class);
    }

    public void testUpdate() {
        poplulate();
        //This loop is necessary since we need a timestamp which has been rounded up
        //by the database, or by OpenJPA such that the in-memory version of the Timestamp
        //varies from that which is in the database.
        for (int i = 0; i < 5000; i++) {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();

            // Find an existing VersionTSEntity:
            // stored with microsecond precision, e.g. 2014-01-21 13:16:46.595428
            VersionTSEntity t = em.find(VersionTSEntity.class, 1);

            tx.begin();
            t.setSomeInt(t.getSomeInt() + 1);
            t = em.merge(t);

            tx.commit();
            // If this clear is removed the test works fine.
            em.clear();

            // Lets say at this point the 'in-memory' timestamp is: 2014-01-22 07:22:11.548778567.  What we
            // actually sent to the DB (via the previous merge) is by default rounded (see DBDictionary.setTimestamp)
            // to the nearest millisecond on Oracle (see DBDictionary.datePrecision) and nearest microsecond on
            // DB2 (see DB2Dictionary.datePrecision) when sending the value to the db.
            // Therefore, what we actually send to the db is: 2014-01-22 07:22:11.548779 (for DB2) or
            // 2014-01-22 07:22:11.549 (for Oracle).  Notice in either case we rounded up.

            // now, do a merge with the unchanged entity
            tx = em.getTransaction();
            tx.begin();

            t = em.merge(t); // this results in a select of VersionTSEntity

            //This 'fixes' the issue (but customer doesn't really want to add this):
            //em.refresh(t);

            // Here is where things get interesting.....an error will happen here when the timestamp
            // has been rounded up, as I'll explain:
            // As part of this merge/commit, we select the timestamp from the db to get its value
            // (see method ColumnVersionStrategy.checkVersion below), i.e:
            // 'SELECT t0.updateTimestamp FROM VersionTSEntity t0 WHERE t0.id = ?'.
            // We then compare the 'in-memory' timestamp to that which we got back from the DB, i.e. on
            // DB2 we compare:
            // in-mem:  2014-01-22 07:22:11.548778567
            // from db: 2014-01-22 07:22:11.548779
            // Because these do not 'compare' properly (the db version is greater), we throw the OptimisticLockEx!!
            // For completeness, lets look at an example where the timestamp is as follows after the above
            // update: 2014-01-22 07:22:11.548771234.  We would send to DB2
            // the following value: 2014-01-22 07:22:11.548771.  Then, as part of the very last merge/commit, we'd
            // compare:
            // in-mem:  2014-01-22 07:22:11.548771234
            // from db: 2014-01-22 07:22:11.548771
            // These two would 'compare' properly (the db version is lesser), as such we would not throw an
            // OptLockEx and the test works fine.
            tx.commit();
            em.close();
        }
    }

    public void poplulate(){
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        VersionTSEntity r = new VersionTSEntity();

        r.setId(1L);
        r.setSomeInt(0);
        em.persist(r);
        tx.commit();
        em.close();
    }
}
