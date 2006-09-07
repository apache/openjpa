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
package org.apache.openjpa.jdbc.kernel.exps;

/**
 * Struct to hold the state of a query expressions instance.
 *
 * @author Abe White
 * @nojavadoc
 */
public class QueryExpressionsState {

    public static final ExpState[] EMPTY_STATES = new ExpState[0];

    public ExpState[] projections = EMPTY_STATES;
    public ExpState filter = null;
    public ExpState[] grouping = EMPTY_STATES;
    public ExpState having = null;
    public ExpState[] ordering = EMPTY_STATES;
}
