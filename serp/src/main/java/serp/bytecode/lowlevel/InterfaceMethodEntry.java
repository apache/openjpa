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
package serp.bytecode.lowlevel;

import serp.bytecode.visitor.*;

import java.io.*;


/**
 *  <p>A reference to an interface method.</p>
 *
 *  @author Abe White
 */
public class InterfaceMethodEntry extends ComplexEntry {
    /**
     *  Default constructor.
     */
    public InterfaceMethodEntry() {
    }

    /**
     *  Constructor.
      *
     *  @see ComplexEntry#ComplexEntry(int,int)
     */
    public InterfaceMethodEntry(int classIndex, int nameAndTypeIndex) {
        super(classIndex, nameAndTypeIndex);
    }

    public int getType() {
        return Entry.INTERFACEMETHOD;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterInterfaceMethodEntry(this);
        visit.exitInterfaceMethodEntry(this);
    }
}
