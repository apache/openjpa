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

import java.io.File;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Processes Application model test classes and enhances them by running OpenJPA
 * Enhancer tool.
 *
 * @version $Id: OpenJpaTestEnhancerMojo.java 9137 2009-02-28 21:55:03Z struberg $
 * @since 1.1
 */
@Mojo(name="test-enhance", defaultPhase=LifecyclePhase.PROCESS_TEST_CLASSES,
      requiresDependencyResolution=ResolutionScope.TEST)
public class OpenJpaTestEnhancerMojo extends AbstractOpenJpaEnhancerMojo {

    /**
     * List of all class path elements that will be searched for the
     * <code>persistence-enabled</code> classes and resources expected by
     * PCEnhancer.
     */
    @Parameter(defaultValue="${project.testClasspathElements}", required=true, readonly=true)
    protected List<String> testClasspathElements;

    /**
     * This is where compiled test classes go.
     */
    @Parameter(defaultValue="${project.build.testOutputDirectory}", required=true, readonly=true)
    private File testClasses;

    /**
     * Use this flag to skip test enhancement. It will automatically be
     * set if maven got invoked with the -Dmaven.test.skip=true option
     * because no compiled test clases are available in this case.
     */
    @Parameter(defaultValue="${maven.test.skip}", readonly=true)
    private boolean skipTestEnhancement;

    /**
     * This function overloads {@code AbstractOpenJpaMojo#getClasspathElements()} to return the test
     * classpath elements.
     *
     * @return List of classpath elements for the test phase
     */
    protected List<String> getClasspathElements() {
        return testClasspathElements;
    }


    /**
     * The File where the class files of the entities to enhance reside
     *
     * @return normaly the test entity classes are located in target/test-classes
     */
    protected File getEntityClasses() {
        return testClasses;
    }

    protected boolean skipMojo() {
        boolean skip = super.skipMojo();

        // we also need to skip enhancing test classes if all
        // tests got skipped.
        skip |= skipTestEnhancement;

        return skip;
    }


}
