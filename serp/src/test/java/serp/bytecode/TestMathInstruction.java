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
 * Tests the {@link MathInstruction} type.
 * 
 * @author Abe White
 */
public class TestMathInstruction extends TestCase {
    private Code _code = new Code();

    public TestMathInstruction(String test) {
        super(test);
    }

    /**
     * Test that the instruction intitializes correctly when generated.
     */
    public void testInitialize() {
        assertEquals(Constants.NOP, _code.math().getOpcode());
        assertEquals(Constants.NOP, _code.xadd().getOpcode());
        assertEquals(Constants.IADD, _code.iadd().getOpcode());
        assertEquals(Constants.LADD, _code.ladd().getOpcode());
        assertEquals(Constants.FADD, _code.fadd().getOpcode());
        assertEquals(Constants.DADD, _code.dadd().getOpcode());
        assertEquals(Constants.NOP, _code.xsub().getOpcode());
        assertEquals(Constants.ISUB, _code.isub().getOpcode());
        assertEquals(Constants.LSUB, _code.lsub().getOpcode());
        assertEquals(Constants.FSUB, _code.fsub().getOpcode());
        assertEquals(Constants.DSUB, _code.dsub().getOpcode());
        assertEquals(Constants.NOP, _code.xmul().getOpcode());
        assertEquals(Constants.IMUL, _code.imul().getOpcode());
        assertEquals(Constants.LMUL, _code.lmul().getOpcode());
        assertEquals(Constants.FMUL, _code.fmul().getOpcode());
        assertEquals(Constants.DMUL, _code.dmul().getOpcode());
        assertEquals(Constants.NOP, _code.xdiv().getOpcode());
        assertEquals(Constants.IDIV, _code.idiv().getOpcode());
        assertEquals(Constants.LDIV, _code.ldiv().getOpcode());
        assertEquals(Constants.FDIV, _code.fdiv().getOpcode());
        assertEquals(Constants.DDIV, _code.ddiv().getOpcode());
        assertEquals(Constants.NOP, _code.xrem().getOpcode());
        assertEquals(Constants.IREM, _code.irem().getOpcode());
        assertEquals(Constants.LREM, _code.lrem().getOpcode());
        assertEquals(Constants.FREM, _code.frem().getOpcode());
        assertEquals(Constants.DREM, _code.drem().getOpcode());
        assertEquals(Constants.NOP, _code.xneg().getOpcode());
        assertEquals(Constants.INEG, _code.ineg().getOpcode());
        assertEquals(Constants.LNEG, _code.lneg().getOpcode());
        assertEquals(Constants.FNEG, _code.fneg().getOpcode());
        assertEquals(Constants.DNEG, _code.dneg().getOpcode());
        assertEquals(Constants.NOP, _code.xshl().getOpcode());
        assertEquals(Constants.ISHL, _code.ishl().getOpcode());
        assertEquals(Constants.LSHL, _code.lshl().getOpcode());
        assertEquals(Constants.NOP, _code.xshr().getOpcode());
        assertEquals(Constants.ISHR, _code.ishr().getOpcode());
        assertEquals(Constants.LSHR, _code.lshr().getOpcode());
        assertEquals(Constants.NOP, _code.xushr().getOpcode());
        assertEquals(Constants.IUSHR, _code.iushr().getOpcode());
        assertEquals(Constants.LUSHR, _code.lushr().getOpcode());
        assertEquals(Constants.NOP, _code.xand().getOpcode());
        assertEquals(Constants.IAND, _code.iand().getOpcode());
        assertEquals(Constants.LAND, _code.land().getOpcode());
        assertEquals(Constants.NOP, _code.xor().getOpcode());
        assertEquals(Constants.IOR, _code.ior().getOpcode());
        assertEquals(Constants.LOR, _code.lor().getOpcode());
        assertEquals(Constants.NOP, _code.xxor().getOpcode());
        assertEquals(Constants.IXOR, _code.ixor().getOpcode());
        assertEquals(Constants.LXOR, _code.lxor().getOpcode());
    }

