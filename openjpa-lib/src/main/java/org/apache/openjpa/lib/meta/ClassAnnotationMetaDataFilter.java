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
package org.apache.openjpa.lib.meta;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.xbean.asm9.AnnotationVisitor;
import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassVisitor;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;


/**
 * Filter that looks for classes with one of a set of annotations.
 * See JDK 1.5 JVM spec for details on annotation bytecode:<br />
 * java.sun.com/docs/books/vmspec/2nd-edition/ClassFileFormat-final-draft.pdf
 *
 * @author Abe White
 */
public class ClassAnnotationMetaDataFilter implements MetaDataFilter {

    private final Set<String> _annos;

    private static final Localizer _loc = Localizer.forPackage(ClassAnnotationMetaDataFilter.class);
    private Log _log = null;

    /**
     * Constructor; supply annotation to match against.
     */
    public ClassAnnotationMetaDataFilter(Class<?> anno) {
        this(new Class[]{ anno });
    }

    /**
     * Constructor; supply annotations to match against.
     */
    public ClassAnnotationMetaDataFilter(Class<?>[] annos) {
        _annos = new HashSet<>();
        for (Class<?> anno : annos) {
            _annos.add(Type.getDescriptor(anno));
        }
    }

    @Override
    public boolean matches(Resource rsrc) throws IOException {
        if (_annos.isEmpty() || !rsrc.getName().endsWith(".class")) {
            return false;
        }

        ClassReader cr = new ClassReader(rsrc.getContent());
        final MatchAnnotationScanner classVisitor = new MatchAnnotationScanner(Opcodes.ASM9);
        cr.accept(classVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return classVisitor.matches;
    }



    public Log getLog() {
        return _log;
    }

    public void setLog(Log _log) {
        this._log = _log;
    }

    public class MatchAnnotationScanner extends ClassVisitor {
        boolean matches = false;

        public MatchAnnotationScanner(int api) {
            super(api);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (visible && _annos.contains(descriptor)) {
                matches = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }
    }
}
