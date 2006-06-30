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
 * Tests the {@link Code} class.
 * 
 * @author Eric Lindauer
 */
public class TestCode extends TestCase {
    public TestCode(String test) {
        super(test);
    }

    /**
     * Test that removing Instructions from a Code block
     * removes the correct Instructions.
     */
    public void testRemove() {
        Code code = new Code();
        JumpInstruction go2 = code.go2();
        Instruction first = code.nop();
        Instruction second = code.nop();
        Instruction third = code.nop();
        Instruction fourth = code.nop();
        go2.setTarget(second);

        // remove 'second' after a next() call
        code.beforeFirst();
        code.next();
        code.next();
        code.next();
        code.remove();
        assertEquals(third, code.next());
        assertEquals(third, go2.getTarget());
        code.beforeFirst();
        assertEquals(go2, code.next());
        assertEquals(first, code.next());
        assertEquals(third, code.next());
        assertEquals(fourth, code.next());

        // remove 'third' after a previous() call
        code.beforeFirst();
        code.next();
        code.next();
        code.next();
        code.next();
        code.previous();
        code.previous();
        code.remove();
        assertEquals(fourth, go2.getTarget());
        assertEquals(fourth, code.next());

        assertTrue(!code.hasNext());
        assertEquals(fourth, code.previous());
        code.remove();
        code.afterLast();
        assertEquals(code.previous(), go2.getTarget());
        assertEquals(first, code.previous());
    }

    /**
     * Test that instruction indexes work correctly.
     */
    public void testIndexes() {
        Code code = new Code();
        assertEquals(0, code.nextIndex());
        assertEquals(-1, code.previousIndex());
        Instruction first = code.nop();
        assertEquals(1, code.nextIndex());
        assertEquals(0, code.previousIndex());
        Instruction second = code.nop();
        assertEquals(2, code.nextIndex());
        assertEquals(1, code.previousIndex());
        code.previous();
        assertEquals(1, code.nextIndex());
        assertEquals(0, code.previousIndex());
        code.next();
        assertEquals(2, code.nextIndex());
        assertEquals(1, code.previousIndex());
        Instruction third = code.nop();
        assertEquals(3, code.nextIndex());
        assertEquals(2, code.previousIndex());

        code.afterLast();
        assertEquals(3, code.nextIndex());
        assertEquals(2, code.previousIndex());

        code.beforeFirst();
        assertEquals(0, code.nextIndex());
        assertEquals(-1, code.previousIndex());

        code.before(first);
        assertEquals(0, code.nextIndex());
        assertEquals(-1, code.previousIndex());
        code.before(second);
        assertEquals(1, code.nextIndex());
        assertEquals(0, code.previousIndex());
        code.before(third);
        assertEquals(2, code.nextIndex());
        assertEquals(1, code.previousIndex());

        code.after(first);
        assertEquals(1, code.nextIndex());
        assertEquals(0, code.previousIndex());
        code.after(second);
        assertEquals(2, code.nextIndex());
        assertEquals(1, code.previousIndex());
        code.after(third);
        assertEquals(3, code.nextIndex());
        assertEquals(2, code.previousIndex());
    }

    public static Test suite() {
        return new TestSuite(TestCode.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
