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

import java.io.*;

import java.util.*;


/**
 *  <p>Representation of a code block of a class.
 *  The methods of this class mimic those of the same name in the
 *  {@link java.util.ListIterator} class.  Note that the size and index
 *  information of the code block will change as opcodes are added.</p>
 *
 *  <p>Code blocks are usually obtained from a {@link BCMethod}, but can also
 *  be constructed via the default constructor.  Blocks created this way can
 *  be used to provide template instructions to the various search/replace
 *  methods in this class.</p>
 *
 *  <p>The code class contains methods named after most JVM instructions, each
 *  of which adds the matching opcode to the code block at the
 *  current iterator position.  It also contains generic versions of various
 *  JVM instructions whose opcodes are not set until their properties are set
 *  with additional information.  Almost all instruction types are able to
 *  'morph' their opcode on the fly as the arguments to the instruction change.
 *  Thus the developer can initially call, for example, the <code>aload</code>
 *  opcode, but later change the type to load to <code>int</code> and the
 *  opcode will automatically morph to the <code>iload</code> opcode.</p>
 *
 *  @author Abe White
 */
public class Code extends Attribute {
    private final CodeEntry _head;
    private final CodeEntry _tail;
    private CodeIterator _ci;
    private int _maxStack = 0;
    private int _maxLocals = 0;
    private int _size = 0;
    private Collection _handlers = new LinkedList();
    private Collection _attrs = new LinkedList();

    Code(int nameIndex, Attributes owner) {
        super(nameIndex, owner);

        _head = new CodeEntry();
        _tail = new CodeEntry();
        _head.next = _tail;
        _tail.prev = _head;

        _ci = new CodeIterator(_head, -1);
    }

    /**
     *  The public constructor is for creating template code modules
      *  that produce {@link Instruction}s used in matching through
     *  the various <code>search</code> and <code>replace</code>
     *  methods.
     */
    public Code() {
        this(0,
            new Project().loadClass("", null).declareMethod("", void.class, null));
    }

    /**
     *  The owning method.
     */
    public BCMethod getMethod() {
        return (BCMethod) getOwner();
    }

    Collection getAttributesHolder() {
        return _attrs;
    }

    ////////////////////////////
    // Stack, Locals operations
    ////////////////////////////

    /**
     *  Return the maximum stack depth set for this code block.
     */
    public int getMaxStack() {
        return _maxStack;
    }

    /**
     *  Set the maximum stack depth for this code block.
     */
    public void setMaxStack(int max) {
        _maxStack = max;
    }

    /**
     *  Return the maximum number of local variables (including params)
     *  set for this method.
     */
    public int getMaxLocals() {
        return _maxLocals;
    }

    /**
     *  Set the maximum number of local variables (including params) in
     *  this method.
     */
    public void setMaxLocals(int max) {
        _maxLocals = max;
    }

    /**
     *  Return the local variable index for the paramIndex'th parameter to
     *  the method.         Local variable indexes differ from parameter indexes
     *  because:
     *  a) non-static methods use the 0th local variable for the 'this' ptr, and
     *  b) double and long values occupy two spots in the local
     *  variable array.
     *  Returns -1 if the given index is not valid.
     */
    public int getLocalsIndex(int paramIndex) {
        if (paramIndex < 0) {
            return -1;
        }

        int pos = 0;

        if (!getMethod().isStatic()) {
            pos = 1;
        }

        String[] params = getMethod().getParamNames();

        for (int i = 0; i < paramIndex; i++, pos++) {
            if (i == params.length) {
                return -1;
            }

            if (params[i].equals(long.class.getName()) ||
                    params[i].equals(double.class.getName())) {
                pos++;
            }
        }

        return pos;
    }

    /**
     *  Return the parameter index for the given local index, or -1 if
     *  the given local does not reference a param.
     *
     *  @see #getLocalsIndex
     */
    public int getParamsIndex(int localIndex) {
        int pos = 0;

        if (!getMethod().isStatic()) {
            pos = 1;
        }

        String[] params = getMethod().getParamNames();

        for (int i = 0; i < params.length; i++, pos++) {
            if (localIndex == pos) {
                return i;
            }

            if (params[i].equals(long.class.getName()) ||
                    params[i].equals(double.class.getName())) {
                pos++;
            }
        }

        return -1;
    }

    /**
     *  Return the next available local variable index.
     */
    public int getNextLocalsIndex() {
        calculateMaxLocals();

        return getMaxLocals();
    }

    /**
     *  Calculate and set the number of locals needed based on
     *  the instructions used and the parameters of the method this code
     *  block is a part of.
     *
      *  @see #setMaxLocals
     */
    public void calculateMaxLocals() {
        // start off assuming the max number needed is the 
        // number for all the params
        String[] params = getMethod().getParamNames();
        int max = 0;

        if ((params.length == 0) && !getMethod().isStatic()) {
            max = 1;
        } else if (params.length > 0) {
            max = getLocalsIndex(params.length - 1) + 1;

            if (params[params.length - 1].equals(long.class.getName()) ||
                    params[params.length - 1].equals(double.class.getName())) {
                max++;
            }
        }

        // check to see if there are any store instructions that
        // try to reference beyond that point
        StoreInstruction store;
        int current;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next) {
            current = 0;

            if (entry instanceof StoreInstruction) {
                store = (StoreInstruction) entry;
                current = store.getLocal() + 1;

                if (store.getType().equals(long.class) ||
                        store.getType().equals(double.class)) {
                    current++;
                }

                if (current > max) {
                    max = current;
                }
            }
        }

