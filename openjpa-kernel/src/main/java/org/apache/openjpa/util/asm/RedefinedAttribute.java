/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util.asm;

import org.apache.xbean.asm9.Attribute;
import org.apache.xbean.asm9.ByteVector;
import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.Label;

/**
 * Custom Attribute to mark that this class already got redefined.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class RedefinedAttribute extends Attribute {
    public static final String ATTR_TYPE = "org/apache/openjpa/Redefined";
    public RedefinedAttribute() {
        super(ATTR_TYPE);
    }

    @Override
    protected Attribute read(ClassReader classReader, int offset, int length, char[] charBuffer, int codeAttributeOffset, Label[] labels) {
        return new RedefinedAttribute();
    }

    @Override
    protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
        int idx = classWriter.newUTF8("Redefined");
        return new ByteVector().putShort(idx);
    }
}