    /**
     * Test that the instruction returns its type correctly.
     */
    public void testGetType() {
        MathInstruction ins = _code.math();
        assertNull(ins.getType());
        assertEquals(-1, ins.getOperation());

        ins = _code.xadd();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_ADD, ins.getOperation());
        ins = _code.iadd();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_ADD, ins.getOperation());
        ins = _code.ladd();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_ADD, ins.getOperation());
        ins = _code.fadd();
        assertEquals(float.class, ins.getType());
        assertEquals(Constants.MATH_ADD, ins.getOperation());
        ins = _code.dadd();
        assertEquals(double.class, ins.getType());
        assertEquals(Constants.MATH_ADD, ins.getOperation());

        ins = _code.xsub();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_SUB, ins.getOperation());
        ins = _code.isub();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_SUB, ins.getOperation());
        ins = _code.lsub();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_SUB, ins.getOperation());
        ins = _code.fsub();
        assertEquals(float.class, ins.getType());
        assertEquals(Constants.MATH_SUB, ins.getOperation());
        ins = _code.dsub();
        assertEquals(double.class, ins.getType());
        assertEquals(Constants.MATH_SUB, ins.getOperation());

        ins = _code.xmul();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_MUL, ins.getOperation());
        ins = _code.imul();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_MUL, ins.getOperation());
        ins = _code.lmul();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_MUL, ins.getOperation());
        ins = _code.fmul();
        assertEquals(float.class, ins.getType());
        assertEquals(Constants.MATH_MUL, ins.getOperation());
        ins = _code.dmul();
        assertEquals(double.class, ins.getType());
        assertEquals(Constants.MATH_MUL, ins.getOperation());

        ins = _code.xdiv();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_DIV, ins.getOperation());
        ins = _code.idiv();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_DIV, ins.getOperation());
        ins = _code.ldiv();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_DIV, ins.getOperation());
        ins = _code.fdiv();
        assertEquals(float.class, ins.getType());
        assertEquals(Constants.MATH_DIV, ins.getOperation());
        ins = _code.ddiv();
        assertEquals(double.class, ins.getType());
        assertEquals(Constants.MATH_DIV, ins.getOperation());

        ins = _code.xrem();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_REM, ins.getOperation());
        ins = _code.irem();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_REM, ins.getOperation());
        ins = _code.lrem();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_REM, ins.getOperation());
        ins = _code.frem();
        assertEquals(float.class, ins.getType());
        assertEquals(Constants.MATH_REM, ins.getOperation());
        ins = _code.drem();
        assertEquals(double.class, ins.getType());
        assertEquals(Constants.MATH_REM, ins.getOperation());

        ins = _code.xneg();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_NEG, ins.getOperation());
        ins = _code.ineg();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_NEG, ins.getOperation());
        ins = _code.lneg();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_NEG, ins.getOperation());
        ins = _code.fneg();
        assertEquals(float.class, ins.getType());
        assertEquals(Constants.MATH_NEG, ins.getOperation());
        ins = _code.dneg();
        assertEquals(double.class, ins.getType());
        assertEquals(Constants.MATH_NEG, ins.getOperation());

        ins = _code.xshl();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_SHL, ins.getOperation());
        ins = _code.ishl();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_SHL, ins.getOperation());
        ins = _code.lshl();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_SHL, ins.getOperation());

        ins = _code.xshr();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_SHR, ins.getOperation());
        ins = _code.ishr();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_SHR, ins.getOperation());
        ins = _code.lshr();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_SHR, ins.getOperation());

        ins = _code.xushr();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_USHR, ins.getOperation());
        ins = _code.iushr();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_USHR, ins.getOperation());
        ins = _code.lushr();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_USHR, ins.getOperation());

        ins = _code.xand();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_AND, ins.getOperation());
        ins = _code.iand();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_AND, ins.getOperation());
        ins = _code.land();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_AND, ins.getOperation());

        ins = _code.xor();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_OR, ins.getOperation());
        ins = _code.ior();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_OR, ins.getOperation());
        ins = _code.lor();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_OR, ins.getOperation());

        ins = _code.xxor();
        assertNull(ins.getType());
        assertEquals(Constants.MATH_XOR, ins.getOperation());
        ins = _code.ixor();
        assertEquals(int.class, ins.getType());
        assertEquals(Constants.MATH_XOR, ins.getOperation());
        ins = _code.lxor();
        assertEquals(long.class, ins.getType());
        assertEquals(Constants.MATH_XOR, ins.getOperation());
    }

    /**
     * Test that the opcode is morphed correctly when the type and operation
     * of the instruction are changed.
     */
    public void testOpcodeMorph() {
        MathInstruction math = _code.math();

        math.setOperation(Constants.MATH_ADD);
        assertEquals(Constants.NOP, math.setType((String) null).getOpcode());
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.NOP, math.setType((BCClass) null).getOpcode());
        assertEquals(Constants.IADD, math.setType(int.class).getOpcode());
        assertEquals(Constants.LADD, math.setType(long.class).getOpcode());
        assertEquals(Constants.FADD, math.setType(float.class).getOpcode());
        assertEquals(Constants.DADD, math.setType(double.class).getOpcode());
        assertEquals(Constants.IADD, math.setType(boolean.class).getOpcode());
        assertEquals(Constants.IADD, math.setType(short.class).getOpcode());
        assertEquals(Constants.IADD, math.setType(char.class).getOpcode());

        math.setOperation(Constants.MATH_SUB);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.ISUB, math.setType(int.class).getOpcode());
        assertEquals(Constants.LSUB, math.setType(long.class).getOpcode());
        assertEquals(Constants.FSUB, math.setType(float.class).getOpcode());
        assertEquals(Constants.DSUB, math.setType(double.class).getOpcode());

        math.setOperation(Constants.MATH_MUL);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IMUL, math.setType(int.class).getOpcode());
        assertEquals(Constants.LMUL, math.setType(long.class).getOpcode());
        assertEquals(Constants.FMUL, math.setType(float.class).getOpcode());
        assertEquals(Constants.DMUL, math.setType(double.class).getOpcode());

        math.setOperation(Constants.MATH_DIV);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IDIV, math.setType(int.class).getOpcode());
        assertEquals(Constants.LDIV, math.setType(long.class).getOpcode());
        assertEquals(Constants.FDIV, math.setType(float.class).getOpcode());
        assertEquals(Constants.DDIV, math.setType(double.class).getOpcode());

        math.setOperation(Constants.MATH_REM);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IREM, math.setType(int.class).getOpcode());
        assertEquals(Constants.LREM, math.setType(long.class).getOpcode());
        assertEquals(Constants.FREM, math.setType(float.class).getOpcode());
        assertEquals(Constants.DREM, math.setType(double.class).getOpcode());

        math.setOperation(Constants.MATH_NEG);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.INEG, math.setType(int.class).getOpcode());
        assertEquals(Constants.LNEG, math.setType(long.class).getOpcode());
        assertEquals(Constants.FNEG, math.setType(float.class).getOpcode());
        assertEquals(Constants.DNEG, math.setType(double.class).getOpcode());

        math.setOperation(Constants.MATH_SHL);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.ISHL, math.setType(int.class).getOpcode());
        assertEquals(Constants.LSHL, math.setType(long.class).getOpcode());

        math.setOperation(Constants.MATH_SHR);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.ISHR, math.setType(int.class).getOpcode());
        assertEquals(Constants.LSHR, math.setType(long.class).getOpcode());

        math.setOperation(Constants.MATH_USHR);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IUSHR, math.setType(int.class).getOpcode());
        assertEquals(Constants.LUSHR, math.setType(long.class).getOpcode());

        math.setOperation(Constants.MATH_AND);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IAND, math.setType(int.class).getOpcode());
        assertEquals(Constants.LAND, math.setType(long.class).getOpcode());

        math.setOperation(Constants.MATH_OR);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IOR, math.setType(int.class).getOpcode());
        assertEquals(Constants.LOR, math.setType(long.class).getOpcode());

        math.setOperation(Constants.MATH_XOR);
        assertEquals(Constants.NOP, math.setType((Class) null).getOpcode());
        assertEquals(Constants.IXOR, math.setType(int.class).getOpcode());
        assertEquals(Constants.LXOR, math.setType(long.class).getOpcode());
    }

    public static Test suite() {
        return new TestSuite(TestMathInstruction.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
