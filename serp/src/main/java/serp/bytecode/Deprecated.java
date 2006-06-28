/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

import serp.bytecode.visitor.*;


/**
 *  <p>Attribute signifying that a method or class is deprecated.</p>
 *
 *  @author Abe White
 */
public class Deprecated extends Attribute {
    Deprecated(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterDeprecated(this);
        visit.exitDeprecated(this);
    }
}
