/*
 * Copyright © NORD/LB Norddeutsche Landesbank Girozentrale, Hannover - Alle Rechte vorbehalten -
 */
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
package org.apache.openjpa.jira2780;

import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.PropertiesReverseCustomizer;
import org.apache.openjpa.jdbc.meta.strats.EnumValueHandler;

public class Jira2780ReverseCustomizer extends PropertiesReverseCustomizer {
    @Override
    public void customize(FieldMapping field) {
        super.customize(field);
        if (field.getDeclaredType().isEnum()) {
            EnumValueHandler enumValueHandler = new EnumValueHandler();
            enumValueHandler.setStoreOrdinal(false);
            field.setHandler(enumValueHandler);
            // As a work-around for the error, we can set the type code to
            // OBJECT to generate the @Enumerated annotation.
            // field.setDeclaredTypeCode(JavaTypes.OBJECT);
        }
    }
}
