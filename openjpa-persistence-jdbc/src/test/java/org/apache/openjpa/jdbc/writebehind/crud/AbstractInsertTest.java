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
package org.apache.openjpa.jdbc.writebehind.crud;

public abstract class AbstractInsertTest extends AbstractCrudTest {

    // Commenting out the timed tests to keep build time down. 
    // public void testTimedInsertsAfterCommit() throws InterruptedException {
    // cleanup();
    // insertEntities(false);
    // assertEntitiesDeleted();
    // Thread.sleep(SLEEP_TIME);
    // assertEntitiesExist();
    // }
    //
    // public void testTimedInsertsAfterCommitWithFlush()
    // throws InterruptedException {
    // cleanup();
    // insertEntities(true);
    // assertEntitiesDeleted();
    // Thread.sleep(SLEEP_TIME);
    // assertEntitiesExist();
    // }

    public void testTriggeredInsertsAfterCommit() throws InterruptedException {
        cleanup();
        insertEntities(false);
        assertEntitiesDeleted();
        getWBCallback().flush();
        assertEntitiesExist();
    }

    public void testTriggeredInsertsAfterCommitWithFlush()
        throws InterruptedException {
        cleanup();
        insertEntities(true);
        assertEntitiesDeleted();
        getWBCallback().flush();
        assertEntitiesExist();
    }

}
