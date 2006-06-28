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

import serp.bytecode.lowlevel.*;

import serp.bytecode.visitor.*;

import serp.util.*;

import java.io.*;

import java.util.*;


/**
 *  <p>Represents a <code>try {} catch() {}</code> statement in bytecode.</p>
 *
 *  @author Abe White
 */
public class ExceptionHandler implements InstructionPtr, BCEntity,
    VisitAcceptor {
    private int _catchIndex = 0;
    private Code _owner = null;
    private InstructionPtrStrategy _tryStart = new InstructionPtrStrategy(this);
    private InstructionPtrStrategy _tryEnd = new InstructionPtrStrategy(this);
    private InstructionPtrStrategy _tryHandler = new InstructionPtrStrategy(this);

    ExceptionHandler(Code owner) {
        _owner = owner;
    }

    /**
     *  Return the owning code block.
     */
    public Code getCode() {
        return _owner;
    }

    ///////////////////
    // Body operations
    ///////////////////

    /**
      *  Return the instruction marking the beginning of the try {} block.
     */
    public Instruction getTryStart() {
        return _tryStart.getTargetInstruction();
    }

    /**
     *  Set the {@link Instruction} marking the beginning of the try block.
     *  The instruction must already be a part of the method.
     */
    public void setTryStart(Instruction instruction) {
        _tryStart.setTargetInstruction(instruction);
    }

    /**
      *  Return the instruction at the end of the try {} block.
     */
    public Instruction getTryEnd() {
        return _tryEnd.getTargetInstruction();
    }

    /**
     *  Set the Instruction at the end of the try block.  The
     *  Instruction must already be a part of the method.
     */
    public void setTryEnd(Instruction instruction) {
        _tryEnd.setTargetInstruction(instruction);
    }

    //////////////////////
    // Handler operations
    //////////////////////

    /**
      *  Return the instruction marking the beginning of the catch {} block.
     */
    public Instruction getHandlerStart() {
        return _tryHandler.getTargetInstruction();
    }

    /**
     *  Set the {@link Instruction} marking the beginning of the catch block.
     *  The instruction must already be a part of the method.
     *  WARNING: if this instruction is deleted, the results are undefined.
     */
    public void setHandlerStart(Instruction instruction) {
        _tryHandler.setTargetInstruction(instruction);
    }

    ////////////////////
    // Catch operations
    ////////////////////

    /**
     *  Return the index into the class {@link ConstantPool} of the
     *  {@link ClassEntry} describing the exception type this handler catches.
     */
    public int getCatchIndex() {
        return _catchIndex;
    }

    /**
     *  Set the index into the class {@link ConstantPool} of the
     *  {@link ClassEntry} describing the exception type this handler catches.
     */
    public void setCatchIndex(int catchTypeIndex) {
        _catchIndex = catchTypeIndex;
    }

    /**
     *  Return the name of the exception type; returns null for catch-all
     *  clauses used to implement finally blocks.  The name will be returned
     *  in a forum suitable for a {@link Class#forName} call.
     */
    public String getCatchName() {
        if (_catchIndex == 0) {
            return null;
        }

        ClassEntry entry = (ClassEntry) getPool().getEntry(_catchIndex);

        return getProject().getNameCache()
                   .getExternalForm(entry.getNameEntry().getValue(), false);
    }

    /**
     *  Return the {@link Class} of the exception type; returns null for
     *  catch-all clauses used to implement finally blocks.
     */
    public Class getCatchType() {
        String name = getCatchName();

        if (name == null) {
            return null;
        }

        return Strings.toClass(name, getClassLoader());
    }

    /**
     *  Return the bytecode of the exception type; returns null for
     *  catch-all clauses used to implement finally blocks.
     */
    public BCClass getCatchBC() {
        String name = getCatchName();

        if (name == null) {
            return null;
        }

        return getProject().loadClass(name, getClassLoader());
    }

    /**
     *  Set the class of the exception type, or null for catch-all clauses used
     *  with finally blocks.
     */
    public void setCatch(String name) {
        if (name == null) {
            _catchIndex = 0;
        } else {
            _catchIndex = getPool()
                              .findClassEntry(getProject().getNameCache()
                                                  .getInternalForm(name, false),
                    true);
        }
    }

    /**
     *  Set the class of the exception type, or null for catch-all clauses used
     *  for finally blocks.
     */
    public void setCatch(Class type) {
        if (type == null) {
            setCatch((String) null);
        } else {
            setCatch(type.getName());
        }
    }

    /**
     *  Set the class of the exception type, or null for catch-all clauses used
     *  for finally blocks.
     */
    public void setCatch(BCClass type) {
        if (type == null) {
            setCatch((String) null);
        } else {
            setCatch(type.getName());
        }
    }

    /////////////////////////////////
    // InstructionPtr implementation
    /////////////////////////////////
    public void updateTargets() {
        _tryStart.updateTargets();
        _tryEnd.updateTargets();
        _tryHandler.updateTargets();
    }

    public void replaceTarget(Instruction oldTarget, Instruction newTarget) {
        _tryStart.replaceTarget(oldTarget, newTarget);
        _tryEnd.replaceTarget(oldTarget, newTarget);
        _tryHandler.replaceTarget(oldTarget, newTarget);
    }

    ///////////////////////////
    // BCEntity implementation
    ///////////////////////////
    public Project getProject() {
        return _owner.getProject();
    }

    public ConstantPool getPool() {
        return _owner.getPool();
    }

    public ClassLoader getClassLoader() {
        return _owner.getClassLoader();
    }

    public boolean isValid() {
        return _owner != null;
    }

    ////////////////////////////////
    // VisitAcceptor implementation
    ////////////////////////////////
    public void acceptVisit(BCVisitor visit) {
        visit.enterExceptionHandler(this);
        visit.exitExceptionHandler(this);
    }

    //////////////////
    // I/O operations
    //////////////////
    void read(ExceptionHandler orig) {
        _tryStart.setByteIndex(orig._tryStart.getByteIndex());
        _tryEnd.setByteIndex(orig._tryEnd.getByteIndex());
        _tryHandler.setByteIndex(orig._tryHandler.getByteIndex());

        // done at a high level so that if the name isn't in our constant pool,
        // it will be added
        setCatch(orig.getCatchName());
    }

    void read(DataInput in) throws IOException {
        setTryStart(in.readUnsignedShort());
        setTryEnd(in.readUnsignedShort());
        setHandlerStart(in.readUnsignedShort());
        setCatchIndex(in.readUnsignedShort());
    }

    void write(DataOutput out) throws IOException {
        out.writeShort(getTryStartPc());
        out.writeShort(getTryEndPc());
        out.writeShort(getHandlerStartPc());
        out.writeShort(getCatchIndex());
    }

    public void setTryStart(int start) {
        _tryStart.setByteIndex(start);
    }

    public int getTryStartPc() {
        return _tryStart.getByteIndex();
    }

    public void setTryEnd(int end) {
        setTryEnd((Instruction) _owner.getInstruction(end).prev);
    }

    /**
     *  Return the program counter end position for this exception handler.
     *  This represents an index into the code byte array.
     */
    public int getTryEndPc() {
        return _tryEnd.getByteIndex() + getTryEnd().getLength();
    }

    public void setHandlerStart(int handler) {
        _tryHandler.setByteIndex(handler);
    }

    public int getHandlerStartPc() {
        return _tryHandler.getByteIndex();
    }

    void invalidate() {
        _owner = null;
    }
}
