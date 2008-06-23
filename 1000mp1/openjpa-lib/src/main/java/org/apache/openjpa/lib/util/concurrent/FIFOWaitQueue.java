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
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */
package org.apache.openjpa.lib.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple linked list queue used in FIFOSemaphore.
 * Methods are not synchronized; they depend on synch of callers.
 * Must be public, since it is used by Semaphore(outside this package).
 * NOTE: this class is NOT present in java.util.concurrent.
 */
class FIFOWaitQueue extends WaitQueue implements java.io.Serializable {

    protected transient WaitNode head_ = null;
    protected transient WaitNode tail_ = null;

    public FIFOWaitQueue() {
    }

    public void insert(WaitNode w) {
        if (tail_ == null)
            head_ = tail_ = w;
        else {
            tail_.next = w;
            tail_ = w;
        }
    }

    public WaitNode extract() {
        if (head_ == null)
            return null;
        else {
            WaitNode w = head_;
            head_ = w.next;
            if (head_ == null)
                tail_ = null;
            w.next = null;
            return w;
        }
    }

    public boolean hasNodes() {
        return head_ != null;
    }

    public int getLength() {
        int count = 0;
        WaitNode node = head_;
        while (node != null) {
            if (node.waiting) count++;
            node = node.next;
        }
        return count;
    }

    public Collection getWaitingThreads() {
        List list = new ArrayList();
        WaitNode node = head_;
        while (node != null) {
            if (node.waiting) list.add(node.owner);
            node = node.next;
        }
        return list;
    }

    public boolean isWaiting(Thread thread) {
        if (thread == null) throw new NullPointerException();
        for (WaitNode node = head_; node != null; node = node.next) {
            if (node.waiting && node.owner == thread) return true;
        }
        return false;
    }
}
