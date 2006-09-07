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

import org.apache.openjpa.jdbc.sql.Select;

/**
 * Distinct the specified path.
 *
 * @author Marc Prud'hommeaux
 */
class Distinct
    extends UnaryOp {

    public Distinct(Val val) {
        super(val);
    }

    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        // join into related object if present
        return initializeValue(sel, ctx, JOIN_REL);
    }

    protected String getOperator() {
        return "DISTINCT";
    }
}
