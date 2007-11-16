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
package org.apache.openjpa.lib.conf;

import java.util.List;

import junit.framework.TestCase;
import org.apache.openjpa.lib.util.Options;

public class TestAnchorParsing extends TestCase {

    public void testFQAnchor() {
        String fqLoc = "META-INF/persistence.xml#test";
        Options opts = new Options();
        opts.setProperty("p", fqLoc);
        List locs =
            Configurations.getFullyQualifiedAnchorsInPropertiesLocation(opts);
        assertNotNull(locs);
        assertEquals(1, locs.size());
        assertEquals(fqLoc, locs.get(0));
    }

    public void testNoResource() {
        allHelper(null);
    }

    public void testNoAnchor() {
        allHelper("META-INF/persistence.xml");
    }

    private void allHelper(String resource) {
        Options opts = new Options();
        if (resource != null)
            opts.setProperty("p", resource);
        List locs =
            Configurations.getFullyQualifiedAnchorsInPropertiesLocation(opts);
        assertNotNull(locs);
        // approximate so that if someone adds more units, this doesn't break
        assertTrue(locs.size() >= 4);
        assertTrue(locs.contains("META-INF/persistence.xml#test"));
        assertTrue(locs.contains(
            "META-INF/persistence.xml#second-persistence-unit"));
        assertTrue(locs.contains(
            "META-INF/persistence.xml#third-persistence-unit"));
        assertTrue(locs.contains("META-INF/persistence.xml#invalid"));
    }
}
