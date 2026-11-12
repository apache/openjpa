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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.openjpa.lib.log.AbstractLog;
import org.junit.Test;

/**
 * Tests that {@link InstrumentationFactory} loads the attach API from the
 * jdk.attach module and attaches the agent to the running JVM.
 */
public class TestInstrumentationFactory {

    private static final String ALLOW_ATTACH_SELF = "jdk.attach.allowAttachSelf";

    @Test
    public void testLoadVirtualMachineClassFromJdkAttachModule() {
        assumeTrue(ModuleLayer.boot().findModule("jdk.attach").isPresent());

        CapturingLog log = new CapturingLog();
        Class<?> vmClass = InstrumentationFactory.loadVMClass(log);
        assertNotNull(vmClass);
        assertEquals("com.sun.tools.attach.VirtualMachine", vmClass.getName());
        assertEquals("jdk.attach", vmClass.getModule().getName());
        assertTrue(log.messages.toString(), log.messages.isEmpty());
    }

    /**
     * The default test JVM does not allow self-attach. The factory must not
     * throw, must return null and must log one message that names the
     * system property needed to enable the dynamic agent.
     */
    @Test
    public void testSelfAttachNotAllowedLogsHint() {
        assumeTrue(InstrumentationFactory.class.getClassLoader() == ClassLoader.getSystemClassLoader());
        String allow = System.getProperty(ALLOW_ATTACH_SELF);
        assumeFalse("".equals(allow) || "true".equalsIgnoreCase(allow));

        CapturingLog log = new CapturingLog();
        assertNull(InstrumentationFactory.getInstrumentation(log));
        assertEquals(log.messages.toString(), 1, log.messages.size());
        String msg = log.messages.get(0);
        assertTrue(msg, msg.contains("-D" + ALLOW_ATTACH_SELF + "=true"));
        assertTrue(msg, msg.contains("-javaagent"));

        // the attach is not retried and the message is not repeated
        assertNull(InstrumentationFactory.getInstrumentation(log));
        assertEquals(log.messages.toString(), 1, log.messages.size());
    }

    /**
     * Starts a child JVM with -Djdk.attach.allowAttachSelf=true and checks
     * that the agent is attached and an {@link Instrumentation} is obtained.
     */
    @Test
    public void testSelfAttachInChildJvm() throws IOException, InterruptedException {
        assumeTrue(ModuleLayer.boot().findModule("jdk.attach").isPresent());

        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-D" + ALLOW_ATTACH_SELF + "=true");
        cmd.add("-XX:+EnableDynamicAgentLoading");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(AttachMain.class.getName());

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        assertTrue("child JVM did not finish", p.waitFor(120, TimeUnit.SECONDS));
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(out, 0, p.exitValue());
        assertTrue(out, out.contains(AttachMain.SUCCESS));
        assertFalse(out, out.contains("WARNING: A Java agent has been loaded dynamically"));
    }

    /**
     * Entry point of the child JVM started by {@link #testSelfAttachInChildJvm()}.
     */
    public static class AttachMain {
        static final String SUCCESS = "instrumentation obtained";

        public static void main(String[] args) {
            CapturingLog log = new CapturingLog();
            Instrumentation inst = InstrumentationFactory.getInstrumentation(log);
            if (inst == null || !inst.isRedefineClassesSupported()) {
                System.out.println("no instrumentation: " + log.messages);
                System.exit(1);
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
            messages.add(message);
        }
    }
}
