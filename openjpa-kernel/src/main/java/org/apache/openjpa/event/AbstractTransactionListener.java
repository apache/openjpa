/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.event;

/**
 * <p>Abstract implementation of the {@link TransactionListener} interface
 * that provides no-op implementations of all methods.</p>
 *
 * @author Abe White
 * @since 3.0
 */
public abstract class AbstractTransactionListener
    implements TransactionListener {

    /**
     * Catch-all for unhandled events.  This method is called by all other
     * event methods if you do not override them.  Does nothing by default.
     */
    protected void eventOccurred(TransactionEvent event) {
    }

    public void afterBegin(TransactionEvent event) {
        eventOccurred(event);
    }

    public void beforeFlush(TransactionEvent event) {
        eventOccurred(event);
    }

    public void afterFlush(TransactionEvent event) {
        eventOccurred(event);
    }

    public void beforeCommit(TransactionEvent event) {
        eventOccurred(event);
    }

    public void afterCommit(TransactionEvent event) {
        eventOccurred(event);
    }

    public void afterRollback(TransactionEvent event) {
        eventOccurred(event);
    }

    public void afterStateTransitions(TransactionEvent event) {
        eventOccurred(event);
    }

    public void afterCommitComplete(TransactionEvent event) {
        eventOccurred(event);
    }

    public void afterRollbackComplete(TransactionEvent event) {
        eventOccurred(event);
    }
}
