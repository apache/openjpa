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
package org.apache.openjpa.persistence.jdbc;

import java.util.Collection;
import javax.persistence.LockModeType;

import org.apache.openjpa.jdbc.kernel.DelegatingJDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.kernel.DelegatingFetchConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.persistence.FetchPlanImpl;
import org.apache.openjpa.persistence.PersistenceExceptions;

/**
 * JDBC extensions to the fetch plan.
 *
 * @since 0.4.0
 * @author Abe White
 */
public class JDBCFetchPlanImpl
    extends FetchPlanImpl
    implements JDBCFetchPlan {

    private DelegatingJDBCFetchConfiguration _fetch;

    /**
     * Constructor; supply delegate.
     */
    public JDBCFetchPlanImpl(FetchConfiguration fetch) {
        super(fetch);
    }

    @Override
    protected DelegatingFetchConfiguration newDelegatingFetchConfiguration(
        FetchConfiguration fetch) {
        _fetch = new DelegatingJDBCFetchConfiguration((JDBCFetchConfiguration)
            fetch, PersistenceExceptions.TRANSLATOR);
        return _fetch;
    }

    public EagerFetchType getEagerFetchMode() {
        return EagerFetchType.fromKernelConstant(_fetch.getEagerFetchMode());
    }

    public JDBCFetchPlanImpl setEagerFetchMode(EagerFetchType type) {
        _fetch.setEagerFetchMode(type.toKernelConstant());
        return this;
    }

    public EagerFetchType getSubclassFetchMode() {
        return EagerFetchType.fromKernelConstant(_fetch.getSubclassFetchMode());
    }

    public JDBCFetchPlanImpl setSubclassFetchMode(EagerFetchType type) {
        _fetch.setSubclassFetchMode(type.toKernelConstant());
        return this;
    }

    public int getResultSetType() {
        return _fetch.getResultSetType();
    }

    public JDBCFetchPlanImpl setResultSetType(int type) {
        _fetch.setResultSetType(type);
        return this;
    }

    public int getFetchDirection() {
        return _fetch.getFetchDirection();
    }

    public JDBCFetchPlanImpl setFetchDirection(int direction) {
        _fetch.setFetchDirection(direction);
        return this;
    }

    public LRSSizeType getLRSSize() {
        return LRSSizeType.fromKernelConstant(_fetch.getLRSSize());
    }

    public JDBCFetchPlanImpl setLRSSize(LRSSizeType lrsSize) {
        _fetch.setLRSSize(lrsSize.toKernelConstant());
        return this;
    }

    public JoinSyntaxType getJoinSyntax() {
        return JoinSyntaxType.fromKernelConstant(_fetch.getJoinSyntax());
    }

    public JDBCFetchPlanImpl setJoinSyntax(JoinSyntaxType syntax) {
        _fetch.setJoinSyntax(syntax.toKernelConstant());
        return this;
    }

    public IsolationLevel getIsolation() {
        return IsolationLevel.fromConnectionConstant(_fetch.getIsolation());
    }

    public JDBCFetchPlan setIsolation(IsolationLevel level) {
        _fetch.setIsolation(level.getConnectionConstant());
        return this;
    }

    @Override
    public JDBCFetchPlan addFetchGroup(String group) {
        return (JDBCFetchPlan) super.addFetchGroup(group);
    }

    @Override
    public JDBCFetchPlan addFetchGroups(Collection groups) {
        return (JDBCFetchPlan) super.addFetchGroups(groups);
    }

    @Override
    public JDBCFetchPlan addFetchGroups(String... groups) {
        return (JDBCFetchPlan) super.addFetchGroups(groups);
    }

    @Override
    public JDBCFetchPlan addField(Class cls, String field) {
        return (JDBCFetchPlan) super.addField(cls, field);
    }

    @Override
    public JDBCFetchPlan addField(String field) {
        return (JDBCFetchPlan) super.addField(field);
    }

    @Override
    public JDBCFetchPlan addFields(Class cls, Collection fields) {
        return (JDBCFetchPlan) super.addFields(cls, fields);
    }

    @Override
    public JDBCFetchPlan addFields(Class cls, String... fields) {
        return (JDBCFetchPlan) super.addFields(cls, fields);
    }

    @Override
    public JDBCFetchPlan addFields(Collection fields) {
        return (JDBCFetchPlan) super.addFields(fields);
    }

    @Override
    public JDBCFetchPlan addFields(String... fields) {
        return (JDBCFetchPlan) super.addFields(fields);
    }

    @Override
    public JDBCFetchPlan clearFetchGroups() {
        return (JDBCFetchPlan) super.clearFetchGroups();
    }

    @Override
    public JDBCFetchPlan clearFields() {
        return (JDBCFetchPlan) super.clearFields();
    }

    @Override
    public JDBCFetchPlan removeFetchGroup(String group) {
        return (JDBCFetchPlan) super.removeFetchGroup(group);
    }

    @Override
    public JDBCFetchPlan removeFetchGroups(Collection groups) {
        return (JDBCFetchPlan) super.removeFetchGroups(groups);
    }

    @Override
    public JDBCFetchPlan removeFetchGroups(String... groups) {
        return (JDBCFetchPlan) super.removeFetchGroups(groups);
    }

    @Override
    public JDBCFetchPlan removeField(Class cls, String field) {
        return (JDBCFetchPlan) super.removeField(cls, field);
    }

    @Override
    public JDBCFetchPlan removeField(String field) {
        return (JDBCFetchPlan) super.removeField(field);
    }

    @Override
    public JDBCFetchPlan removeFields(Class cls, Collection fields) {
        return (JDBCFetchPlan) super.removeFields(cls, fields);
    }

    @Override
    public JDBCFetchPlan removeFields(Class cls, String... fields) {
        return (JDBCFetchPlan) super.removeFields(cls, fields);
    }

    @Override
    public JDBCFetchPlan removeFields(Collection fields) {
        return (JDBCFetchPlan) super.removeFields(fields);
    }

    @Override
    public JDBCFetchPlan removeFields(String... fields) {
        return (JDBCFetchPlan) super.removeFields(fields);
    }

    @Override
    public JDBCFetchPlan resetFetchGroups() {
        return (JDBCFetchPlan) super.resetFetchGroups();
    }

    @Override
    public JDBCFetchPlan setEnlistInQueryResultCache(boolean cache) {
        return (JDBCFetchPlan) super.setEnlistInQueryResultCache(cache);
    }

    @Override
    public JDBCFetchPlan setFetchBatchSize(int fetchBatchSize) {
        return (JDBCFetchPlan) super.setFetchBatchSize(fetchBatchSize);
    }

    @Override
    public JDBCFetchPlan setLockTimeout(int timeout) {
        return (JDBCFetchPlan) super.setLockTimeout(timeout);
    }

    @Override
    public JDBCFetchPlan setMaxFetchDepth(int depth) {
        return (JDBCFetchPlan) super.setMaxFetchDepth(depth);
    }

    @Override
    public JDBCFetchPlan setReadLockMode(LockModeType mode) {
        return (JDBCFetchPlan) super.setReadLockMode(mode);
    }

    @Override
    public JDBCFetchPlan setWriteLockMode(LockModeType mode) {
        return (JDBCFetchPlan) super.setWriteLockMode(mode);
    }
}
