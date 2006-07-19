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
package org.apache.openjpa.jdbc.meta.strats;

import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Handler for clob values.
 *
 * @nojavadoc
 */
public class ClobValueHandler
    extends AbstractValueHandler {

    private static final ClobValueHandler _instance = new ClobValueHandler();

    /**
     * Singleton instance.
     */
    public static ClobValueHandler getInstance() {
        return _instance;
    }

    public Column[] map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt) {
        Column col = new Column();
        col.setName(name);
        col.setJavaType(JavaTypes.STRING);
        col.setSize(-1);
        return new Column[]{ col };
    }
}
