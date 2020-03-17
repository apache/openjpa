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
package org.apache.openjpa.junit5;

import org.apache.openjpa.junit5.internal.OpenJPAExtension;
import org.apache.openjpa.lib.log.LogFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enables <b>in place</b> enhancement for entities listed or found by scanning in test classpath.
 * It enables to run tests from the IDE with enhancement.
 *
 * WARNING: if you use that for tests but don't want to enhance your entities, ensure to clean+recompile them
 *          after tests otherwise you will have entities depending on OpenJPA at bytecode level.
 */
@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(OpenJPAExtension.class)
public @interface OpenJPASupport {
    /**
     * @return the list of class names (don't use {@code MyEntity.class}) to enhance if {@code auto} is false.
     */
    String[] entities() default {};

    /**
     * @return if true, only directories in the classpath will be browsed to enhance entities,
     *         if false {@link OpenJPASupport#entities()} will be used.
     */
    boolean auto() default true;

    /**
     * @return the log factory to use, if not set slf4j will be tried and if it fails will fallback on the default one.
     */
    Class<?> logFactory() default LogFactory.class;
}
