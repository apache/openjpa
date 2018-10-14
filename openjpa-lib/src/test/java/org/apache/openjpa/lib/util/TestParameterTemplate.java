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
package org.apache.openjpa.lib.util;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ParameterTemplate} utility class.
 *
 * @author Abe White
 */
public class TestParameterTemplate {

    private ParameterTemplate templ = new ParameterTemplate();

    @Test
    public void testParameters() {
        templ.append("{foo$${foo}bar{${bar}}biz baz$");
        templ.append("{booz}booz${java.io.tmpdir}{ack}");

        templ.setParameter("foo", "X");
        templ.setParameter("bar", "Y");
        templ.setParameter("booz", "Z");
        String tmpdir = System.getProperty("java.io.tmpdir");
        assertEquals("{foo$Xbar{Y}biz bazZbooz" + tmpdir + "{ack}",
            templ.toString());

        templ.clearParameters();
        templ.setParameter("foo", "AA");
        templ.setParameter("bar", "BB");
        templ.setParameter("booz", "CC");
        assertEquals("{foo$AAbar{BB}biz bazCCbooz" + tmpdir + "{ack}",
            templ.toString());
    }

}

