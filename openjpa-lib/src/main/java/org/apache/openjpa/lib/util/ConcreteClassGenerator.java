/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.AccessController;

import serp.bytecode.BCClass;
import serp.bytecode.BCClassLoader;
import serp.bytecode.BCMethod;
import serp.bytecode.Code;
import serp.bytecode.Project;

/**
 * Dynamically generates concrete implementations of abstract classes.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 * @since 0.9.8
 */
public class ConcreteClassGenerator {

    /** 
     *  Takes an abstract class and returns a concrete implementation. Note
     *  that it doesn't actually implement any abstract methods, it
     *  merely makes an abstract class loadable. Abstract methods will
     *  throw a {@link AbstractMethodError}.
     *  
     *  @param  abstractClass  the abstract class
     *  @return a concrete class
     */
    public static <T> Class<T> makeConcrete(Class<T> abstractClass)
        throws ClassNotFoundException {
        if (abstractClass == null)
            return null;

        if (!Modifier.isAbstract(abstractClass.getModifiers()))
            return abstractClass;

        Project project = new Project();
        BCClassLoader loader = AccessController.doPrivileged(J2DoPrivHelper
            .newBCClassLoaderAction(project, abstractClass.getClassLoader()));

        String name = abstractClass.getName()+"_";
        BCClass bc = AccessController.doPrivileged(J2DoPrivHelper.
            loadProjectClassAction(project, name));
        
        bc.setSuperclass(abstractClass);

        Constructor[] constructors = abstractClass.getConstructors();
        if (constructors == null || constructors.length == 0) {
            bc.addDefaultConstructor().makePublic();
        } else {
            for (int i = 0; i < constructors.length; i++) {
                Constructor con = constructors[i];
                Class[] args = con.getParameterTypes();

                BCMethod bccon = bc.declareMethod("<init>", void.class, args);
                Code code = bccon.getCode(true);

                code.xload().setThis();

                for (int j = 0; j < args.length; j++) {
                    code.aload().setParam(j);
                    code.checkcast().setType(args[j]);
                }

                code.invokespecial().setMethod(abstractClass,
                    "<init>", void.class, args);
                code.vreturn();

                code.calculateMaxStack();
                code.calculateMaxLocals();
            }
        }

        Class cls = Class.forName(bc.getName(), false, loader);
        return cls;
    }

    /** 
     *  Utility method for safely invoking a constructor that we do
     *  not expect to throw exceptions. 
     *  
     *  @param  c          the class to construct
     *  @param  paramTypes the types of the parameters
     *  @param  params     the parameter values
     *  @return            the new instance
     */
    public static <T> T newInstance(Class<T> c, Class[] paramTypes,
        Object[] params) {
        try {
            return c.getConstructor(paramTypes).newInstance(params);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** 
     *  @see #newInstance(java.lang.Class,java.lang.Class[],java.lang.Object[])
     */
    public static <T> T newInstance(Class<T> c) {
        return newInstance(c, new Class[] { }, new Object[] { });
    }

    /** 
     *  @see #newInstance(java.lang.Class,java.lang.Class[],java.lang.Object[])
     */
    public static <T> T newInstance(Class<T> c, Class paramType, Object param) {
        return newInstance(c,
            new Class[] { paramType },
            new Object[] { param });
    }

    /** 
     *  @see #newInstance(java.lang.Class,java.lang.Class[],java.lang.Object[])
     */
    public static <T> T newInstance(Class<T> c, Class paramType1, Object param1,
        Class paramType2, Object param2) {
        return newInstance(c,
            new Class[] { paramType1, paramType2 },
            new Object[] { param1, param2 });
    }

    /** 
     *  @see #newInstance(java.lang.Class,java.lang.Class[],java.lang.Object[])
     */
    public static <T> T newInstance(Class<T> c, Class paramType1, Object param1,
        Class paramType2, Object param2, Class paramType3, Object param3) {
        return newInstance(c,
            new Class[] { paramType1, paramType2, paramType3 },
            new Object[] { param1, param2, param3 });
    }

    /** 
     *  @see #newInstance(java.lang.Class,java.lang.Class[],java.lang.Object[])
     */
    public static <T> T newInstance(Class<T> c, Class paramType1, Object param1,
        Class paramType2, Object param2, Class paramType3, Object param3,
        Class paramType4, Object param4) {
        return newInstance(c,
            new Class[] { paramType1, paramType2, paramType3, paramType4 },
            new Object[] { param1, param2, param3, param4 });
    }
}

