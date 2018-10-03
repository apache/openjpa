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
package org.apache.openjpa.kernel;

/**
 * Allows facades to control the particulars of persistence operations
 * through callbacks.
 *
 * @author Abe White
 */
public interface OpCallbacks {

    int OP_PERSIST = 0;
    int OP_DELETE = 1;
    int OP_REFRESH = 2;
    int OP_RETRIEVE = 3;
    int OP_RELEASE = 4;
    int OP_EVICT = 5;
    int OP_ATTACH = 6;
    int OP_DETACH = 7;
    int OP_NONTRANSACTIONAL = 8;
    int OP_TRANSACTIONAL = 9;
    int OP_LOCK = 10;

    int ACT_NONE = 0;
    int ACT_CASCADE = 2 << 0;
    int ACT_RUN = 2 << 1;

    /**
     * Process operation argument. Throw proper
     * {@link org.apache.openjpa.util.OpenJPAException} for illegal value.
     *
     * @param op the operation constant
     * @param arg the object passed to the operation
     * @param sm the argument's state manager, or null if none
     * @return the action to take on the argument
     */
    int processArgument(int op, Object arg, OpenJPAStateManager sm);
}
