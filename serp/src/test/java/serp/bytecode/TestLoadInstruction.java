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
 *  <p>Tests the {@link LoadInstruction} type.</p>
 *
 *  @author Abe White
 */
public class TestLoadInstruction extends TestCase {
    private Code _code = new Code();

    public TestLoadInstruction(String test) {
        super(test);
    }

    /**
     *  Test that the instruction intitializes correctly when generated.
     */
    public void testInitialize() {
        assertEquals(Constants.NOP, _code.xload().getOpcode());
        assertNull(_code.xload().getType());
        assertEquals(Constants.NOP, _code.iload().getOpcode());
        assertEquals(int.class, _code.iload().getType());
        assertEquals(Constants.NOP, _code.lload().getOpcode());
        assertEquals(long.class, _code.lload().getType());
        assertEquals(Constants.NOP, _code.fload().getOpcode());
        assertEquals(float.class, _code.fload().getType());
        assertEquals(Constants.NOP, _code.dload().getOpcode());
        assertEquals(double.class, _code.dload().getType());
        assertEquals(Constants.NOP, _code.aload().getOpcode());
        assertEquals(Object.class, _code.aload().getType());
    }

    /**
     *  Test that the instruction returns its type correctly.
     */
    public void testGetType() {
        LoadInstruction ins = _code.xload();
        assertNull(ins.getType());
        assertEquals(-1, ins.getLocal());

        ins = _code.iload();
        assertEquals(int.class, ins.getType());
        assertEquals(int.class, ins.setLocal(1).getType());
        assertEquals(int.class, ins.setLocal(2).getType());
        assertEquals(int.class, ins.setLocal(3).getType());
        assertEquals(int.class, ins.setLocal(100).getType());

        ins = _code.lload();
        assertEquals(long.class, ins.getType());
        assertEquals(long.class, ins.setLocal(1).getType());
        assertEquals(long.class, ins.setLocal(2).getType());
        assertEquals(long.class, ins.setLocal(3).getType());
        assertEquals(long.class, ins.setLocal(100).getType());

        ins = _code.fload();
        assertEquals(float.class, ins.getType());
        assertEquals(float.class, ins.setLocal(1).getType());
        assertEquals(float.class, ins.setLocal(2).getType());
        assertEquals(float.class, ins.setLocal(3).getType());
        assertEquals(float.class, ins.setLocal(100).getType());

        ins = _code.dload();
        assertEquals(double.class, ins.getType());
        assertEquals(double.class, ins.setLocal(1).getType());
        assertEquals(double.class, ins.setLocal(2).getType());
        assertEquals(double.class, ins.setLocal(3).getType());
        assertEquals(double.class, ins.setLocal(100).getType());

        ins = _code.aload();
        assertEquals(Object.class, ins.getType());
        assertEquals(Object.class, ins.setLocal(1).getType());
        assertEquals(Object.class, ins.setLocal(2).getType());
        assertEquals(Object.class, ins.setLocal(3).getType());
        assertEquals(Object.class, ins.setLocal(100).getType());
    }

    /**
     *  Test that the opcode is morphed correctly when the type and local
     *  of the instruction are changed.
     */
    public void testOpcodeMorph() {
        LoadInstruction ins = _code.xload();

        assertEquals(Constants.NOP, ins.getOpcode());
        assertEquals(Constants.NOP, ins.setType(int.class).getOpcode());
        assertEquals(Constants.ILOAD, ins.setLocal(10).getOpcode());
        assertEquals(Constants.ILOAD, ins.setType(boolean.class).getOpcode());
        assertEquals(Constants.ILOAD, ins.setType(byte.class).getOpcode());
        assertEquals(Constants.ILOAD, ins.setType(char.class).getOpcode());
        assertEquals(Constants.ILOAD, ins.setType(short.class).getOpcode());
        assertEquals(Constants.ILOAD0, ins.setLocal(0).getOpcode());
        assertEquals(0, ins.getLocal());
        assertEquals(Constants.ILOAD1, ins.setLocal(1).getOpcode());
        assertEquals(1, ins.getLocal());
        assertEquals(Constants.ILOAD2, ins.setLocal(2).getOpcode());
        assertEquals(2, ins.getLocal());
        assertEquals(Constants.ILOAD3, ins.setLocal(3).getOpcode());
        assertEquals(3, ins.getLocal());
        assertEquals(Constants.ILOAD, ins.setLocal(4).getOpcode());
        assertEquals(4, ins.getLocal());

        assertEquals(Constants.LLOAD, ins.setType(long.class).getOpcode());
        assertEquals(Constants.LLOAD0, ins.setLocal(0).getOpcode());
        assertEquals(0, ins.getLocal());
        assertEquals(Constants.LLOAD1, ins.setLocal(1).getOpcode());
        assertEquals(1, ins.getLocal());
        assertEquals(Constants.LLOAD2, ins.setLocal(2).getOpcode());
        assertEquals(2, ins.getLocal());
        assertEquals(Constants.LLOAD3, ins.setLocal(3).getOpcode());
        assertEquals(3, ins.getLocal());
        assertEquals(Constants.LLOAD, ins.setLocal(4).getOpcode());
        assertEquals(4, ins.getLocal());

        assertEquals(Constants.FLOAD, ins.setType(float.class).getOpcode());
        assertEquals(Constants.FLOAD0, ins.setLocal(0).getOpcode());
        assertEquals(0, ins.getLocal());
        assertEquals(Constants.FLOAD1, ins.setLocal(1).getOpcode());
        assertEquals(1, ins.getLocal());
        assertEquals(Constants.FLOAD2, ins.setLocal(2).getOpcode());
        assertEquals(2, ins.getLocal());
        assertEquals(Constants.FLOAD3, ins.setLocal(3).getOpcode());
        assertEquals(3, ins.getLocal());
        assertEquals(Constants.FLOAD, ins.setLocal(4).getOpcode());
        assertEquals(4, ins.getLocal());

        assertEquals(Constants.DLOAD, ins.setType(double.class).getOpcode());
        assertEquals(Constants.DLOAD0, ins.setLocal(0).getOpcode());
        assertEquals(0, ins.getLocal());
        assertEquals(Constants.DLOAD1, ins.setLocal(1).getOpcode());
        assertEquals(1, ins.getLocal());
        assertEquals(Constants.DLOAD2, ins.setLocal(2).getOpcode());
        assertEquals(2, ins.getLocal());
        assertEquals(Constants.DLOAD3, ins.setLocal(3).getOpcode());
        assertEquals(3, ins.getLocal());
        assertEquals(Constants.DLOAD, ins.setLocal(4).getOpcode());
        assertEquals(4, ins.getLocal());

        assertEquals(Constants.ALOAD, ins.setType(Object.class).getOpcode());
        assertEquals(Constants.ALOAD, ins.setType(String.class).getOpcode());
        assertEquals(Constants.ALOAD0, ins.setLocal(0).getOpcode());
        assertEquals(0, ins.getLocal());
        assertEquals(Constants.ALOAD1, ins.setLocal(1).getOpcode());
        assertEquals(1, ins.getLocal());
        assertEquals(Constants.ALOAD2, ins.setLocal(2).getOpcode());
        assertEquals(2, ins.getLocal());
        assertEquals(Constants.ALOAD3, ins.setLocal(3).getOpcode());
        assertEquals(3, ins.getLocal());
        assertEquals(Constants.ALOAD, ins.setLocal(4).getOpcode());
        assertEquals(4, ins.getLocal());
    }

    public static Test suite() {
        return new TestSuite(TestLoadInstruction.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
