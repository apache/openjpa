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
package org.apache.openjpa.lib.rop;


/**
 * A provider for multiple result sets.
 * This provider acts as a container of other result providers. The underlying  providers
 * contain the actual data and are {@link ResultObjectProvider#getResultObject() iterated}
 * for the results, while this provider iterates over its underlying providers.
 * <br>
 * Designed for the specific purpose of getting results from the execution of Stored Procedures
 * that can produce more than one result set and an optional update count. Few methods related
 * to iterating multiple results and update count mirror the methods in JDBC {@link java.sql.Statement}.
 * <br>
 *
 * @see org.apache.openjpa.kernel.QueryResultCallback
 *
 * @author Pinaki Poddar
 *
 */
public interface BatchedResultObjectProvider extends ResultObjectProvider {
    /**
     * Gets the next result object provider from its batch.
     */
    ResultObjectProvider getResultObject() throws Exception;

    /**
     * Affirms if this batch contains more results.
     */
    boolean hasMoreResults();

    /**
     * Gets the result of executing the underlying JDBC statement.
     * @return a boolean value whose semantics is same as {@link java.sql.PreparedStatement#execute()}.
     */
    boolean getExecutionResult();


    /**
     * Gets the count of  records updated by the underlying JDBC statement.
     * @return an integer value whose semantics is same as {@link java.sql.CallableStatement#getUpdateCount()}.
     */
    int getUpdateCount();

    Object getOut(String name);
    Object getOut(int position);
}
