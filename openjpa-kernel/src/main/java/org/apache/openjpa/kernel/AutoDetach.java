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
 * Bit flags for when to automatically detach the entire persistence context.
 */
public interface AutoDetach {

    /**
     * Detach context on close.
     */
    public static final int DETACH_CLOSE = 2 << 0;

    /**
     * Detach context on transaction commit.
     */
    public static final int DETACH_COMMIT = 2 << 1;

    /**
     * Detach context on any nontransctional read, such that each
     * nontransactional operation uses a new persistence context in essence.
     */
    public static final int DETACH_NONTXREAD = 2 << 2;

    /**
     * Detach context on failed transaction commit / rollback.
     */
    public static final int DETACH_ROLLBACK = 2 << 3;
    
    /**
     * Never Detach. Useful for batch loading where user application is not
     * concerned about the referenced objects after the persistent operation.
     */
    public static final int DETACH_NONE = 2 << 4;
    
    /**
     * Available integer values of AutoDetach options.
     */
	public static final int[] values = {DETACH_NONE, DETACH_CLOSE, DETACH_COMMIT, DETACH_NONTXREAD, 
			DETACH_ROLLBACK};
	
    /**
     * Available user-visible names of AutoDetach options.
     * Order corresponds to integer values of the options.  
     */
	public static final String[] names = {"NONE", "CLOSE", "COMMIT", "NON-TX-READ", "ROLLBACK"};

    
}
