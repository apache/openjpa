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
package org.apache.openjpa.persistence.enhance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.openjpa.enhance.PCEnhancerAgent;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.lib.log.AbstractLog;
import org.junit.jupiter.api.Test;

/**
 * Tests that the class enhancer is attached to the running JVM through the
 * Attach API and enhances an entity that was not enhanced at build time when
 * the entity class is loaded.
 *
 * Self-attach needs -Djdk.attach.allowAttachSelf=true, so the test runs in
 * a child JVM and the test JVM is not changed.
 */
class TestDynamicEnhancementAgent {

    @Test
    void entityEnhancedByDynamicAgent() throws IOException, InterruptedException {
        assumeTrue(ModuleLayer.boot().findModule("jdk.attach").isPresent());

        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-Djdk.attach.allowAttachSelf=true");
        cmd.add("-XX:+EnableDynamicAgentLoading");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(AgentMain.class.getName());

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        assertTrue(p.waitFor(120, TimeUnit.SECONDS), "child JVM did not finish");
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals( 0, p.exitValue(), out);
        assertTrue(out.contains(AgentMain.SUCCESS), out);
    }

    /**
     * Entry point of the child JVM started by
     * {@link #testEntityEnhancedByDynamicAgent()}. It loads the agent the same
     * way as the persistence provider does when an EntityManagerFactory is
     * created, then loads the entity class.
     */
    public static class AgentMain {
        static final String SUCCESS = "entity enhanced";

        public static void main(String[] args) throws Exception {
            CapturingLog log = new CapturingLog();
            if (!PCEnhancerAgent.loadDynamicAgent(log)) {
                System.out.println("agent not loaded: " + log.messages);
                System.exit(1);
            }
            Class<?> cls = Class.forName(AgentMain.class.getPackage().getName() + ".DynamicAgentEntity");
            if (!PersistenceCapable.class.isAssignableFrom(cls)) {
                System.out.println("entity not enhanced: " + log.messages);
                System.exit(2);
            }
            System.out.println(SUCCESS);
        }
    }

    /**
     * Log that keeps all messages of level info and above.
     */
    static class CapturingLog extends AbstractLog {
        final List<String> messages = new ArrayList<>();

        @Override
        protected boolean isEnabled(short level) {
            return level >= INFO;
        }

        @Override
        protected void log(short level, String message, Throwable t) {
            messages.add(message + (t == null ? "" : " " + t));
        }
    }
}
