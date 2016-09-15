/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.serp;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import serp.bytecode.BCClass;
import serp.bytecode.Project;

/**
 * Test for reproducing the Serp bug with loading some classes under Java9
 */
public class SerpBcTEst {

    @Test
    public void testBcGetInterfaceNames() {
        Project project = new Project();

        InputStream in = DummyClass.class.getClassLoader()
                .getResourceAsStream("org/apache/openjpa/serp/SerpBcTEst$DummyClass.class");
        BCClass bcClass = project.loadClass(in);

        String [] interfaces = bcClass.getInterfaceNames();
        Assert.assertNotNull(interfaces);
    }

    static class DummyClass {
    }
}
