/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.enhance;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.JavaVersions;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;

/**
 * Redefines the method bodies of existing classes. Supports Java 5 VMs that
 * have a java agent installed on the command line as well as newer VMs without
 * any <code>-javaagent</code> flag.
 *
 * @since 1.0.0
 */
public class ClassRedefiner {

    private static final Localizer _loc = Localizer.forPackage(ClassRedefiner.class);

    private static Boolean _canRedefine = null;

    /**
     * For each element in <code>classes</code>, this method will redefine
     * all the element's methods such that field accesses are intercepted
     * in-line. If {@link #canRedefineClasses(Log)} returns <code>false</code>,
     * this method is a no-op.
     */
    public static void redefineClasses(OpenJPAConfiguration conf,
        final Map<Class<?>,byte[]> classes) {
        Log log = conf.getLog(OpenJPAConfiguration.LOG_ENHANCE);
        if (classes == null || classes.size() == 0 || !canRedefineClasses(log))
            return;

        Instrumentation inst = null;
        try {
            inst = InstrumentationFactory.getInstrumentation(log);
            log.trace(_loc.get("retransform-types", classes.keySet()));

            // Use redefineClasses instead of retransformClasses to
            // bypass any registered ClassFileTransformers (e.g.
            // PCClassFileTransformer) that might produce incompatible
            // bytecode for classes with non-standard field types.
            java.lang.instrument.ClassDefinition[] defs =
                new java.lang.instrument.ClassDefinition[classes.size()];
            int i = 0;
            for (Map.Entry<Class<?>, byte[]> entry : classes.entrySet()) {
                defs[i++] = new java.lang.instrument.ClassDefinition(
                    entry.getKey(), entry.getValue());
            }
            inst.redefineClasses(defs);
        } catch (UnsupportedOperationException e) {
            throw new InternalException(e);
        } catch (Throwable e) {
            // redefineClasses may fail with VerifyError (an Error,
            // not Exception) when the enhanced bytecode references
            // types not visible to the class's loader, or when the
            // redefined schema doesn't match. Fall back to
            // subclass-based enhancement which is already set up
            // by ManagedClassSubclasser.
            if (log.isInfoEnabled())
                log.info("redefineClasses failed (" + e
                    + "), falling back to subclass enhancement");
        }
    }

    /**
     * @return whether or not this VM has an instrumentation installed that
     * permits redefinition of classes. This assumes that all the arguments
     * will be modifiable classes according to
     * {@link java.lang.instrument.Instrumentation#isModifiableClass}, and
     * only checks whether or not an instrumentation is available and
     * if retransformation is possible.
     */
    public static boolean canRedefineClasses(Log log) {
        if (_canRedefine == null) {
            try {
                Instrumentation inst = InstrumentationFactory.getInstrumentation(log);
                if (inst == null) {
                    _canRedefine = Boolean.FALSE;
                } else if (JavaVersions.VERSION == 5) {
                    // if instrumentation is non-null and we're using Java 5,
                    // isRetransformClassesSupported isn't available,
                    // so we use the more basic class redefinition instead.
                    _canRedefine = Boolean.TRUE;
                } else {
                    _canRedefine = (Boolean) Instrumentation.class.getMethod(
                        "isRetransformClassesSupported").invoke(inst);
                }
            } catch (Exception e) {
                _canRedefine = Boolean.FALSE;
            }
        }
        return _canRedefine;
    }
}
