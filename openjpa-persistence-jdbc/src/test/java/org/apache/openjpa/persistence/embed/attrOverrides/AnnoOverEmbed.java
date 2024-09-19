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
package org.apache.openjpa.persistence.embed.attrOverrides;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

@Embeddable
public class AnnoOverEmbed {

    @Basic
    private int intEmbed;

    @Basic
    private String strEmbed;

    public void setIntEmbed(int intEmbed) {
        this.intEmbed = intEmbed;
    }

    public int getIntEmbed() {
        return intEmbed;
    }

    public void setStrEmbed(String strEmbed) {
        this.strEmbed = strEmbed;
    }

    public String getStrEmbed() {
        return strEmbed;
    }
}
