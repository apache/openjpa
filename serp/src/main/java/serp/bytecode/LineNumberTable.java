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

import java.io.*;
import java.util.*;
import serp.bytecode.visitor.*;

/**
 * Code blocks compiled from source have line number tables mapping
 * opcodes to source lines. This table automatically maintains line
 * numbers in ascending order by their start program counter position
 * at all times.
 * 
 * @author Abe White
 */
public class LineNumberTable extends Attribute implements InstructionPtr {
    private List _lineNumbers = new ArrayList();

    LineNumberTable(int nameIndex, Attributes owner) {
        super(nameIndex, owner);
    }

    /**
     * Return the line numbers held in this table.
     */
    public LineNumber[] getLineNumbers() {
        Collections.sort(_lineNumbers);
        return(LineNumber[]) _lineNumbers.toArray
            (new LineNumber[_lineNumbers.size()]);
    }

    /**
     * Return the line number for the given program counter, or null if none.
     */
    public LineNumber getLineNumber(int pc) {
        for (int i = _lineNumbers.size() - 1; i >= 0; i--)
            if (((LineNumber) _lineNumbers.get(i)).
                _target.getByteIndex() <= pc)
                return(LineNumber) _lineNumbers.get(i);
        return null;
    }

    /**
     * Return the line number for the given instruction, or null if none.
     */
    public LineNumber getLineNumber(Instruction ins) {
        if (ins == null)
            return null;
        return getLineNumber(ins.getByteIndex());
    }

    /**
     * Set the line numbers for the table. This method is useful when
     * importing line numbers from another method.
     */
    public void setLineNumbers(LineNumber[] lines) {
        clear();
        if (lines != null)
            for (int i = 0; i < lines.length; i++)
                addLineNumber(lines[i]);
    }

    /**
     * Import a line number from another method.
     * 
     * @return the newly added line number
     */
    public LineNumber addLineNumber(LineNumber ln) {
        LineNumber line = addLineNumber();
        line.setStartPc(ln.getStartPc());
        line.setLine(ln.getLine());
        return line;
    }

    /**
     * Add a new line number to this table.
     */
    public LineNumber addLineNumber() {
        LineNumber ln = new LineNumber(this);
        _lineNumbers.add(ln);
        return ln;
    }

    /**
     * Add a new line number to this table.
     */
    public LineNumber addLineNumber(int startPc, int line) {
        LineNumber ln = addLineNumber();
        ln.setStartPc(startPc);
        ln.setLine(line);
        return ln;
    }

    /**
     * Add a new line number to this table.
     */
    public LineNumber addLineNumber(Instruction start, int line) {
        LineNumber ln = addLineNumber();
        ln.setStart(start);
        ln.setLine(line);
        return ln;
    }

    /**
     * Clear the line numbers.
     */
    public void clear() {
        for (int i = 0; i < _lineNumbers.size(); i++)
            ((LineNumber) _lineNumbers.get(i)).invalidate();
        _lineNumbers.clear();
    }

    /**
     * Remove the given line.
     * 
     * @return true if the line was removed, false otherwise
     */
    public boolean removeLineNumber(LineNumber ln) {
        if (ln == null || !_lineNumbers.remove(ln))
            return false;

        ln.invalidate();
        return true;
    }

    /**
     * Remove the line number for the given program counter.
     * 
     * @return true if the line was removed, false otherwise
     */
    public boolean removeLineNumber(int pc) {
        return removeLineNumber(getLineNumber(pc));
    }

    /**
     * Remove the line number for the given instruction.
     * 
     * @return true if the line was removed, false otherwise
     */
    public boolean removeLineNumber(Instruction ins) {
        return removeLineNumber(getLineNumber(ins));
    }

    public void updateTargets() {
        for (int i = 0; i < _lineNumbers.size(); i++)
            ((LineNumber) _lineNumbers.get(i)).updateTargets();
    }

    public void replaceTarget(Instruction oldTarget, Instruction newTarget) {
        for (int i = 0; i < _lineNumbers.size(); i++)
            ((LineNumber) _lineNumbers.get(i)).replaceTarget(oldTarget,
                newTarget);
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterLineNumberTable(this);

        LineNumber[] lines = getLineNumbers();
        for (int i = 0; i < lines.length; i++)
            lines[i].acceptVisit(visit);

        visit.exitLineNumberTable(this);
    }

    int getLength() {
        return 2 + 4 * _lineNumbers.size();
    }

    void read(Attribute other) {
        setLineNumbers(((LineNumberTable) other).getLineNumbers());
    }

    void read(DataInput in, int length) throws IOException {
        clear();
        int numLines = in.readUnsignedShort();

        LineNumber lineNumber;
        for (int i = 0; i < numLines; i++) {
            lineNumber = addLineNumber();
            lineNumber.read(in);
        }
    }

    void write(DataOutput out, int length) throws IOException {
        LineNumber[] lines = getLineNumbers();
        out.writeShort(lines.length);
        for (int i = 0; i < lines.length; i++)
            lines[i].write(out);
    }

    public Code getCode() {
        return(Code) getOwner();
    }
}
