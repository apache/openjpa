/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the {@link ArrayStoreInstruction} type.
 *
 * @author Abe White
 */
public class TestArrayStoreInstruction extends TestCase {

    private Code _code = new Code();

    public TestArrayStoreInstruction(String test) {
        super(test);
    }

    /**
     * Test that the instruction initializes correctly when generated.
     */
    public void testIniitalize() {
        assertEquals(Constants.NOP, _code.xastore().getOpcode());
        assertEquals(Constants.IASTORE, _code.iastore().getOpcode());
        assertEquals(Constants.LASTORE, _code.lastore().getOpcode());
        assertEquals(Constants.FASTORE, _code.fastore().getOpcode());
        assertEquals(Constants.DASTORE, _code.dastore().getOpcode());
        assertEquals(Constants.AASTORE, _code.aastore().getOpcode());
        assertEquals(Constants.BASTORE, _code.bastore().getOpcode());
        assertEquals(Constants.CASTORE, _code.castore().getOpcode());
        assertEquals(Constants.SASTORE, _code.sastore().getOpcode());
    }

    /**
     * Test the the instruction returns its type correctly.
     */
    public void testGetType() {
        assertNull(_code.xastore().getType());
        assertEquals(int.class, _code.iastore().getType());
        assertEquals(long.class, _code.lastore().getType());
        assertEquals(float.class, _code.fastore().getType());
        assertEquals(double.class, _code.dastore().getType());
        assertEquals(Object.class, _code.aastore().getType());
        assertEquals(byte.class, _code.bastore().getType());
        assertEquals(char.class, _code.castore().getType());
        assertEquals(short.class, _code.sastore().getType());
    }

    /**
     * Test that the opcode morphs correctly with type changes.
     */
    public void testOpcodeMorph() {
        ArrayStoreInstruction ins = _code.xastore();
        assertEquals(Constants.NOP, ins.getOpcode());
        assertEquals(Constants.NOP, ins.setType((String) null).getOpcode());
        assertEquals(Constants.NOP, ins.setType((BCClass) null).getOpcode());
        assertEquals(Constants.NOP, ins.setType((Class) null).getOpcode());

        assertEquals(Constants.IASTORE, ins.setType(int.class).getOpcode());
        assertEquals(Constants.NOP, ins.setType((String) null).getOpcode());
        assertEquals(Constants.LASTORE, ins.setType(long.class).getOpcode());
        assertEquals(Constants.FASTORE, ins.setType(float.class).getOpcode());
        assertEquals(Constants.DASTORE, ins.setType(double.class).getOpcode());
        assertEquals(Constants.AASTORE, ins.setType(Object.class).getOpcode());
        assertEquals(Constants.BASTORE, ins.setType(byte.class).getOpcode());
        assertEquals(Constants.CASTORE, ins.setType(char.class).getOpcode());
        assertEquals(Constants.SASTORE, ins.setType(short.class).getOpcode());
        assertEquals(Constants.IASTORE, ins.setType(void.class).getOpcode());
        assertEquals(Constants.AASTORE, ins.setType(String.class).getOpcode());
        assertEquals(Constants.IASTORE, ins.setType(boolean.class).
            getOpcode());
    }

    public static Test suite() {
        return new TestSuite(TestArrayStoreInstruction.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
