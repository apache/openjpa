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
package org.apache.openjpa.persistence;

import java.util.EnumSet;

import junit.framework.TestCase;
import org.apache.openjpa.kernel.ConnectionRetainModes;
import org.apache.openjpa.kernel.DetachState;
import org.apache.openjpa.kernel.RestoreState;
import org.apache.openjpa.kernel.AutoClear;
import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.event.CallbackModes;

public class TestEnumToKernelConstantMappings
    extends TestCase {

    public void testConnectionRetainModes() {
        assertEquals(ConnectionRetainModes.CONN_RETAIN_ALWAYS,
            ConnectionRetainType.ALWAYS.toKernelConstant());
        assertEquals(ConnectionRetainType.ALWAYS,
            ConnectionRetainType.fromKernelConstant(
                ConnectionRetainModes.CONN_RETAIN_ALWAYS));
        assertEquals(ConnectionRetainType.ALWAYS.toKernelConstant(),
            ConnectionRetainType.ALWAYS.ordinal());

        assertEquals(ConnectionRetainModes.CONN_RETAIN_DEMAND,
            ConnectionRetainType.ON_DEMAND.toKernelConstant());
        assertEquals(ConnectionRetainType.ON_DEMAND,
            ConnectionRetainType.fromKernelConstant(
                ConnectionRetainModes.CONN_RETAIN_DEMAND));
        assertEquals(ConnectionRetainType.ON_DEMAND.toKernelConstant(),
            ConnectionRetainType.ON_DEMAND.ordinal());

        assertEquals(ConnectionRetainModes.CONN_RETAIN_TRANS,
            ConnectionRetainType.TRANSACTION.toKernelConstant());
        assertEquals(ConnectionRetainType.TRANSACTION,
            ConnectionRetainType.fromKernelConstant(
                ConnectionRetainModes.CONN_RETAIN_TRANS));
        assertEquals(ConnectionRetainType.TRANSACTION.toKernelConstant(),
            ConnectionRetainType.TRANSACTION.ordinal());

        assertEquals(getConstantCount(ConnectionRetainModes.class),
            ConnectionRetainType.values().length);
    }

    public void testDetachState() {
        assertEquals(DetachState.DETACH_ALL,
            DetachStateType.ALL.toKernelConstant());
        assertEquals(DetachStateType.ALL,
            DetachStateType.fromKernelConstant(DetachState.DETACH_ALL));
        assertEquals(DetachStateType.ALL.toKernelConstant(),
            DetachStateType.ALL.ordinal());

        assertEquals(DetachState.DETACH_FETCH_GROUPS,
            DetachStateType.FETCH_GROUPS.toKernelConstant());
        assertEquals(DetachStateType.FETCH_GROUPS,
            DetachStateType.fromKernelConstant(
                DetachState.DETACH_FETCH_GROUPS));
        assertEquals(DetachStateType.FETCH_GROUPS.toKernelConstant(),
            DetachStateType.FETCH_GROUPS.ordinal());

        assertEquals(DetachState.DETACH_LOADED,
            DetachStateType.LOADED.toKernelConstant());
        assertEquals(DetachStateType.LOADED,
            DetachStateType.fromKernelConstant(DetachState.DETACH_LOADED));
        assertEquals(DetachStateType.LOADED.toKernelConstant(),
            DetachStateType.LOADED.ordinal());

        // subtract 1 for DetachState.DETACH_FGS, which is deprecated
        assertEquals(getConstantCount(DetachState.class) - 1,
            DetachStateType.values().length);
    }

    public void testRestoreState() {
        assertEquals(RestoreState.RESTORE_ALL,
            RestoreStateType.ALL.toKernelConstant());
        assertEquals(RestoreStateType.ALL,
            RestoreStateType.fromKernelConstant(RestoreState.RESTORE_ALL));
        assertEquals(RestoreStateType.ALL.toKernelConstant(),
            RestoreStateType.ALL.ordinal());

        assertEquals(RestoreState.RESTORE_IMMUTABLE,
            RestoreStateType.IMMUTABLE.toKernelConstant());
        assertEquals(RestoreStateType.IMMUTABLE,
            RestoreStateType.fromKernelConstant(
                RestoreState.RESTORE_IMMUTABLE));
        assertEquals(RestoreStateType.IMMUTABLE.toKernelConstant(),
            RestoreStateType.IMMUTABLE.ordinal());

        assertEquals(RestoreState.RESTORE_NONE,
            RestoreStateType.NONE.toKernelConstant());
        assertEquals(RestoreStateType.NONE,
            RestoreStateType.fromKernelConstant(RestoreState.RESTORE_NONE));
        assertEquals(RestoreStateType.NONE.toKernelConstant(),
            RestoreStateType.NONE.ordinal());

        assertEquals(getConstantCount(RestoreState.class),
            RestoreStateType.values().length);
    }

    public void testAutoClear() {
        assertEquals(AutoClear.CLEAR_ALL, AutoClearType.ALL.toKernelConstant());
        assertEquals(AutoClearType.ALL,
            AutoClearType.fromKernelConstant(AutoClear.CLEAR_ALL));
        assertEquals(AutoClearType.ALL.toKernelConstant(),
            AutoClearType.ALL.ordinal());

        assertEquals(AutoClear.CLEAR_DATASTORE,
            AutoClearType.DATASTORE.toKernelConstant());
        assertEquals(AutoClearType.DATASTORE,
            AutoClearType.fromKernelConstant(AutoClear.CLEAR_DATASTORE));
        assertEquals(AutoClearType.DATASTORE.toKernelConstant(),
            AutoClearType.DATASTORE.ordinal());

        assertEquals(getConstantCount(AutoClear.class),
            AutoClearType.values().length);
    }

    public void testAutoDetach() {
        assertEquals(getConstantCount(AutoDetach.class),
            AutoDetachType.values().length);

        assertEquals(EnumSet.of(AutoDetachType.CLOSE),
            AutoDetachType.toEnumSet(AutoDetach.DETACH_CLOSE));
        assertEquals(AutoDetach.DETACH_CLOSE,
            AutoDetachType.fromEnumSet(EnumSet.of(AutoDetachType.CLOSE)));

        assertEquals(EnumSet.of(AutoDetachType.COMMIT),
            AutoDetachType.toEnumSet(AutoDetach.DETACH_COMMIT));
        assertEquals(AutoDetach.DETACH_COMMIT,
            AutoDetachType.fromEnumSet(EnumSet.of(AutoDetachType.COMMIT)));

        assertEquals(EnumSet.of(AutoDetachType.NON_TRANSACTIONAL_READ),
            AutoDetachType.toEnumSet(AutoDetach.DETACH_NONTXREAD));
        assertEquals(AutoDetach.DETACH_NONTXREAD,
            AutoDetachType.fromEnumSet(
                EnumSet.of(AutoDetachType.NON_TRANSACTIONAL_READ)));

        assertEquals(EnumSet.of(AutoDetachType.ROLLBACK),
            AutoDetachType.toEnumSet(AutoDetach.DETACH_ROLLBACK));
        assertEquals(AutoDetach.DETACH_ROLLBACK,
            AutoDetachType.fromEnumSet(EnumSet.of(AutoDetachType.ROLLBACK)));


        assertEquals(EnumSet.of(AutoDetachType.CLOSE, AutoDetachType.COMMIT),
            AutoDetachType.toEnumSet(
                AutoDetach.DETACH_CLOSE | AutoDetach.DETACH_COMMIT));
        assertEquals(AutoDetach.DETACH_ROLLBACK | AutoDetach.DETACH_CLOSE,
            AutoDetachType.fromEnumSet(
                EnumSet.of(AutoDetachType.ROLLBACK, AutoDetachType.CLOSE)));


        assertEquals(EnumSet.allOf(AutoDetachType.class),
            AutoDetachType.toEnumSet(
                AutoDetach.DETACH_CLOSE
                    | AutoDetach.DETACH_COMMIT
                    | AutoDetach.DETACH_NONTXREAD
                    | AutoDetach.DETACH_ROLLBACK));
        assertEquals(AutoDetach.DETACH_CLOSE
                    | AutoDetach.DETACH_COMMIT
                    | AutoDetach.DETACH_NONTXREAD
                    | AutoDetach.DETACH_ROLLBACK,
            AutoDetachType.fromEnumSet(EnumSet.allOf(AutoDetachType.class)));
    }

    public void testCallbackMode() {
        assertEquals(getConstantCount(CallbackModes.class),
            CallbackType.values().length);

        assertEquals(EnumSet.of(CallbackType.FAIL_FAST),
            CallbackType.toEnumSet(CallbackModes.CALLBACK_FAIL_FAST));
        assertEquals(CallbackModes.CALLBACK_FAIL_FAST,
            CallbackType.fromEnumSet(EnumSet.of(CallbackType.FAIL_FAST)));

        assertEquals(EnumSet.of(CallbackType.IGNORE),
            CallbackType.toEnumSet(CallbackModes.CALLBACK_IGNORE));
        assertEquals(CallbackModes.CALLBACK_IGNORE,
            CallbackType.fromEnumSet(EnumSet.of(CallbackType.IGNORE)));

        assertEquals(EnumSet.of(CallbackType.LOG),
            CallbackType.toEnumSet(CallbackModes.CALLBACK_LOG));
        assertEquals(CallbackModes.CALLBACK_LOG,
            CallbackType.fromEnumSet(EnumSet.of(CallbackType.LOG)));

        assertEquals(EnumSet.of(CallbackType.RETHROW),
            CallbackType.toEnumSet(CallbackModes.CALLBACK_RETHROW));
        assertEquals(CallbackModes.CALLBACK_RETHROW,
            CallbackType.fromEnumSet(EnumSet.of(CallbackType.RETHROW)));

        assertEquals(EnumSet.of(CallbackType.ROLLBACK),
            CallbackType.toEnumSet(CallbackModes.CALLBACK_ROLLBACK));
        assertEquals(CallbackModes.CALLBACK_ROLLBACK,
            CallbackType.fromEnumSet(EnumSet.of(CallbackType.ROLLBACK)));


        assertEquals(EnumSet.of(CallbackType.ROLLBACK, CallbackType.IGNORE),
            CallbackType.toEnumSet(CallbackModes.CALLBACK_ROLLBACK
                | CallbackModes.CALLBACK_IGNORE));
        assertEquals(
            CallbackModes.CALLBACK_ROLLBACK | CallbackModes.CALLBACK_IGNORE,
            CallbackType.fromEnumSet(
                EnumSet.of(CallbackType.ROLLBACK, CallbackType.IGNORE)));


        assertEquals(EnumSet.allOf(CallbackType.class),
            CallbackType.toEnumSet(
                CallbackModes.CALLBACK_FAIL_FAST
                    | CallbackModes.CALLBACK_IGNORE
                    | CallbackModes.CALLBACK_LOG
                    | CallbackModes.CALLBACK_RETHROW
                    | CallbackModes.CALLBACK_ROLLBACK));
        assertEquals(CallbackModes.CALLBACK_FAIL_FAST
                    | CallbackModes.CALLBACK_IGNORE
                    | CallbackModes.CALLBACK_LOG
                    | CallbackModes.CALLBACK_RETHROW
                    | CallbackModes.CALLBACK_ROLLBACK,
            CallbackType.fromEnumSet(EnumSet.allOf(CallbackType.class)));
    }

    public void testQueryOperationTypes() {
        assertEquals(QueryOperations.OP_SELECT,
            QueryOperationType.SELECT.toKernelConstant());
        assertEquals(QueryOperationType.SELECT,
            QueryOperationType.fromKernelConstant(
                QueryOperations.OP_SELECT));
        assertEquals(QueryOperationType.SELECT.toKernelConstant(),
            QueryOperationType.SELECT.ordinal() + 1);

        assertEquals(QueryOperations.OP_UPDATE,
            QueryOperationType.UPDATE.toKernelConstant());
        assertEquals(QueryOperationType.UPDATE,
            QueryOperationType.fromKernelConstant(
                QueryOperations.OP_UPDATE));
        assertEquals(QueryOperationType.UPDATE.toKernelConstant(),
            QueryOperationType.UPDATE.ordinal() + 1);

        assertEquals(QueryOperations.OP_DELETE,
            QueryOperationType.DELETE.toKernelConstant());
        assertEquals(QueryOperationType.DELETE,
            QueryOperationType.fromKernelConstant(
                QueryOperations.OP_DELETE));
        assertEquals(QueryOperationType.DELETE.toKernelConstant(),
            QueryOperationType.DELETE.ordinal() + 1);

        assertEquals(getConstantCount(QueryOperations.class),
            QueryOperationType.values().length);
    }

    private int getConstantCount(Class cls) {
        return cls.getDeclaredFields().length;
    }
}
