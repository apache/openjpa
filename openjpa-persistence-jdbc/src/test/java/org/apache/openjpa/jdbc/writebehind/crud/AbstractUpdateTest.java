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

public abstract class AbstractUpdateTest extends AbstractCrudTest {
    // Commenting out the timed tests to keep build time down. 
    // public void testTimedUpdatesAfterCommit() throws InterruptedException {
    // cleanup();
    // populate();
    // assertEntitiesUnmodified();
    // updateEntities(false);
    // assertEntitiesUnmodified();
    // Thread.sleep(SLEEP_TIME);
    // assertEntitiesUpdated();
    // }
    //
    // public void testTimedUpdatesAfterCommitWithFlush()
    // throws InterruptedException {
    // cleanup();
    // populate();
    // assertEntitiesUnmodified();
    // updateEntities(true);
    // assertEntitiesUnmodified();
    // Thread.sleep(SLEEP_TIME);
    // assertEntitiesUpdated();
    // }
    
    public void testTriggeredUpdatesAfterCommit() throws InterruptedException {
        cleanup();
        populate();
        assertEntitiesUnmodified();
        updateEntities(false);
        assertEntitiesUnmodified();
        getWBCallback().flush();
        assertEntitiesUpdated();
    }

    public void testTriggeredUpdatesAfterCommitWithFlush()
        throws InterruptedException {
        cleanup();
        populate();
        assertEntitiesUnmodified();
        updateEntities(true);
        assertEntitiesUnmodified();
        getWBCallback().flush();
        assertEntitiesUpdated();
    }
}
