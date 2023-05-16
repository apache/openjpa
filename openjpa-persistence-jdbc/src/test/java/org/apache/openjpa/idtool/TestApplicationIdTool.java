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
package org.apache.openjpa.idtool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.openjpa.enhance.ApplicationIdTool;
import org.apache.openjpa.persistence.jdbc.common.apps.AutoIncrementPC3;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test for the ApplicationIdTool from openjpa-kernel.
 * We cannot test it over in the openjpa-kernel package as
 * we'd not be able to also test JPA features.
 */
public class TestApplicationIdTool {

    @Test
    public void testApplicationIdTool_wIdClassAnnotation() throws Exception{
        String entityJavaFile = "./src/test/java/" + AutoIncrementPC3.class.getName().replace(".", "/") + ".java";
        final String outputDir = "./target/idtooltest/";
        ApplicationIdTool.main(new String[]{"-s","Id", entityJavaFile, "-d", outputDir});

        String idJavaFile = outputDir + AutoIncrementPC3.class.getName().replace(".", "/") + "Id.java";

        assertTrue(new File(idJavaFile).exists());
    }

    @Test
    public void testApplicationIdTool_freshClass() throws Exception{
        String entityJavaFile = "./src/test/java/" + RecordsPerYear.class.getName().replace(".", "/") + ".java";
        final String outputDir = "./target/idtooltest/";
        ApplicationIdTool.main(new String[]{"-s","Id", entityJavaFile, "-d", outputDir});

        String idJavaFile = outputDir + RecordsPerYear.class.getName().replace(".", "/") + "Id.java";

        final File generatedIdFile = new File(idJavaFile);
        assertTrue(generatedIdFile.exists());
        assertContains(generatedIdFile, "public class RecordsPerYearId");
        assertContains(generatedIdFile, "public RecordsPerYearId(String str)");
    }

    private void assertContains(File file, String find) throws IOException {
        final byte[] bytes = Files.readAllBytes(file.toPath());
        assertTrue(new String(bytes).contains(find));
    }
}
