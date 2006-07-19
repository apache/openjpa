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
 * Factory for SQL constructs.
 *
 * @author Abe White
 */
public interface SQLFactory {

    /**
     * Select factory.
     */
    public Select newSelect();

    /**
     * Union factory.
     *
     * @param selects the number of selects in the union
     */
    public Union newUnion(int selects);

    /**
     * Union factory.
     *
     * @param selects the members of the union
     */
    public Union newUnion(Select[] selects);
}
