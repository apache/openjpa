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

import junit.framework.*;
import junit.textui.*;

/**
 * Tests the {@link ConvertInstruction} type.
 * 
 * @author Abe White
 */
public class TestConvertInstruction extends TestCase {
    private Code _code = new Code();

    public TestConvertInstruction(String test) {
        super(test);
    }

    /**
     * Test that the opcode is morphed correctly when the types are set.
     */
    public void testOpcodeMorph() {
        ConvertInstruction ins = _code.convert();
        assertEquals(Constants.NOP, ins.getOpcode());

        ins.setFromType(int.class);
        assertEquals(Constants.NOP, ins.getOpcode());
        assertEquals(int.class, ins.getFromType());
        assertNull(ins.getType());

        ins.setType(int.class);
        assertEquals(Constants.NOP, ins.getOpcode());
        assertEquals(int.class, ins.getFromType());
        assertEquals(int.class, ins.getType());

        ins.setType(long.class);
        assertEquals(Constants.I2L, ins.getOpcode());
        assertEquals(int.class, ins.getFromType());
        assertEquals(long.class, ins.getType());

        ins.setType(float.class);
        assertEquals(Constants.I2F, ins.getOpcode());
        assertEquals(int.class, ins.getFromType());
        assertEquals(float.class, ins.getType());

        ins.setType(double.class);
        assertEquals(Constants.I2D, ins.getOpcode());
        assertEquals(int.class, ins.getFromType());
        assertEquals(double.class, ins.getType());

        ins.setFromType(long.class);
        assertEquals(Constants.L2D, ins.getOpcode());
        assertEquals(long.class, ins.getFromType());
        assertEquals(double.class, ins.getType());

        ins.setType(long.class);
        assertEquals(Constants.NOP, ins.getOpcode());
        assertEquals(long.class, ins.getFromType());
        assertEquals(long.class, ins.getType());

        ins.setType(int.class);
        assertEquals(Constants.L2I, ins.getOpcode());
        assertEquals(long.class, ins.getFromType());
        assertEquals(int.class, ins.getType());

        ins.setType(String.class);
        assertEquals(Constants.L2I, ins.getOpcode());

        ins.setType((Class) null);
        assertEquals(Constants.NOP, ins.getOpcode());

        ins.setType(float.class);
        assertEquals(Constants.L2F, ins.getOpcode());
    }

    public static Test suite() {
        return new TestSuite(TestConvertInstruction.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
