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

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Upper-cases a string.
 *
 * @author Abe White
 */
class ToUpperCase
    extends StringFunction {

    /**
     * Constructor. Provide the string to operate on.
     */
    public ToUpperCase(Val val) {
        super(val);
    }

    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer buf, int index) {

        DBDictionary dict = ctx.store.getDBDictionary();
        String func = dict.toUpperCaseFunction;
        dict.assertSupport(func != null, "ToUpperCaseFunction");

        int idx = func.indexOf("{0}");
        buf.append(func.substring(0, idx));
        getValue().appendTo(sel, ctx, state, buf, index);
        buf.append(func.substring(idx + 3));
    }
}

