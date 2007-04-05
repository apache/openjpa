/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc;

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
    protected DelegatingFetchConfiguration newDelegatingFetchConfiguration
        (FetchConfiguration fetch) {
        _fetch = new DelegatingJDBCFetchConfiguration((JDBCFetchConfiguration)
            fetch, PersistenceExceptions.TRANSLATOR);
        return _fetch;
    }

    public int getEagerFetchMode() {
        return _fetch.getEagerFetchMode();
    }

    public JDBCFetchPlanImpl setEagerFetchMode(int mode) {
        _fetch.setEagerFetchMode(mode);
        return this;
    }

    public int getSubclassFetchMode() {
        return _fetch.getSubclassFetchMode();
    }

    public JDBCFetchPlanImpl setSubclassFetchMode(int mode) {
        _fetch.setSubclassFetchMode(mode);
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

    public int getLRSSize() {
        return _fetch.getLRSSize();
    }

    public JDBCFetchPlanImpl setLRSSize(int lrsSize) {
        _fetch.setLRSSize(lrsSize);
        return this;
    }

    public int getJoinSyntax() {
        return _fetch.getJoinSyntax();
    }

    public JDBCFetchPlanImpl setJoinSyntax(int syntax) {
        _fetch.setJoinSyntax(syntax);
        return this;
    }

    public int getIsolationLevel() {
        return _fetch.getIsolationLevel();
    }

    public JDBCFetchPlan setIsolationLevel(int level) {
        _fetch.setIsolationLevel(level);
        return this;
    }
}
