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
package org.apache.openjpa.enhance.asm;

import serp.bytecode.BCClass;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Enables to abstract ASM usage for the runtime (not for tests).
 *
 * It is recommended to name the impl "AsmSpi{asmVersion}" to let the spi loader sort the impl properly.
 */
public interface AsmSpi {
    void write(BCClass bc) throws IOException;

    void write(BCClass bc, File outFile) throws IOException;

    void write(BCClass bc, OutputStream os) throws IOException;

    byte[] toByteArray(BCClass bc, byte[] returnBytes) throws IOException;

    boolean isEnhanced(byte[] b);
}
