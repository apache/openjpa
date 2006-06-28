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

import java.util.*;


/**
 *  <p>Pseudo-instruction used to place {@link Class} objects onto the stack.
 *  This logical instruction may actually involve a large chunk of code, and
 *  may even add static synthetic fields and methods to the owning class.
 *  Therefore, once the type of class being loaded is set, it cannot
 *  be changed.  Also, this instruction is invalid as the target of
 *  any jump instruction or exception handler.</p>
 *
 *  @author Abe White
 */
public class ClassConstantInstruction {
    private static final Class[] _params = new Class[] { String.class };
    private static final Map _wrappers = new HashMap();

    static {
        _wrappers.put(byte.class.getName(), Byte.class);
        _wrappers.put(boolean.class.getName(), Boolean.class);
        _wrappers.put(char.class.getName(), Character.class);
        _wrappers.put(double.class.getName(), Double.class);
        _wrappers.put(float.class.getName(), Float.class);
        _wrappers.put(int.class.getName(), Integer.class);
        _wrappers.put(long.class.getName(), Long.class);
        _wrappers.put(short.class.getName(), Short.class);
    }

    private Instruction _ins = null;
    private Code _code = null;
    private BCClass _class = null;
    private boolean _invalid = false;

    ClassConstantInstruction(BCClass bc, Code code, Instruction nop) {
        _class = bc;
        _code = code;
        _ins = nop;
    }

    /**
     *  Set the type of class being loaded.
     *
     *  @return the first Instruction of the block added by setting
     *                          the type
     *  @throws IllegalStateException if type has already been set
     */
    public Instruction setClass(String name) {
        name = _class.getProject().getNameCache().getExternalForm(name, false);
        setClassName(name, getWrapperClass(name));

        return _ins;
    }

    /**
     *  Set the type of class being loaded.
     *
     *  @return the first Instruction of the block added by setting
     *                          the type
     *  @throws IllegalStateException if type has already been set
     */
    public Instruction setClass(Class type) {
        return setClass(type.getName());
    }

    /**
     *  Set the type of class being loaded.
     *
     *  @return the first Instruction of the block added by setting
     *                          the type
     *  @throws IllegalStateException if type has already been set
     */
    public Instruction setClass(BCClass type) {
        return setClass(type.getName());
    }

    /**
     *  Set the name of the class to load.
     */
    private void setClassName(String name, Class wrapper) {
        if (_invalid) {
            throw new IllegalStateException();
        }

        // remember the position of the code iterator
        Instruction before = (_code.hasNext()) ? _code.next() : null;
        _code.before(_ins);
        _code.next();

        if (wrapper != null) {
            _code.getstatic().setField(wrapper, "TYPE", Class.class);
        } else {
            setObject(name);
        }

        // move to the old position
        if (before != null) {
            _code.before(before);
        } else {
            _code.afterLast();
        }

        _invalid = true;
    }

    /**
     *  Adds fields and methods as necessary to load a class constant of
     *  an object type.
     */
    private void setObject(String name) {
        BCField field = addClassField(name);
        BCMethod method = addClassLoadMethod();

        // copied from the way jikes loads classes
        _code.getstatic().setField(field);

        JumpInstruction ifnull = _code.ifnull();

        _code.getstatic().setField(field);

        JumpInstruction go2 = _code.go2();

        ifnull.setTarget(_code.constant().setValue(name));
        _code.invokestatic().setMethod(method);
        _code.dup();
        _code.putstatic().setField(field);

        go2.setTarget(_code.nop());
    }

    /**
     *  Adds a static field to hold the loaded class constant.
     */
    private BCField addClassField(String name) {
        String fieldName = "class$L" +
            name.replace('.', '$').replace('[', '$').replace(';', '$');

        BCField field = _class.getDeclaredField(fieldName);

        if (field == null) {
            field = _class.declareField(fieldName, Class.class);
            field.makePackage();
            field.setStatic(true);
            field.setSynthetic(true);
        }

        return field;
    }

    /**
     *  Adds the standard <code>class$<code> method used inernally by classes
     *  to load class constants for object types.
     */
    private BCMethod addClassLoadMethod() {
        BCMethod method = _class.getDeclaredMethod("class$", _params);

        if (method != null) {
            return method;
        }

        // add the special synthetic method
        method = _class.declareMethod("class$", Class.class, _params);
        method.setStatic(true);
        method.makePackage();
        method.setSynthetic(true);

        // copied directly from the output of the jikes compiler
        Code code = method.getCode(true);
        code.setMaxStack(3);
        code.setMaxLocals(2);

        Instruction tryStart = code.aload().setLocal(0);
        code.invokestatic()
            .setMethod(Class.class, "forName", Class.class, _params);

        Instruction tryEnd = code.areturn();
        Instruction handlerStart = code.astore().setLocal(1);
        code.anew().setType(NoClassDefFoundError.class);
        code.dup();
        code.aload().setLocal(1);
        code.invokevirtual()
            .setMethod(Throwable.class, "getMessage", String.class, null);
        code.invokespecial()
            .setMethod(NoClassDefFoundError.class, "<init>", void.class, _params);
        code.athrow();

        code.addExceptionHandler(tryStart, tryEnd, handlerStart,
            ClassNotFoundException.class);

        return method;
    }

    /**
     *  Return the wrapper type for the given primitive class, or null
     *  if the given name is not a primitive type.  The given name should
     *  be in external form.
     */
    private static Class getWrapperClass(String name) {
        if (name == null) {
            return null;
        }

        return (Class) _wrappers.get(name);
    }
}
