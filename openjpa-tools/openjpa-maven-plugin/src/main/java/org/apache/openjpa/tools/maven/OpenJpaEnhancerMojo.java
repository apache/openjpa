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
package org.apache.openjpa.tools.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Processes Application model classes and enhances them by running OpenJPA
 * Enhancer tool.
 *
 * @version $Id: OpenJpaEnhancerMojo.java 10954 2009-10-23 22:05:45Z struberg $
 * @since 1.0
 */
@Mojo(name="enhance", defaultPhase=LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution=ResolutionScope.COMPILE,
    threadSafe = true)
public class OpenJpaEnhancerMojo extends AbstractOpenJpaEnhancerMojo {
}
