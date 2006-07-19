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
package org.apache.openjpa.jdbc.kernel;

import org.apache.openjpa.kernel.FetchState;

/**
 * Store-specific extension of {@link org.apache.openjpa.kernel.FetchState FetchState}.
 *
 * @author <A HREF="mailto:pinaki.poddar@gmail.com>Pinaki Poddar</A>
 */
public interface JDBCFetchState
    extends FetchState {

    /**
     * Returns store-specific fetch configuration.
     */
    public JDBCFetchConfiguration getJDBCFetchConfiguration();
}
