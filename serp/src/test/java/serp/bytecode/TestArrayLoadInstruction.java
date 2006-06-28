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

import junit.framework.*;

import junit.textui.*;


/**
 *  <p>Tests the {@link ArrayLoadInstruction} type.</p>
 *
 *  @author Abe White
 */
public class TestArrayLoadInstruction extends TestCase {
    private Code _code = new Code();

    public TestArrayLoadInstruction(String test) {
        super(test);
    }

    /**
     *  Test that the instruction initializes correctly when generated.
     */
    public void testIniitalize() {
        assertEquals(Constants.NOP, _code.xaload().getOpcode());
        assertEquals(Constants.IALOAD, _code.iaload().getOpcode());
        assertEquals(Constants.LALOAD, _code.laload().getOpcode());
        assertEquals(Constants.FALOAD, _code.faload().getOpcode());
        assertEquals(Constants.DALOAD, _code.daload().getOpcode());
        assertEquals(Constants.AALOAD, _code.aaload().getOpcode());
        assertEquals(Constants.BALOAD, _code.baload().getOpcode());
        assertEquals(Constants.CALOAD, _code.caload().getOpcode());
        assertEquals(Constants.SALOAD, _code.saload().getOpcode());
    }

    /**
     *  Test the the instruction returns its type correctly.
     */
    public void testGetType() {
        assertNull(_code.xaload().getType());
        assertEquals(int.class, _code.iaload().getType());
        assertEquals(long.class, _code.laload().getType());
        assertEquals(float.class, _code.faload().getType());
        assertEquals(double.class, _code.daload().getType());
        assertEquals(Object.class, _code.aaload().getType());
        assertEquals(byte.class, _code.baload().getType());
        assertEquals(char.class, _code.caload().getType());
        assertEquals(short.class, _code.saload().getType());
    }

    /**
     *  Test that the opcode morphs correctly with type changes.
     */
    public void testOpcodeMorph() {
        ArrayLoadInstruction ins = _code.xaload();
        assertEquals(Constants.NOP, ins.getOpcode());
        assertEquals(Constants.NOP, ins.setType((String) null).getOpcode());
        assertEquals(Constants.NOP, ins.setType((BCClass) null).getOpcode());
        assertEquals(Constants.NOP, ins.setType((Class) null).getOpcode());

        assertEquals(Constants.IALOAD, ins.setType(int.class).getOpcode());
        assertEquals(Constants.NOP, ins.setType((String) null).getOpcode());
        assertEquals(Constants.LALOAD, ins.setType(long.class).getOpcode());
        assertEquals(Constants.FALOAD, ins.setType(float.class).getOpcode());
        assertEquals(Constants.DALOAD, ins.setType(double.class).getOpcode());
        assertEquals(Constants.AALOAD, ins.setType(Object.class).getOpcode());
        assertEquals(Constants.BALOAD, ins.setType(byte.class).getOpcode());
        assertEquals(Constants.CALOAD, ins.setType(char.class).getOpcode());
        assertEquals(Constants.SALOAD, ins.setType(short.class).getOpcode());
        assertEquals(Constants.IALOAD, ins.setType(void.class).getOpcode());
        assertEquals(Constants.AALOAD, ins.setType(String.class).getOpcode());
        assertEquals(Constants.IALOAD, ins.setType(boolean.class).getOpcode());
    }

    public static Test suite() {
        return new TestSuite(TestArrayLoadInstruction.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
