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
import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.LRSSizes;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;
import org.apache.openjpa.kernel.DelegatingFetchConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.persistence.FetchPlan;
import org.apache.openjpa.persistence.PersistenceExceptions;

/**
 * JDBC extensions to the fetch plan.
 *
 * @since 0.4.1
 * @author Abe White
 * @author Pinaki Poddar
 * @published
 */
public interface JDBCFetchPlan
    extends FetchPlan, EagerFetchModes, LRSSizes, JoinSyntaxes {

    public int getEagerFetchMode();

    public JDBCFetchPlan setEagerFetchMode(int mode);

    public int getSubclassFetchMode();

    public JDBCFetchPlan setSubclassFetchMode(int mode);

    public int getResultSetType();

    public JDBCFetchPlan setResultSetType(int type);

    public int getFetchDirection();

    public JDBCFetchPlan setFetchDirection(int direction);

    public int getLRSSize();

    public JDBCFetchPlan setLRSSize(int lrsSize);

    public int getJoinSyntax();

    public JDBCFetchPlan setJoinSyntax(int syntax);
}
