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
package org.apache.openjpa.jdbc.meta;

import java.io.IOException;
import java.sql.SQLException;

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.simple.TemporalFieldTypes;
import org.apache.openjpa.persistence.test.SingleEMTestCase;

public class TestCalendarField extends SingleEMTestCase {

    public void setUp() {
        setUp(TemporalFieldTypes.class);
    }

    public void testCalendarField() throws IOException, SQLException {
        TimeZone tz = TimeZone.getTimeZone("Europe/Budapest");

        for (TemporalFieldTypes t : find(TemporalFieldTypes.class))
            remove(t);

        TemporalFieldTypes tft;

        tft = new TemporalFieldTypes();
        assertEquals(tz, tft.getCalendarTimeZoneField().getTimeZone());

        persist(tft);

        // get a fresh EM
        reset();

        tft = find(TemporalFieldTypes.class).get(0);
        assertEquals(tz, tft.getCalendarTimeZoneField().getTimeZone());
    }
}
