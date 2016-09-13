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
package org.apache.openjpa.enhance;

import org.apache.openjpa.conf.OpenJPAConfiguration;

import java.io.IOException;


/**
 */
public class PCEnhancerImplSerp extends PCEnhancer {
    private final OpenJPAConfiguration conf;

    public PCEnhancerImplSerp(OpenJPAConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public int getEnhancerVersion() {
        return PCEnhancerSerp.ENHANCER_VERSION;
    }

    @Override
    public boolean checkEnhancementLevel(Class<?> cls) {
        return PCEnhancerSerp.checkEnhancementLevel(cls, conf.getLog(OpenJPAConfiguration.LOG_RUNTIME));
    }

    @Override
    public boolean enhanceFiles(String[] files, Flags flags, ClassLoader cl) throws IOException {
        return PCEnhancerSerp.run(conf, files, flags, null, flags.byteCodeWriter, cl);
    }

    @Override
    public boolean isPCSubclassName(String className) {
        return PCEnhancerSerp.isPCSubclassName(className);
    }

    @Override
    public String toManagedTypeName(String className) {
        return PCEnhancerSerp.toManagedTypeName(className);
    }
}
