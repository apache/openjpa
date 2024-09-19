/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.enhance;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.xbean.asm9.tree.ClassNode;

import junit.framework.TestCase;

public class TestPCEnhancerFindField
    extends TestCase {

    private String field;

    public String myMethod() {
        if (Math.abs(1) == 1)
            return field;
        else
            return field;
    }

    public void testPCEnhancerFindField() throws Exception {
        ClassNode classNode = AsmHelper.readClassNode(this.getClass().getClassLoader(), TestPCEnhancerFindField.class.getName());

        Method meth = TestPCEnhancerFindField.class.getMethod("myMethod");

        Field field = PCEnhancer.getReturnedField(classNode, meth);
        assertEquals("field", field.getName());
    }
}
