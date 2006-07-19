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
package org.apache.openjpa.jdbc.sql;

/**
 * Constants for ways of describing joins in SQL.
 *
 * @author Abe White
 */
public interface JoinSyntaxes {

    /**
     * ANSI SQL 92 join syntax; outer joins are supported.
     */
    public static final int SYNTAX_SQL92 = 0;

    /**
     * Traditional join syntax; outer joins are not supported.
     */
    public static final int SYNTAX_TRADITIONAL = 1;

    /**
     * Native database join syntax; outer joins are supported.
     */
    public static final int SYNTAX_DATABASE = 2;
}