        setMaxLocals(max);
    }

    /**
     *  Calculate and set the maximum stack depth needed for
     *  the instructions used.
      *
     *  @see #setMaxStack
     */
    public void calculateMaxStack() {
        int stack = 0;
        int max = 0;

        ExceptionHandler[] handlers = getExceptionHandlers();
        Instruction ins;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next) {
            ins = (Instruction) entry;
            stack += ins.getStackChange();

            // if this is the start of a try, the exception will be placed
            // on the stack
            for (int j = 0; j < handlers.length; j++)
                if (handlers[j].getTryStart() == ins) {
                    stack++;
                }

            if (stack > max) {
                max = stack;
            }
        }

        setMaxStack(max);
    }

    ///////////////////////////////
    // ExceptionHandler operations
    ///////////////////////////////

    /**
     *  Return the exception handlers active in this code block, or an
     *  empty array if none.
     */
    public ExceptionHandler[] getExceptionHandlers() {
        return (ExceptionHandler[]) _handlers.toArray(new ExceptionHandler[_handlers.size()]);
    }

    /**
     *  Return the exception handler that catches the given exception type;
     *  if multiple handlers catch the given type, which is returned is
     *  undefined.
     */
    public ExceptionHandler getExceptionHandler(String catchType) {
        catchType = getProject().getNameCache().getExternalForm(catchType, false);

        String type;
        ExceptionHandler[] handlers = getExceptionHandlers();

        for (int i = 0; i < handlers.length; i++) {
            type = handlers[i].getCatchName();

            if (((type == null) && (catchType == null)) ||
                    ((type != null) && type.equals(catchType))) {
                return handlers[i];
            }
        }

        return null;
    }

    /**
     *  Return the exception handler that catches the given exception type;
     *  if multiple handlers catch the given type, which is returned is
     *  undefined.
     */
    public ExceptionHandler getExceptionHandler(Class catchType) {
        if (catchType == null) {
            return getExceptionHandler((String) null);
        }

        return getExceptionHandler(catchType.getName());
    }

    /**
     *  Return the exception handler that catches the given exception type;
     *  if multiple handlers catch the given type, which is returned is
     *  undefined.
     */
    public ExceptionHandler getExceptionHandler(BCClass catchType) {
        if (catchType == null) {
            return getExceptionHandler((String) null);
        }

        return getExceptionHandler(catchType.getName());
    }

    /**
     *  Return all exception handlers that catch the given exception type,
     *  or an empty array if none.
     */
    public ExceptionHandler[] getExceptionHandlers(String catchType) {
        catchType = getProject().getNameCache().getExternalForm(catchType, false);

        List matches = new LinkedList();
        String type;
        ExceptionHandler[] handlers = getExceptionHandlers();

        for (int i = 0; i < handlers.length; i++) {
            type = handlers[i].getCatchName();

            if (((type == null) && (catchType == null)) ||
                    ((type != null) && type.equals(catchType))) {
                matches.add(handlers[i]);
            }
        }

        return (ExceptionHandler[]) matches.toArray(new ExceptionHandler[matches.size()]);
    }

    /**
     *  Return all exception handlers that catch the given exception type,
     *  or an empty array if none.
     */
    public ExceptionHandler[] getExceptionHandlers(Class catchType) {
        if (catchType == null) {
            return getExceptionHandlers((String) null);
        }

        return getExceptionHandlers(catchType.getName());
    }

    /**
     *  Return all exception handlers that catch the given exception type,
     *  or an empty array if none.
     */
    public ExceptionHandler[] getExceptionHandlers(BCClass catchType) {
        if (catchType == null) {
            return getExceptionHandlers((String) null);
        }

        return getExceptionHandlers(catchType.getName());
    }

    /**
     *  Set the exception handlers for this code block.  This method is useful
     *  for importing all handlers from another code block.  Set to null or
     *  empty array if none.
     */
    public void setExceptionHandlers(ExceptionHandler[] handlers) {
        clearExceptionHandlers();

        if (handlers != null) {
            for (int i = 0; i < handlers.length; i++)
                addExceptionHandler(handlers[i]);
        }
    }

    /**
     *  Import the given exception handler from another code block.
      */
    public ExceptionHandler addExceptionHandler(ExceptionHandler handler) {
        ExceptionHandler newHandler = addExceptionHandler();
        newHandler.read(handler);

        return newHandler;
    }

    /**
     *  Add an exception handler to this code block.
     */
    public ExceptionHandler addExceptionHandler() {
        ExceptionHandler handler = new ExceptionHandler(this);
        _handlers.add(handler);

        return handler;
    }

    /**
     *  Add an exception handler to this code block.
     *
     *  @param tryStart                the first instruction of the try {} block
      *  @param tryEnd                        the last instruction of the try {} block
     *  @param handlerStart        the first instruction of the catch {} block
     *  @param catchType                the type of exception being caught
      */
    public ExceptionHandler addExceptionHandler(Instruction tryStart,
        Instruction tryEnd, Instruction handlerStart, String catchType) {
        ExceptionHandler handler = addExceptionHandler();
        handler.setTryStart(tryStart);
        handler.setTryEnd(tryEnd);
        handler.setHandlerStart(handlerStart);
        handler.setCatch(catchType);

        return handler;
    }

    /**
     *  Add an exception handler to this code block.
     *
     *  @param tryStart                the first instruction of the try {} block
      *  @param tryEnd                        the last instruction of the try {} block
     *  @param handlerStart        the first instruction of the catch {} block
     *  @param catchType                the type of exception being caught
     */
    public ExceptionHandler addExceptionHandler(Instruction tryStart,
        Instruction tryEnd, Instruction handlerStart, Class catchType) {
        String catchName = null;

        if (catchType != null) {
            catchName = catchType.getName();
        }

        return addExceptionHandler(tryStart, tryEnd, handlerStart, catchName);
    }

    /**
     *  Add an exception handler to this code block.
     *
     *  @param tryStart                the first instruction of the try {} block
      *  @param tryEnd                        the last instruction of the try {} block
     *  @param handlerStart        the first instruction of the catch {} block
     *  @param catchType                the type of exception being caught
     */
    public ExceptionHandler addExceptionHandler(Instruction tryStart,
        Instruction tryEnd, Instruction handlerStart, BCClass catchType) {
        String catchName = null;

        if (catchType != null) {
            catchName = catchType.getName();
        }

        return addExceptionHandler(tryStart, tryEnd, handlerStart, catchName);
    }

    /**
     *  Clear all exception handlers.
     */
    public void clearExceptionHandlers() {
        ExceptionHandler handler;

        for (Iterator itr = _handlers.iterator(); itr.hasNext();) {
            handler = (ExceptionHandler) itr.next();
            itr.remove();
            handler.invalidate();
        }
    }

    /**
      *  Remove the exception handler that catches the given type.
     */
    public boolean removeExceptionHandler(String catchType) {
        return removeExceptionHandler(getExceptionHandler(catchType));
    }

    /**
     *  Remove the exception handler that catches the given type.
     *
     *  @return true if the handler was removed, false otherwise
     */
    public boolean removeExceptionHandler(Class catchType) {
        if (catchType == null) {
            return removeExceptionHandler((String) null);
        }

        return removeExceptionHandler(catchType.getName());
    }

    /**
     *  Remove the exception handler that catches the given type.
     *
     *  @return true if the handler was removed, false otherwise
     */
    public boolean removeExceptionHandler(BCClass catchType) {
        if (catchType == null) {
            return removeExceptionHandler((String) null);
        }

        return removeExceptionHandler(catchType.getName());
    }

    /**
     *  Remove an exception handler from this code block.  The given handler
     *  must belong to this code block.
     */
    public boolean removeExceptionHandler(ExceptionHandler handler) {
        if ((handler == null) || !_handlers.remove(handler)) {
            return false;
        }

        handler.invalidate();

        return true;
    }

    /////////////////////////
    // Code block operations
    /////////////////////////

    /**
     *  Return the number of instructions in the method.
     */
    public int size() {
        return _size;
    }

    /**
      *  Reset the position of the instruction iterator to the first opcode.
     */
    public void beforeFirst() {
        _ci = new CodeIterator(_head, -1);
    }

    /**
     *  Set the position of the instruction iterator to after the last opcode.
     */
    public void afterLast() {
        if (_size == 0) {
            _ci = new CodeIterator(_head, -1);
        } else {
            _ci = new CodeIterator(_tail.prev, _size - 1);
        }
    }

    /**
     *  Position the iterator just before the given instruction.  The
      *  instruction must belong to this method.
     */
    public void before(Instruction ins) {
        if (ins.getCode() != this) {
            throw new IllegalArgumentException("ins.code != this");
        }

        _ci = new CodeIterator(ins.prev, CodeIterator.UNSET);
    }

    /**
     *  Position the iterator just after the given instruction.  The
      *  instruction must belong to this method.
     */
    public void after(Instruction ins) {
        before(ins);
        next();
    }

    /**
     *  Return true if a subsequent call to {@link #next} will return an
     *  instruction.
     */
    public boolean hasNext() {
        return _ci.hasNext();
    }

    /**
     *  Return true if a subsequent call to {@link #previous} will return an
     *  instruction.
     */
    public boolean hasPrevious() {
        return _ci.hasPrevious();
    }

    /**
     *  Return the next instruction.
     */
    public Instruction next() {
        return (Instruction) _ci.next();
    }

    /**
     *  Return the index of the next instruction, or {@link #size} if at end.
     */
    public int nextIndex() {
        return _ci.nextIndex();
    }

    /**
     *  Return the previous instruction.
     */
    public Instruction previous() {
        return (Instruction) _ci.previous();
    }

    /**
     *  Return the index of the previous instruction, or -1 if at beginning.
     */
    public int previousIndex() {
        return _ci.previousIndex();
    }

    /**
     *  Place the iterator before the given list index.
     */
    public void before(int index) {
        if ((index < 0) || (index >= _size)) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        CodeEntry entry = _head;

        for (int i = 0; i < index; entry = entry.next, i++)
            ;

        _ci = new CodeIterator(entry, index - 1);
    }

    /**
     *  Place the iterator after the given list index.
     */
    public void after(int index) {
        before(index);
        next();
    }

    /**
     *  Find the next instruction from the current iterator position that
     *  matches the given one, according to        the {@link Object#equals} methods of
     *  the instruction types.  This allows for matching based on template
     *  instructions, as the equals methods of most instructions return
     *  true if the information for the given instruction has not been filled
     *  in.  If a match is found, the iterator is placed after the matching
      *  Instruction.  If no match is found, moves the iterator to
     *  {@link #afterLast}.
      *
     *  @return true if match found
     */
    public boolean searchForward(Instruction template) {
        if (template == null) {
            return false;
        }

        while (hasNext())

            if (template.equalsInstruction(next())) {
                return true;
            }

        return false;
    }

    /**
     *  Find the closest previous instruction from the current iterator
     *  position that matches the given one, according to the
     *  {@link Object#equals} methods of the instruction types.  This allows
     *  for matching based on template instructions, as the equals methods of
     *  most instructions return true if the information for the given
     *  instruction has not been filled in.  If a match is found, the iterator
     *  is placed before the matching Instruction.  If no match is found,
     *  moves the iterator to {@link #beforeFirst}.
      *
     *  @return true if match found
     */
    public boolean searchBackward(Instruction template) {
        if (template == null) {
            return false;
        }

        while (hasPrevious())

            if (template.equalsInstruction(previous())) {
                return true;
            }

        return false;
    }

    /**
     *  Adds a copy of the given instruction.
      *
     *  @return the newly added instruction
     */
    public Instruction add(Instruction ins) {
        Instruction newIns = createInstruction(ins.getOpcode());
        newIns.read(ins);
        _ci.add(newIns);

        return newIns;
    }

    /**
     *  Replaces the last iterated instruction with a copy of the given one.
     *  This method will also make sure that all jump points
     *  that referenced the old opcode are updated correctly.
      *
     *  @return the newly added instruction
     *
     *  @see ListIterator#set
     */
    public Instruction set(Instruction ins) {
        Instruction newIns = createInstruction(ins.getOpcode());
        newIns.read(ins);
        _ci.set(newIns);

        return newIns;
    }

    /**
     *  Replaces all the instructions in this code block that match the
     *  given template with the given instruction.  After this method,
     *  the iterator will be {@link #afterLast}.
     *
     *  @return the number of substitutions made
     */
    public int replace(Instruction template, Instruction with) {
        beforeFirst();

        int count;

        for (count = 0; searchForward(template); count++)
            set(with);

        return count;
    }

    /**
     *  Equivalent to looping over each given template/replacement
     *  pair and calling {@link #replace(Instruction,Instruction)} for each.
     */
    public int replace(Instruction[] templates, Instruction[] with) {
        if ((templates == null) || (with == null)) {
            return 0;
        }

        int count = 0;

        for (int i = 0; i < templates.length; i++) {
            if (with == null) {
                count += replace(templates[i], null);
            } else {
                count += replace(templates[i], with[i]);
            }
        }

        return count;
    }

    /**
      *  Remove the last iterated instruction.
     *
     *  @see ListIterator#remove
     */
    public void remove() {
        _ci.remove();
    }

    //////////////////////////
    // Instruction operations
    //////////////////////////

    /**
     *  Load a class constant onto the stack.
     *  For primitive types, this translates into a
     *  getstatic for the TYPE field of the primitive's wrapper type.
     *  For non-primitives, things get much more complex.  Suffice it to
     *  say that the operation involves adding synthetic static fields
     *  and even methods to the class.  Note that this instruction requires
     *  up to 3 stack positions to execute.
     */
    public ClassConstantInstruction classconstant() {
        return new ClassConstantInstruction(getMethod().getDeclarer(), this,
            nop());
    }

    /**
     *  Add the <code>nop</code> opcode.
     */
    public Instruction nop() {
        return addInstruction(Constants.NOP);
    }

    /**
     *  Load some constant onto the stack.  The {@link ConstantInstruction}
     *  type takes any constant and correctly translates it into the proper
     *  opcode, depending on the constant type and value.  For example,
     *  if the constant value is set to 0L, the opcode will be set to
     *  <code>lconst0</code>.
     */
    public ConstantInstruction constant() {
        return (ConstantInstruction) addInstruction(new ConstantInstruction(
                this));
    }

    /**
     *  Load a local variable onto the stack.  This instruction will result
     *  in a <code>nop</code> until its type and local index are set.
     */
    public LoadInstruction xload() {
        return (LoadInstruction) addInstruction(new LoadInstruction(this));
    }

    /**
     *  Load an int local variable onto the stack.  This instruction will
     *  result in a <code>nop</code> until its local index is set.
     */
    public LoadInstruction iload() {
        return (LoadInstruction) addInstruction(new LoadInstruction(this).setType(
                int.class));
    }

    /**
     *  Load a long local variable onto the stack.  This instruction will
     *  result in a <code>nop</code> until its local index is set.
     */
    public LoadInstruction lload() {
        return (LoadInstruction) addInstruction(new LoadInstruction(this).setType(
                long.class));
    }

    /**
     *  Load a float local variable onto the stack.  This instruction will
     *  result in a <code>nop</code> until its local index is set.
     */
    public LoadInstruction fload() {
        return (LoadInstruction) addInstruction(new LoadInstruction(this).setType(
                float.class));
    }

    /**
     *  Load a double local variable onto the stack.  This instruction will
     *  result in a <code>nop</code> until its local index is set.
     */
    public LoadInstruction dload() {
        return (LoadInstruction) addInstruction(new LoadInstruction(this).setType(
                double.class));
    }

    /**
     *  Load an object local variable onto the stack.  This instruction will
     *  result in a <code>nop</code> until its local index is set.
     */
    public LoadInstruction aload() {
        return (LoadInstruction) addInstruction(new LoadInstruction(this).setType(
                Object.class));
    }

    /**
     *  Store a value from the stack into a local variable.  This instruction
     *  will result        in a <code>nop</code> until its type and local index are
     *  set.
     */
    public StoreInstruction xstore() {
        return (StoreInstruction) addInstruction(new StoreInstruction(this));
    }

    /**
     *  Store an int value from the stack into a local variable.  This
     *  instruction        will result        in a <code>nop</code> until its local index is
     *  set.
     */
    public StoreInstruction istore() {
        return (StoreInstruction) addInstruction(new StoreInstruction(this).setType(
                int.class));
    }

    /**
     *  Store a long value from the stack into a local variable.  This
     *  instruction        will result        in a <code>nop</code> until its local index is
     *  set.
     */
    public StoreInstruction lstore() {
        return (StoreInstruction) addInstruction(new StoreInstruction(this).setType(
                long.class));
    }

    /**
     *  Store a float value from the stack into a local variable.  This
     *  instruction        will result        in a <code>nop</code> until its local index is
     *  set.
     */
    public StoreInstruction fstore() {
        return (StoreInstruction) addInstruction(new StoreInstruction(this).setType(
                float.class));
    }

    /**
     *  Store a double value from the stack into a local variable.  This
     *  instruction        will result        in a <code>nop</code> until its local index is
     *  set.
     */
    public StoreInstruction dstore() {
        return (StoreInstruction) addInstruction(new StoreInstruction(this).setType(
                double.class));
    }

    /**
     *  Store an object value from the stack into a local variable.  This
     *  instruction        will result        in a <code>nop</code> until its local index is
     *  set.
     */
    public StoreInstruction astore() {
        return (StoreInstruction) addInstruction(new StoreInstruction(this).setType(
                Object.class));
    }

    /**
     *  Add the <code>ret</code> opcode, used in implementing
     *  <code>finally</code> clauses.
     */
    public RetInstruction ret() {
        return (RetInstruction) addInstruction(Constants.RET);
    }

    /**
     *  Add the <code>iinc</code> opcode.
     */
    public IIncInstruction iinc() {
        return (IIncInstruction) addInstruction(Constants.IINC);
    }

    /**
     *  Add the <code>wide</code> opcode.
     */
    public WideInstruction wide() {
        return (WideInstruction) addInstruction(Constants.WIDE);
    }

    /**
     *  Load an array value onto the stack.  This instruction will result
     *  in a <code>nop</code> until its type is set.
     */
    public ArrayLoadInstruction xaload() {
        return (ArrayLoadInstruction) addInstruction(new ArrayLoadInstruction(
                this));
    }

    /**
     *  Load an int array value onto the stack; the <code>iaload</code>
     *  opcode.
     */
    public ArrayLoadInstruction iaload() {
        return (ArrayLoadInstruction) addInstruction(Constants.IALOAD);
    }

    /**
     *  Load a long array value onto the stack; the <code>laload</code>
     *  opcode.
     */
    public ArrayLoadInstruction laload() {
        return (ArrayLoadInstruction) addInstruction(Constants.LALOAD);
    }

    /**
     *  Load a float array value onto the stack; the <code>faload</code>
     *  opcode.
     */
    public ArrayLoadInstruction faload() {
        return (ArrayLoadInstruction) addInstruction(Constants.FALOAD);
    }

    /**
     *  Load a double array value onto the stack; the <code>daload</code>
     *  opcode.
     */
    public ArrayLoadInstruction daload() {
        return (ArrayLoadInstruction) addInstruction(Constants.DALOAD);
    }

    /**
     *  Load an object array value onto the stack; the <code>aaload</code>
     *  opcode.
     */
    public ArrayLoadInstruction aaload() {
        return (ArrayLoadInstruction) addInstruction(Constants.AALOAD);
    }

    /**
     *  Load a byte array value onto the stack; the <code>baload</code>
     *  opcode.
     */
    public ArrayLoadInstruction baload() {
        return (ArrayLoadInstruction) addInstruction(Constants.BALOAD);
    }

    /**
     *  Load a char array value onto the stack; the <code>caload</code>
     *  opcode.
     */
    public ArrayLoadInstruction caload() {
        return (ArrayLoadInstruction) addInstruction(Constants.CALOAD);
    }

    /**
     *  Load a short array value onto the stack; the <code>saload</code>
     *  opcode.
     */
    public ArrayLoadInstruction saload() {
        return (ArrayLoadInstruction) addInstruction(Constants.SALOAD);
    }

    /**
     *  Store a value from the stack into an array.  This instruction
     *  will result in a <code>nop</code> until its type is set.
     */
    public ArrayStoreInstruction xastore() {
        return (ArrayStoreInstruction) addInstruction(new ArrayStoreInstruction(
                this));
    }

    /**
     *  Store an int value from the stack into an array; the
     *  <code>iastore</code> opcode.
     */
    public ArrayStoreInstruction iastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.IASTORE);
    }

    /**
     *  Store a long value from the stack into an array; the
     *  <code>lastore</code> opcode.
     */
    public ArrayStoreInstruction lastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.LASTORE);
    }

    /**
     *  Store a float value from the stack into an array; the
     *  <code>fastore</code> opcode.
     */
    public ArrayStoreInstruction fastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.FASTORE);
    }

    /**
     *  Store a double value from the stack into an array; the
     *  <code>dastore</code> opcode.
     */
    public ArrayStoreInstruction dastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.DASTORE);
    }

    /**
     *  Store an object value from the stack into an array; the
     *  <code>aastore</code> opcode.
     */
    public ArrayStoreInstruction aastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.AASTORE);
    }

    /**
     *  Store a byte value from the stack into an array; the
     *  <code>bastore</code> opcode.
     */
    public ArrayStoreInstruction bastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.BASTORE);
    }

    /**
     *  Store a char value from the stack into an array; the
     *  <code>castore</code> opcode.
     */
    public ArrayStoreInstruction castore() {
        return (ArrayStoreInstruction) addInstruction(Constants.CASTORE);
    }

    /**
     *  Store a short value from the stack into an array; the
     *  <code>sastore</code> opcode.
     */
    public ArrayStoreInstruction sastore() {
        return (ArrayStoreInstruction) addInstruction(Constants.SASTORE);
    }

    /**
     *  The <code>pop</code> opcode.
     */
    public StackInstruction pop() {
        return (StackInstruction) addInstruction(Constants.POP);
    }

    /**
     *  The <code>pop2</code> opcode.
     */
    public StackInstruction pop2() {
        return (StackInstruction) addInstruction(Constants.POP2);
    }

    /**
     *  The <code>dup</code> opcode.
     */
    public StackInstruction dup() {
        return (StackInstruction) addInstruction(Constants.DUP);
    }

    /**
     *  The <code>dupx1</code> opcode.
     */
    public StackInstruction dupx1() {
        return (StackInstruction) addInstruction(Constants.DUPX1);
    }

    /**
     *  The <code>dupx2</code> opcode.
     */
    public StackInstruction dupx2() {
        return (StackInstruction) addInstruction(Constants.DUPX2);
    }

    /**
     *  The <code>dup2</code> opcode.
     */
    public StackInstruction dup2() {
        return (StackInstruction) addInstruction(Constants.DUP2);
    }

    /**
     *  The <code>dup2x1</code> opcode.
     */
    public StackInstruction dup2x1() {
        return (StackInstruction) addInstruction(Constants.DUP2X1);
    }

    /**
     *  The <code>dup2x2</code> opcode.
     */
    public StackInstruction dup2x2() {
        return (StackInstruction) addInstruction(Constants.DUP2X2);
    }

    /**
     *  The <code>swap</code> opcode.
     */
    public StackInstruction swap() {
        return (StackInstruction) addInstruction(Constants.SWAP);
    }

    /**
     *  Perform some math operation on the stack items.  This instruction will
     *  result in a <code>nop</code> until its operation and type are set.
     */
    public MathInstruction math() {
        return (MathInstruction) addInstruction(new MathInstruction(this));
    }

    /**
     *  Add the top two stack values.  This instruction will result in
     *  a <code>nop</code> until its type is set.
     */
    public MathInstruction xadd() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_ADD);
    }

    /**
     *  Add the top two stack int values; the <code>iadd</code> opcode.
     */
    public MathInstruction iadd() {
        return (MathInstruction) addInstruction(Constants.IADD);
    }

    /**
     *  Add the top two stack long values; the <code>ladd</code> opcode.
     */
    public MathInstruction ladd() {
        return (MathInstruction) addInstruction(Constants.LADD);
    }

    /**
     *  Add the top two stack float values; the <code>fadd</code> opcode.
     */
    public MathInstruction fadd() {
        return (MathInstruction) addInstruction(Constants.FADD);
    }

    /**
     *  Add the top two stack double values; the <code>dadd</code> opcode.
     */
    public MathInstruction dadd() {
        return (MathInstruction) addInstruction(Constants.DADD);
    }

    /**
     *  Subtract the top two stack values.  This instruction will result in
     *  a <code>nop</code> until its type is set.
     */
    public MathInstruction xsub() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_SUB);
    }

    /**
     *  Subtract the top two stack int values; the <code>isub</code> opcode.
     */
    public MathInstruction isub() {
        return (MathInstruction) addInstruction(Constants.ISUB);
    }

    /**
     *  Subtract the top two stack long values; the <code>lsub</code> opcode.
     */
    public MathInstruction lsub() {
        return (MathInstruction) addInstruction(Constants.LSUB);
    }

    /**
     *  Subtract the top two stack float values; the <code>fsub</code> opcode.
     */
    public MathInstruction fsub() {
        return (MathInstruction) addInstruction(Constants.FSUB);
    }

    /**
     *  Subtract the top two stack double values; the <code>dsub</code> opcode.
     */
    public MathInstruction dsub() {
        return (MathInstruction) addInstruction(Constants.DSUB);
    }

    /**
     *  Multiply the top two stack values.  This instruction will result in
     *  a <code>nop</code> until its type is set.
     */
    public MathInstruction xmul() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_MUL);
    }

    /**
     *  Multiply the top two stack int values; the <code>imul</code> opcode.
     */
    public MathInstruction imul() {
        return (MathInstruction) addInstruction(Constants.IMUL);
    }

    /**
     *  Multiply the top two stack long values; the <code>lmul</code> opcode.
     */
    public MathInstruction lmul() {
        return (MathInstruction) addInstruction(Constants.LMUL);
    }

    /**
     *  Multiply the top two stack float values; the <code>fmul</code> opcode.
     */
    public MathInstruction fmul() {
        return (MathInstruction) addInstruction(Constants.FMUL);
    }

    /**
     *  Multiply the top two stack double values; the <code>dmul</code> opcode.
     */
    public MathInstruction dmul() {
        return (MathInstruction) addInstruction(Constants.DMUL);
    }

    /**
     *  Divide the top two stack values.  This instruction will result in
     *  a <code>nop</code> until its type is set.
     */
    public MathInstruction xdiv() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_DIV);
    }

    /**
     *  Divide the top two stack int values; the <code>idiv</code> opcode.
     */
    public MathInstruction idiv() {
        return (MathInstruction) addInstruction(Constants.IDIV);
    }

    /**
     *  Divide the top two stack long values; the <code>ldiv</code> opcode.
     */
    public MathInstruction ldiv() {
        return (MathInstruction) addInstruction(Constants.LDIV);
    }

    /**
     *  Divide the top two stack float values; the <code>fdiv</code> opcode.
     */
    public MathInstruction fdiv() {
        return (MathInstruction) addInstruction(Constants.FDIV);
    }

    /**
     *  Divide the top two stack double values; the <code>ddiv</code> opcode.
     */
    public MathInstruction ddiv() {
        return (MathInstruction) addInstruction(Constants.DDIV);
    }

    /**
     *  Take the remainder of the top two stack values.  This instruction will
     *  result in a <code>nop</code> until its type is set.
     */
    public MathInstruction xrem() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_REM);
    }

    /**
     *  Take the remainder of the top two int stack values; the
     *  <code>irem</code> opcode.
     */
    public MathInstruction irem() {
        return (MathInstruction) addInstruction(Constants.IREM);
    }

    /**
     *  Take the remainder of the top two long stack values; the
     *  <code>lrem</code> opcode.
     */
    public MathInstruction lrem() {
        return (MathInstruction) addInstruction(Constants.LREM);
    }

    /**
     *  Take the remainder of the top two float stack values; the
     *  <code>frem</code> opcode.
     */
    public MathInstruction frem() {
        return (MathInstruction) addInstruction(Constants.FREM);
    }

    /**
     *  Take the remainder of the top two double stack values; the
     *  <code>drem</code> opcode.
     */
    public MathInstruction drem() {
        return (MathInstruction) addInstruction(Constants.DREM);
    }

    /**
     *  Negate the top stack value.  This instruction will result in a
     *  <code>nop</code> until its type is set.
     */
    public MathInstruction xneg() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_NEG);
    }

    /**
     *  Negate the top stack int value; the <code>ineg</code> opcode.
     */
    public MathInstruction ineg() {
        return (MathInstruction) addInstruction(Constants.INEG);
    }

    /**
     *  Negate the top stack long value; the <code>lneg</code> opcode.
     */
    public MathInstruction lneg() {
        return (MathInstruction) addInstruction(Constants.LNEG);
    }

    /**
     *  Negate the top stack float value; the <code>fneg</code> opcode.
     */
    public MathInstruction fneg() {
        return (MathInstruction) addInstruction(Constants.FNEG);
    }

    /**
     *  Negate the top stack double value; the <code>dneg</code> opcode.
     */
    public MathInstruction dneg() {
        return (MathInstruction) addInstruction(Constants.DNEG);
    }

    /**
     *  Shift the top stack values.  This instruction will result in a
     *  <code>nop</code> until its type is set.
     */
    public MathInstruction xshl() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_SHL);
    }

    /**
     *  Shift the top stack int values; the <code>ishl</code> opcode.
     */
    public MathInstruction ishl() {
        return (MathInstruction) addInstruction(Constants.ISHL);
    }

    /**
     *  Shift the top stack long values; the <code>lshl</code> opcode.
     */
    public MathInstruction lshl() {
        return (MathInstruction) addInstruction(Constants.LSHL);
    }

    /**
     *  Shift the top stack values.  This instruction will result in a
     *  <code>nop</code> until its type is set.
     */
    public MathInstruction xshr() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_SHR);
    }

    /**
     *  Shift the top stack int values; the <code>ishr</code> opcode.
     */
    public MathInstruction ishr() {
        return (MathInstruction) addInstruction(Constants.ISHR);
    }

    /**
     *  Shift the top stack long values; the <code>lshr</code> opcode.
     */
    public MathInstruction lshr() {
        return (MathInstruction) addInstruction(Constants.LSHR);
    }

    /**
     *  Shift the top stack values.  This instruction will result in a
     *  <code>nop</code> until its type is set.
     */
    public MathInstruction xushr() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_USHR);
    }

    /**
     *  Shift the top stack int values; the <code>iushr</code> opcode.
     */
    public MathInstruction iushr() {
        return (MathInstruction) addInstruction(Constants.IUSHR);
    }

    /**
     *  Shift the top stack long values; the <code>lushr</code> opcode.
     */
    public MathInstruction lushr() {
        return (MathInstruction) addInstruction(Constants.LUSHR);
    }

    /**
     *  Take the mathematical and of the top two stack values.  This instruction
      *  results in a <code>nop</code> until its type is set.
     */
    public MathInstruction xand() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_AND);
    }

    /**
     *  Take the mathematical and of the top two stack int values; the
     *  <code>iand</code> opcode.
     */
    public MathInstruction iand() {
        return (MathInstruction) addInstruction(Constants.IAND);
    }

    /**
     *  Take the mathematical and of the top two stack long values; the
     *  <code>land</code> opcode.
     */
    public MathInstruction land() {
        return (MathInstruction) addInstruction(Constants.LAND);
    }

    /**
     *  Take the mathematical or of the top two stack values.  This instruction
      *  results in a <code>nop</code> until its type is set.
     */
    public MathInstruction xor() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_OR);
    }

    /**
     *  Take the mathematical or of the top two stack int values; the
     *  <code>ior</code> opcode.
     */
    public MathInstruction ior() {
        return (MathInstruction) addInstruction(Constants.IOR);
    }

    /**
     *  Take the mathematical or of the top two stack long values; the
     *  <code>lor</code> opcode.
     */
    public MathInstruction lor() {
        return (MathInstruction) addInstruction(Constants.LOR);
    }

    /**
     *  Take the mathematical xor of the top two stack values.  This instruction
      *  results in a <code>nop</code> until its type is set.
     */
    public MathInstruction xxor() {
        MathInstruction mi = math();

        return mi.setOperation(Constants.MATH_XOR);
    }

    /**
     *  Take the mathematical xor of the top two stack int values; the
     *  <code>ixor</code> opcode.
     */
    public MathInstruction ixor() {
        return (MathInstruction) addInstruction(Constants.IXOR);
    }

    /**
     *  Take the mathematical xor of the top two stack long values; the
     *  <code>lxor</code> opcode.
     */
    public MathInstruction lxor() {
        return (MathInstruction) addInstruction(Constants.LXOR);
    }

    /**
     *  Convert the top stack value to another type.  This instruction
     *  will result in a <code>nop</code> until the types to convert
     *  between are set.
     */
    public ConvertInstruction convert() {
        return (ConvertInstruction) addInstruction(new ConvertInstruction(this));
    }

    /**
     *  Compare the top two stack values.  This instruction will result in a
     *  <code>nop</code> until its type is set.
     */
    public CmpInstruction xcmp() {
        return (CmpInstruction) addInstruction(new CmpInstruction(this));
    }

    /**
     *  Compare the top two stack values; the <code>lcmp</code> opcode.
     */
    public CmpInstruction lcmp() {
        return (CmpInstruction) addInstruction(Constants.LCMP);
    }

    /**
     *  Compare the top two stack values; the <code>fcmpl</code> opcode.
     */
    public CmpInstruction fcmpl() {
        return (CmpInstruction) addInstruction(Constants.FCMPL);
    }

    /**
     *  Compare the top two stack values; the <code>fcmpg</code> opcode.
     */
    public CmpInstruction fcmpg() {
        return (CmpInstruction) addInstruction(Constants.FCMPG);
    }

    /**
     *  Compare the top two stack values; the <code>dcmpl</code> opcode.
     */
    public CmpInstruction dcmpl() {
        return (CmpInstruction) addInstruction(Constants.DCMPL);
    }

    /**
     *  Compare the top two stack values; the <code>dcmpg</code> opcode.
     */
    public CmpInstruction dcmpg() {
        return (CmpInstruction) addInstruction(Constants.DCMPG);
    }

    /**
     *  The <code>ifeq</code> opcode.
     */
    public IfInstruction ifeq() {
        return (IfInstruction) addInstruction(Constants.IFEQ);
    }

    /**
     *  The <code>ifne</code> opcode.
     */
    public IfInstruction ifne() {
        return (IfInstruction) addInstruction(Constants.IFNE);
    }

    /**
     *  The <code>iflt</code> opcode.
     */
    public IfInstruction iflt() {
        return (IfInstruction) addInstruction(Constants.IFLT);
    }

    /**
     *  The <code>ifge</code> opcode.
     */
    public IfInstruction ifge() {
        return (IfInstruction) addInstruction(Constants.IFGE);
    }

    /**
     *  The <code>ifgt</code> opcode.
     */
    public IfInstruction ifgt() {
        return (IfInstruction) addInstruction(Constants.IFGT);
    }

    /**
     *  The <code>ifle</code> opcode.
     */
    public IfInstruction ifle() {
        return (IfInstruction) addInstruction(Constants.IFLE);
    }

    /**
     *  The <code>ificmpeq</code> opcode.
     */
    public IfInstruction ificmpeq() {
        return (IfInstruction) addInstruction(Constants.IFICMPEQ);
    }

    /**
     *  The <code>ificmpne</code> opcode.
     */
    public IfInstruction ificmpne() {
        return (IfInstruction) addInstruction(Constants.IFICMPNE);
    }

    /**
     *  The <code>ificmplt</code> opcode.
     */
    public IfInstruction ificmplt() {
        return (IfInstruction) addInstruction(Constants.IFICMPLT);
    }

    /**
     *  The <code>ificmpge</code> opcode.
     */
    public IfInstruction ificmpge() {
        return (IfInstruction) addInstruction(Constants.IFICMPGE);
    }

    /**
     *  The <code>ificmpgt</code> opcode.
     */
    public IfInstruction ificmpgt() {
        return (IfInstruction) addInstruction(Constants.IFICMPGT);
    }

    /**
     *  The <code>ificmple</code> opcode.
     */
    public IfInstruction ificmple() {
        return (IfInstruction) addInstruction(Constants.IFICMPLE);
    }

    /**
     *  The <code>ifacmpeq</code> opcode.
     */
    public IfInstruction ifacmpeq() {
        return (IfInstruction) addInstruction(Constants.IFACMPEQ);
    }

    /**
     *  The <code>ifacmpne</code> opcode.
     */
    public IfInstruction ifacmpne() {
        return (IfInstruction) addInstruction(Constants.IFACMPNE);
    }

    /**
     *  The <code>ifnull</code> opcode.
     */
    public IfInstruction ifnull() {
        return (IfInstruction) addInstruction(Constants.IFNULL);
    }

    /**
     *  The <code>ifnonnull</code> opcode.
     */
    public IfInstruction ifnonnull() {
        return (IfInstruction) addInstruction(Constants.IFNONNULL);
    }

    /**
     *  The <code>go2</code> opcode.
     */
    public JumpInstruction go2() {
        return (JumpInstruction) addInstruction(Constants.GOTO);
    }

    /**
     *  The <code>jsr</code> opcode used in implementing <code>finally</code>
     *  clauses.
     */
    public JumpInstruction jsr() {
        return (JumpInstruction) addInstruction(Constants.JSR);
    }

    /**
     *  The <code>tableswitch</code> opcode.
     */
    public TableSwitchInstruction tableswitch() {
        return (TableSwitchInstruction) addInstruction(Constants.TABLESWITCH);
    }

    /**
     *  The <code>lookupswitch</code> opcode.
     */
    public LookupSwitchInstruction lookupswitch() {
        return (LookupSwitchInstruction) addInstruction(Constants.LOOKUPSWITCH);
    }

    /**
     *  Return from a method.  This method will result in a
     *  <code>nop</code> until its type is set.
     */
    public ReturnInstruction xreturn() {
        return (ReturnInstruction) addInstruction(new ReturnInstruction(this));
    }

    /**
     *  Return void from a method; the <code>return</code> opcode.
     */
    public ReturnInstruction vreturn() {
        return (ReturnInstruction) addInstruction(Constants.RETURN);
    }

    /**
     *  Return an int from a method; the <code>ireturn</code> opcode.
     */
    public ReturnInstruction ireturn() {
        return (ReturnInstruction) addInstruction(Constants.IRETURN);
    }

    /**
     *  Return a long from a method; the <code>lreturn</code> opcode.
     */
    public ReturnInstruction lreturn() {
        return (ReturnInstruction) addInstruction(Constants.LRETURN);
    }

    /**
     *  Return a float from a method; the <code>freturn</code> opcode.
     */
    public ReturnInstruction freturn() {
        return (ReturnInstruction) addInstruction(Constants.FRETURN);
    }

    /**
     *  Return a double from a method; the <code>dreturn</code> opcode.
     */
    public ReturnInstruction dreturn() {
        return (ReturnInstruction) addInstruction(Constants.DRETURN);
    }

    /**
     *  Return an object from a method; the <code>areturn</code> opcode.
     */
    public ReturnInstruction areturn() {
        return (ReturnInstruction) addInstruction(Constants.ARETURN);
    }

    /**
     *  Load the value from a field onto the stack; the <code>getfield</code>
     *  opcode.
     */
    public GetFieldInstruction getfield() {
        return (GetFieldInstruction) addInstruction(Constants.GETFIELD);
    }

    /**
     *  Load the value from a static field onto the stack; the
     *  <code>getstatic</code> opcode.
     */
    public GetFieldInstruction getstatic() {
        return (GetFieldInstruction) addInstruction(Constants.GETSTATIC);
    }

    /**
     *  Place the value of a field onto the stack; the <code>putfield</code>
     *  opcode.
     */
    public PutFieldInstruction putfield() {
        return (PutFieldInstruction) addInstruction(Constants.PUTFIELD);
    }

    /**
     *  Place the value of a static field onto the stack; the
     *  <code>putstatic</code> opcode.
     */
    public PutFieldInstruction putstatic() {
        return (PutFieldInstruction) addInstruction(Constants.PUTSTATIC);
    }

    /**
     *  Invoke a virtual method; the <code>invokevirtual</code> opcode.
     */
    public MethodInstruction invokevirtual() {
        return (MethodInstruction) addInstruction(Constants.INVOKEVIRTUAL);
    }

    /**
     *  Invoke a method non-virtually, as for constructors and superclass
     *  methods; the <code>invokespecial</code> opcode.
     */
    public MethodInstruction invokespecial() {
        return (MethodInstruction) addInstruction(Constants.INVOKESPECIAL);
    }

    /**
     *  Invoke a method on an interface; the <code>invokeinterface</code>
     *  opcode.
     */
    public MethodInstruction invokeinterface() {
        return (MethodInstruction) addInstruction(Constants.INVOKEINTERFACE);
    }

    /**
     *  Invoke a static method; the <code>invokestatic</code> opcode.
     */
    public MethodInstruction invokestatic() {
        return (MethodInstruction) addInstruction(Constants.INVOKESTATIC);
    }

    /**
     *  Create a new instance of an object; the <code>new</code> opcode.
     */
    public ClassInstruction anew() {
        return (ClassInstruction) addInstruction(Constants.NEW);
    }

    /**
     *  Create a new instance of an object array; the <code>anew</code> opcode.
     */
    public ClassInstruction anewarray() {
        return (ClassInstruction) addInstruction(Constants.ANEWARRAY);
    }

    /**
     *  Cast an object on the stack to another type; the <code>checkcast</code>
     *  opcode.
     */
    public ClassInstruction checkcast() {
        return (ClassInstruction) addInstruction(Constants.CHECKCAST);
    }

    /**
     *  Test if a stack object is an instance of a class; the
     *  <code>instanceof</code> opcode.
     */
    public ClassInstruction isinstance() {
        return (ClassInstruction) addInstruction(Constants.INSTANCEOF);
    }

    /**
     *  Create a new multidimensional array; the <code>multianewarray</code>
     *  opcode.
     */
    public MultiANewArrayInstruction multianewarray() {
        return (MultiANewArrayInstruction) addInstruction(Constants.MULTIANEWARRAY);
    }

    /**
     *  Create a new array of a primitive type; the <code>newarray</code>
     *  opcode.
     */
    public NewArrayInstruction newarray() {
        return (NewArrayInstruction) addInstruction(Constants.NEWARRAY);
    }

    /**
     *  Get the length of an array on the stack; the <code>arraylength</code>
     *  opcode.
     */
    public Instruction arraylength() {
        return addInstruction(Constants.ARRAYLENGTH);
    }

    /**
     *  Throw an exception; the <code>athrow</code> opcode.
     */
    public Instruction athrow() {
        return addInstruction(Constants.ATHROW);
    }

    /**
     *  The <code>monitorenter</code> opcode.
     */
    public MonitorEnterInstruction monitorenter() {
        return (MonitorEnterInstruction) addInstruction(Constants.MONITORENTER);
    }

    /**
     *  The <code>monitorexit</code> opcode.
     */
    public MonitorExitInstruction monitorexit() {
        return (MonitorExitInstruction) addInstruction(Constants.MONITOREXIT);
    }

    /////////////////////////
    // Wholisitic operations
    /////////////////////////

    /**
     *  Return all the Instructions of this method.
     */
    public Instruction[] getInstructions() {
        Instruction[] arr = new Instruction[_size];
        int i = 0;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next)
            arr[i++] = (Instruction) entry;

        return arr;
    }

    int getLength() {
        // covers maxStack, maxLocals, codeLength, exceptionTableLength,
        // attributeCount
        int length = 12;

        // add code
        try {
            length += toByteArray().length;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.toString());
        }

        // add exception reps; each is 8 bytes
        length += (8 * _handlers.size());

        // add all attribute lengths
        Attribute[] attrs = getAttributes();

        for (int i = 0; i < attrs.length; i++)
            length += (attrs[i].getLength() + 6);

        return length;
    }

    public void acceptVisit(BCVisitor visit) {
        visit.enterCode(this);

        Instruction ins;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next) {
            ins = (Instruction) entry;
            visit.enterInstruction(ins);
            ins.acceptVisit(visit);
            visit.exitInstruction(ins);
        }

        for (Iterator i = _handlers.iterator(); i.hasNext();)
            ((ExceptionHandler) i.next()).acceptVisit(visit);

        visitAttributes(visit);

        visit.exitCode(this);
    }

    //////////////////////////
    // Convenience operations
    //////////////////////////

    /**
     *  Return line number information for the code.
     *  Acts internally through the {@link Attributes} interface.
     *
     *  @param add                if true, a new line number table will be added
     *                                  if not already present
     *  @return the line number information, or null if none
     *                                  and the <code>add</code> param is set to false
     */
    public LineNumberTable getLineNumberTable(boolean add) {
        LineNumberTable attr = (LineNumberTable) getAttribute(Constants.ATTR_LINENUMBERS);

        if (!add || (attr != null)) {
            return attr;
        }

        return (LineNumberTable) addAttribute(Constants.ATTR_LINENUMBERS);
    }

    /**
     *  Remove the line number table for the code.
     *  Acts internally through the {@link Attributes} interface.
     *
     *  @return true if there was a table to remove
     */
    public boolean removeLineNumberTable() {
        return removeAttribute(Constants.ATTR_LINENUMBERS);
    }

    /**
     *  Return local variable information for the code.
     *  Acts internally through the {@link Attributes} interface.
     *
     *  @param add                if true, a new local variable table will be
     *                                  added if not already present
     *  @return the local variable information, or null if none
     *                                  and the <code>add</code> param is set to false
     */
    public LocalVariableTable getLocalVariableTable(boolean add) {
        LocalVariableTable attr = (LocalVariableTable) getAttribute(Constants.ATTR_LOCALS);

        if (!add || (attr != null)) {
            return attr;
        }

        return (LocalVariableTable) addAttribute(Constants.ATTR_LOCALS);
    }

    /**
     *  Remove the local variable table for the code.
     *  Acts internally through the {@link Attributes} interface.
     *
     *  @return true if there was a table to remove
     */
    public boolean removeLocalVariableTables() {
        return removeAttribute(Constants.ATTR_LOCALS);
    }

    /**
     *  Return local variable generics information for the code.
     *  Acts internally through the {@link Attributes} interface.
     *
     *  @param add                if true, a new local variable type table will be
     *                                  added if not already present
     *  @return the local variable type information, or null if none
     *                                  and the <code>add</code> param is set to false
     */
    public LocalVariableTypeTable getLocalVariableTypeTable(boolean add) {
        LocalVariableTypeTable attr = (LocalVariableTypeTable) getAttribute(Constants.ATTR_LOCAL_TYPES);

        if (!add || (attr != null)) {
            return attr;
        }

        return (LocalVariableTypeTable) addAttribute(Constants.ATTR_LOCAL_TYPES);
    }

    /**
     *  Remove the local variable type table for the code.
     *  Acts internally through the {@link Attributes} interface.
     *
     *  @return true if there was a table to remove
     */
    public boolean removeLocalVariableTypeTables() {
        return removeAttribute(Constants.ATTR_LOCAL_TYPES);
    }

    //////////////////
    // I/O operations
    //////////////////
    void read(Attribute attr) {
        Code orig = (Code) attr;

        _maxStack = orig.getMaxStack();
        _maxLocals = orig.getMaxLocals();

        // clear existing code
        _head.next = _tail;
        _tail.prev = _head;
        _size = 0;
        beforeFirst();
        _handlers.clear();

        // copy all instructions; don't set constant instruction values until
        // instruction ptrs have been updated in case the instruction width
        // changes because of differences in the constant pool (LDC vs LDCW)
        Instruction ins;
        Instruction origIns;

        for (CodeEntry entry = orig._head.next; entry != orig._tail;
                entry = entry.next) {
            origIns = (Instruction) entry;
            ins = addInstruction(origIns.getOpcode());

            if (!(ins instanceof ConstantInstruction)) {
                ins.read(origIns);
            }
        }

        // copy exception handlers
        ExceptionHandler[] origHandlers = orig.getExceptionHandlers();
        ExceptionHandler handler;

        for (int i = 0; i < origHandlers.length; i++) {
            handler = addExceptionHandler();
            handler.read(origHandlers[i]);
            handler.updateTargets();
        }

        // reset all opcode ptrs to the new copied opcodes
        updateInstructionPointers();
        setAttributes(orig.getAttributes());

        // setup local variable markers
        LocalVariableTable locals = getLocalVariableTable(false);

        if (locals != null) {
            locals.updateTargets();
        }

        // setup local variable markers
        LocalVariableTypeTable localTypes = getLocalVariableTypeTable(false);

        if (localTypes != null) {
            localTypes.updateTargets();
        }

        // setup line number markers
        LineNumberTable lines = getLineNumberTable(false);

        if (lines != null) {
            lines.updateTargets();
        }

        // now copy constant instruction values
        CodeEntry copy = _head.next;

        for (CodeEntry entry = orig._head.next; entry != orig._tail;
                entry = entry.next, copy = copy.next) {
            if (entry instanceof ConstantInstruction) {
                ((ConstantInstruction) copy).read((Instruction) entry);
            }
        }

        beforeFirst();
    }

    void read(DataInput in, int length) throws IOException {
        _maxStack = in.readUnsignedShort();
        _maxLocals = in.readUnsignedShort();

        readCode(in, in.readInt());

        _handlers.clear();

        int exceptionCount = in.readUnsignedShort();
        ExceptionHandler excep;

        for (int i = 0; i < exceptionCount; i++) {
            excep = addExceptionHandler();
            excep.read(in);
            excep.updateTargets();
        }

        readAttributes(in);

        // setup local variable markers
        LocalVariableTable locals = getLocalVariableTable(false);

        if (locals != null) {
            locals.updateTargets();
        }

        // setup local variable markers
        LocalVariableTypeTable localTypes = getLocalVariableTypeTable(false);

        if (localTypes != null) {
            localTypes.updateTargets();
        }

        // setup line number markers
        LineNumberTable lines = getLineNumberTable(false);

        if (lines != null) {
            lines.updateTargets();
        }
    }

    void write(DataOutput out, int length) throws IOException {
        out.writeShort(_maxStack);
        out.writeShort(_maxLocals);

        byte[] code = toByteArray();
        out.writeInt(code.length);
        out.write(code);

        out.writeShort(_handlers.size());

        for (Iterator itr = _handlers.iterator(); itr.hasNext();)
            ((ExceptionHandler) itr.next()).write(out);

        writeAttributes(out);
    }

    private void readCode(DataInput in, int len) throws IOException {
        _head.next = _tail;
        _tail.prev = _head;
        _size = 0;
        beforeFirst();

        Instruction ins;

        for (int byteIndex = 0; byteIndex < len;) {
            ins = addInstruction(in.readUnsignedByte());
            ins.read(in);
            byteIndex += ins.getLength();
        }

        updateInstructionPointers();
        beforeFirst();
    }

    /**
     *  Ensures that all the opcode targets are set up correctly.
     */
    private void updateInstructionPointers() {
        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next)
            if (entry instanceof InstructionPtr) {
                ((InstructionPtr) entry).updateTargets();
            }
    }

    /**
     *  Returns the byteIndex of the given instruction.
     */
    int getByteIndex(Instruction ins) {
        int byteIndex = 0;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next) {
            if (entry == ins) {
                return byteIndex;
            }

            byteIndex += ((Instruction) entry).getLength();
        }

        throw new IllegalArgumentException("ins.owner != this");
    }

    /**
     *  Returns the instruction in this code block found at the given
     *  byte index.
     */
    Instruction getInstruction(int byteIndex) {
        if (byteIndex < 0) {
            return null;
        }

        int curIndex = 0;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next) {
            if (byteIndex == curIndex) {
                return (Instruction) entry;
            }

            curIndex += ((Instruction) entry).getLength();
        }

        throw new IllegalArgumentException(String.valueOf(byteIndex));
    }

    /**
     *  Returns the number of instructions that occur before 'ins'
     *  in this code block that 'ins' is a part of.
     *
     *  @throws IllegalArgumentException if this code block is not the owner
     *                  of ins
     */
    private int indexOf(Instruction ins) {
        int i = 0;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next, i++)
            if (entry == ins) {
                return i;
            }

        throw new IllegalArgumentException("ins.code != this");
    }

    private void writeCode(DataOutput out) throws IOException {
        Instruction ins;

        for (CodeEntry entry = _head.next; entry != _tail;
                entry = entry.next) {
            ins = (Instruction) entry;
            out.writeByte(ins.getOpcode());
            ins.write(out);
        }
    }

    private byte[] toByteArray() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);

        try {
            writeCode(stream);

            return byteStream.toByteArray();
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    private void fromByteArray(byte[] code) throws IOException {
        if (code == null) {
            _head.next = _tail;
            _tail.prev = _head;
            _size = 0;
        } else {
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                        code));

            try {
                readCode(stream, code.length);
            } finally {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private Instruction addInstruction(Instruction ins) {
        _ci.add(ins);

        return ins;
    }

    private Instruction addInstruction(int opcode) {
        return addInstruction(createInstruction(opcode));
    }

    /**
     *  Creates an Instruction, with this code block as the owner.
     *  Note that the Instruction is not added to this Code block.
     */
    private Instruction createInstruction(int opcode) {
        switch (opcode) {
        case Constants.NOP:
        case Constants.ARRAYLENGTH:
        case Constants.ATHROW:
            return new Instruction(this, opcode);

        case Constants.ACONSTNULL:
        case Constants.ICONSTM1:
        case Constants.ICONST0:
        case Constants.ICONST1:
        case Constants.ICONST2:
        case Constants.ICONST3:
        case Constants.ICONST4:
        case Constants.ICONST5:
        case Constants.LCONST0:
        case Constants.LCONST1:
        case Constants.FCONST0:
        case Constants.FCONST1:
        case Constants.FCONST2:
        case Constants.DCONST0:
        case Constants.DCONST1:
        case Constants.BIPUSH:
        case Constants.SIPUSH:
        case Constants.LDC:
        case Constants.LDCW:
        case Constants.LDC2W:
            return new ConstantInstruction(this, opcode);

        case Constants.ILOAD:
        case Constants.LLOAD:
        case Constants.FLOAD:
        case Constants.DLOAD:
        case Constants.ALOAD:
        case Constants.ILOAD0:
        case Constants.ILOAD1:
        case Constants.ILOAD2:
        case Constants.ILOAD3:
        case Constants.LLOAD0:
        case Constants.LLOAD1:
        case Constants.LLOAD2:
        case Constants.LLOAD3:
        case Constants.FLOAD0:
        case Constants.FLOAD1:
        case Constants.FLOAD2:
        case Constants.FLOAD3:
        case Constants.DLOAD0:
        case Constants.DLOAD1:
        case Constants.DLOAD2:
        case Constants.DLOAD3:
        case Constants.ALOAD0:
        case Constants.ALOAD1:
        case Constants.ALOAD2:
        case Constants.ALOAD3:
            return new LoadInstruction(this, opcode);

        case Constants.IALOAD:
        case Constants.LALOAD:
        case Constants.FALOAD:
        case Constants.DALOAD:
        case Constants.AALOAD:
        case Constants.BALOAD:
        case Constants.CALOAD:
        case Constants.SALOAD:
            return new ArrayLoadInstruction(this, opcode);

        case Constants.ISTORE:
        case Constants.LSTORE:
        case Constants.FSTORE:
        case Constants.DSTORE:
        case Constants.ASTORE:
        case Constants.ISTORE0:
        case Constants.ISTORE1:
        case Constants.ISTORE2:
        case Constants.ISTORE3:
        case Constants.LSTORE0:
        case Constants.LSTORE1:
        case Constants.LSTORE2:
        case Constants.LSTORE3:
        case Constants.FSTORE0:
        case Constants.FSTORE1:
        case Constants.FSTORE2:
        case Constants.FSTORE3:
        case Constants.DSTORE0:
        case Constants.DSTORE1:
        case Constants.DSTORE2:
        case Constants.DSTORE3:
        case Constants.ASTORE0:
        case Constants.ASTORE1:
        case Constants.ASTORE2:
        case Constants.ASTORE3:
            return new StoreInstruction(this, opcode);

        case Constants.IASTORE:
        case Constants.LASTORE:
        case Constants.FASTORE:
        case Constants.DASTORE:
        case Constants.AASTORE:
        case Constants.BASTORE:
        case Constants.CASTORE:
        case Constants.SASTORE:
            return new ArrayStoreInstruction(this, opcode);

        case Constants.POP:
        case Constants.POP2:
        case Constants.DUP:
        case Constants.DUPX1:
        case Constants.DUPX2:
        case Constants.DUP2:
        case Constants.DUP2X1:
        case Constants.DUP2X2:
        case Constants.SWAP:
            return new StackInstruction(this, opcode);

        case Constants.IADD:
        case Constants.LADD:
        case Constants.FADD:
        case Constants.DADD:
        case Constants.ISUB:
        case Constants.LSUB:
        case Constants.FSUB:
        case Constants.DSUB:
        case Constants.IMUL:
        case Constants.LMUL:
        case Constants.FMUL:
        case Constants.DMUL:
        case Constants.IDIV:
        case Constants.LDIV:
        case Constants.FDIV:
        case Constants.DDIV:
        case Constants.IREM:
        case Constants.LREM:
        case Constants.FREM:
        case Constants.DREM:
        case Constants.INEG:
        case Constants.LNEG:
        case Constants.FNEG:
        case Constants.DNEG:
        case Constants.ISHL:
        case Constants.LSHL:
        case Constants.ISHR:
        case Constants.LSHR:
        case Constants.IUSHR:
        case Constants.LUSHR:
        case Constants.IAND:
        case Constants.LAND:
        case Constants.IOR:
        case Constants.LOR:
        case Constants.IXOR:
        case Constants.LXOR:
            return new MathInstruction(this, opcode);

        case Constants.IINC:
            return new IIncInstruction(this);

        case Constants.I2L:
        case Constants.I2F:
        case Constants.I2D:
        case Constants.L2I:
        case Constants.L2F:
        case Constants.L2D:
        case Constants.F2I:
        case Constants.F2L:
        case Constants.F2D:
        case Constants.D2I:
        case Constants.D2L:
        case Constants.D2F:
        case Constants.I2B:
        case Constants.I2C:
        case Constants.I2S:
            return new ConvertInstruction(this, opcode);

        case Constants.LCMP:
        case Constants.FCMPL:
        case Constants.FCMPG:
        case Constants.DCMPL:
        case Constants.DCMPG:
            return new CmpInstruction(this, opcode);

        case Constants.IFEQ:
        case Constants.IFNE:
        case Constants.IFLT:
        case Constants.IFGE:
        case Constants.IFGT:
        case Constants.IFLE:
        case Constants.IFICMPEQ:
        case Constants.IFICMPNE:
        case Constants.IFICMPLT:
        case Constants.IFICMPGE:
        case Constants.IFICMPGT:
        case Constants.IFICMPLE:
        case Constants.IFACMPEQ:
        case Constants.IFACMPNE:
        case Constants.IFNULL:
        case Constants.IFNONNULL:
            return new IfInstruction(this, opcode);

        case Constants.GOTO:
        case Constants.JSR:
        case Constants.GOTOW:
        case Constants.JSRW:
            return new JumpInstruction(this, opcode);

        case Constants.RET:
            return new RetInstruction(this);

        case Constants.TABLESWITCH:
            return new TableSwitchInstruction(this);

        case Constants.LOOKUPSWITCH:
            return new LookupSwitchInstruction(this);

        case Constants.IRETURN:
        case Constants.LRETURN:
        case Constants.FRETURN:
        case Constants.DRETURN:
        case Constants.ARETURN:
        case Constants.RETURN:
            return new ReturnInstruction(this, opcode);

        case Constants.GETSTATIC:
        case Constants.GETFIELD:
            return new GetFieldInstruction(this, opcode);

        case Constants.PUTSTATIC:
        case Constants.PUTFIELD:
            return new PutFieldInstruction(this, opcode);

        case Constants.INVOKEVIRTUAL:
        case Constants.INVOKESPECIAL:
        case Constants.INVOKESTATIC:
        case Constants.INVOKEINTERFACE:
            return new MethodInstruction(this, opcode);

        case Constants.NEW:
        case Constants.ANEWARRAY:
        case Constants.CHECKCAST:
        case Constants.INSTANCEOF:
            return new ClassInstruction(this, opcode);

        case Constants.NEWARRAY:
            return new NewArrayInstruction(this);

        case Constants.MONITORENTER:
            return new MonitorEnterInstruction(this);

        case Constants.MONITOREXIT:
            return new MonitorExitInstruction(this);

        case Constants.WIDE:
            return new WideInstruction(this);

        case Constants.MULTIANEWARRAY:
            return new MultiANewArrayInstruction(this);

        default:
            throw new IllegalArgumentException("Illegal opcode: " + opcode);
        }
    }

    /**
     *  Returns another listIterator view of the Instructions in this
     *  code block.  Useful for performing read-only searches through
     *  Instructions without effecting the pointer location of the main
     *  code block.
     */
    public ListIterator listIterator() {
        return new CodeIterator(_head, -1);
    }

    /**
     *  Helper class to handle invalidation of instructions on removal
     *  and notification of modification on addition.
     */
    private class CodeIterator implements ListIterator {
        public static final int UNSET = -99;
        private CodeEntry _bn = null; // "before next" entry
        private Instruction _last = null; // last entry returned
        private int _index = UNSET; // index of _bn

        public CodeIterator(CodeEntry entry, int index) {
            _bn = entry;
            _index = index;
        }

        public boolean hasNext() {
            return _bn.next != _tail;
        }

        public boolean hasPrevious() {
            return _bn != _head;
        }

        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            _bn = _bn.next;
            _last = (Instruction) _bn;

            if (_index != UNSET) {
                _index++;
            }

            return _last;
        }

        public int nextIndex() {
            return initIndex() + 1;
        }

        public Object previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }

            _last = (Instruction) _bn;
            _bn = _bn.prev;

            if (_index != UNSET) {
                _index--;
            }

            return _last;
        }

        public int previousIndex() {
            return initIndex();
        }

        private int initIndex() {
            if (_index == UNSET) {
                if (_bn == _head) {
                    _index = -1;
                } else {
                    _index = indexOf((Instruction) _bn);
                }
            }

            return _index;
        }

        public void add(Object obj) {
            if (obj == null) {
                throw new NullPointerException("obj = null");
            }

            Instruction ins = (Instruction) obj;

            if (_size == 0) {
                _head.next = ins;
                _tail.prev = ins;
                ins.prev = _head;
                ins.next = _tail;
                _index = 0;
            } else {
                CodeEntry next = _bn.next;
                _bn.next = ins;
                next.prev = ins;
                ins.prev = _bn;
                ins.next = next;

                if (_index != UNSET) {
                    _index++;
                }
            }

            _bn = ins;
            _last = ins;
            _size++;
        }

        public void set(Object obj) {
            if (obj == null) {
                throw new NullPointerException("obj = null");
            }

            if (_last == null) {
                throw new IllegalStateException();
            }

            Instruction ins = (Instruction) obj;
            ins.prev = _last.prev;
            ins.next = _last.next;
            ins.prev.next = ins;
            ins.next.prev = ins;

            replaceTarget(_last, ins);
            _last.invalidate();

            if (_bn == _last) {
                _bn = ins;
            }

            _last = ins;
        }

        public void remove() {
            if (_last == null) {
                throw new IllegalStateException();
            }

            if (_bn == _last) {
                _bn = _last.prev;
            }

            _index--;

            _last.prev.next = _last.next;
            _last.next.prev = _last.prev;
            _size--;

            Instruction orig = _last;
            Instruction replace = null;

            if (orig.next != _tail) {
                replace = (Instruction) orig.next;
            } else {
                replace = nop();
            }

            replaceTarget(orig, replace);

            orig.invalidate();
            _last = null;
        }

        private void replaceTarget(Instruction orig, Instruction replace) {
            for (CodeEntry entry = _head.next; entry != _tail;
                    entry = entry.next)
                if (entry instanceof InstructionPtr) {
                    ((InstructionPtr) entry).replaceTarget(orig, replace);
                }

            // update the ExceptionHandler pointers
            ExceptionHandler[] handlers = getExceptionHandlers();

            for (int i = 0; i < handlers.length; i++)
                handlers[i].replaceTarget(orig, replace);

            // update LineNumber pointers
            LineNumberTable lineNumbers = getLineNumberTable(false);

            if (lineNumbers != null) {
                lineNumbers.replaceTarget(orig, replace);
            }

            // update LocalVariable pointers
            LocalVariableTable variables = getLocalVariableTable(false);

            if (variables != null) {
                variables.replaceTarget(orig, replace);
            }

            // update LocalVariableType pointers
            LocalVariableTypeTable types = getLocalVariableTypeTable(false);

            if (types != null) {
                types.replaceTarget(orig, replace);
            }
        }
    }
}
